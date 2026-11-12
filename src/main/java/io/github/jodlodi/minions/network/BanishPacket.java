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
public class BanishPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<BanishPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("banish_packet"));
    public static final StreamCodec<FriendlyByteBuf, BanishPacket> STREAM_CODEC = StreamCodec.ofMember(BanishPacket::encode, BanishPacket::new);

    @Override
    public CustomPacketPayload.Type<BanishPacket> type() {
        return TYPE;
    }

	private final UUID id;

	public BanishPacket(UUID id) {
		this.id = id;
	}

	public BanishPacket(FriendlyByteBuf buf) {
		this.id = buf.readUUID();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(this.id);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
		if (capability.minionCount() <= 0) return; //Just in case
		capability.removeMinion(this.id);
	}
}
