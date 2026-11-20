package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.StaffActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Uses the same release-to-select interaction as the other staff actions. */
public final class StaffActionButton extends AbstractMastersButton {
    private final int action, secondary;
    private final String label, secondaryLabel;
    private final BlockPos pos;
    public StaffActionButton(int x, int y, MastersStaffScreen screen, BlockPos pos, int action, String label, int secondary, String secondaryLabel) {
        super(x, y, 149, 0, screen); this.pos = pos; this.action = action; this.label = label; this.secondary = secondary; this.secondaryLabel = secondaryLabel;
    }
    @Override public void onPress() { PacketDistributor.sendToServer(new StaffActionPacket(this.screen.isShiftKeyDown() ? this.secondary : this.action, this.pos)); this.screen.onClose(); }
    @Override protected MutableComponent getName() { return Component.translatable(this.screen.isShiftKeyDown() ? this.secondaryLabel : this.label); }
    @Override protected List<MutableComponent> getTooltipLines() { return List.of(Component.translatable("gui.minions_remastered.shift_action", Component.translatable(this.secondaryLabel))); }
}
