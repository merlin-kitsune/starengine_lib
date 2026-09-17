# StarEngine Lib

`starengine_lib` — **Astral Dice（星之骰戏）** 系列模组共用的引擎库。

由 [Astral Dice](https://github.com/) 的 1.20.1 / 1.21.1 双版本单仓中提取，目标是为后续
**Astral Dice Extra（星之骰戏：扩展）** 及同系列模组提供可直接复用的共享实现。

---

## 1. 核心设计约束

理解这三条即可理解本仓库全部结构决策：

### 1.1 库不注册任何注册表条目

物品、效果、附件、数据组件（DataComponent）、能力（Capability）**全部留在消费方 mod**。

这条约束的含义是其价值所在：库的引入与升级**不会改变任何 `ResourceLocation` 的归属**，
因此既不会损坏存档（物品/方块状态丢失），也不会破坏数据包（tag / 配方 / 战利品表引用）。

### 1.2 跨 MC 版本，共享源码而非共享产物

1.20.1（SRG 成员名 / Java 17）与 1.21.1（Mojmap / Java 21）**不能共用同一份编译产物**。
因此本项目不做单一 `common` Gradle 子项目，而是：

```
common/src/main/java        ← 共享源码目录（不是 Gradle 子项目）
neoforge-1.21.1/            ← 平台子项目：Java 21 · Mojmap + Parchment · ModDevGradle 2.0.141
forge-1.20.1/               ← 平台子项目：Java 17 · reobf(SRG) · ModDevGradle Legacy 2.0.144
```

两个平台子项目各自 `sourceSets.main.java.srcDir('../../common/src/main/java')`，
**同一份共享源码被编译两次**，各产出一个 jar。

### 1.3 共享源码的两条硬约束

`common` 下的源码必须同时通过两侧编译，因此：

| 约束 | 原因 | 由谁强制 |
|---|---|---|
| 不得使用 Java 21 独有语法（record pattern / switch pattern / `SequencedCollection` 等） | 同一份源码要由 Java 17 工具链编译 | `forge-1.20.1` 侧编译失败 |
| 只能使用两版 MC 都存在**且签名一致**的 API | 两侧 API 差异面很大 | 两侧编译任一失败 |

**版本分歧必须留在平台子项目中**，同名类各写一份。已落地的分歧示例：

| 分歧点 | neoforge-1.21.1 | forge-1.20.1 |
|---|---|---|
| 客户端帧差参数 | `DeltaTracker` | `float partialTick` |
| 效果实例类型 | `Holder<MobEffect>` | `MobEffect` |
| 自定义数据键 | `DataComponent`（原版组件体系） | `ItemDataKey`（Forge 扩展点） |
| 饰品集成 | 直接使用原版/NeoForge 能力 | `CuriosCompat` 适配器 |

---

## 2. 加载器差异的收敛方式（shim）

共享源码中不允许出现加载器专有命名空间，差异统一由 `platform/` 下的 shim 吸收：

| shim | 作用 |
|---|---|
| `platform/LoaderEvent` | 抽象基类，两侧分别 `extends net.neoforged.bus.api.Event` / `net.minecraftforge.eventbus.api.Event`。共享的 `SignActiveTriggeredEvent` 只引用本类。 |
| `platform/LoaderTags` | `c:bosses` 通用 tag。NeoForge 取自 `net.neoforged.neoforge.common.Tags`，Forge 取自 `net.minecraftforge.common.Tags`。共享的 `BossEntityUtil` 只引用本类。 |

这样共享代码里**没有任何 `net.neoforged.*` / `net.minecraftforge.*` 字面量**，
新增平台时只需补一份 shim。

---

## 3. 配置：配置文件留在消费方，库只提供值快照 seam

`ModConfigSpec`（NeoForge）与 `ForgeConfigSpec`（Forge）不通用，而配置文件的读写与配置 GUI
**只能由各平台自己做**。因此配置**不**进库：配置文件（消费方侧的 `config/astral_dice-common.toml`）、
配置屏幕与配置项定义全部留在消费方 mod，库内只保留一层平台无关的 seam：

```
库（共享，common/）                                    消费方（各平台）
GameplayConfigValues（纯值快照 record）                config/ModCommonConfig（ModConfigSpec / ForgeConfigSpec）
GameplayConstants.applyConfig(GameplayConfigValues)   → @Mod 构造期注册配置，并在加载/重载时构造快照推送
```

- 库**不读配置文件、不持有配置 schema、不提供配置 GUI**：它只声明「哪些玩法数值可配置」
  （record 字段）以及如何应用（`applyConfig`）。
- 消费方注册自己的配置后，把当前值装进 `GameplayConfigValues` 推给 `GameplayConstants`；
  **改完配置不需重启**——`GameplayConstants` 的字段是非 final 的运行时读取，重新推送一次即生效。
- 由此 `common` 中既没有 `net.neoforged.*` / `net.minecraftforge.*` 字面量，也没有任何第三方
  配置库依赖；库的必需前置只剩加载器本身（Forge 侧另有 Curios）。
- **record 字段即契约**：消费方按位置构造该 record，增删/改序字段会让消费方**编译期**失败
  （这正是刻意用 record 而非松散 getter 集合的原因）。调整可配置项时必须两侧同步。

> 本库曾经把配置 schema 下沉进 `common`（Cloth Config AutoConfig，配置文件被改写为
> `config/astral_dice.json`）。该方案已整体撤回：Cloth Config 前置与 AutoConfig 相关类
> 全部移除，配置回归消费方自己的 **TOML**（`ModConfigSpec` / `ForgeConfigSpec`），
> 库只留下上述 seam。

---

## 4. 构建与发布

```bash
# 构建两个平台（并自动推送到两个整合包 mods 目录，见下）
./gradlew build

# 仅构建单平台
./gradlew :neoforge-1.21.1:build
./gradlew :forge-1.20.1:build

# 发布到本地 Maven，供消费方 modImplementation 解析
./gradlew publishToMavenLocal
```

### 4.1 发布坐标

| 平台 | 坐标 | 发布产物 |
|---|---|---|
| NeoForge 1.21.1 | `com.merlinkitsune.starenginelib:starengine_lib-neoforge-1.21.1:1.0.0-SNAPSHOT.3` | `jar`（NeoForge 编译与生产同为 Mojmap，无需重映射） |
| Forge 1.20.1 | `com.merlinkitsune.starenginelib:starengine_lib-forge-1.20.1:1.0.0-SNAPSHOT.3` | `reobfJar`（**生产 SRG jar**） |

> 两侧版本号**同号**，升级时两个 `gradle.properties` 必须一起改。

**Forge 侧必须发布 `reobfJar` 而非 `jar`**：消费方 MDG LegacyForge 在解析期会把生产 SRG jar
重映射为 dev Mojmap 命名；若发布未重混淆的 dev jar，生产环境会因成员名为 Mojmap 而 `NoSuchFieldError`。

### 4.2 消费方接入方式

```groovy
// NeoForge 1.21.1
repositories { mavenLocal() }
dependencies {
    implementation "com.merlinkitsune.starenginelib:starengine_lib-neoforge-1.21.1:1.0.0-SNAPSHOT.3"
}

// Forge 1.20.1
repositories { mavenLocal() }
dependencies {
    modImplementation "com.merlinkitsune.starenginelib:starengine_lib-forge-1.20.1:1.0.0-SNAPSHOT.3"
}
```

本库目前只发布到 **mavenLocal**（无远程 maven），因此消费方要么本机 clone 本仓并
`./gradlew publishToMavenLocal`，要么从本仓 Release 直接取 jar 放进整合包。
CI 场景见 §4.4。

两侧 `mods.toml` 需声明为必需前置：

```toml
[[dependencies.<mod_id>]]
    modId="starengine_lib"
    type="required"        # 1.20.1 Forge 用 mandatory=true
    versionRange="[1.0.0-SNAPSHOT.3,2.0)"
    ordering="AFTER"
    side="BOTH"
```

> ⚠️ **区间下界不要写成 `[1.0,2.0)`**。按 Maven `ComparableVersion` 语义，
> `1.0.0-SNAPSHOT.x < 1.0`（预发布限定符排在正式版本之前），因此 `[1.0,2.0)` **不含**任何
> 快照版本——游戏会以「缺失/不满足必需前置」拒绝加载。
>
> ⚠️ **下界要精确到当前快照序号（如 `.2`）**，不要停在 `[1.0.0-SNAPSHOT,2.0)`。
> 宽松区间会把**改名前的旧库 jar** 一并接受——它 `modId` 相同、版本号也可能相同，
> 但带的是旧包名 `com.merlinkitsune.starengine`，加载后必然 `NoClassDefFoundError` 崩溃。
> 收紧区间可把这种错配变成加载器层面的「缺必需前置」明确报错。
>
> 实测（`maven-artifact` 3.8.5，两侧加载器均走 `MavenVersionAdapter.createFromVersionSpec`）：
>
> | 区间 | `.1`（改名前） | `.2` | `.3` | `.10` | `1.0.0` | `1.1.0` | `2.0.0` |
> |---|---|---|---|---|---|---|---|
> | `[1.0,2.0)` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
> | `[1.0.0-SNAPSHOT,2.0)` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
> | `[1.0.0-SNAPSHOT.2,2.0)` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
>
> （注：`.10` 一列验证序号按数值而非字典序比较。）

> 修改库代码后，消费方 Gradle 会缓存 mavenLocal 的解析结果。
> 若消费方未取到新版本，用 `--refresh-dependencies` 或 bump `lib_version`。

### 4.3 自动部署到整合包

`build` 结束后 `pushToPack` 会把本库 jar 推送到两个整合包 mods 目录，
与消费方 Astral Dice 既有的「build 即部署」约定一致：

| 平台 | 目标目录 | 推送产物 |
|---|---|---|
| NeoForge | `D:/.minecraft/versions/狐の航空学 Voxy Edition/mods` | `jar` |
| Forge | `D:/.minecraft/versions/1.20.1 模组测试/mods` | `reobfJar` |

旧版本清理按 `starengine_lib-` 前缀；整合包根目录不存在时（如 CI）自动跳过。

> **这一步不可省略**：消费方把 `starengine_lib` 声明为必需前置，
> 库 jar 不在整合包内会导致整合包直接拒绝启动。

### 4.4 CI / 自动 Release（GitHub Actions）

仓库托管于 <https://github.com/merlin-kitsune/starengine_lib>，工作流 `.github/workflows/build.yml`
沿用消费方 Astral Dice 的发布规范：

| 触发 | 行为 |
|---|---|
| push / PR / 手动 | 双 JDK（21 + 17）→ `./gradlew build` → 上传 `starengine_lib-jars` 构建产物 |
| push 到 `main` 且版本号为**正式版** | 自动打 tag（tag = 基础版本号，如 `1.0.0`，无 `v` 前缀、无 `+加载器` 后缀） |
| push tag `/^[0-9]/` | 创建/更新 GitHub Release，附件 = 两个平台的 jar |

> tag 规则比消费方多一道守卫：版本号含 `-`（即快照）时**不打 tag**。
> 否则 `1.0.0-SNAPSHOT.2` 会被 `%%-*` 截成 `1.0.0` 并自动打出正式 tag，把未定型的快照误标为发布。

**消费方 CI 依赖本库**：Astral Dice 的 `build.yml` 会先 checkout 本仓并
`./gradlew publishToMavenLocal`，再构建自身——因为本库只在 mavenLocal 发布，CI 上无法直接解析。
两者版本号必须对齐（消费方 `starengine_lib_version` ↔ 本库 `lib_version`），否则消费方 CI 断在依赖解析。

---

## 5. 目录内容

```
common/src/main/java/com/merlinkitsune/starenginelib/  # 共享源码（28 个文件）
├── client/       ClientDamageNumbers
├── component/    GameplayConfigValues（配置值快照）, GameplayConstants
├── effect/       18 个 MobEffect 实现
├── event/        EventTargetCollector
│                 AmethystDiceHandler / SignActiveTriggeredEvent
├── item/         BossEntityUtil
└── target/       TargetSelectionAction / TargetSelectionRegistry / TargetType

neoforge-1.21.1/src/main/java/.../starenginelib/       # 平台专有（5 个文件）
├── StarEngineLib          @Mod 入口
├── client/ActionBarManager        (DeltaTracker)
├── event/ModEffectRemoval         (Holder<MobEffect>)
└── platform/LoaderEvent, LoaderTags

forge-1.20.1/src/main/java/.../starenginelib/          # 平台专有（7 个文件）
├── StarEngineLib          @Mod 入口
├── client/ActionBarManager        (float partialTick)
├── component/ItemDataKey
├── event/ModEffectRemoval         (MobEffect)
├── item/CuriosCompat
└── platform/LoaderEvent, LoaderTags
```

**关于 `StarEngineLib`**：入口类的唯一作用是让本库成为可被加载的 mod（`@Mod`），
它**不注册任何东西**。这正是「不改变 ResourceLocation 归属」的技术前提。

---

## 6. 版本与兼容

- 库版本遵循 semver，`1.x` 内保持 API 兼容；消费方 `mods.toml` 声明 `versionRange="[1.0.0-SNAPSHOT.3,2.0)"`。
  > 下界必须写到 `1.0.0-SNAPSHOT`：`[1.0,2.0)` 不含任何快照版（见 §4.2）。
  > 下界还要精确到当前快照序号（`.3`），否则更早的旧库 jar（改名前的 `.1`、含已删除配置类的 `.2`）
  > 会被宽松区间接受，分别表现为 `NoClassDefFoundError` 与编译期 `找不到符号`（见 §4.2）。
- 当前为 `1.0.0-SNAPSHOT.3`，SNAPSHOT 系列**不作**语义化兼容承诺；转正式 `1.0.0` 后再适用上一条。
- **破坏性变更记录**：`1.0.0-SNAPSHOT.3` 完全移除 Cloth Config 前置与库内公共配置模块
  （`StarEngineCommonConfig` / `StarEngineConfigs` / `LegacyCommonTomlImporter` / `StarEngineConfigScreen`），
  并把 `GameplayConfigValues` 由 13 字段收敛为 6 字段、`GameplayConstants.refresh()` 改为
  `applyConfig(GameplayConfigValues)`，同时删除事件框架死代码
  （`AstralEventType` / `EventContext` / `EventEffect`）。消费方需回归自己的 TOML 配置
  （`ModConfigSpec` / `ForgeConfigSpec`）并在配置加载后推送值快照（见 §3）。
  注意 `modId`（`starengine_lib`）**未**变更，故整合包文件名不受影响。
- **破坏性变更记录**：`1.0.0-SNAPSHOT.2` 将 Java 包名由 `com.merlinkitsune.starengine`
  改为 `com.merlinkitsune.starenginelib`（maven group 同步改为 `com.merlinkitsune.starenginelib`）。
  消费方所有 `import` 必须同步改写，且必须把区间下界提到 `[1.0.0-SNAPSHOT.2,...)`。
  注意 `modId`（`starengine_lib`）**未**变更，故 `mods.toml` 的依赖声明与整合包文件名不受影响。
- 共享源码中对 MC API 的使用受两侧编译期约束，破坏性变更会在**编译期**而非运行期暴露。

---

## 7. 许可

MIT License，见 [LICENSE](LICENSE)。
