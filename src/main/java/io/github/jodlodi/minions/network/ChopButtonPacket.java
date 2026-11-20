package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.orders.ChopOrder;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.work.GameplayRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Objects;

@ParametersAreNonnullByDefault
public class ChopButtonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<ChopButtonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("chop_button_packet"));
    public static final StreamCodec<FriendlyByteBuf, ChopButtonPacket> STREAM_CODEC = StreamCodec.ofMember(ChopButtonPacket::encode, ChopButtonPacket::new);

    @Override
    public CustomPacketPayload.Type<ChopButtonPacket> type() {
        return TYPE;
    }

	protected final boolean paused;
	protected final BlockPos minPos;
	protected final BlockPos maxPos;
	protected final Block predicate;

	public ChopButtonPacket(boolean paused, BlockPos minPos, BlockPos maxPos, @Nullable Block predicate) {
		this.paused = paused;
		this.minPos = minPos;
		this.maxPos = maxPos;
		this.predicate = predicate;
	}

	public ChopButtonPacket(FriendlyByteBuf buf) {
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
        if (!CommandValidation.harvest(player, this.minPos, this.maxPos, this.predicate, BlockTags.LOGS)) return;
		if (!GameplayRules.start(player, capability, new ChopOrder(this.minPos, this.maxPos, this.predicate))) return;
		capability.setPaused(this.paused);
	}
}
