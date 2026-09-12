package com.merlinkitsune.starenginelib.component;

/**
 * 玩法配置值快照:由消费方 mod 从自己的配置系统读取后传入
 * {@link GameplayConstants#applyConfig(GameplayConfigValues)}。
 *
 * <p>存在的意义:1.20.1 用 {@code ForgeConfigSpec}、1.21.1 用 {@code ModConfigSpec},
 * 两侧配置类型不通用,无法放进共享源码;共享的是「值」而不是「配置对象」,
 * 因此把可配置项收拢成本 record,配置读取留在各平台侧。
 */
public record GameplayConfigValues(
        int maxStarlight,
        int maxMarker,
        int effectCardCooldownSeconds,
        int maxEffectStacks,
        boolean giveGuideBookOnFirstJoin,
        int eventRange,
        boolean eventApplyMcTeam,
        boolean eventApplyFtbTeam,
        boolean eventApplyOpac,
        boolean eventApplyMaid,
        int handFanBigRange,
        int actionbarDurationTicks,
        int actionbarFadeTicks) {
}
