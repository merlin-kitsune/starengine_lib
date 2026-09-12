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

## 3. 配置解耦

`ModConfigSpec`（NeoForge）与 `ForgeConfigSpec`（Forge）不通用，因此配置**不进库**：

```
库（共享）                          消费方（各平台）
GameplayConfigValues  (record)  ←──  GameplayConfigBinder.refresh()
GameplayConstants.applyConfig()      读取自身 ModCommonConfig 后回填
```

- `GameplayConfigValues` 是平台无关的**值快照**（纯 record，13 个字段）。
- `GameplayConstants.applyConfig(...)` 把快照写入静态常量，并派生 `×20` 的 tick 量。
- 消费方在两个平台各有一个 `GameplayConfigBinder`，负责「读自己的配置 → 构造快照 → 调用 `applyConfig`」。

**配置文件名与路径不变**（`config/astral_dice-common.toml`），存量配置不会重置。

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
| NeoForge 1.21.1 | `com.merlinkitsune.starengine:starengine_lib-neoforge-1.21.1:1.0.0` | `jar`（NeoForge 编译与生产同为 Mojmap，无需重映射） |
| Forge 1.20.1 | `com.merlinkitsune.starengine:starengine_lib-forge-1.20.1:1.0.0` | `reobfJar`（**生产 SRG jar**） |

**Forge 侧必须发布 `reobfJar` 而非 `jar`**：消费方 MDG LegacyForge 在解析期会把生产 SRG jar
重映射为 dev Mojmap 命名；若发布未重混淆的 dev jar，生产环境会因成员名为 Mojmap 而 `NoSuchFieldError`。

### 4.2 消费方接入方式

```groovy
// NeoForge 1.21.1
repositories { mavenLocal() }
dependencies {
    implementation "com.merlinkitsune.starengine:starengine_lib-neoforge-1.21.1:1.0.0"
}

// Forge 1.20.1
repositories { mavenLocal() }
dependencies {
    modImplementation "com.merlinkitsune.starengine:starengine_lib-forge-1.20.1:1.0.0"
}
```

两侧 `mods.toml` 需声明为必需前置：

```toml
[[dependencies.<mod_id>]]
    modId="starengine_lib"
    type="required"        # 1.20.1 Forge 用 mandatory=true
    versionRange="[1.0,2.0)"
    ordering="AFTER"
    side="BOTH"
```

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

---

## 5. 目录内容

```
common/src/main/java/com/merlinkitsune/starengine/     # 共享源码（31 个文件）
├── client/       ClientDamageNumbers                  # 伤害数字浮字
├── component/    GameplayConfigValues, GameplayConstants
├── effect/       18 个 MobEffect 实现
├── event/        AstralEventType / EventContext / EventEffect / EventTargetCollector
│                 AmethystDiceHandler / SignActiveTriggeredEvent
├── item/         BossEntityUtil
└── target/       TargetSelectionAction / TargetSelectionRegistry / TargetType

neoforge-1.21.1/src/main/java/.../starengine/          # 平台专有（5 个文件）
├── StarEngineLib          @Mod 入口
├── client/ActionBarManager        (DeltaTracker)
├── event/ModEffectRemoval         (Holder<MobEffect>)
└── platform/LoaderEvent, LoaderTags

forge-1.20.1/src/main/java/.../starengine/             # 平台专有（7 个文件）
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

- 库版本遵循 semver，`1.x` 内保持 API 兼容；消费方 `mods.toml` 声明 `versionRange="[1.0,2.0)"`。
- 共享源码中对 MC API 的使用受两侧编译期约束，破坏性变更会在**编译期**而非运行期暴露。

---

## 7. 许可

MIT License，见 [LICENSE](LICENSE)。
