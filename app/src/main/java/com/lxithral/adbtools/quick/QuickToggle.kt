package com.lxithral.adbtools.quick

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import com.lxithral.adbtools.logic.WirelessDebugging
import com.lxithral.adbtools.logic.executeShellCommand
import com.topjohnwu.superuser.Shell

/**
 * 供桌面小部件与长按快捷方式在应用界面之外切换单个开关（移植自 DevSwitch）。
 * 内部会启动 Root shell，必须在后台线程调用。
 */
object QuickToggle {

    enum class Target(val key: String, val label: String) {
        DEVELOPER_OPTIONS("development_settings_enabled", "开发者选项"),
        USB_DEBUGGING("adb_enabled", "USB 调试"),
        WIRELESS_DEBUGGING("adb_wifi_enabled", "无线调试"),
    }

    fun isEnabled(context: Context, target: Target): Boolean = when (target) {
        Target.WIRELESS_DEBUGGING -> WirelessDebugging.getEnabled(context)
        else -> executeShellCommand("settings get global ${target.key}", context) == "1"
    }

    /** 翻转 [target]，并用 Toast 告知结果。 */
    fun flip(context: Context, target: Target) {
        val appContext = context.applicationContext

        Shell.getShell()
        if (Shell.isAppGrantedRoot() != true) {
            toast(appContext, "需要 Root 权限")
            return
        }

        // 锁屏时不允许从快捷入口打开调试通道（移植自 DevSwitch）
        val keyguard = appContext.getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) {
            toast(appContext, "请先解锁屏幕")
            return
        }

        val turnOn = !isEnabled(appContext, target)
        when (target) {
            Target.DEVELOPER_OPTIONS -> {
                executeShellCommand(
                    "settings put global ${target.key} ${if (turnOn) 1 else 0}",
                    appContext
                )
                // 与系统设置一致：关闭开发者选项时级联关闭两类调试（移植自 DevSwitch）
                if (!turnOn) {
                    executeShellCommand("settings put global adb_enabled 0", appContext)
                    WirelessDebugging.setEnabled(appContext, false)
                }
            }

            Target.USB_DEBUGGING ->
                executeShellCommand(
                    "settings put global ${target.key} ${if (turnOn) 1 else 0}",
                    appContext
                )

            Target.WIRELESS_DEBUGGING ->
                WirelessDebugging.setEnabled(appContext, turnOn)
        }

        toast(appContext, "已${if (turnOn) "开启" else "关闭"}${target.label}")
        SwitchesWidget.updateAll(appContext)
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
