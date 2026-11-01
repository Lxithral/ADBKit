package com.lxithral.adbtools.quick

import android.app.Activity
import android.os.Bundle

/**
 * 桌面长按快捷方式的目标。快捷方式只能启动 Activity，所以它没有界面：
 * 切换 intent action 指定的开关后立即结束（移植自 DevSwitch）。
 */
class ToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = when (intent?.action) {
            ACTION_TOGGLE_DEVELOPER_OPTIONS -> QuickToggle.Target.DEVELOPER_OPTIONS
            ACTION_TOGGLE_USB_DEBUGGING -> QuickToggle.Target.USB_DEBUGGING
            ACTION_TOGGLE_WIRELESS_DEBUGGING -> QuickToggle.Target.WIRELESS_DEBUGGING
            else -> null
        }
        if (target != null) {
            // QuickToggle 内部起 Root shell，放后台线程；进程不会在切换完成前退出
            Thread { QuickToggle.flip(this, target) }.start()
        }
        finish()
    }

    companion object {
        const val ACTION_TOGGLE_DEVELOPER_OPTIONS =
            "com.lxithral.adbtools.action.TOGGLE_DEVELOPER_OPTIONS"
        const val ACTION_TOGGLE_USB_DEBUGGING =
            "com.lxithral.adbtools.action.TOGGLE_USB_DEBUGGING"
        const val ACTION_TOGGLE_WIRELESS_DEBUGGING =
            "com.lxithral.adbtools.action.TOGGLE_WIRELESS_DEBUGGING"
    }
}
