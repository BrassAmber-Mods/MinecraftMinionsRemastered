package io.github.jodlodi.minions.client.gui.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractAdjustableOrderButton extends AbstractMastersButton {
    protected final boolean leftAndRight;
    protected final boolean toggle;
    protected final boolean pausable;

    public AbstractAdjustableOrderButton(int x, int y, int textureX, int textureY, MastersStaffScreen screen, boolean leftAndRight, boolean toggle, boolean pausable) {
        super(x, y, textureX, textureY, screen);
        this.leftAndRight = leftAndRight;
        this.toggle = toggle;
        this.pausable = pausable;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.leftAndRight && this.isMouseOverLeftMini(mouseX, mouseY)) {
            this.onLeftPress();
            return;
        }

        if (this.toggle && this.isMouseOverToggleMini(mouseX, mouseY)) {
            this.onTogglePress();
            return;
        }

        if (this.leftAndRight && this.isMouseOverRightMini(mouseX, mouseY)) {
            this.onRightPress();
            return;
        }


        super.onClick(mouseX, mouseY);
    }

    abstract void onLeftPress();

    abstract void onTogglePress();

    abstract void onRightPress();

    @Override
    protected void renderExtra(GuiGraphics stack, int mouseX, int mouseY, float partialTick) {
        super.renderExtra(stack, mouseX, mouseY, partialTick);
        RenderSystem.setShaderTexture(0, this.getShaderTexture(mouseX, mouseY));

        if (this.leftAndRight) {
            boolean left = this.isMouseOverLeftMini(mouseX, mouseY);
            this.blit(stack, this.getX() + MastersStaffScreen.MENU_SCALE, this.getY() + 11 * MastersStaffScreen.MENU_SCALE, 225, left ? 5 : 0, 5, 5);

            boolean right = this.isMouseOverRightMini(mouseX, mouseY);
            this.blit(stack, this.getX() + 13 * MastersStaffScreen.MENU_SCALE, this.getY() + 11 * MastersStaffScreen.MENU_SCALE, 230, right ? 5 : 0, 5, 5);
        }

        if (this.toggle) {
            boolean toggle = this.isMouseOverToggleMini(mouseX, mouseY);
            this.blit(stack, this.getX() + 7 * MastersStaffScreen.MENU_SCALE, this.getY() + 13 * MastersStaffScreen.MENU_SCALE, 235, toggle ? 5 : 0, 5, 5);
        }
    }

    public boolean isMouseOverLeftMini(double mouseX, double mouseY) {
        return this.leftAndRight && this.isMouseOverMini(mouseX, mouseY, 0, 7, 11);
    }

    public boolean isMouseOverToggleMini(double mouseX, double mouseY) {
        return this.toggle && this.isMouseOverMini(mouseX, mouseY, 7, 13, 13);
    }

    public boolean isMouseOverRightMini(double mouseX, double mouseY) {
        return this.leftAndRight && this.isMouseOverMini(mouseX, mouseY, 13, TEXTURE_BUTTON_SIZE, 11);
    }

    private boolean isMouseOverMini(double mouseX, double mouseY, int left, int right, int top) {
        int scale = MastersStaffScreen.MENU_SCALE;
        return this.active && this.visible && this.isMouseOver(mouseX, mouseY)
                && mouseX >= this.getX() + left * scale && mouseX < this.getX() + right * scale
                && mouseY >= this.getY() + top * scale;
    }

    @Override
    protected final List<MutableComponent> getTooltipLines() {
        List<MutableComponent> list = new ArrayList<>(this.getAdjustableTooltip());
        if (this.pausable) list.add(Component.literal("[SHIFT]").withStyle(this.screen.isShiftKeyDown() ? ChatFormatting.AQUA : ChatFormatting.DARK_RED).append(Component.literal(" Preview mode").withStyle(ChatFormatting.DARK_GRAY)));
        return list;
    }

    abstract protected List<MutableComponent> getAdjustableTooltip();
}
