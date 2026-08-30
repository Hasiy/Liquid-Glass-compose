# SPEC-02 · 玻璃面与双模糊策略

## 1. 分层模型（自下而上）

```
L0 页面背景（bgBase + 彩色光斑，光斑自身 blurStrong 柔化）
L1 页面内容（列表、文本等真实内容）
L2 GlassSurface 玻璃面 = 模糊(L1 内容) ⊕ glassFill ⊕ 高光/阴影 ⊕ rim 描边
L3 玻璃面上的前景内容（文本/图标/子组件）
```

REQ-GS-1：玻璃面必须模糊"其下方的真实内容"，而非仅半透明叠色。

## 2. 模糊策略（双实现，可切换）

`enum BlurStrategy { RENDER_EFFECT, OFFSCREEN_CAPTURE }`

全局开关：`LocalBlurStrategy: ProvidableCompositionLocal<BlurStrategy>`，默认 `RENDER_EFFECT`。

### 2.1 RENDER_EFFECT（实时）

- 实现：`Modifier.blur(radius)`，边缘处理使用默认裁剪模式（Compose ui-graphics，API 31+ 生效；<31 自动 no-op 降级为纯半透明，REQ-BLUR-DEGRADE）
- 约束：禁止使用 `BlurredEdgeTreatment.Unbounded`。实测部分 GPU 驱动下 Unbounded 会将模糊的黑色溢出区扩散到组件边界之外，在滚动容器中形成大面积黑色遮盖（2026-08 修复）
- 结构：`Box { 内容层(Modifier.blur(medium)) ; 玻璃叠层 }`
- 优点：零拷贝、逐帧实时、实现简单
- 局限：模糊的是"同层已绘制内容"，即玻璃面需包裹内容副本

### 2.2 OFFSCREEN_CAPTURE（离屏合成）

- 实现链路：
  1. 内容层绘制进 `androidx.compose.ui.layer.GraphicsLayer`（`record { drawContent() }`）
  2. `LaunchedEffect` 中以 `offscreenScale` 降采样 `ImageBitmap`，`graphicsLayer.toImageBitmap()` 捕获
  3. 对快照执行 `android.renderscript` 替代方案：`RenderScript` 已废弃 → 使用 **StackBlur 纯 Kotlin 实现**（半径 `offscreenBlurRadius`，作用于降采样图，等效强模糊且 O(n)）
  4. 模糊快照以 `Image` 铺满玻璃面区域（`ContentScale.Crop`），其上叠 glassFill/高光/描边
  5. 内容变化时按 `snapshotThrottleMs` 节流重捕（`snapshotFlow { }` 监听内容尺寸 + 滚动偏移）
- 优点：真实"透视下层"，跨层级可用（玻璃盖在任意内容上）
- 局限：快照滞后 ≤100ms，滚动时允许轻微滞后（液态感反而更自然）

REQ-BLUR-SWITCH：Demo 提供运行时切换开关，两种策略同屏可对比。

## 3. GlassSurface 绘制规范（REQ-GS-2）

在模糊层之上，按序绘制：

1. `glassFill` 圆角矩形填充
2. 顶部 `innerHighlight`：上半区垂直渐变（alpha 10%→0）
3. 底部 `innerShadow`：下 12% 区垂直渐变（0→18%）
4. rim 描边：`drawRoundRect` stroke 1dp `rimLight`；顶部 1/3 弧段叠加 `rimLightTop`（用 `drawArc` 或垂直渐变 stroke brush 模拟光源）
5. 前景内容 slot

## 4. 圆角与裁切

- 全部裁切使用同一 `RoundedCornerShape(radius)`，模糊层/填充/描边共享，保证边缘对齐
- 离屏快照绘制时同样 clip 到该 shape

## 5. 性能约束

- OFFSCREEN 模式：快照位图复用（同尺寸不重建），模糊在 `withContext(Dispatchers.Default)`
- 单屏同时存在的 OFFSCREEN 玻璃面 ≤ 6（超出自动降级 RENDER_EFFECT，REQ-PERF-CAP）
