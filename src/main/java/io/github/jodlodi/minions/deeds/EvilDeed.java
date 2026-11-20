package io.github.jodlodi.minions.deeds;

import io.github.jodlodi.minions.MinionsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The original Minions default deeds and their audio durations, in seconds. */
public record EvilDeed(String id, int durationSeconds, String label, String sound) {
    public EvilDeed(String id, int durationSeconds) { this(id, durationSeconds, "", ""); }
    public static final List<EvilDeed> ALL = List.of(
            new EvilDeed("spawnsatanistcult", 3),
            new EvilDeed("gong", 1),
            new EvilDeed("corporatesupremacy", 4),
            new EvilDeed("baseballbatjingle", 3),
            new EvilDeed("fff", 8),
            new EvilDeed("keyboardcat", 3),
            new EvilDeed("romance", 7),
            new EvilDeed("republicans", 4),
            new EvilDeed("graveyard", 8),
            new EvilDeed("boomheadshot", 2),
            new EvilDeed("denied", 1),
            new EvilDeed("boo", 1),
            new EvilDeed("cornholio", 3),
            new EvilDeed("crysomemore", 3),
            new EvilDeed("evillaugh", 3),
            new EvilDeed("radios", 3),
            new EvilDeed("pfft", 2),
            new EvilDeed("nooo", 3),
            new EvilDeed("ohyeah", 8),
            new EvilDeed("babysealclubbing", 6),
            new EvilDeed("haha", 1),
            new EvilDeed("march", 5),
            new EvilDeed("chicken", 2),
            new EvilDeed("laughter2", 3),
            new EvilDeed("superstarme", 5),
            new EvilDeed("teabagnuns", 5),
            new EvilDeed("leeroy", 5));
    private static final Map<String, EvilDeed> BY_ID = ALL.stream().collect(Collectors.toUnmodifiableMap(EvilDeed::id, Function.identity()));
    private static List<? extends String> cachedCustom;
    private static Map<String, EvilDeed> configuredById = BY_ID;
    private static List<EvilDeed> configuredDeeds = ALL;

    public static synchronized EvilDeed byId(String id) {
        configured(); return configuredById.get(id);
    }

    public static synchronized List<EvilDeed> configured() {
        var custom = MinionsConfig.CUSTOM_DEEDS.get();
        if (custom.equals(cachedCustom)) return configuredDeeds;
        Map<String, EvilDeed> deeds = new LinkedHashMap<>();
        ALL.forEach(deed -> deeds.put(deed.id(), deed));
        for (String entry : custom.stream().limit(256).toList()) {
            String[] parts = entry.split("\\|", 4);
            if (parts.length != 4 || !parts[0].matches("[a-z0-9_.-]{1,64}") || parts[1].length() > 160
                    || ResourceLocation.tryParse(parts[2]) == null) continue;
            try {
                int duration = Integer.parseInt(parts[3]);
                if (duration >= 1 && duration <= 120) deeds.put(parts[0], new EvilDeed(parts[0], duration, parts[1], parts[2]));
            } catch (NumberFormatException ignored) {}
        }
        cachedCustom = List.copyOf(custom); configuredById = Map.copyOf(deeds); configuredDeeds = List.copyOf(deeds.values());
        return configuredDeeds;
    }

    public Component displayName() {
        return this.label.isEmpty() ? Component.translatable("deed.minions_remastered." + this.id) : Component.literal(this.label);
    }
}
