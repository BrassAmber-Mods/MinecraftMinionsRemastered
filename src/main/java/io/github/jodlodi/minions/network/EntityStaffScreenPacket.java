package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EntityStaffScreenPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EntityStaffScreenPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("entity_staff_screen_packet"));
    public static final StreamCodec<FriendlyByteBuf, EntityStaffScreenPacket> STREAM_CODEC = StreamCodec.ofMember(EntityStaffScreenPacket::encode, EntityStaffScreenPacket::new);

    @Override
    public CustomPacketPayload.Type<EntityStaffScreenPacket> type() {
        return TYPE;
    }

    private final int player;
    private final int entityID;
    private final boolean hostile;

    public EntityStaffScreenPacket(Entity entity, Player player) {
        this.player = player.getId();
        this.entityID = entity.getId();
        this.hostile = !player.getAbilities().instabuild && ((entity instanceof NeutralMob neutralMob && neutralMob.isAngryAt(player)) || entity instanceof Enemy);
    }

    public EntityStaffScreenPacket(FriendlyByteBuf buf) {
        this.player = buf.readInt();
        this.entityID = buf.readInt();
        this.hostile = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.player);
        buf.writeInt(this.entityID);
        buf.writeBoolean(this.hostile);
    }

    public static class Handler {
        @SuppressWarnings("Convert2Lambda")
        public static void onMessage(EntityStaffScreenPacket message, IPayloadContext ctx) {
            ctx.enqueueWork(new Runnable() {
                @Override
                public void run() {
                    Minecraft minecraft = Minecraft.getInstance();
                    LocalPlayer player = minecraft.player;
                    if (player != null && player.getId() == message.player) {
                        Entity target = player.level().getEntity(message.entityID);
                        if (target != null) minecraft.setScreen(MastersStaffScreen.make(player, new EntityHitResult(target), message.hostile));
                    }
                }
            });


        }
    }
}
