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
        val port = Regex("""tls_port=(-?\d+)""").find(dump)?.groupValues?.get(1) ?: return ""
        // 坑：关闭无线调试后 dumpsys 会残留 tls_port=0/-1，必须视为无效，
        // 否则 getEnabled 的端口兜底会把关闭状态误判成开启（开关卡在 ON）
        return port.toIntOrNull()?.takeIf { it > 0 }?.toString() ?: ""
    }

    /**
     * 坑：手机开 VPN 后 activeNetwork 的地址是 VPN 网卡（如 172.19.0.1），外部设备路由不到。
     * 优先取 wlan 接口的 IPv4，取不到再退回任意 IPv4。
     */
    fun getAddress(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val candidates = connectivityManager.allNetworks.mapNotNull { net ->
            val caps = connectivityManager.getNetworkCapabilities(net) ?: return@mapNotNull null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@mapNotNull null
            val linkProperties = connectivityManager.getLinkProperties(net) ?: return@mapNotNull null
            val ip = linkProperties.linkAddresses
                .firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
                ?.address?.hostAddress ?: return@mapNotNull null
            ip to linkProperties.interfaceName
        }
        val chosen = candidates.firstOrNull { it.second?.startsWith("wlan") == true }
            ?: candidates.firstOrNull()
        return chosen?.first ?: "未连接 Wi-Fi"
    }
}
