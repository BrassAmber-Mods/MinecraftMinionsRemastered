package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.GameplayRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Explicitly selected valuable material follows only the same block, as in the original. */
public final class VeinOrder extends AbstractWorkOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("vein_order");
    private final BlockPos origin;
    private final Block block;
    private boolean queued;
    public VeinOrder(BlockPos origin, Block block) { this.origin = origin.immutable(); this.block = block; }
    public VeinOrder(CompoundTag tag) {
        super(tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("Block")));
        this.queued = tag.getBoolean("Queued");
    }
    @Override public ResourceLocation getID() { return ID; }
    @Override protected boolean followsVeins() { return true; }
    @Override protected boolean expandVeins() { return true; }
    @Override protected boolean isVeinBlock(BlockState state) { return state.is(this.block) && GameplayRules.isValuable(state); }
    @Override protected boolean scanNext(ServerPlayer owner, ServerLevel level) {
        if (!this.queued) {
            if (!this.loaded(owner, level, this.origin)) return false;
            if (level.getBlockState(this.origin).is(this.block) && !this.scanBlock(owner, level, this.origin, null)) return false;
            this.queued = true;
        }
        this.finishSource();
        return true;
    }
    @Override protected void preview(Player player, Level level) {}
    @Override public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Origin", this.origin.asLong());
        tag.putString("Block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
        tag.putBoolean("Queued", this.queued);
        return this.saveWork(tag);
    }
}
