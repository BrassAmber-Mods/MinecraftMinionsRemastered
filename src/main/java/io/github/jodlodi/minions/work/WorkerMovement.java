package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

public final class WorkerMovement {
    private static final double WORK_REACH_SQUARED = 12.25D;

    private WorkerMovement() {}

    public static boolean canReach(Minion worker, BlockPos target) {
        return canReachFrom(worker, worker.getEyePosition(), target);
    }

    private static boolean canReachFrom(Minion worker, Vec3 eyes, BlockPos target) {
        Vec3 center = Vec3.atCenterOf(target);
        if (eyes.distanceToSqr(center) > WORK_REACH_SQUARED) return false;
        var sight = worker.level().clip(new ClipContext(eyes, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, worker));
        return sight.getType() == HitResult.Type.MISS || sight.getBlockPos().equals(target);
    }

    public static boolean canStand(Minion worker, Vec3 feet) {
        var level = worker.level();
        var box = MinUtil.getRidingAABB(worker).move(feet.subtract(worker.position()));
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box)
                || !level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 0.05D, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                || level.containsAnyLiquid(box) || !level.noCollision(worker, box)) return false;
        return level.getBlockCollisions(worker, box.move(0, -0.05D, 0)).iterator().hasNext();
    }

    public static boolean canWorkFrom(Minion worker, Vec3 feet, BlockPos target) {
        return canStand(worker, feet) && canReachFrom(worker, feet.add(0, worker.getEyeHeight(), 0), target);
    }

    @Nullable
    public static Vec3 findWorkPosition(Minion worker, BlockPos target) {
        return findPosition(worker, target, true);
    }

    @Nullable
    public static Vec3 findStandNear(Minion worker, BlockPos target) {
        return findPosition(worker, target, false);
    }

    @Nullable
    private static Vec3 findPosition(Minion worker, BlockPos target, boolean requireReach) {
        var level = worker.level();
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = -2; y <= 2; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos support = target.offset(x, y - 1, z);
            if (!level.isInWorldBounds(support) || !level.hasChunkAt(support)) continue;
            var shape = level.getBlockState(support).getCollisionShape(level, support, CollisionContext.of(worker));
            if (shape.isEmpty()) continue;
            Vec3 feet = new Vec3(support.getX() + 0.5D, support.getY() + shape.max(Direction.Axis.Y), support.getZ() + 0.5D);
            double distance = requireReach ? worker.distanceToSqr(feet)
                    : feet.distanceToSqr(Vec3.atBottomCenterOf(target)) * 16 + worker.distanceToSqr(feet) * 0.05D;
            if (distance >= bestDistance || !canStand(worker, feet)
                    || requireReach && !canReachFrom(worker, feet.add(0, worker.getEyeHeight(), 0), target)) continue;
            best = feet;
            bestDistance = distance;
        }
        return best;
    }
}
