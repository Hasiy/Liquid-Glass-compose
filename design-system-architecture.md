# Hasiy Design System Compose 架构说明

## 1. 文档目的

本文档定义 Hasiy Design System Compose 的模块边界、主题模型、依赖方向和扩展约定。

SDK 当前以 Liquid Glass 控件为第一套视觉风格，但 SDK 名称、主题 Token 和组件接入方式不绑定某一种视觉风格，后续可以继续增加其他主题与渲染风格。

## 2. 当前模块结构

```text
HasiyDesignSystemDemo
├── designsystem/                 Android Compose UI SDK
├── designsystem-tokens/          KMP 主题规格与语义颜色
└── app/                          Demo 应用与组件目录
```

### `:designsystem`

- Android Library，提供 Compose / Material3 控件和视觉渲染实现。
- 当前包含 Liquid Glass 风格的基础控件、导航、弹层、输入、反馈和布局组件。
- 对外包名：`top.hasiy.designsystem`。
- 对外主题入口：`DesignSystemTheme`。
- 当前 Android 模糊、阴影和资源实现留在本模块，不依赖 Demo 的资源和业务代码。

### `:designsystem-tokens`

- Kotlin Multiplatform 元数据模块，不依赖 Android、Compose 或 Android 资源。
- 对外包名：`top.hasiy.designsystem.tokens`。
- 提供 `GlassThemeSpec` 和 `GlassVisualStyle`，承载跨平台主题身份、语义颜色和视觉风格选择。
- 当前启用 JVM 与 iOS targets；Windows 开发机上的 iOS target 只是不执行构建，不影响 JVM 验证。

### `:app`

- 仅作为示例和验收应用。
- 展示所有 SDK 控件、主题切换和交互效果。
- 可以依赖 SDK，但 SDK 不得反向依赖 Demo 的资源、状态或页面代码。

## 3. 依赖方向

```text
app ───────────────▶ designsystem ───────────────▶ designsystem-tokens
 │                         │
 └── Demo 页面与验收         └── Android Compose 渲染
```

约束如下：

1. `designsystem-tokens` 只保存跨平台主题契约和纯值对象。
2. `designsystem` 负责把 Token 转换为 Android `GlassConfig` 和 Material3 `ColorScheme`。
3. `app` 不直接修改 SDK 内部实现，也不在页面中复制主题映射逻辑。
4. 未来 CMP 共享 UI 时，`commonMain` 只能依赖 tokens 和框架无关的状态；Android、iOS 各自提供渲染器。

## 4. 主题模型

主题由三层组成：

```text
GlassThemeSpec       跨平台主题规格：颜色、明暗、风格身份
        │
        ├── toGlassConfig()          Android 玻璃控件配置
        └── toMaterialColorScheme()  Material3 颜色方案
```

### 主题规格

`GlassThemeSpec` 至少包含：

- `id`：主题稳定标识。
- `visualStyle`：`GlassVisualStyle`，当前包括 `DROP`、`NEUTRAL`、`DARK`、`NATIVE`。
- `isLight`：Material3 和系统栏明暗判断。
- `primary`、`secondary`、`surface` 等语义颜色。
- `glassBase`、`glassHighlight`、`glassBorder` 等玻璃视觉颜色。
- `accent`、`accentEnabled` 等强调色配置。

新增主题时优先新增 `GlassThemeSpec` 预设或业务侧规格，不要在每个控件中写主题分支。

### Compose 接入

页面根节点调用一次：

```kotlin
DesignSystemTheme(spec = GlassThemeSpec.default(GlassVisualStyle.NEUTRAL)) {
    GlassBackdropHost(Modifier.fillMaxSize()) {
        ScreenContent()
    }
}
```

SDK 通过 `LocalGlassConfig` 和 `LocalGlassThemeSpec` 向控件树提供当前配置。控件仍可通过显式 `GlassConfig` 覆盖局部参数，但不应绕过主题规格自行拼装颜色。

## 5. Liquid Glass 的定位

Liquid Glass 是当前 SDK 的视觉实现，不是 SDK 的架构名称：

- `Glass*` 控件是当前第一批控件 API。
- `GlassVisualStyle` 表达视觉风格选择。
- `Hasiy Design System` 是模块、包名和发布坐标的稳定名称。
- 后续可以在同一套 Token 和控件契约上增加其他视觉风格，而不重新命名 SDK 模块。

本次项目尚未对外发布，因此旧的 `liquidglass` 模块、包名和坐标直接移除，不提供兼容别名。

## 6. 发布约定

Android SDK 发布坐标：

```text
top.hasiy:hasiy-design-system-compose:1.0.0
```

本地模块依赖：

```kotlin
implementation(project(":designsystem"))
```

SDK 的 AAR 不隐式打包 Haze 的传递依赖；直接消费 AAR 时，调用方需要显式声明 Haze。

## 7. 后续扩展规则

### 新增主题

1. 在 `designsystem-tokens` 增加主题规格或预设。
2. 为语义颜色补充测试。
3. 在 Android 适配层确认 `GlassConfig` 和 Material3 映射。
4. 在 Demo 增加切换入口和可读性验收。
5. 不在单个控件内硬编码主题名称。

### 新增平台

1. 先复用 `designsystem-tokens` 的主题契约。
2. 在平台 source set 提供对应渲染实现。
3. 不把 Android 的 Haze、资源或 Canvas 实现泄漏到 `commonMain`。
4. 完成至少一套跨平台主题和组件验证后，再扩大共享 UI 范围。

### 新增控件

1. 优先复用已有 `GlassConfig`、主题 Local 和语义颜色。
2. 保持内容层清晰，背景模糊只发生在控件自己的圆角表面内。
3. 为 API 26–30 保留可读的降级路径。
4. 同步更新 SDK README、Demo 组件目录和单元测试。

## 8. 架构验收清单

- [ ] SDK 可以脱离 `app` 独立编译和发布。
- [ ] `designsystem-tokens` 不引入 Android 或 Compose 依赖。
- [ ] Demo 只依赖 SDK，不存在 SDK 反向引用 Demo 的情况。
- [ ] 新主题只需扩展 Token 和映射，不修改所有控件。
- [ ] Android 与未来 iOS/CMP 渲染实现可以共享同一份主题规格。
- [ ] 发布坐标、包名和模块名统一使用 `Hasiy Design System`。

