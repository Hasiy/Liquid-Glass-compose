# FTMS Responsive State Matrix 复刻规格

阶段 0 的交付物：HTML 元素到 Compose 的对照矩阵、主题 token 映射表、状态与交互清单。

参考文件：

```text
D:\Github\FTMS\docs\ui-designs\02-运动控制-workout-controls\02-响应式状态矩阵-Responsive-State-Matrix.html
```

实施计划见 [responsive-state-matrix-replication-plan.md](responsive-state-matrix-replication-plan.md)。

---

## 1. 主题 token 映射表

CSS 自定义属性到 `ThemePalette` 字段的对应关系。`ThemePalette` 位于
`designsystem-tokens`，颜色以无符号 ARGB `Long` 保存。

| CSS 变量 | `ThemePalette` 字段 | 说明 |
|---|---|---|
| `--lime` | `accent` | 主强调色。变量名沿用基准主题，实际是「当前主题的强调色」 |
| `--accent-light` | `accentLight` | 填充段渐层起点 |
| `--accent-deep` | `accentDeep` | 压在浅底上的小字；深色主题与 `accent` 同值 |
| `--on-accent` | `onAccent` | 压在强调色上的文字色 |
| `--ambient-rgb` | `ambient` | 背景第二个径向渐层 |
| `--track` | `track` | 仪表环未到达量程、滑杆未填充段 |
| `--danger` | `danger` | 断连、写入失败、设备拒绝 |
| `--bg` | `background` | 画布底（设备外框之外） |
| `--screen` | `screen` | 设备屏幕底 |
| `--panel` | `panel` | 比屏底高一阶的容器 |
| `--line` | `line` | 分隔线与卡片描边（含 alpha） |
| `--muted` | `muted` | 次级文字（指标标签、单位、说明） |
| `--ink` | `ink` | 画布上的主文字色 |
| `--white` | `screenContent` | 屏内主文字色。原变量名误导，实际随主题反转 |
| `--lift` | `lift` | 卡片表面（比 panel 亮一阶） |
| `--sunk` | `sunk` | 底部 dock 与内凹容器 |
| `--glass` | `glass` | 调节浮层、Toast |
| `--glass-deep` | `glassDeep` | 抽屉、对话框 |

### 1.1 派生与取值依据

HTML 只在 `theme-nordic` 里完整定义了 `--lift` / `--sunk` / `--glass` /
`--glass-deep`；其余主题这些值是从实际渲染色反推的：

| 主题 | token | 取值 | 依据 |
|---|---|---|---|
| 深色 5 组 | `lift` | `#1D2121` | `.p-focus` 的 `rgba(29,33,33,.97)` |
| 深色 5 组 | `sunk` | `#151919` | `.p-bottom button` 的底色 |
| 深色 5 组 | `glassDeep` | `#161A19` | `.metric-dialog` 的底色 |
| Digital | `lift` | `#DBEBEEEA` | `.metric` 的 `rgba(235,238,234,.86)` |
| Digital | `sunk` | `#F0E4E8E4` | `.l-dock` 的 `rgba(228,232,228,.94)` |
| Digital | `track` | `#D7DAD7` | 点环未到达色。HTML 未覆盖 `--track`，级联下来的深灰在这套浅色仪表里不会出现 |
| Tactile | `lift` / `sunk` | `#3B3C3D` / `#1B1C1D` | 卡片三段斜向渐层的两端 |
| Tactile | `glass` / `glassDeep` | `#363839` / `#1C1D1E` | `.metric-dialog` 渐层的两端 |

### 1.2 isLight 的判断依据

`ThemePalette.isLight` 判断的是**屏内表面**，不是画布：

| 主题 | 画布 | 屏内 | `isLight` |
|---|---|---|---|
| Lime / Sky / Ocean / Ember / Aurora | 浅灰 `#EDF0E9` | 深 `#070909` | `false` |
| Nordic | 中灰 `#ABABAB` | 浅 `#D1D1D1` | `true` |
| Digital | 浅灰 `#E7E9E7` | 近白 `#F4F5F2` | `true` |
| Tactile | 浅塑胶 `#DEDFDD` | 深 `#191A1B` | `false` |

