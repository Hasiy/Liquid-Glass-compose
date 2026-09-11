# 编译、打包与安装注意事项

本文记录在 Windows 上编译本项目、生成 Debug APK 并安装到 Android 真机时需要特别注意的事项。命令默认在项目根目录执行。

## 1. 已验证的构建基线

| 项目 | 版本或要求 |
|---|---|
| Gradle Wrapper | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| 推荐构建 JDK | JDK 21 |
| Windows Shell | PowerShell |

Windows 下应使用 `gradlew.bat`：

```powershell
.\gradlew.bat --version
```

执行构建前先确认输出中的 `Launcher JVM` 与 `Daemon JVM`。本项目已验证可使用 Oracle JDK 21.0.11。不要无条件使用 Android Studio 自带的 JBR；当前开发机上的 JBR 是 Java 25.0.2，Gradle 8.9 的 Kotlin DSL 会在配置阶段报以下错误：

```text
java.lang.IllegalArgumentException: 25.0.2
```

这个错误属于 Gradle/JDK 版本兼容问题，与项目 Kotlin 源码无关。

如需为当前 PowerShell 会话明确指定 JDK 21：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
.\gradlew.bat --version
```

不要把本机示例路径复制到其他电脑后直接使用；应改为那台电脑实际安装的 JDK 21 路径。

## 2. 常用构建命令

只生成 demo Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug
```

产物位置：

```text
app\build\outputs\apk\debug\app-debug.apk
```

生成并安装 demo Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug :app:installDebug
```

只编译设计系统 Release AAR：

```powershell
.\gradlew.bat :designsystem:assembleRelease
```

需要完整错误链时增加 `--stacktrace`；只有在普通日志不足以定位时才使用 `--info` 或 `--debug`，避免产生大量噪声：

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace
```

## 3. 多设备连接时必须指定目标

先检查在线设备：

```powershell
adb devices -l
```

当同时连接手机、平板或模拟器时，不要直接执行 `installDebug`，否则 Gradle 可能拒绝安装或安装到非预期设备。为当前 PowerShell 会话指定序号：

```powershell
$env:ANDROID_SERIAL = '<adb-device-serial>'
.\gradlew.bat :app:installDebug
```

也可以先构建，再通过 ADB 明确安装：

```powershell
adb -s <adb-device-serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
```

`-r` 表示保留应用数据并覆盖安装。若签名发生变化，系统可能拒绝覆盖；除非明确允许清除手机上的旧应用及其数据，否则不要直接执行卸载。

## 4. `Selector.open()` / loopback 故障

### 典型错误

```text
java.io.IOException: Unable to establish loopback connection
    at java.base/sun.nio.ch.PipeImpl...
    at java.base/sun.nio.ch.WEPollSelectorImpl...
    at java.base/java.nio.channels.Selector.open...
```

有时底层原因还会出现：

```text
java.net.SocketException: Invalid argument: connect
    at java.base/sun.nio.ch.UnixDomainSockets.connect0...
```

### 如何判断

这个异常发生在 Gradle client 建立内部 NIO Selector/wakeup pipe 时，通常早于项目配置和 Kotlin 编译。因此：

- 它不是业务源码的编译错误。
- Gradle daemon 日志出现 `Accepted connection from /127.0.0.1...`，说明对应的 client/daemon loopback TCP 连接曾经成功，不能据此诊断为“防火墙完全阻断 loopback”。
- 手工 loopback socket 和 `Pipe.open()` 成功，但 `Selector.open()` 失败时，故障范围已经收窄到 Windows Selector 的特殊初始化路径。
- 本机安全软件或端口扫描抢先连接临时监听端口，是 TCP secret 握手异常的强候选原因；在没有抓到抢连进程、异常 secret 或停用对应扫描后恢复的证据前，应写成候选根因，而不是已证明事实。
- 如果堆栈明确落在 `UnixDomainSockets.connect0`，则实际失败的是 AF_UNIX 路径，不能用 TCP 端口抢连直接解释；应继续检查启动环境、临时目录和具体 JDK 实现。

以下设置已确认不能作为通用修复：

- `-Djava.net.preferIPv4Stack=true`
- 仅更换 Oracle JDK/JBR
- 仅清除 Gradle 缓存
- 在未核对调用栈前笼统调整防火墙规则

### 当前开发机验证过的绕行组合

本次成功构建使用了以下组合：

1. Oracle JDK 21；
2. 在受限沙箱之外启动 Gradle；
3. 为该次进程提供短且可写的 `TEMP` / `TMP`；
4. 明确指定目标设备。

