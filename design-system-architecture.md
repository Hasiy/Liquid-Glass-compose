# Hasiy Design System Compose 架构说明

## 1. 文档目的

本文档定义 Hasiy Design System Compose 的模块边界、主题模型、依赖方向和扩展约定。

SDK 当前以 Liquid Glass 控件为第一套视觉风格，但 SDK 名称、主题 Token 和组件接入方式不绑定某一种视觉风格，后续可以继续增加其他主题与渲染风格。

## 2. 当前模块结构

```text
HasiyDesignSystemDemo
├── designsystem/                 Android Compose UI SDK
├── designsystem-tokens/          KMP 主题规格与语义颜色
└── app/                          Demo 应用与组件目录
```

### `:designsystem`

- Android Library，提供 Compose / Material3 控件和视觉渲染实现。
- 当前包含 Liquid Glass 风格的基础控件、导航、弹层、输入、反馈和布局组件。
- 对外包名：`top.hasiy.designsystem`。
- 对外主题入口：`DesignSystemTheme`。
- 当前 Android 模糊、阴影和资源实现留在本模块，不依赖 Demo 的资源和业务代码。

### `:designsystem-tokens`

- 纯 Kotlin Multiplatform Token 模块，不依赖 Android、Compose 或 Android 资源。
- 对外包名：`top.hasiy.designsystem.tokens`。
- 提供 `GlassThemeSpec` 和 `GlassVisualStyle`，承载跨平台主题身份、语义颜色和视觉风格选择。
- 当前启用 JVM、`iosArm64`、`iosSimulatorArm64` 与 `iosX64` targets。
- Windows 开发机只能验证 JVM 与部分 metadata 任务；iOS 编译、测试和实际消费必须在 macOS/Xcode 环境完成。

### `:app`

- 仅作为示例和验收应用。
- 展示所有 SDK 控件、主题切换和交互效果。
- 可以依赖 SDK，但 SDK 不得反向依赖 Demo 的资源、状态或页面代码。

## 3. 依赖方向

```text
app ───────────────▶ designsystem ───────────────▶ designsystem-tokens
 │                         │
 └── Demo 页面与验收         └── Android Compose 渲染
```

约束如下：

1. `designsystem-tokens` 只保存跨平台主题契约和纯值对象。
2. `designsystem` 负责把 Token 转换为 Android `GlassConfig` 和 Material3 `ColorScheme`。
3. `app` 不直接修改 SDK 内部实现，也不在页面中复制主题映射逻辑。
4. 未来 CMP 共享 UI 时，`commonMain` 只能依赖 tokens 和框架无关的状态；Android、iOS 各自提供渲染器。

## 4. 主题模型

主题由跨平台规格、Android 映射和组件消费三层组成：

```text
GlassThemeSpec / ThemePalette    跨平台主题规格：颜色、明暗、风格身份
                 │
                 ├── toGlassConfig()          Android 玻璃控件配置
                 └── toMaterialColorScheme()  Material3 颜色方案
                                      │
                                      └── DesignSystemTheme / CompositionLocal
```

`GlassThemeSpec` 是较精简的主题契约；`ThemePalette` 额外包含轨道、危险态、面板层级、
前景层级等完整语义 Token。公模或二次开发优先使用 `ThemePalette`，只有不需要完整语义色
时才直接构造 `GlassThemeSpec`。

### 主题规格

`GlassThemeSpec` 至少包含：

- `id`：主题稳定标识。
- `visualStyle`：`GlassVisualStyle`，当前包括 `DROP`、`NEUTRAL`、`DARK`、`NATIVE`。
- `isLight`：Material3 和系统栏明暗判断。
- `primary`、`secondary`、`surface` 等语义颜色。
- `glassBase`、`glassHighlight`、`glassBorder` 等玻璃视觉颜色。
- `accent`、`accentEnabled` 等强调色配置。

