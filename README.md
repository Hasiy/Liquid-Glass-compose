# Liquid Glass for Compose

一套 Jetpack Compose 的液态玻璃（Liquid Glass）组件库，外加一个把所有组件都摆出来的示例 app。

玻璃质感由一个 `Modifier.glassSurface` 提供 —— 半透明基底渐变、跟随手指的柔光、边缘描边、接触阴影 —— 再往上包出 Material 3 常用组件的玻璃版本。整套视觉由一个 `GlassConfig` 驱动，可以在深色玻璃与浅色中性两种完全不同的风格之间切换，而不用改任何组件代码。

```
├── liquidglass/    SDK，可独立发布的 Android Library，不依赖 demo 的任何资源
└── app/            示例 app，展示全部组件 + 主题实时切换
```

---

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [主题与定制](#主题与定制)
- [组件清单](#组件清单)
- [使用约束（踩过的坑）](#使用约束踩过的坑)
- [示例 app](#示例-app)
- [构建与发布](#构建与发布)
- [相关文档](#相关文档)

---

## 环境要求

| 项 | 版本 |
|---|---|
| minSdk | 26 |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.0.21 |
| AGP | 8.7.3 |
| Compose BOM | 2024.10.01（Material3 1.3.1） |
| 背景模糊引擎 | [haze](https://github.com/chrisbanes/haze) 1.4.0 |

部分效果有 API 门槛，SDK 内部会自动降级，不需要调用方判断：

- **弹层背景模糊**：设备不支持时退回更浓的磨砂底色（`overlayFallbackAlpha`），保证底下的文字不会清晰穿透。
- **柔和外阴影**：API 28+ 用 `BlurMaskFilter` 做真高斯模糊；API 26–27 的硬件加速 Canvas 会忽略它，退回多层同心 Stroke 叠加。

---

## 快速开始

### 引入

同一个工程内直接依赖 module：

```kotlin
dependencies {
    implementation(project(":liquidglass"))
}
```

或者打成 AAR：

```bash
./gradlew :liquidglass:assembleRelease
```

产物在 `liquidglass/build/outputs/aar/liquidglass-release.aar`。**直接拷贝 AAR** 时（而不是通过 module 或 Maven 依赖）需要自己补上模糊引擎，因为裸 AAR 不带传递依赖：

```kotlin
implementation("dev.chrisbanes.haze:haze:1.4.0")
```

也可以走本地 Maven：

```bash
./gradlew :liquidglass:publishReleasePublicationToMavenLocal
```

```text
top.hasiyliquidglass:liquidglass-compose:1.0.0
```

### 最小示例

```kotlin
import top.hasiy.liquidglass.*

@Composable
fun Screen() {
    // 每个页面包一层，作为弹层的背景取样源
    GlassBackdropHost(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            GlassCard {
                Text("玻璃卡片", color = GlassConfig.Default.contentColor)
            }
            GlassButton(text = "送出", onClick = { })
        }
    }
}
```

给任意自定义组件套玻璃质感：

```kotlin
Box(
    modifier = Modifier
        .glassSurface(shape = RoundedCornerShape(24.dp))
        .padding(16.dp)
) {
    Text("任何内容")
}
```

---

## 核心概念

### `Modifier.glassSurface`

所有玻璃组件都建立在它之上。它按固定顺序叠了这些东西：

```
onSizeChanged → shadow（阴影在裁切外侧）→ 柔和外阴影 → clip（内容裁成形状）
→ drawBehind（基底渐变 + 柔光 + 指尖高光）→ 内阴影 → border（内部描边）
→ pointerInput（触摸观察）
```

触摸观察全程只用 `PointerEventPass.Initial` 监听、不消费事件，所以不影响点击和拖动手势。按住时柔光跟随手指、亮度提高并收敛，同时在指尖叠一层高光；松手后以 spring 动画淡出并移回原位。

### `GlassConfig`

一个 `@Stable data class`，所有视觉参数都在里面，按用途分组：

| 分组 | 字段 |
|---|---|
| 基底 | `baseColor`、`bodyTopAlpha`、`bodyBottomAlpha` |
| 柔光 | `highlightColor`、`highlightBlendMode`、`highlightInnerAlpha`、`highlightOuterAlpha`、`highlightCenterX/Y`、`highlightRadiusFactor`、`touchSpotPeakAlpha` |
| 描边 | `borderColor`、`borderWidth`、`borderTopAlpha`、`borderBottomAlpha` |
| 内阴影 | `innerShadowAlpha`、`innerShadowWidth` |
| 接触阴影 | `shadowEnabled`、`shadowElevation`、`shadowAmbientAlpha`、`shadowSpotAlpha` |
| 柔和外阴影 | `softShadowSpread`、`softShadowOffsetY`、`softShadowAlpha`、`softShadowColor` |
| 内容 | `contentColor`、`accentEnabled`、`accentColor` |
| 页面 | `pageBackgroundTop`、`pageBackgroundBottom` |
| 交互 | `followTouchHighlight`、`hideHighlightOnTouch` |
| 弹层 | `overlayBlurRadius`、`overlayFallbackAlpha` |
| 模式 | `native` |

每个字段都有默认值，所以 `copy()` 只写要改的那几个就行。

### 主题预设

`GlassPresets` 提供四套：

| 预设 | 风格 |
|---|---|
| `Drop` | 半透明白玻璃、左上柔光、底部聚光（默认） |
| `Neutral` | 浅灰页面 + 毛玻璃容器 + 白色控件，唯一的**浅色**主题 |
| `Dark` | 深色半透明玻璃，柔光较强 |
| `Native` | 关掉玻璃效果，组件内部改用 Material 3 原生渲染 |

`Neutral` 不只是「Drop 调淡」，它跨越了明暗反转 —— 柔光从「白色 Screen 提亮」变成「黑色 Multiply 压暗」（白底上提亮是无效的），描边、内容色、页面底色全部反向。`isLightSurface` 扩展属性就是靠 `highlightBlendMode == Multiply` 来判断的。

### `GlassBackdropHost` 与弹层模糊

页面根节点包一层 `GlassBackdropHost`。它把 `content` 记录成背景取样源，本身不会被模糊；所有弹层通过同一个 `HazeState`，**只在自己的圆角轮廓内**绘制取样后的高斯模糊，弹层之外保持清晰。

覆盖范围：Dialog、Popup、Dropdown / ExposedDropdown、ContextMenu、docked search 结果层、Tooltip、DatePicker、TimePicker、Drawer、BottomSheet / ModalBottomSheet、Snackbar。

玻璃版的模态组件用透明 scrim —— 看不见，但仍然负责「点击外部关闭」。

---

## 主题与定制

### 用 colors.xml 换配色

配色的权威来源是 `liquidglass/src/main/res/values/colors.xml`，换色不用动 Kotlin。使用端在自己的 `res/values/colors.xml` 里声明同名资源即可覆盖（Android 资源合并的标准行为）：

```xml
<!-- app/src/main/res/values/colors.xml -->
<resources>
    <color name="glass_neutral_base">#FFFAFAFA</color>
    <color name="glass_accent">#FF2F80ED</color>
</resources>
```

取用入口是 `@Composable` 的 `GlassPresets.themed()` / `drop()` / `neutral()` / `dark()` / `native()`，它们把资源颜色套到静态预设的**结构参数**（透明度、圆角、阴影）上。透明度这类留在 Kotlin，因为它们属于主题的结构而不是配色。

> `GlassPresets.Drop` 这些静态字段保留着，作为非 Composable 场景（组件默认参数）的回退值，**不读资源**。所以判断「当前是哪个主题」不要拿 `GlassConfig` 去比对 —— 资源被覆盖后 `themed()` 的值就不等于静态字段了。用 `isLightSurface`，或者按 `themed()` 的索引。

可覆盖的资源名：

```
glass_{drop|neutral|dark|native}_{base|content|highlight|border|page_top|page_bottom}
glass_accent
```

### 强调色

`accentEnabled` 默认 **false**。开启后，选中态与填充段改用 `accentColor` 实色：进度条 / 滑杆 / 音量条的已填充段、导航项与 Chip 的选中态、开关的开启轨道。

只有 `Neutral` 默认打开 —— 近白表面没有提亮空间，选中态必须靠颜色区分。`Drop` 与 `Dark` 保持原本的「玻璃提亮」质感。

两个分支的逻辑集中在三个派生函数里，不散落在各组件：

| 函数 | 用途 | 强调色开启 | 强调色关闭 |
|---|---|---|---|
| `asFillSurface(enabled)` | 进度 / 滑杆 / 音量的已填充段 | 强调色实心 | 深色往白提亮 / 浅色往黑压暗 |
| `asSelectedSurface(strong)` | 导航项、Chip、开关轨道的选中态 | 强调色淡底 + 同色描边 | 深色调亮玻璃 / 浅色压暗玻璃 |
| `asControlSurface()` | 内嵌控件的**未选中**表面 | — | 浅色下换成近白实面，深色下恒等 |

### 阴影

两套阴影，由 `shadowEnabled` 总开关控制：

- **系统 elevation 阴影**（`shadowElevation` + `shadowAmbientAlpha` / `shadowSpotAlpha`）—— `Drop` / `Dark` 用它。
- **柔和外阴影**（`softShadowSpread` > 0 时启用，取代前者）—— 自绘的大扩散阴影，`Neutral` 用它。系统 elevation 阴影的扩散被 elevation 绑死，在浅色底上怎么调都偏紧。

柔和外阴影与 `shadowElevation` 联动：**elevation 设为 0 的元件不画外阴影**。所以填充段、选中态、滑杆轨道这些本来就不该投影的地方，不必逐个再关一次。

三个旋钮各管一件事，注释里也写了：

- `softShadowAlpha` — 深浅。实测贴边最深处与背景的灰阶差：`0.032f` → 3 阶、`0.05f` → 4 阶、`0.075f` → 6 阶。
- `softShadowSpread` — 范围。
- `SOFT_SHADOW_BLUR_FACTOR`（在 `GlassModifier.kt`）— 模糊半径比例。**注意它会连带影响深浅**：高斯模糊是把固定的浓度摊开，调大扩散就会同时变淡。想单独改深浅只动 alpha。

---

## 组件清单

| 分类 | API |
|---|---|
| 基础 | `glassSurface`、`glassBlur`、`glassBackdrop`、`GlassCard`、`GlassButton`、`GlassIconButton` |
| 弹层宿主 | `GlassBackdropHost`、`GlassDialogBlurHost`、`GlassPopupBlurBox` |
| 对话框 / 浮层 | `GlassDialog`、`GlassPopup`、`GlassSnackbar`、`GlassTooltipBox` |
| 选择 | `GlassCheckbox`、`GlassRadioButton`、`GlassSwitch`、`GlassSlider`、`GlassVolumeSlider` |
| 导航 | `GlassSegmentedTabBar`、`GlassNavigationBar`、`GlassNavigationBarItem`、`GlassNavigationRail`、`GlassNavigationRailItem`、`GlassModalNavigationDrawer` |
| 底部面板 | `GlassBottomSheet`、`GlassBottomSheetScaffold`、`GlassModalBottomSheet` |
| 操作 | `GlassFloatingActionButton`、`GlassExtendedFloatingActionButton`、`GlassBadge` |
| Chip | `GlassAssistChip`、`GlassSuggestionChip`、`GlassFilterChip`、`GlassInputChip` |
| 菜单 | `GlassDropdownMenu`、`GlassDropdownMenuItem`、`GlassExposedDropdownMenuBox`、`GlassContextMenuArea` |
| 输入 | `GlassTextField`、`GlassSearchBar`、`GlassDockedSearchBar` |
| 日期时间 | `GlassDatePickerDialog`、`GlassTimePickerDialog` |
| 反馈 | `GlassProgressBar`、`GlassPullToRefreshBox` |
| 布局 / 列表 | `GlassTopBar`、`GlassListItem` |
| 设置类 | `GlassThemeSelector`、`GlassColorPicker` |

每个组件都接受 `config: GlassConfig` 参数，且都实现了 `native = true` 时的 Material 3 原生分支。

---

## 使用约束（踩过的坑）

这一节记的都是实机上撞出来的问题，值得先看一眼。

### 点击 modifier 必须排在 `glassSurface` 之后

`glassSurface` 内部含 `clip(shape)`。Material 的涟漪（indication）由点击 modifier 自己绘制，排在 `clip` 之前就不会被裁，会以**直角矩形溢出圆角**。

```kotlin
// ✅
Modifier.glassSurface(shape, config).clickable { }

// ❌ 涟漪溢出圆角
Modifier.clickable { }.glassSurface(shape, config)
```

`selectable`、`toggleable`、`combinedClickable` 同理。

### 浮层要放在 host 的 `overlay` 而不是 `content`

要对背后做背景模糊的元件（浮动底栏、顶栏、FAB 容器），必须放在 `GlassBackdropHost` 的 `overlay` 里。放在 `content` 里它自己也是取样源的一部分，会**取样到自己**。

```kotlin
GlassBackdropHost(
    modifier = Modifier.fillMaxSize(),
    overlay = {
        MyFloatingTabBar(Modifier.align(Alignment.BottomCenter))
    }
) {
    ScreenContent()
}
```

### 浅色主题的层次规则

`Neutral` 是三层结构，破坏其中任何一层都会糊在一起：

| 层 | 做法 |
|---|---|
| 页面 | 浅灰渐变 `#E3E3E3 → #D1D1D1` |
| 容器（毛玻璃） | 半透明白，透出页面底色而偏灰 |
| 控件（白色） | `asControlSurface()` → 近白实面 |

两条配套规则：

- **只有容器投影，控件不投影**。给每个小控件都加阴影会让整页布满灰晕。`asControlSurface()` 在浅色分支把 `shadowElevation` 归零，借助上面那条联动一并关掉外阴影。
- **阴影扩散不要大过元件之间的间隙**，否则相邻元件的阴影互相叠加，间隙会糊成一条灰带。

### 细长元件上方向性效果会自动淡出

柔光半径按宽度算、中心固定在左上，在宽高比悬殊的元件（列表项、输入框、进度条轨道）上会被拉成一道横跨整个宽度的**横向渐变**，左亮右暗。

所以 `glassSurface` 按 `elongation`（长边 / 短边）渐进淡出方向性效果：比例 ≤ `1.6` 完全保留，≥ `2.5` 完全关闭，中间线性过渡。淡出的不只是柔光，还包括基底的垂直渐变（lerp 到上下平均值）和被拉长的阴影。接近方形的元件（按钮、卡片、圆钮）完全不受影响。

### 系统栏 inset 要加在 `verticalScroll` 之前

```kotlin
Column(
    Modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .verticalScroll(state)   // ← inset 必须在这之前
        .padding(20.dp)
)
```

加在 `verticalScroll` 之后，这个 padding 就属于滚动内容，一往上滚就跟着跑掉，标题照样会钻进状态栏和摄像头挖孔。另外用 `safeDrawing` 而不是 `statusBarsPadding()`，横放时侧边的挖孔才会一并避开。

### `GlassBottomSheetScaffold` 是页面级组件

和 Material 3 原版一样，它需要一个有限高度，**不能**直接放进 `verticalScroll` 或其他以无限高度测量子节点的父容器里。

### 系统栏图标要跟着主题走

`enableEdgeToEdge()` 之后，深色主题配深色状态栏图标会看不清。让它跟随 `isLightSurface`：

```kotlin
val view = LocalView.current
SideEffect {
    val window = (view.context as Activity).window
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = lightSurface
        isAppearanceLightNavigationBars = lightSurface
    }
}
```

---

## 示例 app

`:app` 把所有组件摆在一个可滚动页面里，顶部是设置区：

- **主题切换** — Drop / Neutral / Dark / Native 实时切换，`MaterialTheme` 的深浅也跟着走（否则 Native 模式下会出现深色 Material 组件配浅色页面）。
- **强调色** — 开关 + 六色选色盘，候选色定义在 `app/src/main/res/values/colors.xml` 的 `accent_*`。切主题时会重置为该主题的默认值。
- **阴影** — 总开关，关掉后画面变平，轮廓由描边和内阴影撑住。

底部是一个浮动的动态光照 Tab Bar：拖动时有跟手的光斑（`RuntimeShader`，API 33+），叠在内容之上并对背后做真背景模糊，内容会从它底下滚过去。

```bash
./gradlew :app:installDebug
```

---

## 构建与发布

```bash
# 编译 SDK
./gradlew :liquidglass:assembleRelease

# 编译并安装 demo
./gradlew :app:installDebug

# 发布到本地 Maven
./gradlew :liquidglass:publishReleasePublicationToMavenLocal
```

---

## 相关文档

- [liquidglass/README.md](liquidglass/README.md) — SDK 的 API 说明与模糊契约（英文）
- [neutral_theme_plan.md](neutral_theme_plan.md) — Neutral 浅色主题的完整改造方案与实施记录，包含每一轮调整的原因
- [dynamic_light_tab_bar.md](dynamic_light_tab_bar.md) — 动态光照 Tab Bar 的设计说明
