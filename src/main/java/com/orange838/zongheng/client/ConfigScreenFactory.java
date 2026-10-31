package com.orange838.zongheng.client;

import com.orange838.zongheng.Config;
import com.orange838.zongheng.ZongHengRailway;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 游戏内图形化配置界面（Cloth Config）。
 *
 * <p>界面只读写 NeoForge 原生 {@code ModConfigSpec} 里已有的值，配置文件格式与内容完全不变，
 * 老存档和已经改过 toml 的玩家不受影响。保存时写回 toml 并立即重新应用，
 * 无需重启游戏。界面可按分类切换，每个开关和数值都带说明与「重置为默认」。
 *
 * <p>Cloth Config 是可选的：它不在时由 {@link ConfigScreens} 在探测阶段拦下，
 * 模组照常按 toml 运行，只是没有图形界面。
 */
public final class ConfigScreenFactory implements IConfigScreenFactory {

    private ConfigScreenFactory() {
    }

    /**
     * 注册 NeoForge 原生配置界面扩展点，挂到模组列表的「配置」按钮上。
     *
     * <p>调用方必须已经确认 Cloth Config 在场（见 {@link ConfigScreens}），
     * 否则本类在加载时就会因为找不到 Cloth 的类而失败。
     */
    static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, new ConfigScreenFactory());
        ZongHengRailway.LOGGER.info("In-game config screen registered via Cloth Config.");
    }

    @Override
    public Screen createScreen(ModContainer container, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.zongheng_railway.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        buildGenerationCategory(builder, entries);
        buildTrackCategory(builder, entries);
        buildSpawnerCategory(builder, entries);

        // 点「保存并退出」时写回 toml，并立刻重载一次，让静态字段取到新值。
        builder.setSavingRunnable(Config::saveAndReload);
        return builder.build();
    }

    /** 世界生成：总开关、概率、半径、跨区连通。 */
    private void buildGenerationCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.zongheng_railway.category.generation"));

        var everywhere = entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.generateRailwayEverywhere"),
                        Config.GENERATE_RAILWAY_EVERYWHERE.get())
                .setDefaultValue(Config.GENERATE_RAILWAY_EVERYWHERE.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.generateRailwayEverywhere.tooltip.1"),
                        Component.translatable("config.zongheng_railway.generateRailwayEverywhere.tooltip.2"))
                .setSaveConsumer(Config.GENERATE_RAILWAY_EVERYWHERE::set)
                .build();

        var probability = entries.startDoubleField(
                        Component.translatable("config.zongheng_railway.areaGenerationRailwayProbability"),
                        Config.AREA_GEN_RAILWAY_PROBABILITY.get())
                .setDefaultValue(Config.AREA_GEN_RAILWAY_PROBABILITY.getDefault())
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(
                        Component.translatable("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.1"),
                        Component.translatable("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.2"))
                .setSaveConsumer(Config.AREA_GEN_RAILWAY_PROBABILITY::set)
                .build();

        var distance = entries.startIntField(
                        Component.translatable("config.zongheng_railway.maxRegionDistance"),
                        Config.MAX_REGION_DISTANCE.get())
                .setMin(0)
                .setMax(100000)
                .setDefaultValue(Config.MAX_REGION_DISTANCE.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.maxRegionDistance.tooltip.1"),
                        Component.translatable("config.zongheng_railway.maxRegionDistance.tooltip.2"))
                .setSaveConsumer(Config.MAX_REGION_DISTANCE::set)
                .build();

        var connect = entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.connectAcrossRegions"),
                        Config.CONNECT_ACROSS_REGIONS.get())
                .setDefaultValue(Config.CONNECT_ACROSS_REGIONS.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.connectAcrossRegions.tooltip.1"),
                        Component.translatable("config.zongheng_railway.connectAcrossRegions.tooltip.2"))
                .setSaveConsumer(Config.CONNECT_ACROSS_REGIONS::set)
                .build();

        var preheat = entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.preheatAroundPlayer"),
                        Config.PREHEAT_AROUND_PLAYER.get())
                .setDefaultValue(Config.PREHEAT_AROUND_PLAYER.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.preheatAroundPlayer.tooltip.1"),
                        Component.translatable("config.zongheng_railway.preheatAroundPlayer.tooltip.2"))
                .setSaveConsumer(Config.PREHEAT_AROUND_PLAYER::set)
                .build();

        var preheatRadius = entries.startIntSlider(
                        Component.translatable("config.zongheng_railway.preheatRadius"),
                        Config.PREHEAT_RADIUS.get(), 0, 3)
                .setDefaultValue(Config.PREHEAT_RADIUS.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.preheatRadius.tooltip.1"),
                        Component.translatable("config.zongheng_railway.preheatRadius.tooltip.2"))
                .setSaveConsumer(Config.PREHEAT_RADIUS::set)
                .build();

        var planTimeout = entries.startIntField(
                        Component.translatable("config.zongheng_railway.planTimeoutSeconds"),
                        Config.PLAN_TIMEOUT_SECONDS.get())
                .setMin(0)
                .setMax(600)
                .setDefaultValue(Config.PLAN_TIMEOUT_SECONDS.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.planTimeoutSeconds.tooltip.1"),
                        Component.translatable("config.zongheng_railway.planTimeoutSeconds.tooltip.2"))
                .setSaveConsumer(Config.PLAN_TIMEOUT_SECONDS::set)
                .build();

        category.addEntry(everywhere);
        category.addEntry(probability);
        category.addEntry(distance);
        category.addEntry(connect);
        category.addEntry(preheat);
        category.addEntry(preheatRadius);
        category.addEntry(planTimeout);
    }

    /** 轨道：双线与间距，间距跟随双线开关。 */
    private void buildTrackCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.zongheng_railway.category.track"));

        var doubleTrack = entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.doubleTrackEnabled"),
                        Config.DOUBLE_TRACK.get())
                .setDefaultValue(Config.DOUBLE_TRACK.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.doubleTrackEnabled.tooltip.1"),
                        Component.translatable("config.zongheng_railway.doubleTrackEnabled.tooltip.2"))
                .setSaveConsumer(Config.DOUBLE_TRACK::set)
                .build();

        var spacing = entries.startIntSlider(
                        Component.translatable("config.zongheng_railway.trackSpacing"),
                        Config.TRACK_SPACING.get(), 2, 8)
                .setDefaultValue(Config.TRACK_SPACING.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.trackSpacing.tooltip.1"),
                        Component.translatable("config.zongheng_railway.trackSpacing.tooltip.2"))
                .setSaveConsumer(Config.TRACK_SPACING::set)
                .build();

        category.addEntry(doubleTrack);
        category.addEntry(spacing);
    }

    /** 铁轨生成器方块：开关、生成、取用方式。 */
    private void buildSpawnerCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.zongheng_railway.category.spawner"));

        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.enableTrackSpawner"),
                        Config.ENABLE_TRACK_SPAWNER.get())
                .setDefaultValue(Config.ENABLE_TRACK_SPAWNER.getDefault())
                .setTooltip(Component.translatable("config.zongheng_railway.enableTrackSpawner.tooltip"))
                .setSaveConsumer(Config.ENABLE_TRACK_SPAWNER::set)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.generateTrackSpawner"),
                        Config.GENERATE_TRACK_SPAWNER.get())
                .setDefaultValue(Config.GENERATE_TRACK_SPAWNER.getDefault())
                .setTooltip(Component.translatable("config.zongheng_railway.generateTrackSpawner.tooltip"))
                .setSaveConsumer(Config.GENERATE_TRACK_SPAWNER::set)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("config.zongheng_railway.placeTracksUsingTrackSpawner"),
                        Config.PLACE_TRACKS_USING_TRACK_SPAWNER.get())
                .setDefaultValue(Config.PLACE_TRACKS_USING_TRACK_SPAWNER.getDefault())
                .setTooltip(
                        Component.translatable("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.1"),
                        Component.translatable("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.2"))
                .setSaveConsumer(Config.PLACE_TRACKS_USING_TRACK_SPAWNER::set)
                .build());
    }
}
