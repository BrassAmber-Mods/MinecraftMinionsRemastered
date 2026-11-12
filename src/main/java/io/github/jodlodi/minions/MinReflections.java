package io.github.jodlodi.minions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

public final class MinReflections {
    private static final MethodHandle AMBIENT_SOUND = findVirtual(Mob.class, "getAmbientSound", SoundEvent.class);
    private static final MethodHandle SOUND_VOLUME = findVirtual(LivingEntity.class, "getSoundVolume", float.class);

    private MinReflections() {}

    private static MethodHandle findVirtual(Class<?> owner, String name, Class<?> result) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findVirtual(owner, name, MethodType.methodType(result));
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Nullable
    public static SoundEvent getAmbientSound(Mob mob) {
        try {
            return (SoundEvent) AMBIENT_SOUND.invokeExact(mob);
        } catch (Throwable exception) {
            MinionsRemastered.LOGGER.warn("Unable to read entity ambient sound", exception);
            return null;
        }
    }

    public static float getSoundVolume(LivingEntity entity) {
        try {
            return (float) SOUND_VOLUME.invokeExact(entity);
        } catch (Throwable exception) {
            MinionsRemastered.LOGGER.warn("Unable to read entity sound volume", exception);
            return 1.0F;
        }
    }
}
