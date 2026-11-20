package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.work.GameplayRules;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Custom cubic footprint, with an interior access stairwell followed by top-down layers. */
public final class ExcavateOrder extends AbstractWorkOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("excavate_order");
    private final BlockPos min;
    private final BlockPos max;
    private final BlockPos anchor;
    private int stair;
    private int stairTask;
    private int cursor;
    private int layer;
    public ExcavateOrder(BlockPos min, BlockPos max) { this(min, max, min); }
    public ExcavateOrder(BlockPos min, BlockPos max, BlockPos anchor) { this.min = min.immutable(); this.max = max.immutable(); this.anchor = anchor.immutable(); this.validate(); }
    public ExcavateOrder(CompoundTag tag) {
        super(tag); this.min = BlockPos.of(tag.getLong("Min")); this.max = BlockPos.of(tag.getLong("Max"));
        this.anchor = tag.contains("Anchor") ? BlockPos.of(tag.getLong("Anchor")) : this.min;
        this.stair = Math.max(0, tag.getInt("Stair")); this.stairTask = Math.max(0, tag.getInt("StairTask"));
        this.cursor = Math.max(0, tag.getInt("Cursor")); this.layer = Math.max(0, tag.getInt("Layer")); this.validate();
    }
    private void validate() {
        long w = (long)this.max.getX() - this.min.getX() + 1, d = (long)this.max.getZ() - this.min.getZ() + 1, h = (long)this.max.getY() - this.min.getY() + 1;
        if (!GameplayRules.isHoleWidth(w, 71) || !GameplayRules.isHoleWidth(d, 71) || h < 3 || h > 25)
            throw new IllegalArgumentException("Custom excavation requires odd widths/depths 3..71 and height 3..25");
    }
    @Override public ResourceLocation getID() { return ID; }
    @Override protected boolean checkSafety() { return true; }
    private BlockPos step(int index) {
        int w = this.max.getX() - this.min.getX(), d = this.max.getZ() - this.min.getZ();
        int offset = this.anchor.getZ() == this.min.getZ() ? this.anchor.getX() - this.min.getX()
                : this.anchor.getX() == this.max.getX() ? w + this.anchor.getZ() - this.min.getZ()
                : this.anchor.getZ() == this.max.getZ() ? w + d + this.max.getX() - this.anchor.getX()
                : 2 * w + d + this.max.getZ() - this.anchor.getZ();
        int i = Math.floorMod(offset + index, 2 * (w + d));
        if (i < w) return this.min.offset(i, index - 1, 0);
        i -= w; if (i < d) return this.min.offset(w, index - 1, i);
        i -= d; if (i < w) return this.min.offset(w - i, index - 1, d);
        i -= w; return this.min.offset(0, index - 1, d - i);
    }
    private BlockState stairs(int index) {
        BlockPos current = this.step(index), next = this.step(index + 1);
        Direction direction = Direction.fromDelta(Integer.signum(next.getX() - current.getX()), 0, Integer.signum(next.getZ() - current.getZ()));
        return Blocks.BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, direction != null ? direction : Direction.NORTH);
    }
    private BlockState preserved(BlockPos pos) {
        int index = pos.getY() - this.min.getY() + 1;
        return index >= 0 && index < this.max.getY() - this.min.getY() && pos.equals(this.step(index)) ? this.stairs(index) : null;
    }
    @Override protected boolean scanNext(ServerPlayer owner, ServerLevel level) {
        int height = this.max.getY() - this.min.getY() + 1;
        if (this.stair < height - 1) {
            BlockPos floor = this.step(this.stair);
            if (this.stairTask < 3) {
                BlockPos pos = this.stairTask < 2 ? floor.above(this.stairTask + 1) : floor;
                if (!this.scanBlock(owner, level, pos, this.stairTask == 2 ? this.stairs(this.stair) : null)) return false;
                this.stairTask++; return true;
            }
            if (!this.drained()) return false;
            this.stair++; this.stairTask = 0; return true;
        }
        if (this.layer >= height) { this.finishSource(); return true; }
        int w = this.max.getX() - this.min.getX() + 1, d = this.max.getZ() - this.min.getZ() + 1;
        if (this.cursor >= w * d) {
            if (!this.drained()) return false;
            this.layer++; this.cursor = 0; this.nextStage(); return true;
        }
        BlockPos pos = new BlockPos(this.min.getX() + this.cursor % w, this.max.getY() - this.layer, this.min.getZ() + this.cursor / w);
        if (!this.scanBlock(owner, level, pos, this.preserved(pos))) return false;
        this.cursor++; return true;
    }
    @Override protected void preview(Player player, Level level) {
        if (player.tickCount % 4 == 0) MinUtil.particleAtBorders(player.getRandom(), this.max.getY() + 1.1, level, this.min.getX(), this.max.getX() + 1, this.min.getZ(), this.max.getZ() + 1);
    }
    @Override public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag(); tag.putLong("Min", this.min.asLong()); tag.putLong("Max", this.max.asLong());
        tag.putLong("Anchor", this.anchor.asLong());
        tag.putInt("Stair", this.stair); tag.putInt("StairTask", this.stairTask); tag.putInt("Cursor", this.cursor); tag.putInt("Layer", this.layer);
        return this.saveWork(tag);
    }
}
