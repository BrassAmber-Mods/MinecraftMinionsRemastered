package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reconcile offline recalls with player attachments on their next login, without editing player files. */
public final class WorkerRecallLedger extends SavedData {
    private record Entry(UUID owner, CompoundTag state) {}
    private final Map<UUID, Entry> recalled = new HashMap<>();
    public static WorkerRecallLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(WorkerRecallLedger::new, WorkerRecallLedger::load), "minions_recalls");
    }
    private static WorkerRecallLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        WorkerRecallLedger data = new WorkerRecallLedger(); ListTag entries = tag.getList("Workers", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.hasUUID("Worker") && entry.hasUUID("Owner")) data.recalled.put(entry.getUUID("Worker"), new Entry(entry.getUUID("Owner"), entry.getCompound("State")));
        }
        return data;
    }
    public void record(Minion worker) {
        if (worker.getOwnerUUID() == null) return;
        this.recalled.put(worker.getUUID(), new Entry(worker.getOwnerUUID(), worker.recallState())); this.setDirty();
    }
    public void restore(ServerPlayer player, IMasterCapability master) {
        var entries = this.recalled.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next(); if (!entry.getValue().owner().equals(player.getUUID())) continue;
            int slot = master.getMinions().indexOf(entry.getKey());
            if (slot >= 0) { master.setInventory(slot, entry.getValue().state().copy()); master.removeMinion(entry.getKey()); }
            entries.remove(); this.setDirty();
        }
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag entries = new ListTag(); this.recalled.forEach((id, data) -> {
            CompoundTag entry = new CompoundTag(); entry.putUUID("Worker", id); entry.putUUID("Owner", data.owner()); entry.put("State", data.state().copy()); entries.add(entry);
        }); tag.put("Workers", entries); return tag;
    }
}
