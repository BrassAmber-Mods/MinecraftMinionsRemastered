package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

public record LightningVisualPacket(Vec3 start, Vec3 end, long seed) implements CustomPacketPayload {
    public static final Type<LightningVisualPacket> TYPE = new Type<>(MinionsRemastered.locate("lightning_visual"));
    public static final StreamCodec<FriendlyByteBuf, LightningVisualPacket> STREAM_CODEC = StreamCodec.ofMember(LightningVisualPacket::encode, LightningVisualPacket::new);
    public LightningVisualPacket(FriendlyByteBuf buf) { this(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readLong()); }
    public void encode(FriendlyByteBuf buf) { buf.writeDouble(this.start.x); buf.writeDouble(this.start.y); buf.writeDouble(this.start.z); buf.writeDouble(this.end.x); buf.writeDouble(this.end.y); buf.writeDouble(this.end.z); buf.writeLong(this.seed); }
    @Override public Type<LightningVisualPacket> type() { return TYPE; }
    public static void handle(LightningVisualPacket packet, IPayloadContext context) { context.enqueueWork(() -> {
        var level = Minecraft.getInstance().level; if (level == null) return;
        var random = RandomSource.create(packet.seed());
        var dust = new DustParticleOptions(new Vector3f(0.45F, 0.7F, 1.0F), 0.8F);
        for (int strand = 0; strand < 3; strand++) for (int i = 0; i <= 48; i++) {
            double t = i / 48.0, jitter = Math.sin(t * Math.PI) * 0.12;
            Vec3 pos = packet.start().lerp(packet.end(), t).add((random.nextDouble() - 0.5) * jitter, (random.nextDouble() - 0.5) * jitter, (random.nextDouble() - 0.5) * jitter);
            level.addParticle(dust, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }); }
}
