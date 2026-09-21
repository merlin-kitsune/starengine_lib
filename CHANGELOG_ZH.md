# 中文更新日志

> 本文件仅收录中文更新日志；英文版见 [`CHANGELOG.md`](CHANGELOG.md)。
> 两个文件按版本号一一对应：同一版本号在两边各出现一次，每次改动必须同时更新中英两份，禁止只改一侧。

## 未发布（1.0.0-SNAPSHOT.14）

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加「再次修改」条目。
> 版本号说明：`.14` 是**新增玩家余额账本底层**版本（星币钱包功能的库侧实现）。

### 新功能

- **新增玩家余额账本（星币钱包的库侧底层）**：`common` 新增 `economy` 包 ——
  `EconomyStorage`（平台存储 seam：读 / 写 / 离线读）、`StarEngineEconomy`（对外 API：`getBalance / setBalance / hasBalance /
  deposit / withdraw / transfer` + 离线读；**存储缺失时一律返回 0 / false 且不抛异常**，便于第三方模组（如 FTB）安全联动）、
  `StarCoinCommand`（`/starcoin add|set|remove|get|rank`，其中 `rank` 含**离线玩家**）。
  余额以**玩家持久化 NBT** 承载 —— 库的既有不变量是「不注册任何注册表条目」，故**不使用** `AttachmentType`；
  并在 `PlayerEvent.Clone` 时显式复制 ⇒ **余额不因死亡丢失**。两平台各一份实现
  （`NeoForgeEconomyStorage` / `ForgeEconomyStorage`，差异仅事件包名与玩家 `.dat` 里持久化数据的根段
  `NeoForgeData` / `ForgeData`）；26.1.2 侧**未接入**（API 安全降级为不可用）。库入口构造时注入存储实现。
- **新增命令权限 seam `CommandPermissionGate`**：`CommandSourceStack#hasPermission(int)` 在 **26.1.2** 上签名不一致
  （1.20.1 / 1.21.1 一致），故共享源码不直接写权限谓词，改由平台侧注入 `(source, level) -> source.hasPermission(level)`。

### 工程

- 三平台版本号 `1.0.0-SNAPSHOT.13 → 1.0.0-SNAPSHOT.14` 并已 `publishToMavenLocal`；
  三平台 `build` 通过（含共享源码在 26.1.2 的编译验证，正是它保证了 keep-common-version-agnostic 这条红线）。

## 1.0.0-SNAPSHOT.13

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：`.13` 是**资源包元数据修复 + 版本线统一**版本 —— 给 `forge-1.20.1` 补上缺失的 `pack.mcmeta`（Forge 把每个模组 jar 当作资源包校验，缺该文件时在加载界面报一条警告，且该模组的资源包**不会被注册**），并沿用 `.6` 的加载门槛修复。按用户 2026-09-17 指令，库版本号统一为 `1.0.0-SNAPSHOT.10`（与消费方 `2.0.0-SNAPSHOT.10` 的 patch 段同号）；该修复原先以 `.7` 为号在途（只发布过 mavenLocal、从未进入 git），按上面的合并约定并入本节，不单列 `.7` 小节。必须 bump 的原因同前几版：该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不把它当作 changing module —— 不 bump 则消费方会继续解析 `mavenLocal` 里那份没有 `pack.mcmeta`（且需 `--refresh-dependencies` 才能重取）的旧 jar，警告依旧。

- **新增模组图标，并把描述文本统一到三平台。** 仓库根目录的 `icon.png` 现作为各 jar 的模组图标发布：同一份字节（2671 字节，PNG 签名 `89 50 4E 47 0D 0A 1A 0A`）被复制到 `neoforge-1.21.1/src/main/resources/icon.png`、`forge-1.20.1/src/main/resources/icon.png` 与 `neoforge-26.1.2/src/main/resources/icon.png`，即 **jar 根目录** —— 正是 FML 的 `logofile` 解析器查找的位置（与消费方 `astral_dice` 各 jar 用根级 `icon.png` 的做法同源）。三份元数据模板均新增 `logofile="icon.png"`，且 `description` 统一为 1.21.1 侧文本：`A common library for the Astral Dice mod. It includes cross-platform implementations and supports functional integration with other mods.` 原先的中文多段描述随之从 `forge-1.20.1` 与 `neoforge-26.1.2` 侧移除，模组列表条目三平台一致。注：仓库根目录的 `icon.png` 不是任何子项目的资源根，真正随 jar 发布的是 `src/main/resources` 下这三份副本。本次仅元数据与资源改动，**库内 Java 源码零改动**。三平台版本号 `1.0.0-SNAPSHOT.12 → 1.0.0-SNAPSHOT.13`，必须 bump 的原因同前几版：该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不把它当作 changing module —— 不 bump 则消费方会继续解析 `mavenLocal` 里那份既没有图标、描述也仍旧的 jar。

