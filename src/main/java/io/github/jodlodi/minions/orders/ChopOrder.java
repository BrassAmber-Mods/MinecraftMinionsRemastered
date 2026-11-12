package io.github.jodlodi.minions.orders;

import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Predicate;

public class ChopOrder extends HarvestAreaOrder {
    public static final ResourceLocation ID = MinionsRemastered.locate("chop_order");
    public static final Predicate<BlockState> DEFAULT_CHOP = state -> state.is(BlockTags.LOGS);

    public ChopOrder(BlockPos min, BlockPos max, Block predicate) { super(min, max, predicate); }
    public ChopOrder(CompoundTag tag) { super(tag); }
    @Override
    public ResourceLocation getID() { return ID; }
    @Override
    protected boolean matches(BlockState state) { return DEFAULT_CHOP.test(state); }
}
