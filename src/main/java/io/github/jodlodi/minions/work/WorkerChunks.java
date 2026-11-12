package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinionsConfig;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Transient tickets expire after five seconds and never survive the owning server session. */
public final class WorkerChunks {
    private static final TicketType<UUID> TICKET = TicketType.create("minions_worker", Comparator.comparing(UUID::toString), 100);
    private record Key(ResourceKey<Level> dimension, long chunk) {}
    private static final Map<UUID, LinkedHashMap<Key, Long>> ACTIVE = new HashMap<>();

    private WorkerChunks() {}

    public static void keepLoaded(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!MinionsConfig.WORKER_CHUNK_LOADING.get() || !player.isAlive() || player.serverLevel() != level
                || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) return;
        var tickets = ACTIVE.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
        long now = level.getGameTime();
        tickets.entrySet().removeIf(entry -> now - entry.getValue() >= 100);
        Key key = new Key(level.dimension(), new ChunkPos(pos).toLong());
        Long refreshed = tickets.get(key);
        if (refreshed != null && now - refreshed < 20) return;
        tickets.remove(key);
        tickets.put(key, now);
        while (tickets.size() > MinionsConfig.MAX_WORKER_CHUNKS.get()) {
            Key oldest = tickets.keySet().iterator().next();
            tickets.remove(oldest);
            remove(player.getServer(), player.getUUID(), oldest);
        }
        level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(pos), 2, player.getUUID(), true);
    }

    private static void remove(MinecraftServer server, UUID owner, Key key) {
        ServerLevel level = server.getLevel(key.dimension);
        if (level != null) level.getChunkSource().removeRegionTicket(TICKET, new ChunkPos(key.chunk), 2, owner, true);
    }

    public static void release(ServerPlayer player) {
        var tickets = ACTIVE.remove(player.getUUID());
        if (tickets != null) for (Key key : tickets.keySet()) remove(player.getServer(), player.getUUID(), key);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
