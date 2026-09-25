package com.lxithral.adbtools.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.net.Inet4Address

object WirelessDebugging {

    private const val TAG = "WirelessDebuggingFeature"

    fun getEnabled(context: Context): Boolean {
        val command = "settings get global adb_wifi_enabled"
        val result = executeShellCommand(command, context).trim()
        if (result == "1") return true

        // 兜底：检查端口属性（无需 Root）
        val isActive = getPort(context).isNotEmpty()

        Log.d(TAG, "getEnabled: settings=$result, isActive=$isActive")
        return isActive
    }

    fun setEnabled(context: Context, value: Boolean) {
        val state = if (value) 1 else 0
        executeShellCommand("settings put --user current global adb_wifi_enabled $state", context)
        executeShellCommand("settings put secure adb_wifi_enabled $state", context)
        Log.d(TAG, "setEnabled: $value")
    }

    /**
     * 无线调试端口。
     * `service.adb.tls.port` 由 adbd 在无线调试开启后写入（Android 11+），是全局可读的
     * 系统属性——系统设置页的"IP 地址和端口"同样读它，因此这里不需要 Root。
     */
    fun getPort(context: Context): String {
        val candidates = listOf("service.adb.tls.port", "service.adb.tcp.port")
        for (prop in candidates) {
            val value = Shell.cmd("getprop $prop").exec().out.firstOrNull()?.trim()
            if (!value.isNullOrEmpty() && value != "0" && value != "-1") return value
        }
        return ""
    }

    fun getAddress(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "未连接"
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!isWifi) return "未连接 Wi-Fi"

        return linkProperties?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress ?: "未知 IP"
    }
}