### 工程

- **修复：Forge 1.20.1 加载界面报「File … failed to load a valid ResourcePackInfo」**。现象：消费方 `run/1.20.1` 客户端进入 Forge 的 `Warning while loading mods` 界面，列出 1 条警告并指向库 jar（用户截图与 `latest.log` 均指向 `starengine_lib-forge-1.20.1-1.0.0-SNAPSHOT.5.jar`；`.6` 同样缺该文件，故该警告与快照序号无关），日志对应行 `[net.minecraft.server.packs.repository.Pack/]: Missing metadata in pack mod:starengine_lib`。根因（**读本机 Forge 1.20.1 源码取证**，非推测）：`net.minecraftforge.client.loading.ClientModLoader#clientPackFinder`（行 154-165）对**每一个**模组文件调用 `Pack.readMetaAndCreate(name, …, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, …)`，返回 `null` 时执行 `ModLoader.get().addWarning(new ModLoadingWarning(mod, ModLoadingStage.ERROR, "fml.modloading.brokenresources", e.getKey()))` —— `assets/forge/lang/*.json` 里该键的文案即 `File {2} failed to load a valid ResourcePackInfo`；`Pack.readMetaAndCreate` 在 jar 内**没有 `pack.mcmeta`**（或元数据无法按该 PackType 解析）时返回 null。服务端同源：`net.minecraftforge.server.ServerLifecycleHooks`（行 214）在 `PackType.SERVER_DATA` 上做同一检查。⇒ 凡缺 `pack.mcmeta` 的 Forge 1.20.1 模组 jar 都会各报一条警告，且其资源包**不被注册**（库当前无资源 ⇒ 无功能损失，但属静默失效面）。处置：新增 `forge-1.20.1/src/main/resources/pack.mcmeta`（`pack_format` **15**、`description` = `starengine_lib resources`，与消费方 `forge-1.20.1/src/main/resources/pack.mcmeta` 同形）。**范围仅 Forge 1.20.1**：NeoForge 1.21.1 / 26.1.2 不走这两段代码（两侧运行日志无该警告，消费方三线 jar 也只在 1.20.1 侧带 `pack.mcmeta`），且各版本 `pack_format` 不同，故**不猜测**其余平台的取值（需要时按各自版本客户端源码取值再补）。三平台 `lib_version` / `mod_version` 由 `1.0.0-SNAPSHOT.6` 统一到 **`1.0.0-SNAPSHOT.10`**（后缀各自 `+neoforge_1.21.1` / `+forge_1.20.1` / `+neoforge_26.1.2`，与消费方 `2.0.0-SNAPSHOT.10` 的 patch 段同号）并已 `publishToMavenLocal`；**库内 Java 源码零改动**。
- **新增配置驱动常量 `ALLOW_FIREARM_DAMAGE`（默认 `false`），供消费方公共配置 `allow_firearm_damage` 推入**。`component/GameplayConstants` 新增**非 final** 的 `public static boolean ALLOW_FIREARM_DAMAGE = false`（默认**仍屏蔽**枪弹/炮弹类伤害，与既有行为完全一致；配置为 `true` 时才允许这类伤害计入法伤），并放在既有的「仍可配置」布尔量一组里（`EVENT_APPLY_OPAC` 之后）；`component/GameplayConfigValues` 在**末尾追加**第 7 个分量 `boolean allowFirearmDamage`（前 6 个分量的名称与顺序**逐字未变**，消费方仍按位置构造，属**源码兼容**的追加）；`applyConfig(GameplayConfigValues)` 在「仅写入仍可配置的少量字段」那一段内写入该值，固定常量派生 tick 值的部分未动。库内仍**不读配置文件、不持有配置 schema**，配置文件与配置项定义照旧留在消费方。三平台 `lib_version` / `mod_version` 由 `1.0.0-SNAPSHOT.11` 统一为 **`1.0.0-SNAPSHOT.12`**（后缀各自 `+neoforge_1.21.1` / `+forge_1.20.1` / `+neoforge_26.1.2`）；必须 bump 的原因同前几版：该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不把它当作 changing module —— 不 bump 则消费方会继续解析 `mavenLocal` 里那份既没有该常量、也没有第 7 个 record 分量的旧 jar，构造快照时编译期即报错。

