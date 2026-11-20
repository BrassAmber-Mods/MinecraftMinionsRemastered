package io.github.jodlodi.minions.minion;

import io.github.jodlodi.minions.work.WorkerMovement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinionNavigation extends GroundPathNavigation {
    public Vec3 lastPos = Vec3.ZERO;
    private int stationaryTicks;

    public MinionNavigation(Minion minion, Level level) {
        super(minion, level);
    }

    protected Minion getMinion() {
        return (Minion)this.mob;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new MinionNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    public void tick() {
        Minion minion = this.getMinion();
        if (minion.sittingOrRiding()) return;
        Path path = this.getPath();
        if (path != null && !this.isDone() && !minion.getBlinking() && (minion.isEnactingOrder() || minion.isHauling())) {
            if (this.hasStoppedMoving() && minion.canBlink()) {
                Vec3 stand = WorkerMovement.findStandNear(minion, path.getTarget());
                if (stand != null && minion.distanceToSqr(stand) > 0.01D) {
                    minion.blink(stand); this.stop(); this.stationaryTicks = 0;
                }
            }
        } else {
            this.stationaryTicks = 0;
            this.lastPos = minion.position();
        }
        if (!this.getMinion().getBlinking()) super.tick();
    }

    private boolean hasStoppedMoving() {
        Vec3 pos = this.mob.position();
        if (pos.distanceToSqr(this.lastPos) > 0.01D) {
            this.lastPos = pos; this.stationaryTicks = 0; return false;
        }
        return ++this.stationaryTicks >= 40;
    }
}
