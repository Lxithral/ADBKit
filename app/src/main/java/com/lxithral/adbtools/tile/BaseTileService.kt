package com.lxithral.adbtools.tile

import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.topjohnwu.superuser.Shell

abstract class BaseTileService : TileService() {

    companion object {
        private const val TAG = "BaseTileService"
        private const val KILL_DELAY_MS = 10_000L

        private val mainHandler = Handler(Looper.getMainLooper())
        private var pendingKill: Runnable? = null

        fun cancelPendingKill() {
            pendingKill?.let {
                mainHandler.removeCallbacks(it)
                pendingKill = null
                Log.d(TAG, "Pending kill cancelled")
            }
        }

        private fun scheduleKill() {
            cancelPendingKill()
            val kill = Runnable {
                Log.d(TAG, "Kill timeout reached, killing process")
                Process.killProcess(Process.myPid())
            }
            pendingKill = kill
            mainHandler.postDelayed(kill, KILL_DELAY_MS)
            Log.d(TAG, "Kill scheduled in ${KILL_DELAY_MS / 1000}s")
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.icon = Icon.createWithResource(this, getIconRes())
            it.label = getTileLabel()
        }
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        scheduleKill()
    }

    override fun onClick() {
        Thread {
            toggleFeature()
            updateTileState()
            scheduleKill()
        }.start()
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
