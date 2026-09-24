# Hasiy Design System Compose SDK 公模接入与二次开发指南

## 1. 适用范围

本文面向两类读者：

- **SDK 使用方**：在 Android Compose 应用中接入 Hasiy Design System，使用主题、玻璃表面和现成组件。
- **二次开发方**：增加品牌主题、自建业务组件、扩充 SDK 公共组件，或为新平台实现渲染器。

当前可直接使用的 UI SDK 是 Android Library `:designsystem`；`:designsystem-tokens` 是纯
Kotlin Multiplatform 主题契约，不包含 Compose UI 或 iOS 渲染实现。

## 2. 当前基线

| 项目 | 当前值 |
|:--|:--|
| Android minSdk | 26 |
| Android compileSdk / targetSdk | 35 / 35 |
| Kotlin | 2.0.21 |
| Java bytecode target | 11 |
| 推荐构建 JDK | 21 |
| Compose BOM | 2024.10.01 |
| Haze | 1.4.0 |
| Android SDK 坐标 | `top.hasiy:hasiy-design-system-compose:1.0.0` |
| Token artifact 坐标 | `top.hasiy:designsystem-tokens:1.0.0` |

构建 JDK 与产物 bytecode target 是两件事：本项目推荐用 JDK 21 启动 Gradle，但 Android
模块仍以 Java 11 为 source/target compatibility。

## 3. 选择接入方式

| 方式 | 当前状态 | 适用场景 |
|:--|:--:|:--|
| 源码模块 | 推荐、可用 | 同仓开发、内部公模、需要调试或二次开发 |
| Maven 仓库 | 目标推荐、发布链待补齐 | 多项目稳定消费、统一版本治理 |
| 裸 AAR | 非推荐、非单文件 | 受限离线环境，且能成套管理所有依赖 |

### 3.1 源码模块接入

将 `designsystem` 与 `designsystem-tokens` 两个目录同时纳入工程，并在根
`settings.gradle.kts` 中注册：

```kotlin
include(":designsystem")
include(":designsystem-tokens")
```

应用模块只需要依赖 Android SDK；它会通过 project dependency 获得 tokens：

```kotlin
dependencies {
    implementation(project(":designsystem"))
}
```

不要让 `designsystem` 反向依赖应用模块，也不要把应用的图片、字符串或业务状态复制到
SDK 内部。

### 3.2 Maven 接入

最终消费者应只声明顶层 Android 坐标：

```kotlin
dependencies {
    implementation("top.hasiy:hasiy-design-system-compose:1.0.0")
}
```

Maven/Gradle metadata 会负责 Compose、Material3、Haze 与 tokens 的依赖关系。但当前仓库的
tokens 模块尚未配置 Maven publish task，因此 **1.0.0 不能仅凭已有 Android publication
配置视为可对外发布**。发布者必须先完成以下事项：

1. 为 `designsystem-tokens` 配置 `maven-publish`。
2. 同一版本同时发布 `designsystem-tokens` 与 `hasiy-design-system-compose`。
3. 在空 Gradle 缓存、独立 consumer 工程中只声明顶层坐标，验证依赖可解析、编译和运行。
4. 保存 POM/Gradle Module Metadata、artifact checksum 与 API 快照作为发布证据。

### 3.3 裸 AAR / 离线包

当前 AAR 产物是：

```text
designsystem/build/outputs/aar/designsystem-release.aar
```

它不内嵌以下依赖：

- `designsystem-tokens/build/libs/designsystem-tokens-jvm-1.0.0.jar`
- Haze 1.4.0
- Compose UI、Material3、Material Icons、Lifecycle 与 Core KTX

因此只复制 AAR 并补一条 Haze 依赖仍不完整。若业务环境强制使用离线包，交付物至少应包含
AAR、tokens JVM JAR、依赖版本清单、consumer Gradle 示例、SHA-256 校验值和许可证清单。
未建立自动 consumer fixture 前，不把裸 AAR 宣称为支持的标准接入方式。

## 4. 最小使用方式

