# SPEC-01 · 设计 Tokens

所有组件唯一允许引用的视觉常量来源。单位：dp（模糊半径为渲染像素 f）。

## 1. 颜色（暗色 palette，对齐参考截图）

| Token | 值 | 用途 |
|---|---|---|
| `bgBase` | #0D0A14 | 页面底色（近黑紫） |
| `blobOrange` | #B4551E | 背景光斑·左上 |
| `blobMagenta` | #8E2A5A | 背景光斑·右 |
| `blobPurple` | #5A2E8E | 背景光斑·左下 |
| `blobTeal` | #1E6E78 | 背景光斑·右下 |
| `glassFill` | #FFFFFF @ 7% | 玻璃面基础填充 |
| `glassFillRaised` | #FFFFFF @ 11% | 抬升层（选中 pill、列表项） |
| `rimLight` | #FFFFFF @ 30% | 外描边（rim light） |
| `rimLightTop` | #FFFFFF @ 45% | 顶部加亮描边（光源方向） |
| `innerHighlight` | #FFFFFF @ 10% | 内侧顶部高光带 |
| `innerShadow` | #000000 @ 18% | 内侧底部暗带（厚度感） |
| `textPrimary` | #FFFFFF @ 92% | 主文本 |
| `textSecondary` | #FFFFFF @ 55% | 次文本 |
| `trackFill` | #FFFFFF @ 12% | 进度/滑杆轨道 |
| `progressFill` | 渐变 #FFFFFF@45% → #FFFFFF@80% | 进度条/滑杆填充 |
| `iconTint` | #FFFFFF @ 92% | 图标 |

亮色 palette（预留，本期不启用）：`glassFill=#FFFFFF@55%`、`rimLight=#FFFFFF@80%`、文本改黑。

## 2. 形状

| Token | 值 | 用途 |
|---|---|---|
| `radiusCardL` | 28dp | 大卡片 / 面板 |
| `radiusCard` | 20dp | 列表项 / 中卡片 |
| `radiusPill` | 999dp | 按钮 / tab pill / 进度条 |
| `radiusIcon` | 50% | 圆形图标底 |

## 3. 模糊与层级

| Token | 值 | 说明 |
|---|---|---|
| `blurStrong` | 48f | 背景光斑/离屏强模糊 |
| `blurMedium` | 24f | 玻璃面背景模糊 |
| `offscreenScale` | 0.25 | 离屏快照降采样比例 |
| `offscreenBlurRadius` | 25 | 快照模糊半径（随 scale 折算） |
| `snapshotThrottleMs` | 100 | 离屏重捕节流 |

## 4. 尺寸与间距

| Token | 值 |
|---|---|
| `strokeWidth` | 1dp（rim）/ 1.5dp（pill 强调） |
| `pagePadding` | 20dp |
| `cardPadding` | 16dp |
| `listItemHeight` | 76dp |
| `iconSize` | 44dp |
| `tabBarHeight` | 68dp |
| `tabBarBottomMargin` | 16dp |

## 5. 动效

| Token | 值 | 用途 |
|---|---|---|
| `springLiquid` | dampingRatio 0.6, stiffness 320 | pill 滑动 / 按压回弹 |
| `pressScale` | 0.96 | 按压缩放 |
| `animMedium` | 320ms | 页面切换 |
