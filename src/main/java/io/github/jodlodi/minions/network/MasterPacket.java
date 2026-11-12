package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MasterPacket implements CustomPacketPayload {
    public static final Type<MasterPacket> TYPE = new Type<>(MinionsRemastered.locate("master_packet"));
    public static final StreamCodec<FriendlyByteBuf, MasterPacket> STREAM_CODEC = StreamCodec.ofMember(MasterPacket::encode, MasterPacket::new);

    private final int entityID;
    private final CompoundTag tag;

    public MasterPacket(Entity entity, IMasterCapability master) {
        this.entityID = entity.getId();
        this.tag = master.serializeNBT();
    }

    public MasterPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readInt();
        CompoundTag data = buf.readNbt();
        this.tag = data != null ? data : new CompoundTag();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeNbt(this.tag);
    }

    @Override
    public Type<MasterPacket> type() {
        return TYPE;
    }

    public static class Handler {
        public static void onMessage(MasterPacket message, IPayloadContext context) {
            context.enqueueWork(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null && player.getId() == message.entityID) {
                    CommonRegistry.getMaster(player).ifPresent(master -> master.deserializeNBT(message.tag));
                }
            });
        }
    }
}