推荐用完整语义主题 `ThemePalette`，并在页面根节点只放一个 `GlassBackdropHost`：

```kotlin
@Composable
fun ExampleScreen() {
    DesignSystemTheme(palette = ThemePalette.Lime) {
        val config = LocalGlassConfig.current
        GlassBackdropHost(
            modifier = Modifier.fillMaxSize(),
            overlay = {
                GlassNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    config = config,
                ) {
                    // Navigation items
                }
            },
        ) {
            ScreenContent()
        }
    }
}
```

主题与宿主职责不同：

- `DesignSystemTheme` 提供 `MaterialTheme`、`LocalGlassConfig` 和 `LocalGlassThemeSpec`。
- `GlassBackdropHost` 记录页面背景，供浮层或悬浮控件在自身轮廓内取样模糊。
- 普通页面内容放 `content`；需要模糊背后内容的悬浮控件放 `overlay`。
- 一个页面只建立一个背景取样源；嵌套 Host 会复用外层状态，但不应成为常规布局手段。

## 5. 主题选择与定制

### 5.1 两种主题入口

| 入口 | 内容 | 建议 |
|:--|:--|:--|
| `DesignSystemTheme(spec = GlassThemeSpec(...))` | 基础 Material 与玻璃颜色、明暗、视觉结构 | 简单主题或跨平台契约最小化场景 |
| `DesignSystemTheme(palette = ThemePalette...)` | 完整语义色：强调、轨道、危险态、面板、前景层级等 | 公模和业务二次开发默认选择 |

SDK 内置 `ThemePalette.Lime`、`Sky`、`Nordic`、`Ocean`、`Ember`、`Aurora`、
`Digital` 与 `Tactile`。持久化主题选择时保存 `ThemePaletteId.id`，恢复时调用
`ThemePalette.fromId(value)`，不要保存显示文案或 enum ordinal。

### 5.2 品牌主题

品牌主题应在应用或品牌扩展模块中定义一个 `ThemePalette`，再统一传给
`DesignSystemTheme`。不要在每个组件中写 `if (brand == ...)`，也不要在业务页面自行复制
`toGlassConfig()` 或 Material3 映射。

新增或修改主题时至少验证：

- `accent` 上的 `onAccent` 可读性。
- `screenContent`、`muted` 在 `screen`/`panel` 上的对比度。
- `danger` 的文字、容器与禁用态。
- `track`、已填充段和选中态在浅/深主题下都可区分。
- `background` 与 `screen` 的明暗语义；Tactile 是“浅画布、深屏幕”，系统栏应看
  `ThemePalette.isCanvasLight`，不能直接看 `isLight`。

### 5.3 Android 资源覆盖

使用端可以用同名 `colors.xml` 覆盖 `GlassPresets.themed()` 使用的 SDK 颜色资源：

```xml
<resources>
    <color name="glass_neutral_base">#FFFAFAFA</color>
    <color name="glass_accent">#FF2F80ED</color>
</resources>
```

资源覆盖不等于修改 `ThemePalette` 中的纯 Kotlin Token。使用完整语义主题时，应创建或复制
`ThemePalette`，不要期待同名 Android 资源自动改变 KMP Token。

## 6. 组件使用规则

### 6.1 优先使用稳定入口

- 页面主题：`DesignSystemTheme`、`LocalGlassConfig`、`LocalGlassThemeSpec`。
- 表面：`Modifier.glassSurface`、`glassBackdrop`、`glassBlur`。
- 背景取样：`GlassBackdropHost`。
- 组件：公开的 `Glass*` 控件。
- 自建视觉原料：`asFillSurface`、`asOverlaySurface`、`asTrackSurface`、
  `asSelectedSurface`、`asControlSurface`、Tactile modifiers 与 `SevenSegmentText`。

`GlassThemeSelector`、`GlassColorPicker`、`GlassPaletteSelector` 属于展示/调试组件；生产业务
可以参考，但不要把它们当成稳定业务设置 API。`legacy` 包以及
`GlassDialogBlurHost`、`GlassPopupBlurBox` 只为旧接入保留，新代码不再采用。

