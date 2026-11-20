package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.work.StaffLightning;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

/** The server derives origin, aim, range, and victims; clients supply no hit coordinates. */
public final class StaffLightningPacket extends AbstractServerboundPacket {
    public static final Type<StaffLightningPacket> TYPE = new Type<>(MinionsRemastered.locate("staff_lightning"));
    public static final StreamCodec<FriendlyByteBuf, StaffLightningPacket> STREAM_CODEC = StreamCodec.ofMember(StaffLightningPacket::encode, StaffLightningPacket::new);
    public StaffLightningPacket() {}
    public StaffLightningPacket(FriendlyByteBuf ignored) {}
    @Override public void encode(FriendlyByteBuf ignored) {}
    @Override public Type<StaffLightningPacket> type() { return TYPE; }
    @Override void execute(ServerPlayer player, IMasterCapability master) { StaffLightning.cast(player); }
}
