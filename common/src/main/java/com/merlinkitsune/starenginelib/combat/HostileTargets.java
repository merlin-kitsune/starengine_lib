package com.merlinkitsune.starenginelib.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 「敌对目标」判定的**唯一入口**(2026-09-14 用户裁决,必须遵守)。
 * (2026-09-22 自主模组 {@code combat.HostileTargets} 下沉;口径逐字未变。)
 *
 * <p>口径:{@code 敌对目标 = 敌对生物 ∪ 已被激怒的中立生物 ∪ 消费方额外声明的实体}
 * <ul>
 *   <li><b>敌对生物</b>:{@link Enemy} 实例 —— 含 {@code Monster} 全部子类,以及
 *       {@code Ghast}/{@code Phantom}/{@code EnderDragon}/{@code Slime}(含岩浆怪)/
 *       {@code Shulker}/{@code Zoglin}/{@code Hoglin}。**平静的敌对类中立生物
 *       (末影人/僵尸猪灵/猪灵/猪灵蛮兵/疣猪兽)同样计入** —— 它们本身就是 {@link Enemy}。</li>
 *   <li><b>已被激怒的中立生物</b>:{@link NeutralMob} 实例且 {@code isAngry()}
 *       (= {@code getRemainingPersistentAngerTime() > 0},被激怒后 20~39 秒)为真。
 *       全原版 {@code NeutralMob} 直接实现者仅 6 个:{@code EnderMan}/{@code ZombifiedPiglin}
 *       (二者即 {@link Enemy})与 <b>狼 / 铁傀儡 / 北极熊 / 蜜蜂</b>(仅这 4 个靠 anger 判定进入敌对集合)。</li>
 *   <li><b>消费方额外声明的实体</b>:见下方「额外敌对判定 seam」—— 既非 {@link Enemy}、也非
 *       「被激怒的中立生物」,但消费方在玩法上需要当敌对处理的实体(典型:<b>测试用假人</b>这类靶子,
 *       它既不是 {@code Enemy} 也不会被激怒,却必须能被「需要敌对目标」的效果选中)。</li>
 * </ul>
 *
 * <p>熊猫/骆驼/山羊/羊驼/行商羊驼/海豚/狐狸等**不是** {@code NeutralMob},永不视为敌对目标
 * (除非消费方通过 seam 显式声明)。
 *
 * <p><b>禁止</b>在玩法代码里再写裸的 {@code instanceof Enemy} 来判定敌对目标 —— 那会漏掉
 * 被激怒的狼/铁傀儡/北极熊/蜜蜂。新增判定一律调用本类。
 *
 * <p><b>敌对玩家(用户裁决,全局规则)</b>:带「视谁为敌」上下文的
 * {@link #isHostile(Entity, Entity)} 额外把「非同队伍、且曾主动攻击过观察者的玩家」计入敌对目标
 * (记录见 {@link PlayerHostilityTracker});能提供上下文的调用点都应使用两参重载。
 * 无上下文的重载 {@link #isHostile(Entity)} 保持既有语义(玩家不计入)——
 * "曾主动攻击过谁"必须由上下文决定,不能由被判定实体自身推出。
 *
 * <h2>额外敌对判定 seam(2026-09-24 新增)</h2>
 * <b>为什么需要它</b>:本类不仅被消费方的玩法代码调用,还被库内的
 * {@link com.merlinkitsune.starenginelib.target.SelectorTargets} 用于「可选中目标」判定
 * (客户端射线 / 客户端半径高亮 / 服务端确认三处共用)。而「某个第三方实体应当算敌对」属于
 * **消费方玩法口径**(库不该知道任何具体模组的实体),判定点却有一部分落在库内 ——
 * 消费方无处插手。故库内只留判定接口,由消费方在启动时把实现注入进来(与
 * {@link InternalDamageWindows} 同构,同一套「seam + 消费方 static 注入」范式)。
 *
 * <p><b>未注入时的默认语义</b>:不额外认定任何实体为敌对(逐字保持既有口径)。这是安全方向 ——
 * 漏注入只会退回原有行为,不会凭空把中立生物变成敌对目标。消费方应尽早在模组构造/公共初始化中
 * 调用 {@link #installExtraHostileProbe}。
 */
public final class HostileTargets {
    private HostileTargets() {
    }

    /** 额外敌对判定:消费方实现的「某实体应当视为敌对目标」判定体。 */
    @FunctionalInterface
    public interface ExtraHostileProbe {
        /**
         * @param target 待判定实体(可能为 {@code null};实现方应容忍并返回 {@code false})
         * @return 该实体是否应当被额外视为敌对目标
         */
        boolean isHostile(Entity target);
    }

    /** 未注入时的默认语义:不额外认定任何实体为敌对。 */
    private static volatile ExtraHostileProbe extraHostileProbe = target -> false;

    /**
     * 注入「额外敌对判定」(消费方启动时调用一次)。
     *
     * <p>传 {@code null} 等价于恢复默认(不额外认定任何实体)。重复调用以最后一次为准。
     */
    public static void installExtraHostileProbe(ExtraHostileProbe probe) {
        extraHostileProbe = (probe != null) ? probe : target -> false;
    }

    /** 该实体是否为「敌对目标」(敌对生物,或已被激怒的中立生物,或消费方额外声明的实体)。 */
    public static boolean isHostile(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Enemy) return true;
        if (entity instanceof NeutralMob neutral && neutral.isAngry()) return true;
        // 前两条是原版口径的早退路径(覆盖绝大多数目标);仅未命中者才落到消费方注入的判定。
        return extraHostileProbe.isHostile(entity);
    }

    /**
     * 带「视谁为敌」上下文的敌对判定(全局规则,见类注释):
     * {@code 敌对目标 = 敌对生物 ∪ 已被激怒的中立生物 ∪ 消费方额外声明的实体
     * ∪ 「非同队伍,且曾主动攻击过 viewer 的玩家」}。
     *
     * <p>{@code viewer} 是本次判定的观察者(如溅射施放者、被攻击方);viewer 不是玩家时无法确定
     * "曾主动攻击过谁",玩家一律不计入敌对。
     */
    public static boolean isHostile(Entity viewer, Entity target) {
        if (target == null) return false;
        // 原有语义:敌对生物 ∪ 已被激怒的中立生物 (∪ 消费方额外声明的实体)
        if (isHostile(target)) return true;
        if (!(viewer instanceof Player viewerPlayer)) return false;
        if (!(target instanceof Player targetPlayer)) return false;
        if (viewerPlayer == targetPlayer) return false;
        // 同队豁免("非同队伍"才可能为敌对):任何一方未加入队伍时不算同队。
        // 注意:EventTargetCollector「未加入队伍视为全服玩家」的既有约定只适用于发奖,不适用于此处。
        if (viewerPlayer.getTeam() != null && viewerPlayer.isAlliedTo(targetPlayer)) return false;
        return PlayerHostilityTracker.hasAttacked(viewerPlayer, targetPlayer);
    }
}
