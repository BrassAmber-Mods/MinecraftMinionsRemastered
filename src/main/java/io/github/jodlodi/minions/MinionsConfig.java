package io.github.jodlodi.minions;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class MinionsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue EVIL_DEED_XP_COST;
    public static final ModConfigSpec.IntValue MINIONS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue FOOD_COST_ORDER;
    public static final ModConfigSpec.DoubleValue FOOD_COST_LIGHTNING;
    public static final ModConfigSpec.IntValue OFFLINE_RECALL_SECONDS;
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_TREE_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WORTHLESS_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_DEEDS;
    public static final ModConfigSpec.BooleanValue LEGACY_XP_EASTER_EGG;

    public static final ModConfigSpec.IntValue WORK_SCAN_BUDGET;
    public static final ModConfigSpec.IntValue MAX_TUNNEL_LENGTH;
    public static final ModConfigSpec.IntValue MAX_VEIN_BLOCKS;
    public static final ModConfigSpec.IntValue VEIN_RADIUS;
    public static final ModConfigSpec.IntValue UNREACHABLE_TICKS;
    public static final ModConfigSpec.BooleanValue WORKER_CHUNK_LOADING;
    public static final ModConfigSpec.IntValue MAX_WORKER_CHUNKS;
    public static final ModConfigSpec.BooleanValue MINE_ORE_VEINS;
    public static final ModConfigSpec.BooleanValue PLACE_TORCHES;
    public static final ModConfigSpec.BooleanValue REPAIR_TUNNELS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EVIL_DEED_XP_COST = builder.comment("Experience levels paid per evil deed. Set to -1 to disable evil deeds; 0 makes them free.")
                .defineInRange("evilDeedXPCost", 2, -1, 1000);
        MINIONS_PER_PLAYER = builder.defineInRange("minionsAmountPerPlayer", 4, 1, 16);
        FOOD_COST_ORDER = builder.comment("Cost for summoning and complex orders. Four units remove one food point directly; default 20 costs 2.5 drumsticks.")
                .defineInRange("foodCostBig", 20.0, 0.0, 100.0);
        FOOD_COST_LIGHTNING = builder.defineInRange("foodCostSmall", 1.5, 0.0, 20.0);
        OFFLINE_RECALL_SECONDS = builder.comment("Recall loaded workers after their owner is absent; cargo remains saved. Zero disables automatic recall.")
                .defineInRange("automaticDespawnDelay", 300, 0, 86400);
        FOLLOW_RANGE = builder.defineInRange("minionFollowRange", 30.0, 8.0, 64.0);
        EXTRA_TREE_BLOCKS = builder.defineListAllowEmpty("additionalTreeBlocks", List.of(), value -> value instanceof String);
        WORTHLESS_BLOCKS = builder.defineListAllowEmpty("additionalWorthlessBlocks", List.of(), value -> value instanceof String);
        CUSTOM_DEEDS = builder.comment("Extra or replacement deeds: id|Display name|namespace:sound_event|seconds (1..120). Clients need the matching audio resource pack.")
                .defineListAllowEmpty("customDeeds", List.of(), value -> value instanceof String);
        LEGACY_XP_EASTER_EGG = builder.comment("Allow the original iluvevil menu Easter egg, which awards 200 XP points.")
                .define("legacyXpEasterEgg", true);
        builder.push("workers");
        WORK_SCAN_BUDGET = builder.comment("Maximum source positions inspected per player per tick.")
                .defineInRange("scanBudget", 128, 16, 512);
        MAX_TUNNEL_LENGTH = builder.defineInRange("maxTunnelLength", 1024, 1, 4096);
        MAX_VEIN_BLOCKS = builder.comment("Maximum additional valuable-material targets per excavation layer/segment.")
                .defineInRange("maxVeinBlocks", 256, 1, 512);
        VEIN_RADIUS = builder.defineInRange("veinRadius", 16, 1, 32);
        UNREACHABLE_TICKS = builder.comment("Ticks without approaching a target before retrying it. Three attempts are allowed.")
                .defineInRange("unreachableTicks", 200, 40, 1200);
        WORKER_CHUNK_LOADING = builder.comment("Keep a bounded number of work/storage chunks ticking while their owner is online in that dimension.")
                .define("chunkLoading", true);
        MAX_WORKER_CHUNKS = builder.defineInRange("maxChunksPerPlayer", 12, 4, 32);
        MINE_ORE_VEINS = builder.define("followOreVeins", true);
        PLACE_TORCHES = builder.define("placeTorches", true);
        REPAIR_TUNNELS = builder.define("repairTunnels", true);
        builder.pop();
        SPEC = builder.build();
    }

    private MinionsConfig() {}
}
