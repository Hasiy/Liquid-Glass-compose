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
