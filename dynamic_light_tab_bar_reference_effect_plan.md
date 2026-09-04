# Dynamic Light Tab Bar 参考效果升级方案

> 状态：方案草案，尚未实施  
> 目标组件：`DynamicLightTabBar`  
> 目标效果：深色悬浮导航栏、凸出式液态玻璃选中透镜、图标与文字折射、彩色边缘色散、独立中央操作按钮

## 1. 背景与目标

当前 `DynamicLightTabBar` 已具备毛玻璃背景、触摸跟随光照、拖动吸附、方向性拉伸、触觉反馈以及平台降级能力。参考效果在此基础上进一步强调“玻璃透镜”本身：选中态不是普通的半透明背景块，而是一块覆盖在导航内容之上的凸透镜。

本次升级目标如下：

- Bar 使用近黑灰的低反射玻璃材质，减少当前偏亮、偏紫的观感。
- 选中 Pill 比 Bar 更高、更宽，并允许上下突出 Bar。
- Pill 内的图标、文字和背景产生轻微放大、弯曲及色散，而不只是叠加白色光斑。
- 导航项调整为“图标在上、文字在下”，并支持独立的中央主操作按钮。
- 保留当前点击、拖动、吸附、方向性液态动画、震动反馈和无障碍语义。
- API 33+ 提供完整 AGSL 效果，低版本保持交互一致并做视觉降级。

## 2. 非目标

以下内容不属于本次方案范围：

- 不修改页面导航框架或业务路由。
- 不引入逐帧 Bitmap 截图、CPU 模糊或 StackBlur。
- 不要求低版本完全复刻 API 33+ 的真实折射和 RGB 色散。
- 不把参考图中的具体品牌图标、文案或颜色写死进 SDK 组件。
- 不以继续提高中心亮度来模拟折射。

## 3. 当前能力与保留项

| 当前能力 | 处理方式 |
|---|---|
| `GlassBackdropHost` / Haze 背景采样 | 保留，继续作为 Bar 和 Lens 的背景玻璃来源 |
| 点击、拖动和吸附 | 保留，并适配中央操作按钮不参与选中索引的情况 |
| 跨项震动 | 保留 |
| Indicator 方向性拉伸和回弹 | 保留，待 Lens 尺寸完成后再微调倍率 |
| API 33+ `RuntimeShader` | 保留实例复用方式，但替换 Shader 的视觉模型 |
| API 33 以下 Compose 渐层降级 | 保留，并改成边缘高光优先 |
| `selectableGroup` / `Role.Tab` | 保留；中央按钮使用独立 Button 语义 |

现有实现的主要限制：

1. `items` 只有文字，无法表达图标、稳定 key 或中央操作项。
2. Indicator 与单个 Item 等宽，并在 Bar 内缩，无法形成参考效果中的悬浮凸透镜。
3. Indicator 先绘制，导航内容后绘制；Shader 只能处理 Indicator 自身像素，不能折射图标和文字。
4. 当前 Shader 的核心是 `base.rgb + lighting`，只会补光，不会改变采样坐标。
5. 当前边缘计算接近矩形边界，不符合胶囊透镜的真实轮廓。

## 4. 目标视觉拆解

### 4.1 Bar 主体

- 材质：近黑灰半透明玻璃。
- 高光：仅保留顶部极弱的连续反光。
- 描边：细且低对比，不出现一圈明显白边。
- 阴影：以黑色环境阴影为主，移除明显蓝紫色外发光。
- 内容：未选中图标和文字使用灰白色，保证暗背景上的对比度。

### 4.2 选中 Lens

- 宽度约为单个 Item 的 `1.40～1.50` 倍。
- 高度比 Bar 高约 `8～12dp`，上下各突出 `4～6dp`。
- Lens 以当前 Item 中心对齐；靠近首尾时在 Bar 内边距范围内限位。
- 主体为深灰透明玻璃，中心区域不出现清晰的圆形亮核。
- 内容有轻微放大和弯曲，边缘出现可控的青、绿、紫色散。
- 顶边和底边的折射高光可以略强，左右边缘保持克制。

