package io.github.jodlodi.minions;

import com.mojang.logging.LogUtils;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.DeedSounds;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import io.github.jodlodi.minions.registry.MinionEnumParameters;
import io.github.jodlodi.minions.registry.OrderRegistry;
import io.github.jodlodi.minions.registry.PacketRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MinionsRemastered.MODID)
public class MinionsRemastered {
    public static final String MODID = "minions_remastered";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Rarity RARITY = MinionEnumParameters.MASTER_RARITY.getValue();

    public MinionsRemastered(IEventBus eventBus, ModContainer container) {
        CommonRegistry.ENTITY_TYPES.register(eventBus);
        CommonRegistry.ITEMS.register(eventBus);
        CommonRegistry.ATTACHMENT_TYPES.register(eventBus);
        DeedSounds.SOUNDS.register(eventBus);
        container.registerConfig(ModConfig.Type.SERVER, MinionsConfig.SPEC);
        eventBus.addListener(PacketRegistry::registerPayloads);
        eventBus.addListener(CommonRegistry::addCreativeItems);
        OrderRegistry.init();
    }

    public static ResourceLocation locate(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }
}
