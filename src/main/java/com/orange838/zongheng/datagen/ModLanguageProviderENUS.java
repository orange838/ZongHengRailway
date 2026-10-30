package com.orange838.zongheng.datagen;

import com.orange838.zongheng.ZongHengRailway;
import com.orange838.zongheng.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProviderENUS extends LanguageProvider {
    public ModLanguageProviderENUS(PackOutput output) {
        super(output, ZongHengRailway.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "Track Spawner");
        this.add("commands.zongheng_railway.searchstation.already_searching", "A station search is already running.");
        this.add("commands.zongheng_railway.searchstation.failed", "Station search failed. Check the server log for details.");
        this.add("commands.zongheng_railway.searchstation.not_found", "No station was found in the surrounding %s x %s railway regions.");
        this.add("commands.zongheng_railway.searchstation.searching", "Searching the surrounding %s x %s railway regions for the nearest station...");
        this.add("commands.zongheng_railway.searchstation.success", "Nearest station coordinates: %s");

        // ≈‰÷√ΩÁ√Ê£®Cloth Config£©
        this.add("config.zongheng_railway.title", "ZongHeng Railway");
        this.add("config.zongheng_railway.category.generation", "World Generation");
        this.add("config.zongheng_railway.category.track", "Tracks");
        this.add("config.zongheng_railway.category.spawner", "Track Spawner");
        this.add("config.zongheng_railway.generateRailwayEverywhere", "Generate railway everywhere");
        this.add("config.zongheng_railway.generateRailwayEverywhere.tooltip.1", "Master switch for full-world generation.");
        this.add("config.zongheng_railway.generateRailwayEverywhere.tooltip.2", "When off, generation is limited by the probability and the region radius below.");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability", "Generation probability");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.1", "Chance that a region generates a railway network.");
        this.add("config.zongheng_railway.areaGenerationRailwayProbability.tooltip.2", "Only used when \"Generate railway everywhere\" is off. 0 = never, 1 = always.");
        this.add("config.zongheng_railway.maxRegionDistance", "Region radius");
        this.add("config.zongheng_railway.maxRegionDistance.tooltip.1", "How many regions away from the world origin may still generate railways.");
        this.add("config.zongheng_railway.maxRegionDistance.tooltip.2", "Only used when \"Generate railway everywhere\" is off. One region is 4096 blocks.");
        this.add("config.zongheng_railway.connectAcrossRegions", "Connect neighbouring regions");
        this.add("config.zongheng_railway.connectAcrossRegions.tooltip.1", "Joins the railway networks of adjacent regions at their shared border.");
        this.add("config.zongheng_railway.connectAcrossRegions.tooltip.2", "Off = every 4096x4096 region keeps its own separate network.");
        this.add("config.zongheng_railway.doubleTrackEnabled", "Double track");
        this.add("config.zongheng_railway.doubleTrackEnabled.tooltip.1", "Lays two parallel tracks instead of a single centre line.");
        this.add("config.zongheng_railway.doubleTrackEnabled.tooltip.2", "Off = the original single track.");
        this.add("config.zongheng_railway.trackSpacing", "Track spacing");
        this.add("config.zongheng_railway.trackSpacing.tooltip.1", "Distance in blocks between the two parallel tracks.");
        this.add("config.zongheng_railway.trackSpacing.tooltip.2", "Only used when \"Double track\" is on. 3 keeps both tracks inside the narrow bridge templates.");
        this.add("config.zongheng_railway.enableTrackSpawner", "Enable track spawner");
        this.add("config.zongheng_railway.enableTrackSpawner.tooltip", "Whether the track spawner block is usable.");
        this.add("config.zongheng_railway.generateTrackSpawner", "Generate track spawner");
        this.add("config.zongheng_railway.generateTrackSpawner.tooltip", "Whether track spawner blocks appear in the world.");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner", "Place rails with the track spawner");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.1", "On = rails are placed when the track spawner runs.");
        this.add("config.zongheng_railway.placeTracksUsingTrackSpawner.tooltip.2", "Off = rails are placed directly during world generation.");
    }
}
