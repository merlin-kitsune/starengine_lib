package com.merlinkitsune.starenginelib.combat;

import net.minecraft.world.entity.Entity;

/**
 * 「生物初始攻防值」查询（2026-09-26 用户裁决，必须遵守）。
 *
 * <p>口径（用户原话）：
 * <ul>
 *   <li>敌对生物初始防御力 <b>2</b>；中立 / 被动 / 友好生物初始防御力 <b>0</b>；</li>
 *   <li>敌对生物初始攻击力 <b>5</b>；中立生物初始攻击力 <b>4</b>；被动 / 友好生物 <b>0</b>。</li>
 * </ul>
 *
 * <p>初始值只是**加成起点**：最终防御力/攻击力还要叠加「当前护甲值 / 盔甲韧性 / 生物自身伤害值」
 * 等项，换算见 {@link CombatFormula}。
 *
 * <p>本类**纯查表、无状态**：不注册注册表条目、不注册事件、不反向依赖任何消费方。
 */
public final class TargetBattleStats {

    /** 敌对生物初始防御力。 */
    public static final int HOSTILE_BASE_DEFENSE = 2;
    /** 中立生物初始防御力。 */
    public static final int NEUTRAL_BASE_DEFENSE = 0;
    /** 被动生物初始防御力。 */
    public static final int PASSIVE_BASE_DEFENSE = 0;
    /** 友好生物（已驯服宠物）初始防御力。 */
    public static final int FRIENDLY_BASE_DEFENSE = 0;

    /** 敌对生物初始攻击力。 */
    public static final int HOSTILE_BASE_ATTACK = 5;
    /** 中立生物初始攻击力。 */
    public static final int NEUTRAL_BASE_ATTACK = 4;
    /** 被动生物初始攻击力。 */
    public static final int PASSIVE_BASE_ATTACK = 0;
    /** 友好生物（已驯服宠物）初始攻击力。 */
    public static final int FRIENDLY_BASE_ATTACK = 0;

    private TargetBattleStats() {
    }

    /** 按分类取初始防御力。 */
    public static int baseDefense(TargetCategory category) {
        if (category == null) return PASSIVE_BASE_DEFENSE;
        return switch (category) {
            case HOSTILE -> HOSTILE_BASE_DEFENSE;
            case NEUTRAL -> NEUTRAL_BASE_DEFENSE;
            case PASSIVE -> PASSIVE_BASE_DEFENSE;
            case FRIENDLY -> FRIENDLY_BASE_DEFENSE;
        };
    }

    /** 按分类取初始攻击力。 */
    public static int baseAttack(TargetCategory category) {
        if (category == null) return PASSIVE_BASE_ATTACK;
        return switch (category) {
            case HOSTILE -> HOSTILE_BASE_ATTACK;
            case NEUTRAL -> NEUTRAL_BASE_ATTACK;
            case PASSIVE -> PASSIVE_BASE_ATTACK;
            case FRIENDLY -> FRIENDLY_BASE_ATTACK;
        };
    }

    /** 对实体取初始防御力（内部按 {@link TargetCategory#classify} 分类）。 */
    public static int baseDefense(Entity entity) {
        return baseDefense(TargetCategory.classify(entity));
    }

    /** 对实体取初始攻击力（内部按 {@link TargetCategory#classify} 分类）。 */
    public static int baseAttack(Entity entity) {
        return baseAttack(TargetCategory.classify(entity));
    }
}
