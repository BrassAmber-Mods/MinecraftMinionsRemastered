package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.minion.goals.EnactOrderGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/** One independent 80-block, two-high gallery per assigned worker slot. */
public final class StripMineOrder extends AbstractOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("strip_mine_order");
    private final Map<Integer, MineAheadOrder> galleries = new LinkedHashMap<>();
    public StripMineOrder() {}
    public StripMineOrder(CompoundTag tag) {
        ListTag entries = tag.getList("Galleries", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(16, entries.size()); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < 16) this.galleries.put(slot, new MineAheadOrder(entry.getCompound("Order")));
        }
    }
    public boolean hasSlot(int slot) { return this.galleries.containsKey(slot); }
    public void add(int slot, BlockPos start, Direction direction) {
        this.galleries.put(slot, new MineAheadOrder(start, start.above(), direction, Math.min(80, MinionsConfig.MAX_TUNNEL_LENGTH.get())));
    }
    private MineAheadOrder gallery(EnactOrderGoal goal) { return this.galleries.get(goal.getMinion().getMinionID(goal.getMasterCapability())); }
    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean goalCanUse(EnactOrderGoal goal) { var gallery = this.gallery(goal); return gallery != null && gallery.goalCanUse(goal); }
    @Override public boolean goalCanContinueToUse(EnactOrderGoal goal) { return this.goalCanUse(goal); }
    @Override public void goalStart(EnactOrderGoal goal) { var gallery = this.gallery(goal); if (gallery != null) gallery.goalStart(goal); }
    @Override public void goalStop(EnactOrderGoal goal) {
        var gallery = this.gallery(goal); if (gallery != null) gallery.goalStop(goal);
        goal.getMinion().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
    @Override public void goalTick(EnactOrderGoal goal) { var gallery = this.gallery(goal); if (gallery != null) gallery.goalTick(goal); }
    @Override public void interrupt(ServerLevel level) { this.galleries.values().forEach(gallery -> gallery.interrupt(level)); }
    @Override public void tick(IMasterCapability master, Player player, Level level) {
        for (var entry : this.galleries.entrySet()) {
            MineAheadOrder gallery = entry.getValue();
            gallery.tickWithBudget(master, player, level, Math.max(1, MinionsConfig.WORK_SCAN_BUDGET.get() / Math.max(1, this.galleries.size())));
            if (!level.isClientSide && gallery.isFinished() && entry.getKey() < master.getMinions().size()) {
                var id = master.getMinions().get(entry.getKey());
                if (id != null && level instanceof ServerLevel server && server.getEntity(id) instanceof Minion worker) worker.requestDelivery();
            }
        }
        if (!level.isClientSide) {
            this.galleries.values().removeIf(AbstractWorkOrder::isFinished);
            if (this.galleries.isEmpty()) master.setOrder(null);
        }
    }
    private CompoundTag save(boolean client) {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        this.galleries.forEach((slot, gallery) -> {
            CompoundTag entry = new CompoundTag(); entry.putInt("Slot", slot);
            entry.put("Order", client ? gallery.serializeForClient() : gallery.serialize()); entries.add(entry);
        });
        tag.put("Galleries", entries); return tag;
    }
    @Override public CompoundTag serialize() { return this.save(false); }
    @Override public CompoundTag serializeForClient() { return this.save(true); }
}
