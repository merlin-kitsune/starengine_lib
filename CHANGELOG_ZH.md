# 中文更新日志

> 本文件仅收录中文更新日志；英文版见 [`CHANGELOG.md`](CHANGELOG.md)。
> 两个文件按版本号一一对应：同一版本号在两边各出现一次，每次改动必须同时更新中英两份，禁止只改一侧。

## 未发布（1.0.0）

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。

### 新内容

- 首次从 Astral Dice 1.20.1 / 1.21.1 多加载器单仓中提取出 **StarEngine Lib**（`starengine_lib`）：将两个 MC 版本可共同引用的 34 个共享单元（18 个 `MobEffect` 实现、`AstralEventType`/`EventContext`/`EventEffect`/`EventTargetCollector` 事件框架、目标选择框架、`BossEntityUtil`、`ClientDamageNumbers`、`GameplayConstants`、`SignActiveTriggeredEvent`、`AmethystDiceHandler`）下沉至本库，供后续 **Astral Dice Extra（星之骰戏：扩展）** 及同系列模组复用。
- 本库**不注册任何**注册表条目（物品/效果/附件/数据组件/能力全部留在消费方 mod）。因此引入或升级本库都不会改变任何 `ResourceLocation` 归属，既不损坏存档，也不破坏数据包。

### 工程

- **多加载器单仓结构**：`common/src/main/java` 是共享**源码目录**（不是 Gradle 子项目）；`neoforge-1.21.1`（Java 21 · Mojmap + Parchment · ModDevGradle 2.0.141）与 `forge-1.20.1`（Java 17 · reobf SRG · ModDevGradle Legacy 2.0.144）两个子项目各自把它加入 `sourceSet` 并编译同一份源码一次，每个 MC 版本各产出一个 jar。
- **共享源码的两条硬约束**，均由编译期而非约定强制：`common` 不得使用 Java 21 独有语法（record pattern / switch pattern / `SequencedCollection` 等，由 Java 17 工具链编译同一份源码强制），且只能使用两版 MC 中都存在且**签名一致**的 API。
- **加载器差异留在平台子项目中**，每个差异各写一份：`ActionBarManager`（`DeltaTracker` / `float partialTick`）、`ModEffectRemoval`（`Holder<MobEffect>` / `MobEffect`）、自定义数据键（原版 `DataComponent` / Forge 的 `ItemDataKey`）、饰品集成（`CuriosCompat`，仅 Forge）。
- **加载器差异以 shim 收敛**：`platform/LoaderEvent` 是抽象基类，两侧分别继承各自的 Event Bus 事件类型（NeoForge / Forge）；`platform/LoaderTags` 暴露 `c:bosses` 通用 tag，取自平台自身的 `Tags` 类。共享文件（如 `SignActiveTriggeredEvent`、`BossEntityUtil`）只引用 shim，因此共享代码中不再残留任何 `net.neoforged.*` / `net.minecraftforge.*` 字面量。
- **配置解耦**：`ModConfigSpec` 与 `ForgeConfigSpec` 不通用，故配置类不进库 —— 库内提供平台无关的 `GameplayConfigValues`（纯 record 值快照）与 `GameplayConstants.applyConfig(...)`，两侧的 `GameplayConfigBinder.refresh()` 读取自身配置后回填快照。**配置文件名与路径不变**（`config/astral_dice-common.toml`），存量配置不重置。
- **Maven 发布**：NeoForge 侧 `from components.java` 发布普通 Mojmap jar（NeoForge 自 1.20.5 起编译与生产同为 Mojmap，无需重映射）；Forge 1.20.1 侧发布 `reobfJar`，即**生产 SRG jar** —— 消费方 MDG LegacyForge 会在解析期把 SRG jar 重映射为 dev Mojmap 命名，若发布未重混淆的 dev jar，生产环境会因成员名为 Mojmap 而 `NoSuchFieldError`。两侧均显式指定 `artifactId`，使坐标与 `archivesName` 一致。
- **build 自动部署**：`build` 结束后 `pushToPack` 会把库 jar 复制进两个整合包的 `mods` 目录（Forge 测试包推 SRG jar、NeoForge 包推普通 jar），与消费方既有的「build 即部署」约定一致。此步不可省略：消费方把 `starengine_lib` 声明为必需前置，库 jar 不在包内会导致整合包直接拒绝启动。
