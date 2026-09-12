package com.merlinkitsune.starengine.config;

import com.merlinkitsune.starengine.component.GameplayConfigValues;
import com.merlinkitsune.starengine.component.GameplayConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;

/**
 * 配置生命周期入口:注册、读取、把值推给 {@link GameplayConstants}。
 *
 * <p>平台侧只需要在自己的 {@code @Mod} 构造里调一次 {@link #register()},再各自注册
 * 配置屏幕扩展点(NeoForge 用 {@code IConfigScreenFactory}、Forge 用
 * {@code ConfigScreenHandler.ConfigScreenFactory})——屏幕扩展点的 API 两版不通用,
 * 是唯一无法收进 common 的部分,其屏幕工厂实现见
 * {@link com.merlinkitsune.starengine.client.StarEngineConfigScreen}。
 *
 * <h2>时机</h2>
 * {@code AutoConfig.register} 内部构造 {@code ConfigManager},后者构造时立即
 * {@code load()} 并在成功时 {@code save()}(已核对字节码:{@code if (load()) save();}),
 * 所以<b>注册完成时配置值就已是磁盘上的真实值</b>,可以直接推给常量,不必等
 * {@code FMLCommonSetupEvent}。旧实现是在 CommonSetup 里 refresh 的,现在提前到
 * 模组构造期,语义更强(任何 gameplay 代码读到的一定是配置值而非默认值)。
 *
 * <h2>热生效</h2>
 * 常量字段是非 final 的运行时读取,所以只要在每次保存/加载后重新推送一次,
 * GUI 里改完点 Save 即生效,不需要重启。注意因此<b>不要</b>给配置项加
 * {@code @ConfigEntry.Gui.RequiresRestart}。
 *
 * <p>监听器返回 {@code InteractionResult.PASS} 是刻意的。已核对 ConfigManager 的
 * save()/load() 字节码:循环里只有 {@code == FAIL} 会中断(FAIL 在 load 路径上会
 * resetToDefault),{@code == PASS} 继续通知后面的监听器,其它值提前跳出循环;
 * 三条路径最终都会落到 serialize/采用已加载值。我们用 PASS 表示「只是观察,不干预」,
 * 避免抢断其它监听器。
 */
public final class StarEngineConfigs {

    /** 分类键:核心玩法数值(与 {@code @ConfigEntry.Category} 的值一致)。 */
    public static final String CATEGORY_DEFAULT = "default";
    /** 分类键:事件系统。 */
    public static final String CATEGORY_EVENT_SYSTEM = "event_system";
    /** 分类键:筹码。 */
    public static final String CATEGORY_CHIPS = "chips";
    /** 分类键:actionbar。 */
    public static final String CATEGORY_ACTIONBAR = "actionbar";

    private StarEngineConfigs() {
    }

    /**
     * 注册配置并立即把值推给 {@link GameplayConstants}。
     *
     * <p>必须在模组构造期调用且<b>只能调用一次</b>——{@code AutoConfig.register} 对重复注册
     * 直接抛 {@code RuntimeException("Config '%s' already registered")}(已核对字节码)。
     */
    public static void register() {
        // 先做旧格式迁移:目标 JSON 不存在而旧 TOML 存在时,把旧值 seed 成 JSON,
        // 这样紧接着的 register() 读到的就是玩家原来的设置,而不是默认值。
        LegacyCommonTomlImporter.importIfNeeded();

        ConfigHolder<StarEngineCommonConfig> holder =
                AutoConfig.register(StarEngineCommonConfig.class, GsonConfigSerializer::new);
        holder.registerLoadListener((configHolder, config) -> {
            applyToConstants(configHolder.getConfig());
            return InteractionResult.PASS;
        });
        holder.registerSaveListener((configHolder, config) -> {
            applyToConstants(configHolder.getConfig());
            return InteractionResult.PASS;
        });
        applyToConstants(holder.getConfig());
    }

    /** 当前配置值。调用前必须已 {@link #register()}。 */
    public static StarEngineCommonConfig get() {
        return AutoConfig.getConfigHolder(StarEngineCommonConfig.class).getConfig();
    }

    /** 手动把当前配置值推给 {@link GameplayConstants}(一般不需要,GUI 保存时会自动推送)。 */
    public static void refreshConstants() {
        applyToConstants(get());
    }

    private static void applyToConstants(StarEngineCommonConfig config) {
        GameplayConstants.applyConfig(new GameplayConfigValues(
                config.maxStarlight,
                config.maxMarker,
                config.effectCardCooldownSeconds,
                config.maxEffectStacks,
                config.giveGuideBookOnFirstJoin,
                config.eventRange,
                config.eventApplyMcTeam,
                config.eventApplyFtbTeam,
                config.eventApplyOpac,
                config.eventApplyMaid,
                config.handFanBigRange,
                config.actionbarDurationTicks,
                config.actionbarFadeTicks));
    }
}
