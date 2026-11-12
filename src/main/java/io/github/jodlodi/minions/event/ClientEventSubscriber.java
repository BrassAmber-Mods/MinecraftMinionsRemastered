package io.github.jodlodi.minions.event;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.registry.ClientRegistry;
import io.github.jodlodi.minions.network.OpenDeedsPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

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
