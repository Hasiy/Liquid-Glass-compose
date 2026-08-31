# Neutral（中性）主题改造方案

目标：把 `GlassPresets.Neutral` 从「Drop 的低饱和版本」改成参考图那种**浅色中性风格**——浅灰页面、近白卡片、黑色文字、极淡大扩散阴影、绿色点缀。

---

## 1. 现状：为什么 Neutral 和 Drop 看起来差不多

两个 preset 的参数逐项对比（`GlassPresets.kt`）：

| 参数 | Drop | Neutral | 差异 |
|---|---|---|---|
| `baseColor` | `#9A9AA8` | `#A5A5AE` | 几乎相同的中灰紫 |
| `bodyTopAlpha` / `bodyBottomAlpha` | 0.30 / 0.32 | 0.26 / 0.28 | −0.04 |
| `highlightInnerAlpha` | 0.16 | 0.08 | 减半，但基数本来就小 |
| `highlightOuterAlpha` | 0.035 | 0.02 | 肉眼不可分辨 |
| `highlightCenter` | (0.20, 0.08) | (0.42, 0.20) | 柔光位置偏移 |
| `borderTopAlpha` | 0.20 | 0.16 | −0.04 |
| `shadowElevation` | 5dp | 4dp | −1dp |
| `contentColor` | `White` | `White` | **完全相同** |

**结论**：两者是同一套视觉语言（深色底 + 半透明灰白玻璃 + 白字）在数值上的微调。差异全部落在「透明度差 0.04」这个量级，低于感知阈值。要做出参考图的效果，不是调数值的问题，而是**要跨越明暗反转**。

---

## 2. 参考图的风格拆解

从四张参考图提取的视觉特征：

1. **页面底色是浅灰**（`#D1D1D1` ~ `#E3E3E3`），不是深色
2. **卡片是近白的高不透明度表面**，不是「透出深色背景的半透明玻璃」
3. **文字与图标是黑色**（`#000000`），次要文字是中灰（`#BDBDBD`）
4. **表面均匀，没有可见的方向性柔光**——没有左上角那一团高光
5. **描边几乎不可见**，层次靠「大扩散 + 极低透明度」的柔和阴影撑起来
6. **绿色只作为强调色**出现在少数元素：Send 按钮、地图 pin、选中态圆点、日期箭头
7. **圆角偏大**：卡片 16–24dp，按钮/胶囊全圆角
8. **字体 Lufga**（几何无衬线），字重偏轻

色板：`#000000` / `#BDBDBD` / `#D1D1D1` / `#E3E3E3` / 绿色强调。

---

## 3. 三个阻碍：SDK 里写死的深色假设

改 preset 数值走不通，因为 `glassSurface` 有三处硬编码是按「深色底」写的。

### 3.1 高光用白色 + Screen 混合 — [GlassModifier.kt:191](liquidglass/src/main/java/com/example/liquidglass/GlassModifier.kt:191)

```kotlin
0f to Color.White.copy(alpha = innerAlpha),
...
blendMode = BlendMode.Screen
```

`Screen` 的作用是提亮。白色 Screen 到接近白的表面上 → **结果不变**。所以浅色 Neutral 的静态柔光和**触摸跟随光影会同时消失**。这是最关键的一条：浅色表面的按压反馈必须改成「压暗」而不是「提亮」。

### 3.2 边框用白色 — [GlassModifier.kt:233](liquidglass/src/main/java/com/example/liquidglass/GlassModifier.kt:233)

```kotlin
Color.White.copy(alpha = config.borderTopAlpha),
Color.White.copy(alpha = config.borderBottomAlpha)
```

白描边画在白卡片上不可见。浅色主题要么用极淡的深色描边，要么干脆把描边 alpha 归零、只靠阴影分层（参考图是后者）。

### 3.3 页面背景写死在 Demo — [DynamicLightTabBarDemoScreen.kt:70](app/src/main/java/com/example/liquidglassdemo/ui/DynamicLightTabBarDemoScreen.kt:70)

```kotlin
private val BACKGROUND_TOP = Color(0xFF2A1630)
private val BACKGROUND_MIDDLE = Color(0xFF19142B)
private val BACKGROUND_BOTTOM = Color(0xFF0B0D1A)
```

