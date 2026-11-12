package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class StopButtonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<StopButtonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("stop_button_packet"));
    public static final StreamCodec<FriendlyByteBuf, StopButtonPacket> STREAM_CODEC = StreamCodec.ofMember(StopButtonPacket::encode, StopButtonPacket::new);

    @Override
    public CustomPacketPayload.Type<StopButtonPacket> type() {
        return TYPE;
    }

	protected final boolean shift;

	public StopButtonPacket(boolean shift) {
		this.shift = shift;
	}

	public StopButtonPacket(FriendlyByteBuf buf) {
		this.shift = buf.readBoolean();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(this.shift);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
		if (this.shift) capability.setOrder(null);
		else capability.setPaused(!capability.isPaused());
	}
}
