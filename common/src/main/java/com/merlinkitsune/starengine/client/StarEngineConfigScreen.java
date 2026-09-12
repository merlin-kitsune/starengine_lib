package com.merlinkitsune.starengine.client;

import com.merlinkitsune.starengine.config.StarEngineCommonConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screens.Screen;

/**
 * 配置屏幕工厂(客户端专用)。
 *
 * <p>单独成类而不是塞进 {@link com.merlinkitsune.starengine.config.StarEngineConfigs}:
 * 本类的方法签名引用 {@code net.minecraft.client.gui.screens.Screen},而
 * {@code StarEngineConfigs} 会被服务端加载。分开后服务端 classpath 上永远不会有类
 * 因为链接到 Screen 而 NoClassDefFoundError。
 *
 * <p>平台侧只需把本方法接到各自的扩展点上:
 * <pre>{@code
 * // NeoForge 21.1.x / MC 1.21.1
 * modContainer.registerExtensionPoint(IConfigScreenFactory.class,
 *         (container, parent) -> StarEngineConfigScreen.create(parent));
 * }</pre>
 * <pre>{@code
 * // Forge 47.x / MC 1.20.1
 * ModLoadingContext.get().registerExtensionPoint(
 *         ConfigScreenHandler.ConfigScreenFactory.class,
 *         () -> new ConfigScreenHandler.ConfigScreenFactory(
 *                 (minecraft, parent) -> StarEngineConfigScreen.create(parent)));
 * }</pre>
 *
 * <p>注意 {@code AutoConfig.getConfigScreen} 返回的是 {@code Supplier<Screen>} 而不是
 * {@code Screen}(15.0.140 / 11.1.136 均是,已核对两个版本的公开签名),所以要 {@code get()}。
 */
public final class StarEngineConfigScreen {

    private StarEngineConfigScreen() {
    }

    /** 构造指向本库配置的 Cloth Config 屏幕;{@code parent} 用于返回按钮。 */
    public static Screen create(Screen parent) {
        return AutoConfig.getConfigScreen(StarEngineCommonConfig.class, parent).get();
    }
}
