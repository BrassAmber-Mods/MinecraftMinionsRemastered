package io.github.jodlodi.minions.client.gui;

import io.github.jodlodi.minions.network.StaffActionPacket;
import io.github.jodlodi.minions.work.StaffActions;
import io.github.jodlodi.minions.work.GameplayRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.stream.IntStream;

/** Minecraft-scaled access to the original global orders and custom excavation dimensions. */
public final class WorkerOrdersScreen extends Screen {
    private EditBox heightInput;
    private int selectedWidth = 3;
    private String selectedHeight = "3";
    public WorkerOrdersScreen() { super(Component.translatable("gui.minions_remastered.orders")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        int left = this.width / 2 - 110, top = Math.max(24, this.height / 2 - 94);
        this.action(left, top, 106, StaffActions.FOLLOW, "gui.minions_remastered.follow");
        this.action(left + 114, top, 106, StaffActions.UNSUMMON, "gui.minions_remastered.unsummon");
        this.action(left, top + 26, 106, StaffActions.STRIP, "gui.minions_remastered.stripmine");
        this.action(left + 114, top + 26, 106, StaffActions.VEIN, "gui.minions_remastered.vein");
        this.action(left, top + 52, 106, StaffActions.DROP, "gui.minions_remastered.drop");
        this.action(left + 114, top + 52, 106, StaffActions.DELIVER, "gui.minions_remastered.deliver");
        this.addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value.toString()))
                .withValues(IntStream.rangeClosed(3, 71).filter(value -> (value & 1) != 0).boxed().toList())
                .withInitialValue(this.selectedWidth).displayOnlyValue()
                .create(left + 58, top + 88, 44, 20, Component.translatable("gui.minions_remastered.width"),
                        (button, value) -> this.selectedWidth = value));
        this.heightInput = new EditBox(this.font, left + 176, top + 88, 44, 20, Component.translatable("gui.minions_remastered.height"));
        this.heightInput.setMaxLength(2);
        this.heightInput.setFilter(value -> value.matches("[0-9]{0,2}"));
        this.heightInput.setValue(this.selectedHeight);
        this.heightInput.setResponder(value -> this.selectedHeight = value);
        this.addRenderableWidget(this.heightInput);
        this.action(left, top + 116, 220, StaffActions.DIG, "gui.minions_remastered.dig");
        this.addRenderableWidget(Button.builder(Component.translatable("gui.minions_remastered.deeds.cancel"), button -> this.onClose()).bounds(left, top + 144, 220, 20).build());
    }
    private void action(int x, int y, int width, int action, String label) {
        this.addRenderableWidget(Button.builder(Component.translatable(label), button -> this.issue(action)).bounds(x, y, width, 20).build());
    }
    private void issue(int action) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        var hit = this.minecraft.player.pick(30, 0, false);
        boolean targeted = action == StaffActions.DIG || action == StaffActions.VEIN || action == StaffActions.STRIP;
        if (targeted && hit.getType() != HitResult.Type.BLOCK) {
            this.minecraft.player.displayClientMessage(Component.translatable("message.minions_remastered.look_at_block"), true); return;
        }
        int width = 3, height = 3;
        if (action == StaffActions.DIG) {
            try { width = this.selectedWidth; height = Integer.parseInt(this.heightInput.getValue()); }
            catch (NumberFormatException ignored) { return; }
            if (!GameplayRules.isHoleWidth(width, 71) || height < 3 || height > 25) {
                this.minecraft.player.displayClientMessage(Component.translatable("message.minions_remastered.dig_dimensions"), true); return;
            }
        }
        PacketDistributor.sendToServer(new StaffActionPacket(action, hit instanceof BlockHitResult block ? block.getBlockPos() : this.minecraft.player.blockPosition(), width, height));
        this.onClose();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = Math.max(24, this.height / 2 - 94), left = this.width / 2 - 110;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.minions_remastered.width"), left, top + 94, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.minions_remastered.height"), left + 114, top + 94, 0xFFFFFF);
    }
}
