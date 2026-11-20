package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.work.StaffActions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class StaffActionPacket extends AbstractServerboundPacket {
    public static final Type<StaffActionPacket> TYPE = new Type<>(MinionsRemastered.locate("staff_action"));
    public static final StreamCodec<FriendlyByteBuf, StaffActionPacket> STREAM_CODEC = StreamCodec.ofMember(StaffActionPacket::encode, StaffActionPacket::new);
    private final int action, width, height;
    private final BlockPos pos;
    public StaffActionPacket(int action, BlockPos pos) { this(action, pos, 3, 3); }
    public StaffActionPacket(int action, BlockPos pos, int width, int height) { this.action = action; this.pos = pos; this.width = width; this.height = height; }
    public StaffActionPacket(FriendlyByteBuf buf) { this.action = buf.readVarInt(); this.pos = buf.readBlockPos(); this.width = buf.readVarInt(); this.height = buf.readVarInt(); }
    @Override public void encode(FriendlyByteBuf buf) { buf.writeVarInt(this.action); buf.writeBlockPos(this.pos); buf.writeVarInt(this.width); buf.writeVarInt(this.height); }
    @Override public Type<StaffActionPacket> type() { return TYPE; }
    @Override void execute(ServerPlayer player, IMasterCapability master) { StaffActions.execute(player, master, this.action, this.pos, this.width, this.height); }
}
