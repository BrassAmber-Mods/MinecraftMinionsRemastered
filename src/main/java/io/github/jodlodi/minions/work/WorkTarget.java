package io.github.jodlodi.minions.work;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

/** A bounded, persistable unit of work. Replacements are placed through normal block-item hooks. */
public final class WorkTarget {
    public final BlockPos pos;
    public final BlockState expected;
    public BlockState replacement;
    public final BlockPos veinOrigin;
    public int attempts;
    public long retryAt;
    public int priority;

    public WorkTarget(BlockPos pos, BlockState expected, BlockState replacement, BlockPos veinOrigin) {
        this.pos = pos.immutable();
        this.expected = expected;
        this.replacement = replacement;
        this.veinOrigin = veinOrigin;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", this.pos.asLong());
        tag.put("Expected", NbtUtils.writeBlockState(this.expected));
        if (this.replacement != null) tag.put("Replacement", NbtUtils.writeBlockState(this.replacement));
        if (this.veinOrigin != null) tag.putLong("VeinOrigin", this.veinOrigin.asLong());
        tag.putInt("Attempts", this.attempts);
        tag.putLong("RetryAt", this.retryAt);
        tag.putInt("Priority", this.priority);
        return tag;
    }

    public static WorkTarget load(CompoundTag tag) {
        WorkTarget target = new WorkTarget(BlockPos.of(tag.getLong("Pos")),
                NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Expected")),
                tag.contains("Replacement") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Replacement")) : null,
                tag.contains("VeinOrigin") ? BlockPos.of(tag.getLong("VeinOrigin")) : null);
        target.attempts = Math.max(0, Math.min(3, tag.getInt("Attempts")));
        target.retryAt = tag.getLong("RetryAt");
        target.priority = Math.max(0, Math.min(1, tag.getInt("Priority")));
        return target;
    }
}
