package io.github.jodlodi.minions.registry;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.orders.AbstractOrder;
import io.github.jodlodi.minions.orders.CarryLivingOrder;
import io.github.jodlodi.minions.orders.ChopOrder;
import io.github.jodlodi.minions.orders.MineDownOrder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.Map;

public final class WorkerSounds {
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new HashMap<>();
    static {
        for (String id : new String[]{"bolt", "fart", "foryou", "grabanimalorder", "minionspawn", "minionsqueak", "orderfollowplayer", "ordermineshaft", "ordertreecutting", "randomorder"}) {
            EVENTS.put(id, DeedSounds.SOUNDS.register("worker." + id,
                    () -> SoundEvent.createVariableRangeEvent(MinionsRemastered.locate("worker." + id))));
        }
    }
    private WorkerSounds() {}
    public static void init() {}
    public static void play(Entity entity, String id) {
        if (!entity.level().isClientSide) entity.level().playSound(null, entity.blockPosition(), EVENTS.get(id).get(), SoundSource.PLAYERS, 1, 1);
    }
    public static void order(Entity entity, AbstractOrder order) {
        play(entity, order instanceof ChopOrder ? "ordertreecutting" : order instanceof MineDownOrder ? "ordermineshaft"
                : order instanceof CarryLivingOrder ? "grabanimalorder" : "randomorder");
    }
}
