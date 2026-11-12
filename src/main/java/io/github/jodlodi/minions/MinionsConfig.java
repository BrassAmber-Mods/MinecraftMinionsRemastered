package io.github.jodlodi.minions;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MinionsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue EVIL_DEED_XP_COST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EVIL_DEED_XP_COST = builder.comment("Experience levels paid per evil deed. Set to -1 to disable evil deeds; 0 makes them free.")
                .defineInRange("evilDeedXPCost", 2, -1, 1000);
        SPEC = builder.build();
    }

    private MinionsConfig() {}
}
