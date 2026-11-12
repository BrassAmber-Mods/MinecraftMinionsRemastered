package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockStaffScreenPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlockStaffScreenPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("block_staff_screen_packet"));
    public static final StreamCodec<FriendlyByteBuf, BlockStaffScreenPacket> STREAM_CODEC = StreamCodec.ofMember(BlockStaffScreenPacket::encode, BlockStaffScreenPacket::new);

    @Override
    public CustomPacketPayload.Type<BlockStaffScreenPacket> type() {
        return TYPE;
    }

    private final int player;
    private final double x;
    private final double y;
    private final double z;
    private final Direction direction;
    private final BlockPos pos;
    private final boolean inside;

    public BlockStaffScreenPacket(UseOnContext onContext, Player player) {
        this.player = player.getId();
        Vec3 vec3 = onContext.getClickLocation();
        this.x = vec3.x;
        this.y = vec3.y;
        this.z = vec3.z;
        this.direction = onContext.getClickedFace();
        this.pos = onContext.getClickedPos();
        this.inside = onContext.isInside();
    }

    public BlockStaffScreenPacket(FriendlyByteBuf buf) {
        this.player = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.direction = Direction.values()[buf.readInt()];
        this.pos = BlockPos.of(buf.readLong());
        this.inside = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.player);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeInt(this.direction.ordinal());
        buf.writeLong(this.pos.asLong());
        buf.writeBoolean(this.inside);
    }

    public static class Handler {
        @SuppressWarnings("Convert2Lambda")
        public static void onMessage(BlockStaffScreenPacket message, IPayloadContext ctx) {
            ctx.enqueueWork(new Runnable() {
                @Override
                public void run() {
                    Minecraft minecraft = Minecraft.getInstance();
                    LocalPlayer player = minecraft.player;
                    if (player != null && player.getId() == message.player) {
                        Vec3 vec3 = new Vec3(message.x, message.y, message.z);
                        if (!player.isSecondaryUseActive() && player.distanceToSqr(vec3) <= 0.5D && vec3.y <= player.position().y + 0.25D) {
                            minecraft.setScreen(MastersStaffScreen.make(player, new EntityHitResult(player), false));
                        } else {
                            minecraft.setScreen(MastersStaffScreen.make(player, new BlockHitResult(vec3, message.direction, message.pos, message.inside), false));
                        }
                    }
                }
            });


        }
    }
}
