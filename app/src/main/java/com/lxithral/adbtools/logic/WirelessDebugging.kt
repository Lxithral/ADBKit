package com.lxithral.adbtools.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.net.Inet4Address

object WirelessDebugging {

    private const val TAG = "WirelessDebuggingFeature"

    /**
     * 获取当前开启状态
     * 1:1 同步原版逻辑并增加属性检查作为兜底
     */
    fun getEnabled(context: Context): Boolean {
        val command = "settings get global adb_wifi_enabled"
        val result = executeShellCommand(command, context).trim()
        
        // 原版逻辑：settings 值为 1
        if (result == "1") return true
        
        // 扩展逻辑：如果端口属性已经分配，说明服务其实是开启的（MIUI 状态同步可能延迟）
        val port = Shell.cmd("getprop service.adb.tls.port").exec().out.firstOrNull()?.trim()
        val isActive = !port.isNullOrBlank() && port != "0" && port != "-1"
        
        Log.d(TAG, "getEnabled: settings=$result, port=$port, isActive=$isActive")
        return isActive
    }

    /**
     * 设置开启状态
     */
    fun setEnabled(context: Context, value: Boolean) {
        val state = if (value) 1 else 0
        // 1:1 同步原版指令
        val command = "settings put --user current global adb_wifi_enabled $state"
        executeShellCommand(command, context)
        
        // 针对某些设备，如果 global 不生效，尝试也设置到 secure
        executeShellCommand("settings put secure adb_wifi_enabled $state", context)
        
        Log.d(TAG, "setEnabled: $value (command executed)")
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

    fun getConnectionData(context: Context): String =
        "${getAddress(context)}:${getPort(context)}"

    fun syncConnectionData(context: Context) {
        // Removed synchronization and automation logic as requested.
    }
}
