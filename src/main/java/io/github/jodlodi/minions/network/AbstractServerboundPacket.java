package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.work.CommandValidation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class AbstractServerboundPacket implements CustomPacketPayload {
    public abstract void encode(FriendlyByteBuf buf);

    abstract void execute(ServerPlayer player, IMasterCapability capability);

    public static class Handler {
        private static final Map<ServerPlayer, long[]> RATE = new WeakHashMap<>();

        private static boolean allow(ServerPlayer player) {
            long now = player.level().getGameTime();
            long[] window = RATE.computeIfAbsent(player, ignored -> new long[]{now, 0});
            if (now - window[0] >= 20 || now < window[0]) { window[0] = now; window[1] = 0; }
            return ++window[1] <= 64;
        }

        public static void onMessage(AbstractServerboundPacket message, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && CommandValidation.canCommand(player) && allow(player) &&
                    (player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) || player.getOffhandItem().is(CommonRegistry.MASTERS_STAFF.get()))) {
                CommonRegistry.getMaster(player).ifPresent(capability -> message.execute(player, capability));
            }
        }
    }
}