这三个常量与主题无关。切到 Neutral 时白卡片会压在深紫背景上，效果比现在更糟。**页面背景必须跟着主题走**，否则前面所有改动都白做。

### 3.4 进度条填充 lerp 到白色 — [GlassComponents.kt:626](liquidglass/src/main/java/com/example/liquidglass/GlassComponents.kt:626)

```kotlin
baseColor = lerp(config.baseColor, Color.White, 0.35f)
```

浅色主题下「更白的填充压在近白轨道上」= 看不见进度。

---

## 4. 改造方案

### 4.1 `GlassConfig` 新增字段

全部带默认值，Drop / Dark / Native 的行为完全不变。

| 字段 | 类型 | 默认值 | 作用 |
|---|---|---|---|
| `highlightColor` | `Color` | `Color.White` | 柔光与指尖高光的颜色 |
| `highlightBlendMode` | `BlendMode` | `BlendMode.Screen` | 深色底用 `Screen`（提亮），浅色底用 `Multiply`（压暗） |
| `borderColor` | `Color` | `Color.White` | 描边颜色 |
| `pageBackgroundTop` | `Color` | `Color(0xFF2A1630)` | 页面背景渐变起点 |
| `pageBackgroundBottom` | `Color` | `Color(0xFF0B0D1A)` | 页面背景渐变终点 |
| `accentColor` | `Color` | `Color(0xFF00A15C)` | 强调色（可选，见 4.5） |

`BlendMode` 是 value class，放进 `data class` 不影响 `equals` / `copy`，也不破坏 `@Stable`。

> 关于页面背景：不建议用 `Brush` 或 `List<Color>` 字段——`Brush` 不是稳定类型，`List` 会让 `@Stable` 的语义变弱。两个 `Color` 字段足够表达线性渐变，中间色由使用者自行插值。

### 4.2 `glassSurface` 的三处替换

`GlassModifier.kt` 里把硬编码换成读 config：

```kotlin
// 柔光（原 191-200 行）
0f to config.highlightColor.copy(alpha = innerAlpha),
0.45f to config.highlightColor.copy(alpha = outerAlpha),
1f to Color.Transparent
...
blendMode = config.highlightBlendMode

// 指尖高光（原 214-225 行）同样替换 Color.White → config.highlightColor
//                                blendMode → config.highlightBlendMode

// 描边（原 233-234 行）
config.borderColor.copy(alpha = config.borderTopAlpha),
config.borderColor.copy(alpha = config.borderBottomAlpha)
```

改动量很小，且对现有三个 preset 是恒等变换。

### 4.3 Neutral preset 的新数值

```kotlin
/** 中性：淺灰頁面上的近白卡片，無方向性柔光，靠大擴散淡陰影分層 */
val Neutral = GlassConfig(
    // 近白表面，高不透明度——不再是「透出底色的玻璃」
    baseColor = Color(0xFFF7F7F7),
    bodyTopAlpha = 0.96f,
    bodyBottomAlpha = 0.92f,

    // 柔光改為「壓暗」：白底上用 Screen 提亮是無效的
    highlightColor = Color.Black,
    highlightBlendMode = BlendMode.Multiply,
    highlightInnerAlpha = 0.05f,
    highlightOuterAlpha = 0.012f,
    highlightCenterX = 0.50f,
    highlightCenterY = 0.50f,
    highlightRadiusFactor = 0.85f,

    // 描邊近乎不可見，層次交給陰影
    borderWidth = 1.dp,
    borderColor = Color(0xFF000000),
    borderTopAlpha = 0.04f,
    borderBottomAlpha = 0.02f,

    // 大擴散、極低透明度的柔和陰影
    shadowElevation = 10.dp,
    shadowAmbientAlpha = 0.10f,
    shadowSpotAlpha = 0.06f,

    contentColor = Color(0xFF111111),
    accentColor = Color(0xFF00A15C),

    pageBackgroundTop = Color(0xFFE3E3E3),
    pageBackgroundBottom = Color(0xFFD1D1D1),

    followTouchHighlight = true,
    hideHighlightOnTouch = false,

    // 淺色彈層的磨砂底也要是淺的（baseColor 已改，這裡只調濃度）
    overlayFallbackAlpha = 0.88f,
)
```

