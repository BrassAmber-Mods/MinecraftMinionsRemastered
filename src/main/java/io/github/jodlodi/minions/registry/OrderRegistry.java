package io.github.jodlodi.minions.registry;

import io.github.jodlodi.minions.orders.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class OrderRegistry {
    public static final Map<ResourceLocation, Function<CompoundTag, AbstractOrder>> ORDERS = new HashMap<>();

    public static void init() {
        ORDERS.put(MineDownOrder.ID, MineDownOrder::new);
        ORDERS.put(CarryLivingOrder.ID, CarryLivingOrder::new);
        ORDERS.put(MineAheadOrder.ID, MineAheadOrder::new);
        ORDERS.put(ChopOrder.ID, ChopOrder::new);
        ORDERS.put(HoeWartOrder.ID, HoeWartOrder::new);
        ORDERS.put(VeinOrder.ID, VeinOrder::new);
        ORDERS.put(MoveOrder.ID, MoveOrder::new);
        ORDERS.put(ExcavateOrder.ID, ExcavateOrder::new);
        ORDERS.put(StripMineOrder.ID, StripMineOrder::new);
    }
}
