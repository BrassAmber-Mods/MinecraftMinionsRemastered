package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.minion.goals.EnactOrderGoal;
import io.github.jodlodi.minions.work.WorkTarget;
import io.github.jodlodi.minions.work.WorkerActions;
import io.github.jodlodi.minions.work.WorkerChunks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

/** One scheduler per player's order: bounded scans, unique block leases, and retryable saved targets. */
public abstract class AbstractWorkOrder extends AbstractOrder {
    public static final int MAX_PENDING = 256;
    private final LinkedHashMap<Long, WorkTarget> pending = new LinkedHashMap<>();
    private final Map<UUID, Claim> claims = new HashMap<>();
    private final Set<Long> seenVeins = new HashSet<>();
    private final Set<Long> blockedSafety = new HashSet<>();
    private record VeinSearch(BlockPos pos, BlockPos origin) {}
    private final LinkedHashMap<Long, VeinSearch> veinSearches = new LinkedHashMap<>();
    private boolean sourceFinished;
    private int skipped;

    private static final class Claim {
        final WorkTarget target;
        float progress;
        long lastTouched;
        int stalled;
        double bestDistance = Double.MAX_VALUE;
        Claim(WorkTarget target, long now) { this.target = target; this.lastTouched = now; }
    }

    protected AbstractWorkOrder() {}

    protected AbstractWorkOrder(CompoundTag tag) {
        CompoundTag work = tag.getCompound("Work");
        this.sourceFinished = work.getBoolean("Finished");
        this.skipped = Math.max(0, work.getInt("Skipped"));
        ListTag queue = work.getList("Queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(queue.size(), MAX_PENDING + 4); i++) {
            WorkTarget target = WorkTarget.load(queue.getCompound(i));
            this.pending.put(target.pos.asLong(), target);
        }
        long[] visited = work.getLongArray("Veins");
        for (int i = 0; i < Math.min(visited.length, 512); i++) this.seenVeins.add(visited[i]);
        long[] blocked = work.getLongArray("BlockedSafety");
        for (int i = 0; i < Math.min(blocked.length, 512); i++) this.blockedSafety.add(blocked[i]);
        ListTag searches = work.getList("VeinSearches", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(searches.size(), 512); i++) {
            CompoundTag search = searches.getCompound(i);
            BlockPos pos = BlockPos.of(search.getLong("Pos"));
            this.veinSearches.put(pos.asLong(), new VeinSearch(pos, search.contains("Origin") ? BlockPos.of(search.getLong("Origin")) : null));
        }
    }

    protected final CompoundTag saveWork(CompoundTag tag) {
        CompoundTag work = new CompoundTag();
        work.putBoolean("Finished", this.sourceFinished);
        work.putInt("Skipped", this.skipped);
        ListTag queue = new ListTag();
        for (WorkTarget target : this.pending.values()) queue.add(target.save());
        for (Claim claim : this.claims.values()) queue.add(claim.target.save());
        work.put("Queue", queue);
        work.putLongArray("Veins", this.seenVeins.stream().mapToLong(Long::longValue).toArray());
        work.putLongArray("BlockedSafety", this.blockedSafety.stream().mapToLong(Long::longValue).toArray());
        ListTag searches = new ListTag();
        for (VeinSearch search : this.veinSearches.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", search.pos().asLong());
            if (search.origin() != null) entry.putLong("Origin", search.origin().asLong());
            searches.add(entry);
        }
        work.put("VeinSearches", searches);
        tag.put("Work", work);
        return tag;
    }

    @Override
    public CompoundTag serializeForClient() {
        CompoundTag tag = this.serialize();
        tag.remove("Work");
        return tag;
    }

    protected abstract boolean scanNext(ServerPlayer owner, ServerLevel level);
    protected abstract void preview(Player player, Level level);
    protected boolean followsVeins() { return false; }
    protected void afterBreak(Minion worker, BlockPos pos) {}
    protected final boolean drained() { return this.pending.isEmpty() && this.claims.isEmpty() && this.veinSearches.isEmpty(); }
    protected final void finishSource() { this.sourceFinished = true; }
    protected final void nextStage() { this.seenVeins.clear(); this.blockedSafety.clear(); }

