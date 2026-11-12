package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.MinionsConfig;
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

public class MineAheadOrder extends AbstractWorkOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("mine_ahead_order");
    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final Direction direction;
    private final int depth;
    private int current;
    private int cursor;

    public MineAheadOrder(BlockPos minPos, BlockPos maxPos, Direction direction, int depth) {
        this.minPos = minPos.immutable();
        this.maxPos = maxPos.immutable();
        this.direction = direction;
        this.depth = depth;
        this.validate();
    }

    public MineAheadOrder(CompoundTag tag) {
        super(tag);
        this.minPos = BlockPos.of(tag.getLong("min"));
        this.maxPos = BlockPos.of(tag.getLong("max"));
        this.direction = Direction.from3DDataValue(tag.getInt("direction"));
        this.depth = tag.getInt("depth");
        this.current = Math.max(0, tag.getInt("current"));
        this.cursor = Math.max(0, tag.getInt("cursor"));
        this.validate();
    }

    private void validate() {
        int width = this.direction.getAxis() == Direction.Axis.X ? this.maxPos.getZ() - this.minPos.getZ() + 1 : this.maxPos.getX() - this.minPos.getX() + 1;
        int height = this.maxPos.getY() - this.minPos.getY() + 1;
        if (!this.direction.getAxis().isHorizontal() || this.depth < 1 || this.depth > 4096
                || this.minPos.get(this.direction.getAxis()) != this.maxPos.get(this.direction.getAxis())
                || !((width == 1 && height == 2) || (width == 3 && height == 3) || (width == 5 && height == 5))) throw new IllegalArgumentException("Invalid tunnel bounds");
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
        tag.putInt("current", this.current);
        tag.putInt("direction", this.direction.ordinal());
        tag.putInt("depth", this.depth);
        tag.putInt("cursor", this.cursor);
        return this.saveWork(tag);
    }

    @Override
    protected boolean scanNext(ServerPlayer owner, ServerLevel level) {
        BlockPos min = this.minPos.relative(this.direction, this.current);
        BlockPos max = this.maxPos.relative(this.direction, this.current);
        if (this.current >= this.depth || !level.isInWorldBounds(min) || !level.getWorldBorder().isWithinBounds(min)
                || !level.getWorldBorder().isWithinBounds(max)) { this.finishSource(); return true; }
        int width = this.direction.getAxis() == Direction.Axis.X ? max.getZ() - min.getZ() + 1 : max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;
        int blocks = width * height;
        if (this.cursor < blocks) {
            int horizontal = this.cursor % width;
            BlockPos pos = this.direction.getAxis() == Direction.Axis.X
                    ? new BlockPos(min.getX(), min.getY() + this.cursor / width, min.getZ() + horizontal)
                    : new BlockPos(min.getX() + horizontal, min.getY() + this.cursor / width, min.getZ());
            if (!this.scanBlock(owner, level, pos, null)) return false;
        } else if (this.cursor < blocks + width) {
            if (this.cursor == blocks && !this.drained()) return false;
            int horizontal = this.cursor - blocks;
            BlockPos floor = this.direction.getAxis() == Direction.Axis.X ? new BlockPos(min.getX(), min.getY() - 1, min.getZ() + horizontal)
                    : new BlockPos(min.getX() + horizontal, min.getY() - 1, min.getZ());
            if (!this.loaded(owner, level, floor)) return false;
            if (MinionsConfig.REPAIR_TUNNELS.get() && level.getBlockState(floor).canBeReplaced()
                    && !this.scanBlock(owner, level, floor, Blocks.BLACKSTONE.defaultBlockState())) return false;
        } else if (this.cursor == blocks + width) {
            if (!this.drained()) return false;
            BlockPos torch = this.direction.getAxis() == Direction.Axis.X ? new BlockPos(min.getX(), min.getY(), min.getZ() + width / 2)
                    : new BlockPos(min.getX() + width / 2, min.getY(), min.getZ());
            if (MinionsConfig.PLACE_TORCHES.get() && this.current % 7 == 0 && this.loaded(owner, level, torch)
                    && level.getBlockState(torch).isAir() && level.getMaxLocalRawBrightness(torch) < 8
                    && !this.scanBlock(owner, level, torch, Blocks.TORCH.defaultBlockState())) return false;
        } else {
            if (!this.drained()) return false;
            this.current++;
            this.cursor = 0;
            this.nextStage();
            return true;
        }
        this.cursor++;
        return true;
    }

    @Override
    protected void preview(Player player, Level level) {
        if (player.tickCount % 4 != 0) return;
        boolean axis = this.direction.getAxis() == Direction.Axis.Z;
        int mul = (axis ? this.direction.getStepZ() : this.direction.getStepX()) == -1 ? 1 : 0;
        MinUtil.particleAtWall(player.getRandom(), (axis ? this.minPos.getZ() : this.minPos.getX()) + mul, level, axis,
                axis ? this.minPos.getX() : this.minPos.getZ(), (axis ? this.maxPos.getX() : this.maxPos.getZ()) + 1, this.minPos.getY(), this.maxPos.getY() + 1);
    }
}