按画布判断会让 Tactile 屏内的柔光方向反掉（浅色表面用 `Screen` 提亮无效）。

### 1.3 两个明暗判断不能混用

`ThemePalette` 提供两个明暗判断，作用于不同的表面：

| 属性 | 判断的表面 | 用在哪 |
|---|---|---|
| `isLight` | 设备屏幕内（`screen`） | 屏内的柔光方向、屏内文字色 |
| `isCanvasLight` | 画布（`background`） | 系统栏图标明暗、画布上的文字与描边 |

Tactile 两者相反（画布浅、屏内深），拿错一个就会在近白的画布上画白色图标。
`isCanvasLight` 按「黑字与白字哪边对比度更高」判断，不是拿亮度跟 0.5 比——
Nordic 的画布 `#ABABAB` 亮度约 0.40，按 0.5 会误判成深色，但它对黑字 8.9:1、
对白字只有 2.4:1。

对应地，designsystem 层的语义色也分两套：

| 屏内 | 画布 |
|---|---|
| `screenContentColor` | `inkColor` |
| `mutedContentColor` | 由 `inkColor` 淡化 |
| `lineColor` | `canvasLineColor` |
| `isLightSurface` | `isCanvasLight` |

### 1.4 描边色属于视觉结构，不属于配色

`GlassThemeSpec.fromPalette` 的描边色按 `GlassVisualStyle` 取，不按明暗推：

| 结构 | 描边色 |
|---|---|
| DROP / DARK / NATIVE | 白（深色表面往白） |
| NEUTRAL | 黑（浅色表面往黑） |
| DIGITAL | `#0C110F` |
| TACTILE | `#080909` |

TACTILE 是深色表面却用近黑描边——那是胶帽之间的机械切边。渲染层的描边与
内缘阴影共用同一个颜色，按明暗推会给每个胶帽刷上 74% 白框加一层白色内发光，
把凸起感反掉。

### 1.5 强调色只有一个真相源

`GlassConfig.accentColor` 是强调色的唯一来源，`GlassPalette` 刻意不持有 `accent`。
两边各存一份的话，`copy(accentColor = ...)` 这个既有的覆写手段会被 palette 里的
旧值悄悄盖掉。要连带换掉浅／深变体请用 `GlassConfig.withAccent(color)`。

### 1.6 对比度实测与已知偏差

阶段 2 收尾时按 WCAG 2.1 实测了 8 组配色的关键前景／背景组合。**深色六组全部通过**，
两个浅色主题有以下偏差：

| 主题 | 组合 | 实测 | 要求 |
|---|---|---|---|
| Nordic | `onAccent` on `accent`（CTA 文字） | 3.48 | 4.5（大文字 3.0） |
| Nordic | `accentDeep` on `screen`（连接状态） | 4.16 | 4.5 |
| Nordic | `danger` on `screen` | 3.35 | 4.5 |
| Nordic | `accent` vs `track`（填充段与轨道） | **1.61** | 3.0 |
| Digital | `onAccent` on `accent`（CTA 文字） | 3.55 | 4.5（大文字 3.0） |
| Digital | `accentDeep` on `screen` | 3.24 | 4.5 |
| Digital | `muted` on `screen`（次级文字） | 4.02 | 4.5 |
| Digital | `danger` on `screen` | 3.24 | 4.5 |
| Digital | `accent` vs `track` | **2.52** | 3.0 |

**这些是参考稿本身的取值，不是实现引入的**，因此按原值保留（决策于 2026-09-08）。
CTA 那两项若按大文字标准（≥18sp，或 ≥14sp 粗体）算是达标的；`muted` 与 `danger`
是小字，确实不过 AA。

最影响可用性的是 **Nordic 的填充段与轨道只有 1.61:1** —— 进度条走到哪儿几乎看不出来。

有意思的是设计者知道这类问题：参考稿注释写过「压白字的绿要够深：`#0a6d4b`
对白字 6.4:1，原 `#087a55` 只有 4.3:1，小字不过 AA」，`accentDeep` 就是为此定义的。
但 CTA 走的是 `accent` 而非 `accentDeep`，所以白字仍只有 3.48。

