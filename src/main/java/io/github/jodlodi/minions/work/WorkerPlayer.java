package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Runs vanilla harvesting/placement and protection events under the master's identity. */
public final class WorkerPlayer extends FakePlayer {
    private final Minion worker;

    public WorkerPlayer(ServerLevel level, ServerPlayer owner, Minion worker) {
        super(level, owner.getGameProfile());
        this.worker = worker;
        this.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
    }

    public Minion worker() {
        return this.worker;
    }
}