示例：

```powershell
$buildTemp = Join-Path $PWD '.build-tmp-local'
New-Item -ItemType Directory -Force -Path $buildTemp | Out-Null

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = $buildTemp
$env:TMP = $buildTemp
$env:ANDROID_SERIAL = '<adb-device-serial>'

.\gradlew.bat :app:assembleDebug :app:installDebug --stacktrace
```

这是一个已经成功的组合，但单次成功不能证明四项中的哪一项单独解决了问题。尤其不要因此把 Selector 故障重新归结为普通防火墙阻断或单纯的临时目录长度问题。

构建结束后，先让对应 Gradle/Kotlin 进程退出，再删除 `.build-tmp-local`。若目录中的 `hsperfdata` 正被 JVM 占用，Windows 会拒绝删除；不要为了清理目录而结束无法确认来源的 Java 进程。

## 5. Gradle daemon 注意事项

不同 JDK 启动的 daemon 互不兼容，出现以下提示通常不是失败：

```text
1 incompatible Daemon could not be reused
```

可查看 daemon 状态：

```powershell
.\gradlew.bat --status
```

Android Studio 正在使用 Gradle 时，不要随意执行全局 `--stop` 或批量结束所有 `java.exe`，这可能中断 IDE 的同步或编译。确实需要清理时，应先确认 PID、JDK 路径、启动时间和命令行，只处理属于本次构建的进程。

Android Studio 内能编译、外部 PowerShell 失败，也不代表外部命令的源码或 Gradle task 不同。IDE 可能复用了在干扰发生前已经成功启动的 daemon，而新的命令行 client 仍需重新创建 Selector。

## 6. Windows 上可忽略与不可忽略的警告

### Kotlin/Native iOS targets disabled

```text
The following Kotlin/Native targets cannot be built on this machine and are disabled:
iosArm64, iosSimulatorArm64, iosX64
```

这是 Windows 无法构建 iOS/Native target 的预期提示。只构建 Android demo 或 JVM token 模块时可以忽略；它不等同于 iOS 产物已经通过验证。

### SDK XML version warning

```text
This version only understands SDK XML versions up to 3 but an SDK XML file of version 4 was encountered.
```

这表示 Android Studio 与 Android SDK command-line tools 的版本存在差距。本次 Debug APK 仍可成功生成和安装，因此当前是非阻断警告；若随后出现 SDK 解析、资源处理或 lint 异常，应优先对齐 Android Studio、SDK Command-line Tools 和 AGP，而不是修改业务代码。

### Restricted method warning

较新 JDK 可能提示 `java.lang.System::load` 是 restricted method。这是 Gradle/native-platform 与新 JDK 的前向兼容提示。不要为了消除该提示强行改用当前项目不能解析的 JDK 25；以项目可工作的 JDK 21 为准。

## 7. 安装后的验证

确认包已安装：

```powershell
adb -s <adb-device-serial> shell pm path top.hasiyliquidglassdemo
```

确认版本：

```powershell
adb -s <adb-device-serial> shell dumpsys package top.hasiyliquidglassdemo |
    Select-String -Pattern 'versionCode=|versionName='
```

冷启动并等待结果：

```powershell
adb -s <adb-device-serial> shell am force-stop top.hasiyliquidglassdemo
adb -s <adb-device-serial> shell am start -W -n top.hasiyliquidglassdemo/.MainActivity
```

成功时应至少看到：

```text
Status: ok
Activity: top.hasiyliquidglassdemo/.MainActivity
Complete
```

确认应用处于前台：

```powershell
adb -s <adb-device-serial> shell dumpsys activity activities |
    Select-String -Pattern 'topResumedActivity'
```

## 8. 最小验收清单

- `gradlew.bat --version` 显示 JDK 21。
- `:app:assembleDebug` 返回 `BUILD SUCCESSFUL`。
- APK 存在于 `app\build\outputs\apk\debug\app-debug.apk`。
- 多设备场景已明确设置 `ANDROID_SERIAL`。
- 安装输出为 `Installed on 1 device`，或 ADB 返回 `Success`。
- `pm path` 能找到 `top.hasiyliquidglassdemo`。
- `am start -W` 返回 `Status: ok`。
- `topResumedActivity` 指向 `top.hasiyliquidglassdemo/.MainActivity`。
- 未把 Windows 上被禁用的 iOS target 提示误写成跨平台构建成功。
- 未清理或覆盖工作区内与本次构建无关的改动。