### 6.2 Modifier 顺序

`glassSurface` 内部负责圆角裁切。需要 ripple 的交互 modifier 必须放在它之后：

```kotlin
Modifier
    .glassSurface(shape = shape, config = config)
    .clickable(onClick = onClick)
```

`clickable`、`selectable`、`toggleable`、`combinedClickable` 都遵循同一规则；顺序反过来会让
ripple 以矩形溢出圆角。

### 6.3 背景模糊与降级

- API 31+、存在 `GlassBackdropHost` 且非 Native 风格时使用 Haze 背景模糊。
- API 26–30、缺少 Host 或 Native 风格时走磨砂色回退。
- 前景文字、图标和控件内容不能被模糊；模糊只发生在表面自身轮廓内。
- `overlayFallbackAlpha` 必须保证背景文字不会穿透到影响可读性。
- `GlassBottomSheetScaffold` 必须获得有限高度，不能直接放在 `verticalScroll` 等无限高度父容器中。

### 6.4 系统栏、窗口与可访问性

- edge-to-edge 页面应依据当前画布明暗同步状态栏/导航栏图标。
- `safeDrawing` inset 放在滚动 modifier 之前，横屏时同时避开侧边 cutout。
- 无可见文字的 IconButton/Image 必须提供本地化 `contentDescription`。
- 业务文案放调用方 `strings.xml`；SDK 不接收或硬编码品牌业务文案。
- API 26、30、31 与当前 targetSdk 设备都要检查字体缩放、TalkBack、键盘和横竖屏。

## 7. 二次开发边界

### 7.1 代码应该放在哪里

| 变更类型 | 归属 |
|:--|:--|
| 纯颜色、主题身份、跨平台枚举或值对象 | `designsystem-tokens/commonMain` |
| Android Compose 组件、Haze、Canvas、Android 资源 | `designsystem` |
| 产品页面、业务状态、导航、品牌图片与验收场景 | 使用方 app 或独立业务组件模块 |
| iOS/其他平台渲染 | 新的平台 renderer 模块，不进入 tokens `commonMain` |

禁止把 Android `Color`、`Dp`、资源 ID、`Context`、Haze 或 Material3 类型放进
`designsystem-tokens/commonMain`。

### 7.2 自建业务组件

业务组件默认从 `LocalGlassConfig.current` 取配置，并允许调用方显式覆盖：

```kotlin
@Composable
fun BrandStatusCard(
    modifier: Modifier = Modifier,
    config: GlassConfig = LocalGlassConfig.current,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.glassSurface(
            shape = shape,
            config = config.asControlSurface(),
        ),
    ) {
        content()
    }
}
```

自建组件要复用语义派生函数，避免复制 alpha、边框、阴影和主题判断。若组件需要悬浮模糊，
由页面把它放入 Host 的 `overlay`，组件本身只负责 `glassBackdrop + glassSurface + foreground`。

### 7.3 新增 SDK 公共组件

新增 `Glass*` API 前依次确认：

1. Material3 是否已有可包装的行为和无障碍语义。
2. 默认 `config` 使用 `LocalGlassConfig.current`，局部仍可覆盖。
3. Native 风格是否需要退回 Material3 原生控件。
4. 浅色、深色、Digital、Tactile 的内容与交互态是否可读。
5. API 26–30 是否有不依赖真模糊的降级路径。
6. public 参数、返回类型与 KDoc 是否明确；未打算公开的 helper 使用 `internal`/`private`。
7. 单元测试、必要的 Compose UI 测试、Demo 目录和文档是否同步。
8. 执行 API dump 并审阅 diff，判断是兼容新增还是破坏性变更。

### 7.4 新增视觉风格或主题

- 只加配色：优先新增 `ThemePalette`/Token，不增加组件分支。
- 增加全新材质行为：才新增 `GlassVisualStyle`，同时更新所有 exhaustive `when`、Android
  preset、主题映射、Demo 和测试。
