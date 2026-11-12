package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.work.WorkerActions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
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

	public ContainerBlockPacket(BlockPos pos) {
		this.pos = pos;
	}

	public ContainerBlockPacket(FriendlyByteBuf buf) {
		this.pos = buf.readBlockPos();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
        if (WorkerActions.container(player, this.pos) == null) return;
		capability.setContainerBlock(this.pos);
		capability.setContainerEntity(null);
	}
}