几个数值的理由：

- `highlightCenter` 移到正中 + `radiusFactor` 放大到 0.85：参考图的卡片是均匀的，没有方向性反光。居中的大半径低透明度暗角只是给表面一点体积感，不会读成「高光」。
- `highlightInnerAlpha` 从 0.08 降到 0.05：`Multiply` 下 alpha 的感知强度比 `Screen` 高，同样数值会显脏。
- `shadowElevation` 反而从 4dp 提到 10dp：浅色 Soft UI 靠的是「远而淡」的阴影，不是「近而重」。alpha 必须同步降到 0.10 / 0.06，否则会糊成灰边。

### 4.4 组件层跟进清单

| 位置 | 现状 | 改法 |
|---|---|---|
| [GlassComponents.kt:626](liquidglass/src/main/java/com/example/liquidglass/GlassComponents.kt:626) `GlassProgressBar` 填充 | `lerp(baseColor, White, 0.35f)` | 改为 `lerp(baseColor, contentColor, 0.72f)`，或直接用 `accentColor` |
| `GlassVolumeSlider` 填充 | `baseColor = config.contentColor` | 浅色下变黑色填充，语义可接受；建议改用 `accentColor` 与参考图一致 |
| `GlassSlider` 填充 | 同上 | 同上 |
| `GlassNavigationBarItem` / `RailItem` 选中高亮 | `asNavigationItemHighlight()` 按比例缩 alpha | 浅色下 `bodyTopAlpha * 0.45` 仍接近白，选中态会看不出来。需要改成「选中时压暗」或用 `accentColor` 描边 |
| `GlassCheckbox` / `GlassRadioButton` | 勾号/圆点用 `contentColor` | 自动跟随（黑色），无需改 |
| `GlassBlurComponents.kt:119` 弹层回退 | `baseColor.copy(overlayFallbackAlpha)` | 自动跟随，只需调 `overlayFallbackAlpha` |

导航项那条是唯一需要真正重新设计的——现在的「提亮表示选中」在浅色下不成立。

### 4.5 强调色（可选但建议）

参考图里绿色承担了明确的语义：**可提交的主操作**（Send）、**当前位置**（pin）、**已选中**（付款方式圆点）。SDK 目前没有这个概念，各组件的「选中」一律靠提高 alpha——这正是导航项在浅色下失效的根因。

加 `accentColor` 后可以统一处理：主按钮填充、选中态指示、进度/音量填充。Drop 与 Dark 也能受益。

### 4.6 Demo 层：背景随主题

```kotlin
// DynamicLightTabBarDemoScreen.kt，删掉三个 BACKGROUND_* 常量
.background(
    Brush.verticalGradient(
        colors = listOf(
            glassTheme.pageBackgroundTop,
            lerp(glassTheme.pageBackgroundTop, glassTheme.pageBackgroundBottom, 0.5f),
            glassTheme.pageBackgroundBottom
        )
    )
)
```

四角的橙红紫蓝光晕（`GlowBlob`）在浅色底上会显脏，Neutral 下应降低 alpha 或整体跳过：

```kotlin
val glowAlphaScale = if (glassTheme == GlassPresets.Neutral) 0.25f else 1f
```

另外 [Theme.kt](app/src/main/java/com/example/liquidglassdemo/ui/theme/Theme.kt) 写死 `darkColorScheme`，`native` 模式下切到 Neutral 会得到深色 Material 组件配浅色页面。要么让它跟随主题，要么明确说明 Native + Neutral 不组合。

---

## 5. 触摸光影在浅色下的行为

`GlassModifier.kt` 顶部那批常量（`PRESS_INNER_BOOST`、`TOUCH_SPOT_PEAK_ALPHA` 等）是按「白色 Screen 提亮」标定的。切到「黑色 Multiply 压暗」后：

- **倍率类常量**（`PRESS_INNER_BOOST` = 1.25、`PRESS_OUTER_BOOST` = 1.6、`PRESS_RADIUS_SHRINK` = 0.92）语义不变，可以沿用。
- **绝对 alpha 类常量**（`TOUCH_SPOT_PEAK_ALPHA` = 0.07、`PRESS_INNER_MAX` = 0.22）在 Multiply 下感知更强，浅色主题需要更小的值，建议 peak 取 0.04~0.05。