## 1.0.0-SNAPSHOT.6

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：`.6` 是**加载门槛修复**版本 —— 修掉「库自身被 FML 拒载」的根因，并把 forge 侧门槛对齐到已文档化口径。必须 bump 的原因同前几版：该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不把它当作 changing module —— 不 bump 则消费方会继续解析 `mavenLocal` 里那份**带错 `loaderVersion`、会被 FML 拒载**的 `.5` jar。

### 工程

- **修复：库自身被 FML 拒载（`loaderVersion` 误用 NeoForge 版本带）**。现象：消费方 `run/1.21.1` 连续两次启动在 FML 阶段崩溃（`crash-2026-09-17_20.41.28-fml.txt` / `…_20.42.17-fml.txt`），报 `Mod File …\starengine_lib-neoforge-1.21.1-1.0.0-SNAPSHOT.5.jar needs language provider javafml:21.1 or above, and below 21.2 to load / We have found 4.0.42`，随后消费方 `astral_dice` 被判 `requires starengine_lib 1.0.0-SNAPSHOT.5 or above … Currently, starengine_lib is not installed`。根因：`neoforge-1.21.1/gradle.properties` 的 `loader_version_range` 曾被写成 **NeoForge 版本带** `[21.1,21.2)`，而模板 `neoforge.mods.toml:2 loaderVersion="${loader_version_range}"` 比的是 **javafml 语言提供者版本**（本机 NeoForge 21.1.235 报 `4.0.42`）。处置：该属性改为语言区间 **`[1,)`**（与消费方 `astral_dice_multiloader-next/neoforge-1.21.1/gradle.properties:23` 同值）；NeoForge 版本带 `[21.1,21.2)` 仍由模板里 `modId="neoforge"` 依赖承担（**区间未变**）。`neoforge-26.1.2` 侧本来就是 `[1,)`（未触发拒载），本次不改其值。
- **同批：`forge-1.20.1` 门槛对齐已文档化口径 + 三平台版本同步**。`loaderVersion` 由 `[47,)` 收敛为 **`[47,48)`**（javafml 语言提供者的版本号就是 Forge 主系列号 `47`，写完整版本号区间会被 FML 判 `fml.language.missingversion`），`modId="forge"` 依赖改用 `build.gradle` 从 `forge_version` **派生**的精确区间 **`[47.4.10,48)`**（模板占位由 `${loader_version_range}` 改为 `${forge_version_range}`），与消费方 `forge-1.20.1/build.gradle:139-152` 的口径逐条一致。三平台 `lib_version` / `mod_version` 由 `1.0.0-SNAPSHOT.5` → **`1.0.0-SNAPSHOT.6`**（后缀各自 `+neoforge_1.21.1` / `+forge_1.20.1` / `+neoforge_26.1.2`）并已 `publishToMavenLocal`（产物落在 `~/.m2/repository/com/merlinkitsune/starenginelib/<artifact>/1.0.0-SNAPSHOT.6/`）；开包断言三个 jar 的 `loaderVersion` 分别为 `[1,)` / `[47,48)` / `[1,)`、forge 依赖 `versionRange="[47.4.10,48)"`，`neoforge` / `minecraft` 依赖区间未变。**库内 Java 源码零改动**（只改元数据、模板注释与文档）；消费方引脚与 CI ref 由消费方侧同步，不在本次范围。

## 1.0.0-SNAPSHOT.5

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：`.5` 是**合并收尾**版本 —— 主线（`multi-1.20.1-1.21.1` @ `8f68482`）已并入消费方 `multi-dev-next`，库侧据此按期删除「合并后即删」的过渡符号。必须 bump 的原因同 `.4`：该版本号不以 `-SNAPSHOT` 结尾（属普通版本），Gradle 不把它当作 changing module，不 bump 则消费方会继续解析 `mavenLocal` 里的 `.4` 旧 jar。

### 工程

