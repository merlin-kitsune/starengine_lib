package com.merlinkitsune.starenginelib.target;

import com.merlinkitsune.starenginelib.combat.HostileTargets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 目标选择器的**可选中判定**（客户端射线 / 客户端半径内高亮 / 服务端确认三处共用）。
 * (2026-09-22 自主模组 {@code target.SelectorTargets} 下沉;口径逐字未变。)
 *
 * <h2>为什么需要它（2026-09-18 用户实测缺陷）</h2>
 * 本库 {@link TargetType} 的 {@code matches} 对「敌对」族（{@code ENEMY} / {@code ENEMY_OR_RIVAL}）
 * 只做裸的 {@code instanceof Enemy} 判定，而全局口径（**见 {@link HostileTargets} 类注释**，
 * 2026-09-14 用户裁决、2026-09-24 重写）不只认敌对生物：全原版 {@code NeutralMob} 的直接实现者里，
 * <b>狼 / 铁傀儡 / 北极熊 / 蜜蜂</b> 四个不属 {@code Enemy}。⇒ 若不委托统一入口，它们在选择器里会被判
 * 「不可选 / 对准错误目标」，与全局口径不符。
 *
 * <p>本类把「敌对」族并到唯一入口 {@link HostileTargets#isHostile(net.minecraft.world.entity.Entity)}
 * 上，其余类型仍走库的基础语义。
 *
 * <h2>口径（逐条）</h2>
 * <ul>
 *   <li>{@link TargetType#ENEMY} → {@code HostileTargets.isHostile(target)}
 *       （口径见 {@link HostileTargets} 类注释；**不含玩家**，与库的「仅敌对生物」一致）；</li>
 *   <li>{@link TargetType#ENEMY_OR_RIVAL} → {@code HostileTargets.isHostile(target)}
 *       ∪ 库 {@code matches} 的「非队友玩家」分支；</li>
 *   <li>{@link TargetType#PLAYER} / {@link TargetType#LIVING} → 原样交给库的 {@code matches}。</li>
 * </ul>
 *
 * <p><b>禁止</b>在选择器代码里再直接调用 {@code targetType.matches(...)} 或写裸的
 * {@code instanceof Enemy} —— 那会漏掉中立生物；新增判定一律调用本类。
 */
public final class SelectorTargets {
    private SelectorTargets() {
    }

    /**
     * 目标是否属于本次会话的可选集合。
     *
     * @param type     会话的目标类型（本库枚举）
     * @param selector 选择者（{@link TargetType#PLAYER} / {@link TargetType#ENEMY_OR_RIVAL} 的玩家分支需要）
     * @param target   待判定的活体目标
     */
    public static boolean matches(TargetType type, Player selector, LivingEntity target) {
        if (type == null || target == null) return false;
        if (type == TargetType.ENEMY) {
            return HostileTargets.isHostile(target);
        }
        if (type == TargetType.ENEMY_OR_RIVAL) {
            return HostileTargets.isHostile(target) || type.matches(selector, target);
        }
        return type.matches(selector, target);
    }

    /**
     * 目标是否属于本次会话的可选集合(**含会话级的「允许对自身使用」判定**)。
     *
     * <p>三处调用点里只有**服务端确认**需要本重载:本库 {@link TargetType#matches} 始终排除选择者
     * 自身,故「右键对自身使用」必须由消费方放行 —— 仅当本次会话确实允许自身目标
     * ({@code allowSelf},来自 {@code SelfTargetable#allowSelf()} 的启动时快照)且目标就是选择者时
     * 才放行,其余情况逐字沿用三参方法(占星师/秘密侦探/枪匠三个选择器类立牌 {@code allowSelf=false} ⇒ 行为不变)。
     *
     * @param allowSelf 本次会话是否允许对自身使用(会话启动时快照)
     */
    public static boolean matches(TargetType type, Player selector, LivingEntity target, boolean allowSelf) {
        if (allowSelf && target != null && target == selector) return true;
        return matches(type, selector, target);
    }
}