**留给阶段 7**：这几项要跟设计确认是照原样发布，还是在浅色主题上偏离设计稿修对比度。
不要在实现层偷偷改数值。

### 1.7 视觉结构与配色的组合

`GlassVisualStyle` 只描述材质结构，`ThemePalette` 只描述配色：

| 配色 | 默认结构 |
|---|---|
| Lime / Sky / Ocean / Ember / Aurora | `DROP` |
| Nordic | `NEUTRAL` |
| Digital | `DIGITAL` |
| Tactile | `TACTILE` |

`Digital` 与 `Tactile` 是新增的结构，不共享 Liquid Glass 的材质参数——
Digital 的卡片是 `box-shadow: none` 的实色面，Tactile 是内凹底座加凸起胶帽。

---

## 2. UI parity matrix

### 2.1 竖屏（2 行 × 3 列）

| HTML 元素 | 角色 | Compose 对应 | 阶段 |
|---|---|---|---|
| `.phone` / `.phone-screen` | 设备外框与屏幕 | `WorkoutDeviceFrame` | 3 |
| `.p-island` / `.p-status` | 灵动岛与状态栏 | `WorkoutDeviceFrame` 内部 | 3 |
| `.p-top` / `.p-title` | 设备名与连接状态 | `WorkoutHeader` | 3 |
| `.p-top-actions .pause` | 暂停按钮 | `SessionActions` | 4 |
| `.slide-end` | 右滑结束 | `SlideToEndControl` | 4 |
| `.p-session` / `.live` | 运动时长与记录状态 | `WorkoutHeader` | 3 |
| `.p-gauge` / `.p-ticks` / `.gauge-scale` | 仪表环、刻度、数字标注 | `WorkoutGauge` | 3 |
| `.gauge-copy` | 仪表中心读数 | `WorkoutGauge` | 3 |
| `.phase-mini` | 阶段迷你条 | `WorkoutGauge` | 3 |
| `.p-metrics .metric` | 指标卡 2×3 | `MetricGrid` / `MetricCard` | 3 |
| `.metric.adjust` / `.mini-step` | 可调节标记 | `MetricCard` | 4 |
| `.metric.control-disabled` | 只读指标 | `MetricCard` | 4 |
| `.p-bottom button` | 更多指标（换组） | `MetricGroupSwitcher` | 3 |
| `.p-focus` | 竖屏调节浮层 | `ControlFocusPanel` | 4 |
| `.focus-read` / `.focus-range` / `.slider` / `.focus-stepper` | 读数、量程、粗调、精调 | `ControlFocusPanel` | 4 |
| `.control-toast` | 写入结果 Toast | `ControlWriteToast` | 4 |

### 2.2 横屏（2 行 × 4 列）

| HTML 元素 | 角色 | Compose 对应 | 阶段 |
|---|---|---|---|
| `.land` / `.land-screen` | 横屏设备与屏幕 | `WorkoutDeviceFrame` | 3 |
| `.l-top` / `.l-meta` | 顶部会话信息 | `WorkoutHeader` | 3 |
| `.l-actions` | 顶部会话操作（暂停 + 滑动结束） | `SessionActions` | 4 |
| `.l-main` / `.l-hero` | 中央 hero 仪表 | `WorkoutGauge` | 3 |
| `.l-side-stack .l-card` | 侧边 4 个数据卡 | `MetricGrid` 侧栏槽位 | 3 |
| `.l-duration` | 时长卡 | `MetricCard` art=duration | 3 |
| `.l-heart` / `.pulse-wave` | 心率卡与脉搏波 | `MetricCard` art=heart | 3 |
| `.l-power` / `.l-power-bars` | 功率卡与条形 | `MetricCard` art=power | 3 |
| `.l-slope` / `.slope-graphic` | 坡度卡与坡形 | `MetricCard` art=slope | 3 |
| `.l-dock .metric` | 底部 dock 2×4 | `MetricGrid` | 3 |
| `.l-focus` | 横屏调节浮层 | `ControlFocusPanel` | 4 |

### 2.3 运行状态与第二组

