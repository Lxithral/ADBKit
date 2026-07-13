package com.lxithral.adbtools.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.topjohnwu.superuser.Shell

abstract class BaseTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.icon = Icon.createWithResource(this, getIconRes())
            it.label = getTileLabel()
        }
        updateTileState()
    }

    override fun onClick() {
        toggleFeature()
        updateTileState()
    }

    protected fun executeCommand(command: String): String {
        return Shell.cmd(command).exec().out.joinToString("\n").trim()
    }

    protected fun updateTile(active: Boolean) {
        qsTile?.let {
            it.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = getTileLabel()
            it.icon = Icon.createWithResource(this, getIconRes())
            it.updateTile()
        }
    }

    protected abstract fun toggleFeature()
    protected abstract fun updateTileState()
    protected abstract fun getTileLabel(): String
    protected abstract fun getIconRes(): Int
}
