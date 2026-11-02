package ch.factory49.miningwidget

import android.content.Context

/** Einstellungen und zuletzt geladene Werte (SharedPreferences). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mining_widget", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v.trim()).apply()

    var accountId: String
        get() = sp.getString("account_id", "") ?: ""
        set(v) = sp.edit().putString("account_id", v.trim()).apply()

    /** Pool- oder Algorithmus-Slug, z. B. "sha256", "bitcoin", "scrypt". */
    var pool: String
        get() = sp.getString("pool", "sha256") ?: "sha256"
        set(v) = sp.edit().putString("pool", v.trim().lowercase()).apply()

    /** Einheit, in der die API die Hashrate liefert (Index in HASH_UNITS). */
    var unitIndex: Int
        get() = sp.getInt("unit_index", 1) // Standard: kH/s (MPOS-Standard)
        set(v) = sp.edit().putInt("unit_index", v).apply()

    /** Bezeichnung für das Guthaben, z. B. "BTC". */
    var currency: String
        get() = sp.getString("currency", "BTC") ?: "BTC"
        set(v) = sp.edit().putString("currency", v.trim().uppercase()).apply()

    // ---- zuletzt geladene Werte ----
    var lastHashrate: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong("last_hashrate", 0L))
        set(v) = sp.edit().putLong("last_hashrate", java.lang.Double.doubleToRawLongBits(v)).apply()

    var lastConfirmed: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong("last_confirmed", 0L))
        set(v) = sp.edit().putLong("last_confirmed", java.lang.Double.doubleToRawLongBits(v)).apply()

    var lastUnconfirmed: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong("last_unconfirmed", 0L))
        set(v) = sp.edit().putLong("last_unconfirmed", java.lang.Double.doubleToRawLongBits(v)).apply()

    var lastWorkersActive: Int
        get() = sp.getInt("last_workers_active", -1)
        set(v) = sp.edit().putInt("last_workers_active", v).apply()

    var lastWorkersTotal: Int
        get() = sp.getInt("last_workers_total", -1)
        set(v) = sp.edit().putInt("last_workers_total", v).apply()

    var lastUpdate: Long
        get() = sp.getLong("last_update", 0L)
        set(v) = sp.edit().putLong("last_update", v).apply()

    var lastError: String
        get() = sp.getString("last_error", "") ?: ""
        set(v) = sp.edit().putString("last_error", v).apply()

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        val HASH_UNITS = listOf("H/s", "kH/s", "MH/s", "GH/s", "TH/s")
        val HASH_FACTORS = listOf(1.0, 1e3, 1e6, 1e9, 1e12)
    }
}
