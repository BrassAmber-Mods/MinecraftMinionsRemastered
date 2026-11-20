package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.orders.AbstractOrder;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.WorkerSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

public final class GameplayRules {
    private static final Map<ServerPlayer, Long> LAST_DENIED = new WeakHashMap<>();
    public static final TagKey<Block> WORTHLESS = TagKey.create(Registries.BLOCK, MinionsRemastered.locate("worthless"));
    private GameplayRules() {}

    public static boolean hasStaff(ServerPlayer player) {
        return player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) || player.getOffhandItem().is(CommonRegistry.MASTERS_STAFF.get());
    }

    public static boolean pay(ServerPlayer player, double exhaustion) {
        if (!canPay(player, exhaustion)) return false;
        if (!player.getAbilities().instabuild) player.causeFoodExhaustion((float)exhaustion);
        return true;
    }

    private static boolean canPay(ServerPlayer player, double exhaustion) {
        if (!CommandValidation.canCommand(player)) return false;
        if (player.getAbilities().instabuild || exhaustion == 0) return true;
        if (player.getFoodData().getFoodLevel() <= 3) {
            long now = player.level().getGameTime(); Long last = LAST_DENIED.get(player);
            if (last == null || now - last >= 16 || now < last) {
                LAST_DENIED.put(player, now);
                player.displayClientMessage(Component.translatable("message.minions_remastered.exhausted"), true);
                WorkerSounds.play(player, "fart");
            }
            return false;
        }
        return true;
    }

    public static boolean payForOrder(ServerPlayer player) {
        double exhaustion = MinionsConfig.FOOD_COST_ORDER.get();
        if (!canPay(player, exhaustion)) return false;
        if (player.getAbilities().instabuild || exhaustion == 0) return true;
        var data = player.getPersistentData();
        String remainderKey = MinionsRemastered.MODID + ":order_food_remainder";
        double cost = exhaustion / 4.0 + Math.clamp(data.getDouble(remainderKey), 0.0, 0.999999);
        int points = (int)cost;
        data.putDouble(remainderKey, cost - points);
        var food = player.getFoodData();
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - points));
        food.setSaturation(Math.min(food.getSaturationLevel(), food.getFoodLevel()));
        return true;
    }

    public static boolean isHoleWidth(long width, int maximum) {
        return width >= 3 && width <= maximum && (width & 1) != 0;
    }

    public static boolean start(ServerPlayer player, IMasterCapability master, AbstractOrder order) {
        if (master.minionCount() == 0 || !payForOrder(player)) return false;
        master.setOrder(order);
        WorkerSounds.order(player, order);
        return true;
    }

    public static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS) || MinionsConfig.EXTRA_TREE_BLOCKS.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    public static boolean isValuable(BlockState state) {
        return !state.isAir() && !state.is(WORTHLESS) && !state.is(BlockTags.LEAVES) && !state.hasBlockEntity()
                && state.getFluidState().isEmpty()
                && !MinionsConfig.WORTHLESS_BLOCKS.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
}
