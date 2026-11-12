package io.github.jodlodi.minions;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MinionsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue EVIL_DEED_XP_COST;

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
        builder.push("workers");
        WORK_SCAN_BUDGET = builder.comment("Maximum source positions inspected per player per tick.")
                .defineInRange("scanBudget", 128, 16, 512);
        MAX_TUNNEL_LENGTH = builder.defineInRange("maxTunnelLength", 1024, 1, 4096);
        MAX_VEIN_BLOCKS = builder.comment("Maximum additional ore targets per excavation layer/segment.")
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
