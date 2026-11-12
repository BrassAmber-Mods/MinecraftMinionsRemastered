package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class BlinkPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlinkPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("blink_packet"));
    public static final StreamCodec<FriendlyByteBuf, BlinkPacket> STREAM_CODEC = StreamCodec.ofMember(BlinkPacket::encode, BlinkPacket::new);

    @Override
    public CustomPacketPayload.Type<BlinkPacket> type() {
        return TYPE;
    }

    private final int entityID;
    private final double x;
    private final double y;
    private final double z;

    public BlinkPacket(int id, double x, double y, double z) {
        this.entityID = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlinkPacket(int id, Vec3 vec3) {
        this(id, vec3.x, vec3.y, vec3.z);
    }

    public BlinkPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    public static class Handler {
        public static void onMessage(BlinkPacket message, IPayloadContext ctx) {
            ctx.enqueueWork(() -> Optional.ofNullable(Minecraft.getInstance().cameraEntity).ifPresent(entity -> {
                if (entity.level().getEntity(message.entityID) instanceof Minion minion) {
                    minion.setPosRaw(message.x, message.y, message.z);
                    minion.xo = message.x;
                    minion.xOld = message.x;
                    minion.yo = message.y;
                    minion.yOld = message.y;
                    minion.zo = message.z;
                    minion.zOld = message.z;

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
