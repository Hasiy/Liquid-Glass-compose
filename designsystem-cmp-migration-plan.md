# designsystem 迁移为 CMP（Compose Multiplatform）模块方案

日期：2026-09-22
状态：待评审，未实施

## 1. 目标与范围

把 `:designsystem` 从「`com.android.library` + androidx Compose（Android-only）」迁成
「KMP + Compose Multiplatform」模块，使 FTMS 的 `:WorkoutUi`（KMP）能在 `commonMain` /
`iosMain` 直接消费组件，而不只是 Android 侧。

范围约定：

- **坐标不变**：`top.hasiy:hasiy-design-system-compose`。FTMS `settings.gradle.kts` 里的
  `includeBuild` + `dependencySubstitution` 规则不用改（替换按 module 坐标匹配，不看 version）。
- **目标平台**：`androidTarget` + `iosX64` / `iosArm64` / `iosSimulatorArm64`。
  desktop(`jvm`) 本期不做，留扩展位。
- **`:designsystem-tokens` 一并改造**：现在是 `jvm()` + iOS，没有 android target，
  CMP 模块的 `commonMain` 无法依赖它。需加 `androidTarget()`（引入 AGP library 插件）。
- **不迁移的部分**：`legacy/` 包与 SearchBar 系组件保留为 Android-only（见 §4）。

工具链前提（已对齐，2026-09-22 完成）：两仓库统一 Kotlin 2.1.21 / AGP 8.13.2 / Gradle 9.5.0。
CMP 选 **1.8.2**（官方基于 Kotlin 2.1.21 构建，与现有工具链完全匹配，无需再动 Kotlin 版本）。

## 2. 现状盘点

22 个 main 源文件中，绝大多数只用 `androidx.compose.*` 的 common 子集，
可直接换 `org.jetbrains.compose.*` 坐标平移。真正的 Android 专属点只有 5 处：

| # | 位置 | 内容 | 迁移难度 |
| --- | --- | --- | --- |
| A | `GlassModifier.kt:233-251` | 软阴影：`BlurMaskFilter` + `asAndroidPath` + `nativeCanvas`（API 28+，低版本已有降级分支） | 中 |
| B | `GlassBlurComponents.kt:29,152` | `Build.VERSION.SDK_INT >= S` 的 backdrop blur 能力门控 | 低 |
| C | `legacy/DynamicLightTabBarLegacy*.kt` | AGSL `RuntimeShader` + `RenderEffect`（API 33+），整包深度 Android-only | 不迁 |
| D | `res/values/{colors,strings}.xml` + `GlassComponents.kt` / `GlassPresets.kt` / `GlassPaletteSelector.kt` | `R.color.*`（53 行色值）、`R.string.*`、公开 API 带 `@StringRes Int`（`paletteNameRes`、`PRESET_NAME_RES`） | 中（含破坏性 API 变更） |
| E | `GlassInputComponents.kt` | M3 `SearchBar` / `DockedSearchBar`：CMP material3 common **没有**这两个组件 | 不迁（或后续自绘） |

依赖情况：

- `haze 1.4.0` 本身是 multiplatform（含 iOS 原生 blur），坐标不变，直接进 `commonMain`。
- material3 / ui / foundation / icons 从 androidx BOM 换成 `org.jetbrains.compose.*`；
  用到的 `DatePicker`、`TimePicker`、`pulltorefresh`、`TooltipBox`、`ExposedDropdownMenuBox`、
  `ModalBottomSheet`、`BottomSheetScaffold` 在 CMP material3 common 里都有，无需处理。
- 测试：`test/` 5 个纯逻辑单测 → `commonTest`；`androidTest/GlassLensTabBarTest` → 留在 androidTarget。
- BCV：`api/designsystem.api` 440 行，迁移后需 `apiDump` 重生成并人工 diff。

## 3. 目标形态

