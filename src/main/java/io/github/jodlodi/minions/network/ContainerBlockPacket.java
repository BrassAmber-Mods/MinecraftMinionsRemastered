package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.registry.WorkerSounds;
import io.github.jodlodi.minions.work.StaffActions;
import io.github.jodlodi.minions.work.WorkerActions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ContainerBlockPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<ContainerBlockPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("container_block_packet"));
    public static final StreamCodec<FriendlyByteBuf, ContainerBlockPacket> STREAM_CODEC = StreamCodec.ofMember(ContainerBlockPacket::encode, ContainerBlockPacket::new);

    @Override
    public CustomPacketPayload.Type<ContainerBlockPacket> type() {
        return TYPE;
    }

	private final BlockPos pos;
	private final boolean keepWorking;

	public ContainerBlockPacket(BlockPos pos) {
		this(pos, false);
	}
	public ContainerBlockPacket(BlockPos pos, boolean keepWorking) { this.pos = pos; this.keepWorking = keepWorking; }

	public ContainerBlockPacket(FriendlyByteBuf buf) {
		this.pos = buf.readBlockPos();
		this.keepWorking = buf.readBoolean();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeBoolean(this.keepWorking);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
        if (WorkerActions.container(player, this.pos) == null) return;
		capability.setContainerBlock(this.pos);
		capability.setContainerEntity(null);
        if (!this.keepWorking) {
            capability.setOrder(null);
            StaffActions.forEach(player, capability, Minion::requestDelivery);
        }
        WorkerSounds.play(player, "randomorder");
	}
}
