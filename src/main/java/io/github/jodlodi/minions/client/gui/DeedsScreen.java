package io.github.jodlodi.minions.client.gui;

import io.github.jodlodi.minions.deeds.DeedProgress;
import io.github.jodlodi.minions.deeds.EvilDeed;
import io.github.jodlodi.minions.network.CommitDeedPacket;
import io.github.jodlodi.minions.network.DeedsScreenPacket;
import io.github.jodlodi.minions.network.OpenDeedsPacket;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class DeedsScreen extends Screen {
    private final DeedsScreenPacket state;
    private final List<Button> deedButtons = new ArrayList<>();
    private Button commitButton;
    private boolean choosingDeed;
    private int remainingTicks;
    private boolean requestedRefresh;
    private int cheatProgress;

    public DeedsScreen(DeedsScreenPacket state) {
        super(Component.translatable("gui.minions_remastered.deeds.title"));
        this.state = state;
        this.remainingTicks = state.remainingTicks();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.deedButtons.clear();
        this.commitButton = null;
        int width = Math.min(220, this.width - 32);
        int left = (this.width - width) / 2;
        if (this.choosingDeed) {
            int y = this.height / 2 - 20;
            for (String id : this.state.options()) {
                EvilDeed deed = EvilDeed.byId(id);
                if (deed == null) continue;
                this.deedButtons.add(this.addRenderableWidget(Button.builder(deed.displayName(), button -> {
                    PacketDistributor.sendToServer(new CommitDeedPacket(this.state.offerId(), deed.id()));
                    this.onClose();
                }).bounds(left, y, width, 20).build()));
                y += 28;
            }
        } else {
            this.commitButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.minions_remastered.deeds.commit"), button -> {
                this.choosingDeed = true;
                this.rebuildWidgets();
            }).bounds(left, this.height / 2, width, 20).build());
            if (this.minecraft != null && this.minecraft.player != null && CommonRegistry.getMaster(this.minecraft.player)
                    .map(master -> master.minionCount() > 0).orElse(false)) {
                this.addRenderableWidget(Button.builder(Component.translatable("gui.minions_remastered.orders"), button -> this.minecraft.setScreen(new WorkerOrdersScreen()))
                        .bounds(left, this.height / 2 + 44, width, 20).build());
            }
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.minions_remastered.deeds.cancel"), button -> this.onClose())
                .bounds(left, this.height / 2 + 72, width, 20).build());
        this.updateAvailability();
    }

    private boolean canCommit() {
        return this.state.xpCost() >= 0 && this.remainingTicks == 0 && this.state.options().size() == 3
                && this.minecraft != null && this.minecraft.player != null && this.minecraft.player.experienceLevel >= this.state.xpCost();
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        String phrase = "iluvevil";
        char letter = Character.toLowerCase(codePoint);
        this.cheatProgress = letter == phrase.charAt(this.cheatProgress) ? this.cheatProgress + 1 : letter == 'i' ? 1 : 0;
        if (this.cheatProgress == phrase.length()) {
            this.cheatProgress = 0;
            PacketDistributor.sendToServer(new CommitDeedPacket(this.state.offerId(), "__legacy_cheat"));
            this.onClose(); return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void updateAvailability() {
        boolean available = this.canCommit();
        if (this.commitButton != null) this.commitButton.active = available;
        for (Button button : this.deedButtons) button.active = available;
    }

    @Override
    public void tick() {
        if (this.remainingTicks > 0 && --this.remainingTicks == 0 && !this.requestedRefresh) {
            this.requestedRefresh = true;
            PacketDistributor.sendToServer(new OpenDeedsPacket());
        }
        this.updateAvailability();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render draws the blurred background before its widgets.
        super.render(graphics, mouseX, mouseY, partialTick);
        int center = this.width / 2;
        int top = this.height / 2 - 80;
        graphics.drawCenteredString(this.font, this.choosingDeed ? Component.translatable("gui.minions_remastered.deeds.choose") : this.title, center, top, 0xFFFFFF);
        Component progress = this.state.rewardPending()
                ? Component.translatable("gui.minions_remastered.deeds.reward_pending")
                : Component.translatable("gui.minions_remastered.deeds.progress", this.state.completedDeeds(), DeedProgress.DEEDS_PER_STAFF);
        graphics.drawCenteredString(this.font, progress, center, top + 18, 0xAAAAAA);
        if (this.state.xpCost() < 0) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.minions_remastered.deeds.disabled"), center, top + 34, 0xFF5555);
        } else if (this.remainingTicks > 0) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.minions_remastered.deeds.in_progress", (this.remainingTicks + 19) / 20), center, top + 34, 0xAAAAAA);
        } else {
            Component cost = Component.translatable("gui.minions_remastered.deeds.cost", this.state.xpCost())
                    .withStyle(this.canCommit() ? ChatFormatting.GREEN : ChatFormatting.RED);
            graphics.drawCenteredString(this.font, cost, center, top + 34, 0xFFFFFF);
        }
    }
}
