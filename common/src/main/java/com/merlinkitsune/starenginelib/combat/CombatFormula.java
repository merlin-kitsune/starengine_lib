package com.merlinkitsune.starenginelib.combat;

/**
 * 防御力换算公式（2026-09-26 用户裁决，必须遵守）。
 *
 * <h2>玩家侧（换算保持不变，仅**移除护甲 20 硬上限**）</h2>
 * <pre>
 *   playerDefense = 2 + 当前护甲值 / 2 + 1.4 × 盔甲韧性
 * </pre>
 * 原实现额外有 {@code Math.min(护甲, 20)} 与 {@code Math.min(有效护甲, 20)} 两道上限，
 * 自本版起**两侧一并移除**（用户 2026-09-26 裁决），护甲按其真实值参与换算。
 *
 * <h2>生物侧（新公式）</h2>
 * <pre>
 *   mobDefense = baseDefense(category) + 当前护甲值 / 2 + 1.125 × 盔甲韧性   ← 最终取整
 * </pre>
 * 其中 {@code baseDefense} 见 {@link TargetBattleStats}（敌对 2 / 中立 0 / 被动 0 / 友好 0）。
 * 与玩家侧的差异有两处：① 起始项按分类取 2 或 0；② 韧性系数为 <b>1.125</b>（玩家侧为 1.4）。
 *
 * <h2>护甲取值口径（兼容性硬约束）</h2>
 * 调用方传入的 {@code armor} **必须是属性终值**（{@code LivingEntity#getArmorValue()}）——
 * 即「所有状态效果与第三方模组（如七咒之戒）修饰完毕之后的真实护甲值」。
 * 本库**不做**任何二次修正、不识别任何具体模组 ⇒ 第三方效果恒先生效、永远不被本系统覆盖，
 * 这是「最大模组兼容性」的实现基础。
 *
 * <p>本类**纯函数、无状态**：不注册注册表条目、不注册事件、不反向依赖任何消费方。
 */
public final class CombatFormula {

    /** 玩家侧起始防御力。 */
    public static final double PLAYER_BASE_DEFENSE = 2.0;
    /** 玩家侧盔甲韧性系数。 */
    public static final double PLAYER_TOUGHNESS_COEF = 1.4;
    /** 生物侧盔甲韧性系数。 */
    public static final double MOB_TOUGHNESS_COEF = 1.125;

    /** 护甲值 → 防御力的统一除数（护甲值 ÷ 该值）。 */
    public static final double ARMOR_DIVISOR = 2.0;

    private CombatFormula() {
    }

    /**
     * 玩家侧防御力换算（不含骰点与战斗牌加成）。
     *
     * @param armor     当前护甲值（属性终值，建议 {@code LivingEntity#getArmorValue()}）
     * @param toughness 当前盔甲韧性（属性终值）
     */
    public static double playerDefense(double armor, double toughness) {
        return PLAYER_BASE_DEFENSE + armor / ARMOR_DIVISOR + PLAYER_TOUGHNESS_COEF * toughness;
    }

    /**
     * 生物侧防御力换算（不含骰点与战斗牌加成）。
     *
     * @param category  生物立场分类（决定起始项 2 或 0）
     * @param armor     当前护甲值（属性终值）
     * @param toughness 当前盔甲韧性（属性终值）
     */
    public static double mobDefense(TargetCategory category, double armor, double toughness) {
        return TargetBattleStats.baseDefense(category) + armor / ARMOR_DIVISOR + MOB_TOUGHNESS_COEF * toughness;
    }

    /**
     * 生物侧防御力换算并**最终取整**（向下取整，与既有显示口径 {@code Math.floor} 一致）。
     */
    public static int mobDefenseInt(TargetCategory category, double armor, double toughness) {
        return (int) Math.floor(mobDefense(category, armor, toughness));
    }
}