这批常量目前是文件级 `private const`，无法按主题区分。两个选择：

- **A（推荐，改动小）**：把 `TOUCH_SPOT_PEAK_ALPHA` 提升为 `GlassConfig` 字段 `touchSpotPeakAlpha`，默认 0.07，Neutral 给 0.045。其余倍率常量保持不变。
- **B（改动大）**：把整批常量都搬进 config，灵活但参数爆炸，不建议。

---

## 6. 影响面与风险

- **向后兼容**：新字段全部带默认值，Drop / Dark / Native 三个 preset 的渲染结果逐像素不变。`GlassConfig` 是 `data class`，外部 `copy()` 调用不受影响。
- **`native = true` 不走 `glassSurface`**，完全不受影响。
- **风险点 1**：`Multiply` 混合在半透明表面上的表现与 `Screen` 不对称，`bodyTopAlpha` 提到 0.96 后表面接近不透明，`glassOverlayBackdrop` 的背景模糊会几乎看不到。浅色主题下模糊的意义下降，可考虑把 `overlayBlurRadius` 降到 12dp 以省开销。
- **风险点 2**：阴影 elevation 提到 10dp 后，密集排列的小组件（导航项、Chip）会互相投影糊成一片。这类组件本来就传 `shadowElevation = 0.dp`，需要逐个确认。
- **风险点 3**：Demo 的 `GlowBlob` 与浅色底冲突，见 4.6。

---

## 7. 实施顺序

每一步都能单独编译 + 真机验证，出问题好定位。

1. **加字段**：`GlassConfig` 六个新字段 + KDoc，不改任何 preset。编译通过即可，视觉零变化。
2. **改 `glassSurface`**：三处硬编码换成读 config。视觉仍应零变化（默认值恒等）。
3. **改 Neutral preset**：套用 4.3 的数值。此时切到 Neutral 应能看到浅色卡片，但页面还是深色，会很违和——预期之内。
4. **改 Demo 背景**：4.6。此时 Neutral 主题应该整体成立。
5. **组件层跟进**：4.4 的清单，重点是导航项选中态和进度条填充。
6. **（可选）强调色**：4.5，把绿色接进主按钮、选中态、填充。

---

## 8. 验收清单

切到 Neutral 主题后逐条对照参考图：

- [ ] 页面底色是浅灰，不是深色
- [ ] 卡片是近白实色，边界靠柔和阴影而非描边
- [ ] 所有文字/图标是黑色，次要文字是中灰
- [ ] 卡片表面均匀，看不出方向性高光
- [ ] 按住卡片有「压暗」的跟手光影，松手平滑复原
- [ ] 阴影是大扩散低透明度，不是深色描边感
- [ ] 导航项选中态在浅色下清晰可辨
- [ ] 进度条 / 音量条的填充段与轨道对比明确
- [ ] Dialog / Popup / Drawer / BottomSheet 的浅色磨砂底不透黑
- [ ] 切回 Drop / Dark 后视觉与改造前完全一致

---

## 9. 工作量估计

| 步骤 | 文件 | 规模 |
|---|---|---|
| 1. 加字段 | `GlassConfig.kt` | 小 |
| 2. 改 `glassSurface` | `GlassModifier.kt` | 小 |
| 3. Neutral preset | `GlassPresets.kt` | 小 |
| 4. Demo 背景 | `DynamicLightTabBarDemoScreen.kt` | 小 |
| 5. 组件跟进 | `GlassComponents.kt`、`GlassNavigationComponents.kt`、`GlassVolumeSlider.kt`、`GlassSelectionComponents.kt` | 中（导航项选中态需要重新设计） |
| 6. 强调色 | 多个组件 | 中（可延后） |

前 4 步是「让 Neutral 主题成立」的最小集合，第 5 步决定完成度，第 6 步是锦上添花。

---

## 10. 实施记录

计划的 6 步已全部落地，`:liquidglass:assembleRelease` 与 `:app:assembleDebug` 均构建通过。**尚未真机验证**（实施期间设备断连）。

### 按计划完成

