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

        // 兜底：检查端口属性
        val port = Shell.cmd("getprop service.adb.tls.port").exec().out.firstOrNull()?.trim()
        val isActive = !port.isNullOrBlank() && port != "0" && port != "-1"

        Log.d(TAG, "getEnabled: settings=$result, port=$port, isActive=$isActive")
        return isActive
    }

    fun setEnabled(context: Context, value: Boolean) {
        val state = if (value) 1 else 0
        executeShellCommand("settings put --user current global adb_wifi_enabled $state", context)
        executeShellCommand("settings put secure adb_wifi_enabled $state", context)
        Log.d(TAG, "setEnabled: $value")
    }

    fun getPort(context: Context): String =
        if (getPrivilegeLevel(PrivilegeLevel.Root, context) == PrivilegeLevel.Root) {
            Shell.cmd("getprop service.adb.tls.port").exec().out.firstOrNull()?.trim() ?: ""
        } else {
            ""
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
