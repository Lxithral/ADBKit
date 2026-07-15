package com.lxithral.adbtools.tile

import android.os.Handler
import android.os.Looper
import com.lxithral.adbtools.R

class WirelessDebuggingTile : BaseTileService() {

    override fun getTileLabel(): String = "无线调试"
    override fun getIconRes(): Int = R.drawable.tile_ic_wireless_debugging

    override fun toggleFeature() {
        val enabled = isFeatureEnabled()
        val newState = if (enabled) "0" else "1"
        executeCommand("settings put --user current global adb_wifi_enabled $newState")
        executeCommand("settings put secure adb_wifi_enabled $newState")
        Handler(Looper.getMainLooper()).postDelayed({ updateTileState() }, 500)
    }

    override fun updateTileState() {
        updateTile(isFeatureEnabled())
    }

    private fun isFeatureEnabled(): Boolean {
        val settingsValue = executeCommand("settings get global adb_wifi_enabled")
        if (settingsValue == "1") return true
        val port = executeCommand("getprop service.adb.tls.port")
        return port.isNotBlank() && port != "0" && port != "-1"
    }
}