| 步骤 | 结果 |
|---|---|
| 1. 加字段 | `GlassConfig` 新增 `highlightColor` / `highlightBlendMode` / `touchSpotPeakAlpha` / `borderColor` / `accentColor` / `pageBackgroundTop` / `pageBackgroundBottom`，全部带默认值 |
| 2. 改 `glassSurface` | 柔光、指尖高光、描边三处硬编码改为读 config；`TOUCH_SPOT_PEAK_ALPHA` 常量搬进 config（第 5 节方案 A） |
| 3. Neutral preset | 套用 4.3 的数值 |
| 4. Demo 背景 | 删掉 `BACKGROUND_*` 常量改读主题；浅色下装饰光晕按 `GLOW_ALPHA_SCALE_LIGHT` 调淡 |
| 5. 组件跟进 | 进度条 / `GlassSlider` / `GlassVolumeSlider` 的填充段改用 `accentColor` 实色并关掉柔光；导航项选中态改用强调色淡底 + 同色描边 + 同色内容 |
| 6. 强调色 | 见下方「计划外补充」 |

### 计划外补充

实施时发现计划漏掉的浅色失效点，一并处理：

1. **`isLightSurface` 扩展属性**（`GlassConfig.kt`）。多处需要按明暗选择对比方向（容器该比表面更深还是更浅），计划里没给判断手段。用 `highlightBlendMode == Multiply` 推导，避免再加一个可能与 `highlightColor` 互相矛盾的旗标。

2. **`GlassSegmentedTabBar` 轨道色写死 `#15151A`**。4.4 清单没列到，浅色主题下会是一条突兀的黑带。改为从 `baseColor` 按 `isLightSurface` 选择压暗比例派生。

3. **`DynamicLightTabBar`（Demo 组件）整套按深色写死**：玻璃反光、边缘描边、Pill 柔光与描边、分页文字全是 `Color.White`，Bar 底色写死 `BACKGROUND_COLOR_HEX`。计划完全没提到它，但它是页面常驻元素，不改的话 Neutral 下会是「深色条 + 白字」压在浅灰页面上。已改为从 `pillGlassConfig` 派生 `glassInk` / `glassBlend` / `glassBorderInk` / `tabTextColor` / `barBackgroundColor`。

4. **页面级文字写死白色**：`DynamicLightTabBarDemoScreen` 三处、`GlassSdkCatalog` 的 `CatalogTitle`。这些文字直接压在页面背景上，不在玻璃卡片内，浅色下不可见。改读 `contentColor`（`CatalogTitle` 因此加了 `config` 参数）。

5. **更多「靠提亮表示选中」的组件**。4.4 只列了导航项，实际还有三处，浅色下同样失效甚至语义反转（柔光是压暗的，选中反而更暗）：
   - `GlassChip` 选中 → 强调色淡底 + 同色描边 + 同色内容
   - `GlassSwitch` 开启 → 轨道换成强调色实底，thumb 保持原玻璃圆钮，形成「绿轨 + 亮钮」
   - `GlassThemeSelector` 选中 → 加强调色 2dp 描边。刻意不改按钮底色，每颗按钮仍以自己的主题渲染，保留预览各主题外观的作用

6. **`Theme.kt` 深浅跟随**（4.6 提出了问题但没给方案）。采用状态提升：`glassTheme` 移到 `MainActivity`，`LiquidGlassDemoTheme(lightSurface = glassTheme.isLightSurface)` 在深浅两套 `ColorScheme` 间切换，`DynamicLightTabBarDemoScreen` 改为受控组件（`glassTheme` + `onThemeChange`）。

---

## 11. 第二轮调整：强调色改为可选 + 配色移入 colors.xml

第一轮把强调色无条件套到所有主题，导致 Drop 与 Dark 的填充段、选中态全变成绿色。这不是想要的结果——强调色是**浅色主题的必需品**（近白表面没有提亮空间），但对深色主题只是一种风格选择。于是改成可开关的。

### 11.1 `accentEnabled` 开关

`GlassConfig` 新增 `accentEnabled: Boolean = false`。`accentColor` 只在它为 true 时生效。

- **Drop / Dark / Native**：默认 `false`，外观与改造前完全一致
- **Neutral**：默认 `true`——浅色表面没有别的选择

