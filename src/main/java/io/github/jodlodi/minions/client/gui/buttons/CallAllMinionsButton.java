package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.StaffActionPacket;
import io.github.jodlodi.minions.work.StaffActions;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CallAllMinionsButton extends AbstractMastersButton {
    protected final IMasterCapability capability;

    public CallAllMinionsButton(int x, int y, MastersStaffScreen.BlockStaffScreen screen, IMasterCapability capability) {
        super(x, y, 149, 0, screen);
        this.capability = capability;
    }

    @Override
    public void onPress() {
        BlockPos pos = this.getScreen().getContext().getBlockPos();
        PacketDistributor.sendToServer(new StaffActionPacket(StaffActions.RECALL, pos));
        this.screen.onClose();
    }

    @Override
    public MastersStaffScreen.BlockStaffScreen getScreen() {
        return (MastersStaffScreen.BlockStaffScreen)this.screen;
    }

    @Override
    protected MutableComponent getName() {
        return Component.translatable("gui.minions_remastered.recall").withStyle(ChatFormatting.BLUE);
    }
}
