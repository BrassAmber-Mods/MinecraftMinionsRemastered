package io.github.jodlodi.minions.deeds;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;

/** The original Minions default deeds and their audio durations, in seconds. */
public record EvilDeed(String id, int durationSeconds) {
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

    public static EvilDeed byId(String id) {
        return BY_ID.get(id);
    }

    public Component displayName() {
        return Component.translatable("deed.minions_remastered." + this.id);
    }
}
