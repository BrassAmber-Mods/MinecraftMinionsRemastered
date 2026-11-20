package io.github.jodlodi.minions.event;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.network.OpenDeedsPacket;
import io.github.jodlodi.minions.network.StaffLightningPacket;
import io.github.jodlodi.minions.registry.ClientRegistry;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = MinionsRemastered.MODID, value = Dist.CLIENT)
public class ClientEventSubscriber {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientRegistry.DEED_MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) PacketDistributor.sendToServer(new OpenDeedsPacket());
        }
        if (minecraft.player != null && minecraft.screen == null && minecraft.options.keyAttack.isDown()
                && minecraft.player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) && minecraft.player.tickCount % 2 == 0) {
            PacketDistributor.sendToServer(new StaffLightningPacket());
        }
    }

    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get())) {
            event.setSwingHand(false); event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTooltipEvent(ItemTooltipEvent event) {
        ItemStack item = event.getItemStack();
        if (item.is(CommonRegistry.MASTERS_STAFF.get())) {
            event.getToolTip().add(Component.translatable("tooltip.minions_remastered.masters_staff.deeds", ClientRegistry.DEED_MENU.getTranslatedKeyMessage()));
        }
    }

    @SubscribeEvent
    public static <T extends LivingEntity, M extends EntityModel<T>> void onRenderLiving(RenderLivingEvent.Pre<T, M> event) {
        if (event.getEntity().getVehicle() instanceof Minion minion && minion.getBlinking()) {
            event.setCanceled(true);
        }
    }
}