### 11.2 三个共用的派生函数

选中态与填充段的双分支逻辑散落在六七个组件里会难以维护，集中到 `GlassConfig.kt`：

| 函数 | 用途 | 强调色开启 | 强调色关闭 |
|---|---|---|---|
| `asFillSurface(enabled)` | 进度条完成区、滑杆已选区、音量条已填区 | 强调色实心 | 深色往白提亮 / 浅色往黑压暗 |
| `asSelectedSurface(strong)` | 导航项、Chip、开关轨道的选中态 | 强调色淡底 + 同色描边 | 深色调亮玻璃 / 浅色压暗玻璃 |
| `asControlSurface()` | 开关轨道、勾选框、单选圈的**未选中**态 | — | 浅色压暗一阶，深色不变 |

`asControlSurface` 是实机测出来的：浅色主题下未选中的开关轨道是近白，压在同样近白的卡片或浅灰页面上几乎看不见轮廓。深色主题没这个问题，所以只在 `isLightSurface` 时压暗。

### 11.3 配色移入 `colors.xml`

`liquidglass/src/main/res/values/colors.xml` 成为**配色的权威来源**，换色不需要动 Kotlin。使用端在自己的 `res/values/colors.xml` 定义同名资源即可覆盖，这是 Android 资源合并的标准行为。

取用入口是 `@Composable` 的 `GlassPresets.themed()` / `drop()` / `neutral()` / `dark()` / `native()`——它们把资源颜色套到静态 preset 的**结构参数**（透明度、圆角、阴影）上。透明度这类参数留在 Kotlin，因为它们是主题的结构而非配色。

静态的 `GlassPresets.Drop` 等字段保留，作为非 Composable 情境（组件默认参数）的回退值，不读资源。

> 因此 `GlassThemeSelector` 里判断主题名称不能再拿 `GlassConfig` 去比对——使用端覆盖资源后 `themed()` 的值就不等于静态字段了。改成按索引取名（`PRESET_NAME_RES`）。同理 demo 里判断浅色不能用 `== GlassPresets.Neutral`，改用 `isLightSurface`。

### 11.4 Demo 设置页

新增 `GlassColorPicker`（SDK 组件，一排可点选的圆形色块，选中的套同色外环）。Demo 设置区加：

- **强调色开关**：切主题时重置为该主题的默认值（浅色开、其余关），之后可手动改
- **选色盘**：仅在开关打开时显示，候选色定义在 `app/res/values/colors.xml` 的 `accent_*`

主题状态收敛成 `DemoThemeUiState`，`glassTheme = preset.copy(accentEnabled, accentColor)`——preset 用来标示设定页选中的是哪一颗，glassTheme 才是实际套用的。

### 11.5 验收结果

真机（Android 16）逐项验证通过：

- [x] 页面浅灰、卡片近白、黑字、无方向性高光
- [x] 按压是**压暗**的跟手光影，松手平滑复原
- [x] 阴影大扩散低透明度
- [x] 进度条 / 音量条填充与轨道对比明确
- [x] 导航项、主题按钮选中态清晰
- [x] Drop / Dark 切回后与改造前一致（强调色默认关闭）
- [x] 强调色开关与选色即时生效，切主题时按主题默认值重置
- [x] 浅色下未选中的开关轨道有可见轮廓（`asControlSurface`）

---

## 12. 第三轮调整：容器/控件层次 + 阴影系统重做

前两轮把 Neutral 做成了「近白卡片压在浅灰页面上」，但容器和它内部的控件用的是同一份 config —— 同样的 `#F7F7F7`、同样的 elevation，输入框和卡片糊成一片。对照参考图，正确的层次是三层。

### 12.1 毛玻璃容器 + 白色控件

| 层 | 做法 |
|---|---|
| 页面 | 浅灰渐变 `#E3E3E3 → #D1D1D1` |
| 容器（毛玻璃） | `baseColor = White`，alpha `0.60/0.52` —— 半透明后透出页面底色而偏灰 |
| 控件（白色） | `asControlSurface()` → 纯白 `alpha 0.98` |