新增主题时优先新增 `GlassThemeSpec` 预设或业务侧规格，不要在每个控件中写主题分支。

### Compose 接入

页面根节点调用一次：

```kotlin
DesignSystemTheme(spec = GlassThemeSpec.default(GlassVisualStyle.NEUTRAL)) {
    GlassBackdropHost(Modifier.fillMaxSize()) {
        ScreenContent()
    }
}
```

SDK 通过 `LocalGlassConfig` 和 `LocalGlassThemeSpec` 向控件树提供当前配置。控件仍可通过显式 `GlassConfig` 覆盖局部参数，但不应绕过主题规格自行拼装颜色。

## 5. Liquid Glass 的定位

Liquid Glass 是当前 SDK 的视觉实现，不是 SDK 的架构名称：

- `Glass*` 控件是当前第一批控件 API。
- `GlassVisualStyle` 表达视觉风格选择。
- `Hasiy Design System` 是模块、包名和发布坐标的稳定名称。
- 后续可以在同一套 Token 和控件契约上增加其他视觉风格，而不重新命名 SDK 模块。

本次项目尚未对外发布，因此旧的 `liquidglass` 模块、包名和坐标直接移除，不提供兼容别名。

## 6. 发布约定

Android SDK 计划发布坐标：

```text
top.hasiy:hasiy-design-system-compose:1.0.0
```

该 Android artifact 的 POM 会依赖：

```text
top.hasiy:designsystem-tokens:1.0.0
```

本地模块依赖：

```kotlin
implementation(project(":designsystem"))
```

当前源码模块接入是依赖闭包完整的方式。Maven 发布尚未达到可交付状态：
`designsystem` 生成的 POM 会引用 `designsystem-tokens`，但 tokens 模块当前没有 Maven
publish task，因此不能只发布 Android artifact。

裸 AAR 也不是单文件 SDK。AAR 不打包 `designsystem-tokens`、Haze、Compose 或 Material3；
若必须离线交付，必须同时提供 tokens JVM JAR、第三方依赖清单和版本约束。公模默认不采用
裸 AAR 方式，优先使用 Maven 仓库或源码模块。

## 7. 后续扩展规则

### 新增主题

1. 在 `designsystem-tokens` 增加主题规格或预设。
2. 为语义颜色补充测试。
3. 在 Android 适配层确认 `GlassConfig` 和 Material3 映射。
4. 在 Demo 增加切换入口和可读性验收。
5. 不在单个控件内硬编码主题名称。

### 新增平台

1. 先复用 `designsystem-tokens` 的主题契约。
2. 在平台 source set 提供对应渲染实现。
3. 不把 Android 的 Haze、资源或 Canvas 实现泄漏到 `commonMain`。
4. 完成至少一套跨平台主题和组件验证后，再扩大共享 UI 范围。

### 新增控件

1. 优先复用已有 `GlassConfig`、主题 Local 和语义颜色。
2. 保持内容层清晰，背景模糊只发生在控件自己的圆角表面内。
3. 为 API 26–30 保留可读的降级路径。
4. 同步更新 SDK README、Demo 组件目录和单元测试。

## 8. 公开 API 分级与稳定性约束

内部 SDK 不追求二进制兼容门禁，但「哪些 API 能碰」必须有明确依据。公开面以
`designsystem/api/designsystem.api` 的快照为准——该文件由工具生成并提交进仓库，
不在本文手工维护，避免随代码漂移。

### 分级