| HTML 元素 | 角色 | Compose 对应 | 阶段 |
|---|---|---|---|
| `.scenario-screen.disconnected` | 断连中 | `RuntimeStateOverlay` | 4 |
| `.reconnect-note` | 保留最后值提示 | `RuntimeStateOverlay` | 4 |
| `.scenario-metric.disabled` / `.read-only` | 只读指标 | `MetricCard` | 4 |
| `.pause-layer` / `.pause-actions` | 暂停浮层 | `RuntimeStateOverlay` | 4 |
| `.feedback-toast` / `.failed` / `.rejected` | 三种写入反馈 | `ControlWriteToast` | 4 |
| `.group-grid.portrait` / `.landscape` | 第 2 组 2×3 / 2×4 | `MetricGrid` | 3 |
| `.metric-trend` | 趋势图 | `MetricTrendChart` | 3 |
| `.group-nav` | 组切换指示 | `MetricGroupSwitcher` | 3 |

### 2.4 指标编辑抽屉

| HTML 元素 | 角色 | Compose 对应 | 阶段 |
|---|---|---|---|
| `[data-slot-key].pressing` + `hold-progress` | 长按 500ms 描边进度 | `MetricSlot` 长按手势 | 5 |
| `.metric-picker.picker-portrait` | 竖屏底部抽屉 | `MetricPickerSheet` | 5 |
| `.metric-picker.picker-landscape` + `.drawer-left/right` | 横屏侧边抽屉 | `MetricPickerDrawer` | 5 |
| `.metric-handle` | 拖动关闭把手 | 抽屉拖动手势 | 5 |
| `.metric-categories` | 实时 / 平均 / 最大·总数值 | `MetricCategoryTabs` | 5 |
| `.metric-choice` / `.used-badge` | 候选项与占用提示 | `MetricChoiceItem` | 5 |
| `.metric-scroll-indicator` | 滚动指示器 | `MetricPicker` 内部 | 5 |
| `.metric-reset` | 恢复默认布局 | `MetricPicker` 页脚 | 5 |

### 2.5 Digital / Tactile 专属绘制

| HTML 元素 | 角色 | Compose 对应 | 阶段 |
|---|---|---|---|
| `.seven-char` / `.seg-a`…`.seg-g` | 七段数码管逐段绘制 | `SevenSegmentText` | 6 |
| `.digital-dot-ring` | 点环进度（52 点，第 43 点为当前值） | `DigitalGaugeRenderer` | 6 |
| `.digital-energy-bars` | 7 根能量刻度 | `DigitalGaugeRenderer` | 6 |
| `.theme-tactile .metric` 内外阴影 | 凸起胶帽 | `TactileSurfaceRenderer` | 6 |
| `.theme-tactile .metric.adjust::before` | 绿色指示灯 | `TactileSurfaceRenderer` | 6 |

---

## 3. 状态与交互清单

### 3.1 运行状态 `WorkoutRuntimeState`

| 状态 | 表现 | 调节入口 |
|---|---|---|
| `ACTIVE` | 计时走、实时指标更新 | 可用 |
| `PAUSED` | 计时与实时指标冻结，显示暂停浮层 | 禁用 |
| `DISCONNECTED` | 保留最后数值，显示「已断开 · 正在重连」 | 禁用 |
| `RECONNECTING` | 同上，重连提示 | 禁用 |
| `ENDED` | 会话结束 | 禁用 |

### 3.2 控制写入状态 `ControlWriteState`

| 状态 | Toast | 数值处理 |
|---|---|---|
| `IDLE` | 无 | — |
| `WRITING` | 「正在设定…」+ 旋转指示 | 乐观显示目标值 |
| `FAILED` | 「设定失败 · 连接已中断」+「已回弹」 | 回退到原值 |
| `REJECTED` | 「设备已拒绝 · 已恢复 X」+「保留原值」 | 回退到原值 |

### 3.3 设备能力

指标分两类：可调节（`resistance`、`target-speed`）与只读（其余）。
只读设备把可调节指标也降级为只读，显示「设备只读」。
候选项由设备能力动态提供——未支持的指标不出现，也不用 0 占位。

