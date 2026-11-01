package com.lxithral.adbtools.quick

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lxithral.adbtools.MainActivity
import com.lxithral.adbtools.R
import com.topjohnwu.superuser.Shell

/**
 * 桌面小部件：每个设置一格，点按即切换（移植自 DevSwitch）。
 * Provider 必须对桌面导出，所以它只负责绘制；实际切换走私有的 [WidgetToggleReceiver]。
 */
class SwitchesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // render 要起 Root shell，放后台线程避免阻塞主线程
        Thread { manager.updateAppWidget(ids, render(context)) }.start()
    }

    /** 语言变化时重绘文字。 */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            Thread { updateAll(context) }.start()
        } else {
            super.onReceive(context, intent)
        }
    }

    private class Cell(
        val target: QuickToggle.Target,
        val cell: Int,
        val fill: Int,
        val icon: Int,
        val label: Int,
        val textRes: Int,
    )

    companion object {
        private val cells = listOf(
            Cell(
                QuickToggle.Target.DEVELOPER_OPTIONS,
                R.id.cell_developer, R.id.fill_developer, R.id.icon_developer,
                R.id.label_developer, R.string.widget_short_developer,
            ),
            Cell(
                QuickToggle.Target.USB_DEBUGGING,
                R.id.cell_usb, R.id.fill_usb, R.id.icon_usb,
                R.id.label_usb, R.string.widget_short_usb,
            ),
            Cell(
                QuickToggle.Target.WIRELESS_DEBUGGING,
                R.id.cell_wireless, R.id.fill_wireless, R.id.icon_wireless,
                R.id.label_wireless, R.string.widget_short_wireless,
            ),
        )

        /** 重绘所有已放置的小部件；没有放置时是空操作。需在后台线程调用。 */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SwitchesWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, render(context))
        }

        private fun render(context: Context): RemoteViews {
            Shell.getShell()
            val views = RemoteViews(context.packageName, R.layout.widget_switches)
            for (cell in cells) {
                val on = QuickToggle.isEnabled(context, cell.target)
                // 从这里设置文字，使标签跟随应用自身语言
                views.setTextViewText(cell.label, context.getString(cell.textRes))
                // 布局里的 selector 依据 enabled 状态着色；格子本身保持 enabled 以继续接收点按
                for (id in intArrayOf(cell.fill, cell.icon, cell.label)) views.setBoolean(id, "setEnabled", on)
                views.setContentDescription(
                    cell.cell,
                    "${cell.target.label}，${if (on) "已开启" else "已关闭"}"
                )
                views.setOnClickPendingIntent(cell.cell, click(context, cell.target))
            }
            return views
        }

        /** 没有 Root 时点按打开应用；否则把切换交给私有 receiver。 */
        private fun click(context: Context, target: QuickToggle.Target): PendingIntent {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            if (Shell.isAppGrantedRoot() != true) {
                val open = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return PendingIntent.getActivity(context, 0, open, flags)
            }
            val toggle = Intent(context, WidgetToggleReceiver::class.java)
                .putExtra(WidgetToggleReceiver.EXTRA_TARGET, target.name)
            return PendingIntent.getBroadcast(context, target.ordinal + 1, toggle, flags)
        }
    }
}
