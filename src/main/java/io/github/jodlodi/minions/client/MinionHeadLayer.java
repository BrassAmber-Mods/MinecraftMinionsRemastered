package io.github.jodlodi.minions.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinionHeadLayer<T extends Minion, M extends MinionEntityModel<T>> extends CustomHeadLayer<T, M> {
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public MinionHeadLayer(RenderLayerParent<T, M> parent, EntityModelSet set, ItemInHandRenderer itemInHandRenderer) {
        super(parent, set, itemInHandRenderer);
        this.skullModels = SkullBlockRenderer.createSkullRenderers(set);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource source, int light, T minion, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = minion.getItemBySlot(EquipmentSlot.HEAD);
        if (!itemstack.isEmpty()) {
            Item item = itemstack.getItem();
            stack.pushPose();
            if (minion.isBaby()) {
                stack.translate(0.0D, 0.03125D, 0.0D);
                stack.scale(0.7F, 0.7F, 0.7F);
                stack.translate(0.0D, 1.0D, 0.0D);
            }

            this.getParentModel().translateToHead(stack);
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock) {
                stack.scale(1.1875F, -1.1875F, -1.1875F);

                ResolvableProfile profile = itemstack.get(DataComponents.PROFILE);

                stack.translate(-0.5D, 0.0D, -0.5D);
                SkullBlock.Type skullblock$type = ((AbstractSkullBlock) ((BlockItem) item).getBlock()).getType();
                SkullModelBase skullmodelbase = this.skullModels.get(skullblock$type);
                RenderType rendertype = SkullBlockRenderer.getRenderType(skullblock$type, profile);
                SkullBlockRenderer.renderSkull(null, 180.0F, limbSwing, stack, source, light, skullmodelbase, rendertype);
            } else if (!(item instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != EquipmentSlot.HEAD) {
                translateToHead(stack, false);
                this.itemInHandRenderer.renderItem(minion, itemstack, ItemDisplayContext.HEAD, false, stack, source, light);
            }

            stack.popPose();
        }
    }
}