### 3.4 交互参数（HTML 实测值）

| 交互 | 参数 |
|---|---|
| 长按触发编辑 | 500 ms；移动容差 10 px；触发后 650 ms 内抑制 click |
| 长按触觉反馈 | 12 ms 振动 |
| 抽屉拖动关闭阈值 | 44 px |
| 滑动结束完成阈值 | 行程的 78% |
| 滑动结束键盘步进 | 方向键 12 px |
| 仪表量程 | 0–30 km/h 铺满 286° 弧，起始 218° |
| 仪表主刻度 | 每 5 km/h 一根，步距 47.667° |
| 仪表小刻度 | 每 1 km/h 一根，步距 9.533° |

仪表环角度按参考稿原样照抄，含一处不对称：218° 起、286° 扫，缺口 74°，
中点落在 91°，比正下方偏 1°。要完全对称起点得是 217°。与 1.6 的对比度一样，
这是参考稿自己的值，不在复刻阶段改；`WorkoutGaugeGeometryTest` 把这 1° 钉住，
避免日后被当成 bug 修掉。

Compose 的 0° 在 3 点钟，参考稿从 12 点钟量，代码里的起始角是 `218f - 90f`。
| 阻力量程 | 1–16，步进 1 级 |
| 目标速度量程 | 0.5–20.0 km/h，步进 0.1 |

### 3.5 指标目录

16 个候选指标，分三类：

- **实时**：心率、实时功率、踏频、当前坡度、阻力（可调）、目标速度（可调）、当前速度
- **平均**：平均速度、平均心率、平均功率
- **最大 / 总数值**：运动时长、距离、热量、步数、峰值心率、最大功率

### 3.6 布局持久化

| HTML | 本项目 |
|---|---|
| `localStorage["ftms-state-matrix-theme"]` | `ThemePaletteStore`（SharedPreferences，只存稳定 ID） |
| `localStorage["ftms-state-matrix-layout-v1"]` | 阶段 5 实作，键为槽位 key，值为指标 ID |

槽位 key 沿用 HTML 的命名：`portrait-{0..5}`、`land-side-{0..3}`、`land-dock-{0..7}`。

---

## 4. 模拟数据边界

本阶段只做 UI 复刻，不接 FTMS 真机、BLE 或 WorkoutSession：

- 指标数值使用参考稿的固定示例值，可由状态切换重复触发。
- 连接状态、设备能力、写入结果由 Demo 内的模拟控制器驱动。
- 接入真实 FTMS runtime 应另立任务。

---

## 5. 构建与验证

### 5.1 构建环境

见 [BUILDING.md](BUILDING.md)。要点：Oracle JDK 21 + 为该次进程提供短且可写的
`TEMP`/`TMP` + 明确指定 `ANDROID_SERIAL`。

```powershell
$buildTemp = Join-Path $PWD '.build-tmp-local'
New-Item -ItemType Directory -Force -Path $buildTemp | Out-Null
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = $buildTemp; $env:TMP = $buildTemp
$env:ANDROID_SERIAL = '<adb-device-serial>'
.\gradlew.bat :app:assembleDebug :app:installDebug
```

阶段 1 用这套组合验证过：`:designsystem-tokens:jvmTest`、
`:designsystem:testDebugUnitTest`、`:app:testDebugUnitTest`、`:app:assembleDebug`
全部 BUILD SUCCESSFUL。

**不要自己造绕过 Gradle 的编译脚本。** 手工串 aapt2 → kotlinc → d8 等于重写 AGP：
依赖版本要自己锁（取最高版本会挑到 lifecycle 2.10.0，它把 id 资源搬走、包名也改了，
真机上 `setContent` 直接 `ClassNotFoundException`）、KMP 的 android variant 发布成
`<base>-release.aar` 而不是 `<artifact>-<version>.aar`、R 的字段是编译期常量不能用
假值……这些 AGP 都处理好了。`Selector.open()` 故障是环境问题，按 BUILDING.md 解决它，
不要绕过它。

### 5.2 真机像素验证的注意事项

阶段 7 要做 8 主题 × 多状态的截图对照，先记下阶段 1 踩到的：

