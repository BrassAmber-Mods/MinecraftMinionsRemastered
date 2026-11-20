package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class CommandValidation {
    private CommandValidation() {}

    public static boolean canCommand(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE;
    }

    public static boolean block(ServerPlayer player, BlockPos pos) {
        return player.serverLevel().isInWorldBounds(pos) && player.serverLevel().getWorldBorder().isWithinBounds(pos)
                && player.serverLevel().hasChunkAt(pos) && player.canInteractWithBlock(pos, 1.0D)
                && player.serverLevel().mayInteract(player, pos);
    }

    public static boolean entity(ServerPlayer player, Entity entity) {
        return entity != null && entity.isAlive() && entity.level() == player.level() && player.canInteractWithEntity(entity, 1.0D);
    }

    public static boolean owned(ServerPlayer player, IMasterCapability master, Minion minion) {
        return master.isMinion(minion.getUUID()) && player.getUUID().equals(minion.getOwnerUUID());
    }

    public static boolean friendly(ServerPlayer player, Entity target) {
        return !(target instanceof Enemy) && (!(target instanceof NeutralMob neutral) || !neutral.isAngryAt(player))
                && (!(target instanceof OwnableEntity ownable) || ownable.getOwnerUUID() == null || player.getUUID().equals(ownable.getOwnerUUID()));
    }

    private static boolean bounds(ServerPlayer player, BlockPos min, BlockPos max, int maxWidth, int maxHeight) {
        long width = (long)max.getX() - min.getX() + 1;
        long height = (long)max.getY() - min.getY() + 1;
        long depth = (long)max.getZ() - min.getZ() + 1;
        if (width < 1 || width > maxWidth || depth < 1 || depth > maxWidth || height < 1 || height > maxHeight) return false;
        Vec3 center = new Vec3(((long)min.getX() + max.getX()) / 2.0D + 0.5D,
                ((long)min.getY() + max.getY()) / 2.0D + 0.5D, ((long)min.getZ() + max.getZ()) / 2.0D + 0.5D);
        BlockPos anchor = BlockPos.containing(center);
        return player.distanceToSqr(center) <= 64.0D && player.serverLevel().hasChunkAt(anchor)
                && player.serverLevel().getWorldBorder().isWithinBounds(min) && player.serverLevel().getWorldBorder().isWithinBounds(max)
                && min.getY() >= player.level().getMinBuildHeight() - 32 && max.getY() < player.level().getMaxBuildHeight() + 32
                && player.serverLevel().mayInteract(player, anchor);
    }

    public static boolean shaft(ServerPlayer player, BlockPos min, BlockPos max, int startDir, int stairs) {
        return bounds(player, min, max, 31, 1) && GameplayRules.isHoleWidth((long)max.getX() - min.getX() + 1, 31)
                && max.getX() - min.getX() == max.getZ() - min.getZ()
                && startDir >= 0 && startDir < 4 && stairs >= 0 && stairs <= 2;
    }

    public static boolean tunnel(ServerPlayer player, BlockPos min, BlockPos max, int direction, int length) {
        if (direction < 0 || direction >= Direction.values().length || length < 1 || length > MinionsConfig.MAX_TUNNEL_LENGTH.get()) return false;
        Direction facing = Direction.values()[direction];
        if (!facing.getAxis().isHorizontal() || !bounds(player, min, max, 5, 5) || min.get(facing.getAxis()) != max.get(facing.getAxis())) return false;
        int width = facing.getAxis() == Direction.Axis.X ? max.getZ() - min.getZ() + 1 : max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;
        return (width == 1 && height == 2) || (width == 3 && height == 3) || (width == 5 && height == 5);
    }

    public static boolean harvest(ServerPlayer player, BlockPos min, BlockPos max, Block predicate, TagKey<Block> tag) {
        return bounds(player, min, max, 32, 65) && (predicate == null || predicate.defaultBlockState().is(tag)
                || tag.equals(BlockTags.LOGS) && GameplayRules.isTree(predicate.defaultBlockState()));
    }
}
