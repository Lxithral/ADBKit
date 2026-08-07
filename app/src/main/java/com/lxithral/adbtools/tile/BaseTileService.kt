package com.lxithral.adbtools.tile

import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.lxithral.adbtools.MainApp
import com.topjohnwu.superuser.Shell

abstract class BaseTileService : TileService() {

    companion object {
        private const val TAG = "BaseTileService"
        private const val KILL_DELAY_MS = 10_000L
        private const val KILL_RETRY_DELAY_MS = 30_000L

        private val mainHandler = Handler(Looper.getMainLooper())
        private var pendingKill: Runnable? = null
        private var activeTileCount = 0

        fun cancelPendingKill() {
            pendingKill?.let {
                mainHandler.removeCallbacks(it)
                pendingKill = null
                Log.d(TAG, "Pending kill cancelled")
            }
        }

        private fun scheduleKill(delayMs: Long = KILL_DELAY_MS) {
            cancelPendingKill()
            val kill = Runnable {
                // 应用在前台、或有磁贴正在可见时不自杀，延迟重试
                if (MainApp.isMainActivityInForeground || activeTileCount > 0) {
                    Log.d(TAG, "In use (foreground=${MainApp.isMainActivityInForeground}, activeTiles=$activeTileCount), retry in ${KILL_RETRY_DELAY_MS / 1000}s")
                    scheduleKill(KILL_RETRY_DELAY_MS)
                    return@Runnable
                }
                Log.d(TAG, "Kill timeout reached, killing process")
                Process.killProcess(Process.myPid())
            }
            pendingKill = kill
            mainHandler.postDelayed(kill, delayMs)
            Log.d(TAG, "Kill scheduled in ${delayMs / 1000}s")
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 每次被 SystemUI 绑定（包括自杀后被其重新拉起）都重新武装自杀计时。
        // 自杀后若 SystemUI 仍持有磁贴绑定会再次拉起本服务并触发 onCreate，
        // 直到绑定最终解除，下一次自杀才真正成功，进程退出、不常驻后台。
        scheduleKill()
    }

    override fun onStartListening() {
        super.onStartListening()
        // 磁贴可见（面板展开）期间解除自杀，避免浏览面板时被杀
        activeTileCount++
        cancelPendingKill()
        qsTile?.let {
            it.icon = Icon.createWithResource(this, getIconRes())
            it.label = getTileLabel()
        }
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        if (activeTileCount > 0) activeTileCount--
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
