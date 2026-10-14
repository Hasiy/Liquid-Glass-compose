# 项目优化与验证记录

日期：2026-10-13

## 1. 实施结果

| 范围 | 修改 | 验证 |
|:--|:--|:--|
| 滑杆交互 | 最新回调、量程与宽度；离散步进；RTL；取消手势不提交完成事件 | Android 真机 UI 回归 |
| 分段 Tab Bar | 最新回调、RTL 拖动和 Tab 选中语义 | Android 真机 UI 回归 |
| 无障碍 | Checkbox/Switch 开关状态、RadioButton 选中状态、Slider 范围/SetProgress/禁用状态 | Android 真机语义与动作测试 |
| 控制数值 | `overrides` 保存 Float，控制与图形计算直接取数值，展示时再格式化 | 德语 Locale 下连续调节测试 |
| 异步写入 | 区分已确认值和待提交预览；新写入、暂停、断线、结束及切换能力时取消旧任务并撤回预览 | 虚拟时钟测试成功、失败、拒绝、连续提交和取消 |
| 状态恢复 | 强调色开关/颜色与阴影开关改用 rememberSaveable；Color Saver 保留完整 packed value | 真机横竖屏往返后保留开启强调色、蓝色选择、关闭阴影；设置区域像素一致 |
| 主题默认值 | `GlassConfig.Default` 改为 getter，避免 Presets 初始化期间捕获 null | common 单元测试和默认参数 UI 调用 |
| 绘制缓存 | 缓存阴影 Outline/Path/Stroke、Android Paint/filter、基底渐变和七段数字路径；Tactile 使用平铺纹理、可跨重组复用的倒角图层，单个 RGBA 缓存限制 4 MiB | 真机前后截图与三轮帧数据对比；超限表面保留矢量绘制 |
| 主题映射 | spec/palette 对应配置与 Material ColorScheme 使用 remember 缓存 | 编译与现有主题测试 |
| SDK 交付 | 两模块发布根 metadata 和 Android artifact 到项目本地 Maven 仓库；tokens 同时支持 JVM 发布 | 独立 consumer 只声明顶层坐标即可编译组件和 tokens |
| API 保护 | 生成并审阅 tokens Android/JVM API 快照，现有 SDK Android API 保持不变 | 根 apiCheck 通过；Native 检查在 Windows 被跳过 |
| 文档/CI | 同步模块、版本、接入及迁移状态；增加 Android 测试/API/consumer/UI 与 macOS iOS 编译 workflow | 本地对应 Android 命令通过；远端 workflow 尚未执行 |

本轮仅执行本地开发与验证；未创建分支、推送或外部发布。提交状态以 Git 记录为准。

## 2. 实际验证

| 检查 | 结果 |
|:--|:--|
| `:app:testDebugUnitTest` | 104 个测试，0 failures / errors；包含 6 个新增异步回归 |
| `:designsystem:testDebugUnitTest` | 30 个测试，0 failures / errors；包含默认主题初始化回归 |
| `:designsystem-tokens:jvmTest` | 27 个测试，0 failures / errors；本轮复用缓存，tokens 源码未修改 |
| `:designsystem:connectedDebugAndroidTest` | PLJ110、Android 17 / API 37 真机，8 个测试，0 failures / errors / skipped |
| `apiCheck` | SDK Android 与 tokens Android/JVM 检查通过；iOS KLib 检查被宿主限制跳过 |
| `:app:assembleDebug` | 成功生成 Debug APK |
| 本地 Maven 发布 | SDK 根/Android、tokens 根/Android/JVM 均成功；仓库在 `build/verification-maven` |
| `-p fixtures/android-consumer assembleDebug` | 最终 SDK 产物下编译成功；独立 Gradle build，无 project dependency 或 includeBuild 替换 |
| `git diff --check` | 通过 |

本轮真机测试发现旧 Espresso 反射调用 `InputManager.getInstance()` 失败。
将测试依赖升到 Espresso 3.7.0 后恢复运行；官方修复说明见
[AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)。
运行后进一步定位并修复默认主题循环初始化，以及 Checkbox 状态位于子交互节点导致外层语义不完整的问题。

