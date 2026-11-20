package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.DropWorkerPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DropWorkerButton extends AbstractMastersButton {
    public DropWorkerButton(int x, int y, MastersStaffScreen.EntityStaffScreen screen) { super(x, y, 168, 0, screen); }
    @Override public void onPress() { PacketDistributor.sendToServer(new DropWorkerPacket(((MastersStaffScreen.EntityStaffScreen)this.screen).target.getUUID())); this.screen.onClose(); }
    @Override protected MutableComponent getName() { return Component.translatable("gui.minions_remastered.drop"); }
}
