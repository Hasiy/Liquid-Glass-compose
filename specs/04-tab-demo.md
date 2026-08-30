# SPEC-04 · Tab Demo（glass-demo）

1:1 复刻参考截图的暗色玻璃界面，三 tab 结构 + 底部液态 Tab 栏。

## 1. 全局

- 状态栏：透明，内容沉浸（`enableEdgeToEdge`，暗色图标关）
- 背景：`bgBase` + 四色光斑（位置：左上橙、右中品红、左下紫、右下青），光斑为径向渐变大圆 + `blurStrong`
- 内容整体垂直滚动（`verticalScroll`），底部 Tab 栏固定悬浮

## 2. Tab0 · 控件页（复刻截图上半 + 组件区）

自上而下：

1. 文本 `完成度 89%`（15sp textPrimary）
2. GlassProgress（0.89）
3. 文本 `音量条（按住整条即可直接调节）`
4. GlassSlider（初始 0.8）
5. 文本 `载入中...`
6. GlassIndeterminate
7. GlassCard「玻璃组件」：
   - GlassButton「送出」
   - 行：GlassIconButton(+) / GlassIconButton(♥，点击切换填充/描边 + 弹跳)
8. GlassCard「玻璃列表」：三条 GlassListItem
   - ⚙ 设置 / 一般 / 开启
   - 🔔 通知 / 声音与震动 / 开启
   - ⓘ 存储空间 / 可用空间 / 128 GB

## 3. Tab1 · 材质页

- 顶部行：`模糊策略` 标签 + 玻璃切换开关（RENDER_EFFECT ⇄ OFFSCREEN_CAPTURE，REQ-BLUR-SWITCH 的入口）
- 两张并排 GlassCard，各自使用一种策略（强制覆盖全局值），用于同屏对比
- 下方一段说明文本（textSecondary 13sp）：当前策略名称与原理一句话

## 4. Tab2 · 关于页

- GlassCard：项目名「Liquid-Glass Compose」、版本 0.1.0、spec 目录路径、技术基线（AGP/Kotlin/Compose 版本）
- GlassCard：SDD 说明（specs/ 为事实来源）

## 5. 交互验收

- tab 切换：内容 `AnimatedContent`（fade+slide 24dp，`animMedium`）；pill 液态滑动（SPEC-03 §8）
- 滑杆按住整条可拖；进度条 89% 静态
- 心形按钮点击有弹跳（scale 1→1.25→1，springLiquid）
- 策略切换后 Tab1 两张卡片分别实时/离屏模糊，滚动页面时离屏卡片按 100ms 节流重捕
