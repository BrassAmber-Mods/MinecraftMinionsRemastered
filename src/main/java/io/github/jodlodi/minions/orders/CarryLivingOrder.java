package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.minion.goals.EnactOrderGoal;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.work.WorkerChunks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CarryLivingOrder extends AbstractOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("carry_living_order");
    private final UUID target;
    private BlockPos lastPosition;
    private UUID carrier;
    private int waiting;

    public CarryLivingOrder(UUID target, BlockPos position) { this.target = target; this.lastPosition = position.immutable(); }
    public CarryLivingOrder(UUID target) { this.target = target; }
    public CarryLivingOrder(CompoundTag tag) {
        if (!tag.hasUUID("target")) throw new IllegalArgumentException("Missing carry target");
        this.target = tag.getUUID("target");
        this.lastPosition = tag.contains("Position") ? BlockPos.of(tag.getLong("Position")) : null;
        this.waiting = Math.max(0, Math.min(600, tag.getInt("Waiting")));
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("target", this.target);
        if (this.lastPosition != null) tag.putLong("Position", this.lastPosition.asLong());
        tag.putInt("Waiting", this.waiting);
        return tag;
    }
    @Override
    public ResourceLocation getID() { return ID; }

    private boolean available(EnactOrderGoal goal) {
        Minion worker = goal.getMinion();
        if (worker.isVehicle() || worker.needsDelivery() || !(goal.getOwner() instanceof ServerPlayer owner)
                || !owner.isAlive() || owner.level() != worker.level() || !goal.getMasterCapability().isOrderIn(worker.level())) return false;
        if (this.carrier != null && worker.level() instanceof ServerLevel level && level.getEntity(this.carrier) == null) this.carrier = null;
        return this.carrier == null || this.carrier.equals(worker.getUUID());
    }

    @Override
    public boolean goalCanUse(EnactOrderGoal goal) { return this.available(goal); }
    @Override
    public boolean goalCanContinueToUse(EnactOrderGoal goal) { return this.available(goal); }
    @Override
    public void goalStart(EnactOrderGoal goal) { this.carrier = goal.getMinion().getUUID(); }
    @Override
    public void goalStop(EnactOrderGoal goal) { if (goal.getMinion().getUUID().equals(this.carrier)) this.carrier = null; }
    @Override
    public void interrupt(ServerLevel level) { this.carrier = null; }

    @Override
    public void goalTick(EnactOrderGoal goal) {
        if (!(goal.getMinion().level() instanceof ServerLevel level)) return;
        Entity victim = level.getEntity(this.target);
        if (victim == null || victim.isPassenger() || !victim.isAlive()) return;
        Minion worker = goal.getMinion();
        worker.getLookControl().setLookAt(victim);
        if (worker.distanceToSqr(victim) > 3.0D) {
            if (worker.tickCount % 10 == 0) worker.getNavigation().moveTo(victim, goal.getSpeedModifier());
        } else if (!worker.getBlinking() && victim.startRiding(worker, true)) {
            goal.getNavigation().stop();
        }
    }

    @Override
    public void tick(IMasterCapability master, Player player, Level level) {
        if (!(player instanceof ServerPlayer owner) || !master.isOrderIn(level) || master.isPaused() || !owner.isAlive()) return;
        Entity victim = owner.serverLevel().getEntity(this.target);
        if (victim != null) {
            this.lastPosition = victim.blockPosition();
            if (!victim.isAlive() || victim.isPassenger() || !CommandValidation.friendly(owner, victim)) { master.setOrder(null); return; }
        } else if (this.lastPosition != null) WorkerChunks.keepLoaded(owner, owner.serverLevel(), this.lastPosition);
        if (++this.waiting >= 600) {
            master.setOrder(null);
            owner.displayClientMessage(Component.translatable("message.minions_remastered.work.carry_failed"), false);
        }
    }
}