- 新 enum 值会影响下游的 exhaustive `when`；即使二进制兼容，也可能造成源码重新编译失败，
  必须作为 API 变更审阅。
- 不用 `GlassConfig` 值相等判断当前主题；资源覆盖后静态 preset 与运行时配置可能不相等。

### 7.5 新增平台

`designsystem-tokens` 可以被 JVM/iOS 共同消费，但 Android `:designsystem` 不能进入
`commonMain`。新平台应实现独立映射和 renderer，并验证同一 Token 在各平台保持相同语义。

Windows 上看到 iOS targets disabled 只表示宿主不支持，不能写成 iOS 已通过。iOS 交付至少要有：

- macOS 上的 KLib/API check 与 simulator/device test。
- Swift 或 CMP consumer fixture 的真实编译。
- 明暗、动态字体、VoiceOver、safe area 和生命周期验证。

## 8. 公开 API 与版本治理

Android 公开面以 `designsystem/api/designsystem.api` 为准。任何公共签名变化都执行：

```powershell
.\gradlew.bat :designsystem:apiDump
git diff -- designsystem/api/designsystem.api
.\gradlew.bat :designsystem:apiCheck
```

tokens 类型属于 SDK 的公共依赖，也必须建立并校验自己的 JVM/KLib 快照。当前缺少该快照，
发布前应先执行 `:designsystem-tokens:apiDump` 并审阅生成文件。

版本规则建议：

- 兼容新增组件/参数重载：minor。
- 修复实现且不改变契约：patch。
- 删除/重命名 API、改变默认行为、公共数据类构造参数或 enum 兼容性：major，或在 1.0 前明确迁移窗口。
- `legacy` 只能修复缺陷，不增加新能力；删除前提供替代 API 和迁移说明。

## 9. 构建与验收

Windows PowerShell 使用 `gradlew.bat`。本机若出现 Kotlin daemon 临时目录问题，可以临时使用：

```powershell
.\gradlew.bat '-Pkotlin.compiler.execution.strategy=in-process' <tasks>
```

发布候选的最小命令集：

```powershell
.\gradlew.bat :designsystem-tokens:jvmTest
.\gradlew.bat :designsystem-tokens:apiCheck
.\gradlew.bat :designsystem:testDebugUnitTest :designsystem:apiCheck
.\gradlew.bat :designsystem:assembleRelease
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

还必须执行两个消费者验收：

1. 空缓存 Maven consumer 只声明顶层坐标，可以解析、编译、启动并显示至少一个组件。
2. API 26–30 和 API 31+ 设备各验证一套普通表面、悬浮模糊、Dialog/Popup 与输入组件。

当前仓库的详细 Windows/JDK/设备故障排查见 [BUILDING.md](BUILDING.md)。架构验收的当前状态
与未通过项见 [design-system-architecture.md](design-system-architecture.md)。

## 10. 提交前检查表

- [ ] 变更放在正确模块，没有把平台类型泄漏进 `commonMain`。
- [ ] 新组件默认读取 `LocalGlassConfig`，没有硬编码主题名或业务文案。
- [ ] 浅色、深色、Digital、Tactile 与 Native 分支均已检查。
- [ ] API 26–30 降级仍可读，API 31+ 模糊只在自身轮廓内发生。
- [ ] 交互 modifier 顺序正确，ripple 不溢出圆角。
- [ ] TalkBack/VoiceOver、字体缩放、inset、横竖屏和键盘行为已覆盖。
- [ ] public API 的可见性、返回类型、KDoc 和稳定级别明确。
- [ ] Android 与 tokens API dump 已更新并审阅，`apiCheck` 通过。
- [ ] JVM/Android 测试、Release AAR、Demo 和独立 consumer fixture 通过。
- [ ] Maven 同版本发布 Android 与 tokens artifact，空缓存可解析。
- [ ] iOS 相关结论来自 macOS/Xcode 实测，没有用 Windows metadata 结果代替。
- [ ] 发布说明包含版本、依赖、迁移、已知限制和 artifact checksum。
