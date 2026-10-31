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

    public static final ModConfigSpec.BooleanValue GENERATE_RAILWAY_EVERYWHERE = BUILDER
            .comment("Master switch for full-world generation.",
                    "true  = every railway region generates a network (restores the pre-1.1.0 behaviour).",
                    "false = regions are gated by areaGenerationRailwayProbability and maxRegionDistance.")
            .define("generateRailwayEverywhere", true);

    public static final ModConfigSpec.DoubleValue AREA_GEN_RAILWAY_PROBABILITY = BUILDER
            .comment("Only used when generateRailwayEverywhere is false.",
                    "The probability of generating a railway in a region. 0 means never generate, 1 means always generate.")
            .defineInRange("areaGenerationRailwayProbability", 0.4, 0, 1);

    public static final ModConfigSpec.IntValue MAX_REGION_DISTANCE = BUILDER
            .comment("Only used when generateRailwayEverywhere is false.",
                    "How many regions away from the world origin may still generate railways.",
                    "One region is 4096 blocks, so 100 means a 409600 block radius.")
            .defineInRange("maxRegionDistance", 100, 0, 100000);

    public static final ModConfigSpec.BooleanValue CONNECT_ACROSS_REGIONS = BUILDER
            .comment("Connect the railway networks of neighbouring regions at their shared border.",
                    "跨区连通：相邻两个区域的路网在公共边界处对接，默认开启。")
            .define("connectAcrossRegions", true);

    public static final ModConfigSpec.BooleanValue DOUBLE_TRACK = BUILDER
            .comment("Generate two parallel tracks instead of a single centre line.",
                    "false = keep the original single track.",
                    "true  = lay two tracks spaced by trackSpacing blocks.")
            .define("doubleTrackEnabled", true);

    public static final ModConfigSpec.IntValue TRACK_SPACING = BUILDER
            .comment("Only used when doubleTrackEnabled is true.",
                    "Distance in blocks between the two parallel tracks.",
                    "3 keeps both tracks inside the narrow bridge templates.")
            .defineInRange("trackSpacing", 3, 2, 8);

    public static final ModConfigSpec.BooleanValue ENABLE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner work")
            .define("enableTrackSpawner", true);

    public static final ModConfigSpec.BooleanValue GENERATE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner be generated")
            .define("generateTrackSpawner", true);

    public static final ModConfigSpec.BooleanValue PLACE_TRACKS_USING_TRACK_SPAWNER = BUILDER
            .comment("Use the track spawner,(true) or place rails during world generation.(false)")
            .define("placeTracksUsingTrackSpawner", true);

    public static final ModConfigSpec.BooleanValue PREHEAT_AROUND_PLAYER = BUILDER
            .comment("Pre-plan the railway regions around a newly entered region.",
                    "进入一个新的铁路区域时，顺便把周围一圈区域的路网提前算好。",
                    "只提前做规划（计算），不会在未加载的区块里凭空放置铁轨。")
            .define("preheatAroundPlayer", true);

    public static final ModConfigSpec.IntValue PREHEAT_RADIUS = BUILDER
            .comment("Only used when preheatAroundPlayer is true.",
                    "How many regions around the current one are pre-planned. 1 = 3x3 regions (12288x12288 blocks).",
                    "只在「预热周边区域」开启时生效。0 表示只规划当前区域，1 表示周围 3x3 个区域。")
            .defineInRange("preheatRadius", 1, 0, 3);

    public static final ModConfigSpec.IntValue PLAN_TIMEOUT_SECONDS = BUILDER
            .comment("How long chunk generation may wait for a region plan before giving up.",
                    "0 = wait forever (the original behaviour).",
                    "区块生成等待区域规划的最长秒数。超时后该区块先正常生成，这一段暂时没有铁轨，",
                    "但规划仍在后台继续，走开再回来就会补上。0 表示一直等（原版行为）。")
            .defineInRange("planTimeoutSeconds", 60, 0, 600);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean generateRailwayEverywhere;
    public static double areaGenRailwayProbability;
    public static int maxRegionDistance;
    public static boolean connectAcrossRegions;
    public static boolean doubleTrackEnabled;
    public static int trackSpacing;
    public static boolean enableTrackSpawner;
    public static boolean generateTrackSpawner;
    public static boolean useTrackSpawnerPlaceTrack;
    public static boolean preheatAroundPlayer;
    public static int preheatRadius;
    public static int planTimeoutSeconds;

    /**
     * 把当前值写回配置文件，并立即重新应用一次。
     *
     * <p>供游戏内配置界面在点「保存并退出」时调用：先算好各配置项的新值（尚未写入静态字段），
     * 再保存文件，最后统一刷新一次静态字段，避免读到半旧半新的状态。
     */
    public static void saveAndReload() {
        try {
            SPEC.save();
        } catch (Exception e) {
            ZongHengRailway.LOGGER.error("Failed to save the ZongHeng Railway config file", e);
            return;
        }
        applyValues();
    }

    /** 把 SPEC 里的当前值同步到静态字段，世界生成代码只读这些字段。 */
    private static void applyValues() {
        generateRailwayEverywhere = GENERATE_RAILWAY_EVERYWHERE.get();
        areaGenRailwayProbability = AREA_GEN_RAILWAY_PROBABILITY.get();
        maxRegionDistance = MAX_REGION_DISTANCE.get();
        connectAcrossRegions = CONNECT_ACROSS_REGIONS.get();
        doubleTrackEnabled = DOUBLE_TRACK.get();
        trackSpacing = TRACK_SPACING.get();
        enableTrackSpawner = ENABLE_TRACK_SPAWNER.get();
        generateTrackSpawner = GENERATE_TRACK_SPAWNER.get();
        useTrackSpawnerPlaceTrack = PLACE_TRACKS_USING_TRACK_SPAWNER.get();
        preheatAroundPlayer = PREHEAT_AROUND_PLAYER.get();
        preheatRadius = PREHEAT_RADIUS.get();
        planTimeoutSeconds = PLAN_TIMEOUT_SECONDS.get();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        applyValues();
    }
}
