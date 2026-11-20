package io.github.jodlodi.minions.client.gui.buttons;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.client.gui.MastersStaffScreen;
import io.github.jodlodi.minions.network.ChopButtonPacket;
import io.github.jodlodi.minions.orders.ChopOrder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChopOrderButton extends AbstractAdjustableOrderButton {
    protected int range;
    protected boolean allLogs;

    public ChopOrderButton(int x, int y, MastersStaffScreen.BlockStaffScreen screen) {
        super(x, y, 92, 57, screen, true, true, true);
        this.range = 12;
        this.allLogs = true;
    }

    @Override
    public MastersStaffScreen.BlockStaffScreen getScreen() {
        return (MastersStaffScreen.BlockStaffScreen)this.screen;
    }

    @Override
    public void onPress() {
        MastersStaffScreen.BlockStaffScreen screen = this.getScreen();
        LocalPlayer player = screen.getPlayer();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);

        int offset = this.range / 2;
        int trueSet = (this.range + 1) % 2;

        boolean south =  Direction.SOUTH.isFacingAngle(player.getViewYRot(partialTick));
        boolean east =  Direction.EAST.isFacingAngle(player.getViewYRot(partialTick));

        BlockPos context = screen.getContext().getBlockPos();
        BlockPos minPos = context.north(offset - (south ? trueSet : 0)).west(offset - (east ? trueSet : 0)).below(32);
        BlockPos maxPos = context.south(offset - (!south ? trueSet : 0)).east(offset - (!east ? trueSet : 0)).above(32);

        PacketDistributor.sendToServer(new ChopButtonPacket(this.screen.isShiftKeyDown(), minPos, maxPos, this.allLogs ? null : this.getScreen().target.getBlock()));

        this.screen.onClose();
    }

    @Override
    public void onSelectedTick() {
        super.onSelectedTick();
        MastersStaffScreen.BlockStaffScreen screen = this.getScreen();
        LocalPlayer player = screen.getPlayer();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        ClientLevel level = player.clientLevel;
        int offset = this.range / 2;
        int trueSet = (this.range + 1) % 2;

        boolean south =  Direction.SOUTH.isFacingAngle(player.getViewYRot(partialTick));
        boolean east =  Direction.EAST.isFacingAngle(player.getViewYRot(partialTick));

        BlockPos context = screen.getContext().getBlockPos();
        BlockPos minPos = context.north(offset - (south ? trueSet : 0)).west(offset - (east ? trueSet : 0)).below(32);
        BlockPos maxPos = context.south(offset - (!south ? trueSet : 0)).east(offset - (!east ? trueSet : 0)).above(32);

        MinUtil.particlesAroundTag(player.getRandom(), level, minPos, maxPos, this.allLogs ? ChopOrder.DEFAULT_CHOP : (blockState) -> blockState.is(this.getScreen().target.getBlock()));
    }

    @Override
    protected void onLeftPress() {
        this.range = Math.max(1, this.range - (this.screen.isShiftKeyDown() ? 5 : 1));
    }

    @Override
    protected void onTogglePress() {
        this.allLogs = !this.allLogs;
    }

    @Override
    protected void onRightPress() {
        this.range = Math.min(32, this.range + (this.screen.isShiftKeyDown() ? 5 : 1));
    }

    @Override
    protected MutableComponent getName() {
        return Component.translatable("gui.minions_remastered.harvest_wood").withStyle(ChatFormatting.BLUE);
    }

    @Override
    protected List<MutableComponent> getAdjustableTooltip() {
        return List.of(
                Component.translatable("gui.minions_remastered.range", this.range).withStyle(ChatFormatting.GRAY),
                (this.allLogs ? Component.translatable("gui.minions_remastered.harvest_all_logs")
                        : Component.translatable("gui.minions_remastered.harvest_only", this.getScreen().target.getBlock().getName())).withStyle(ChatFormatting.GRAY));

    }
}
