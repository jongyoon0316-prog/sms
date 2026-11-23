package com.jongyun.smsforward

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var db: LogDb
    private lateinit var etNumber1: EditText
    private lateinit var etNumber2: EditText
    private lateinit var swEnabled: Switch
    private lateinit var tvStatus: TextView
    private lateinit var lvHistory: ListView

    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            refreshHistory()
            handler.postDelayed(this, 3000)
        }
    }
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        db = LogDb(this)
        etNumber1 = findViewById(R.id.etNumber1)
        etNumber2 = findViewById(R.id.etNumber2)
        swEnabled = findViewById(R.id.swEnabled)
        tvStatus = findViewById(R.id.tvStatus)
        lvHistory = findViewById(R.id.lvHistory)

        etNumber1.setText(prefs.number1)
        etNumber2.setText(prefs.number2)
        swEnabled.isChecked = prefs.enabled

        swEnabled.setOnCheckedChangeListener { _, checked ->
            prefs.enabled = checked
            updateStatus()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveNumbers() }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("포워딩 이력을 모두 삭제할까요?")
                .setPositiveButton("삭제") { _, _ ->
                    db.clear()
                    refreshHistory()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        handler.post(refresher)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresher)
    }

    private fun normalize(s: String) = s.replace(Regex("[\\s\\-()]"), "")

    private fun saveNumbers() {
        val n1 = normalize(etNumber1.text.toString())
        val n2 = normalize(etNumber2.text.toString())
        val ok = Regex("^\\+?\\d{8,15}$")
        if (!ok.matches(n1) || !ok.matches(n2)) {
            Toast.makeText(this, "번호 2개를 숫자로 정확히 입력해 주세요", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.number1 = n1
        prefs.number2 = n2
        etNumber1.setText(n1)
        etNumber2.setText(n2)
        Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun hasPermissions() =
        checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermissions() {
        if (!hasPermissions()) {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS), 100)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        updateStatus()
    }

    private fun updateStatus() {
        val t = prefs.targets()
        tvStatus.text = when {
            !hasPermissions() -> "⚠ 문자 권한이 필요합니다. 설정에서 문자(SMS) 권한을 허용해 주세요."
            t.size < 2 -> "⚠ 전달 번호 2개를 저장해 주세요."
            !prefs.enabled -> "전달 기능이 꺼져 있습니다."
            else -> "작동 중: 수신 문자를 ${t[0]}, ${t[1]} 로 전달합니다."
        }
    }

    private fun refreshHistory() {
        val rows = db.all().map {
            val body = if (it.body.length > 80) it.body.take(80) + "…" else it.body
            "${fmt.format(Date(it.time))}  [${it.status}]\n발신: ${it.sender} → 전달: ${it.target}\n$body"
        }
        lvHistory.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }
}
