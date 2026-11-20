package io.github.jodlodi.minions.registry;

import io.github.jodlodi.minions.network.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketRegistry {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToServer(StaffActionPacket.TYPE, StaffActionPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(DropWorkerPacket.TYPE, DropWorkerPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(StaffLightningPacket.TYPE, StaffLightningPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToClient(LightningVisualPacket.TYPE, LightningVisualPacket.STREAM_CODEC, LightningVisualPacket::handle);
        registrar.playToClient(DeedsScreenPacket.TYPE, DeedsScreenPacket.STREAM_CODEC, DeedsScreenPacket::handle);
        registrar.playToServer(OpenDeedsPacket.TYPE, OpenDeedsPacket.STREAM_CODEC, OpenDeedsPacket::handle);
        registrar.playToServer(CommitDeedPacket.TYPE, CommitDeedPacket.STREAM_CODEC, CommitDeedPacket::handle);
        registrar.playToClient(MasterPacket.TYPE, MasterPacket.STREAM_CODEC, MasterPacket.Handler::onMessage);
        registrar.playToClient(BlinkPacket.TYPE, BlinkPacket.STREAM_CODEC, BlinkPacket.Handler::onMessage);
        registrar.playToClient(PoofPacket.TYPE, PoofPacket.STREAM_CODEC, PoofPacket.Handler::onMessage);
        registrar.playToClient(EntityStaffScreenPacket.TYPE, EntityStaffScreenPacket.STREAM_CODEC, EntityStaffScreenPacket.Handler::onMessage);
        registrar.playToClient(BlockStaffScreenPacket.TYPE, BlockStaffScreenPacket.STREAM_CODEC, BlockStaffScreenPacket.Handler::onMessage);
        registrar.playToServer(CarryLivingButtonPacket.TYPE, CarryLivingButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(MineAheadButtonPacket.TYPE, MineAheadButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(ContainerBlockPacket.TYPE, ContainerBlockPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(MineDownButtonPacket.TYPE, MineDownButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(StopButtonPacket.TYPE, StopButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(ChopButtonPacket.TYPE, ChopButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(SitPacket.TYPE, SitPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(HoeWartButtonPacket.TYPE, HoeWartButtonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(ContainerEntityPacket.TYPE, ContainerEntityPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(BanishPacket.TYPE, BanishPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(SummonPacket.TYPE, SummonPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
        registrar.playToServer(ContainerClearPacket.TYPE, ContainerClearPacket.STREAM_CODEC, AbstractServerboundPacket.Handler::onMessage);
    }

    public static void sendToPlayer(Player player, CustomPacketPayload payload) {
        if (player instanceof ServerPlayer serverPlayer) PacketDistributor.sendToPlayer(serverPlayer, payload);
    }
}