- **高饱和色不能做精确像素比对。** 设备以 Display P3 截图，转 sRGB 时饱和的绿/青会
  去饱和（R 通道被抬高）：Tactile 的 `#10E66B` 采样成 `#6AE279`，差 90。
  结构色（screen/canvas/track/panel）几乎不失真，误差 0–1，可以精确比对。
  要核对强调色，看页面上由 `toArgb()` 印出来的 hex 标签，那个不受色彩管理影响。
- **采样前必须先滚到顶。** 页面滚动位置会让硬编码坐标全部错位，
  拿到的是隔壁 token 的色块。
- **`input tap` 之后要留时间。** 不 sleep 直接 screencap 会抓到切换前的帧。
- **连续 adb 操作可能让设备掉线**（实测跑到第 4 个主题时断了一次），
  批量脚本要在每轮前 `adb wait-for-device`，并检查截图大小是否为 0 或被截断。

### 5.3 编译过不等于跑得起来

阶段 1 有两个 bug 是编译期完全看不出来的，都是装到真机才炸出来：

- `Color(argb.toULong())` 用错重载。`Color(ULong)` 收的是 Compose 内部打包格式
  （高 32 位 RGBA、低 6 位 color space id），ARGB 直接塞进去会让 color space id
  取到低 6 位，`0xFFFF6969 → 41` 而 `ColorSpaces` 只有 18 项，任何 `copy()` /
  `luminance()` 都 `ArrayIndexOutOfBoundsException`。已由
  `ThemePaletteColorTest` 守住。
- 依赖版本挑错（见 5.1）。

改完 Compose 或主题相关的东西，跑一次 `:app:installDebug` 再说完成。

### 5.4 仍未验证的部分

- `designsystem/src/androidTest`（instrumented 测试）——`GlassLensTabBarTest`
  从加进来就没跑过，需要 `:designsystem:connectedDebugAndroidTest`。
- Lint、R8/ProGuard、release 签名。

## 6. 阶段进度

| 阶段 | 状态 | 产出 |
|---|---|---|
| 0 基线与规格冻结 | 完成 | 本文档 |
| 1 主题基础设施 | 完成 | `ThemePalette`、`GlassPalette`、`GlassPaletteSelector`、`ThemePaletteStore`、主题色验收页 |
| 1 的 review 修正 | 完成 | Tactile 描边、系统栏明暗、强调色单一真相源、选择器与色板对比度 |
| 1 的真机验证 | 完成 | 小米 24091RPADC：8 主题逐个切换无崩溃；结构色（screen/canvas/track）像素级匹配 token，误差 0–1；Nordic/Digital/Tactile/Lime 目视核对 hex 标签全对；Neutral 预设与强调色选色器未被 palette 改动破坏 |
| 2 通用组件主题贯穿 | 完成（8 条工作内容做了 5 条） | `asSelectedSurface` 补前景色、`asTrackSurface`、`asOverlaySurface`、`bodyEndColor` 双色渐层、Lens 接主题；CTA／Connection／danger 三条因 SDK 无对应组件，留到阶段 3/4 |
| 3 Responsive State Matrix 主画面 | 未开始 | — |
| 4 运行状态与控制交互 | 未开始 | — |
| 5 指标编辑抽屉 | 未开始 | — |
| 6 Digital 与 Tactile 专属风格 | 未开始 | — |
| 7 视觉与交互验收 | 未开始 | — |

---

## 6. 与 UI 稿的对齐核对（2026-09-08）

核对对象：`D:/Github/FTMS/docs/ui-designs/02-运动控制-workout-controls/02-响应式状态矩阵-Responsive-State-Matrix.html`

### 6.1 已对齐

阶段 3–5 的结构、角度、阈值、文案均逐项比对过 HTML 源码，包括：
长按 500 ms / 移动容差 / 触发后抑制 click、抽屉 44 px 拖动关闭、抽屉出现在数据位对侧、
三分类页签、候选项两列与「已在其他位置显示」、页脚说明在竖屏隐藏、候选项按设备能力筛选、
仪表 218°/286°/47.667°/9.533°、滑动结束 78%、三种写入 Toast 的文案与数值处理。

