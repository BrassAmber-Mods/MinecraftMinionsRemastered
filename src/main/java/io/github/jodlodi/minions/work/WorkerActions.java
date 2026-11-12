package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.util.TriState;

public final class WorkerActions {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    private WorkerActions() {}

    public static boolean isOre(BlockState state) {
        return state.is(ORES);
    }

    public static boolean mayChange(ServerPlayer owner, ServerLevel level, BlockPos pos) {
        return CommandValidation.canCommand(owner) && owner.serverLevel() == level && level.isInWorldBounds(pos)
                && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(owner, pos);
    }

    public static boolean canHarvest(Minion worker, BlockPos pos) {
        if (!(worker.getOwner() instanceof ServerPlayer owner) || !(worker.level() instanceof ServerLevel level) || !mayChange(owner, level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        ItemStack tool = MinUtil.bestToolForBlock(state);
        return !state.hasBlockEntity() && !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0
                && state.canEntityDestroy(level, pos, worker) && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
    }

    public static boolean harvest(Minion worker, BlockPos pos) {
        if (!canHarvest(worker, pos)) return false;
        WorkerPlayer actor = worker.workerPlayer();
        BlockState state = worker.level().getBlockState(pos);
        actor.moveTo(worker.getX(), worker.getY(), worker.getZ(), worker.getYRot(), worker.getXRot());
        actor.setItemInHand(InteractionHand.MAIN_HAND, MinUtil.bestToolForBlock(state));
        try {
            if (!state.canHarvestBlock(worker.level(), pos, actor)) return false;
            return WorkerEvents.harvest(actor, () -> actor.gameMode.destroyBlock(pos))
                    && !worker.level().getBlockState(pos).is(state.getBlock());
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    public static boolean place(Minion worker, BlockPos pos, BlockState desired) {
        if (!(worker.getOwner() instanceof ServerPlayer owner) || !(worker.level() instanceof ServerLevel level) || !mayChange(owner, level, pos)) return false;
        BlockState old = level.getBlockState(pos);
        if (old.equals(desired)) return true;
        if (!old.canBeReplaced() || old.hasBlockEntity() || !desired.canSurvive(level, pos)) return false;
        WorkerPlayer actor = worker.workerPlayer();
        actor.moveTo(worker.getX(), worker.getY(), worker.getZ(), worker.getYRot(), worker.getXRot());
        ItemStack material = new ItemStack(desired.getBlock());
        actor.setItemInHand(InteractionHand.MAIN_HAND, material);
        // Click the replaceable target itself so an air/fluid support cannot redirect placement below it.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try {
            var access = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(actor, InteractionHand.MAIN_HAND, pos, hit));
            if (access.isCanceled() || access.getUseItem() == TriState.FALSE) return false;
            material.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit));
            if (!level.getBlockState(pos).is(desired.getBlock())) return false;
            if (!level.getBlockState(pos).equals(desired)) level.setBlock(pos, desired, 3);
            return true;
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    public static boolean sealFluids(Minion worker, BlockPos pos) {
        ServerLevel level = (ServerLevel)worker.level();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.hasChunkAt(neighbor)) return false;
            BlockState state = level.getBlockState(neighbor);
            if (!state.getFluidState().isEmpty() && !place(worker, neighbor, Blocks.BLACKSTONE.defaultBlockState())) return false;
        }
        return true;
    }

    public static IItemHandler container(Minion worker, BlockPos pos) {
        if (!(worker.getOwner() instanceof ServerPlayer owner) || !(worker.level() instanceof ServerLevel level) || !mayChange(owner, level, pos)) return null;
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container && !container.canOpen(owner)) return null;
        WorkerPlayer actor = worker.workerPlayer();
        actor.moveTo(worker.getX(), worker.getY(), worker.getZ(), worker.getYRot(), worker.getXRot());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var access = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(actor, InteractionHand.MAIN_HAND, pos, hit));
        if (access.isCanceled() || access.getUseBlock() == TriState.FALSE) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    public static IItemHandler container(ServerPlayer owner, BlockPos pos) {
        if (!CommandValidation.block(owner, pos)) return null;
        if (owner.level().getBlockEntity(pos) instanceof BaseContainerBlockEntity container && !container.canOpen(owner)) return null;
        var access = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(owner, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        return access.isCanceled() || access.getUseBlock() == TriState.FALSE ? null : owner.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    public static IItemHandler container(Minion worker, Entity entity) {
        if (!(worker.getOwner() instanceof ServerPlayer owner) || entity == null || entity.level() != worker.level()
                || !entity.isAlive() || !CommandValidation.friendly(owner, entity)) return null;
        var actor = worker.workerPlayer();
        actor.moveTo(worker.getX(), worker.getY(), worker.getZ(), worker.getYRot(), worker.getXRot());
        var access = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(actor, InteractionHand.MAIN_HAND, entity));
        return access.isCanceled() ? null : entity.getCapability(Capabilities.ItemHandler.ENTITY);
    }

    public static IItemHandler container(ServerPlayer owner, Entity entity) {
        if (!CommandValidation.friendly(owner, entity)) return null;
        return entity.getCapability(Capabilities.ItemHandler.ENTITY);
    }
}
