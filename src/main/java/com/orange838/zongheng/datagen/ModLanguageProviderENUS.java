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
    }
}
