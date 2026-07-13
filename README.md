# ADBKit

一个安卓 ADB 调试工具，需要 root 权限。

## 功能

- 开发者选项开关
- USB 调试开关
- USB 安装开关
- USB 调试安全设置开关
- 无线调试开关
- 快捷设置磁贴（下拉通知栏直接切换）
- 自动检测 IP 和端口

## 环境要求

- Android 13+（minSdk 33）
- 已 root（通过 libsu 获取权限）
- Kotlin + Jetpack Compose
- Miuix UI 组件库

## 构建

```bash
git clone https://github.com/Lxithral/ADBKit.git
cd ADBKit
./gradlew assembleDebug
```

APK 产出在 `app/build/outputs/apk/debug/`

## 技术栈

- [libsu](https://github.com/topjohnwu/libsu) - root shell 操作
- [Miuix](https://github.com/yukonga/miuix) - UI 组件

## License

MIT
