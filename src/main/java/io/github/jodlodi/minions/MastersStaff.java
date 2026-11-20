package io.github.jodlodi.minions;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.network.BlockStaffScreenPacket;
import io.github.jodlodi.minions.network.EntityStaffScreenPacket;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.PacketRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MastersStaff extends Item {
    public MastersStaff(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            CommonRegistry.getMaster(player).ifPresent(IMasterCapability::sendUpdatePacket);
            PacketRegistry.sendToPlayer(player, new EntityStaffScreenPacket(player, player));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext onContext) {
        Player player = onContext.getPlayer();
        if (player != null) {
            if (!player.level().isClientSide) {
                CommonRegistry.getMaster(player).ifPresent(IMasterCapability::sendUpdatePacket);
                PacketRegistry.sendToPlayer(player, new BlockStaffScreenPacket(onContext, player));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
