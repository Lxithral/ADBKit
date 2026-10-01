# AGENTS

## 构建与安装（强制惯例）

- 每轮代码改动验证通过后，**直接构建并安装，不要再询问**：
  1. `./gradlew.bat :app:assembleDebug`
  2. `adb connect 192.168.0.112:40049`，失败换 `adb connect 172.19.0.1:40049`（USB 已连接则直接用 USB 设备）
  3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- 测试设备：小米手机，无线调试端口会随开关/重启变化；`40049` 失效时先向用户要新端口或从 App 内「无线调试」卡片读当前地址，不要盲目重试旧端口。
- 安装后如需验证 UI 行为，可用 `adb shell am start` + `adb exec-out screencap -p` 辅助，最终以用户目视为准。

## 其他

- 仓库无 CI；资源/图标改动用 `:app:processDebugResources` 快速验证，行为改动用 `assembleDebug` + 真机。