- **过渡符号按期删除（已删部分）**：删除前**逐符号 grep 举证**合并后消费方三线（`neoforge-1.21.1` / `forge-1.20.1` / `neoforge-26.1.2`）零引用，然后删除 ①`EventTargetCollector` 的 `collectTargets` / `collectTeamTargets` / `collectMaids` / `isMaidOwnedBy`（含其 `GameplayConstants.EVENT_RANGE` / `EVENT_APPLY_MAID` 分支）、②`GameplayConstants.KOMACHI_EXTRA_PLAYS_CAP`、③`effect/ReadyEffect`（`.4` 才下沉、合并后三线零引用）。判定要点：`collectTargets` 的 6 处 grep 命中全部是消费方 `RandomCardHandler` 自己的同名方法（不同类、不同签名），库侧方法确为 0 引用；`EVENT_APPLY_MC_TEAM` / `EVENT_APPLY_FTB_TEAM` / `EVENT_APPLY_OPAC` 与 `EventTargetCollector.collectTeamPlayers` 是**在用**能力（消费方多处调用），原样保留。命令与输出见 `temp/t5-cleanup-notes-20260917.md`。
- **过渡符号按期删除（同批完成）——事件框架三件套**：`event/AstralEventType` / `EventContext` / `EventEffect` 三个文件整体删除（三者互相引用：`AstralEventType(ResourceLocation, EventEffect)` 与 `EventEffect.apply(EventContext)`、`AstralEventType.trigger(EventContext)`，必须同批删），并移除 `neoforge-26.1.2/build.gradle` 里专为 `AstralEventType` 留的 `sourceSets.main.java.exclude` 行（文件已删，该行只会变成无效残留）。**前提工序**：合并后的消费方 `event/AstralEventSystem.java` 曾在 import 段留下 4 处指向本库的 `import`（方法体已不用，但未解析的 import 就是编译错误），先由消费方侧提交 `7dc64cb` 清掉，库侧才可删除；此后 `git grep -E 'AstralEventType|EventContext|EventEffect' -- '*.java'` 在消费方三线为 **0 命中**。⚠️ 由于这一前提在 `.5` 首次发布之后才满足，`.5` 的库字节**重新发布过**（同版本号、不同字节）——消费方必须以 `--refresh-dependencies`（或清 mavenLocal 缓存）重新解析，否则会继续用带三件套的旧 `.5` 字节。
- **版本与消费方同步**：库版本 `1.0.0-SNAPSHOT.4 → 1.0.0-SNAPSHOT.5`（三个平台 `gradle.properties` 同号）；dev-next 两侧 `starengine_lib_version` 与 `starengine_lib_version_range`（两个 `mods.toml` 经 `${starengine_lib_version_range}` 展开）同步到 `[1.0.0-SNAPSHOT.5,2.0)`；消费方 CI 钉住的库检出 `ref` 换成 `.5` 删除三件套后的库提交 sha，并在注释里写明「库版本 bump 必须与本 ref 同一次提交更新」。⚠️ 库仓库目前**尚未 push**，该 sha 仅存在于本机，CI 检出会失败直到库被推送且该 sha 可达。

## 1.0.0-SNAPSHOT.4

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：`.4` 是 `.3` 的**续接**而非重做 —— 库内容只**新增**了 `common/effect/ReadyEffect`。必须 bump 的原因与 `.3` 相同：该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不会把它当作 changing module，不 bump 则消费方会继续解析 `mavenLocal` 里的 `.3` 旧 jar（缺该类 → 编译期报「找不到符号」）。`.3` 的条目原样保留在下一节。

### 新内容

- **`effect/ReadyEffect` 下沉进库**：该类在三线（`neoforge-1.21.1` / `forge-1.20.1` / `neoforge-26.1.2`）各有一份且**字节完全一致**，只 import 原版 `MobEffect` / `MobEffectCategory`，不引用任何消费方专有类 —— 是本轮按「三线字节一致 + 自包含」口径筛出的 53 个候选里**唯一尚未进库**者（其余 52 个已在前序 Phase 下沉或按原因排除，见「工程」）。下沉后位于 `common/src/main/java/com/merlinkitsune/starenginelib/effect/ReadyEffect.java`（14 行 / 540 字节），与原文件逐字节相同、仅 `package` 一行改为库包名，行尾保持 CRLF 不变。

### 工程