| 级别 | 内容 | 约定 |
|:--|:--|:--|
| **稳定 API** | `DesignSystemTheme`、`GlassConfig`、`GlassPalette`、`GlassPresets`、`LocalGlassConfig`、`Modifier.glassSurface` / `glassBackdrop` / `blur`、`GlassBackdropHost`、全部 `Glass*` 控件 | 破坏性变更需同步下游 |
| **自製原料** | `GlassPalette` 的语义色扩展属性、`asFillSurface` / `asOverlaySurface` / `asTrackSurface` / `asSelectedSurface` / `asControlSurface`、`Modifier.tactileKeycap` / `tactilePanel` / `tactileScreenTexture`、`SevenSegmentText`、`TACTILE_*` 常量、`paletteNameRes` | 允许下游自建组件，语义可能调整 |
| **展示 / 调试** | `GlassThemeSelector`、`GlassColorPicker`、`GlassPaletteSelector` | 仅供组件目录与演示页使用 |
| **遗留** | `GlassDialogBlurHost`、`GlassPopupBlurBox`、`legacy/DynamicLightTabBar*` | 维持既有接入，不再扩展 |
| **内部** | 所有 `internal` 声明 | 不属于公开面，可随时改动 |

分级依据是「下游是否真的在用」——Demo 大量使用自製原料（Tactile 原语、语义色、
七段数字）自建画面，因此这些保持公开，而不是因为它们看起来像内部实现就收起来。

### 约束手段

- **`explicitApiWarning()`**：新增 public 声明必须明确写出可见性与返回类型，
  防止无意扩大公开面。现存告警清零后可升级为 `explicitApi()`。
- **Android API 快照**：`./gradlew :designsystem:apiDump` 导出
  `designsystem/api/designsystem.api`；`:designsystem:apiCheck` 校验该快照。
- **Token API 快照**：tokens 类型出现在 Android SDK 的公开签名中，必须另外执行
  `./gradlew :designsystem-tokens:apiDump` 并提交 JVM/KLib 快照。目前该快照尚未建立，
  `:designsystem-tokens:apiCheck` 会失败。
- **变更流程**：`apiDump` → review diff → 与代码一并提交。快照 diff 即 API 变更提案。

## 9. 架构审阅结论

审阅基于当前 Gradle 配置、source set、Android API 快照、生成的 Maven POM、单元测试目录
和 Demo 调用路径。整体依赖方向正确，主题契约也已从 Android 渲染层分离；目前主要缺口在
「公模发布闭包」和「tokens 公开 API 治理」，不是组件渲染层需要重构。

### Findings

| 严重度 | 问题 | 影响 | 建议 |
|:--|:--|:--|:--|
| P1 | Maven 发布缺少 tokens artifact | `hasiy-design-system-compose` 的 POM 引用 `top.hasiy:designsystem-tokens:1.0.0`，但 tokens 模块没有 publish task；独立消费者无法解析完整依赖 | 为 tokens 配置 `maven-publish`，统一发布两个 artifact，并增加空仓库 consumer fixture 验证 |
| P1 | 裸 AAR 不是单文件交付物 | 只复制 `designsystem-release.aar` 即使补 Haze，仍缺 tokens JVM 类型；Compose/Material3 版本也无法由 AAR 管理 | 公模默认只支持 Maven；若保留离线包，成套交付 AAR、tokens JAR、版本清单和校验值 |
| P2 | tokens API 门禁未闭合 | Android 公开 API 直接暴露 `GlassThemeSpec`、`ThemePalette` 等类型，但 tokens 变更不受现有快照保护 | 生成并提交 tokens JVM/KLib API 快照；将其 `apiCheck` 纳入 CI |
| P2 | `explicitApiWarning()` 只覆盖 Android SDK | tokens 公开声明仍可能被无意扩大；warning 也不是发布阻断 | 两个发布模块先清零告警，再升级为 `explicitApi()` |
| P2 | iOS 只有契约，没有渲染/消费验收 | `commonMain` Token 可复用，不等于 iOS SDK 或 CMP UI 已完成 | 在 macOS CI 增加 KLib/iOS test，并以一个 Swift/CMP consumer fixture 验证 |

## 10. 可追溯验收矩阵

