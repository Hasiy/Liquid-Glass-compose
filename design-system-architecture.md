# Hasiy Design System Compose 架构说明

## 1. 文档目的

本文档定义 Hasiy Design System Compose 的模块边界、主题模型、依赖方向和扩展约定。

SDK 当前以 Liquid Glass 控件为第一套视觉风格，但 SDK 名称、主题 Token 和组件接入方式不绑定某一种视觉风格，后续可以继续增加其他主题与渲染风格。

## 2. 当前模块结构

```text
HasiyDesignSystemDemo
├── designsystem/                 CMP UI SDK（Android / iOS）
├── designsystem-tokens/          KMP 主题规格与语义颜色
└── app/                          Demo 应用与组件目录
```

### `:designsystem`

- Kotlin Multiplatform + Compose Multiplatform，提供共享 Compose / Material3 控件。
- 当前包含 Liquid Glass 风格的基础控件、导航、弹层、输入、反馈和布局组件。
- 对外包名：`top.hasiy.designsystem`。
- 对外主题入口：`DesignSystemTheme`。
- 当前 Android 模糊、阴影和资源实现留在本模块，不依赖 Demo 的资源和业务代码。

### `:designsystem-tokens`

- 纯 Kotlin Multiplatform Token 模块，不依赖 Android、Compose 或 Android 资源。
- 对外包名：`top.hasiy.designsystem.tokens`。
- 提供 `GlassThemeSpec` 和 `GlassVisualStyle`，承载跨平台主题身份、语义颜色和视觉风格选择。
- 当前启用 Android、JVM、`iosArm64`、`iosSimulatorArm64` 与 `iosX64` targets。
- Windows 开发机可验证 Android、JVM 与 metadata 任务；iOS 编译、测试和实际消费必须在 macOS/Xcode 环境完成。

### `:app`

- 仅作为示例和验收应用。
- 展示所有 SDK 控件、主题切换和交互效果。
- 可以依赖 SDK，但 SDK 不得反向依赖 Demo 的资源、状态或页面代码。

## 3. 依赖方向

```text
app ───────────────▶ designsystem ───────────────▶ designsystem-tokens
 │                         │
 └── Demo 页面与验收         └── CMP 共享渲染与平台阴影适配
```

约束如下：

1. `designsystem-tokens` 只保存跨平台主题契约和纯值对象。
2. `designsystem` 负责把 Token 转换为共享 `GlassConfig` 和 Material3 `ColorScheme`。
3. `app` 不直接修改 SDK 内部实现，也不在页面中复制主题映射逻辑。
4. `commonMain` 已共享 UI，可依赖 Compose、tokens 和跨平台 Haze；Android Canvas 等平台 API 必须留在 actual/source set 中。

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
top.hasiy:hasiy-design-system-compose:2.0.0
```

该 Android artifact 的 POM 会依赖：

```text
top.hasiy:designsystem-tokens:1.0.0
```

本地模块依赖：

```kotlin
implementation(project(":designsystem"))
```

源码与 Maven 接入都必须包含完整依赖闭包。两个模块已配置 `maven-publish`，本地验证仓库
位于 `build/verification-maven`。必须一起发布根 metadata 与所需平台 artifact，独立消费者
验证工程位于 `fixtures/android-consumer`，不使用 project dependency 或 includeBuild 替换。

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
| **自製原料** | `GlassPalette` 的语义色扩展属性、`asFillSurface` / `asOverlaySurface` / `asTrackSurface` / `asSelectedSurface` / `asControlSurface`、`Modifier.tactileKeycap` / `tactilePanel` / `tactileScreenTexture`、`SevenSegmentText`、`TACTILE_*` 常量、`paletteName` | 允许下游自建组件，语义可能调整 |
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
  `./gradlew :designsystem-tokens:apiDump` 并提交 JVM/KLib 快照。Android 与 JVM 快照已建立；Native ABI 快照仍需在 macOS 生成并审阅。
- **变更流程**：`apiDump` → review diff → 与代码一并提交。快照 diff 即 API 变更提案。

## 9. 优化与验证边界（2026-10-13）

- 滑杆、分段 Tab Bar 使用最新手势回调；量程/尺寸/RTL 改变时重建相应手势。
- Checkbox、RadioButton 与 Slider 提供选中/勾选/范围和 SetProgress 语义。
- Demo 控制值保存为 Float，显示阶段再格式化；确认值与乐观预览分离，取消时撤回未确认值。
- 外/内阴影缓存 Path、Stroke 与 Android Paint/filter；Tactile 点阵使用平铺 Shader，倒角图层可跨重组复用且单个缓存不超过 4 MiB；七段数字缓存两种段形状。
- 主 Activity 保存强调色、阴影配置；主题映射按 palette/spec 缓存。
- JVM 异步回归覆盖成功、失败、拒绝、重复提交、切指標、运行状态取消及德语 Locale。
- Android UI 回归覆盖最新回调/量程、无障碍状态与 RTL 分段拖动。

验证命令及 fixture 说明见 [BUILDING.md](BUILDING.md)。实际执行结果记录在
[OPTIMIZATION_REPORT.md](OPTIMIZATION_REPORT.md)，不要把实现完成等同于性能或所有平台验收通过。

仍需分别验收：API 26–30 模糊回退、iOS 真机视觉和外部消费者、macOS Native ABI 快照、
正式外部 Maven 仓库与空缓存消费。`explicitApiWarning()` 的既有公开面告警仍保留，后续升级
严格模式需单独清理并审阅全部公开声明。
