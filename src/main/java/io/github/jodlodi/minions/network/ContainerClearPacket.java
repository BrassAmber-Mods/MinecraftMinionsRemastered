package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ContainerClearPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<ContainerClearPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("container_clear_packet"));
    public static final StreamCodec<FriendlyByteBuf, ContainerClearPacket> STREAM_CODEC = StreamCodec.ofMember(ContainerClearPacket::encode, ContainerClearPacket::new);

    @Override
    public CustomPacketPayload.Type<ContainerClearPacket> type() {
        return TYPE;
    }


	public ContainerClearPacket() {

	}

	public ContainerClearPacket(FriendlyByteBuf buf) {

	}

	@Override
	public void encode(FriendlyByteBuf buf) {

	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
		capability.setContainerEntity(null);
		capability.setContainerBlock(null);
	}
}
