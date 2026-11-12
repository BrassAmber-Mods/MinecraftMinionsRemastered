package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.ContainerBlockPacket;
import io.github.jodlodi.minions.network.ContainerEntityPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContainerButton extends AbstractMastersButton {
    public ContainerButton(int x, int y, MastersStaffScreen screen) {
        super(x, y, 168, 0, screen);
    }

    @Override
    public void onPress() {
        if (this.getScreen().getContext() instanceof BlockHitResult blockHitResult) {
            PacketDistributor.sendToServer(new ContainerBlockPacket(blockHitResult.getBlockPos()));
        } else if (this.getScreen().getContext() instanceof EntityHitResult entityHitResult) {
            PacketDistributor.sendToServer(new ContainerEntityPacket(entityHitResult.getEntity().getUUID()));
        }
        this.screen.onClose();
    }

    @Override
    protected MutableComponent getName() {
        return Component.literal("Set Container").withStyle(ChatFormatting.BLUE);
    }
}
