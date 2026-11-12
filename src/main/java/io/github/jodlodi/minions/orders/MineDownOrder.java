package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.MinionsRemastered;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MineDownOrder extends AbstractWorkOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("mine_down_order");
    public static final List<Direction> DIRECTIONS = List.of(Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST);
    public static final BlockState STAIRS = Blocks.BLACKSTONE_STAIRS.defaultBlockState();
    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final int startDir;
    private final int stairs;
    private int currentY;
    private int cursor;

    public MineDownOrder(BlockPos minPos, BlockPos maxPos, int startDir, int stairs) {
        this.minPos = minPos.immutable();
        this.maxPos = maxPos.immutable();
        this.currentY = maxPos.getY();
        this.startDir = startDir;
        this.stairs = stairs;
        this.validate();
    }

    public MineDownOrder(CompoundTag tag) {
        super(tag);
        this.minPos = BlockPos.of(tag.getLong("min"));
        this.maxPos = BlockPos.of(tag.getLong("max"));
        this.currentY = tag.getInt("currentY");
        this.startDir = tag.getInt("startDir");
        this.stairs = tag.getInt("stairs");
        this.cursor = Math.max(0, tag.getInt("cursor"));
        this.validate();
    }

    private void validate() {
        long width = (long)this.maxPos.getX() - this.minPos.getX() + 1;
        long depth = (long)this.maxPos.getZ() - this.minPos.getZ() + 1;
        if (width < 1 || width > 32 || width != depth || this.minPos.getY() != this.maxPos.getY()
                || this.startDir < 0 || this.startDir > 3 || this.stairs < 0 || this.stairs > 2) throw new IllegalArgumentException("Invalid shaft bounds");
    }

    @Override
    public ResourceLocation getID() { return ID; }
    @Override
    protected boolean followsVeins() { return true; }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("min", this.minPos.asLong());
        tag.putLong("max", this.maxPos.asLong());
        tag.putInt("currentY", this.currentY);
        tag.putInt("startDir", this.startDir);
        tag.putInt("stairs", this.stairs);
        tag.putInt("cursor", this.cursor);
        return this.saveWork(tag);
    }

    @Override
    protected boolean scanNext(ServerPlayer owner, ServerLevel level) {
        if (level.isOutsideBuildHeight(this.currentY) || this.currentY > this.maxPos.getY()) { this.finishSource(); return true; }
        int width = this.maxPos.getX() - this.minPos.getX() + 1;
        if (this.cursor >= width * width) {
            if (!this.drained()) return false;
            this.currentY--;
            this.cursor = 0;
            this.nextStage();
            return true;
        }
        BlockPos pos = new BlockPos(this.minPos.getX() + this.cursor % width, this.currentY, this.minPos.getZ() + this.cursor / width);
        BlockState replacement = this.stairs > 0 ? this.stairCheck(pos, RandomSource.create(pos.asLong())) : null;
        if (!this.scanBlock(owner, level, pos, replacement)) return false;
        this.cursor++;
        return true;
    }

    @Override
    protected void preview(Player player, Level level) {
        if (player.tickCount % 4 == 0) MinUtil.particleAtBorders(player.getRandom(), this.maxPos.getY() + 1.1D, level,
                this.minPos.getX(), this.maxPos.getX() + 1, this.minPos.getZ(), this.maxPos.getZ() + 1);
    }

    @Nullable
    protected BlockState stairCheck(BlockPos pos, RandomSource randomSource) {
        int maxX = this.maxPos.getX();
        int minX = this.minPos.getX();
        int sizeMinusOne = maxX - minX;
        if (sizeMinusOne <= 0) return null;
        int maxZ = this.maxPos.getZ();
        int minZ = this.minPos.getZ();
        int x = pos.getX();
        int z = pos.getZ();

        boolean xMax = maxX == x;
        boolean xMin = minX == x;
        boolean zMax = maxZ == z;
        boolean zMin = minZ == z;

        int depth = this.maxPos.getY() - pos.getY();

        int adjustedSize = sizeMinusOne - 1;
        if (adjustedSize <= 0) adjustedSize = 1;

        boolean clockWise = this.stairs == 1;

        int index = (this.startDir + (depth / adjustedSize)) % 4;
        if (!clockWise) index = 3 - index;
        Direction dir = DIRECTIONS.get(index);

        int adjustedDepth = depth % adjustedSize;

        if (dir.getAxis() == Direction.Axis.X) {
            Direction faceDir = DIRECTIONS.get((DIRECTIONS.indexOf(dir) + 1) % 4);

            if (sizeMinusOne == 1) faceDir = clockWise ? dir : dir.getOpposite();
            int dirX = dir.getNormal().getX();
            if (dirX == 1) {
                if (clockWise ? xMax : xMin) {
                    if (sizeMinusOne > 1 && adjustedDepth == 0 && zMin) return randomSource.nextInt(100) == 1 ? MinUtil.GILDED_BLACKSTONE : MinUtil.BLACKSTONE;
                    if (z - 1 == minZ + adjustedDepth) return STAIRS.setValue(StairBlock.FACING, faceDir);
                }
            } else if (clockWise ? xMin : xMax) {
                if (sizeMinusOne > 1 && adjustedDepth == 0 && zMax) return randomSource.nextInt(100) == 1 ? MinUtil.GILDED_BLACKSTONE : MinUtil.BLACKSTONE;
                if (z + 1 == maxZ - adjustedDepth) return STAIRS.setValue(StairBlock.FACING, faceDir);
            }
        } else {
            Direction faceDir = DIRECTIONS.get((DIRECTIONS.indexOf(dir) + 3) % 4);
            if (sizeMinusOne == 1) faceDir = clockWise ? dir.getOpposite() : dir;
            int dirZ = dir.getNormal().getZ();
            if (dirZ == 1) {
                if (clockWise ? zMin : zMax) {
                    if (sizeMinusOne > 1 && adjustedDepth == 0 && xMin) return randomSource.nextInt(100) == 1 ? MinUtil.GILDED_BLACKSTONE : MinUtil.BLACKSTONE;
                    if (x - 1 == minX + adjustedDepth) return STAIRS.setValue(StairBlock.FACING, faceDir);
                }
            } else if (clockWise ? zMax : zMin) {
                if (sizeMinusOne > 1 && adjustedDepth == 0 && xMax) return randomSource.nextInt(100) == 1 ? MinUtil.GILDED_BLACKSTONE : MinUtil.BLACKSTONE;
                if (x + 1 == maxX - adjustedDepth) return STAIRS.setValue(StairBlock.FACING, faceDir);
            }
        }

        return null;
    }

}
