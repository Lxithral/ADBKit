# AGENTS

## 构建与安装（强制惯例）

- 每轮代码改动验证通过后，**直接构建并安装，不要再询问**：
  1. `./gradlew.bat :app:assembleRelease`（设备上装的是 release 签名版，debug 签名会 INSTALL_FAILED_UPDATE_INCOMPATIBLE）
  2. `adb connect 192.168.0.112:40049`（已配对过；USB 已连接则直接用 USB 设备）
  3. `adb install -r app/build/outputs/apk/release/app-release.apk`
- 测试设备：小米手机 HyperOS 4.0 / Android 17（API 37）。无线调试端口会随开关/重启变化；`40049` 失效时用 `adb mdns services` 自动发现新端口（服务名 `adb-<id>-xxx._adb-tls-connect._tcp`），不要盲目重试旧端口。
- 安装后如需验证 UI 行为，可用 `adb shell am start` + `adb exec-out screencap -p` 辅助；Git Bash 里设备路径要加 `MSYS_NO_PATHCONV=1`。
- 开发者选项「无线调试」条目的 preference key 是 `toggle_adb_wireless`（非 `wireless_debugging`），高亮跳转写法见 AdbScreen.kt 快捷入口。

## 其他

- 仓库无 CI；资源/图标改动用 `:app:processDebugResources` 快速验证，行为改动用 `assembleDebug` + 真机。
