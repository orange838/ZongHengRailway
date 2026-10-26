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
        this.add(ModBlocks.TRACK_SPAWNER.get(), "¹ìµÀË¢¹ÖÁý");
        this.add("commands.zongheng_railway.searchstation.already_searching", "\u5df2\u6709\u4e00\u4e2a\u8f66\u7ad9\u641c\u7d22\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\u3002");
        this.add("commands.zongheng_railway.searchstation.failed", "\u8f66\u7ad9\u641c\u7d22\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u670d\u52a1\u5668\u65e5\u5fd7\u3002");
        this.add("commands.zongheng_railway.searchstation.not_found", "\u5468\u56f4 %s x %s \u4e2a\u94c1\u8def\u89c4\u5212\u533a\u5185\u672a\u627e\u5230\u8f66\u7ad9\u3002");
        this.add("commands.zongheng_railway.searchstation.searching", "\u6b63\u5728\u5468\u56f4 %s x %s \u4e2a\u94c1\u8def\u89c4\u5212\u533a\u5185\u641c\u7d22\u6700\u8fd1\u8f66\u7ad9\u2026\u2026");
        this.add("commands.zongheng_railway.searchstation.success", "\u6700\u8fd1\u7684\u8f66\u7ad9\u5750\u6807\uff1a%s");
    }
}
