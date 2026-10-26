package com.orange838.zongheng;

import com.orange838.zongheng.blocks.ModBlockEntities;
import com.orange838.zongheng.blocks.ModBlocks;
import com.orange838.zongheng.blocks.TrackSpawnerBlockRenderer;
import com.orange838.zongheng.datagen.ModDataGen;
import com.orange838.zongheng.event.FeatureRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ZongHengRailway.MODID)
public class ZongHengRailway {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "zongheng_railway";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Mod常量
    public static final int CHUNK_GROUP_SIZE = 256;  // 一个路线生成区域的大小
    public static final int HEIGHT_MAX_INCREMENT = 50;  // 路线生成最大高度相对于海平面的增量

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ZongHengRailway(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(FeatureRegistry::register);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(ModDataGen::gatherData);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ZongHengRailway) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
    }

//    @SubscribeEvent
//    public void onServerStopping(ServerStoppingEvent event) {
//        RailwayBuilder.clearAll();
//    }
//
//    @SubscribeEvent
//    public void onServerStopped(ServerStoppedEvent event) {
//        RailwayBuilder.clearAll();
//    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code

            event.enqueueWork(() -> {
                // 注册方块实体渲染器
                BlockEntityRenderers.register(ModBlockEntities.TRACK_SPAWNER.get(), TrackSpawnerBlockRenderer::new);
            });
        }
    }
}