### 4.3 导航内容

- 普通项采用图标在上、标签在下的纵向布局。
- 选中项使用主题强调色，默认参考色可采用青绿色。
- 中央操作按钮为独立圆形控件，不参与 `selectedIndex`。
- 中央按钮需要独立点击回调、触觉反馈、按压动画和无障碍描述。

## 5. 推荐组件结构

```text
DynamicLightTabBar
├─ BarGlassLayer                 Bar 毛玻璃、暗色填充、描边、阴影
├─ BaseNavigationContent         正常绘制的图标和文字
├─ SelectionLensOverlay          可突出 Bar 的独立选中透镜
│  ├─ LensBackdropLayer          Lens 内的背景模糊与深色玻璃
│  ├─ RefractedContentLayer      对齐后的导航内容副本，仅在 Lens 内显示
│  ├─ ChromaticEdgeLayer         RGB 色散、胶囊 Fresnel 边缘
│  └─ SpecularLayer              顶部/底部柔和高光
└─ CenterAction                  独立主操作按钮
```

关键原则：Bar 的裁切只约束 Bar 材质，不裁切 `SelectionLensOverlay`。Lens 在独立 Overlay 中绘制，才能上下突出并保留外缘阴影。

## 6. API 调整建议

将纯文字列表改成具备图标和稳定标识的模型：

```kotlin
@Immutable
data class DynamicLightTabItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)
```

组件接口建议增加中央操作项：

```kotlin
@Composable
fun DynamicLightTabBar(
    items: List<DynamicLightTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    centerAction: (@Composable () -> Unit)? = null,
    onCenterActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

约束：

- `selectedIndex` 只对应 `items`，不包含中央操作按钮。
- 中央按钮仅影响视觉插入位置，不改变业务索引。
- 如果需要完全通用的 SDK API，可把 `ImageVector` 改成图标 Slot，但首版优先控制复杂度。
- 标签继续由调用方通过 `stringResource` 提供，不在组件内部写死业务文案。

## 7. 绘制层级改造

### 7.1 Bar 与 Lens 分离

当前 Indicator 位于 Bar 内部并使用 `fillMaxHeight().padding(4.dp)`。需要改成：

- 外层容器高度按 Lens 高度计算。
- Bar 在容器内垂直居中。
- Lens 作为 Bar 的兄弟层，不受 Bar Shape 裁切。
- 点击区域仍以完整 Item 槽位计算，不跟随 Lens 的放大尺寸改变。

### 7.2 Lens 内重复绘制内容

若只对 Lens 背景施加 `RuntimeShader`，无法折射后绘制的图标和文字。推荐方案是在 Lens 内重复绘制一份导航内容：

1. `BaseNavigationContent` 正常显示完整导航。
2. `RefractedContentLayer` 绘制相同内容，并按 `-indicatorOffset` 平移，使它与底层内容对齐。
3. 将副本裁切在 Lens Shape 内。
4. 对该副本应用 AGSL 坐标扭曲、放大和 RGB 分通道采样。
5. Lens 的暗色玻璃和边缘高光覆盖在副本的适当层级。

此方案不需要逐帧截屏，也不会把 Bitmap 分配放入动画热路径。

### 7.3 避免内容重影

- Lens 主体需要足够的深色透明度，遮住底层未折射内容。
- 内容副本必须和底层使用完全一致的排版尺寸、字体、图标及颜色状态。
- Lens 外部严格裁切，不允许副本泄漏。
- 对齐计算使用像素坐标，避免 dp 转换带来的半像素漂移。

## 8. AGSL 折射模型

完整效果不再以径向白光为核心，而采用以下处理顺序：

1. 根据胶囊 Signed Distance Field 计算 Lens 内外和边缘距离。
2. 以 Lens 中心为基准生成轻微径向坐标扭曲。
3. 对中心区域做小幅放大。
4. 沿法线方向分别偏移 R、G、B 三个通道的采样坐标。
5. 在胶囊边缘叠加 Fresnel 高光。
6. 顶边和底边增加低透明度的彩色折射带。
7. 最后叠加很弱的触摸跟随补光。

概念流程：

```agsl
float2 local = coord - lensCenter;
float edgeDistance = capsuleSdf(local, lensHalfSize, radius);
float edgeMask = edgeBand(edgeDistance);

