# FTMS Responsive State Matrix 完整复刻计划

## 1. 文档定位

本文档用于规划在当前 `Liquid-Glass-demo` Android Compose Demo 中，完整复刻 FTMS Responsive State Matrix HTML 的视觉与交互。

参考文件：

```text
D:\Github\FTMS\docs\ui-designs\02-运动控制-workout-controls\02-响应式状态矩阵-Responsive-State-Matrix.html
```

本阶段只做 UI Demo 复刻，不接入 FTMS 真机、BLE、WorkoutSession 或真实设备能力。设备数据、控制写入和连接状态先使用可重复触发的模拟状态。

## 2. 当前基线

当前项目包含三个 Gradle 模块：

```text
:app                 示例应用与交互展示页
:designsystem        可复用 Glass/Material3 Compose 组件
:designsystem-tokens 跨平台主题规格 token
```

当前已经具备：

- `Drop`、`Neutral`、`Dark`、`Native` 四种视觉预设。
- Neutral 的浅灰背景、近白容器、黑色内容、Multiply 浅色压暗效果。
- `accentColor` 对选中态、进度条、滑杆和音量条的部分支持。
- 动态 Liquid Glass Tab Bar、Lens 折射模式、Legacy 对照模式。
- 通用 Glass Card、Button、Dialog、Popup、Slider、Navigation、Selection 等组件。

当前需要注意：

- 工作区存在未提交的 Tab Bar 与展示页改动，实施前必须保留并建立基线记录。
- 当前主题色候选值与 HTML 的主题色不完全一致。
- 当前 App 主要使用 `GlassPresets.themed()` 与 `LiquidGlassDemoTheme`，尚未将完整 `GlassThemeSpec` 作为唯一主题入口。
- `Digital` 与 `Tactile` 属于独立材质/布局风格，不能只按换色处理。

## 3. 复刻目标

### 3.1 主题范围

完整支持 HTML 中的 8 个主题：

| 主题 ID | 显示名称 | 类型 |
|---|---|---|
| `lime` | 荧光绿 | Liquid Glass 色彩主题 |
| `sky` | 品牌天蓝 | Liquid Glass 色彩主题 |
| `nordic` | 北欧雾绿 | Neutral 浅色风格 + 绿色语义强调 |
| `ocean` | 冰川蓝 | Liquid Glass 色彩主题 |
| `ember` | 熔岩橙 | Liquid Glass 色彩主题 |
| `aurora` | 极光紫 | Liquid Glass 色彩主题 |
| `digital` | 数码白 Digital | 独立数字仪表风格 |
| `tactile` | 实体按键 Tactile | 独立实体按键风格 |

### 3.2 UI 与交互范围

- 竖屏默认状态：2 行 × 3 列。
- 竖屏阻力调节状态：卡片原位展开，底部控制浮层。
- 横屏默认状态：2 行 × 4 列。
- 横屏目标速度调节状态：保持顶部会话操作，侧边调节浮层。
- 断连/重连状态：保留最后数值，调节入口禁用。
- 设备不支持调节状态：显示只读提示。
- 暂停状态：计时和实时数据冻结，提供继续和滑动结束。
- 控制写入中、失败、设备拒绝三种 Toast 反馈。
- 第二组指标：竖屏 2 × 3、横屏 2 × 4，并支持趋势图。
- 指标卡长按编辑：约 500ms 触发，支持描边进度反馈。
- 指标选择抽屉：实时、平均、最大/总数值三类。
- 竖屏底部抽屉与横屏左右侧抽屉。
- 抽屉拖动关闭、点击外部关闭、返回键/Escape、焦点管理和滚动指示器。
- 指标布局保存、恢复默认和页面重入恢复。
- Tab Bar 普通模式、Legacy 模式、Lens 折射模式。

## 4. 核心架构调整

### 4.1 分离视觉风格与主题色

当前 `GlassVisualStyle` 与强调色逻辑需要拆成两层：

```text
VisualStyle
├── DROP
├── NEUTRAL
├── DIGITAL
└── TACTILE

ThemePalette
├── LIME
├── SKY
├── NORDIC
├── OCEAN
├── EMBER
└── AURORA
```

目标是支持组合：

```text
Neutral + Nordic
Neutral + Sky
Digital + Ember
Tactile + Lime
```

### 4.2 补齐主题 token

每组主题至少需要：

- `accent`
- `accentLight`
- `accentDeep`
- `onAccent`
- `ambient`
- `track`
- `danger`
- `background`
- `screen`
- `panel`
- `line`
- `muted`
- `lift`
- `sunk`
- `glass`
- `glassDeep`

HTML 的基础主题色应保持原值：