- **三线共享类的下沉判定口径固化**（清单：`temp/sink-manifest-20260917.md`）：候选闸门 = 「三个子项目同相对路径的 java 文件 Sha256 完全相同」（三线共 229 个 java 文件 → 候选 53 个）；自包含闸门 = 「引用闭包只落在候选集 ∪ 库内已有类 ∪ Minecraft/加载器 API」。排除项逐条列明并给出原因：非三线一致（176 个）、依赖消费方专有类（如 `ModItems` / `ModAttachments` / `BaseChipItem` / `DiceCombatModifiers` / `SpellDamageRegistry`）、以及 3 个 mixin（`mixin/trade/Merchant{Container,Menu,ResultSlot}Mixin`，需要 mixin 配置与 refmap，库内无此基建）。
- **合并前漂移核对（本次据此判定「不需再改这些类」）**：dev-next 已删除、而主线在删除之后又改过的库同源副本逐条比对 —— `component/GameplayConstants` 的常量改动（`MAX_MARKER` 16→32、`HAND_FAN_BIG_RANGE` 转 `final`、`MAX_STARLIGHT`/`EFFECT_CARD_COOLDOWN_SECONDS`/`MAX_EFFECT_STACKS` 转 `final` 等）库内**已取主线值**，两侧差异只剩「库用值快照 seam、主线用 `ModCommonConfig`」与按既有约定暂留的过渡符号（`EVENT_RANGE` / `EVENT_APPLY_MAID` / `KOMACHI_EXTRA_PLAYS_CAP` / `TARGET_SELECT_RADIUS`）；`event/EventTargetCollector` 库内**暂留全量实现**（合并前消费方仍在调用 `collectTargets(...)`）；`item/BossEntityUtil`、`event/SignActiveTriggeredEvent`、`event/ModEffectRemoval`、`client/ActionBarManager`（三个平台各一份）的差异全部是 shim 基类与注释。⇒ 主线这批改动不会在合并中丢失。
- **`common` 内两处消费方包名残留已清除**：`effect/HealingEffect` 与 `event/AmethystDiceHandler` 的 javadoc 里还留着 `{@link com.merlinkitsune.astral_dice.item.HealingManager}` 与 `{@link com.merlinkitsune.astral_dice.combat.SpellDamageRegistry}` —— 上一轮的包名重构只把 `starengine` 段换成 `starenginelib`，消费方包名被原样留下，不影响编译但违反「`common` 不得出现消费方专有类引用」的验收口径。改为纯文本描述，功能与签名零改动；清理后**全库（含三个平台子项目）对 `com.merlinkitsune.astral_dice` 的引用为 0**。
- **消费方区间下界抬到 `1.0.0-SNAPSHOT.4`**：dev-next worktree 两个 `gradle.properties` 的 `starengine_lib_version` 与 `starengine_lib_version_range` 已同步，两侧 `mods.toml` 经 `${starengine_lib_version_range}` 展开后同为新区间。语义与 `.3` 同构（同为「精确下界」）：`[1.0.0-SNAPSHOT.4,2.0)` 拒绝 `.1` / `.2` / `.3`，接受 `.4` / `.10` / `1.0.0` / `1.1.0`，仍拒绝 `2.0.0`。

## 1.0.0-SNAPSHOT.3

> 约定：对当前版本已记录条目的后续改动，直接合并进原条目，仅保留改动后的最终版本，不追加“再次修改”条目。
> 版本号说明：本库此前以 `1.1.0` 在本地构建（除 `mavenLocal` 外未发布到任何地方），为首次对外发布改号为 `1.0.0-SNAPSHOT.1`；随后改为 **`1.0.0-SNAPSHOT.2`** —— Java 包名发生重命名，而 `mod_version` 仍停在 `1.0.0`，只能靠 patch 段承载这次改名，否则依赖区间无从区分新旧 jar；现又改为 **`1.0.0-SNAPSHOT.3`** —— 完全移除 Cloth Config 前置与库内公共配置模块、把 `GameplayConfigValues` 由 13 字段收敛为 6 字段并删除事件框架死代码，同样是二进制不兼容变更，必须再占一个 patch 段才能让消费方摆脱 `mavenLocal` 里的 `.2` 旧 jar。又因 `1.0.0-SNAPSHOT.1` 与 `.2` 除 `mavenLocal` 外未进入任何仓库，三个版本号的条目在此合并为一份 `1.0.0-SNAPSHOT.3` 更新日志。

### 破坏性变更

