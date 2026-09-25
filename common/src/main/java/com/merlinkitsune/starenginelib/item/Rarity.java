package com.merlinkitsune.starenginelib.item;

import java.util.function.UnaryOperator;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * 本模组（含消费方 mod）的**稀有度等级**定义 —— 等级、常量名、序列化名与**颜色码的唯一权威**。
 *
 * <h2>这些等级是怎么"存在"的</h2>
 * <p>它们不是独立枚举，而是被**扩展进原版 {@code net.minecraft.world.item.Rarity}**：
 * <ul>
 *   <li><b>NeoForge（1.21.1 / 26.1.2）</b>：消费方 mod 在自己的 {@code neoforge.mods.toml} 里声明
 *       {@code enumExtensions="META-INF/enumextensions.json"}，该 json 的 4 条 entry 通过
 *       {@code AstralRarities} 的 {@link net.neoforged.fml.common.asm.enumextension.EnumProxy} 字段
 *       取得**序列化名**与**染色函数**；FML 的 {@code RuntimeEnumExtender} 在 {@code Rarity} 类加载时注入常量；</li>
 *   <li><b>Forge（1.20.1）</b>：{@code Rarity} 实现 {@code IExtensibleEnum}，由 {@code AstralRarities}
 *       的静态初始化调用 {@code Rarity.create(name, styleModifier)} 登记。</li>
 * </ul>
 *
 * <h2>颜色</h2>
 * <p>颜色在**扩展时**以 Style 变换函数的形式交给原版枚举常量（{@link #styleModifier()}），此后由原版
 * tooltip 链路（{@code ItemStack#getTooltipLines} → {@code Rarity#getStyleModifier()}）自动套用 ——
 * 因此本类即是**染色权威**：改色只需改这里的常量，无需任何 tooltip 侧代码。
 *
 * <p>⚠️ 原版自带的 {@code COMMON/UNCOMMON/RARE/EPIC} 在本模组内**不再用于分档**（仅 {@code COMMON} 保留作"普通"）。
 *
 * @see com.merlinkitsune.starenginelib.item.AstralRarities
 */
public enum Rarity {
    /** 稀有 —— 浅蓝。 */
    RARE("ASTRAL_DICE_RARE", "astral_dice:rare", 0x8FD3FF),
    /** 史诗 —— 粉紫。 */
    EPIC("ASTRAL_DICE_EPIC", "astral_dice:epic", 0xE3A6FF),
    /** 传奇 —— 金。 */
    LEGENDARY("ASTRAL_DICE_LEGENDARY", "astral_dice:legendary", 0xFFC24B),
    /** 巅峰 —— 亮红。 */
    PINNACLE("ASTRAL_DICE_PINNACLE", "astral_dice:pinnacle", 0xFF4D4D);

    private final String constantName;
    private final String serializedName;
    private final int rgb;

    Rarity(String constantName, String serializedName, int rgb) {
        this.constantName = constantName;
        this.serializedName = serializedName;
        this.rgb = rgb;
    }

    /**
     * 扩展后原版 {@code Rarity} 的**枚举常量名**（= {@code Rarity#name()}；探针/日志/崩溃报告里看到的就是它）。
     * NeoForge 侧它同时是 {@code enumextensions.json} 的 {@code name} 字段值（FML 注入的字段名）。
     */
    public String constantName() {
        return this.constantName;
    }

    /**
     * **序列化名**（= 扩展后 {@code Rarity#getSerializedName()}）。
     *
     * <p>⚠️ NeoForge 对它有**硬性格式要求**（{@code RuntimeEnumExtender#validateNameParameter}）：必须形如
     * {@code <声明该 enumextensions.json 的 modId>:<名字>} —— **冒号分隔**；当前 modId 为 {@code astral_dice}
     * ⇒ 形如 {@code astral_dice:rare}（写 {@code astral_dice_rare} 会在 {@code Rarity} 类加载时抛
     * {@code IllegalArgumentException: Name parameter must be prefixed by mod ID}，游戏直接起不来）。
     *
     * <p>注意这与 {@code enumextensions.json} 里 {@code name} 字段（**注入进枚举的字段名**，要求以 modId 小写前缀开头、
     * 形如 {@code ASTRAL_DICE_RARE}）是**两条不同的规则**，别混。
     */
    public String serializedName() {
        return this.serializedName;
    }

    /** 颜色码（0xRRGGBB）。 */
    public int rgb() {
        return this.rgb;
    }

    /** 颜色码对应的 {@link TextColor}。 */
    public TextColor textColor() {
        return TextColor.fromRgb(this.rgb);
    }

    /** 染色：本项目**唯一**的颜色写入点。 */
    public Style apply(Style style) {
        return style.withColor(this.textColor());
    }

    /** 交给原版枚举常量的 Style 变换函数（扩展时传入；原版 tooltip 链路会调用它）。 */
    public UnaryOperator<Style> styleModifier() {
        return this::apply;
    }
}
