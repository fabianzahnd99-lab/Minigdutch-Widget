package ch.factory49.miningwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class MiningWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
        UpdateWorker.schedulePeriodic(context)
    }

    override fun onEnabled(context: Context) {
        UpdateWorker.schedulePeriodic(context)
        UpdateWorker.runNow(context)
    }

    override fun onDisabled(context: Context) {
        UpdateWorker.cancelAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            showLoading(context)
            UpdateWorker.runNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "ch.factory49.miningwidget.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MiningWidgetProvider::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun showLoading(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MiningWidgetProvider::class.java))
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_mining)
                views.setTextViewText(R.id.txt_updated, "lädt …")
                manager.partiallyUpdateAppWidget(id, views)
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val p = Prefs(context)
            val v = RemoteViews(context.packageName, R.layout.widget_mining)

            v.setTextViewText(R.id.txt_title, "Mining-Dutch · ${p.pool.uppercase()}")

            if (!p.isConfigured) {
                v.setTextViewText(R.id.txt_hashrate, "—")
                v.setTextViewText(R.id.txt_workers, "Tippen zum Einrichten")
                v.setTextViewText(R.id.txt_balance, "")
                v.setTextViewText(R.id.txt_updated, "")
            } else if (p.lastUpdate == 0L && p.lastError.isBlank()) {
                v.setTextViewText(R.id.txt_hashrate, "…")
                v.setTextViewText(R.id.txt_workers, "")
                v.setTextViewText(R.id.txt_balance, "")
                v.setTextViewText(R.id.txt_updated, "lädt …")
            } else {
                v.setTextViewText(R.id.txt_hashrate, Format.hashrate(p.lastHashrate))
                v.setTextViewText(
                    R.id.txt_workers,
                    if (p.lastWorkersTotal >= 0) "Worker: ${p.lastWorkersActive}/${p.lastWorkersTotal} aktiv" else ""
                )
                v.setTextViewText(
                    R.id.txt_balance,
                    "${Format.coin(p.lastConfirmed)} ${p.currency}  (+${Format.coin(p.lastUnconfirmed)} unbest.)"
                )
                val updated = "Stand ${Format.time(p.lastUpdate)}"
                v.setTextViewText(
                    R.id.txt_updated,
                    if (p.lastError.isNotBlank()) "$updated · Fehler: ${p.lastError}" else updated
                )
            }

            // Warnfarbe, wenn Hashrate 0 ist (Miner offline?)
            val color = if (p.isConfigured && p.lastUpdate > 0 && p.lastHashrate <= 0.0) 0xFFFF6B6B.toInt() else 0xFFFFFFFF.toInt()
            v.setTextColor(R.id.txt_hashrate, color)
            v.setViewVisibility(R.id.txt_balance, if (p.isConfigured) View.VISIBLE else View.GONE)

            // Tippen auf Widget -> App öffnen
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)

            // Refresh-Knopf
            val refreshIntent = Intent(context, MiningWidgetProvider::class.java).setAction(ACTION_REFRESH)
            val refresh = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.btn_refresh, refresh)

            manager.updateAppWidget(id, v)
        }
    }
}
