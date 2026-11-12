package io.github.jodlodi.minions.minion;

import io.github.jodlodi.minions.MinUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MinionNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        Entity passenger = mob.getFirstPassenger();
        if (passenger != null) {
            float width = Math.max(mob.getBbWidth(), passenger.getBbWidth());
            double height = mob.getPassengerRidingPosition(passenger).y - mob.getY()
                    - passenger.getVehicleAttachmentPoint(mob).y + passenger.getBbHeight();
            this.entityWidth = Mth.floor(width + 1.0F);
            this.entityDepth = this.entityWidth;
            this.entityHeight = Mth.floor(Math.max(mob.getBbHeight(), height) + 1.0D);
        }
    }

    @Override
    public boolean canReachWithoutCollision(Node node) {
        AABB box = MinUtil.getRidingAABB(this.mob);
        Vec3 movement = new Vec3(node.x - this.mob.getX() + box.getXsize() / 2.0D,
                node.y - this.mob.getY() + box.getYsize() / 2.0D,
                node.z - this.mob.getZ() + box.getZsize() / 2.0D);
        int steps = Math.max(1, Mth.ceil(movement.length() / box.getSize()));
        Vec3 step = movement.scale(1.0D / steps);
        for (int i = 0; i < steps; i++) {
            box = box.move(step);
            if (this.hasCollisions(box)) return false;
        }
        return true;
    }
}