float2 warpedCoord = lensCenter + local * magnification + distortion(local);
float2 rgbOffset = lensNormal(local) * chromaticAmount * edgeMask;

half red = content.eval(warpedCoord + rgbOffset).r;
half green = content.eval(warpedCoord).g;
half blue = content.eval(warpedCoord - rgbOffset).b;

half3 refracted = half3(red, green, blue);
half3 result = refracted + fresnel(edgeMask) + softTouchLight;
```

注意：以上只用于说明算法，不是可直接提交的最终 Shader。正式实现时需要处理采样越界、透明像素、色域和不同 GPU 的精度差异。

## 9. 初始参数建议

以下数值仅作为第一轮真机调参起点，最终以截图和拖动录屏为准：

| 参数 | 当前值/状态 | 建议起点 | 目的 |
|---|---:|---:|---|
| Bar 高度 | `72dp` | `72dp` | 保持现有触控尺寸 |
| Lens 高度 | Bar 内缩 | `80～84dp` | 形成上下突出效果 |
| Lens 宽度 | `1.0 × itemWidth` | `1.45 × itemWidth` | 覆盖当前项并轻微侵入相邻项 |
| Bar 背景色 | 浅灰低透明 | 近黑灰、较高不透明度 | 接近参考图的深色材质 |
| Bar Glow 强度 | `0.20` | `0.10～0.14` | 避免整体中心过亮 |
| Lens 中心补光 | `0.035` | `0.02～0.04` | 保持柔和，不形成亮核 |
| Lens 放大倍率 | 无 | `1.04～1.08` | 产生凸透镜感 |
| RGB 偏移 | 无 | `1～3px` | 产生克制的边缘色散 |
| Fresnel 强度 | 矩形近似 | `0.12～0.22` | 强调胶囊边缘 |
| 外阴影 | 蓝紫色 | 黑色低透明阴影 | 降低偏紫和发光感 |
| 中央按钮尺寸 | 无 | `48～52dp` | 建立主操作视觉层级 |

调参顺序必须是：先确定几何尺寸和暗色材质，再调整折射，最后调整光照。否则容易用过亮高光掩盖结构问题。

## 10. 动画与交互

### 10.1 继续保留

- 点击后向目标方向拉伸。
- 到位后轻微压缩并回弹。
- 拖动时跟手移动，松手吸附。
- 跨越 Item 时每个边界最多震动一次。

### 10.2 需要适配

- Lens 基础宽度增加后，现有 `scaleX = 1.15` 可能显得过度，可从 `1.10～1.12` 开始验证。
- Transform Origin 继续根据移动方向设置在旧选项一侧。
- Lens 首尾位置需要按自身宽度限位，不能再直接使用单个 Item 宽度计算边界。
- 拖动命中逻辑仍以 Item 中心决定选中项，不以 Lens 覆盖范围决定。
- 中央按钮区域不能被映射成普通 Tab；拖动经过时可选择跳过或保持最近的普通 Tab，行为需固定并测试。

推荐的中央按钮拖动规则：中央按钮不参与选择，经过其范围时保持最近一次普通 Tab，越过中心区域后再切换到另一侧 Item。

## 11. 色彩与主题

不要在组件中永久写死参考图颜色。建议将下列 Token 纳入 `DynamicLightTabBarConfig` 或对应主题配置：

- `barContainerColor`
- `barContainerAlpha`
- `barBorderColor`
- `barShadowColor`
- `lensBodyColor`
- `lensBodyAlpha`
- `lensEdgeHighlightColor`
- `lensChromaticCyan`
- `lensChromaticGreen`
- `lensChromaticMagenta`
- `selectedContentColor`
- `unselectedContentColor`
- `centerActionContainerColor`
- `centerActionContentColor`

Dark 主题可以最接近参考效果；Neutral 和其他主题仍需保证图标、文字及 Lens 边缘具有足够对比度。

## 12. 平台降级

| 平台 | Lens 背景 | 内容折射 | 色散 | 交互 |
|---|---|---|---|---|
| API 33+ | Haze/背景玻璃 | AGSL 坐标扭曲与放大 | AGSL RGB 分通道采样 | 完整保留 |
| API 31–32 | Haze/平台模糊 | 不做真实扭曲 | Compose 多色边缘渐层 | 完整保留 |
| API 26–30 | 高浓度磨砂回退色 | 不做真实扭曲 | 简化描边渐层 | 完整保留 |

低版本的验收重点是层级、可读性和交互一致，不要求像素级复刻高版本 Shader。

## 13. 性能约束

- `RuntimeShader` 使用 `remember`，动画过程中只更新 uniform。
- 不在每帧创建 Bitmap、图标资源、列表或大尺寸离屏缓存。
- 内容副本只在 Lens 裁切区域内参与绘制。
- 静止时若不需要触摸动态光，不持续触发 Shader uniform 更新。
- Bar 与 Lens 的效果分层控制在必要数量，避免重复模糊同一区域。
- 至少在一台 60 Hz 中端设备上记录连续拖动的 `gfxinfo`。
- 如果特定 GPU 出现 Shader 编译或渲染问题，允许通过运行时开关降级到 Compose 边缘渐层。

## 14. 无障碍要求

- 普通导航项保持 `selectableGroup` 和 `Role.Tab`。
- 每个 Item 暴露标签和选中状态；装饰性图标不重复朗读。
- 中央按钮使用独立 Button 语义和明确的 `contentDescription`。
- 触控目标至少 `48dp × 48dp`。
- 不能只依赖颜色表达选中状态，Lens 位置和语义状态必须同步。
- 开启系统“移除动画”时缩短或取消形变动画，但保持状态切换正确。

## 15. 实施阶段

### 阶段一：结构与 API

- 引入带图标的 Item 模型。
- 增加中央操作按钮 Slot 和独立回调。
- 建立五槽位布局，并修正业务索引与视觉槽位的映射。
- 补充索引映射、拖动命中和中央区域跳过规则的单元测试。

验收：图标、文字和中央按钮布局正确；所有点击、拖动和无障碍语义正确。

### 阶段二：悬浮 Lens 几何与材质

- Bar 与 Lens 分层。
- Lens 调整为约 `1.45 × itemWidth`，并允许上下突出。
- 改用暗色 Bar、暗色 Lens 和低亮度边缘。
- 修正首尾位置限位和阴影裁切。

验收：即使尚未启用真实折射，静态轮廓、层级和配色已经接近参考效果。

### 阶段三：内容折射和色散

- 增加 Lens 内对齐的导航内容副本。
- 使用胶囊 SDF 重写边缘计算。
- 增加坐标扭曲、轻微放大、RGB 分通道采样和 Fresnel 高光。
- 移除明显中心亮核，将视觉能量转移到 Lens 边缘。

验收：图标和文字在 Lens 内可见轻微放大/弯曲；色散主要出现在边缘，不影响阅读。

### 阶段四：动效、降级和验证

- 根据新 Lens 尺寸重新调整拉伸与回弹倍率。
- 完成 API 31–32、API 26–30 的视觉降级。
- 执行主题、TalkBack、快速连续点击、拖动取消和性能验证。

验收：各平台交互一致，无明显掉帧、错位、内容重影或 Shader 崩溃。

## 16. 预计修改范围

| 文件/区域 | 预计调整 |
|---|---|
| `DynamicLightTabBar.kt` | API、布局层级、Lens Overlay、内容副本、Shader、索引映射 |
| `DynamicLightTabBarConfig.kt` | Lens 几何、暗色材质、色散、Fresnel 和中央按钮参数 |
| `DynamicLightTabBarDemoScreen.kt` | 图标导航项、中央操作按钮和参考场景 |
| `res/values/strings.xml` | 中央按钮和新增演示文案的无障碍字符串 |
| `src/test/` | 槽位映射、Lens 限位、拖动命中和取消回位测试 |
| `dynamic_light_tab_bar.md` | 实现完成后同步公共 API、平台能力和使用示例 |

## 17. 风险与应对

| 风险 | 影响 | 应对方式 |
|---|---|---|
| Lens 内容副本与底层内容错位 | 出现双影或跳动 | 共用同一布局参数；使用像素坐标对齐并补截图测试 |
| 色散过强 | 文字模糊、像故障效果 | 色散仅在边缘生效；限制偏移并对文字中心区域降权 |
| Lens 被父层裁切 | 无法形成突出效果 | Bar 材质与 Lens Overlay 分离，检查所有父级 `clip` |
| 中央按钮破坏索引映射 | 点击或拖动选错项 | 分离业务索引和视觉槽位，使用纯函数并单测 |
| 多层效果导致掉帧 | 拖动不流畅 | 禁止逐帧 Bitmap；复用 Shader；仅绘制 Lens 裁切区域 |
| 不同 GPU 的 AGSL 差异 | 编译失败或颜色异常 | 真机覆盖并提供运行时降级开关 |
| 多主题下对比不足 | 图标或边缘不可读 | 所有颜色由主题 Token 提供，逐主题截图验收 |

## 18. 验收清单

### 静态视觉

- [ ] Bar 为近黑灰玻璃，不出现明显蓝紫外发光。
- [ ] Lens 宽于单个 Item，并在 Bar 上下形成可见突出。
- [ ] Lens 中心柔和，不出现清晰的圆形亮点。
- [ ] Lens 边缘存在克制的青、绿、紫色散。
- [ ] Lens 内的图标和文字有轻微放大或弯曲。
- [ ] 图标、文字和中央按钮的层级与间距稳定。

### 交互与动画

- [ ] 点击左右 Item 时 Lens 拉伸方向正确。
- [ ] 快速连续点击不会回跳到旧状态。
- [ ] 拖动时 Lens 跟手，松手后吸附到正确普通 Item。
- [ ] 经过中央按钮区域不会误选中央按钮或产生错误索引。
- [ ] 首尾 Item 上 Lens 不越界、不被裁切。
- [ ] 拖动取消后回到外部传入的 `selectedIndex`。

### 兼容与质量

- [ ] API 33+ 使用完整折射和色散。
- [ ] API 31–32 使用背景玻璃和渐层边缘降级。
- [ ] API 26–30 使用磨砂回退色，图标和文字仍清晰。
- [ ] Dark、Neutral 及当前 Demo 主题均通过可读性检查。
- [ ] TalkBack 正确朗读标签、选中状态和中央按钮。
- [ ] 连续拖动没有持续性掉帧或明显内存增长。
- [ ] 单元测试和项目构建通过。

## 19. 完成定义

只有同时满足以下条件，才能认为参考效果升级完成：

1. 组件结构支持图标导航和独立中央操作按钮。
2. Lens 可以突出 Bar，且宽高、限位和动画均稳定。
3. API 33+ 确实改变 Lens 内内容的采样坐标，而不是只增加亮度。
4. 色散集中在边缘，中心内容保持清晰可读。
5. 低版本视觉降级不改变交互和业务索引。
6. 静态截图、拖动录屏、TalkBack、单元测试和构建均有验证结果。

