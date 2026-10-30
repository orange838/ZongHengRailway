package com.orange838.zongheng.client;

import com.orange838.zongheng.ZongHengRailway;
import net.neoforged.fml.ModContainer;

/**
 * 配置界面的注册入口。
 *
 * <p>这个类刻意不引用任何 Cloth Config 的类。先在这里探测前置是否存在，
 * 确认存在后才去碰 {@link ConfigScreenFactory}，这样即使玩家没装 Cloth Config，
 * 也不会在类加载阶段抛 NoClassDefFoundError，模组照常按 toml 运行。
 */
public final class ConfigScreens {

    private static final String CLOTH_PROBE = "me/shedaniel/clothconfig2/api/ConfigBuilder.class";

    private ConfigScreens() {
    }

    /**
     * 只在客户端调用。Cloth Config 在场则注册图形配置界面。
     *
     * @return true 表示界面已注册
     */
    public static boolean tryRegisterClient(ModContainer container) {
        if (!hasClothConfig(container)) {
            ZongHengRailway.LOGGER.info(
                    "Cloth Config is not installed; the in-game config screen is disabled. "
                            + "Edit config/zongheng_railway-common.toml instead.");
            return false;
        }
        ConfigScreenFactory.register(container);
        return true;
    }

    /** 通过资源探测判断 Cloth Config 是否在场，不触发任何 Cloth 类的加载。 */
    private static boolean hasClothConfig(ModContainer container) {
        try {
            return container.getModInfo().getOwningFile().getFile().findResource(CLOTH_PROBE) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
