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
     * - Android 11~16：`service.adb.tls.port` 由 adbd 写入，全局可读（系统设置页也读它）；
     * - Android 17+/HyperOS 实测（23013RK75C）：该属性不再写入，端口只出现在
     *   `dumpsys adb` 的 `adb_wifi{ tls_port=… }` 里（需要 root，普通 app 无 DUMP 权限）。
     */
    fun getPort(context: Context): String {
        for (prop in listOf("service.adb.tls.port", "service.adb.tcp.port")) {
            val value = Shell.cmd("getprop $prop").exec().out.firstOrNull()?.trim()
            if (!value.isNullOrEmpty() && value != "0" && value != "-1") return value
        }
        val dump = Shell.cmd("dumpsys adb").exec().out.joinToString("\n")
        return Regex("""tls_port=(\d+)""").find(dump)?.groupValues?.get(1) ?: ""
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
