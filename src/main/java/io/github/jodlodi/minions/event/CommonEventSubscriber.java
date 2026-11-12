package io.github.jodlodi.minions.event;

import io.github.jodlodi.minions.MastersStaff;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.network.EntityStaffScreenPacket;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.PacketRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = MinionsRemastered.MODID)
public class CommonEventSubscriber {
    @SubscribeEvent
    public static void onLivingTick(PlayerTickEvent.Post event) {
        CommonRegistry.getMaster(event.getEntity()).ifPresent(IMasterCapability::tick);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player != null && player.getItemInHand(event.getHand()).getItem() instanceof MastersStaff) {
            if (!player.level().isClientSide) {
                CommonRegistry.getMaster(player).ifPresent(IMasterCapability::sendUpdatePacket);
                PacketRegistry.sendToPlayer(player, new EntityStaffScreenPacket(event.getTarget(), player));
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL) && event.getEntity().getVehicle() instanceof Minion) {
            event.setCanceled(true);
        }
    }

}
