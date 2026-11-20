package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.orders.*;
import io.github.jodlodi.minions.registry.WorkerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Common entry points for both staff buttons and player commands. */
public final class StaffActions {
    public static final int MOVE = 0, FOLLOW = 1, VEIN = 2, STRIP = 3, DROP = 4, UNSUMMON = 5, DIG = 6, DELIVER = 7, RECALL = 8;
    private StaffActions() {}

    public static boolean target(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 32 * 32 || !level.mayInteract(player, pos)) return false;
        var hit = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    public static BlockPos lookedAt(ServerPlayer player) {
        var hit = player.pick(30, 0, false);
        return hit.getType() == HitResult.Type.BLOCK ? ((BlockHitResult)hit).getBlockPos() : null;
    }

    public static int execute(ServerPlayer player, IMasterCapability master, int action, BlockPos pos, int width, int height) {
        if (!GameplayRules.hasStaff(player) || !CommandValidation.canCommand(player)) return 0;
        if (action == UNSUMMON) { release(player, master); return 1; }
        if (master.minionCount() == 0) { player.displayClientMessage(Component.translatable("message.minions_remastered.no_workers"), false); return 0; }
        if (action == FOLLOW) {
            master.setOrder(null);
            forEach(player, master, worker -> worker.setSit(false));
            WorkerSounds.play(player, "orderfollowplayer"); return 1;
        }
        if (action == DROP) { forEach(player, master, worker -> { if (worker.isVehicle()) worker.ejectPassengers(); else worker.dropCargo(); }); return 1; }
        if (action == DELIVER) { master.setOrder(null); forEach(player, master, Minion::requestDelivery); return 1; }
        if (pos == null || !target(player, pos)) return 0;
        if (action == RECALL) return recall(player, master, pos);
        if (action == MOVE) {
            BlockPos stand = player.level().getBlockState(pos).canBeReplaced() ? pos : pos.above();
            master.setOrder(new MoveOrder(stand)); WorkerSounds.play(player, "randomorder"); return 1;
        }
        if (action == VEIN) {
            var state = player.level().getBlockState(pos);
            if (!GameplayRules.isValuable(state) || state.getDestroySpeed(player.level(), pos) < 0) return 0;
            return GameplayRules.start(player, master, new VeinOrder(pos, state.getBlock())) ? 1 : 0;
        }
        if (action == STRIP) {
            StripMineOrder strips = master.getOrder() instanceof StripMineOrder existing && master.isOrderIn(player.level()) ? existing : new StripMineOrder();
            int slot = -1;
            for (int i = 0; i < master.getMinions().size(); i++) if (master.getMinions().get(i) != null && !strips.hasSlot(i)) { slot = i; break; }
            if (slot < 0 || !GameplayRules.payForOrder(player)) return 0;
            strips.add(slot, pos, facing(player, pos));
            if (master.getOrder() != strips) master.setOrder(strips); else { master.setPaused(false); master.sendUpdatePacket(); }
            WorkerSounds.play(player, "randomorder"); return 1;
        }
        if (action == DIG && GameplayRules.isHoleWidth(width, 71) && height >= 3 && height <= 25) {
            Direction facing = facing(player, pos), across = facing.getClockWise();
            BlockPos first = pos.relative(across, -(width - 1) / 2);
            BlockPos last = first.relative(across, width - 1).relative(facing, width - 1).above(height - 1);
            BlockPos min = new BlockPos(Math.min(first.getX(), last.getX()), first.getY(), Math.min(first.getZ(), last.getZ()));
            BlockPos max = new BlockPos(Math.max(first.getX(), last.getX()), last.getY(), Math.max(first.getZ(), last.getZ()));
            if (!player.serverLevel().isInWorldBounds(min) || !player.serverLevel().isInWorldBounds(max)
                    || !player.serverLevel().getWorldBorder().isWithinBounds(min) || !player.serverLevel().getWorldBorder().isWithinBounds(max)) return 0;
            return GameplayRules.start(player, master, new ExcavateOrder(min, max, pos)) ? 1 : 0;
        }
        return 0;
    }