| 主题 | 主强调色 | 浅色变体 | 环境色 | 强调色上的文字 |
|---|---|---|---|---|
| 荧光绿 | `#DFFF32` | `#EFFF9A` | `#69E2CE` | `#151800` |
| 品牌天蓝 | `#5AA9EE` | `#C4E2FB` | `#6FD3E8` | `#06202F` |
| 北欧雾绿 | `#079D68` | `#74D4AC` | `#72C7A9` | `#FFFFFF` |
| 冰川蓝 | `#43D9FF` | `#B8F2FF` | `#5B7EFF` | `#041E29` |
| 熔岩橙 | `#FF9A3D` | `#FFD0A3` | `#FF635B` | `#2A1400` |
| 极光紫 | `#B78CFF` | `#DECAFF` | `#45E2BE` | `#1A0F2E` |

Digital/Tactile 还需要保留 HTML 中各自的背景、屏幕、面板、文字、危险态和轨道色，不与普通 Liquid Glass 主题共用全部结构参数。

## 5. 分阶段实施安排

### 阶段 0：基线与规格冻结

预计：0.5–1 天

工作内容：

- 保存当前 `git status`、diff 和现有 Tab Bar 改动边界。
- 建立 HTML 元素到 Compose 组件的对照表。
- 标记所有 CSS 主题 token、状态、交互和响应式规则。
- 确认模拟数据、状态切换和不接真实设备的范围。
- 处理 Gradle 测试环境问题；当前 `test` 曾因无法建立 loopback connection 失败。

交付物：

- UI parity matrix。
- 主题 token 映射表。
- 状态与交互清单。

### 阶段 1：主题基础设施

预计：1–1.5 天

工作内容：

- 新增独立主题色 Palette 模型。
- 将 6 组 Liquid Glass 主题色写入 token/resource。
- 将 `GlassThemeSpec`、`GlassConfig` 和 Material3 `ColorScheme` 统一接入 Palette。
- 补齐 `onAccent`、`accentLight`、`accentDeep`、`ambient`、`track`、`danger` 等字段。
- 删除或替换主题相关的固定颜色。
- 主题选择使用稳定 ID，并支持保存和恢复。

阶段验收：

- 8 个主题可以被选择。
- 切换主题后基础背景、文字、强调色、轨道色和危险色同时变化。
- Neutral 的浅色表面逻辑不被其他主题破坏。

### 阶段 2：通用组件主题贯穿

预计：1–1.5 天

工作内容：

- CTA、暂停/继续、Send 等按钮使用 `accent` 和 `onAccent`。
- Navigation、Chip、Checkbox、Radio、Switch 使用主题选中态。
- Progress、Slider、VolumeSlider 使用 `accentLight → accent`。
- Connection/Live 状态使用 `accentDeep`。
- 错误、断线和拒绝状态使用 `danger`。
- 轨道使用 `track`。
- Dialog、Popup、Drawer、BottomSheet、Snackbar 使用对应 surface token。
- 修正固定白色勾选标记和其他固定前景色。

阶段验收：

- 任意一组主题色切换后，SDK 目录内所有组件无残留固定主题色。
- 浅色主题和高亮主题的文字对比度满足可读性要求。

### 阶段 3：Responsive State Matrix 主画面

预计：2–2.5 天

工作内容：

- 新增响应式主画面，不再把 HTML 的每个场景写成独立硬编码页面。
- 使用 `BoxWithConstraints` 或 Window Size Class 处理横竖屏。
- 建立可复用设备外框、屏幕、仪表环、指标卡、设备信息和底部操作组件。
- 完成竖屏 2×3 和横屏 2×4 指标布局。
- 完成仪表盘、状态栏、目标值、设备名称、指标单位和调整标记。
- 支持第二组指标和趋势图。

建议模型：

```text
ResponsiveStateMatrixScreen
├── WorkoutDeviceFrame
├── WorkoutHeader
├── WorkoutGauge
├── MetricGrid
├── SessionActions
└── RuntimeStateOverlay
```

### 阶段 4：运行状态与控制交互

预计：2–2.5 天

工作内容：

- 建立 `WorkoutRuntimeState`：ACTIVE、PAUSED、DISCONNECTED、RECONNECTING、ENDED。
- 建立 `ControlWriteState`：IDLE、WRITING、FAILED、REJECTED。
- 建立设备能力模型，区分可调节指标和只读指标。
- 实现阻力/速度的粗调滑杆与 ± 精调。
- 实现写入中、失败、设备拒绝 Toast。
- 实现断连时禁用调节并保留最后值。
- 实现暂停时冻结计时和实时指标。
- 实现继续和滑动结束确认。

阶段验收：

- 每种状态都可重复进入和退出。
- 不允许在断连、只读、暂停等状态下误触发调节。
- 滑动结束未达到阈值时不会结束会话。

### 阶段 5：指标编辑抽屉

预计：1.5–2 天

工作内容：

