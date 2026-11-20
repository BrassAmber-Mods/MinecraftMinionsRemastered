package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.GameplayRules;
import io.github.jodlodi.minions.work.WorkTarget;
import io.github.jodlodi.minions.work.WorkerActions;
import io.github.jodlodi.minions.work.WorkerChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Discover connected trees on the server thread; workers fell each from its reachable trunk base. */
public final class ChopOrder extends HarvestAreaOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("chop_order");
    public static final Predicate<BlockState> DEFAULT_CHOP = GameplayRules::isTree;
    private static final int MAX_LOGS = 512, MAX_LEAVES = 2048;
    private static final class Tree {
        final BlockPos base;
        final Set<BlockPos> logs = new LinkedHashSet<>(), leaves = new LinkedHashSet<>();
        final Set<ChunkPos> chunks = new HashSet<>();
        Tree(BlockPos base) { this.base = base; }
        void indexChunks() { this.logs.forEach(pos -> this.chunks.add(new ChunkPos(pos))); this.leaves.forEach(pos -> this.chunks.add(new ChunkPos(pos))); }
    }
    private record Node(BlockPos pos, int depth) {}
    private final Map<Long, Tree> trees = new LinkedHashMap<>();
    private final Set<BlockPos> reserved = new HashSet<>();
    private final ArrayDeque<Node> frontier = new ArrayDeque<>();
    private Tree discovering;

    public ChopOrder(BlockPos min, BlockPos max, Block predicate) { super(min, max, predicate); }
    public ChopOrder(CompoundTag tag) {
        super(tag);
        ListTag saved = tag.getList("Trees", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(saved.size(), 8); i++) {
            CompoundTag entry = saved.getCompound(i); Tree tree = new Tree(BlockPos.of(entry.getLong("Base")));
            for (long pos : entry.getLongArray("Logs")) { if (tree.logs.size() >= MAX_LOGS) break; tree.logs.add(BlockPos.of(pos)); }
            for (long pos : entry.getLongArray("Leaves")) { if (tree.leaves.size() >= MAX_LEAVES) break; tree.leaves.add(BlockPos.of(pos)); }
            tree.indexChunks(); this.trees.put(tree.base.asLong(), tree); this.reserved.addAll(tree.logs);
        }
        if (tag.contains("Discovering")) this.discovering = this.trees.get(tag.getLong("Discovering"));
        if (this.discovering != null) {
            ListTag nodes = tag.getList("TreeFrontier", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(nodes.size(), MAX_LOGS + MAX_LEAVES); i++) {
                CompoundTag node = nodes.getCompound(i); this.frontier.add(new Node(BlockPos.of(node.getLong("Pos")), Math.max(0, Math.min(6, node.getInt("Depth")))));
            }
        }
    }
    @Override public ResourceLocation getID() { return ID; }
    @Override protected boolean matches(BlockState state) { return DEFAULT_CHOP.test(state); }
    @Override protected boolean scanMatch(ServerPlayer owner, ServerLevel level, BlockPos pos) {
        if (this.reserved.contains(pos)) return true;
        if (!this.loaded(owner, level, pos.below())) return false;
        if (this.getPredicate().test(level.getBlockState(pos.below()))) return true;
        if (this.trees.size() >= 8) return false;
        this.discovering = new Tree(pos.immutable());
        this.discovering.logs.add(pos.immutable()); this.reserved.add(pos.immutable());
        this.trees.put(pos.asLong(), this.discovering); this.frontier.add(new Node(pos.immutable(), 0));
        return true;
    }
    @Override protected boolean advanceDiscovery(ServerPlayer owner, ServerLevel level) {
        if (this.discovering == null) return true;
        int budget = Math.max(1, MinionsConfig.WORK_SCAN_BUDGET.get() / 26);
        while (budget-- > 0 && !this.frontier.isEmpty()) {
            Node node = this.frontier.removeFirst();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockPos next = node.pos().offset(x, y, z);
                if (next.getY() < this.discovering.base.getY() || next.getY() > this.discovering.base.getY() + 64
                        || Math.abs(next.getX() - this.discovering.base.getX()) > 16 || Math.abs(next.getZ() - this.discovering.base.getZ()) > 16
                        || !level.isInWorldBounds(next) || !level.getWorldBorder().isWithinBounds(next)) continue;
                if (!this.loaded(owner, level, next)) { this.frontier.addFirst(node); return false; }
                BlockState state = level.getBlockState(next);
                if (node.depth() == 0 && this.getPredicate().test(state) && this.discovering.logs.size() < MAX_LOGS && !this.reserved.contains(next)) {
                    this.discovering.logs.add(next); this.reserved.add(next); this.frontier.addLast(new Node(next, 0));
                } else if (state.is(BlockTags.LEAVES) && (!state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT)) && node.depth() < 6
                        && Math.abs(x) + Math.abs(y) + Math.abs(z) == 1 && this.discovering.leaves.size() < MAX_LEAVES && this.discovering.leaves.add(next)) {
                    this.frontier.addLast(new Node(next, node.depth() + 1));
                }
            }
        }
        if (!this.frontier.isEmpty()) return false;
        Tree tree = this.discovering; this.discovering = null;
        tree.indexChunks();
        if (!level.getBlockState(tree.base).isAir()) this.enqueue(new WorkTarget(tree.base, level.getBlockState(tree.base), null, null));
        else { this.trees.remove(tree.base.asLong()); this.reserved.removeAll(tree.logs); }
        return true;
    }
    @Override protected float workSpeed(Minion worker, WorkTarget target, float speed) {
        Tree tree = this.trees.get(target.pos.asLong());
        return tree == null || speed <= 0 ? speed : worker.getMineSpeed() / 3.75F / Math.max(20, tree.logs.size() * 20);
    }
    @Override protected boolean harvest(Minion worker, WorkTarget target) {
        Tree tree = this.trees.get(target.pos.asLong());
        if (tree == null) return super.harvest(worker, target);
        if (!WorkerActions.harvest(worker, tree.base)) return false;
        boolean denied = false;
        for (BlockPos pos : tree.logs) if (!pos.equals(tree.base) && this.getPredicate().test(worker.level().getBlockState(pos))) {
            if (!WorkerActions.harvest(worker, pos)) denied = true;
        }
        if (!denied) for (BlockPos pos : tree.leaves) {
            BlockState state = worker.level().getBlockState(pos);
            if (state.is(BlockTags.LEAVES) && (!state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT))) WorkerActions.harvest(worker, pos);
        }
        return true;
    }
    @Override protected void targetDone(WorkTarget target) {
        Tree tree = this.trees.remove(target.pos.asLong()); if (tree != null) this.reserved.removeAll(tree.logs);
    }
    @Override protected boolean readyToHarvest(Minion worker, WorkTarget target) {
        Tree tree = this.trees.get(target.pos.asLong());
        if (tree == null || !(worker.getOwner() instanceof ServerPlayer owner) || !(worker.level() instanceof ServerLevel level)) return true;
        boolean loaded = true;
        for (ChunkPos chunk : tree.chunks) {
            BlockPos pos = new BlockPos(chunk.getMinBlockX(), tree.base.getY(), chunk.getMinBlockZ());
            WorkerChunks.keepLoaded(owner, level, pos); if (!level.hasChunkAt(pos)) loaded = false;
        }
        return loaded;
    }
    @Override protected CompoundTag saveHarvest(CompoundTag tag) {
        ListTag saved = new ListTag();
        for (Tree tree : this.trees.values()) {
            CompoundTag entry = new CompoundTag(); entry.putLong("Base", tree.base.asLong());
            entry.putLongArray("Logs", tree.logs.stream().mapToLong(BlockPos::asLong).toArray());
            entry.putLongArray("Leaves", tree.leaves.stream().mapToLong(BlockPos::asLong).toArray()); saved.add(entry);
        }
        tag.put("Trees", saved);
        if (this.discovering != null) {
            tag.putLong("Discovering", this.discovering.base.asLong()); ListTag nodes = new ListTag();
            for (Node node : this.frontier) { CompoundTag entry = new CompoundTag(); entry.putLong("Pos", node.pos().asLong()); entry.putInt("Depth", node.depth()); nodes.add(entry); }
            tag.put("TreeFrontier", nodes);
        }
        return tag;
    }
    @Override public CompoundTag serializeForClient() {
        CompoundTag tag = super.serializeForClient(); tag.remove("Trees"); tag.remove("Discovering"); tag.remove("TreeFrontier"); return tag;
    }
}
