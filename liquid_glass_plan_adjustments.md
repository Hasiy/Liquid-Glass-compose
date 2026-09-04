# Liquid Glass 方案可调整项

本文记录从 `spike` 计划草案中可吸收到当前分支的设计方法。它是一份后续调整清单，不代表这些项目已经进入实现范围。

## 1. 需求编号与追踪

### 建议

只给跨文件、需要独立验收的行为增加稳定编号，避免给每个视觉数值都编号。

| 建议编号 | 契约 | 主要落点 | 验收证据 |
|---|---|---|---|
| `REQ-BACKDROP-1` | 玻璃区域只模糊其背后的页面内容，前景文字与图标保持清晰 | `GlassBackdropHost`、`glassBackdrop` | 截图与滚动验证 |
| `REQ-BLUR-DEGRADE-1` | 不支持背景模糊或缺少 Host 时，使用不透明度更高的磨砂底色 | `overlayFallbackAlpha` | API 26–30 验证 |
| `REQ-THEME-1` | Drop、Dark、Neutral 的内容色、强调色和页面背景保持可读 | `GlassConfig`、资源色 | 三主题截图 |
| `REQ-TAB-1` | Tab 支持点击、拖动吸附、跨项震动和方向性液态动画 | `DynamicLightTabBar` | 点击与拖动录屏 |

### 可调整边界

- 编号只描述行为，不绑定 Haze、RenderEffect 或具体 Shader 实现。
- 文档、代码注释和测试使用同一个编号。
- 小范围样式调整继续留在 `GlassConfig` 或 `DynamicLightTabBarConfig`，不新增需求编号。

## 2. 玻璃分层契约

建议把当前 SDK 的绘制关系固定为以下四层：

```text
L0  页面背景与装饰
L1  页面实际内容，作为 GlassBackdropHost 的采样源
L2  玻璃表面：背景模糊、磨砂填充、高光、描边、阴影
L3  前景内容：文字、图标和交互控件，始终保持清晰
```

### 建议约束

- 每个页面只放一个 `GlassBackdropHost`，具体玻璃组件只消费它提供的背景源。
- 模糊、填充、描边使用相同 `Shape`，避免圆角边缘错位。
- 缺少 Host 或平台不支持时只降级 L2，不影响 L3 的可读性与交互。
- Demo 的背景颜色属于主题，不应由 SDK 组件反向依赖 Demo 资源。

### 可调整参数

| 参数 | 当前入口 | 调整原则 |
|---|---|---|
| 模糊半径 | `overlayBlurRadius` | 先保证文字不可辨识，再控制性能 |
| 回退浓度 | `overlayFallbackAlpha` | API 26–30 上保持内容可读 |
| 表面与描边 | `GlassConfig` | 深色提亮、浅色压暗 |
| 阴影 | `softShadow*` | 小控件避免大范围阴影互相叠加 |

### 实施结果（2026-08-31）

- `GlassBackdropHost` 检测到外层 Host 时复用既有 `HazeState`，不会建立第二个采样源。
- `GlassDialogBlurHost` 统一委托给 `GlassBackdropHost`，旧 API 也遵守单一来源契约。
- `glassBackdrop` 在 API 26–30 明确使用 `overlayFallbackAlpha`，API 31+ 且存在 Host 才进入 Haze 背景模糊。
- SDK README 已加入 L0–L3 分层、同 Shape 和平台降级契约。
- `GlassBackdropPolicyTest` 覆盖 API 26–30、API 31+、缺少 Host 与 Native 模式。

## 3. Dynamic Light Tab Bar 验收矩阵

### 平台降级

| 平台 | 背景处理 | 动态光照 | 必须保持的能力 |
|---|---|---|---|
| API 26–30 | 磨砂回退色 | Compose 渐层 | 点击、拖动、吸附、可读性 |
| API 31–32 | Haze/平台支持的背景模糊 | Compose 渐层 | 同上，且玻璃区域外保持清晰 |
| API 33+ | 背景模糊 | AGSL Shader | 光照跟手且不遮挡文字 |

### 交互验收

- 点击左右两侧 Tab，Indicator 的拉伸方向与移动方向一致。
- 连续快速点击时动画可取消并从最新状态继续，不跳回旧选项。
- 拖动跨越 Item 时每个边界最多触发一次震动。
- 拖动取消后回到外部传入的 `selectedIndex`。
- Drop、Dark、Neutral 下文字、边框和触摸光照都清晰可辨。
- TalkBack 能读出每个 Tab 的文字、选中状态和点击动作。

### 实施结果（2026-08-31）

- Tab 容器使用 `selectableGroup`，Item 使用 `selectable(selected, role = Role.Tab)`，向 TalkBack 暴露文字、选中状态与点击动作。
- 点击方向、拖动落点与取消回位使用可单测的纯函数；取消拖动后目标位置重新取外部 `selectedIndex`。
- `DynamicLightTabBarLogicTest` 覆盖左右拉伸方向、触点边界、无效尺寸和取消回位。
- API 33+ 保持 AGSL；API 33 以下保持 Compose 渐层。背景模糊的 API 26–30 回退由 SDK 层统一处理。
- Drop、Dark、Neutral 的视觉与真机 TalkBack/拖动验收仍需在可构建环境执行，结果不得仅以代码检查代替。
- 当前 Windows 桌面沙箱需为 Gradle 进程指定短 `TEMP/TMP`，避免 JDK NIO 的 AF_UNIX 管道路径过长。使用 `C:\Windows\Temp` 后，`:designsystem:testDebugUnitTest` 与 `:app:testDebugUnitTest` 已通过；已连接设备 `PLJ110` 的视觉/TalkBack 验收仍待补跑。

## 4. 性能守则

不引入 `spike` 的 StackBlur 双策略，仅保留可量化的性能要求。

- `RuntimeShader` 实例使用 `remember`，只更新 uniform。
- 只有拖动期间启用动态 RenderEffect；静止状态不持续刷新 Shader。
- 每帧路径避免创建 Bitmap、Brush 集合或大对象。
- 在 60 Hz 设备连续拖动时不应出现持续掉帧；验证时至少记录一次 `gfxinfo`。
- 若低端设备 AGSL 表现不稳定，允许增加运行时开关，降级到 Compose 渐层，不改变手势行为。

## 5. 暂不纳入

- 不增加 `OFFSCREEN_CAPTURE + StackBlur`：当前分支已经采用 Haze/`GlassBackdropHost`。
- 不采用固定暗色 Token 表：当前资源色与 `GlassConfig` 需要继续支持多主题覆写。
- Continuum 共享元素转场只作为 Demo 的独立增强项，不进入玻璃 SDK 核心契约。

## 6. 后续调整顺序

1. 先补需求编号和分层契约，不改变代码。
2. 按 API 与主题矩阵完成 Tab Bar 真机验收。
3. 有性能证据后再决定是否增加 AGSL 运行时开关。
4. Continuum 若要展示，单独立项并独立验收。
