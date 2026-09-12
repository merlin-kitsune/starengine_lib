package com.merlinkitsune.starenginelib.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * StarEngine-Lib 的公共配置 schema(Cloth Config «AutoConfig» 注解驱动)。
 *
 * <p>本类被两个平台各编译一次(common 共享源码),因此只允许使用
 * <b>1.20.1 的 cloth-config-forge 11.1.136 与 1.21.1 的 cloth-config-neoforge 15.0.140
 * 逐字节一致的 API</b>。这两个版本已实测比对:ConfigBuilder / ConfigEntryBuilder /
 * ConfigCategory / 全部 autoconfig.* 类反编译后 diff 零差异,故此约束成立。
 *
 * <h2>为什么配置 schema 放在库里</h2>
 * 本类承载的 13 个字段与 {@link com.merlinkitsune.starenginelib.component.GameplayConstants}
 * 一一对应,而后者是库的一部分。schema 与它喂养的常量同仓,避免在消费方两个平台树里
 * 各写一份完全相同的 POJO(库抽取前 ModCommonConfig / GameplayConfigBinder 就是这么重复的)。
 *
 * <h2>命名空间</h2>
 * {@code @Config(name = "astral_dice")} 同时决定两件事,均已定型、改名即等于玩家配置重置:
 * <ul>
 *   <li>文件落点 = {@code Utils.getConfigFolder().resolve("astral_dice" + ".json")}
 *       → 实测两侧 UtilsImpl 都走 {@code FMLPaths.CONFIGDIR.get()},即
 *       {@code <游戏目录>/config/astral_dice.json}(Gson 序列化器的后缀在字节码里是硬编码的
 *       {@code \u0001.json},无法保留旧的 .toml 后缀);</li>
 *   <li>翻译键前缀 = {@code text.autoconfig.astral_dice.*}(见 ConfigScreenProvider 里
 *       {@code "text.autoconfig.%s"} / {@code "%s.title"} / {@code "%s.category.%s"} /
 *       {@code "%s.option.%s"} 四处拼接)。</li>
 * </ul>
 *
 * <h2>字段名是持久化契约</h2>
 * Gson 直接用 Java 字段名做 JSON 键,Cloth Config 的 GUI 也直接用
 * {@code Field#getName()} 拼翻译键。因此<b>重命名字段 = 该字段的值在玩家侧静默重置,
 * 且 GUI 标签退化为原始键名</b>。要改名必须同时改消费方的 4 个 lang 文件。
 *
 * <h2>本类刻意不含任何静态成员</h2>
 * ConfigScreenProvider 通过 {@code Class#getDeclaredFields()} 遍历字段且<b>不过滤
 * static/final</b>(已核对字节码:该类未引用 java/lang/reflect/Modifier),静态常量一旦
 * 被当成配置项渲染,GUI 会尝试写入 final 字段。故静态逻辑全部放在
 * {@link StarEngineConfigs} 与 {@link LegacyCommonTomlImporter} 里。
 */
@Config(name = "astral_dice")
public class StarEngineCommonConfig implements ConfigData {

    /*
     * 分类键取值(翻译键 = text.autoconfig.astral_dice.category.<值>):
     *   "default"      → 核心玩法数值
     *   "event_system" → 事件系统
     *   "chips"        → 筹码
     *   "actionbar"    → actionbar
     * 这四组与旧 astral_dice-common.toml 的分组一一对应(仅去掉不再需要的 config_version)。
     */

    /** 星光获取上限(默认 32) */
    @ConfigEntry.Category("default")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 8, max = 48)
    public int maxStarlight = 32;

    /** 标记层数上限(默认 16) */
    @ConfigEntry.Category("default")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 8, max = 48)
    public int maxMarker = 16;

    /** 效果牌公共冷却(秒,默认 30) */
    @ConfigEntry.Category("default")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 5, max = 120)
    public int effectCardCooldownSeconds = 30;

    /** 功能效果牌叠加层数上限(默认 3;伤害效果牌不使用该叠加) */
    @ConfigEntry.Category("default")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 9)
    public int maxEffectStacks = 3;

    /** 玩家第一次加入世界时是否给予《恋的规则书》(默认 true;每个玩家每个世界只发一次) */
    @ConfigEntry.Category("default")
    @ConfigEntry.Gui.Tooltip
    public boolean giveGuideBookOnFirstJoin = true;

    /** 事件作用范围(格,默认 16) */
    @ConfigEntry.Category("event_system")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
    public int eventRange = 16;

    /** 事件是否作用于 Minecraft 同队玩家 */
    @ConfigEntry.Category("event_system")
    @ConfigEntry.Gui.Tooltip
    public boolean eventApplyMcTeam = true;

    /** 事件是否作用于 FTB Teams 队友(需安装 FTB Teams,API 不符时自动跳过) */
    @ConfigEntry.Category("event_system")
    @ConfigEntry.Gui.Tooltip
    public boolean eventApplyFtbTeam = true;

    /** 事件是否作用于 OPAC 队伍(需安装 Open Parties and Claims,API 不符时自动跳过) */
    @ConfigEntry.Category("event_system")
    @ConfigEntry.Gui.Tooltip
    public boolean eventApplyOpac = true;

    /** 事件是否作用于玩家拥有的已放出女仆(需安装车万女仆模组) */
    @ConfigEntry.Category("event_system")
    @ConfigEntry.Gui.Tooltip
    public boolean eventApplyMaid = true;

    /** 手持风扇-大:主动技能后对周围敌对目标施加标记的范围(格,默认 16) */
    @ConfigEntry.Category("chips")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
    public int handFanBigRange = 16;

    /** actionbar 消息显示总时长上限(tick,默认 60 = 3 秒) */
    @ConfigEntry.Category("actionbar")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 20, max = 200)
    public int actionbarDurationTicks = 60;

    /** actionbar 消息最后淡出时长(tick,默认 20 = 1 秒) */
    @ConfigEntry.Category("actionbar")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 60)
    public int actionbarFadeTicks = 20;

    /**
     * Gson 反序列化与 {@code Utils.constructUnsafely} 都需要可实例化。
     * 显式声明而不是依赖隐式构造:Cloth Config 走
     * {@code getDeclaredConstructor()} + {@code setAccessible(true)},
     * Gson 则在无默认构造时退化为 Unsafe 分配(那样字段初始化器不执行)。
     */
    public StarEngineCommonConfig() {
    }

    /**
     * 手改 JSON 越界时钳制回区间内。
     *
     * <p>这是对旧的 {@code ForgeConfigSpec/ModConfigSpec#defineInRange} 行为的等价替代:
     * Forge 的区间定义在读到越界值时会回落默认值并打警告,而 Cloth Config 的
     * {@code @ConfigEntry.BoundedDiscrete} <b>只在 GUI 侧生效</b>——直接编辑 JSON 写入 999
     * 不会被任何东西拦下。{@code ConfigManager} 在 deserialize / createDefault /
     * resetToDefault 三条路径上都会调用本方法(已核对字节码的三处 invokeinterface),
     * 因此这里是唯一的钳制点。
     *
     * <p>这里选「钳到边界」而不是 Forge 的「回落默认值」:边界值本身合法且最接近玩家意图,
     * 静默跳到默认值反而更意外。
     */
    @Override
    public void validatePostLoad() {
        maxStarlight = clamp(maxStarlight, 8, 48);
        maxMarker = clamp(maxMarker, 8, 48);
        effectCardCooldownSeconds = clamp(effectCardCooldownSeconds, 5, 120);
        maxEffectStacks = clamp(maxEffectStacks, 1, 9);
        eventRange = clamp(eventRange, 1, 32);
        handFanBigRange = clamp(handFanBigRange, 1, 64);
        actionbarDurationTicks = clamp(actionbarDurationTicks, 20, 200);
        actionbarFadeTicks = clamp(actionbarFadeTicks, 1, 60);
    }

    // 刻意不用 Mth.clamp:common 源码受「两版签名必须一致」约束,而这里根本不值得把 MC 类
    // 拖进来——纯算术在两侧等价且零映射风险。
    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