```
designsystem/
  build.gradle.kts        # kotlin("multiplatform") + com.android.library + org.jetbrains.compose
  src/
    commonMain/kotlin/    # 全部 Glass* 组件（除 legacy、SearchBar 系）
    commonTest/kotlin/    # 现有 5 个纯逻辑单测
    androidMain/kotlin/   # legacy 包、GlassSearchBar/GlassDockedSearchBar、软阴影 actual、能力门控 actual
    androidInstrumentedTest/  # GlassLensTabBarTest
    iosMain/kotlin/       # 软阴影 actual（降级实现）、能力门控 actual
```

build 脚本要点：

- `androidTarget { publishLibraryVariants("release") }`，保留 `singleVariant("release")` 发布配置；
  namespace / compileSdk 35 / minSdk 26 不变。
- `explicitApiWarning()` 保留（迁移后警告面会变化，legacy 的警告随包进 androidMain 不扩大）。
- maven-publish 坐标不变；BCV 对 KMP 模块同样生效。
- `:designsystem-tokens` 加 `com.android.library` + `androidTarget()`，其余 target 不动。

## 4. 五处 Android 专属点的处理

**A. 软阴影（GlassModifier）** —— expect/actual 收口：

```kotlin
// commonMain
internal expect fun Paint.applySoftShadow(config: GlassConfig, ...)  // 或等价 DrawScope 扩展
```

- android actual：照搬现有 `BlurMaskFilter` 路径（含 API < 28 降级分支）。
- ios actual：第一版直接走普通 `drawShadow` 近似，视觉验收后如不达标再做多层渐变模拟。
  软阴影参数（`softShadowSpread` 比例）保持 token 化，降级实现只换绘制不换参数。

**B. 能力门控（GlassBlurComponents）** —— `internal expect val supportsBackdropBlur: Boolean`：
android = `SDK_INT >= 31`；ios = `true`（haze 在 iOS 用原生 UIBlur，无版本门槛）。

**C. legacy 包** —— 整体移入 `androidMain`，公开 API 原样保留（Android 消费者无感知），
不做 iOS 移植。BCV dump 中 legacy 条目应保持在 android 变体里，diff 时确认。

**D. 资源**：

- **色值（colors.xml 53 行）** → 迁入 `:designsystem-tokens` 的 Kotlin 常量
  （`PaletteTokens` 机制已有），删除 `colors.xml`。色值即数据，不该依赖平台资源系统。
- **字符串** → **公开 API 去资源化**：`@StringRes Int` 参数改为 `String` 参数 + 内置默认值。
  `paletteNameRes(id): Int` 删除或改为 `paletteName(id): String`；
  `PRESET_NAME_RES` 改为字符串常量列表。SDK 不做本地化，本地化交给消费方 App
  （FTMS 侧已有 strings.xml 体系）。
  ⚠️ 这是**破坏性 API 变更**，见 §5。
  （备选：`org.jetbrains.compose.resources` 生成资源类，改动小但把本地化责任留在 SDK 里，
  且 demo 的 preset/palette 名称本质是开发调试文案，不值得进资源系统。不推荐。）

**E. SearchBar 系** —— `GlassSearchBar` / `GlassDockedSearchBar` 移入 `androidMain`，
API 不变。iOS 如将来需要，再基于 `BasicTextField` 自绘一个 common 实现，本期不做。

## 5. API 与版本策略

- D 项的资源签名变更是 breaking change：版本 `1.0.0 → 2.0.0`。
  FTMS 当前经 dependencySubstitution 消费，替换只匹配 `group:name`，version 写什么都行，
  但两边脚本里的字面量应同步改，避免将来切真实发布时踩版本对不上。
- `apiDump` 重生成后逐条 diff 验收标准：
  1. android 变体除「资源签名变更 + version 变化」外无其他删减；
  2. common/iOS 变体不含 legacy 与 SearchBar 条目；
  3. tokens 的 API 只增不减（新增 androidTarget 变体）。

## 6. 对两个消费方的影响

- **demo `:app`**：依赖写法不变（`project(":designsystem")` 自动解析 android 变体）。
  它演示了 legacy tab bar 与 SearchBar——都还在 androidMain，无需改 demo 代码。
  demo 是本次迁移的**首要视觉验收面**（模糊、软阴影、tab bar 光效逐项过）。
