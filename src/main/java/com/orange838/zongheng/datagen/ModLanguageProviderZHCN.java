package com.orange838.zongheng.datagen;

import com.orange838.zongheng.ZongHengRailway;
import com.orange838.zongheng.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProviderZHCN extends LanguageProvider {
    public ModLanguageProviderZHCN(PackOutput output) {
        super(output, ZongHengRailway.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "轨道刷怪笼");
        this.add("commands.zongheng_railway.searchstation.already_searching", "\u5df2\u6709\u4e00\u4e2a\u8f66\u7ad9\u641c\u7d22\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\u3002");
        this.add("commands.zongheng_railway.searchstation.failed", "\u8f66\u7ad9\u641c\u7d22\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u670d\u52a1\u5668\u65e5\u5fd7\u3002");
        this.add("commands.zongheng_railway.searchstation.not_found", "\u5468\u56f4 %s x %s \u4e2a\u94c1\u8def\u89c4\u5212\u533a\u5185\u672a\u627e\u5230\u8f66\u7ad9\u3002");
        this.add("commands.zongheng_railway.searchstation.searching", "\u6b63\u5728\u5468\u56f4 %s x %s \u4e2a\u94c1\u8def\u89c4\u5212\u533a\u5185\u641c\u7d22\u6700\u8fd1\u8f66\u7ad9\u2026\u2026");
        this.add("commands.zongheng_railway.searchstation.success", "\u6700\u8fd1\u7684\u8f66\u7ad9\u5750\u6807\uff1a%s");

        // 配置界面（Cloth Config）
        this.add("config.zongheng_railway.title", "纵横铁路");
        this.add("config.zongheng_railway.category.generation", "世界生成");
        this.add("config.zongheng_railway.category.track", "轨道");
        this.add("config.zongheng_railway.category.spawner", "铁轨生成器");
        this.add("config.zongheng_railway.generateRailwayEverywhere", "全世界生成铁路");
        this.add("config.zongheng_railway.generateRailwayEverywhere.tooltip.1", "全世界生成的总开关。");
        this.add("config.zongheng_railway.generateRailwayEverywhere.tooltip.2", "关闭后由下面的生成概率与区域半径决定哪些区域生成。");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability", "生成概率");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.1", "单个区域生成铁路网的几率。");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.2", "只在「全世界生成铁路」关闭时生效。0 为从不生成，1 为必定生成。");
        this.add("config.zongheng_railway.maxRegionDistance", "区域半径");
        this.add("config.zongheng_railway.maxRegionDistance.tooltip.1", "距离世界原点多少个区域以内仍会生成铁路。");
        this.add("config.zongheng_railway.maxRegionDistance.tooltip.2", "只在「全世界生成铁路」关闭时生效。一个区域为 4096 格。");
        this.add("config.zongheng_railway.connectAcrossRegions", "相邻区域路网连通");
        this.add("config.zongheng_railway.connectAcrossRegions.tooltip.1", "相邻两个区域的路网在公共边界处对接。");
        this.add("config.zongheng_railway.connectAcrossRegions.tooltip.2", "关闭后每个 4096×4096 区域各自独立成网，互不相连。");
        this.add("config.zongheng_railway.doubleTrackEnabled", "双线轨道");
        this.add("config.zongheng_railway.doubleTrackEnabled.tooltip.1", "每条线路生成两条平行轨道，而不是单条中线。");
        this.add("config.zongheng_railway.doubleTrackEnabled.tooltip.2", "关闭后恢复原来的单线。");
        this.add("config.zongheng_railway.trackSpacing", "双线间距");
        this.add("config.zongheng_railway.trackSpacing.tooltip.1", "两条平行轨道之间相隔的方块数。");
        this.add("config.zongheng_railway.trackSpacing.tooltip.2", "只在「双线轨道」开启时生效。3 能让两条轨道都落在窄桥模板内。");
        this.add("config.zongheng_railway.enableTrackSpawner", "启用铁轨生成器");
        this.add("config.zongheng_railway.enableTrackSpawner.tooltip", "铁轨生成器方块能否使用。");
        this.add("config.zongheng_railway.generateTrackSpawner", "生成铁轨生成器");
        this.add("config.zongheng_railway.generateTrackSpawner.tooltip", "世界中是否会出现铁轨生成器方块。");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner", "由生成器铺设铁轨");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.1", "开启时，铁轨在生成器运行时才铺设。");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.2", "关闭时，铁轨在世界生成阶段直接铺好。");
    }
}
