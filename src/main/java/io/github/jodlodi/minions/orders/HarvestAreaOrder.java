package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinUtil;
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

import java.util.function.Predicate;

public abstract class HarvestAreaOrder extends AbstractWorkOrder {
    protected final BlockPos minPos;
    protected final BlockPos maxPos;
    protected final Block predicate;
    private int cursor;

    protected HarvestAreaOrder(BlockPos min, BlockPos max, Block predicate) {
        this.minPos = min.immutable();
        this.maxPos = max.immutable();
        this.predicate = predicate;
        this.validate();
    }

    protected HarvestAreaOrder(CompoundTag tag) {
        super(tag);
        this.minPos = BlockPos.of(tag.getLong("min"));
        this.maxPos = BlockPos.of(tag.getLong("max"));
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("predicate"));
        this.predicate = id != null ? BuiltInRegistries.BLOCK.getOptional(id).orElse(null) : null;
        this.cursor = Math.max(0, tag.getInt("cursor"));
        this.validate();
    }

    private void validate() {
        long width = (long)this.maxPos.getX() - this.minPos.getX() + 1;
        long depth = (long)this.maxPos.getZ() - this.minPos.getZ() + 1;
        long height = (long)this.maxPos.getY() - this.minPos.getY() + 1;
        if (width < 1 || width > 32 || depth < 1 || depth > 32 || height < 1 || height > 65) throw new IllegalArgumentException("Invalid harvesting bounds");
    }

    protected abstract boolean matches(BlockState state);
    protected boolean advanceDiscovery(ServerPlayer owner, ServerLevel level) { return true; }
    protected boolean scanMatch(ServerPlayer owner, ServerLevel level, BlockPos pos) { return this.scanBlock(owner, level, pos, null); }
    protected CompoundTag saveHarvest(CompoundTag tag) { return tag; }
    public final Predicate<BlockState> getPredicate() {
        return state -> this.matches(state) && (this.predicate == null || state.is(this.predicate));
    }

    @Override
    public final CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("min", this.minPos.asLong());
        tag.putLong("max", this.maxPos.asLong());
        tag.putInt("cursor", this.cursor);
        if (this.predicate != null) tag.putString("predicate", BuiltInRegistries.BLOCK.getKey(this.predicate).toString());
        return this.saveWork(this.saveHarvest(tag));
    }

    private static int centered(int index, int size) {
        return size / 2 + (index % 2 == 0 ? index / 2 : -(index + 1) / 2);
    }

    @Override
    protected final boolean scanNext(ServerPlayer owner, ServerLevel level) {
        if (!this.advanceDiscovery(owner, level)) return false;
        int width = this.maxPos.getX() - this.minPos.getX() + 1;
        int depth = this.maxPos.getZ() - this.minPos.getZ() + 1;
        int height = this.maxPos.getY() - this.minPos.getY() + 1;
        if (this.cursor >= width * depth * height) { this.finishSource(); return true; }
        BlockPos pos = this.minPos.offset(centered(this.cursor % width, width), centered(this.cursor / (width * depth), height), centered((this.cursor / width) % depth, depth));
        if (level.isInWorldBounds(pos)) {
            if (!this.loaded(owner, level, pos)) return false;
            if (this.getPredicate().test(level.getBlockState(pos)) && !this.scanMatch(owner, level, pos)) return false;
        }
        this.cursor++;
        return true;
    }

    @Override
    protected final void preview(Player player, Level level) {
        if (player.tickCount % 4 == 0) MinUtil.particlesAroundTag(player.getRandom(), level, this.minPos, this.maxPos, this.getPredicate());
    }
}
