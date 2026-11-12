package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.client.gui.DeedsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeedsScreenPacket(UUID offerId, int xpCost, int completedDeeds, int remainingTicks, boolean rewardPending, List<String> options) implements CustomPacketPayload {
    public static final Type<DeedsScreenPacket> TYPE = new Type<>(MinionsRemastered.locate("deeds_screen"));
    public static final StreamCodec<FriendlyByteBuf, DeedsScreenPacket> STREAM_CODEC = StreamCodec.ofMember(DeedsScreenPacket::encode, DeedsScreenPacket::new);

    public DeedsScreenPacket {
        options = List.copyOf(options);
    }

    public DeedsScreenPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), readOptions(buf));
    }

    private static List<String> readOptions(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 3) throw new IllegalArgumentException("Invalid evil deed option count: " + count);
        List<String> options = new ArrayList<>(count);
        for (int i = 0; i < count; i++) options.add(buf.readUtf(64));
        return options;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.offerId);
        buf.writeInt(this.xpCost);
        buf.writeVarInt(this.completedDeeds);
        buf.writeVarInt(this.remainingTicks);
        buf.writeBoolean(this.rewardPending);
        buf.writeVarInt(this.options.size());
        for (String id : this.options) buf.writeUtf(id, 64);
    }

    @Override
    public Type<DeedsScreenPacket> type() {
        return TYPE;
    }

    public static void handle(DeedsScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new DeedsScreen(packet)));
    }
}
