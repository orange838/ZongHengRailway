package com.orange838.zongheng;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = ZongHengRailway.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue GENERATE_RAILWAY_EVERYWHERE = BUILDER
            .comment("Master switch for full-world generation.",
                    "true  = every railway region generates a network (restores the pre-1.1.0 behaviour).",
                    "false = regions are gated by areaGenerationRailwayProbability and maxRegionDistance.")
            .define("generateRailwayEverywhere", true);

    private static final ModConfigSpec.DoubleValue AREA_GEN_RAILWAY_PROBABILITY = BUILDER
            .comment("Only used when generateRailwayEverywhere is false.",
                    "The probability of generating a railway in a region. 0 means never generate, 1 means always generate.")
            .defineInRange("areaGenerationRailwayProbability", 0.4, 0, 1);

    private static final ModConfigSpec.IntValue MAX_REGION_DISTANCE = BUILDER
            .comment("Only used when generateRailwayEverywhere is false.",
                    "How many regions away from the world origin may still generate railways.",
                    "One region is 4096 blocks, so 100 means a 409600 block radius.")
            .defineInRange("maxRegionDistance", 100, 0, 100000);

    private static final ModConfigSpec.BooleanValue ENABLE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner work")
            .define("enableTrackSpawner", true);

    private static final ModConfigSpec.BooleanValue GENERATE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner be generated")
            .define("generateTrackSpawner", true);

    private static final ModConfigSpec.BooleanValue PLACE_TRACKS_USING_TRACK_SPAWNER = BUILDER
            .comment("Use the track spawner,(true) or place rails during world generation.(false)")
            .define("placeTracksUsingTrackSpawner", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean generateRailwayEverywhere;
    public static double areaGenRailwayProbability;
    public static int maxRegionDistance;
    public static boolean enableTrackSpawner;
    public static boolean generateTrackSpawner;
    public static boolean useTrackSpawnerPlaceTrack;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        generateRailwayEverywhere = GENERATE_RAILWAY_EVERYWHERE.get();
        areaGenRailwayProbability = AREA_GEN_RAILWAY_PROBABILITY.get();
        maxRegionDistance = MAX_REGION_DISTANCE.get();
        enableTrackSpawner = ENABLE_TRACK_SPAWNER.get();
        generateTrackSpawner = GENERATE_TRACK_SPAWNER.get();
        useTrackSpawnerPlaceTrack = PLACE_TRACKS_USING_TRACK_SPAWNER.get();
    }
}
