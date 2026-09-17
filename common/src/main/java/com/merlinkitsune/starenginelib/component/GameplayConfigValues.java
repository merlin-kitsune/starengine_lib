package com.merlinkitsune.starenginelib.component;

/**
 * 玩法配置值快照:由消费方 mod 从自己的配置系统读取后传入
 * {@link GameplayConstants#applyConfig(GameplayConfigValues)}。
 *
 * <p>存在的意义:{@code ModConfigSpec}(NeoForge)与 {@code ForgeConfigSpec}(Forge)不通用,
 * 配置文件的读写与配置 GUI 只能各平台自己做,无法放进共享源码;共享的是「值」而不是「配置对象」,
 * 因此把**仍可配置**的项收拢成本 record,配置读取留在各平台侧。
 *
 * <p>字段即契约:消费方按**位置**构造本 record,增删或改序字段会让消费方在编译期失败;
 * 调整可配置项时必须两侧同步(消费方配置项定义 + 本 record)。
 */
public record GameplayConfigValues(
        boolean giveGuideBookOnFirstJoin,
        boolean eventApplyMcTeam,
        boolean eventApplyFtbTeam,
        boolean eventApplyOpac,
        int actionbarDurationTicks,
        int actionbarFadeTicks) {
}
