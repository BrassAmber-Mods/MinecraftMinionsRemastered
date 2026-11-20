package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.orders.MineDownOrder;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.work.GameplayRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MineDownButtonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<MineDownButtonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("mine_down_button_packet"));
    public static final StreamCodec<FriendlyByteBuf, MineDownButtonPacket> STREAM_CODEC = StreamCodec.ofMember(MineDownButtonPacket::encode, MineDownButtonPacket::new);

    @Override
    public CustomPacketPayload.Type<MineDownButtonPacket> type() {
        return TYPE;
    }

	protected final boolean paused;
	protected final BlockPos minPos;
	protected final BlockPos maxPos;
	protected final int startDir;
	protected final int stairs;

	public MineDownButtonPacket(boolean paused, BlockPos minPos, BlockPos maxPos, int startDir, int stairs) {
		this.paused = paused;
		this.minPos = minPos;
		this.maxPos = maxPos;
		this.startDir = startDir;
		this.stairs = stairs;
	}

	public MineDownButtonPacket(FriendlyByteBuf buf) {
		this.paused = buf.readBoolean();
		this.minPos = buf.readBlockPos();
		this.maxPos = buf.readBlockPos();
		this.startDir = buf.readInt();
		this.stairs = buf.readInt();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(this.paused);
		buf.writeBlockPos(this.minPos);
		buf.writeBlockPos(this.maxPos);
		buf.writeInt(this.startDir);
		buf.writeInt(this.stairs);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
        if (!CommandValidation.shaft(player, this.minPos, this.maxPos, this.startDir, this.stairs)) return;
		if (!GameplayRules.start(player, capability, new MineDownOrder(this.minPos, this.maxPos, this.startDir, this.stairs))) return;
		capability.setPaused(this.paused);
	}
}
