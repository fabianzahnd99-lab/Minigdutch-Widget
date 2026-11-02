package ch.factory49.miningwidget

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var edtKey: EditText
    private lateinit var edtId: EditText
    private lateinit var edtPool: EditText
    private lateinit var edtCurrency: EditText
    private lateinit var spnUnit: Spinner
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        edtKey = findViewById(R.id.edt_api_key)
        edtId = findViewById(R.id.edt_account_id)
        edtPool = findViewById(R.id.edt_pool)
        edtCurrency = findViewById(R.id.edt_currency)
        spnUnit = findViewById(R.id.spn_unit)
        txtResult = findViewById(R.id.txt_result)

        spnUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Prefs.HASH_UNITS)

        edtKey.setText(prefs.apiKey)
        edtId.setText(prefs.accountId)
        edtPool.setText(prefs.pool)
        edtCurrency.setText(prefs.currency)
        spnUnit.setSelection(prefs.unitIndex)

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            save()
            UpdateWorker.schedulePeriodic(this)
            UpdateWorker.runNow(this)
            Toast.makeText(this, "Gespeichert – Widget wird aktualisiert", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_test).setOnClickListener { test() }
    }

    private fun save() {
        prefs.apiKey = edtKey.text.toString()
        prefs.accountId = edtId.text.toString()
        prefs.pool = edtPool.text.toString().ifBlank { "sha256" }
        prefs.currency = edtCurrency.text.toString().ifBlank { "BTC" }
        prefs.unitIndex = spnUnit.selectedItemPosition
    }

    private fun test() {
        save()
        txtResult.text = "Frage Mining-Dutch ab …"
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { MiningDutchApi.fetch(prefs) } }
            result.onSuccess { s ->
                UpdateWorker.store(prefs, s)
                MiningWidgetProvider.updateAll(this@MainActivity)
                txtResult.text = buildString {
                    appendLine("✅ Verbindung OK")
                    appendLine("Hashrate: ${Format.hashrate(s.hashrateHs)}")
                    if (s.workersTotal >= 0) appendLine("Worker: ${s.workersActive}/${s.workersTotal} aktiv")
                    appendLine("Bestätigt: ${Format.coin(s.confirmed)} ${prefs.currency}")
                    appendLine("Unbestätigt: ${Format.coin(s.unconfirmed)} ${prefs.currency}")
                    appendLine()
                    appendLine("Vergleiche die Hashrate mit der Website. Stimmt sie um Faktor 1000 nicht, oben die API-Einheit anpassen.")
                    appendLine()
                    appendLine("— Rohdaten getuserstatus —")
                    appendLine(s.rawStatus.take(600))
                    appendLine()
                    appendLine("— Rohdaten getuserbalance —")
                    appendLine(s.rawBalance.take(400))
                    if (s.rawWorkers.isNotBlank()) {
                        appendLine()
                        appendLine("— Rohdaten getuserworkers —")
                        appendLine(s.rawWorkers.take(800))
                    }
                }
            }.onFailure { e ->
                txtResult.text = "❌ Fehler: ${e.message}"
            }
        }
    }
}
