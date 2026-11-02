package ch.factory49.miningwidget

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MiningStats(
    val hashrateHs: Double,        // in H/s (bereits umgerechnet)
    val confirmed: Double,
    val unconfirmed: Double,
    val workersActive: Int,
    val workersTotal: Int,
    val rawStatus: String,
    val rawBalance: String,
    val rawWorkers: String,
)

/**
 * Zugriff auf die Mining-Dutch Pool-API:
 * https://www.mining-dutch.nl/pools/{pool}.php?page=api&action=...&api_key=...&id=...
 */
object MiningDutchApi {

    private const val BASE = "https://www.mining-dutch.nl/pools/"

    fun fetch(prefs: Prefs): MiningStats {
        require(prefs.apiKey.isNotBlank()) { "Kein API-Key eingetragen" }
        val factor = Prefs.HASH_FACTORS.getOrElse(prefs.unitIndex) { 1e3 }

        val statusRaw = call(prefs, "getuserstatus")
        val balanceRaw = call(prefs, "getuserbalance")
        val workersRaw = runCatching { call(prefs, "getuserworkers") }.getOrNull()

        // getuserstatus -> data.hashrate
        val statusData = dataOf(statusRaw, "getuserstatus") as? JSONObject
            ?: throw IllegalStateException("Unerwartete Antwort (getuserstatus): ${statusRaw.take(200)}")
        val hashrate = num(statusData.opt("hashrate"))

        // getuserbalance -> data.confirmed / data.unconfirmed
        val balanceData = dataOf(balanceRaw, "getuserbalance") as? JSONObject
        val confirmed = num(balanceData?.opt("confirmed"))
        val unconfirmed = num(balanceData?.opt("unconfirmed"))

        // getuserworkers -> data = Array (oder Objekt) von Workern mit "hashrate"
        var active = -1
        var total = -1
        if (workersRaw != null) {
            val list = mutableListOf<JSONObject>()
            when (val w = dataOf(workersRaw, "getuserworkers")) {
                is JSONArray -> for (i in 0 until w.length()) w.optJSONObject(i)?.let(list::add)
                is JSONObject -> w.keys().forEach { k -> w.optJSONObject(k)?.let(list::add) }
            }
            total = list.size
            active = list.count { num(it.opt("hashrate")) > 0.0 }
        }

        return MiningStats(
            hashrateHs = hashrate * factor,
            confirmed = confirmed,
            unconfirmed = unconfirmed,
            workersActive = active,
            workersTotal = total,
            rawStatus = statusRaw,
            rawBalance = balanceRaw,
            rawWorkers = workersRaw ?: "",
        )
    }

    private fun call(prefs: Prefs, action: String): String {
        val sb = StringBuilder(BASE)
            .append(enc(prefs.pool)).append(".php?page=api&action=").append(action)
            .append("&api_key=").append(enc(prefs.apiKey))
        if (prefs.accountId.isNotBlank()) sb.append("&id=").append(enc(prefs.accountId))

        val conn = URL(sb.toString()).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", "MiningDutchWidget/1.0 (Android)")
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("HTTP $code bei $action: ${body.take(150)}")
            if (body.isBlank()) throw IllegalStateException("Leere Antwort bei $action")
            return body
        } finally {
            conn.disconnect()
        }
    }

    /** Liest {action: {data: ...}} oder {data: ...} – je nachdem, was der Pool liefert. */
    private fun dataOf(raw: String, action: String): Any? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) {
            throw IllegalStateException("Keine JSON-Antwort ($action): ${trimmed.take(150)}")
        }
        val root = JSONObject(trimmed)
        val wrapper = root.optJSONObject(action) ?: root
        return wrapper.opt("data")
    }

    private fun num(v: Any?): Double = when (v) {
        null, JSONObject.NULL -> 0.0
        is Number -> v.toDouble()
        else -> v.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
