package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public abstract class AbstractServerboundPacket implements CustomPacketPayload {
    public abstract void encode(FriendlyByteBuf buf);

    abstract void execute(ServerPlayer player, IMasterCapability capability);

    public static class Handler {
        public static void onMessage(AbstractServerboundPacket message, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player &&
                    (player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) || player.getOffhandItem().is(CommonRegistry.MASTERS_STAFF.get()))) {
                CommonRegistry.getMaster(player).ifPresent(capability -> message.execute(player, capability));
            }
        }
    }
}
