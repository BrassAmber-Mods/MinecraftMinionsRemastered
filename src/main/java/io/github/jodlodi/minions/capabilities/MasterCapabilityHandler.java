package io.github.jodlodi.minions.capabilities;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.network.MasterPacket;
import io.github.jodlodi.minions.orders.AbstractOrder;
import io.github.jodlodi.minions.registry.OrderRegistry;
import io.github.jodlodi.minions.registry.PacketRegistry;
import io.github.jodlodi.minions.work.WorkerChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MasterCapabilityHandler implements IMasterCapability {
    public static final int MAX_MINIONS = 16;
    private final List<UUID> minions = new ArrayList<>();
    private final List<CompoundTag> inventories = new ArrayList<>();
    private Player player;
    private AbstractOrder order;
    private ResourceLocation orderDimension;
    private boolean synced;
    private boolean paused;
    private BlockPos containerBlock;
    private UUID containerEntity;
    private ResourceLocation containerDimension;
    private BlockPos containerEntityPosition;

    public MasterCapabilityHandler(Player player) {
        this.player = player;
        for (int i = 0; i < MAX_MINIONS; i++) { this.minions.add(null); this.inventories.add(new CompoundTag()); }
    }

    @Override
    public void setEntity(Player player) { this.player = player; }
    @Override
    public Player getPlayer() { return this.player; }

    @Override
    public void tick() {
        if (this.player instanceof ServerPlayer owner) {
            if (!this.synced) { this.sendUpdatePacket(); this.synced = true; }
            if (owner.tickCount % 20 == 0) this.refreshWorkers(owner);
        }
        if (this.order != null) this.order.tick(this, this.player, this.player.level());
    }

    private void refreshWorkers(ServerPlayer owner) {
        for (int i = 0; i < MAX_MINIONS; i++) {
            UUID id = this.minions.get(i);
            if (id == null) continue;
            CompoundTag state = this.inventories.get(i);
            ResourceLocation dimension = ResourceLocation.tryParse(state.getString("WorkerDimension"));
            ServerLevel level = dimension != null ? owner.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension)) : owner.serverLevel();
            if (level == null) continue;
            if (level.getEntity(id) instanceof Minion worker) {
                worker.saveWorkerState(this);
                if ((!this.paused && this.order != null && this.isOrderIn(level)) || worker.hasCargo()) WorkerChunks.keepLoaded(owner, level, worker.blockPosition());
            } else if (state.contains("WorkerPosition") && ((!this.paused && this.order != null && this.isOrderIn(level)) || state.contains("Cargo"))) {
                WorkerChunks.keepLoaded(owner, level, BlockPos.of(state.getLong("WorkerPosition")));
            }
        }
        if (this.containerEntity != null) {
            for (ServerLevel level : owner.getServer().getAllLevels()) {
                Entity target = level.getEntity(this.containerEntity);
                if (target != null) {
                    this.containerEntityPosition = target.blockPosition();
                    this.containerDimension = level.dimension().location();
                    break;
                }
            }
        }
    }

    @Override
    public List<UUID> getMinions() { return Collections.unmodifiableList(this.minions); }
    @Override
    public boolean isMinion(UUID uuid) { return uuid != null && this.minions.contains(uuid); }
    @Override
    public int minionCount() { return (int)this.minions.stream().filter(Objects::nonNull).count(); }

    @Override
    public void addMinion(UUID uuid) {
        if (uuid == null || this.isMinion(uuid)) return;
        for (int i = 0; i < MAX_MINIONS; i++) if (this.minions.get(i) == null) { this.addSpecificMinion(uuid, i); return; }
    }

    @Override
    public void addSpecificMinion(UUID uuid, int id) {
        if (id < 0 || id >= MinionsConfig.MINIONS_PER_PLAYER.get() || uuid == null || this.minions.get(id) != null || this.isMinion(uuid)) return;
        this.minions.set(id, uuid);
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }

    @Override
    public void removeMinion(UUID uuid) {
        int id = this.minions.indexOf(uuid);
        if (id < 0 || uuid == null) return;
        this.minions.set(id, null);
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }

    @Override
    public void releaseMinions(int count) {
        for (int i = MAX_MINIONS - 1; i >= 0 && count > 0; i--) if (this.minions.get(i) != null) { this.minions.set(i, null); count--; }
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }

    @Override
    public void setInventory(int id, CompoundTag tag) { if (id >= 0 && id < MAX_MINIONS) this.inventories.set(id, tag); }
    @Override
    public CompoundTag getInventory(int id) { return id >= 0 && id < MAX_MINIONS ? this.inventories.get(id) : new CompoundTag(); }

    @Override
    public void setOrder(@Nullable AbstractOrder order) {
        if (this.order != null && this.player instanceof ServerPlayer owner && this.orderDimension != null) {
            ServerLevel level = owner.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.orderDimension));
            if (level != null) this.order.interrupt(level);
        }
        this.order = order;
        this.orderDimension = order != null ? this.player.level().dimension().location() : null;
        this.paused = false;
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }

    @Override
    public AbstractOrder getOrder() { return this.order; }
    @Override
    public boolean isOrderIn(Level level) { return this.order != null && level.dimension().location().equals(this.orderDimension); }
    @Override
    public void setContainerBlock(@Nullable BlockPos pos) {
        this.containerBlock = pos != null ? pos.immutable() : null;
        if (pos != null) this.containerDimension = this.player.level().dimension().location();
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }
    @Override
    public BlockPos getContainerBlock() { return this.containerBlock; }
    @Override
    public void setContainerEntity(@Nullable UUID uuid) {
        this.containerEntity = uuid;
        this.containerEntityPosition = null;
        if (uuid != null) {
            this.containerDimension = this.player.level().dimension().location();
            if (this.player.level() instanceof ServerLevel level && level.getEntity(uuid) instanceof Entity entity) this.containerEntityPosition = entity.blockPosition();
        }
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }
    @Override
    public UUID getContainerEntity() { return this.containerEntity != null ? this.containerEntity : this.player.getUUID(); }
    @Override
    public ResourceLocation getContainerDimension() {
        return (this.containerBlock != null || this.containerEntity != null) && this.containerDimension != null
                ? this.containerDimension : this.player.level().dimension().location();
    }
    @Override
    public BlockPos getContainerEntityPosition() { return this.containerEntityPosition; }

    @Override
    public void finalizeMinion(Minion minion) {
        minion.setOwnerUUID(this.player.getUUID());
        minion.restoreWorkerState(this.getInventory(this.minions.indexOf(minion.getUUID())).copy());
        minion.saveWorkerState(this);
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!this.player.level().isClientSide) this.sendUpdatePacket();
    }
    @Override
    public boolean isPaused() { return this.paused; }
    @Override
    public void sendUpdatePacket() { PacketRegistry.sendToPlayer(this.player, new MasterPacket(this.player, this)); }

    @Override
    public CompoundTag serializeForClient() {
        CompoundTag tag = this.serializeNBT();
        for (int i = 0; i < MAX_MINIONS; i++) {
            CompoundTag worker = tag.getCompound("MinionInventory" + i);
            worker.remove("Cargo");
            worker.remove("WorkerPosition");
        }
        if (this.order != null) tag.put("Order", this.order.serializeForClient());
        return tag;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < MAX_MINIONS; i++) {
            if (this.minions.get(i) != null) tag.putUUID("MinionUUID" + i, this.minions.get(i));
            tag.put("MinionInventory" + i, this.inventories.get(i).copy());
        }
        if (this.order != null) {
            tag.putString("OrderID", this.order.getID().toString());
            tag.put("Order", this.order.serialize());
            if (this.orderDimension != null) tag.putString("OrderDimension", this.orderDimension.toString());
        }
        if (this.containerBlock != null) tag.putLong("ChestPos", this.containerBlock.asLong());
        if (this.containerEntity != null) tag.putUUID("ContainerUUID", this.containerEntity);
        if (this.containerDimension != null) tag.putString("StorageDimension", this.containerDimension.toString());
        if (this.containerEntityPosition != null) tag.putLong("StorageEntityPosition", this.containerEntityPosition.asLong());
        tag.putBoolean("Paused", this.paused);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < MAX_MINIONS; i++) {
            this.minions.set(i, tag.hasUUID("MinionUUID" + i) ? tag.getUUID("MinionUUID" + i) : null);
            this.inventories.set(i, tag.getCompound("MinionInventory" + i).copy());
        }
        this.order = null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("OrderID"));
        if (id != null && OrderRegistry.ORDERS.containsKey(id)) {
            try { this.order = OrderRegistry.ORDERS.get(id).apply(tag.getCompound("Order")); }
            catch (IllegalArgumentException exception) { MinionsRemastered.LOGGER.warn("Ignoring invalid saved minion order {}", id, exception); }
        }
        this.orderDimension = ResourceLocation.tryParse(tag.getString("OrderDimension"));
        if (this.order != null && this.orderDimension == null) this.orderDimension = this.player.level().dimension().location();
        this.containerBlock = tag.contains("ChestPos") ? BlockPos.of(tag.getLong("ChestPos")) : null;
        this.containerEntity = tag.hasUUID("ContainerUUID") ? tag.getUUID("ContainerUUID") : null;
        this.containerDimension = ResourceLocation.tryParse(tag.getString("StorageDimension"));
        this.containerEntityPosition = tag.contains("StorageEntityPosition") ? BlockPos.of(tag.getLong("StorageEntityPosition")) : null;
        this.paused = tag.getBoolean("Paused");
    }
}
