package io.github.jodlodi.minions.registry;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

// Kept separate from mod initialization because enum parameters are read during class transformation.
public final class MinionEnumParameters {
    public static final EnumProxy<Rarity> MASTER_RARITY = new EnumProxy<>(
            Rarity.class, -1, "minions_remastered:master", (UnaryOperator<Style>) style -> style.withColor(ChatFormatting.RED));

    private MinionEnumParameters() {}
}