状态定义：`通过` 表示当前证据足以验收；`部分` 表示静态结构成立但动态验证或平台覆盖不足；
`未通过` 表示已复现缺口；`未验证` 表示当前宿主不具备验证条件。

| 验收项 | 状态 | 当前证据 | 完成条件 / 命令 |
|:--|:--:|:--|:--|
| `designsystem-tokens` 不依赖 Android、Compose 或 Android 资源 | 通过 | `commonMain` 仅含纯 Kotlin Token；模块依赖只有 `kotlin("test")` | `./gradlew :designsystem-tokens:jvmJar` |
| 依赖方向保持 `app → designsystem → designsystem-tokens` | 通过 | 三个模块的 Gradle project dependency 与源码引用方向一致 | 在 CI 检查 Gradle 依赖图；禁止 SDK 引用 `top.hasiy.liquidglassdemo` |
| SDK 不依赖 Demo 资源或页面状态 | 通过 | `designsystem` 无 `:app` 依赖；Manifest 与资源位于自身模块 | `./gradlew :designsystem:assembleRelease` |
| 主题通过 Token 集中映射，不要求逐组件分支 | 通过 | `DesignSystemTheme`、`toGlassConfig()`、`toMaterialColorScheme()` 和 `LocalGlassConfig` 已形成单一入口 | 新主题测试覆盖 Token、Android 映射和 Demo 可读性 |
| Android API 快照与当前源码一致 | 通过 | 2026-09-11 实测 `:designsystem:apiCheck` 通过 | 每次公开 API 变更执行 `apiDump → review diff → apiCheck` |
| tokens API 快照存在并通过 | 未通过 | 2026-09-11 实测缺少 `designsystem-tokens/api/designsystem-tokens.api` | `./gradlew :designsystem-tokens:apiDump` 后 review JVM/KLib 快照，再执行 `:designsystem-tokens:apiCheck` |
| Maven 发布能在空缓存消费者中解析 | 未通过 | 生成的 Android POM 引用 tokens 1.0.0；tokens 模块无 publish task | 同仓发布两个 artifact，再用独立 consumer 工程只声明顶层坐标完成 compile/运行 |
| SDK Release AAR 可完整构建 | 部分 | `bundleReleaseAar` 已生成 `designsystem-release.aar`；聚合 `assembleRelease` 在本机因 AAPT2 daemon 启动失败中断 | 在正常 Windows/CI 环境执行 `./gradlew :designsystem:assembleRelease` 并确认退出代码 0 |
| JVM 单元测试通过 | 未验证 | 测试源码与 compilation 均可编译；本机 Gradle Test Executor 因 Windows 进程/管道异常退出，未产生可信测试结果 | `./gradlew :designsystem-tokens:jvmTest :designsystem:testDebugUnitTest :app:testDebugUnitTest` |
| Demo 可编译并消费 SDK | 通过 | 2026-09-11 实测 `:app:assembleDebug` 完成，APK 已生成 | CI 固定执行 `./gradlew :app:assembleDebug` |
| API 26–30 回退与 API 31+ 真模糊均可读 | 部分 | `shouldUseGlassBackdropBlur` 与 JVM 策略测试存在；缺少本轮设备级证据 | API 26/30/31/35 各跑关键页面截图与交互测试 |
| iOS targets 编译、测试并被真实消费者引用 | 未验证 | Windows 明确禁用 `iosArm64`、`iosSimulatorArm64`、`iosX64` | macOS 执行 KLib/API/test 任务，并由 Swift/CMP fixture 消费 |
| 发布坐标、SDK 包名和模块名使用 Hasiy Design System | 通过 | `top.hasiy:hasiy-design-system-compose:1.0.0`、`top.hasiy.designsystem`、`:designsystem` | 发布脚本与仓库说明保持同一命名 |

发布前必须清零所有 `未通过` 项；`部分` 项必须有明确的 CI 或设备验收记录，不能仅凭已有
构建产物或 metadata compilation 推断通过。
