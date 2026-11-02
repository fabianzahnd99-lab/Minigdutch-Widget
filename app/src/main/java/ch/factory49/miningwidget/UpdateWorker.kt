package ch.factory49.miningwidget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Holt die Daten im Hintergrund und aktualisiert alle Widgets. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.isConfigured) {
            MiningWidgetProvider.updateAll(applicationContext)
            return Result.success()
        }
        return try {
            val stats = withContext(Dispatchers.IO) { MiningDutchApi.fetch(prefs) }
            store(prefs, stats)
            MiningWidgetProvider.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            prefs.lastError = e.message ?: e.javaClass.simpleName
            MiningWidgetProvider.updateAll(applicationContext)
            Result.success() // nächster Versuch beim nächsten Intervall
        }
    }

    companion object {
        private const val PERIODIC = "mining_periodic"
        private const val ONCE = "mining_once"

        fun store(prefs: Prefs, s: MiningStats) {
            prefs.lastHashrate = s.hashrateHs
            prefs.lastConfirmed = s.confirmed
            prefs.lastUnconfirmed = s.unconfirmed
            prefs.lastWorkersActive = s.workersActive
            prefs.lastWorkersTotal = s.workersTotal
            prefs.lastUpdate = System.currentTimeMillis()
            prefs.lastError = ""
        }

        private val netConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Alle 15 Minuten (Android-Minimum für Hintergrundarbeit). */
        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(15, TimeUnit.MINUTES)
                .setConstraints(netConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun runNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(netConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, req)
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
