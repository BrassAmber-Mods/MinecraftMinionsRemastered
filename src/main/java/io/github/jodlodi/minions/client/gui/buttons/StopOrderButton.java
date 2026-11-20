package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.StopButtonPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StopOrderButton extends AbstractMastersButton {

    public StopOrderButton(int x, int y, MastersStaffScreen screen) {
        super(x, y, 206, 0, screen);
    }

    @Override
    public void onPress() {
        PacketDistributor.sendToServer(new StopButtonPacket(this.screen.isShiftKeyDown()));
        this.screen.onClose();
    }

    @Override
    protected MutableComponent getName() {
        return Component.translatable("gui.minions_remastered.pause_resume").withStyle(ChatFormatting.DARK_RED);
    }

    @Override
    protected List<MutableComponent> getTooltipLines() {
        return List.of(Component.translatable("gui.minions_remastered.shift").withStyle(this.screen.isShiftKeyDown() ? ChatFormatting.AQUA : ChatFormatting.DARK_RED)
                .append(Component.translatable("gui.minions_remastered.cancel_order_hint").withStyle(ChatFormatting.DARK_GRAY)));
    }
}
