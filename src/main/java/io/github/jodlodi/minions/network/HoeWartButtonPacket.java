package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.orders.HoeWartOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class HoeWartButtonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<HoeWartButtonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("hoe_wart_button_packet"));
    public static final StreamCodec<FriendlyByteBuf, HoeWartButtonPacket> STREAM_CODEC = StreamCodec.ofMember(HoeWartButtonPacket::encode, HoeWartButtonPacket::new);

    @Override
    public CustomPacketPayload.Type<HoeWartButtonPacket> type() {
        return TYPE;
    }

	protected final boolean paused;
	protected final BlockPos minPos;
	protected final BlockPos maxPos;
	protected final Block predicate;

	public HoeWartButtonPacket(boolean paused, BlockPos minPos, BlockPos maxPos, @Nullable Block predicate) {
		this.paused = paused;
		this.minPos = minPos;
		this.maxPos = maxPos;
		this.predicate = predicate;
	}

	public HoeWartButtonPacket(FriendlyByteBuf buf) {
		this.paused = buf.readBoolean();
		this.minPos = buf.readBlockPos();
		this.maxPos = buf.readBlockPos();
		this.predicate = buf.readBoolean() ? BuiltInRegistries.BLOCK.get(buf.readResourceLocation()) : null;
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(this.paused);
		buf.writeBlockPos(this.minPos);
		buf.writeBlockPos(this.maxPos);

		boolean more = this.predicate != null;
		buf.writeBoolean(more);
		if (more) buf.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(this.predicate)));
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
        if (!CommandValidation.harvest(player, this.minPos, this.maxPos, this.predicate, net.minecraft.tags.BlockTags.WART_BLOCKS)) return;
		capability.setOrder(new HoeWartOrder(this.minPos, this.maxPos, this.predicate));
		capability.setPaused(this.paused);
	}
}
