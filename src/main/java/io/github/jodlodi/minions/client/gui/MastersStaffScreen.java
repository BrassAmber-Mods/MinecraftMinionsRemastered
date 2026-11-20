package io.github.jodlodi.minions.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jodlodi.minions.MinReflections;
import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.client.gui.buttons.*;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.work.GameplayRules;
import io.github.jodlodi.minions.work.StaffActions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MastersStaffScreen extends Screen {
    public static final ResourceLocation TEXTURE_LOCATION = MinionsRemastered.locate("textures/gui/minion.png");
    public static final ResourceLocation ALT_TEXTURE_LOCATION = MinionsRemastered.locate("textures/gui/alt.png");
    // Layout uses Minecraft's scaled GUI coordinates; keep texture pixels at an integer scale.
    public static final int MENU_SCALE = 2;
    public static final int BUTTON_DISTANCE = 32 * MENU_SCALE;
    public static final int POINTER_DISTANCE = 8 * MENU_SCALE;
    private static final int POINTER_TEXTURE_SIZE = 27;
    private static final int CENTER_HIT_RADIUS = 10 * MENU_SCALE;

    private final AbstractMastersButton[] buttons = { null, null, null, null }; // [0]TOP, [1]RIGHT, [2]BOTTOM, [3]LEFT

    protected final HitResult context;
    protected final LocalPlayer player;

    protected float realTime = 0.0F;
    protected float time = 0.0F;
    protected float boost = 1.0F;
    protected float shade = 0.0F;

    public MastersStaffScreen(LocalPlayer player, HitResult context) {
        super(GameNarrator.NO_TITLE);
        this.player = player;
        this.context = context;
    }

    public HitResult getContext() {
        return this.context;
    }

    public LocalPlayer getPlayer() {
        return this.player;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        CommonRegistry.getMaster(this.player).ifPresent(capability -> {
            AbstractMastersButton[] buttonSetup = { null, null, null, null }; // [0]TOP, [1]RIGHT, [2]BOTTOM, [3]LEFT

            boolean secondary = this.player.isSecondaryUseActive();
            if (this instanceof BlockStaffScreen blockStaffScreen) { // BLOCK RIGHT CLICK
                // VALUES
                BlockHitResult hitResult = blockStaffScreen.getContext();
                BlockPos pos = hitResult.getBlockPos();
                BlockEntity blockEntity = this.player.level().getBlockEntity(pos);

                // FLAGS
                boolean minionSpace = capability.minionCount() < MinionsConfig.MINIONS_PER_PLAYER.get();
                if (capability.minionCount() > 0) buttonSetup[3] = new StaffActionButton(-BUTTON_DISTANCE, 0, this, pos,
                        StaffActions.MOVE, "gui.minions_remastered.move", StaffActions.FOLLOW, "gui.minions_remastered.follow");

                // TOP BUTTON // SUMMONING
                if (minionSpace && !secondary) {
                    buttonSetup[0] = new SummonButton(0, -BUTTON_DISTANCE, blockStaffScreen, capability);
                } else {
                    buttonSetup[0] = new CallAllMinionsButton(0, -BUTTON_DISTANCE, blockStaffScreen, capability);
                }

                // RIGHT BUTTON // STORAGE
                if (blockEntity != null && this.player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
                    buttonSetup[1] = new ContainerButton(BUTTON_DISTANCE, 0, this);
                }

                // BOTTOM BUTTON // ORDERS
                if (capability.getOrder() == null) {
                    if (GameplayRules.isTree(this.player.level().getBlockState(hitResult.getBlockPos()))) {
                        buttonSetup[2] = new ChopOrderButton(0, BUTTON_DISTANCE, blockStaffScreen);
                    } else if (this.player.level().getBlockState(hitResult.getBlockPos()).is(BlockTags.WART_BLOCKS)) {
                        buttonSetup[2] = new HoeWartButton(0, BUTTON_DISTANCE, blockStaffScreen);
                    } else if (GameplayRules.isValuable(this.player.level().getBlockState(pos))) {
                        buttonSetup[2] = new StaffActionButton(0, BUTTON_DISTANCE, this, pos, StaffActions.VEIN,
                                "gui.minions_remastered.vein", StaffActions.STRIP, "gui.minions_remastered.stripmine");
                    } else if (hitResult.getDirection().getAxis() == Direction.Axis.Y) {
                        buttonSetup[2] = new DigOrderButton(0, BUTTON_DISTANCE, blockStaffScreen);
                    } else {
                        buttonSetup[2] = new MineOrderButton(0, BUTTON_DISTANCE, blockStaffScreen);
                    }
                } else {
                    buttonSetup[2] = new StopOrderButton(0, BUTTON_DISTANCE, blockStaffScreen);
                }
            } else if (this instanceof EntityStaffScreen entityStaffScreen) { // ENTITY RIGHT CLICK
                if (entityStaffScreen.target == this.player) {
                    // RIGHT BUTTON // STORAGE
                    buttonSetup[1] = new ClearContainerButton(BUTTON_DISTANCE, 0, this);

                    // BOTTOM BUTTON // ORDERS
                    if (capability.getOrder() == null) {
                        buttonSetup[2] = new CarryLivingOrderButton(0, BUTTON_DISTANCE, entityStaffScreen);
                    } else {
                        buttonSetup[2] = new StopOrderButton(0, BUTTON_DISTANCE, entityStaffScreen);
                    }
                } else if (entityStaffScreen.target instanceof Minion minion) { // MINION
                    if (capability.isMinion(minion.getUUID())) {
                        // TOP BUTTON // SUMMONING
                        buttonSetup[0] = new BanishButton(0, -BUTTON_DISTANCE, entityStaffScreen, capability);

                        // BOTTOM BUTTON // ORDERS
                        buttonSetup[2] = new SitButton(0, BUTTON_DISTANCE, entityStaffScreen, capability);
                        buttonSetup[3] = new DropWorkerButton(-BUTTON_DISTANCE, 0, entityStaffScreen);
                    }
                } else { // FRIENDLY MOB
                    if (!entityStaffScreen.hostile) {
                        if (entityStaffScreen.target instanceof LivingEntity) {
                            buttonSetup[2] = capability.getOrder() == null ? new CarryLivingOrderButton(0, BUTTON_DISTANCE, entityStaffScreen)
                                    : new StopOrderButton(0, BUTTON_DISTANCE, entityStaffScreen);
                        }
                        if (entityStaffScreen.target.getCapability(Capabilities.ItemHandler.ENTITY) != null) {
                            // RIGHT BUTTON // STORAGE
                            buttonSetup[1] = new ContainerButton(BUTTON_DISTANCE, 0, this);

                            // BOTTOM BUTTON // ORDERS
                            if (capability.getOrder() == null) {
                                buttonSetup[2] = new CarryLivingOrderButton(0, BUTTON_DISTANCE, entityStaffScreen);
                            } else {
                                buttonSetup[2] = new StopOrderButton(0, BUTTON_DISTANCE, entityStaffScreen);
                            }
                        }
                    } else { // HOSTILE MOB

                    }
                }
            }

            boolean emptyFlag = true;
            for (int i = 0; i < buttonSetup.length; i++) {
                if (buttonSetup[i] != null) {
                    this.buttons[i] = this.addRenderableWidget(buttonSetup[i]);
                    emptyFlag = false;
                }
            }
            if (emptyFlag) this.onClose();
        });
    }

    public static MastersStaffScreen make(LocalPlayer player, HitResult context, boolean flag) {
        if (context instanceof EntityHitResult entityContext) {
            return new EntityStaffScreen(player, entityContext, flag);
        } else if (context instanceof BlockHitResult blockContext) {
            return new BlockStaffScreen(player, blockContext);
        }
        return new MastersStaffScreen(player, context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!clicked) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            if (mouseX > centerX - CENTER_HIT_RADIUS && mouseX < centerX + CENTER_HIT_RADIUS && mouseY > centerY - CENTER_HIT_RADIUS && mouseY < centerY + CENTER_HIT_RADIUS) {
                if (!this.isShiftKeyDown()) {
                    this.boost *= 2.0F;
                    this.boost += 3.0F;
                }
                if (this instanceof MastersStaffScreen.BlockStaffScreen screen) {
                    SoundType soundType = this.player.level().getBlockState(screen.getContext().getBlockPos()).getSoundType();
                    this.player.level().playLocalSound(this.player.getX(), this.player.getY(), this.player.getZ(), soundType.getHitSound(), this.player.getSoundSource(), (soundType.getVolume() + 1.0F) / 8.0F, soundType.getPitch() * 0.5F, false);
                } else if (this instanceof MastersStaffScreen.EntityStaffScreen screen && screen.target instanceof Mob mob && screen.ambientSound != null) {
                    this.player.level().playLocalSound(this.player.getX(), this.player.getY(), this.player.getZ(), screen.ambientSound, this.player.getSoundSource(), screen.soundVolume, mob.getVoicePitch(), false);
                }
            }
        }
        return clicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 1) {
            for (AbstractMastersButton button : this.buttons) {
                if (button != null && button.active && button.visible && button.isMouseOver(mouseX, mouseY)) {
                    button.playDownSound(Minecraft.getInstance().getSoundManager());
                    button.onClick(mouseX, mouseY);
                    return true;
                }
            }
            this.onClose();
            return true;
        } else return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void tick() {
        this.realTime += 1.0F;
        this.time += this.boost;
        this.boost *= 0.8F;

        if (this.isShiftKeyDown()) {
            if (this.shade < 1.0F) this.shade = Math.min(this.shade + 0.1F, 1.0F);
        } else if (this.shade > 0.0F) this.shade = Math.max(this.shade - 0.1F, 0.0F);

        this.boost = Math.max(this.boost, 1.0F - this.shade);

        if (this.minecraft == null) return;
        double x = this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
        double y = this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();
        for (int i = 0; i < 4; i++) {
            if (this.buttons[i] != null) {
                if (this.buttons[i].isMouseOver(x, y)) this.buttons[i].onSelectedTick();
                else this.buttons[i].onUnSelectedTick();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.setFocused(null);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.renderPointer(graphics, mouseX, mouseY, 0, 0, -POINTER_DISTANCE);
        this.renderPointer(graphics, mouseX, mouseY, 1, POINTER_DISTANCE, 0);
        this.renderPointer(graphics, mouseX, mouseY, 2, 0, POINTER_DISTANCE);
        this.renderPointer(graphics, mouseX, mouseY, 3, -POINTER_DISTANCE, 0);

        super.render(graphics, mouseX, mouseY, partialTick);
        if (this instanceof EntityStaffScreen entityStaffScreen) {
            this.renderEntity(graphics, entityStaffScreen.target, this.width / 2, this.height / 2, partialTick);
        } else if (this instanceof BlockStaffScreen blockStaffScreen) {
            this.renderSingleBlock(graphics, this.player.level().getBlockState(blockStaffScreen.getContext().getBlockPos()), this.width / 2, this.height / 2, partialTick);
        }
    }

    private void renderPointer(GuiGraphics graphics, int mouseX, int mouseY, int index, int offsetX, int offsetY) {
        AbstractMastersButton button = this.buttons[index];
        if (button == null) return;
        int size = POINTER_TEXTURE_SIZE * MENU_SCALE;
        graphics.blit(TEXTURE_LOCATION, this.width / 2 - size / 2 + offsetX, this.height / 2 - size / 2 + offsetY,
                size, size, button.isMouseOver(mouseX, mouseY) ? POINTER_TEXTURE_SIZE : 0, index * POINTER_TEXTURE_SIZE,
                POINTER_TEXTURE_SIZE, POINTER_TEXTURE_SIZE, 256, 256);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int rgb = 16;
        float mul = this.shade * 0.5F;
        int top = ((int)(192.0F * mul) << 24) | (rgb << 16) | (rgb << 8) | rgb;
        int bottom = ((int)(208.0F * mul) << 24) | (rgb << 16) | (rgb << 8) | rgb;
        graphics.fillGradient(0, 0, this.width, this.height, top, bottom);
    }

    public void renderSingleBlock(GuiGraphics graphics, BlockState state, double x, double y, float partialTick) {
        float spin = (this.time + partialTick) * 1.2F;
        float realTilt = (this.realTime + partialTick) * 0.15F;
        float tilt = (float)Math.sin(realTilt * 0.45F) * 4.0F;
        float scale = 0.85F * 26.0F * MENU_SCALE * ((float)(Math.cos(realTilt * 0.5F) + 1.0F) * 0.01F + 0.98F);
        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y + 12.0D * MENU_SCALE, 150.0D);
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(tilt));
            pose.mulPose(Axis.YP.rotationDegrees(spin));
            pose.translate(-0.5D, 0.0D, -0.5D);
            Lighting.setupForEntityInInventory();
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
            graphics.flush();
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }

    public void renderEntity(GuiGraphics graphics, Entity entity, double x, double y, float partialTick) {
        boolean livingFlag = entity instanceof LivingEntity;
        float height = Math.max(0.01F, entity.getBbHeight());
        float width = Math.max(0.01F, entity.getBbWidth());
        float scale = (livingFlag ? (float)Math.sqrt((Math.min(width / height, height / width) + 0.5D) * 0.5D) : 0.85F) * 26.0F * MENU_SCALE;
        float spin = (this.time + partialTick) * 1.2F;
        float realTilt = (this.realTime + partialTick) * 0.15F;
        float tilt = (float)Math.sin(realTilt * 0.45F) * 4.0F;
        scale *= (float)(Math.cos(realTilt * 0.5F) + 1.0F) * 0.01F + 0.98F;
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        float bodyRot = living != null ? living.yBodyRot : 0.0F;
        float headRot = living != null ? living.yHeadRot : 0.0F;
        float oldHeadRot = living != null ? living.yHeadRotO : 0.0F;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Quaternionf cameraOrientation = new Quaternionf(dispatcher.cameraOrientation());
        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y + ((livingFlag ? 1.5D * height : 0.0D) + 12.0D) * MENU_SCALE, 150.0D);
            pose.scale(scale, scale, -scale);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(tilt));
            entity.setYRot(livingFlag ? spin : 180.0F);
            if (living != null) {
                living.yBodyRot = spin;
                living.setXRot(0.0F);
                living.yHeadRot = spin;
                living.yHeadRotO = spin;
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(spin));
            }
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(Axis.XP.rotationDegrees(-tilt).rotateY((float)Math.PI));
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 180.0F, 1.0F, pose, graphics.bufferSource(), 15728880));
            graphics.flush();
        } finally {
            dispatcher.setRenderShadow(true);
            dispatcher.overrideCameraOrientation(cameraOrientation);
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            if (living != null) {
                living.yBodyRot = bodyRot;
                living.yHeadRot = headRot;
                living.yHeadRotO = oldHeadRot;
            }
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }

    public boolean isShiftKeyDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344);
    }

    public static class EntityStaffScreen extends MastersStaffScreen {
        public final Entity target;
        public final boolean hostile;
        public final SoundEvent ambientSound;
        public final float soundVolume;

        public EntityStaffScreen(LocalPlayer player, EntityHitResult context, boolean hostile) {
            super(player, context);
            this.target = context.getEntity();
            this.hostile = hostile;

            if (this.target instanceof Mob mob) {
                this.ambientSound = MinReflections.getAmbientSound(mob);
                this.soundVolume = MinReflections.getSoundVolume(mob);
            } else {
                this.ambientSound = null;
                this.soundVolume = 1.0F;
            }
        }

        @Override
        public EntityHitResult getContext() {
            return (EntityHitResult)this.context;
        }
    }

    public static class BlockStaffScreen extends MastersStaffScreen {
        public final BlockState target;

        public BlockStaffScreen(LocalPlayer player, BlockHitResult context) {
            super(player, context);
            this.target = player.level().getBlockState(context.getBlockPos());
        }

        @Override
        public BlockHitResult getContext() {
            return (BlockHitResult)this.context;
        }
    }
}
