package com.merlinkitsune.starengine.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 旧 {@code astral_dice-common.toml}(Forge/NeoForge {@code *ConfigSpec} 产物)一次性迁入
 * Cloth Config 的 {@code astral_dice.json}。
 *
 * <p>为什么必须有这一步:换成 AutoConfig 后配置的存储格式(.toml → .json)、文件名
 * (astral_dice-common → astral_dice)和结构(扁平 snake_case → 嵌套 camelCase)全变,
 * 而旧实现恰恰是有「跨版本保留玩家配置」语义的({@code backupOldConfigIfNeeded} +
 * Forge 的值继承)。不加迁移就等于把玩家的调参全部静默重置。
 *
 * <p>触发条件(三者同时成立才动作):
 * <ol>
 *   <li>{@code config/astral_dice.json} <b>不存在</b>——已迁过或玩家已建立新配置就不动;</li>
 *   <li>{@code config/astral_dice-common.toml} <b>存在</b>——否则是全新安装,交给 AutoConfig
 *       自己写默认值即可(实测 GsonConfigSerializer.deserialize 在文件缺失时返回
 *       createDefault 而不抛异常,ConfigManager 随后会 save 出默认文件);</li>
 *   <li>旧文件解析未抛异常。</li>
 * </ol>
 *
 * <p>迁移策略是「seed 而非覆盖」:用旧值构造一个 {@link StarEngineCommonConfig} 实例,
 * 经 {@link GsonConfigSerializer} 写盘;旧文件里没有的键保持新 schema 的默认值。
 * 迁移后旧文件重命名为 {@code astral_dice-common.toml.bak} 保留(不删,保留人工回滚余地),
 * 与新格式文件并存——AutoConfig 不会去读 .bak。
 *
 * <p>路径为什么调 Cloth Config 的 {@link Utils#getConfigFolder()}:它必须与
 * {@link GsonConfigSerializer} 内部的 {@code getConfigPath()} 完全一致,否则会写到一个
 * 序列化器读不到的地方。该方法是 public static(两侧均已核对),且</b>两侧的
 * {@code UtilsImpl} 都实现为 {@code FMLPaths.CONFIGDIR.get()}(已反编译确认),
 * 所以拿到的是标准 config 目录。
 */
final class LegacyCommonTomlImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("starengine_lib/config");

    /** 旧配置文件(相对 config 目录)。 */
    private static final String LEGACY_FILE_NAME = "astral_dice-common.toml";

    private LegacyCommonTomlImporter() {
    }

    static void importIfNeeded() {
        try {
            Path configFolder = Utils.getConfigFolder();
            Config definition = StarEngineCommonConfig.class.getAnnotation(Config.class);
            if (definition == null) {
                // 不该发生:没注解的话 AutoConfig.register 也会先抛
                return;
            }
            Path target = configFolder.resolve(definition.name() + ".json");
            if (Files.exists(target)) {
                return;
            }
            Path legacy = configFolder.resolve(LEGACY_FILE_NAME);
            if (!Files.exists(legacy)) {
                return;
            }

            Map<String, String> flat = parseFlatToml(Files.readAllLines(legacy, StandardCharsets.UTF_8));
            StarEngineCommonConfig seed = new StarEngineCommonConfig();
            int applied = applyLegacy(flat, seed);
            new GsonConfigSerializer<>(definition, StarEngineCommonConfig.class).serialize(seed);

            Path backup = legacy.resolveSibling(LEGACY_FILE_NAME + ".bak");
            Files.move(legacy, backup, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("[StarEngine Lib] 已把旧配置 {} 的 {} 项迁入 {};原文件备份为 {}",
                    LEGACY_FILE_NAME, applied, target.getFileName(), backup.getFileName());
        } catch (Exception e) {
            // 迁移失败不能让游戏起不来:交给 AutoConfig 走默认值即可,但必须留下痕迹。
            LOGGER.warn("[StarEngine Lib] 迁移旧配置 {} 失败,将使用默认值: {}",
                    LEGACY_FILE_NAME, e.toString());
        }
    }

    /**
     * 极简 TOML 平铺解析:只认 {@code key = value} 与 {@code [section]} 两种行,
     * 忽略整行注释与行尾注释。够用是因为旧配置全由
     * {@code defineInRange}/{@code define} 生成,只有 int 与 boolean,没有数组/表数组/引号值
     * 的嵌套结构。返回值键为 {@code section.key}(无 section 时就是 {@code key})。
     */
    private static Map<String, String> parseFlatToml(List<String> lines) {
        Map<String, String> flat = new LinkedHashMap<>();
        String section = "";
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            if (key.length() > 1 && key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            String value = stripInlineComment(line.substring(eq + 1)).trim();
            if (value.isEmpty()) {
                continue;
            }
            flat.put(section.isEmpty() ? key : section + "." + key, value);
        }
        return flat;
    }

    // NightConfig 写出的行尾注释以 " #" 开头;值里不会出现 '#'(只有 int/boolean),
    // 因此直接截断第一个 '#' 即可,不需要处理引号内的 '#'。
    private static String stripInlineComment(String value) {
        int hash = value.indexOf('#');
        return hash < 0 ? value : value.substring(0, hash);
    }

    /** 把旧键值写进 seed,返回实际命中的项数(未命中的键保持默认值)。 */
    private static int applyLegacy(Map<String, String> flat, StarEngineCommonConfig seed) {
        int applied = 0;

        Integer intValue;
        if ((intValue = asInt(flat, "max_starlight")) != null) {
            seed.maxStarlight = intValue;
            applied++;
        }
        if ((intValue = asInt(flat, "max_marker")) != null) {
            seed.maxMarker = intValue;
            applied++;
        }
        if ((intValue = asInt(flat, "effect_card_cooldown_seconds")) != null) {
            seed.effectCardCooldownSeconds = intValue;
            applied++;
        }
        if ((intValue = asInt(flat, "max_effect_stacks")) != null) {
            seed.maxEffectStacks = intValue;
            applied++;
        }

        Boolean boolValue;
        if ((boolValue = asBool(flat, "give_guide_book_on_first_join")) != null) {
            seed.giveGuideBookOnFirstJoin = boolValue;
            applied++;
        }

        // 旧文件里这三个键在 [event_system] 段下;但若玩家手工把段头删了也能兜住:
        // 先按带段路径找,找不到再按扁平键找一次。
        if ((intValue = asInt(flat, "event_system.event_range", "event_range")) != null) {
            seed.eventRange = intValue;
            applied++;
        }
        if ((boolValue = asBool(flat, "event_system.event_apply_mc_team", "event_apply_mc_team")) != null) {
            seed.eventApplyMcTeam = boolValue;
            applied++;
        }
        if ((boolValue = asBool(flat, "event_system.event_apply_ftb_team", "event_apply_ftb_team")) != null) {
            seed.eventApplyFtbTeam = boolValue;
            applied++;
        }
        if ((boolValue = asBool(flat, "event_system.event_apply_opac", "event_apply_opac")) != null) {
            seed.eventApplyOpac = boolValue;
            applied++;
        }
        if ((boolValue = asBool(flat, "event_system.event_apply_maid", "event_apply_maid")) != null) {
            seed.eventApplyMaid = boolValue;
            applied++;
        }
        if ((intValue = asInt(flat, "chips.hand_fan_big_range", "hand_fan_big_range")) != null) {
            seed.handFanBigRange = intValue;
            applied++;
        }
        if ((intValue = asInt(flat, "actionbar.actionbar_duration_ticks", "actionbar_duration_ticks")) != null) {
            seed.actionbarDurationTicks = intValue;
            applied++;
        }
        if ((intValue = asInt(flat, "actionbar.actionbar_fade_ticks", "actionbar_fade_ticks")) != null) {
            seed.actionbarFadeTicks = intValue;
            applied++;
        }
        return applied;
    }

    /** 按给定候选键依次查找并解析为 int;全未命中或解析失败返回 null。 */
    private static Integer asInt(Map<String, String> flat, String... keys) {
        String raw = firstPresent(flat, keys);
        if (raw == null) {
            return null;
        }
        try {
            // 容忍 32_0 / 320L 这类 NightConfig 可能写出的形式
            return Integer.valueOf(raw.replace("_", "").replace("L", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 按给定候选键依次查找并解析为 boolean;全未命中返回 null。 */
    private static Boolean asBool(Map<String, String> flat, String... keys) {
        String raw = firstPresent(flat, keys);
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String firstPresent(Map<String, String> flat, String... keys) {
        for (String key : keys) {
            String value = flat.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
