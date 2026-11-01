package com.lxithral.adbtools.quick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 接收小部件格子的点按；只有本应用自己的 PendingIntent 能到达（未导出）。 */
class WidgetToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val target = QuickToggle.Target.entries
            .firstOrNull { it.name == intent.getStringExtra(EXTRA_TARGET) } ?: return
        val pending = goAsync()
        Thread {
            try {
                QuickToggle.flip(context, target)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_TARGET = "target"
    }
}
