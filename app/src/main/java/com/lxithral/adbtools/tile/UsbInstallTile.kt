package com.lxithral.adbtools.tile

import android.os.Handler
import android.os.Looper
import com.lxithral.adbtools.R

class UsbInstallTile : BaseTileService() {

    override fun getTileLabel(): String = "USB 安装"
    override fun getIconRes(): Int = R.drawable.tile_ic_usb_install

    override fun toggleFeature() {
        val enabled = isFeatureEnabled()
        val newState = if (enabled) "0" else "1"
        executeCommand("setprop persist.security.adbinstall $newState")
        Handler(Looper.getMainLooper()).postDelayed({ updateTileState() }, 500)
    }

    override fun updateTileState() {
        updateTile(isFeatureEnabled())
    }

    private fun isFeatureEnabled(): Boolean {
        return executeCommand("getprop persist.security.adbinstall") == "1"
    }
}
