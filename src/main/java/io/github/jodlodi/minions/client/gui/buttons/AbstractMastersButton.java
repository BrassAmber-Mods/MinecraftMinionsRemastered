package io.github.jodlodi.minions.client.gui.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractMastersButton extends Button {
    // Static
    public static final int BUTTON_SIZE = 19;

    // Finals
    protected final int textureX;
    protected final int textureY;
    protected final MastersStaffScreen screen;

    // Variables
    protected int time = 0;
    protected int lastMouseX;
    protected int lastMouseY;

    public AbstractMastersButton(int x, int y, int textureX, int textureY, MastersStaffScreen screen) {
        super((screen.width - BUTTON_SIZE) / 2 + x, (screen.height - BUTTON_SIZE) / 2 + y, BUTTON_SIZE, BUTTON_SIZE, CommonComponents.EMPTY, (button) -> {}, Button.DEFAULT_NARRATION);
        this.textureX = textureX;
        this.textureY = textureY;
        this.screen = screen;
    }

    @Override
    public abstract void onPress();

    public MastersStaffScreen getScreen() {
        return this.screen;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, this.getShaderTexture(mouseX, mouseY));
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.renderFrame(graphics, mouseX, mouseY, partialTick);
        this.renderIcon(graphics, mouseX, mouseY, partialTick);
        this.renderExtra(graphics, mouseX, mouseY, partialTick);
        if (this.isHoveredOrFocused()) this.renderToolTip(graphics, mouseX, mouseY);
    }

    protected ResourceLocation getShaderTexture(int mouseX, int mouseY) {
        return this.isMouseOver(mouseX, mouseY) && (this.time / 20) % 2 == 1 ? MastersStaffScreen.ALT_TEXTURE_LOCATION : MastersStaffScreen.TEXTURE_LOCATION;
    }

    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.blit(graphics, this.textureX, this.textureY);
    }

    protected void renderFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.blit(graphics, this.textureX, this.textureY + BUTTON_SIZE);
    }

    protected void renderIcon(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.blit(graphics, this.textureX, this.textureY + BUTTON_SIZE + BUTTON_SIZE);
    }

    protected void renderExtra(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

    }

    public void blit(GuiGraphics graphics, int textureX, int textureY) {
        graphics.blit(this.getShaderTexture(this.lastMouseX, this.lastMouseY), this.getX(), this.getY(), textureX, textureY, this.width, this.height);
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FormattedCharSequence> list = new ArrayList<>();
        list.add(this.getName().getVisualOrderText());
        for (MutableComponent toolTip : this.getTooltipLines()) list.add(toolTip.getVisualOrderText());
        graphics.renderTooltip(Minecraft.getInstance().font, list, mouseX, mouseY);
    }

    public void blit(GuiGraphics graphics, int x, int y, int textureX, int textureY, int width, int height) {
        graphics.blit(this.getShaderTexture(this.lastMouseX, this.lastMouseY), x, y, textureX, textureY, width, height);
    }

    abstract protected MutableComponent getName();

    protected List<MutableComponent> getTooltipLines() {
        return List.of();
    }

    public void onSelectedTick() {
        this.time++;
    }

    public void onUnSelectedTick() {
        this.time = 1;
    }
}