- **Java 包名 `com.merlinkitsune.starengine` → `com.merlinkitsune.starenginelib`，Maven `groupId` 随之迁移。** 只在「这是包名」的位置把 `starengine` 段替换为 `starenginelib`：`package` / `import` 语句、javadoc `{@link}` 目标、三个源码目录（`common`、`neoforge-1.21.1`、`forge-1.20.1`）与两侧 `gradle.properties` 的 `mod_group_id` —— 共 50 个文件、69 处，另加发布用 `groupId`（`com.merlinkitsune.starengine` → `com.merlinkitsune.starenginelib`）。**有意保持不变**的：mod id `starengine_lib`、显示名 `StarEngine Lib`、全部 `StarEngine*` 类名，以及所有历史记录（旧 CHANGELOG 条目、`docs/starengine-lib/`、一次性脚本 `tools/migrate_to_starengine_lib.py`）。本次改名**在同一版本号上二进制不兼容** —— 改名前的 `.1` jar 与本 `.2` jar 拥有完全相同的 `modId` 与相同的 MC 版本后缀，但类名不同 —— 因此消费方必须同时抬高区间下界（见下方「工程」）。替换为字面量、字节级（`starengine(?![A-Za-z0-9_])`，大小写敏感），故 `starengine_lib`（mod id）、`starenginelib`（新包名）、`StarEngine`（品牌/类名前缀）均不可能被命中；且保留原行尾，diff 严格「一处一行」，无 CRLF/LF 噪音。改名后以开包方式核对：两个平台 jar 分别 45（neoforge）/ 47（forge）个 class，类路径与常量池中**旧包名 0 处**；对仓库（含二进制）的全字节扫描同样 0 处真引用。若某消费方产物把库的全限定类名以**字符串**形式配对（Mixin 配置指向 `com.merlinkitsune.starengine.*`、混淆映射、反射或代码生成），也必须同步改写；本仓库已无此类引用。客户端可见影响：无。

### 新内容

- 首次从 Astral Dice 1.20.1 / 1.21.1 多加载器单仓中提取出 **StarEngine Lib**（`starengine_lib`）：将两个 MC 版本可共同引用的 34 个共享单元（18 个 `MobEffect` 实现、`AstralEventType`/`EventContext`/`EventEffect`/`EventTargetCollector` 事件框架、目标选择框架、`BossEntityUtil`、`ClientDamageNumbers`、`GameplayConstants`、`SignActiveTriggeredEvent`、`AmethystDiceHandler`）下沉至本库，供后续 **Astral Dice Extra（星之骰戏：扩展）** 及同系列模组复用。
- 本库**不注册任何**注册表条目（物品/效果/附件/数据组件/能力全部留在消费方 mod）。因此引入或升级本库都不会改变任何 `ResourceLocation` 归属，既不损坏存档，也不破坏数据包。
- 提供**平台无关的配置值 seam**：`GameplayConfigValues`（纯值快照 record）与 `GameplayConstants.applyConfig(...)`。配置文件与配置读写**不进库** —— `ModConfigSpec`（NeoForge）与 `ForgeConfigSpec`（Forge）不通用，故配置项定义、TOML 读写与配置屏幕全部留在消费方 mod，库只声明「哪些玩法数值可配置」以及如何应用。消费方在自己的配置加载/重载回调里构造快照并推送，**改完配置不需重启**（`GameplayConstants` 字段是非 final 的运行时读取）。

- 新增 **`neoforge-26.1.2` 平台**：本库由「两版同源」变为「**三版同源**」—— `common` 同一份共享源码现由 Java 17（forge-1.20.1）、Java 21（neoforge-1.21.1）与 Java 25（neoforge-26.1.2；MC 26.1.2 / NeoForge 26.1.2.109 / ModDevGradle 2.0.147 / **无 Parchment**，Parchment 官方 maven 对 26.1.x / 26.2 全部 404）各编译一次，三个平台各产出一个 jar 并共用同一版本号。
### 工程