- 长按指标卡约 500ms 触发编辑。
- 长按过程中显示时间描边和按压反馈。
- 根据屏幕方向选择底部抽屉或侧边抽屉。
- 实时、平均、最大/总数值分类切换。
- 已在其他位置使用的指标显示占用提示。
- 实现拖动关闭、外部点击、返回键和焦点恢复。
- 实现滚动指示器和恢复默认布局。
- 使用稳定 key 管理指标槽位，避免布局调整导致状态错位。
- 保存布局并在页面重入时恢复。

### 阶段 6：Digital 与 Tactile 专属风格

预计：1.5–2 天

工作内容：

- Digital：浅色数字仪表、分段数字、低圆角、红绿状态点、简化刻度。
- Tactile：深色实体按键、内阴影、机械按钮、实体滑杆和按压反馈。
- 将两种风格的仪表、卡片、按钮、Slider、Toast 和状态层分别抽成 renderer。
- 只共享状态模型和布局契约，不强行共享不兼容的材质参数。

### 阶段 7：视觉与交互验收

预计：2–3 天

工作内容：

- 8 个主题分别验证默认态、调节态、暂停态和断连态。
- 横屏、竖屏分别截图与 HTML 对照。
- 验证指标编辑抽屉的方向、动画和焦点行为。
- 验证所有主题的文字对比度。
- 增加主题 token、状态转换、指标布局和结束滑动的单元测试。
- 在 Android 模拟器上验证 API 26 回退、API 33+ AGSL/Lens 效果。

## 6. 预计排期

按 1 名 Android 工程师估算：

| 时间 | 目标 |
|---|---|
| 第 1 天 | 基线、HTML 对照矩阵、主题 token 规格冻结 |
| 第 2–3 天 | Palette、GlassConfig、Material3 主题接入 |
| 第 4 天 | 通用组件主题贯穿 |
| 第 5–6 天 | 响应式主画面与指标布局 |
| 第 7–8 天 | 运行状态、控制交互、Toast、暂停/结束 |
| 第 9 天 | 指标编辑抽屉与布局持久化 |
| 第 10–11 天 | Digital/Tactile 专属风格 |
| 第 12–13 天 | 截图对照、模拟器验证、测试和缺陷修正 |

总工期预计：10–13 个工作日。

## 7. 预计变更文件边界

### `designsystem-tokens`

- 新增主题色 Palette 定义。
- 扩充 `GlassThemeSpec`。
- 增加主题 token 单元测试。

### `designsystem`

- 扩充 `GlassConfig` 和派生状态配置。
- 更新 `GlassPresets` 与 Material3 颜色映射。
- 统一组件对 `accent`、`onAccent`、`danger` 和 `track` 的使用。
- 清理固定主题色。

### `app`

- 新增主题选择与持久化状态。
- 新增 Responsive State Matrix UI 状态模型和模拟控制器。
- 新增主画面、指标卡、仪表、状态层、抽屉和趋势图组件。
- 将现有 Tab Bar 接入统一 Palette。
- 保留现有 Legacy/新 Tab Bar 对照能力。

## 8. 最终验收标准

- 8 个 HTML 主题均可选择，并且名称、色值和语义一致。
- 主题切换会同步影响页面、仪表、指标、按钮、滑杆、Tab Bar、Toast 和所有弹层。
- Neutral、Digital、Tactile 的视觉差异清晰可辨。
- 竖屏 2×3、横屏 2×4 与 HTML 的响应式结构一致。
- HTML 中的所有运行状态均可在 Demo 中进入。
- 指标长按编辑、分类切换、抽屉关闭、布局保存和恢复默认均可用。
- 暂停冻结数据，断连禁用调节，只读设备显示只读，控制失败会回弹并提示。
- 滑动结束必须达到完成阈值。
- API 26 有可接受的降级效果，API 33+ 启用 Lens/AGSL 效果。
- 主题相关固定颜色清理完成，保留的固定颜色必须有结构性说明。
- 单元测试通过，关键页面完成截图对照。
- 不覆盖当前工作区已有的未提交改动。

## 9. 风险与前置决策

1. **Lufga 字体授权**：如果仓库没有可用字体文件，需要使用 Outfit、Poppins 或系统字体回退，字体像素级一致性会受影响。
2. **Digital/Tactile 范围**：这两种风格需要独立 renderer，若只换色会无法达到 HTML 复刻标准。
3. **Lens/AGSL 平台差异**：API 26–32 需要降级，必须以“结构和交互一致、效果可接受”为验收口径。
4. **当前 dirty worktree**：实施前只允许增加受控文件，不使用 `git add .`、`reset` 或覆盖已有改动。
5. **真实设备接入边界**：本计划只做 UI 模拟状态；接入 FTMS runtime 应另立任务，避免把设计复刻和业务协议实现混在一起。

