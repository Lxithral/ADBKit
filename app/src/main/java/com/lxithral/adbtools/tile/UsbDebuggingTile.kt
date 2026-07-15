package com.lxithral.adbtools.tile

import android.os.Handler
import android.os.Looper
import com.lxithral.adbtools.R

class UsbDebuggingTile : BaseTileService() {

    override fun getTileLabel(): String = "USB 调试"
    override fun getIconRes(): Int = R.drawable.tile_ic_usb_debugging

    override fun toggleFeature() {
        val enabled = isFeatureEnabled()
        val newState = if (enabled) "0" else "1"
        executeCommand("settings put global adb_enabled $newState")
        Handler(Looper.getMainLooper()).postDelayed({ updateTileState() }, 500)
    }

    override fun updateTileState() {
        updateTile(isFeatureEnabled())
    }

    private fun isFeatureEnabled(): Boolean {
        return executeCommand("settings get global adb_enabled") == "1"
    }
}