`asControlSurface()` 的语义因此**反转**了：它原本在浅色下是压暗（第二轮为了解决「开关轨道融进背景」），现在改成提白——容器变灰之后，控件该往白的方向浮起来。反转后原来的对比问题依然成立。

按容器/控件把 40 多个 `glassSurface` 调用点分了类：

- **容器**（保持毛玻璃）：Card、TopBar、BottomSheet、NavigationBar/Rail、Drawer、ContextMenu、DropdownMenu、Tooltip、SegmentedTabBar 的轨道、Snackbar
- **控件**（走 `asControlSurface()`）：Button、IconButton、FAB、Badge、TextField、SearchBar、Chip、ListItem、Switch、Checkbox、RadioButton、Slider/VolumeSlider/ProgressBar 的轨道与圆钮、SegmentedTabBar 的滑块

深色主题下 `asControlSurface()` 是恒等，Drop / Dark 完全不受影响。

### 12.2 阴影：从内阴影到真高斯模糊

这一项反复调了很多轮，记录一下每一步的原因，避免以后走回头路。

1. **内阴影**（第二轮加的）——画在控件内侧边缘，方向就是反的
2. **系统 elevation 阴影** → 自绘。`Modifier.shadow` 的扩散跟着 elevation 走，在浅色底上怎么调都偏紧
3. **线性衰减 → 高斯**。多层同心 `Stroke` 每层等浓度时，累积浓度是线性衰减，边缘会形成一圈看得见的环
4. **收窄扩散**。扩散 26dp 而元件间隙只有 18dp，相邻元件的阴影互相叠加，间隙糊成灰带（像素扫描：最深 205，背景 233）
5. **只有容器投影**。给每个控件都加阴影会让整页佈满灰晕——这是「突兀」的主因。对照参考图，卡片有阴影、卡片**里面**的元素没有。`asControlSurface()` 把 `shadowElevation` 归零，借助「elevation 为 0 就不画外阴影」的联动一并关掉
6. **近似 → 真高斯模糊**。多层 Stroke 终究是近似，边缘再怎么调曲线都有台阶感。换成 `BlurMaskFilter` 一次绘制：过渡变成每 3px 0–1 个灰阶，形状边界的跳变从 +11 降到 +6。有了这个平滑度，浓度才敢压到 3 个灰阶还不显得断

最终：`softShadowSpread = 26dp`、`softShadowAlpha = 0.032`（贴边最深处与背景差 3 个灰阶）、`softShadowOffsetY = 7dp`、`SOFT_SHADOW_BLUR_FACTOR = 0.55`。API 27 以下的硬件加速 Canvas 会忽略 `BlurMaskFilter`，保留多层 Stroke 作为回退（minSdk 26）。

调参的注意事项写在 `GlassPresets.kt` 的注释里：三个旋钮各管一件事，其中 `BLUR_FACTOR` 会连带影响深浅（高斯模糊是把固定浓度摊开，调大扩散就会变淡），想单独改深浅只动 alpha。

### 12.3 阴影开关

`GlassConfig.shadowEnabled`（默认 true），`glassSurface` 里包住 `Modifier.shadow` 与自绘外阴影。设置页加了「阴影」开关，与强调色并列，切主题时同样重置为该主题的默认值。

### 12.4 顺带修的（不属于 Neutral 改造，但同一轮做的）

- **系统栏安全区**：`enableEdgeToEdge()` 让内容延伸到状态栏下，但滚动容器没有 inset padding，页面标题画进了状态栏。padding 必须加在 `verticalScroll()` **之前**——加在之后它属于滚动内容，一往上滚就跟着跑掉。用 `WindowInsets.safeDrawing` 而非 `statusBarsPadding()`，横放时侧边的摄像头挖孔也一并避开。
- **系统栏图标明暗**：用 `SideEffect` 让 `isAppearanceLightStatusBars` 跟随 `isLightSurface`，否则深色主题配深色图标看不清。
- **浮动 Tab Bar**：页面从 `Column`（tab bar 占版面高度）改成 `Box` 叠层，内容可以从 tab bar 底下滚过去。`glassOverlayBackdrop` 公开成 `Modifier.glassBackdrop`，tab bar 用它做真背景模糊——但它必须放在 `GlassBackdropHost` 的 `overlay` 而不是 `content` 里，否则会取样到自己。
