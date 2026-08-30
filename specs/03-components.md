# SPEC-03 · 组件库（glass-core/components）

所有组件基于 `GlassSurface`（SPEC-02）构建，视觉常量仅引用 `GlassTokens`（SPEC-01）。

## 1. GlassCard

- 容器组件：`GlassSurface(radius=radiusCard) { content }`
- 内边距 `cardPadding`，可选 `title` 槽

## 2. GlassButton（pill 文本按钮）

- 形状 `radiusPill`，高度 56dp，水平 padding 28dp
- 按压：`pressScale` 0.96 + `springLiquid` 回弹
- 文本 `textPrimary` 16sp

## 3. GlassIconButton（圆形图标按钮）

- 尺寸 `iconSize` 44dp（列表内）/ 56dp（独立），形状 `radiusIcon`
- 图标 tint `iconTint`；按压同 GlassButton

## 4. GlassProgress（确定进度条）

- 高度 28dp（加宽版），轨道 `trackFill` + rim 描边
- 填充 `progressFill` 水平渐变，端部 pill 圆角；进度端带 2dp 亮边（knob 高光）
- 动画：`animateFloatAsState(springLiquid)`

## 5. GlassSlider（可拖拽滑杆）

- 外观同 GlassProgress；交互：`pointerInput` 按住整条任意位置直接拖动/点按设置值（REQ-SLIDER-DRAG）
- 拖动时填充端轻微放大 1.05（液态反馈）

## 6. GlassIndeterminate（载入条）

- 高度 10dp 细轨道；亮段宽度 30%，沿轨道循环滑动（`InfiniteTransition`，easeInOut）
- 亮段为 `progressFill` 渐变 + 微 blur 边缘

## 7. GlassListItem（列表项）

- 高度 `listItemHeight`，形状 `radiusCard`，填充 `glassFillRaised` + 细 rim
- 布局：`[圆形图标底 44dp(glassFillRaised+rim)] 标题16sp/副标题13sp(textSecondary) … trailing`
- trailing 文本 `textSecondary` 15sp

## 8. GlassTabBar（底部液态 Tab 栏）

- 容器：`GlassSurface(radiusPill)`，高 `tabBarHeight`，底部 margin `tabBarBottomMargin`，水平 `pagePadding`
- 选中指示：`glassFillRaised` + rim 的 pill，宽度按 tab 数等分，位置用 `springLiquid` 滑动（液态吸入感）
- 文本：选中 `textPrimary`，未选中 `textSecondary`；切换时文本颜色 320ms 渐变
- 点击：指示 pill 先向点击方向"拉伸"再落位（scaleX 1.15 过渡，REQ-TAB-LIQUID）

## 9. 通用交互

- 所有可点击组件 `Modifier.combinedClickable` + 按压缩放，禁止水波纹（玻璃无水纹，REQ-NO-RIPPLE）
