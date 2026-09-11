# Compose UI 实做踩坑记录

这个项目一路做下来在真机上撞出的问题，涵盖三个阶段：

1. **玻璃组件与 `Modifier.glassSurface`**（`designsystem/`）
2. **主题配色拆分与 token 回退链**（`designsystem-tokens/`、`GlassPalette.kt`）
3. **FTMS 状态矩阵页面复刻**（`app/.../ui/workout/`）

外加一轮 code review 之后的修复（无障碍语意那一节全部来自那一轮）。

按**症状**分组，因为调试时人是从「看起来不对」出发去找原因的，不是从 API 文档出发。
每条的结构：症状 → 真正的原因 → 该怎么写。

> README 的「使用约束（踩过的坑）」是给 **SDK 调用方**看的用法约束；这份是给**实做者**看的，讲布局、颜色机制、绘制原理和调试方法。

---

## 目录

- [一、尺寸与约束](#一尺寸与约束)
- [二、空状态与编辑态](#二空状态与编辑态)
- [三、排版与文字](#三排版与文字)
- [四、照抄设计稿（CSS / SVG → Compose）](#四照抄设计稿css--svg--compose)
- [五、颜色与 token](#五颜色与-token)
- [六、玻璃与绘制机制](#六玻璃与绘制机制)
- [七、开关与持久化的语义](#七开关与持久化的语义)
- [八、组件选择与 API 降级](#八组件选择与-api-降级)
- [九、无障碍语意](#九无障碍语意)
- [十、验证方式](#十验证方式)

---

## 一、尺寸与约束

这一组占了状态矩阵阶段全部返工的一半以上，而且**同一个错误犯了两次**（先是塌成 0，改完变成拉成长条），所以放在最前面。

### 1. 卡片该不该填满，取决于「这一格的高度是谁给的」

这是那两次返工的共同根因。同一个 `MetricCard` 出现在四种容器里：竖屏网格（卡片自己撑开格子）、横屏侧边（外面算好等高）、dock（外面算好等高）、pad 双表页（跟着内容走）。写死 `fillMaxSize()` 或写死不填，必有一半场景是坏的。

正解是把这件事变成参数，由**放它的人**回答：

```kotlin
// MetricGrid.kt：只有外面确实给了固定高度，才让卡片撑满
MetricSlotCell(
    fillCell = cellHeight != null,
    ...
)

// MetricSlotCell.kt
val fill = if (fillCell) Modifier.fillMaxSize() else Modifier
MetricCard(..., modifier = fill)
```

判断口诀：**格子高度是外面算出来的（同排要等高）→ 撑满；格子高度由卡片内容决定 → 见下面那半。**

#### 没人给高度的时候，同排怎么等高

上面那半只回答了「外面给了高度」的情况。`cellHeight == null` 时（跟着内容走的网格）还有一个坑：
**`Row` 默认不给子项任何高度意见。**

```kotlin
// ❌ Row 只有 zIndex，没有高度约束
Row(modifier = Modifier.zIndex(...)) { cells() }
```

于是每张卡各自按内容撑高。「18:42」是纯读数、「3.82 km」多一个上标单位，两者内容高度就差一截；Row 本身取最高那张的高度，**矮的那张不会被拉平**，底色只画到自己的高度——看起来就是同一排一高一矮。

```kotlin
// ✅ 行先用内在高度定出「这一排该多高」，卡片再撑满它
Row(
    modifier = Modifier
        .height(IntrinsicSize.Min)     // = 各子项最小内在高度的最大值
        .zIndex(...)
) { cells() }

// 卡片侧
val fill = if (fillCell) Modifier.fillMaxSize() else Modifier.fillMaxHeight()
```

**为什么这里撑高是安全的，而第 3 条那次会拉成长条**：那次 `fillMaxSize()` 吃的是父约束的**剩余整页**高度；有了 `IntrinsicSize.Min`，行高变成「最高那张卡的内容高度」，不再是剩余空间。同一个 `fillMaxHeight` 安全与否，取决于它上面那层给的是什么约束——又回到第 3 条那句话。

代价：`IntrinsicSize` 会多走一遍测量。几个格子的量级可以接受，长列表里要谨慎。

见 [MetricSlotCell.kt:112](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/MetricSlotCell.kt#L112)、[MetricGrid.kt:192](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/MetricGrid.kt#L192)。

### 2. `matchParentSize()` 不参与父容器测量

**症状**：整个 dock 消失了，一行都不剩。

`matchParentSize()` 只在**布局阶段**跟随父尺寸，它**不给父容器提供测量意见**。如果 Box 的所有子项都用它，父容器就没有任何子项告诉它「我要多大」——于是塌成 0×0，里面的东西一起消失。

```kotlin
// ❌ 子项全用 matchParentSize，Box 塌成 0
Box {
    Card(Modifier.matchParentSize())
}

// ✅ 至少有一个子项参与测量
Box {
    Card(Modifier.fillMaxSize())        // 或者让内容自己撑开
}
```

当初为什么会用它：想让编辑虚线严丝合缝贴住卡片边。这个诉求用 `fillMaxSize()` 一样能达到，代价小得多。

### 3. `fillMaxSize()` 会吃满父约束，父约束可能是「剩余整页」

**症状**：pad 双表页的卡片被拉成通天长条，第一行吃满整屏，后面所有行和仪表全被挤走。

这是第 2 条改错之后的连锁反应。`fillMaxSize()` 本身没问题，问题是它拿到的**父约束是「这一列剩下的全部高度」**——在 `Column` 里没有 `height()` / `weight()` 约束的子项，最大高度就是剩余空间。

在把某个 modifier 换成 `fillMaxSize()` 之前，先回答：**这个 composable 的父约束是谁给的、有多大？** 答不出来就不要写它。

### 4. Modifier 链有顺序，后面的约束可能已经无效

```kotlin
// ❌ heightIn 完全没作用：fillMaxHeight 已经把高度钉死成父约束的最大值
Modifier.fillMaxHeight().heightIn(max = 120.dp)

// ✅
Modifier.heightIn(max = 120.dp).fillMaxHeight()   // 或直接 .height(120.dp)
```

`Modifier` 是从左到右层层包裹的约束流。左边先把值定死了，右边再限制也改不动。

### 5. `Row` 先测量非 weight 子项，内部 `fillMaxWidth` 的子项会吃光整行

**症状**：底部调节条两侧的按键组整组消失。

`Row` 的测量顺序是：先量所有**没有** `weight` 的子项，剩下的宽度才按 weight 分。如果那个非 weight 子项内部是 `fillMaxWidth()`，它会把整行吃光，两侧 weight 分到 0 宽。

```kotlin
// ✅ 中间那段必须先定住宽度，再放进来
Box(Modifier.weight(1f)) { left() }
Box(Modifier.width(TUNE_PROGRESS_WIDTH)) { center() }   // 定宽，不给它吃光的机会
Box(Modifier.weight(1f)) { right() }
```

见 [TargetTuneBar.kt:80](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/TargetTuneBar.kt#L80)。

### 6. 尺寸下限不要用 `coerceAtLeast` 硬撑

**症状**：pad 页的仪表溢出行高，压在底部调节条和顶栏上面。

```kotlin
// ❌ 空间不够时硬撑一个写死的下限 → 溢出去压住别人
val side = minOf(maxWidth, maxHeight - STEPPER_BLOCK).coerceAtLeast(GAUGE_MIN_SIDE)

// ✅ 下限只保证不是负数，宁可画小一点，版面先站得住
val side = minOf(maxWidth, maxHeight - STEPPER_BLOCK).coerceIn(0.dp, maxHeight)
```

「表小一点」是能看出原因的问题，「溢出压住别的元件」是看不出原因的问题。后者调试成本高一个量级。另外负的 `Dp` 传给 `size()` 会直接抛 `IllegalArgumentException`，`coerceIn(0.dp, ...)` 同时挡掉这个。

见 [DualGaugeClusterScreen.kt:223](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/DualGaugeClusterScreen.kt#L223)、[RouteMapScreen.kt:165](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/RouteMapScreen.kt#L165)。

### 7. 等分高度要先扣掉间距

```kotlin
// ❌ 总高多出 (行数 − 1) 份间距，整栏比旁边高一截，把下面的东西往下顶
val cell = heroHeight / SIDE_ROWS

// ✅
val cell = (heroHeight - SIDE_CELL_SPACING * (SIDE_ROWS - 1)) / SIDE_ROWS
```

### 8. 密度档要按「算出来的实际尺寸」选，不能照抄隔壁区域的规格

**症状**：横屏侧边改成一栏四格后，读数被裁掉下半截。

原本侧边的密度是跟着 dock 的规格走的。但侧边是**一栏四格的窄高卡**（每格只剩 ~54dp），dock 是**一排宽扁卡**——同一个「规格」对两边意味着完全不同的可用空间。紧凑档的 17sp 读数要 56dp 才装得下。

```kotlin
// ✅ 按格子实际算出来的高度选档
private fun sideDensity(heroHeight: Dp, columns: Int): MetricCardDensity {
    val cell = sideCellHeight(heroHeight)
    return when {
        cell >= SIDE_REGULAR_MIN -> MetricCardDensity.REGULAR
        cell >= SIDE_COMPACT_MIN -> MetricCardDensity.COMPACT
        else -> MetricCardDensity.DENSE
    }
}
```

阈值要留余量（各档内容高度约 62 / 56 / 40dp，阈值给 76 / 62dp）。卡到刚好，见下一条。

### 9. 写死的 dp 阈值会被系统字体缩放击穿

用户机器上 `fontScale ≈ 1.3`。`sp` 尺寸会跟着放大，但你写死的 `dp` 阈值不会——于是「在我这儿刚好放得下」在真机上就裂开。

两个做法：阈值留 10~20% 余量；或者干脆用 `heightIn(min = )` 让内容自己顶出需要的高度，而不是拿阈值去猜。

### 10. 浮动栏要用 Box 叠加 + contentPadding，不能用 Column 分上下两段

**症状**：浮动 tab bar 看起来像被「垫」在页面下方，而不是浮在页面上。

用 `Column` 把页面分成「内容 + tab bar」两段的话，tab bar 会**占掉版面高度**，内容滚到底就停在它的上缘——那正是「垫着」的观感。

```kotlin
// ✅ Box 叠加，内容滚到 tab bar 底下去；用 contentPadding 保证最后一项露得出来
Box {
    LazyColumn(contentPadding = PaddingValues(bottom = TAB_BAR_BLOCK)) { ... }
    FloatingTabBar(Modifier.align(Alignment.BottomCenter))
}
```

`contentPadding` 的值要覆盖 tab bar 的高度加它的外边距，否则最后一项被永久遮住——「能滚到但看不全」和「滚不到」对使用者是同一件事。

见 [DynamicLightTabBarDemoScreen.kt:281](app/src/main/java/top/hasiy/liquidglassdemo/ui/DynamicLightTabBarDemoScreen.kt#L281)。

### 11. 末行不满时要补空位，否则剩下的格子会被撑大

**症状**：配色预览页最后一行的色块比前面几行宽一截。

`Row` + `weight` 的等分是按**这一行实际的子项数**算的。最后一行只有 2 个而上面每行 4 个，那 2 个就各拿一半宽。

```kotlin
// ✅ 补满这一行的空位，宽度才和上面几行一致
repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
```

这和状态矩阵的「空位要占位」（第 13 条）是同一件事的两个场合：**等分布局里，缺的格子必须有人占着。**

见 [ThemePalettePreviewScreen.kt:340](app/src/main/java/top/hasiy/liquidglassdemo/ui/ThemePalettePreviewScreen.kt#L340)。

---

## 二、空状态与编辑态

### 12. 空格子必须自己有尺寸，否则虚线画在 0×0 的框上

**症状**：编辑模式下虚线框全部消失，而且「隐藏掉的恰好是用户使用时的框」——即两件事同时错了。

`fillCell = false` 时空格子拿到的是空 modifier。有内容的卡片能靠内容撑开，**空格子里什么都没有**，于是 0×0，虚线画在零尺寸的框上等于没画。

```kotlin
// ✅ 空格子这一张永远撑满，不看 fillCell
EmptyMetricCard(config, modifier = Modifier.fillMaxSize(), editing = dragEnabled)
```

一般规律：**任何「靠内容撑开尺寸」的容器，都要单独考虑内容为空的那一版。**

### 13. 空位要占位，但不画框

用户原话：「空位仍该占一格位置，只是不画框。」

补位塌高度和补位不占位是两回事。空位参与布局（占一格、和同行等高），只是不绘制边框——这样同一行的格子才对得齐。

### 14. 编辑虚线的 inset 要按最小卡片算，不是按最大卡片算

**症状**：「编辑框的高度没有和 item 保持一致。」

`EDIT_OUTLINE_INSET` 原本是 3dp，上下共 6dp。在竖屏 100dp 高的卡上看不出来（6%），在横屏侧边 44dp 的卡上占了 **14%**，肉眼就是「虚线比 item 小一圈」。

改成 1dp。凡是「视觉上要贴合」的内缩量，都拿**最小的那个使用场景**去验，不是拿开发时最常看的那个。

### 15. 「整行没有数据源就不显示」= `rowIndex + 1`，不要多给一行

**症状**：用户连提两次「怎么还是没有判断整行没有数据源的时候不显示」。

```kotlin
// ✅ 最后一个有内容的格子在第 rowIndex 行 → 显示 rowIndex + 1 行
internal fun visibleRowCount(rows, lastFilledKey, showEmpty: Boolean = false): Int {
    if (showEmpty) return rows.size          // 编辑模式要摊开全部空位给人拖
    ...
    return rowIndex + 1
}
```

第一次没改对的真正原因不是算错，是**存在一个重复的等宽版重载**，我只改了其中一个。发现同名逻辑有两份时，先合并再改，不要两边各改一次。

---

## 三、排版与文字

### 16. 窄卡片里，标签换行会把数值挤出卡片被裁掉

**症状**：真机截图上「数字整个消失，标签断成两截」。

对比卡的文字栏只占卡宽 46%，两栏侧边再对半分之后不到卡宽四分之一。「峰值拉力」这种四字标签在这个宽度必然换行，换行后数值那一行被挤出卡片高度、被 `clip` 切掉。

```kotlin
// ✅ 标签和数值都锁单行、超出截断
Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
```

标签看不全是可接受的损失，**数值才是这张卡存在的理由**。取舍要按「这个元件是为了给什么」来定。

配套的两处收窄（改完标签仍会把三位小数的 `126.3` 挤掉）：文字栏 weight 从 0.42 提到 0.46、DENSE 档字级降到 12sp。见 [ComparisonMetricCard.kt:231](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/ComparisonMetricCard.kt#L231)。

### 17. 会换行的文案要按「折行后的高度」预留槽位

「已在其他位置显示」是六个字，在 ~120dp 宽的候选卡上必然折成两行。如果按一行预留，有徽标的那些卡就比没有的高一截，整列参差。

```kotlin
// ✅ 按两行预留，空的时候也占着这个位置
private const val BADGE_LINES = 2
Text(..., maxLines = BADGE_LINES)
```

用户的原始要求正是这个：「文字默认两行，不要影响到 item 高度。」

### 18. 行主序索引在换栏数时会重新对应

`index = row * columns + col`。同一个 key 在 2×4 和 2×5 下落在不同的视觉位置——这是行主序的固有性质，不是 bug。dock 从 2×4 切到 2×5 时部分格子「跳位」，属于预期行为，spec 里已有先例。要避免的话得改成显式坐标存储，代价是存档格式全变。

---

## 四、照抄设计稿（CSS / SVG → Compose）

### 19. 照抄任何数值前，先确认它成立的前提

这是本节所有坑的总纲。设计稿里的 `218°`、`1fr auto 1fr`、`xMidYMid meet` 都是在**某个坐标系或某套默认行为**下成立的，Compose 那套默认行为不一样。

### 20. CSS `conic-gradient` 从 12 点起算，Compose `drawArc` 从 3 点起算

差 90°。参考稿写 218°，代码要写 `218f - 90f`。

```kotlin
// spec 写的 218° 是从 12 点钟量的，Compose 的 0° 在 3 点钟，所以减 90
internal const val START_ANGLE_DEG = 218f - 90f
```

见 [WorkoutGauge.kt:427](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/WorkoutGauge.kt#L427)、[TargetGauge.kt:358](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/TargetGauge.kt#L358)。

### 21. SVG `preserveAspectRatio="xMidYMid meet"` = 等比缩放 + 居中留白

不是 `fillBounds`。取两轴比例的**较小**值，再把结果居中。

同一张图里两种元素可以用不同策略：赛道本体必须等比（拉伸就不是那条赛道了），示意用的折线可以跟着画布拉伸。见 [CircuitMap.kt:66](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/CircuitMap.kt#L66)。

### 22. `conic-gradient` + 遮罩在 Compose 里没有直接对应

参考稿常用「铺满整圆的锥形渐变 + 一个圆形遮罩挖掉中间」做环形。Compose 里没有 conic 渐变，硬模拟成本高。改成量出圆角/半径直接 `drawArc` 画环——但**内缩比例必须和遮罩的比例严格相等**，否则环的粗细就和稿子不一样。

### 23. 平滑曲线：Catmull-Rom 转三次贝塞尔

参考稿的曲线是一串采样点。直连是折线，`quadraticTo` 猜控制点会飘。用 Catmull-Rom 转贝塞尔（张力取 6），逐段给出控制点。见 `MetricArtwork.kt` 的 `smoothPath`。

---

## 五、颜色与 token

这一组基本都来自主题配色拆分阶段。共同特征是**症状看起来像「没生效」，而不是「报错」**——所以特别难查。

### 24. `Color(Long)` 期望的是 64 位打包格式，不是 ARGB

**症状**：颜色完全不对，或者变成诡异的荧光色。

Compose 的 `Color(Long)` 构造函数收的是**内部 64 位打包格式**（高位是分量、低 6 位是 color space id），不是 `0xAARRGGBB`。把 ARGB 直接塞进去，低 6 位会被当成 color space id 解读。

```kotlin
// ❌ 低 6 位被当成 color space id
Color(0xFF3B82F6)

// ✅ 走 Int 重载，或显式给 alpha
Color(0xFF3B82F6.toInt())
Color(0xFF3B82F6L shr 8 or 0xFF000000L)   // 视来源格式而定
```

存 token 时**存 `Long` 的 ARGB、取的时候显式转**，比让两种格式在代码里并存安全。见 [GlassPalette.kt:93](designsystem/src/main/java/top/hasiy/designsystem/GlassPalette.kt#L93)。

### 25. `Color(0L)` 是「透明黑」，不是「未指定」

**症状**：想用 0 当哨兵值表示「这个 token 没配」，结果回退链整条失效。

`Color(0L)` 是一个**合法的颜色**（全透明的黑），`isSpecified` 返回 `true`。所以拿 0 当「未设置」的判据是无效的。

```kotlin
// ❌ 0 是合法颜色，这个分支永远走不到
if (raw.isSpecified) return raw

// ✅ 用 0L 本身当哨兵，在转换前判断
if (raw == 0L) return fallback
```

见 [GlassPalette.kt:101](designsystem/src/main/java/top/hasiy/designsystem/GlassPalette.kt#L101)。

### 26. 用 `copy()` 覆盖单个 token 会被 palette 里的旧值悄悄盖掉

**症状**：改了颜色，界面没反应，看起来像「代码没编译进去」。

如果 `GlassConfig` 同时持有「palette 对象」和「单个 token 字段」，而读取时优先读 palette，那么 `config.copy(someColor = X)` 就毫无效果——palette 里的旧值赢了。

规矩：**同一个语意只能有一个来源**。要么全走 palette，要么全走字段；提供 `copy` 覆盖能力时，读取端必须让覆盖值优先。见 [GlassPalette.kt:116](designsystem/src/main/java/top/hasiy/designsystem/GlassPalette.kt#L116)。

### 27. token 回退链要集中在一处

同一个语意（比如「轨道色」）在不同元件里各写一套 `?: fallback`，结果就是同一个 token 在进度条上和滑块上长得不一样。

回退逻辑集中在 `GlassPalette.kt` 的扩展属性里，元件只读扩展属性、不读原始字段。见 [GlassPalette.kt:109](designsystem/src/main/java/top/hasiy/designsystem/GlassPalette.kt#L109)。

### 28. `Color.copy(alpha = x)` 是**设置**，不是相乘

```kotlin
// ❌ 以为是「再淡一半」，实际是把 alpha 直接设成 0.5——原本 0.2 的颜色反而变浓了
someTranslucentColor.copy(alpha = 0.5f)

// ✅ 要相乘就自己乘
someTranslucentColor.copy(alpha = someTranslucentColor.alpha * 0.5f)
```

### 29. 表面色 token 和文字级 token 不能混用

`sunk` / `lift` 这类是**表面**色（用于背景、轨道），`muted` 这类是**文字**级（用于次要文本）。把 `mutedContentColor` 拿去当背景、或把表面色拿去画文字，在某几个主题下会正好糊成一片——而在你开发时用的那个主题下看起来没问题。

同理还有「屏内 vs 屏外」：`lineColor` 是**屏内**的分隔线，深色主题下它自己就很暗，拿去画屏外的边框会看不见。见 [GlassPalette.kt:219](designsystem/src/main/java/top/hasiy/designsystem/GlassPalette.kt#L219)、[ThemePalettePreviewScreen.kt:431](app/src/main/java/top/hasiy/liquidglassdemo/ui/ThemePalettePreviewScreen.kt#L431)。

### 30. 「一律往白提亮」在浅色主题上是错的

**症状**：浅色中性主题下，本该有层次的表面糊成一片白。

深色表面上「往白提亮」能做出玻璃感，但浅色主题的表面本来就接近白——再提亮就没有对比了。对比方向要**按表面明暗决定**：深色表面往白提亮，浅色表面往黑压暗。

同类的还有柔光：唯一的浅色主题把柔光改成 `BlendMode.Screen` 压暗，而不是沿用提亮。见 [GlassConfig.kt:115](designsystem/src/main/java/top/hasiy/designsystem/GlassConfig.kt#L115)、[GlassPresets.kt:51](designsystem/src/main/java/top/hasiy/designsystem/GlassPresets.kt#L51)。

### 31. 写死的颜色会在另一个主题下变成突兀的色带

`#15151A` 在深色主题上是「比底色略深一点」，在浅色主题上就是**一条黑带**。

凡是「比周围深/浅一点」的诉求，都该表达成「相对当前表面色偏移」，而不是写一个绝对值。见 [GlassSegmentedTabBar.kt:74](designsystem/src/main/java/top/hasiy/designsystem/GlassSegmentedTabBar.kt#L74)。

### 32. 三层结构里，容器和控件同色就会糊在一起

浅色主题是「页面 / 容器（毛玻璃）/ 控件（近白实面）」三层。原本容器和控件都用 `baseColor`，结果两层分不开。

控件要走 `asControlSurface()` 变成近白实面，浮在容器之上。配套规则：**只有容器投影，控件不投影**（每个小控件都投影会让整页布满灰晕），而且**阴影扩散不要大过元件间隙**（否则相邻阴影叠加，间隙糊成一条灰带）。见 [GlassConfig.kt:280](designsystem/src/main/java/top/hasiy/designsystem/GlassConfig.kt#L280)、[GlassPresets.kt:82](designsystem/src/main/java/top/hasiy/designsystem/GlassPresets.kt#L82)。

### 33. 加新 token 不能改掉旧预设的观感

四组既有 SDK 预设没有 `track` token。接入新语意时如果让它们走新的默认值，等于**悄悄改了所有既有调用方的界面**。

做法：新 token 的回退值要让旧预设**退化成原本的样子**（两端同色即退化成实色填充、终点色未指定时与起点同色）。见 [GlassConfig.kt:191](designsystem/src/main/java/top/hasiy/designsystem/GlassConfig.kt#L191)、[GlassModifier.kt:305](designsystem/src/main/java/top/hasiy/designsystem/GlassModifier.kt#L305)。

### 34. 同一个量的两次取样要用同色系的深浅，不要换色相

对比卡的「本桨 / 上一桨」是同一个量的两次取样，用两个不相干的颜色会被读成两件不同的事。深浅分先后、亮的是现在。而基准线用**内容色**而不是强调色——它不是「又一个成绩」，是刻度；同色系会被读成第三次取样。

---

## 六、玻璃与绘制机制

### 35. 点击 modifier 必须排在 `glassSurface` 之后

`glassSurface` 内部含 `clip(shape)`。Material 的涟漪由点击 modifier 自己绘制，排在 `clip` 之前不会被裁，会以**直角矩形溢出圆角**。

```kotlin
Modifier.glassSurface(shape, config).clickable { }   // ✅
Modifier.clickable { }.glassSurface(shape, config)   // ❌ 涟漪溢出圆角
```

`selectable` / `toggleable` / `combinedClickable` 同理。见 [GlassModifier.kt:115](designsystem/src/main/java/top/hasiy/designsystem/GlassModifier.kt#L115)。

### 36. 要背景模糊的浮层必须放在 host 的 `overlay`，不是 `content`

放在 `content` 里它自己也是取样源的一部分，会**取样到自己**（正反馈，越来越糊）。见 [GlassBlurComponents.kt:129](designsystem/src/main/java/top/hasiy/designsystem/GlassBlurComponents.kt#L129)。

### 37. `Stroke` 是置中对齐的，外半部分会被 clip 裁掉

**症状**：描边看起来只有设定宽度的一半。

`Stroke` 以路径为中线向两侧各画一半。如果外面有 `clip`，外侧那一半就被裁掉了。要么把路径往内缩半个线宽，要么把线宽设成目标值的两倍。见 [GlassModifier.kt:391](designsystem/src/main/java/top/hasiy/designsystem/GlassModifier.kt#L391)。

### 38. 半透明容器的阴影要把形状内部裁掉

**症状**：半透明卡片中间透出一团灰。

投影是画在元件下方的，元件本身半透明就会**把自己的阴影透出来**。做法是把形状内部裁掉，只保留外侧那一圈。见 [GlassModifier.kt:231](designsystem/src/main/java/top/hasiy/designsystem/GlassModifier.kt#L231)。

### 39. 细长元件上的方向性效果要自动淡出

柔光半径按宽度算、中心固定在左上，在宽高比悬殊的元件（列表项、输入框、进度条轨道）上会被拉成一道**横跨整个宽度的横向渐变**，左亮右暗，看起来就是画错了。

`glassSurface` 按 `elongation`（长边 / 短边）渐进淡出：≤ 1.6 完全保留，≥ 2.5 完全关闭。淡出的不只是柔光，还有被拉长的垂直渐变和阴影。

### 40. 轨道是衬底，柔光与描边一律关掉

进度条轨道上再叠柔光和描边，只会让**填充段的边界糊掉**——而那个边界正是进度条要传达的信息。见 [GlassConfig.kt:193](designsystem/src/main/java/top/hasiy/designsystem/GlassConfig.kt#L193)。

### 41. Canvas 里的 `Path` / `PathMeasure` / `TextLayoutResult` 不要每帧重建

**症状**：不报错，但动画期间掉帧。

只依赖尺寸、不依赖动画值的几何（赛道路径、地形、固定文字的排版结果）在 draw lambda 里重建，等于每帧重算一次本可缓存的东西。

```kotlin
// ✅ 几何只在尺寸变化时重建，每帧只算依赖 progress 的那一段
Modifier.drawWithCache {
    val track = buildCircuitPath(scale, dx, dy)
    val measure = PathMeasure().apply { setPath(track, false) }
    onDrawBehind { /* 只用 measure 取 progress 对应的 segment */ }
}
```

`PathMeasure().setPath()` 尤其贵——它要算整条路径的长度。

### 42. `Float.coerceIn` 挡不住 NaN

**症状**：某个弧或某条线整个画不出来，没有任何报错。

`NaN < min` 和 `NaN > max` 都是 `false`，所以 `coerceIn` 直接把 `NaN` 原样返回。`value / max` 在 `max == 0f` 时得到 `NaN`，clamp 之后还是 `NaN`，传进 `drawArc` 就是「不画」。

```kotlin
// ❌ max 为 0 时 NaN 穿透
(current / max).coerceIn(0f, 1f)

// ✅ 保护分母，而不是保护结果
(current / max.coerceAtLeast(EPS)).coerceIn(0f, 1f)
```

**分母的保护要做在除法之前。**

---

## 七、开关与持久化的语义

### 43. 显示开关只改显示顺序，不要落成版面覆盖

「左对齐」开关的实现要点：

```kotlin
// ✅ 只在算 rows 之前重排显示顺序
val ordered = if (alignStart) compactSlots(slots) { state.metricAt(it.key) != null } else slots
```

落成显式的版面覆盖就等于**帮使用者重排了他的版面**，关掉开关也回不去了。凡是名字里带「显示 / 对齐 / 排序」的开关，都该是纯显示层的，可逆。

对应的测试要覆盖「开关不改变 layout 数据」这一条。

### 44. 存档格式的新字段一律追加在末尾 + `getOrNull` 退回默认

```kotlin
// 格式：竖屏规格|dock规格|k=v;k=v|侧边规格|左对齐
// 旧存档没有第 5 段，getOrNull 回传 null → 退回默认，不是崩溃
val alignStart = fields.getOrNull(4)?.toBooleanStrictOrNull() ?: false
```

插在中间会让所有旧存档整体错位。见 [WorkoutController.kt:301](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/WorkoutController.kt#L301)。

### 45. 菜单项数量受屏幕高度限制，按点击频率排序

手机横屏只有 ~359dp 高，一张下拉菜单放得下大约 **7 项**。加到 9 项之后，「拖动换位」被挤到屏幕外——菜单是可滚动的，但要先滚一段才看得到，实际等于藏起来了。

开关类（每次都可能点）排最上面，规格候选（选一次就不动）往下滚。

### 46. 补位塌陷要落成显式覆盖

`clearSlot` 之后把后面的格子往前挪，**每一格都要落成显式的覆盖值**。只挪不落成的话，下次重算会按默认顺序还原，用户看到格子自己跳回去。

注意这一条和第 43 条方向相反：**用户主动删掉一格**是对版面的编辑，要落盘；**打开一个显示开关**不是编辑，不该落盘。区别在于「关掉之后用户期待回到哪里」。

---

## 八、组件选择与 API 降级

### 47. 需要 token 的地方必须用 SDK 元件，不能手绘 Box

**症状**：自己拼的进度条在某些主题下轨道色不对。

轨道色走的是 `GlassPalette.kt` 的**回退链**（第 27 条）。手绘的 `Box` 拿不到这条链，只能读到原始字段，于是在没配 track token 的主题上就错。

「看起来只是一个圆角矩形」不等于可以自己画。见 [ThemePalettePreviewScreen.kt:257](app/src/main/java/top/hasiy/liquidglassdemo/ui/ThemePalettePreviewScreen.kt#L257)。

### 48. 浅底页面要显式把 `config` 传进去

组件默认读 `LocalGlassConfig`。如果某个页面自己换了底色却没把对应的 config 传给子组件，子组件还按默认（深色）算对比方向，边界就落错了。见 [DynamicLightTabBarShowcaseScreen.kt:222](app/src/main/java/top/hasiy/liquidglassdemo/ui/DynamicLightTabBarShowcaseScreen.kt#L222)。

### 49. `minSdk 26` 下 `RenderEffect` 类效果要有降级路径

`RenderEffect`（模糊）是 **API 31** 起才有的。项目 `minSdk = 26`，所以：

- SDK 内部自动降级为「不可穿字的磨砂底色」，而不是变成完全透明
- 状态矩阵里有些地方直接判断「alpha 已经把层次讲清楚了，就不做模糊」

降级要**降到一个仍然说得通的样子**，不是降到「什么都没有」。见 [GlassBlurComponents.kt:131](designsystem/src/main/java/top/hasiy/designsystem/GlassBlurComponents.kt#L131)、[ResponsiveStateMatrixScreen.kt:189](app/src/main/java/top/hasiy/liquidglassdemo/ui/workout/ResponsiveStateMatrixScreen.kt#L189)。

### 50. 自绘的「字形」要让读屏念原始文字

七段码是把字符拆成段来画的。无障碍要在**整体**上给原始文字，不要让读屏去逐段猜；目录外的字符（单位、中文）原样输出，不要吞掉。见 [SevenSegmentText.kt:52](designsystem/src/main/java/top/hasiy/designsystem/SevenSegmentText.kt#L52)。

---

## 九、无障碍语意

这一节全部来自同一轮修复。它们的共同点是：**编译过、测试过、看起来也对，但语意值是错的或者根本没生效**——而且大半天验不出来。

### 51. `semantics { }` 里的变量遮蔽

```kotlin
// ❌ 右边那个 selected 解析成 receiver 自己的属性，不是外层的 val
val selected = spec == current
Modifier.semantics { this.selected = selected }

// ✅ 外层换个名字
val isSelected = spec == current
Modifier.semantics { selected = isSelected }
```

`SemanticsPropertyReceiver` 自己就有一个 `selected` 属性，而 **receiver 的成员优先于外层局部变量**。所以那行代码是在拿自己的旧值赋给自己：编译得过、单元测试也过，但语意值恒错。真机 dump 出来八个菜单项 `isSelected` 全是 false 才看得出来。

凡是带 receiver 的 lambda（`semantics`、`drawBehind`、`graphicsLayer`、`constrainAs`），外层变量不要和 receiver 的属性同名。

### 52. 裸 `semantics { selected }` 挂在 Material 组件上可能不透出

症状：按标准写法加了 `selected` 语意，dump 里 `isSelected` 还是 false；`mergeDescendants = true` 也不管用。

项目里能正确透出选中态的地方走的都是 **`Modifier.selectable(...)`**——它除了设 `Selected` 还设了 `Role`，产生的节点结构和「把 `semantics` 挂在 `DropdownMenuItem` 的 modifier 上」不一样。要让菜单项透出，得让组件内部用 `selectable` 取代 `clickable`。

这里的判断不是技术问题：如果这条语意的收益只是「补一个读屏能听到的说法」，而视觉上已经有明确标记（行尾的 ✓ 已经把选中讲清楚了），那不值得为它改 SDK 的公开签名和交互行为。**标准写法留着，但明确记下「刻意不再往下追」**，别在代码里留一个假的待办。

### 53. uiautomator dump 验不到的无障碍字段

我在上面那条上花了六轮，才意识到自己用错了验证工具。dump 只导出固定几个字段：

| 改了**能**在 dump 看到 | 改了**看不到** |
|---|---|
| `contentDescription` → `content-desc` | `onClickLabel`、`customActions`（那是 AccessibilityAction 的 label，不是节点属性） |
| `selected` / `checked` / `clickable` | `liveRegion`、`stateDescription` |
| `progressBarRangeInfo`（节点类名变成 `android.widget.ProgressBar`） | `clearAndSetSemantics`（只体现为节点从树里消失） |

所以**「dump 里看不到」不等于「没生效」**。要验右列那些，得开 TalkBack 实听，或者写 instrumented test 用 Compose 的语意断言（`onNodeWithTag().assert(...)`）——后者可重复，值得为长期维护的无障碍行为写上。

---

## 十、验证方式

### 54. 布局改动在真机截图确认前，不要往下改第二轮

状态矩阵阶段连续两轮改坏（塌成 0 → 拉成长条），根因是「编译过了就当改对了，接着改下一处」。Compose 的布局问题在编译期完全看不出来，单元测试也测不到（纯函数测得到，测量结果测不到）。

流程：**改一处 → 装机 → 截图 → 确认 → 再改下一处。** 慢，但比返工快。

### 55. 颜色类改动要过一遍全部主题

第五节几乎每一条都是「在我开发用的那个主题下看起来没问题」。token 语义、对比方向、层次结构的改动，至少过一遍深色玻璃 + 浅色中性两种风格——它们的对比方向是相反的。

### 56. 用 `adb` 点界面之前，每步先 dump 或截图确认坐标

按上一张截图算出来的坐标去 `input tap`，界面稍有变化就会点到别的地方——本次误触到了用户的其他 app。

```bash
adb -s <serial> shell uiautomator dump /sdcard/w.xml && adb -s <serial> pull /sdcard/w.xml -
```

先 dump 拿到目标控件的 bounds，再 tap 那个 bounds 的中心。

### 57. 改多处代码用脚本时，写盘放最后 = 断言失败前面全丢

用 Python 脚本批量替换时，如果所有替换都在内存里做、最后才 `write`，中途任何一个 `assert` 失败就会让**前面已经成功的替换全部丢失**，得整轮重跑。

要么每步单独写盘，要么先备份原文件。另外：正则删除旧函数时，如果新旧两个函数的 KDoc 开头相同，会误删刚写好的新函数——删除类操作的匹配模式要包含结尾锚点，不要只靠开头。

### 58. bash heredoc 在这个环境不可靠，改用脚本文件

复杂内容（含引号、backtick、中文）走 heredoc 反复解析失败。稳的做法是先把内容写成一个 `.py` / `.sh` 文件，再执行它。

### 59. 构建环境

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" TEMP=./.build-tmp-local TMP=./.build-tmp-local ./gradlew :app:testDebugUnitTest
```

短路径的 `TEMP` / `TMP` 是必需的（Windows 路径长度限制）。详见 [BUILDING.md](BUILDING.md)。

---

## 三句话总结

整个项目的返工，基本都能归到这三句上：

> **一、写尺寸相关的 modifier 之前，先回答「这个约束是谁给的、有多大」。**
>
> **二、照抄设计稿数值之前，先回答「它在原本那套坐标系里成立的前提是什么」。**
>
> **三、用一个颜色 token 之前，先回答「它的语意是什么、在另一个主题下会变成什么」。**

三句的共同形状是一样的：**先确认前提，再写代码**。答不出来就先去查，不要先写一个看起来能跑的版本——因为这三类问题都不会在编译期报错，只会在某个主题、某个屏幕尺寸、某台真机上裂开。
