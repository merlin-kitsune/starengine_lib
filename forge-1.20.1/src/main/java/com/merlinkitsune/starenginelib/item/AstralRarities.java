package com.merlinkitsune.starenginelib.item;

/**
 * 本模组稀有度等级的**平台接线** —— 把 {@link Rarity} 里的 4 个等级扩展进原版
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
     * Forge 1.20.1 没有 enumextensions.json：机制是原版 Rarity 实现
     * net.minecraftforge.common.IExtensibleEnum，其静态工厂 Rarity.create(String, UnaryOperator<Style>)
     * 的**方法体**在类加载时被 fmlloader 的 RuntimeEnumExtender 换成「真正构造 + 追加 $VALUES」
     * ⇒ 本类的静态初始化即为登记点。
     *
     * ⚠️ 必须在任何物品构造之前被触发：消费方在 ModItems 注册期调用下面的访问器，天然满足。
     * ⚠️ **不要**改用 Rarity.valueOf(constantName) 取常量：Class#enumConstantDirectory 可能已被其它模组
     *    提前缓存（早于本类登记）⇒ 会抛 IllegalArgumentException。本类自己持有登记结果。
     * ⚠️ 传进去的名字会成为该常量的**枚举字段名**（= name()）⇒ 与 NeoForge 侧 json 的 name 保持一致
     *    （即 Rarity#constantName()，形如 ASTRAL_DICE_RARE），三线日志/探针读数才一致。
     */
    public static final net.minecraft.world.item.Rarity RARE = net.minecraft.world.item.Rarity.create(
            Rarity.RARE.constantName(), Rarity.RARE.styleModifier());

    public static final net.minecraft.world.item.Rarity EPIC = net.minecraft.world.item.Rarity.create(
            Rarity.EPIC.constantName(), Rarity.EPIC.styleModifier());

    public static final net.minecraft.world.item.Rarity LEGENDARY = net.minecraft.world.item.Rarity.create(
            Rarity.LEGENDARY.constantName(), Rarity.LEGENDARY.styleModifier());

    public static final net.minecraft.world.item.Rarity PINNACLE = net.minecraft.world.item.Rarity.create(
            Rarity.PINNACLE.constantName(), Rarity.PINNACLE.styleModifier());

    private AstralRarities() {
    }

    /** 稀有（浅蓝）—— 登记后的原版 {@code Rarity} 常量。 */
    public static net.minecraft.world.item.Rarity rare() {
        return RARE;
    }

    /** 史诗（粉紫）。 */
    public static net.minecraft.world.item.Rarity epic() {
        return EPIC;
    }

    /** 传奇（金）。 */
    public static net.minecraft.world.item.Rarity legendary() {
        return LEGENDARY;
    }

    /** 巅峰（亮红）。 */
    public static net.minecraft.world.item.Rarity pinnacle() {
        return PINNACLE;
    }
}