    protected final boolean loaded(ServerPlayer owner, ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) return true;
        WorkerChunks.keepLoaded(owner, level, pos);
        return level.hasChunkAt(pos);
    }

    protected final boolean scanBlock(ServerPlayer owner, ServerLevel level, BlockPos pos, BlockState replacement) {
        if (!this.loaded(owner, level, pos)) return false;
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if (replacement != null && state.equals(replacement)) return true;
        if (!state.isAir() && !state.canBeReplaced() && state.getDestroySpeed(level, pos) < 0) {
            this.skipped++;
            return true;
        }
        if (!state.isAir() || replacement != null) this.enqueue(new WorkTarget(pos, state, replacement, null));
        if (this.followsVeins() && state.isAir() && replacement == null) this.discoverOres(owner, level, pos, null);
        return true;
    }

    private void enqueue(WorkTarget target) {
        WorkTarget existing = this.pending.get(target.pos.asLong());
        if (existing == null) for (Claim claim : this.claims.values()) {
            if (claim.target.pos.equals(target.pos)) { existing = claim.target; break; }
        }
        if (existing != null) {
            if (target.replacement != null) existing.replacement = target.replacement;
            existing.priority = Math.max(existing.priority, target.priority);
        } else if (this.pending.size() + this.claims.size() < MAX_PENDING + 4) {
            this.pending.put(target.pos.asLong(), target);
        }
    }

    private void discoverOres(ServerPlayer owner, ServerLevel level, BlockPos pos, BlockPos origin) {
        if (!this.followsVeins() || !MinionsConfig.MINE_ORE_VEINS.get() || this.seenVeins.size() >= MinionsConfig.MAX_VEIN_BLOCKS.get()) return;
        // Reserve room for all six neighbors; defer expansion instead of losing branches when busy.
        if (this.pending.size() + this.claims.size() > MAX_PENDING - 6) {
            this.deferVein(pos, origin);
            return;
        }
        for (Direction direction : Direction.values()) {
            if (this.seenVeins.size() >= MinionsConfig.MAX_VEIN_BLOCKS.get() || this.pending.size() + this.claims.size() >= MAX_PENDING) return;
            BlockPos adjacent = pos.relative(direction);
            BlockPos root = origin != null ? origin : adjacent;
            if (adjacent.distManhattan(root) > MinionsConfig.VEIN_RADIUS.get() || !level.isInWorldBounds(adjacent)
                    || !level.getWorldBorder().isWithinBounds(adjacent)) continue;
            WorkerChunks.keepLoaded(owner, level, adjacent);
            if (!level.hasChunkAt(adjacent)) { this.deferVein(pos, origin); continue; }
            if (WorkerActions.isOre(level.getBlockState(adjacent)) && this.seenVeins.add(adjacent.asLong())) {
                this.enqueue(new WorkTarget(adjacent, level.getBlockState(adjacent), null, root));
            }
        }
    }

    private void deferVein(BlockPos pos, BlockPos origin) {
        if (this.veinSearches.size() < 512) this.veinSearches.putIfAbsent(pos.asLong(), new VeinSearch(pos.immutable(), origin));
    }

    @Override
    public final void tick(IMasterCapability master, Player player, Level level) {
        if (!master.isOrderIn(level)) return;
        if (level.isClientSide) { this.preview(player, level); return; }
        if (!(player instanceof ServerPlayer owner) || !owner.isAlive()) return;
        ServerLevel server = owner.serverLevel();
        this.expireClaims(master, server);
        if (master.isPaused()) { this.releaseAll(server); return; }
        int budget = MinionsConfig.WORK_SCAN_BUDGET.get();
        int searches = Math.min(budget, this.veinSearches.size());
        while (searches-- > 0 && this.pending.size() + this.claims.size() <= MAX_PENDING - 6) {
            VeinSearch search = this.veinSearches.values().iterator().next();
            this.veinSearches.remove(search.pos().asLong());
            this.discoverOres(owner, server, search.pos(), search.origin());
            budget--;
        }
        while (budget-- > 0 && this.veinSearches.isEmpty() && !this.sourceFinished && this.pending.size() + this.claims.size() < MAX_PENDING) {
            if (!this.scanNext(owner, server)) break;
        }
        if (this.sourceFinished && this.drained()) {
            for (UUID id : master.getMinions()) if (id != null && server.getEntity(id) instanceof Minion worker) worker.requestDelivery();
            master.setOrder(null);
            if (this.skipped > 0) owner.displayClientMessage(Component.translatable("message.minions_remastered.work.skipped", this.skipped), false);
        }
    }

    private void expireClaims(IMasterCapability master, ServerLevel level) {
        for (UUID id : new ArrayList<>(this.claims.keySet())) {
            Claim claim = this.claims.get(id);
            if (!master.isMinion(id) || !(level.getEntity(id) instanceof Minion worker) || worker.sittingOrRiding()
                    || worker.needsDelivery() || level.getGameTime() - claim.lastTouched > 100) this.release(id, level, false);
        }
    }

    @Override
    public void interrupt(ServerLevel level) { this.releaseAll(level); }

    private void releaseAll(ServerLevel level) {
        for (UUID id : new ArrayList<>(this.claims.keySet())) this.release(id, level, false);
    }

    private void release(UUID id, ServerLevel level, boolean failed) {
        Claim claim = this.claims.remove(id);
        if (claim == null) return;
        if (level.getEntity(id) instanceof Minion worker) level.destroyBlockProgress(worker.getId(), claim.target.pos, -1);
        if (failed && ++claim.target.attempts >= 3) { this.markSkipped(claim.target); return; }
        if (failed) claim.target.retryAt = level.getGameTime() + 100;
        this.pending.put(claim.target.pos.asLong(), claim.target);
    }

    @Override
    public boolean goalCanUse(EnactOrderGoal goal) {
        return goal.getOwner() instanceof ServerPlayer owner && owner.isAlive() && owner.level() == goal.getMinion().level()
                && goal.getMasterCapability().isOrderIn(goal.getMinion().level()) && !goal.getMinion().needsDelivery();
    }

    @Override
    public boolean goalCanContinueToUse(EnactOrderGoal goal) { return this.goalCanUse(goal); }
    @Override
    public void goalStart(EnactOrderGoal goal) {}
    @Override
    public void goalStop(EnactOrderGoal goal) {
        if (goal.getMinion().level() instanceof ServerLevel level) this.release(goal.getMinion().getUUID(), level, false);
        goal.getMinion().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private Claim claim(Minion worker, ServerLevel level) {
        Claim current = this.claims.get(worker.getUUID());
        if (current != null) return current;
        WorkTarget best = null;
        double distance = Double.MAX_VALUE;
        for (WorkTarget target : this.pending.values()) {
            if (target.retryAt > level.getGameTime()) continue;
            if (!level.hasChunkAt(target.pos)) {
                if (worker.getOwner() instanceof ServerPlayer owner) WorkerChunks.keepLoaded(owner, level, target.pos);
                continue;
            }
            double candidate = worker.distanceToSqr(Vec3.atCenterOf(target.pos));
            if (best == null || target.priority > best.priority || (target.priority == best.priority && candidate < distance)) {
                best = target; distance = candidate;
            }
        }
        if (best == null) return null;
        this.pending.remove(best.pos.asLong());
        Claim claim = new Claim(best, level.getGameTime());
        this.claims.put(worker.getUUID(), claim);
        return claim;
    }

    @Override
    public final void goalTick(EnactOrderGoal goal) {
        Minion worker = goal.getMinion();
        if (!(worker.level() instanceof ServerLevel level)) return;
        if (worker.needsDelivery()) { this.release(worker.getUUID(), level, false); return; }
        Claim claim = this.claim(worker, level);
        if (claim == null) { goal.getNavigation().stop(); return; }
        claim.lastTouched = level.getGameTime();
        WorkTarget target = claim.target;
        if (!level.hasChunkAt(target.pos)) { this.release(worker.getUUID(), level, false); return; }
        BlockState state = level.getBlockState(target.pos);
        if (target.replacement != null && state.equals(target.replacement)) { this.complete(worker, level, claim); return; }
        if (state.isAir() && target.replacement == null) { this.complete(worker, level, claim); return; }
        if (!state.isAir() && state.getBlock() != target.expected.getBlock()) { this.markSkipped(target); this.complete(worker, level, claim); return; }
        Vec3 center = Vec3.atCenterOf(target.pos);
        double distance = worker.getEyePosition().distanceToSqr(center);
        worker.getLookControl().setLookAt(center);
        BlockHitResult sight = level.clip(new ClipContext(worker.getEyePosition(), center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, worker));
        boolean reachable = distance <= 12.25D && (sight.getType() == HitResult.Type.MISS || sight.getBlockPos().equals(target.pos));
        if (!reachable) {
            claim.progress = 0;
            level.destroyBlockProgress(worker.getId(), target.pos, -1);
            if (distance < claim.bestDistance - 0.05D) { claim.bestDistance = distance; claim.stalled = 0; }
            else if (++claim.stalled >= MinionsConfig.UNREACHABLE_TICKS.get()) {
                goal.getNavigation().stop();
                this.release(worker.getUUID(), level, true);
                return;
            }
            if (worker.tickCount % 10 == 0) {
                BlockPos approach = MinUtil.randomOpenNearbyOrAboveOrBelow(target.pos, level);
                Vec3 destination = approach != null ? Vec3.atBottomCenterOf(approach) : center;
                goal.getNavigation().moveTo(destination.x, destination.y, destination.z, goal.getSpeedModifier());
            }
            return;
        }
        goal.getNavigation().stop();
        if (worker.getBlinking()) return;
        if (state.canBeReplaced() && target.replacement != null) {
            if (!WorkerActions.place(worker, target.pos, target.replacement)) this.skipped++;
            this.complete(worker, level, claim);
            return;
        }
        if (!WorkerActions.canHarvest(worker, target.pos)) { this.markSkipped(target); this.complete(worker, level, claim); return; }
        if (this.followsVeins()) {
            if (worker.getOwner() instanceof ServerPlayer owner) {
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = target.pos.relative(direction);
                    if (!this.loaded(owner, level, neighbor)) return;
                }
            }
            BlockPos roof = target.pos.above();
            int count = 0;
            while (level.hasChunkAt(roof) && level.getBlockState(roof).getBlock() instanceof FallingBlock && count++ < 16) roof = roof.above();
            if (count > 0) {
                BlockPos falling = roof.below();
                if (count >= 16 || this.blockedSafety.contains(falling.asLong())) { this.markSkipped(target); this.complete(worker, level, claim); return; }
                this.release(worker.getUUID(), level, false);
                WorkTarget safety = new WorkTarget(falling, level.getBlockState(falling), null, null);
                safety.priority = 1;
                this.enqueue(safety);
                return;
            }
            if (!WorkerActions.sealFluids(worker, target.pos)) { this.markSkipped(target); this.complete(worker, level, claim); return; }
        }
        ItemStack tool = MinUtil.bestToolForBlock(state);
        worker.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var actor = worker.workerPlayer();
        actor.moveTo(worker.getX(), worker.getY(), worker.getZ(), worker.getYRot(), worker.getXRot());
        actor.setOnGround(true);
        actor.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        float speed;
        try { speed = state.getDestroyProgress(actor, level, target.pos) * worker.getMineSpeed() / 3.75F; }
        finally { actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); }
        claim.progress += Math.max(0, speed);
        worker.swing(InteractionHand.MAIN_HAND);
        if (claim.progress >= 1.0F) {
            boolean ore = WorkerActions.isOre(state);
            if (WorkerActions.harvest(worker, target.pos)) {
                if (target.replacement != null && !WorkerActions.place(worker, target.pos, target.replacement)) this.skipped++;
                this.afterBreak(worker, target.pos);
                if (worker.getOwner() instanceof ServerPlayer owner) this.discoverOres(owner, level, target.pos,
                        target.veinOrigin != null ? target.veinOrigin : ore ? target.pos : null);
            } else this.markSkipped(target);
            this.complete(worker, level, claim);
        } else {
            if (speed <= 0 && ++claim.stalled >= MinionsConfig.UNREACHABLE_TICKS.get()) this.release(worker.getUUID(), level, true);
            else level.destroyBlockProgress(worker.getId(), target.pos, Math.min(9, (int)(claim.progress * 10)));
        }
    }

    private void complete(Minion worker, ServerLevel level, Claim claim) {
        level.destroyBlockProgress(worker.getId(), claim.target.pos, -1);
        this.claims.remove(worker.getUUID());
    }

    private void markSkipped(WorkTarget target) {
        this.skipped++;
        if (target.priority > 0 && this.blockedSafety.size() < 512) this.blockedSafety.add(target.pos.asLong());
    }
}
