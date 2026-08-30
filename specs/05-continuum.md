# SPEC-05 · 连续变焦转场（Continuum）

复刻 Liquid Glass 的标志性交互：点击玻璃元素 → 平滑缩放铺满成为新页面，
玻璃材质（圆角/边界/mask）随过渡进度插值。

## 1. 目标动效

- 点击首页 hero 玻璃卡 → hero 内容平滑放大、移动到全屏详情页，非共享元素淡入
- 详情页点击关闭 → 反向收缩回 hero 原位
- 过渡期间无跳变：共享元素用 `sharedBounds` 等比缩放（ResizeMode.Scale）

## 2. 技术实现

- API：`SharedTransitionLayout` + `Modifier.sharedBounds(rememberSharedContentState(key), scope)`
  （`ExperimentalSharedTransitionApi`，Compose 1.8）
- 结构：`MainActivity` 将 tab 内容包在 `SharedTransitionLayout` 内；
  `AnimatedContent` 分支 home 与 detail 用**同一 key** 的 `sharedBounds` 桥接
- 共享元素内容：`HeroArt`（纯 Canvas 封面视觉：对角渐变 + 双水滴 + rim）两端一致，
  保证缩放自然；标题文字不参与变形（放 sharedBounds 之外，各自淡入）

## 3. 共享 key

`HERO_KEY = "hero_cover"`（demo 层常量），首页 hero 卡与详情页封面共用。

## 4. 页面状态

`selectedTab ∈ {0 home, 1 materials, 2 about, 3 detail}`，tab 栏仅展示前三项：
- tab 栏高亮映射：`if (selectedTab == 3) 0 else selectedTab`
- 详情页关闭 `→ 0`；切 tab 直接离开详情

## 5. 验收

- home→detail、detail→home 过渡平滑，封面连续变焦无跳帧
- 返回位姿正确（回到 hero 原位）
- 玻璃叠层（fill/rim）随放大保持，缩小时不溢出圆角