    private static Direction facing(ServerPlayer player, BlockPos pos) {
        double x = pos.getX() + 0.5 - player.getX(), z = pos.getZ() + 0.5 - player.getZ();
        return Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.EAST : Direction.WEST) : (z > 0 ? Direction.SOUTH : Direction.NORTH);
    }
    private static int recall(ServerPlayer player, IMasterCapability master, BlockPos selected) {
        BlockPos base = player.level().getBlockState(selected).canBeReplaced() ? selected : selected.above();
        int moved = 0;
        for (int slot = 0; slot < master.getMinions().size(); slot++) {
            var id = master.getMinions().get(slot); if (id == null) continue;
            Minion worker = null;
            for (ServerLevel level : player.getServer().getAllLevels()) if (level.getEntity(id) instanceof Minion found && player.getUUID().equals(found.getOwnerUUID())) { worker = found; break; }
            boolean restored = worker == null;
            if (restored) { worker = new Minion(player.level()); worker.setOwnerUUID(player.getUUID()); }
            BlockPos stand = null;
            for (BlockPos candidate : List.of(base, base.north(), base.south(), base.east(), base.west(), base.north().east(), base.south().west())) {
                if (!player.serverLevel().isInWorldBounds(candidate) || !player.serverLevel().hasChunkAt(candidate)
                        || !player.serverLevel().getWorldBorder().isWithinBounds(candidate) || !player.serverLevel().mayInteract(player, candidate)
                        || !player.level().getBlockState(candidate).getFluidState().isEmpty()) continue;
                Vec3 dest = Vec3.atBottomCenterOf(candidate);
                var box = MinUtil.getRidingAABB(worker).move(dest.subtract(worker.position()));
                if (box.maxY <= player.level().getMaxBuildHeight() && player.level().noCollision(worker, box)) { stand = candidate; break; }
            }
            if (stand == null) continue;
            if (restored) {
                if (slot >= MinionsConfig.MINIONS_PER_PLAYER.get()) continue;
                var saved = master.getInventory(slot).copy();
                master.removeMinion(id); master.addSpecificMinion(worker.getUUID(), slot);
                worker.moveTo(Vec3.atBottomCenterOf(stand)); worker.restoreWorkerState(saved);
                if (!player.level().addFreshEntity(worker)) { master.removeMinion(worker.getUUID()); master.addSpecificMinion(id, slot); continue; }
            } else if (worker.level() != player.level()) {
                var entity = worker.changeDimension(new DimensionTransition(player.serverLevel(), Vec3.atBottomCenterOf(stand), Vec3.ZERO,
                        worker.getYRot(), worker.getXRot(), DimensionTransition.DO_NOTHING));
                if (!(entity instanceof Minion transferred)) continue;
                worker = transferred;
            }
            worker.getNavigation().stop(); worker.blink(stand); worker.saveWorkerState(master); moved++;
        }
        if (moved > 0) { master.setOrder(new MoveOrder(base)); WorkerSounds.play(player, "randomorder"); }
        return moved;
    }
    public static void forEach(ServerPlayer player, IMasterCapability master, Consumer<Minion> action) {
        for (var id : master.getMinions()) if (id != null && player.serverLevel().getEntity(id) instanceof Minion worker && player.getUUID().equals(worker.getOwnerUUID())) action.accept(worker);
    }
    public static void release(ServerPlayer player, IMasterCapability master) {
        master.setOrder(null);
        for (var id : new ArrayList<>(master.getMinions())) if (id != null) {
            for (ServerLevel level : player.getServer().getAllLevels()) if (level.getEntity(id) instanceof Minion worker && player.getUUID().equals(worker.getOwnerUUID())) {
                worker.saveWorkerState(master); worker.ejectPassengers(); master.removeMinion(id); worker.discard(); break;
            }
            master.removeMinion(id);
        }
    }
}
