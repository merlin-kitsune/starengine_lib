package com.merlinkitsune.starenginelib;

import net.fabricmc.api.ModInitializer;

/**
 * StarEngine Lib / Fabric 1.20.1 入口。
 *
 * <p>本库**不注册任何注册表条目**(物品/效果/附件/数据组件全部留在消费方 mod),
 * 因此搬迁不会改变任何 {@code ResourceLocation} 归属、不破坏存档与数据包。
 * 入口存在只为:1) 让库成为独立可加载的 mod(Fabric Loader 需要一个 {@code main} entrypoint);
 * 2) 为将来库自身的配置/日志留锚点。
 *
 * <p><b>组件注册</b>:钱包余额的 CCA 组件由 {@code cardinal-components-entity} entrypoint
 * ({@link com.merlinkitsune.starenginelib.economy.FabricEconomyStorage})承担 —— CCA 要求
 * 组件工厂在世界加载前注册,走 CCA 自己的 entrypoint 最稳。
 */
public final class StarEngineLib implements ModInitializer {
    public static final String MODID = "starengine_lib";

    @Override
    public void onInitialize() {
        // 注入 Fabric 钱包存储实现(Cardinal Components 实体组件 + 死亡/重生复制)
        com.merlinkitsune.starenginelib.economy.FabricEconomyStorage.install();
    }
}
