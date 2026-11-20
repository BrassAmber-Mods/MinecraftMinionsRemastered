package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.goals.EnactOrderGoal;
import io.github.jodlodi.minions.work.WorkerChunks;
import io.github.jodlodi.minions.work.WorkerMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Workers remain at the selected destination until another order or Follow. */
public final class MoveOrder extends AbstractOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("move_order");
    private final BlockPos destination;
    public MoveOrder(BlockPos destination) { this.destination = destination.immutable(); }
    public MoveOrder(CompoundTag tag) { this.destination = BlockPos.of(tag.getLong("Destination")); }
    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean goalCanUse(EnactOrderGoal goal) {
        return goal.getOwner() instanceof ServerPlayer owner && owner.isAlive() && owner.level() == goal.getMinion().level();
    }
    @Override public boolean goalCanContinueToUse(EnactOrderGoal goal) { return this.goalCanUse(goal); }
    @Override public void goalStart(EnactOrderGoal goal) {}
    @Override public void goalStop(EnactOrderGoal goal) { goal.getNavigation().stop(); }
    @Override public void goalTick(EnactOrderGoal goal) {
        var worker = goal.getMinion();
        if (worker.tickCount % 10 != 0 || !(worker.level() instanceof ServerLevel level)) return;
        if (worker.distanceToSqr(Vec3.atBottomCenterOf(this.destination)) <= 4) { goal.getNavigation().stop(); return; }
        Vec3 stand = WorkerMovement.findStandNear(worker, this.destination);
        Vec3 point = stand != null ? stand : Vec3.atBottomCenterOf(this.destination);
        goal.getNavigation().moveTo(point.x, point.y, point.z, goal.getSpeedModifier());
    }
    @Override public void tick(IMasterCapability master, Player player, Level level) {
        if (!master.isPaused() && master.isOrderIn(level) && player instanceof ServerPlayer owner) WorkerChunks.keepLoaded(owner, owner.serverLevel(), this.destination);
    }
    @Override public CompoundTag serialize() { CompoundTag tag = new CompoundTag(); tag.putLong("Destination", this.destination.asLong()); return tag; }
}
