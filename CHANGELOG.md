# Changelog (English)

> This file contains the English changelog only. Chinese version: [`CHANGELOG_ZH.md`](CHANGELOG_ZH.md).
> The two files correspond one-to-one by version number: each version appears once in both files, and every change must update both together — never only one side.

## Unreleased (1.0.0)

> Convention: later edits to an entry already recorded for this version are merged into that entry — only the final version is kept, no "updated again" follow-ups.

### New Content

- First extraction of **StarEngine Lib** (`starengine_lib`) from the Astral Dice 1.20.1 / 1.21.1 multiloader monorepo: 34 shared units that both MC versions can reference (18 `MobEffect` implementations, the `AstralEventType`/`EventContext`/`EventEffect`/`EventTargetCollector` event framework, the target-selection framework, `BossEntityUtil`, `ClientDamageNumbers`, `GameplayConstants`, `SignActiveTriggeredEvent`, `AmethystDiceHandler`) were moved down into this library to be reused by the upcoming **Astral Dice Extra** mod and its siblings.
- The library registers **no** registry entries (items / effects / attachments / data components / capabilities all remain in the consuming mod). Introducing or upgrading the library therefore changes no `ResourceLocation` ownership, so it can neither corrupt saves nor break datapacks.

### Project

- **Multiloader single-repo shape.** `common/src/main/java` is a shared *source directory* (not a Gradle subproject); the `neoforge-1.21.1` (Java 21, Mojmap + Parchment, ModDevGradle 2.0.141) and `forge-1.20.1` (Java 17, reobf SRG, ModDevGradle Legacy 2.0.144) subprojects each add it as an extra `sourceSet` and compile the same sources once, producing one jar per MC version.
- **Two hard constraints on shared sources**, both enforced at compile time rather than by convention: `common` may not use Java 21-only syntax (record patterns, switch patterns, `SequencedCollection`, …) — enforced by the Java 17 toolchain compiling the same sources — and may only use MC APIs that exist with **identical signatures** in both versions.
- **Loader divergence lives in the platform subprojects**, one file per side: `ActionBarManager` (`DeltaTracker` vs `float partialTick`), `ModEffectRemoval` (`Holder<MobEffect>` vs `MobEffect`), the custom data key (`DataComponent` vs Forge's `ItemDataKey`), and the Curios integration (`CuriosCompat`, Forge only).
- **Loader divergence folded into shims.** `platform/LoaderEvent` is an abstract base that each side extends from its own event bus type (NeoForge / Forge), and `platform/LoaderTags` exposes the `c:bosses` common tag from the platform's own `Tags` class. Shared files such as `SignActiveTriggeredEvent` and `BossEntityUtil` reference only the shims, so no `net.neoforged.*` / `net.minecraftforge.*` literal remains in shared code.
- **Configuration decoupled.** `ModConfigSpec` and `ForgeConfigSpec` are not interchangeable, so the config class stays in the consumer: the library provides platform-neutral `GameplayConfigValues` (a plain record value snapshot) plus `GameplayConstants.applyConfig(...)`, and each platform's `GameplayConfigBinder.refresh()` reads its own config and feeds the snapshot back. The config file name and path are unchanged (`config/astral_dice-common.toml`), so existing configs are not reset.
- **Maven publication.** NeoForge publishes `from components.java` (a plain Mojmap jar — NeoForge compiles and ships Mojmap from 1.20.5 on, so no remapping is needed); Forge 1.20.1 publishes `reobfJar`, i.e. the **production SRG jar**, because the consuming MDG LegacyForge remaps SRG jars to dev Mojmap names at resolution time. Publishing the non-reobfuscated dev jar instead would cause `NoSuchFieldError` in production. Both publications set an explicit `artifactId` so the coordinates match `archivesName`.
- **Auto-deploy on build.** `pushToPack` copies the library jar into both modpack `mods` directories after `build` (the SRG jar into the Forge test pack, the plain jar into the NeoForge pack), matching the consumer's existing "build means deploy" convention. This step is not optional: the consumer declares `starengine_lib` as a required dependency, so a missing library jar makes the pack refuse to start.
