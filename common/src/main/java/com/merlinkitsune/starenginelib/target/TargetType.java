package com.merlinkitsune.starenginelib.target;

import com.merlinkitsune.starenginelib.combat.CreatureTargets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 目标选择器的可指定目标类型。
 * 每次选择会话由 {@link TargetSelectionAction#targetType()} 决定允许的目标种类，
 * 客户端用于过滤准星目标（避免错选），服务端用于确认时二次校验（权威判定）。
 */
public enum TargetType {
    /** 仅玩家（排除选择者自身） */
    PLAYER {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            return target instanceof Player && target != selector;
        }
    },
    /** 仅敌对生物（vanilla {@link Enemy} 标记接口：僵尸/骷髅/掠夺者等） */
    ENEMY {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            return target instanceof Enemy;
        }
    },
    /** 任意活体目标（玩家/敌对/中立/被动，排除选择者自身） */
    LIVING {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            return target != selector;
        }
    },
    /**
     * 敌对生物 或 非队友玩家（立牌主动技能专用，如占星师虚弱印记/秘密侦探隐匿调查）：
     * - 敌对生物（vanilla {@link Enemy}）→ 可选中（客户端显示红色高亮）；
     * - 玩家且不属于选择者队伍 → 可选中（黄色高亮）；选择者无队伍时对所有其他玩家生效；
     * - 队友玩家 / 被动生物 / 自己 → 不可选中。
     */
    ENEMY_OR_RIVAL {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            if (target instanceof Enemy) return true;
            if (target instanceof Player other && other != selector) {
                // 选择者无队伍 → 所有玩家可选;有队伍 → 仅非队友玩家可选
                return selector.getTeam() == null || selector.getTeam() != other.getTeam();
            }
            return false;
        }
    },
    /**
     * 敌对目标 ∪ 未驯服的可驯服生物（**效果牌专用**；2026-10-03 用户裁决）。
     *
     * <p>口径见 {@link CreatureTargets}：比 {@link #ENEMY} 多出「未驯服的狼 / 猫 / 鹦鹉」与
     * 「无主的马 / 驴 / 骡 / 骆驼 / 羊驼」；**不含玩家**；已驯服宠物与村民 / 流浪商人不在内。
     * ⚠️ 立牌选择器**不用**本类型（仍走 {@link #ENEMY} / {@link #ENEMY_OR_RIVAL}）。
     */
    CREATURE {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            return CreatureTargets.isCreatureTarget(target);
        }
    },
    /** {@link #CREATURE} ∪ 非队友玩家（**符卡-祸专用**）。 */
    CREATURE_OR_RIVAL {
        @Override
        public boolean matches(Player selector, LivingEntity target) {
            if (CreatureTargets.isCreatureTarget(target)) return true;
            if (target instanceof Player other && other != selector) {
                return selector.getTeam() == null || selector.getTeam() != other.getTeam();
            }
            return false;
        }
    };

    public abstract boolean matches(Player selector, LivingEntity target);
}
