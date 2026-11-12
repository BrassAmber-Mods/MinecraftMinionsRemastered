package io.github.jodlodi.minions.registry;

import io.github.jodlodi.minions.MastersStaff;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.capabilities.MasterCapabilityHandler;
import io.github.jodlodi.minions.minion.Minion;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

@EventBusSubscriber(modid = MinionsRemastered.MODID)
public class CommonRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MinionsRemastered.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MinionsRemastered.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MinionsRemastered.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MasterCapabilityHandler>> MASTER_DATA = ATTACHMENT_TYPES.register(
            "master", () -> AttachmentType.serializable(holder -> new MasterCapabilityHandler((Player) holder)).copyOnDeath().build());

    public static final DeferredHolder<EntityType<?>, EntityType<Minion>> MINION = ENTITY_TYPES.register("minion", () ->
            EntityType.Builder.<Minion>of((type, level) -> new Minion(level), MobCategory.CREATURE)
                    .sized(Minion.DEFAULT_WIDTH, Minion.DEFAULT_HEIGHT)
                    .clientTrackingRange(80)
                    .fireImmune()
                    .updateInterval(3).build("minion"));

    public static final DeferredHolder<Item, Item> MINION_EGG = ITEMS.register("minion_spawn_egg", () -> new DeferredSpawnEggItem(MINION, 0xA03333, 0x3B3836, new Item.Properties()));
    public static final DeferredHolder<Item, Item> MASTERS_STAFF = ITEMS.register("masters_staff", () -> new MastersStaff(new Item.Properties().rarity(MinionsRemastered.RARITY)));

    public static Optional<IMasterCapability> getMaster(Entity entity) {
        return entity instanceof Player player ? Optional.of(player.getData(MASTER_DATA)) : Optional.empty();
    }

    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(MINION.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.SUBMERGED_MINING_SPEED)
                .add(Attributes.STEP_HEIGHT, 1.15D).build());
    }

    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(MINION_EGG.get());
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(MASTERS_STAFF.get());
    }
}
