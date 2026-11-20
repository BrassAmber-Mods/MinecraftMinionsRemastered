package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = MinionsRemastered.MODID)
public final class WorkerEvents {
    private record Harvest(WorkerPlayer actor, List<BlockDropsEvent> events) {}
    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();

    private WorkerEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectHarvest(BlockDropsEvent event) {
        Harvest harvest = HARVEST.get();
        if (harvest != null && event.getBreaker() == harvest.actor()) harvest.events().add(event);
    }

    /** Collect only drops that survived every loot/drop/spawn hook and actually entered the world. */
    public static boolean harvest(WorkerPlayer actor, BooleanSupplier action) {
        Harvest previous = HARVEST.get();
        Harvest harvest = new Harvest(actor, new ArrayList<>());
        HARVEST.set(harvest);
        boolean result;
        try { result = action.getAsBoolean(); }
        finally {
            if (previous == null) HARVEST.remove();
            else HARVEST.set(previous);
        }
        var worker = actor.worker();
        if (!worker.isRemoved()) {
            for (BlockDropsEvent event : harvest.events()) {
                if (event.isCanceled()) continue;
                for (ItemEntity drop : event.getDrops()) {
                    if (!drop.isAddedToLevel() || drop.isRemoved() || drop.level() != worker.level()) continue;
                    ItemStack remainder = worker.insertCargo(drop.getItem().copy());
                    if (remainder.isEmpty()) drop.discard();
                    else {
                        drop.setItem(remainder);
                        drop.setTarget(worker.getOwnerUUID());
                    }
                }
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CommonRegistry.getMaster(player).ifPresent(master -> {
                for (var level : player.getServer().getAllLevels()) for (var id : master.getMinions())
                    if (id != null && level.getEntity(id) instanceof Minion worker) worker.saveWorkerState(master);
            });
            WorkerChunks.release(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CommonRegistry.getMaster(player)
                .ifPresent(master -> WorkerRecallLedger.get(player.getServer()).restore(player, master));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { WorkerChunks.clear(); }
}
