# SPEC-00 · Liquid-Glass Compose 总览规范

> SDD（Spec-Driven Development）：`specs/` 目录为唯一事实来源，实现必须与 spec 一致；
> 变更先改 spec 再改代码。代码中非显而易见的实现需以 `// REQ-xx` 注释回溯到本目录。

## 1. 目标

在 Android 平台以 Jetpack Compose 最高程度复刻 Apple Liquid Glass（iOS 26）视觉语言：

- 玻璃材质：通透填充 + 背景模糊 + 边缘 rim light + 内侧高光 + 液态描边
- 双模糊策略：`RENDER_EFFECT`（实时）与 `OFFSCREEN_CAPTURE`（离屏合成快照）均实现且可切换
- 组件库：卡片 / 按钮 / 进度条 / 滑杆 / 列表项 / Tab 栏
- Demo：三 tab 页面 1:1 复刻参考截图（暗色玻璃风格）

## 2. 技术基线（与本机环境对齐，零额外下载优先）

| 项 | 版本 |
|---|---|
| Gradle | 8.13（wrapper，复用本机 dists 缓存） |
| AGP | 8.9.1 |
| Kotlin | 2.1.21（含 compose-compiler 插件） |
| Compose BOM | 2025.01.01 |
| material3 | 1.3.1 |
| activity-compose | 1.9.3 |
| core-ktx | 1.15.0 |
| JDK | 21 |
| minSdk / compileSdk / targetSdk | 26 / 35 / 35 |
| repositories | google(), mavenCentral() |

## 3. 模块结构

```
Liquid-Glass-compose/
├─ specs/                  # 本目录：SDD 规范
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml
├─ glass-core/             # Android Library：材质 + 组件（无业务依赖）
│   └─ src/main/kotlin/com/liquidglass/core/
│       ├─ theme/          # LiquidGlassTheme、GlassTokens、GlassPalette
│       ├─ blur/           # BlurStrategy：RenderEffect / OffscreenCapture
│       ├─ surface/        # GlassSurface（玻璃面容器）
│       └─ components/     # GlassCard / GlassButton / ... / GlassTabBar
└─ glass-demo/             # Application：三 tab demo
    └─ src/main/kotlin/com/liquidglass/demo/
```

## 4. 约定

- 包名：`com.liquidglass.core` / `com.liquidglass.demo`
- 所有视觉常量集中在 `GlassTokens`（SPEC-01），组件不得硬编码颜色/圆角/模糊半径
- 默认暗色主题（对齐参考截图），亮色 palette 预留
- 性能：RENDER_EFFECT 模式逐帧实时；OFFSCREEN 模式快照节流 ≥100ms
- 构建验收：`gradlew :glass-demo:assembleDebug` 通过

## 5. Spec 索引

| 文件 | 内容 |
|---|---|
| SPEC-01 | 设计 tokens（颜色/圆角/模糊/层级） |
| SPEC-02 | 玻璃面与双模糊策略 |
| SPEC-03 | 组件库 |
| SPEC-04 | Tab Demo 页面与交互 |
| SPEC-05 | 连续变焦转场（Continuum） |
