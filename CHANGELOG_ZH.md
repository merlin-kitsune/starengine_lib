# 中文更新日志

> 本文件仅收录中文更新日志；英文版见 [`CHANGELOG.md`](CHANGELOG.md)。
> 两个文件按版本号一一对应：同一版本号在两边各出现一次，每次改动必须同时更新中英两份，禁止只改一侧。

## 未发布（1.0.0-SNAPSHOT.1）

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：本库此前以 `1.1.0` 在本地构建（除 `mavenLocal` 外未发布到任何地方）。为首次对外发布，已改号为 **`1.0.0-SNAPSHOT.1`**，故本条目覆盖 `1.0.0-SNAPSHOT.1` 的全部内容。

### 新内容

- 首次从 Astral Dice 1.20.1 / 1.21.1 多加载器单仓中提取出 **StarEngine Lib**（`starengine_lib`）：将两个 MC 版本可共同引用的 34 个共享单元（18 个 `MobEffect` 实现、`AstralEventType`/`EventContext`/`EventEffect`/`EventTargetCollector` 事件框架、目标选择框架、`BossEntityUtil`、`ClientDamageNumbers`、`GameplayConstants`、`SignActiveTriggeredEvent`、`AmethystDiceHandler`）下沉至本库，供后续 **Astral Dice Extra（星之骰戏：扩展）** 及同系列模组复用。
- 本库**不注册任何**注册表条目（物品/效果/附件/数据组件/能力全部留在消费方 mod）。因此引入或升级本库都不会改变任何 `ResourceLocation` 归属，既不损坏存档，也不破坏数据包。
- 新增**公共配置模块**（基于 Cloth Config 的 AutoConfig）：`StarEngineCommonConfig`（`@Config(name = "astral_dice")`，13 个配置项：`maxStarlight` / `maxMarker` / `effectCardCooldownSeconds` / `maxEffectStacks` / `giveGuideBookOnFirstJoin` / `eventRange` / `eventApplyMcTeam` / `eventApplyFtbTeam` / `eventApplyOpac` / `eventApplyMaid` / `handFanBigRange` / `actionbarDurationTicks` / `actionbarFadeTicks`）、生命周期入口 `StarEngineConfigs`、一次性迁移器 `LegacyCommonTomlImporter` 与屏幕工厂 `StarEngineConfigScreen`。文件读写与配置 GUI 全部由 Cloth 的 `AutoConfig` 接管，消费方只需在 `@Mod` 构造期调一次 `StarEngineConfigs.register()`，并按平台注册自己的屏幕扩展点。**字段名即持久化契约：重命名任一 `public` 字段等于静默重置玩家该项配置（并使其语言标签退化为键名），与注册名同级，改动前必须按破坏性变更处理。** ⚠️ 该模块的命名空间被固定为 `astral_dice`（同时决定文件名 `config/astral_dice.json` 与语言键前缀 `text.autoconfig.astral_dice.*`），因此当前**只支持单一消费方**；若要被第二个模组复用，需要把 `register()` 改成由消费方传入自己的 `@Config` 类（含各自命名空间与语言文件）。

### 工程