### 6.2 核对中发现并已修正

| 项 | 原实现 | 参考稿 |
|---|---|---|
| 只读提示位置 | 数值下方独立一行 | `float: right`，与 `.mini-step` 同在右上角 |
| 只读提示颜色 | 危险色 | `#89928d` 中性灰（`.read-only`） |
| 只读卡描边 | 实线 | `border-style: dashed`（`.control-disabled`） |
| 只读文案 | 「暂时无法调节」 | 「不可调节」 |
| 调节浮层滑杆 | 水平 `GlassSlider` | **直立**管状滑杆 58×154，从底部往上填，带「当前」标记 |
| 调节浮层量程 | 一行「量程 1 – 16」 | 左栏 MAX/MIN 两端 + 右栏 STEP 步进值 |
| 精调说明 | 无 | `细调 / 每次 1 级` |

### 6.3 补全记录（2026-09-08 第二轮）

6.3 原列的缺口除阶段 6 外已全部实现：

| 元素 | 实现 |
|---|---|
| `.gauge-copy span` | `WorkoutGauge` 的 `delta` 参数，冻结时不显示 |
| `.phase-mini` | `WorkoutGauge` 内的 `PhaseMiniBar` |
| `.pulse-wave` / `.l-power-bars` / `.slope-graphic` / `.card-state` | `MetricArtwork`，由 `MetricSlot.drawsArt` 控制只在横屏侧卡绘制 |
| `.metric-trend` | `MetricTrendChart`，仅第 2 组显示；心率系走危险色 |
| `.metric.source` `visibility: hidden` | **不复刻**。参考稿的浮层压在源卡上，藏起来不留洞；这里浮层居中弹出，源卡多半没被盖住，藏了就是个空槽 |
| `.metric-scroll-indicator` | 按 `firstVisibleItemIndex / 可滚动项数` 定位 |

仍未实现：七段数码管 / 点环 / 拟物胶帽（Digital 与 Tactile 专属绘制，阶段 6）。

补全过程中真机暴露并修掉的问题：

| 问题 | 原因 |
|---|---|
| 竖屏指标格排成 2 列 3 行 | 实现时把 `2 ROWS × 3 COLUMNS` 写反 |
| 顶栏「室内单车18:42」连成一串 | 设备名与时长挤在一行，缺分隔 |
| 手机横屏内容被压成一条缝 | 场景条固定占两行；横屏可用高度仅 ~457dp。改为跟随内容滚动 |
| 仪表「+1.6」压住刻度「30」 | 中心读数用固定 sp，环缩小后不跟着缩。改为按环直径取比例 |
| 趋势线抖成锯齿 | 12 个采样点挤在 ~52dp 宽内。降到 8 点、噪声减半 |

### 6.4 有意偏离

| 项 | 处理 | 原因 |
|---|---|---|
| `.phone` / `.land` 设备外框、灵动岛、状态栏 | 不复刻 | 网页需要在桌面上示意设备；真机上屏幕本身就是外框 |
| 横屏换组按钮位置 | 放在底部（参考稿在 `.l-meta` 顶栏） | 横竖屏共用一个 `MetricGroupSwitcher`，位置统一 |
| 换组副文案 | 两个指示点（参考稿是「第 1 组 · 6 / 8 →」） | 点比文字更省横向空间，且不随组数变化重排 |
| 横竖屏的设备与读数 | 共用一份状态（参考稿竖屏是室内单车 24.8、横屏是跑步机 10.5） | 真机上是同一台设备转屏，不是两台 |
| 精调说明换行 | 单行 | 面板比参考稿宽，单行不挤 |
| 直立滑杆管壁底色 | 走 `track` token（深色主题下是深的） | 参考稿在 base/Digital 下硬编码浅色管、Tactile 下是深色，交给 token 分主题决定 |
| 长按进度环起点 | 左上圆角之后（参考稿 conic 从正上方 −90°） | Compose 无 conic-gradient；改用圆角矩形路径取前一段 |
| 编辑角标图形 | Material 铅笔图示（参考稿是 `✎` U+270E） | 系统字体不保证有该字，真机上会变成空白圆点 |
