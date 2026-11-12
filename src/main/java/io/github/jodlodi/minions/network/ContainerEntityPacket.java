package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ContainerEntityPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<ContainerEntityPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("container_entity_packet"));
    public static final StreamCodec<FriendlyByteBuf, ContainerEntityPacket> STREAM_CODEC = StreamCodec.ofMember(ContainerEntityPacket::encode, ContainerEntityPacket::new);

    @Override
    public CustomPacketPayload.Type<ContainerEntityPacket> type() {
        return TYPE;
    }

	private final UUID uuid;

	public ContainerEntityPacket(UUID uuid) {
		this.uuid = uuid;
	}

	public ContainerEntityPacket(FriendlyByteBuf buf) {
		this.uuid = buf.readUUID();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(this.uuid);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
		capability.setContainerEntity(this.uuid);
		capability.setContainerBlock(null);
	}
}