- **多加载器单仓结构**：`common/src/main/java` 是共享**源码目录**（不是 Gradle 子项目）；`neoforge-1.21.1`（Java 21 · Mojmap + Parchment · ModDevGradle 2.0.141）与 `forge-1.20.1`（Java 17 · reobf SRG · ModDevGradle Legacy 2.0.144）两个子项目各自把它加入 `sourceSet` 并编译同一份源码一次，每个 MC 版本各产出一个 jar。
- **共享源码的两条硬约束**，均由编译期而非约定强制：`common` 不得使用 Java 21 独有语法（record pattern / switch pattern / `SequencedCollection` 等，由 Java 17 工具链编译同一份源码强制），且只能使用两版 MC 中都存在且**签名一致**的 API。
- **加载器差异留在平台子项目中**，每个差异各写一份：`ActionBarManager`（`DeltaTracker` / `float partialTick`）、`ModEffectRemoval`（`Holder<MobEffect>` / `MobEffect`）、自定义数据键（原版 `DataComponent` / Forge 的 `ItemDataKey`）、饰品集成（`CuriosCompat`，仅 Forge）。
- **加载器差异以 shim 收敛**：`platform/LoaderEvent` 是抽象基类，两侧分别继承各自的 Event Bus 事件类型（NeoForge / Forge）；`platform/LoaderTags` 暴露 `c:bosses` 通用 tag，取自平台自身的 `Tags` 类。共享文件（如 `SignActiveTriggeredEvent`、`BossEntityUtil`）只引用 shim，因此共享代码中不再残留任何 `net.neoforged.*` / `net.minecraftforge.*` 字面量。
- **配置模块进库（Cloth Config AutoConfig）**：`ModConfigSpec` 与 `ForgeConfigSpec` 不通用，但 **Cloth Config 的 `ConfigBuilder` / `ConfigEntryBuilder` / `autoconfig.*` 在 15.0.140（neoforge 1.21.1）与 11.1.136（forge 1.20.1）上已逐一字节比对一致**（这是配置 schema 能进 `common` 的唯一依据），故配置 schema 与读写逻辑下沉至库内，平台侧只保留各自的屏幕扩展点。库内仍保留平台无关的 `GameplayConfigValues`（纯 record 值快照）与 `GameplayConstants.applyConfig(...)`，改由 `AutoConfig` 的 load/save 监听器（返回 `InteractionResult.PASS` 表示「仅观察、不拦截」）回填。**破坏性变更**：配置文件由 `config/astral_dice-common.toml`（TOML/扁平 snake_case）变为 `config/astral_dice.json`（JSON/嵌套 camelCase，`.json` 后缀由 `GsonConfigSerializer.getConfigPath()` 硬编码）；由 `LegacyCommonTomlImporter` 在旧 TOML 存在且新 JSON 不存在时一次性 seed 迁移，并把旧文件重命名为 `.bak`，解析异常时退回默认值而不阻断启动。直接手改 JSON 越界由 `validatePostLoad()` 钳回区间（`@ConfigEntry.BoundedDiscrete` 只约束 GUI）。
- **Cloth Config 依赖接入**：neoforge 侧 `implementation "me.shedaniel.cloth:cloth-config-neoforge:15.0.140"`（该 jar 为官方映射、含 0 处 SRG 名，无需重映射）；forge 侧 `modImplementation "me.shedaniel.cloth:cloth-config-forge:11.1.136"`（该 jar 含 819 处 `m_xxxxx_` SRG 方法名，必须由 MDG legacyforge 在解析期重映射为 dev Mojmap，与 Curios 同机制）。库自身两侧 `mods.toml` 也把 `cloth_config` 声明为必需前置（`type="required"` / `mandatory=true`，`ordering="AFTER"`）—— `common` 的 `config` 包直接引用 `me.shedaniel.autoconfig.*`，缺失即 `NoClassDefFoundError`。
- **Maven 发布**：NeoForge 侧 `from components.java` 发布普通 Mojmap jar（NeoForge 自 1.20.5 起编译与生产同为 Mojmap，无需重映射）；Forge 1.20.1 侧发布 `reobfJar`，即**生产 SRG jar** —— 消费方 MDG LegacyForge 会在解析期把 SRG jar 重映射为 dev Mojmap 命名，若发布未重混淆的 dev jar，生产环境会因成员名为 Mojmap 而 `NoSuchFieldError`。两侧均显式指定 `artifactId`，使坐标与 `archivesName` 一致。
- **build 自动部署**：`build` 结束后 `pushToPack` 会把库 jar 复制进两个整合包的 `mods` 目录（Forge 测试包推 SRG jar、NeoForge 包推普通 jar），与消费方既有的「build 即部署」约定一致。此步不可省略：消费方把 `starengine_lib` 声明为必需前置，库 jar 不在包内会导致整合包直接拒绝启动。
- **托管到 GitHub，并配上独立 CI 与 Release**：仓库现位于 <https://github.com/merlin-kitsune/starengine_lib>，新增 `.github/workflows/build.yml`，沿用 Astral Dice 的发布规范：每次 push / PR / 手动触发均以 JDK 21 + JDK 17 构建双平台，并上传 `starengine_lib-jars` 构建产物；push 到 `main` 自动打 tag（tag = 基础版本号，无 `v` 前缀、无 `+加载器` 后缀）；push tag 则创建/更新 GitHub Release，附件为两个平台的 jar。相较消费方规则**多加一道守卫**：版本号含 `-`（即快照）时**不打 tag**，否则 `1.0.0-SNAPSHOT.1` 会被截成 `1.0.0` 并被当成正式发布自动打出，且 tag 难以回收。
- **前置版本区间下界必须是 `1.0.0-SNAPSHOT` 而非 `1.0`**：按 Maven `ComparableVersion` 语义 `1.0.0-SNAPSHOT.1 < 1.0`，消费方若写 `versionRange="[1.0,2.0)"` 会拒绝所有 `1.0.0-SNAPSHOT.x`，游戏直接以「不满足必需前置」拒绝加载。已用 `maven-artifact` 3.8.5 实测（两侧加载器均经 `MavenVersionAdapter.createFromVersionSpec` → `VersionRange.createFromVersionSpec`）：`[1.0,2.0)` 不含 `1.0.0-SNAPSHOT.1`，而 `[1.0.0-SNAPSHOT,2.0)` 连同 `1.0.0` / `1.1.0` 一并包含。
- **消费方 CI 改为先构建本库**：本库仅发布到 `mavenLocal`，故 Astral Dice 的 `build.yml` 会先 checkout 本仓并执行 `./gradlew publishToMavenLocal`，再构建自身。因此两个仓库的版本号必须严格对齐（消费方的 `starengine_lib_version` ↔ 本库的 `lib_version`），否则消费方 CI 会断在依赖解析。
