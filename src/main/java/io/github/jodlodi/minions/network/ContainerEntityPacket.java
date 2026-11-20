package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.registry.WorkerSounds;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.work.StaffActions;
import io.github.jodlodi.minions.work.WorkerActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
	private final boolean keepWorking;

	public ContainerEntityPacket(UUID uuid) {
		this(uuid, false);
	}
	public ContainerEntityPacket(UUID uuid, boolean keepWorking) { this.uuid = uuid; this.keepWorking = keepWorking; }

	public ContainerEntityPacket(FriendlyByteBuf buf) {
		this.uuid = buf.readUUID();
		this.keepWorking = buf.readBoolean();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(this.uuid);
		buf.writeBoolean(this.keepWorking);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
        var target = player.serverLevel().getEntity(this.uuid);
        if (!CommandValidation.entity(player, target) || WorkerActions.container(player, target) == null) return;
		capability.setContainerEntity(this.uuid);
		capability.setContainerBlock(null);
        if (!this.keepWorking) {
            capability.setOrder(null);
            StaffActions.forEach(player, capability, Minion::requestDelivery);
        }
        WorkerSounds.play(player, "randomorder");
	}
}
