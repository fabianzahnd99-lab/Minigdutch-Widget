package ch.factory49.miningwidget

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun hashrate(hs: Double): String {
        val units = listOf("H/s", "kH/s", "MH/s", "GH/s", "TH/s", "PH/s", "EH/s")
        var v = hs
        var i = 0
        while (v >= 1000.0 && i < units.lastIndex) {
            v /= 1000.0
            i++
        }
        val pattern = when {
            v >= 100 -> "%.0f"
            v >= 10 -> "%.1f"
            else -> "%.2f"
        }
        return String.format(Locale.GERMAN, pattern, v) + " " + units[i]
    }

    fun coin(v: Double): String = String.format(Locale.GERMAN, "%.8f", v)

    fun time(ms: Long): String =
        if (ms <= 0) "–" else SimpleDateFormat("HH:mm", Locale.GERMAN).format(Date(ms))
}
