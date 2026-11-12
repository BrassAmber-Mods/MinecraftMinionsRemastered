package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class PoofPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PoofPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("poof_packet"));
    public static final StreamCodec<FriendlyByteBuf, PoofPacket> STREAM_CODEC = StreamCodec.ofMember(PoofPacket::encode, PoofPacket::new);

    @Override
    public CustomPacketPayload.Type<PoofPacket> type() {
        return TYPE;
    }

    private final int entityID;

    public PoofPacket(int id) {
        this.entityID = id;
    }

    public PoofPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityID);
    }

    public static class Handler {
        public static void onMessage(PoofPacket message, IPayloadContext ctx) {
            ctx.enqueueWork(() -> Optional.ofNullable(Minecraft.getInstance().cameraEntity).ifPresent(entity -> {
                if (entity.level().getEntity(message.entityID) instanceof Minion minion) {
                    for (int i = 0; i < 16; i++) {
                        minion.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, minion.getRandomX(0.5D), minion.getRandomY(), minion.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
                        minion.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, minion.getRandomX(1.0D), minion.getRandomY(), minion.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
                        minion.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, minion.getRandomX(1.5D), minion.getRandomY(), minion.getRandomZ(1.5D), 0.0D, 0.0D, 0.0D);
                    }
                }
            }));


        }
    }
}