- **多加载器单仓结构**：`common/src/main/java` 是共享**源码目录**（不是 Gradle 子项目）；`neoforge-1.21.1`（Java 21 · Mojmap + Parchment · ModDevGradle 2.0.141）与 `forge-1.20.1`（Java 17 · reobf SRG · ModDevGradle Legacy 2.0.144）两个子项目各自把它加入 `sourceSet` 并编译同一份源码一次，每个 MC 版本各产出一个 jar。
- **共享源码的两条硬约束**，均由编译期而非约定强制：`common` 不得使用 Java 21 独有语法（record pattern / switch pattern / `SequencedCollection` 等，由 Java 17 工具链编译同一份源码强制），且只能使用两版 MC 中都存在且**签名一致**的 API。
- **加载器差异留在平台子项目中**，每个差异各写一份：`ActionBarManager`（`DeltaTracker` / `float partialTick`）、`ModEffectRemoval`（`Holder<MobEffect>` / `MobEffect`）、自定义数据键（原版 `DataComponent` / Forge 的 `ItemDataKey`）、饰品集成（`CuriosCompat`，仅 Forge）。
- **加载器差异以 shim 收敛**：`platform/LoaderEvent` 是抽象基类，两侧分别继承各自的 Event Bus 事件类型（NeoForge / Forge）；`platform/LoaderTags` 暴露 `c:bosses` 通用 tag，取自平台自身的 `Tags` 类。共享文件（如 `SignActiveTriggeredEvent`、`BossEntityUtil`）只引用 shim，因此共享代码中不再残留任何 `net.neoforged.*` / `net.minecraftforge.*` 字面量。
- **配置不进库（值快照 seam）**：`ModConfigSpec`（NeoForge）与 `ForgeConfigSpec`（Forge）不通用，配置文件的读写与配置 GUI 只能各平台自己做，因此库内**不持有配置 schema、不读配置文件、不提供配置屏幕**，只保留平台无关的 `GameplayConfigValues`（纯 record 值快照）与 `GameplayConstants.applyConfig(...)`；消费方注册自己的配置后，把当前值装进快照推送即可热生效。record 字段即契约 —— 消费方按**位置**构造该 record，增删/改序字段会让消费方在**编译期**失败（这正是刻意选择 record 而非松散 getter 集合的原因）。由此 `common` 中既无加载器字面量、也无任何第三方配置库依赖，库的必需前置只剩加载器本身（Forge 侧另有 Curios）。
- **版本号 bump 到 `1.0.0-SNAPSHOT.3`（必须）**：本次删除库内公共配置模块与事件框架类、并收敛 `GameplayConfigValues` 字段，属二进制不兼容变更；而该版本号不以 `-SNAPSHOT` 结尾（按 Maven 语义属普通版本），Gradle 不会把它当作 changing module —— 不 bump 则消费方会继续解析 `mavenLocal` 里的 `.2` 旧 jar，随后在编译期报「找不到符号」。
- **Maven 发布**：NeoForge 侧 `from components.java` 发布普通 Mojmap jar（NeoForge 自 1.20.5 起编译与生产同为 Mojmap，无需重映射）；Forge 1.20.1 侧发布 `reobfJar`，即**生产 SRG jar** —— 消费方 MDG LegacyForge 会在解析期把 SRG jar 重映射为 dev Mojmap 命名，若发布未重混淆的 dev jar，生产环境会因成员名为 Mojmap 而 `NoSuchFieldError`。两侧均显式指定 `artifactId`，使坐标与 `archivesName` 一致。
- **build 自动部署**：`build` 结束后 `pushToPack` 会把库 jar 复制进两个整合包的 `mods` 目录（Forge 测试包推 SRG jar、NeoForge 包推普通 jar），与消费方既有的「build 即部署」约定一致。此步不可省略：消费方把 `starengine_lib` 声明为必需前置，库 jar 不在包内会导致整合包直接拒绝启动。⚠️ **自 2026-09-12 起，开发分支上不再推送 —— `main` 已在豁免名单中。** 本库与消费方必须以**成对匹配**的版本一起进整合包（消费方既在编译期锁定库版本，又在 `mods.toml` 声明库版本区间），只要一侧单独前进，整合包就会带着不匹配的组合在启动期以 `NoClassDefFoundError` 或「缺失必需前置」崩溃。消费方已在其开发分支 `multi-dev-next` 上停推，故本库同步在 `main` 上停推，让整合包冻结在最后一个匹配的组合上，而不是被单侧覆盖。守卫在**执行期**读 `.git/HEAD` 判定当前分支（对 configuration-cache 友好、切分支立即生效），并兼容两种形态 —— 普通仓库的 `.git` 目录，以及 worktree 下作为**文件**、内容为 `gitdir: <path>` 的 `.git`；无法确定分支时（detached HEAD、无 `.git`，如 CI）同样跳过，取安全侧。`-PdeployToPack` / `-PpackPush` 仍可强制推送（手动出包用）—— 使用时请把消费方 jar 一并推送，保持成对。
- **托管到 GitHub，并配上独立 CI 与 Release**：仓库现位于 <https://github.com/merlin-kitsune/starengine_lib>，新增 `.github/workflows/build.yml`，沿用 Astral Dice 的发布规范：每次 push / PR / 手动触发均以 JDK 21 + JDK 17 构建双平台，并上传 `starengine_lib-jars` 构建产物；push 到 `main` 自动打 tag（tag = 基础版本号，无 `v` 前缀、无 `+加载器` 后缀）；push tag 则创建/更新 GitHub Release，附件为两个平台的 jar。相较消费方规则**多加一道守卫**：版本号含 `-`（即快照）时**不打 tag**，否则 `1.0.0-SNAPSHOT.1` 会被截成 `1.0.0` 并被当成正式发布自动打出，且 tag 难以回收。另修复了 `gradlew` 在 git 索引中丢失的可执行位（`100644` → `100755`）：仓库自 Windows 提交，wrapper 不带执行权限，Runner 上 `./gradlew` 在 Gradle 启动前就因 `Permission denied`（exit 126）退出；又因消费方 CI 需先 checkout 本库，这一个缺陷会同时打红**两个**仓库的 Action。
- **前置版本区间下界必须是 `1.0.0-SNAPSHOT` 而非 `1.0`**：按 Maven `ComparableVersion` 语义 `1.0.0-SNAPSHOT.1 < 1.0`，消费方若写 `versionRange="[1.0,2.0)"` 会拒绝所有 `1.0.0-SNAPSHOT.x`，游戏直接以「不满足必需前置」拒绝加载。已用 `maven-artifact` 3.8.5 实测（两侧加载器均经 `MavenVersionAdapter.createFromVersionSpec` → `VersionRange.createFromVersionSpec`）：`[1.0,2.0)` 不含 `1.0.0-SNAPSHOT.1`，而 `[1.0.0-SNAPSHOT,2.0)` 连同 `1.0.0` / `1.1.0` 一并包含。下界还须精确到快照序号（当前为 `.2`，见下一条），否则区间过宽、挡不住同坐标的旧 jar。
- **消费方必须把区间下界抬到 `1.0.0-SNAPSHOT.3`**：相邻快照之间是**二进制不兼容**的（`.1` → `.2` 改包名；`.2` → `.3` 删除库内公共配置模块、收敛 `GameplayConfigValues` 字段并删除事件框架类），而旧 jar 的 `modId` 与版本号形态完全相同、只差 patch 段，故宽松区间（如 `[1.0.0-SNAPSHOT,2.0)`）会把残留的旧 jar 一并放行 —— `.1` 会在运行期以 `NoClassDefFoundError: com/merlinkitsune/starengine/...` 崩溃，`.2` 会在编译期因找不到 `StarEngineConfigs` 等已删除的类而失败。`[1.0.0-SNAPSHOT.3,2.0)` 拒绝 `.1` / `.2`，同时接受 `.3` / `.10` / `1.0.0` / `1.1.0`，并照旧拒绝 `2.0.0`。已用 `maven-artifact` 3.8.5 按三区间 × 七版本实测（`[1.0.0-SNAPSHOT.2,2.0)` 拒绝 `.1`、接受 `.2`/`.3`/`.10`/`1.0.0`/`1.1.0`、拒绝 `2.0.0`；三者同为「精确下界」语义，故把下界换成 `.3` 的行为与之完全同构）；注意 `.10` 是按**数值**而非字典序比较，故两位数的 patch 段不会掉出区间。
- **消费方 CI 改为先构建本库**：本库仅发布到 `mavenLocal`，故 Astral Dice 的 `build.yml` 会先 checkout 本仓并执行 `./gradlew publishToMavenLocal`，再构建自身。因此两个仓库的版本号必须严格对齐（消费方的 `starengine_lib_version` ↔ 本库的 `lib_version`），否则消费方 CI 会断在依赖解析。
- **26.1 的两处平台差异下沉进 shim**：① 标识符类由 `net.minecraft.resources.ResourceLocation` 改名为 `Identifier`（1.20.1 / 1.21.1 仍用旧名）；② `EntityType#is(TagKey)` 被移除，须经 `builtInRegistryHolder()`。② 由 `platform/LoaderTags#isBoss(Entity)` 吸收（共享源码 `BossEntityUtil` 只调用该 shim，不感知版本差异），26.1.2 侧的 `LoaderTags` 另改用 `TagKey.create` 直接构造 `c:bosses`，不依赖随版本漂移的平台 Tags 常量；① 因 Java 无类型别名，唯一直接用旧名的 `common/event/AstralEventType` 被 `sourceSets.main.java.exclude` 排除在 26.1.2 编译之外（它与其同族在消费方主线已删除，本库也只是暂留）。
