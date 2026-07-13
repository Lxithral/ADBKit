package com.lxithral.adbtools.logic

import android.content.Context

object AdbManager {

    /**
     * 获取 USB 调试状态
     */
    fun isUsbAdbEnabled(context: Context): Boolean {
        val result = executeShellCommand("settings get global adb_enabled", context).trim()
        return result == "1"
    }

}
