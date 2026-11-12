package io.github.jodlodi.minions.registry;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.deeds.EvilDeed;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DeedSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MinionsRemastered.MODID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> DEEDS = EvilDeed.ALL.stream()
            .collect(Collectors.toUnmodifiableMap(EvilDeed::id, deed -> register("deed." + deed.id())));
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCEPTED = register("deed.accepted");
    public static final DeferredHolder<SoundEvent, SoundEvent> REWARDED = register("deed.rewarded");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(MinionsRemastered.locate(name)));
    }

    public static SoundEvent forDeed(EvilDeed deed) {
        return DEEDS.get(deed.id()).get();
    }

    private DeedSounds() {}
}
