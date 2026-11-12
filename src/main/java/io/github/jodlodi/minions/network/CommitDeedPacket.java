package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.deeds.DeedManager;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CommitDeedPacket(UUID offerId, String deedId) implements CustomPacketPayload {
    public static final Type<CommitDeedPacket> TYPE = new Type<>(MinionsRemastered.locate("commit_deed"));
    public static final StreamCodec<FriendlyByteBuf, CommitDeedPacket> STREAM_CODEC = StreamCodec.ofMember(CommitDeedPacket::encode, CommitDeedPacket::new);

    public CommitDeedPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(64));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.offerId);
        buf.writeUtf(this.deedId, 64);
    }

    @Override
    public Type<CommitDeedPacket> type() {
        return TYPE;
    }

    public static void handle(CommitDeedPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) DeedManager.commit(player, packet.offerId, packet.deedId);
    }
}