- **FTMS**：`includeBuild` 与替换规则不变；`app/build.gradle.kts` 只改版本号字面量。
  `:WorkoutUi` 后续可在 commonMain 依赖 tokens、在 commonMain 直接用 CMP 组件子集
  （除 legacy / SearchBar），这正是迁移动机。
- Windows 开发机无法编译 Kotlin/Native（FTMS `gradle.properties` 已设
  `kotlin.native.ignoreDisabledTargets=true`），**iOS 链接验证必须放到 mac / CI**，
  本仓库构建验证只覆盖 android 变体 + common metadata。

## 7. 实施步骤（每步独立可验证、可回滚）

1. **tokens 加 androidTarget**：改 tokens build 脚本，demo `assembleDebug` + FTMS
   `assembleStandardDebug` 回归。（最小步，先打通依赖形状）
2. **designsystem 切 KMP+CMP 骨架**：改插件与依赖坐标，`src/main` → `commonMain`，
   先把 B（能力门控）与 A（软阴影）以「android actual 原样 + iOS 占位降级」立起来，
   C、E 移入 androidMain，D 的色值迁 tokens。目标：`compileDebugKotlinAndroid` 过。
3. **字符串 API 去资源化**（D）：改签名、删 strings.xml，同步 demo `:app` 调用点。
4. **测试迁移**：5 个单测进 commonTest，跑 `allTests`（本机只执行 android/jvm 侧）。
5. **BCV apiDump 重生成 + 按 §5 标准 diff**；版本号升 2.0.0，FTMS 侧字面量同步。
6. **demo app 真机视觉验收**：blur 门控、软阴影、legacy tab bar、SearchBar 各演示页过一遍。
7. **FTMS 全量回归**：`test` + `assembleStandardDebug` + `assembleMobiDebug` 分别报告。
8. **（另行安排）iOS 验证**：mac/CI 上 `iosSimulatorArm64` 链接 + 模拟器跑 WorkoutUi 试验页。

步骤 2 是最大的一步，如评审认为风险集中，可再拆成「骨架+纯平移文件」与
「expect/actual 收口」两个提交。

## 8. 风险与待决问题

| 风险/问题 | 说明 | 处置 |
| --- | --- | --- |
| iOS 软阴影视觉质量 | 无 AGSL 等价物，降级实现观感可能不足 | 第一版接受近似；验收不过再立专项 |
| CMP material3 与 androidx material3 行为差异 | Popup/IME/ripple 细节在个别版本有出入 | demo app 作为回归面，锁 CMP 1.8.2 不随手升级 |
| `@StringRes` API 变更影响未知外部消费者 | 目前消费方只有 demo app 与 FTMS，均可同步改 | 升 2.0.0 明示 breaking |
| 是否保留 desktop(jvm) target | 对 FTMS 无需求，但能加速预览 | 本期不加，接口上不封死 |
| legacy 包的去留 | 已有非 legacy 的 `GlassLensTabBar`；legacy 仅 demo 在用 | 迁 androidMain 保命，删除另议 |

## 9. 实施补记（2026-09-23，落地时与方案的偏差）

- **compose 版本被 CMP 强制升级到 androidx 1.8.2**（catalog 的 compose-bom 2024.10.01 不再钳制
  designsystem 的类路径）。连带影响：ui-test 1.8 删除了 `TouchInjectionScope.size`，
  `GlassLensTabBarTest` 改用 px 单位的 `width`；`Icon` 缺 import 属存量问题（androidTest 此前从未编译过），一并补上。
- **Kotlin 2.1 下三处构建脚本写法必须避开**（否则脚本编译直接报错，不是警告）：
  `compose.materialIconsCore` 访问器不存在（icons-core 由 material3 传递提供，删掉即可）；
  KMP `sourceSets` 块里的 `platform(...)` 命中已废弃重载（instrumented test 依赖移到顶层
  `androidTestImplementation`）；library 模块 `defaultConfig.targetSdk` 已废弃（删除）。
- **commonTest 断言消息位置**：JUnit 的「消息在前」在 kotlin.test 不存在且会被
  `assertEquals(Double, Double, Double)` 重载静默捕获，全部改写为「消息在后」。
