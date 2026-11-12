package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.deeds.DeedManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenDeedsPacket() implements CustomPacketPayload {
    public static final Type<OpenDeedsPacket> TYPE = new Type<>(MinionsRemastered.locate("open_deeds"));
    public static final StreamCodec<FriendlyByteBuf, OpenDeedsPacket> STREAM_CODEC = StreamCodec.unit(new OpenDeedsPacket());

    @Override
    public Type<OpenDeedsPacket> type() {
        return TYPE;
    }

    public static void handle(OpenDeedsPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) DeedManager.openMenu(player);
    }
}
