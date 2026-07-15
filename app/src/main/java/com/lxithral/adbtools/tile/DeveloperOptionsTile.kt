package com.lxithral.adbtools.tile

import android.os.Handler
import android.os.Looper
import com.lxithral.adbtools.R

class DeveloperOptionsTile : BaseTileService() {

    override fun getTileLabel(): String = "开发者选项"
    override fun getIconRes(): Int = R.drawable.tile_ic_developer_options

    override fun toggleFeature() {
        val enabled = isFeatureEnabled()
        val newState = if (enabled) "0" else "1"
        executeCommand("settings put global development_settings_enabled $newState")
        Handler(Looper.getMainLooper()).postDelayed({ updateTileState() }, 500)
    }

    override fun updateTileState() {
        updateTile(isFeatureEnabled())
    }

    private fun isFeatureEnabled(): Boolean {
        return executeCommand("settings get global development_settings_enabled") == "1"
    }
}
