package io.github.jodlodi.minions.minion.goals;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.WorkerActions;
import io.github.jodlodi.minions.work.WorkerChunks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.items.IItemHandler;

/** Cargo stays with the worker while it walks home, waits for space, and resumes its saved job. */
public final class HaulItemsGoal extends Goal {
    private final Minion worker;
    private IMasterCapability master;
    private int retry;
    private int stuck;
    private double bestDistance = Double.MAX_VALUE;
    private BlockPos lastDestination;
    private long lastMessage;

    public HaulItemsGoal(Minion worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.worker.hasCargo() || this.worker.sittingOrRiding() || this.worker.isVehicle()
                || !(this.worker.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.worker.level()) return false;
        this.master = this.worker.getMastersCapability().orElse(null);
        return this.master != null && (this.worker.needsDelivery() || this.master.getOrder() == null || this.master.isPaused() || !this.master.isOrderIn(this.worker.level()));
    }

    @Override
    public boolean canContinueToUse() { return this.canUse(); }
    @Override
    public boolean requiresUpdateEveryTick() { return true; }
    @Override
    public void start() {
        this.worker.requestDelivery();
        this.worker.setHauling(true);
        this.worker.getNavigation().stop();
        this.retry = 0;
        this.stuck = 0;
        this.bestDistance = Double.MAX_VALUE;
    }
    @Override
    public void stop() {
        this.worker.setHauling(false);
        this.worker.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(this.worker.level() instanceof ServerLevel level) || !(this.worker.getOwner() instanceof ServerPlayer owner)) return;
        if (--this.retry > 0) return;
        this.retry = 20;
        if (!this.master.getContainerDimension().equals(level.dimension().location())) { this.waiting(owner); return; }
        BlockPos block = this.master.getContainerBlock();
        Entity target = null;
        Vec3 destination;
        if (block != null) {
            WorkerChunks.keepLoaded(owner, level, block);
            if (!level.hasChunkAt(block)) { this.waiting(owner); return; }
            if (level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, block, null) == null) {
                block = null;
                target = owner;
            }
        } else {
            target = level.getEntity(this.master.getContainerEntity());
            if (target == null) {
                BlockPos last = this.master.getContainerEntityPosition();
                if (last != null) { WorkerChunks.keepLoaded(owner, level, last); if (!level.hasChunkAt(last)) { this.waiting(owner); return; } }
                target = owner;
            }
        }
        destination = block != null ? Vec3.atCenterOf(block) : target.position();
        if (!BlockPos.containing(destination).equals(this.lastDestination)) {
            this.lastDestination = BlockPos.containing(destination);
            this.bestDistance = Double.MAX_VALUE;
            this.stuck = 0;
        }
        double distance = this.worker.distanceToSqr(destination);
        this.worker.getLookControl().setLookAt(destination);
        var sight = level.clip(new ClipContext(this.worker.getEyePosition(), destination, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.worker));
        boolean visible = sight.getType() == HitResult.Type.MISS || (block != null && sight.getBlockPos().equals(block));
        if (distance <= 6.25D && visible) {
            this.worker.getNavigation().stop();
            IItemHandler handler = block != null ? WorkerActions.container(this.worker, block) : WorkerActions.container(this.worker, target);
            if (handler == null || this.worker.unloadCargo(handler) == 0) { this.retry = 40; this.waiting(owner); }
            else this.retry = 1;
            return;
        }
        if (distance < this.bestDistance - 0.1D) { this.bestDistance = distance; this.stuck = 0; }
        else this.stuck += 20;
        BlockPos approach = MinUtil.randomOpenNearbyOrAboveOrBelow(BlockPos.containing(destination), level);
        Vec3 point = approach != null ? Vec3.atBottomCenterOf(approach) : destination;
        this.worker.getNavigation().moveTo(point.x, point.y, point.z, 1.1D);
        if (this.stuck >= 200 && approach != null && this.worker.canBlink()) {
            this.worker.blink(approach);
            this.stuck = 0;
        }
    }

    private void waiting(ServerPlayer owner) {
        this.worker.getNavigation().stop();
        if (owner.level().getGameTime() - this.lastMessage >= 200) {
            this.lastMessage = owner.level().getGameTime();
            owner.displayClientMessage(Component.translatable("message.minions_remastered.work.storage", this.worker.getName()), true);
        }
    }
}
