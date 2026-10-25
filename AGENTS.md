# AGENTS

## 项目概览

ADBKit：单模块 Android 应用（`:app`，namespace `com.lxithral.adbtools`），Root 一键开关 ADB 相关调试选项 + 5 个快捷磁贴。技术栈：Kotlin 2.2 + Jetpack Compose、miuix 组件库（`top.yukonga.miuix.kmp` 0.9.4，经 JitPack）、libsu（Root shell）；minSdk 33 / targetSdk 36 / compileSdk 37，JVM 17。

目录结构（都在 `app/src/main/java/com/lxithral/adbtools/`）：

- `logic/` — Shell 命令执行与状态读写（`AdbManager`、`WirelessDebugging`、`PrivilegeLevel`、`Utilities`）；UI 不直接拼 shell 命令
- `tile/` — `BaseTileService` + 5 个 QS 磁贴；磁贴触发 10 秒自毁计时，被 SystemUI 重拉起会重新武装（改动前先读 `BaseTileService` 的 kill 逻辑，勿破坏）
- `ui/` — Compose 界面（`AdbScreen`、`AdbViewModel`、`component/liquid/` 液态玻璃 RuntimeShader、`theme/`）
- `memory/` — 公平内存机制（`FairMemoryReceiver`）
- `android/debug/IAdbManager.java` — 编译期隐藏 API stub（`getAdbWirelessPort`），包名必须是 `android.debug`，用于反射调用系统服务

## 构建与安装（强制惯例）

- 每轮代码改动验证通过后，**直接构建并安装，不要再询问**：
  1. `./gradlew.bat :app:assembleRelease`（设备上装的是 release 签名版，debug 签名会 INSTALL_FAILED_UPDATE_INCOMPATIBLE）
  2. `adb connect 192.168.0.112:40049`（已配对过；USB 已连接则直接用 USB 设备）
  3. `adb install -r app/build/outputs/apk/release/app-release.apk`
- 测试设备：小米手机 HyperOS 4.0 / Android 17（API 37）。无线调试端口会随开关/重启变化；`40049` 失效时用 `adb mdns services` 自动发现新端口（服务名 `adb-<id>-xxx._adb-tls-connect._tcp`），不要盲目重试旧端口。
- 安装后如需验证 UI 行为，可用 `adb shell am start` + `adb exec-out screencap -p` 辅助；Git Bash 里设备路径要加 `MSYS_NO_PATHCONV=1`。
- 开发者选项「无线调试」条目的 preference key 是 `toggle_adb_wireless`（非 `wireless_debugging`），高亮跳转写法见 AdbScreen.kt 快捷入口。

## 工程注意事项

- 仓库无 CI；资源/图标改动用 `:app:processDebugResources` 快速验证，行为改动用 `assembleDebug` + 真机。
- Release 签名凭据从 `~/.gradle/keystore.properties` 读取（storeFile/storePassword/keyAlias/keyPassword），不入库；缺失时 assembleRelease 走 placeholder 签名。
- R8 keep 规则在 `app/src/main/keepRules/rules.keep`（AGP 自动合并），不是 `app/proguard-rules.pro`；release 开启 minify + shrinkResources。
- 项目路径含中文，依赖 `gradle.properties` 里 `android.overridePathCheck=true`，勿删。
- 版本号手动维护：`app/build.gradle.kts` 的 `versionCode`/`versionName`，发版惯例是 `chore(release): vX.Y.Z` 提交 + README 更新日志同步。
- `work/` 目录是图标等临时工作文件，与构建无关。
