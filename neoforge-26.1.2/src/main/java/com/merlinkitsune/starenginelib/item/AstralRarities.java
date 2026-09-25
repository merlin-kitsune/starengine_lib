package com.merlinkitsune.starenginelib.item;

import net.neoforged.fml.common.asm.enumextension.EnumProxy;

/**
 * 本模组稀有度等级的**平台接线** —— 把 {@link Rarity} 里的 5 个等级扩展进原版
 * {@code net.minecraft.world.item.Rarity}，并提供运行期取回扩展常量的访问器。
 *
 * <h2>等级名 / 颜色 / 序列化名都在 {@link Rarity}</h2>
 * <p>本类**不含任何颜色或等级定义**，只做两件事：① 把 {@link Rarity} 的序列化名与
 * {@link Rarity#styleModifier()} 交给平台扩展机制；② 把扩展结果交回消费方。
 *
 * ⚠️ 同包内的 {@code Rarity} 是本模组枚举 ⇒ 原版类型在本文件里**一律写全限定名**，不要 import。
 */

public final class AstralRarities {
    /*
     * ⚠️ 这 5 个字段名是**契约的一部分**：消费方 mod 的 META-INF/enumextensions.json 里
     *    {"class": "com/merlinkitsune/starenginelib/item/AstralRarities", "field": "RARE"} 按名引用它们，
     *    改名（或改类名/包名）必须同步改该 json，否则该条 entry 会变成「引用不存在的字段」= 启动期报错。
     *    参数顺序 = 原版 (int id, String name, UnaryOperator<Style>) 构造器的**声明参数**：
     *      索引 0 = id（FML 用真实序号覆盖，这里放 null 占位）；索引 1 = 序列化名；索引 2 = 染色函数。
     *
     * ⚠️ 静态初始化只构造 EnumProxy（**不得**在此调用 getValue()）：getValue() 会触发原版 Rarity 的
     *    初始化，而那一刻 FML 注入的代码正在读本类字段 ⇒ 会形成"自己初始化自己"的读序问题。
     *    取回常量一律走下面的访问器（懒解析）。
     */
    public static final EnumProxy<net.minecraft.world.item.Rarity> RARE = new EnumProxy<>(
            net.minecraft.world.item.Rarity.class, null,
            Rarity.RARE.serializedName(), Rarity.RARE.styleModifier());

    public static final EnumProxy<net.minecraft.world.item.Rarity> EPIC = new EnumProxy<>(
            net.minecraft.world.item.Rarity.class, null,
            Rarity.EPIC.serializedName(), Rarity.EPIC.styleModifier());

    public static final EnumProxy<net.minecraft.world.item.Rarity> LEGENDARY = new EnumProxy<>(
            net.minecraft.world.item.Rarity.class, null,
            Rarity.LEGENDARY.serializedName(), Rarity.LEGENDARY.styleModifier());

    public static final EnumProxy<net.minecraft.world.item.Rarity> PINNACLE = new EnumProxy<>(
            net.minecraft.world.item.Rarity.class, null,
            Rarity.PINNACLE.serializedName(), Rarity.PINNACLE.styleModifier());

    public static final EnumProxy<net.minecraft.world.item.Rarity> BIZARRE = new EnumProxy<>(
            net.minecraft.world.item.Rarity.class, null,
            Rarity.BIZARRE.serializedName(), Rarity.BIZARRE.styleModifier());

    private AstralRarities() {
    }

    /** 稀有（浅蓝）—— 扩展后的原版 {@code Rarity} 常量。 */
    public static net.minecraft.world.item.Rarity rare() {
        return RARE.getValue();
    }

    /** 史诗（粉紫）。 */
    public static net.minecraft.world.item.Rarity epic() {
        return EPIC.getValue();
    }

    /** 传奇（金）。 */
    public static net.minecraft.world.item.Rarity legendary() {
        return LEGENDARY.getValue();
    }

    /** 巅峰（亮红）。 */
    public static net.minecraft.world.item.Rarity pinnacle() {
        return PINNACLE.getValue();
    }

    /** 奇特（彩虹/流动）。 */
    public static net.minecraft.world.item.Rarity bizarre() {
        return BIZARRE.getValue();
    }
}