本地报告与日志（build 目录不提交）：

- 应用测试：`app/build/reports/tests/testDebugUnitTest/index.html`
- SDK 测试：`designsystem/build/reports/tests/testDebugUnitTest/index.html`
- 真机 UI 测试：`designsystem/build/reports/androidTests/connected/debug/index.html`
- 最终主工程验证：`.build-tmp-local/continued-final.log`
- 最终 consumer 验证：`.build-tmp-local/continued-consumer.log`

## 3. 发布约定

顶层坐标：`top.hasiy:hasiy-design-system-compose:2.0.0`。
Android target 坐标：`top.hasiy:hasiy-design-system-compose-android:2.0.0`。
tokens 保持独立版本 `top.hasiy:designsystem-tokens:1.0.0`，由 metadata 选择对应 Android/JVM target。
Native target 保留 KMP 默认派生坐标；macOS 发布时须与根 metadata 一起验证。

验证命令见 [BUILDING.md](BUILDING.md)。本地 consumer 可复用第三方依赖缓存，
不等同于空缓存、正式外部 Maven 仓库或独立运行验收。

## 4. Tactile 真机对照

同一 PLJ110 / API 37 真机、Debug 构建、竖屏 Tactile 主题、默认 3 列指标网格。
优化前 APK 从 HEAD（`c71cf88`）的独立源码副本构建，未切换或覆盖当前工作区。
每版先执行 10 次切组预热，然后执行 3 轮，每轮点击同一个切组按钮 20 次，间隔 0.4 秒；
每轮 reset gfxinfo 后采集 gfxinfo、framestats 和 meminfo。每轮得到 30 帧。

| 指标（3 轮统计值的中位数） | 优化前 | 最终倒角图层缓存版本 |
|:--|--:|--:|
| 帧耗时 P50 | 65 ms | 53 ms |
| 帧耗时 P90 | 77 ms | 61 ms |
| UI draw 阶段中位耗时（SyncQueued − DrawStart） | 23.785 ms | 16.223 ms |
| Issue draw 阶段中位耗时（SwapBuffers − IssueDrawCommandsStart） | 12.860 ms | 5.520 ms |
| gfxinfo 卡顿帧比例 | 66.67% | 66.67% |

P50/P90 分别下降约 18%/21%，但该结果仅适用于此次 Debug 页面切组采样；仍有明显卡顿，
不能外推为全应用或 Release 性能结论。未建立生产环境功耗或长期内存稳定性结论。

最初尝试将所有纹理点合并为大型 Path，实测 P50/P90 变为 85/93 ms，发生性能回退，
已移除该实现。最终采用小图块重复 Shader，加上倒角图层复用；硬件缓存与软件栅格化
存在轻微像素差异，内容区域平均 RGB 差为 0.028/255、最大差 6/255，截图未见明显视觉回退。
旋转测试的强调色/阴影设置区域则与旋转前完全一致。

原始证据在 `.build-tmp-local/device-qa/`：

- `baseline-tactile.png` 与 `bevel-tactile.png`：前后画面。
- `before-gfxinfo-*.txt`、`bevel-gfxinfo-*.txt`：三轮统计。
- `before-framestats-*.txt`、`bevel-framestats-*.txt`：帧阶段时间。
- `performance-summary.json`：包含基线、已淘汰候选与最终版本的统计。
- `rotation-before/landscape/after.xml` 和同名 PNG：旋转状态与画面。

设备系统旋转设置已恢复原值（accelerometer_rotation=1、user_rotation=0）；设备保留优化后的 APK。

## 5. 剩余验收范围

- Windows 无法执行 iOS 编译、测试或 Native ABI 快照生成；新增 macOS workflow 仅配置了源码/测试编译，尚未在远端执行。
- iOS 真机和外部消费者、API 26–30 模糊回退仍需分别验收。
- 已采集此次 Tactile 切组帧数据；其余页面、Release、不同 DPI 与设备仍需独立验收，尚无分配次数或长期内存剖析。
- 既有 explicit API 警告与 Gradle 10 兼容性弃用提示仍存在，未将清理全部公共声明或升级构建工具扩大到本次优化。
