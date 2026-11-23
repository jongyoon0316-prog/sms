package com.jongyun.smsforward

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager

/** 문자 수신 즉시 등록된 번호들로 전달하고 이력을 남긴다. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = Prefs(context)
        if (!prefs.enabled) return
        val targets = prefs.targets()
        if (targets.isEmpty()) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isNullOrEmpty()) return

        val sender = msgs[0].displayOriginatingAddress ?: "알 수 없음"
        val body = msgs.joinToString("") { it.displayMessageBody ?: "" }
        val text = "[$sender] $body"
        val now = System.currentTimeMillis()

        val db = LogDb(context)
        val sms = smsManager(context)

        for (target in targets) {
            val logId = db.insert(now, sender, body, target, "전송중")
            try {
                val parts = sms.divideMessage(text)
                val sentIntents = ArrayList<PendingIntent?>()
                for (i in parts.indices) {
                    val sent = Intent(context, SentReceiver::class.java).apply {
                        putExtra("logId", logId)
                    }
                    sentIntents.add(
                        PendingIntent.getBroadcast(
                            context,
                            (logId * 10 + i).toInt(),
                            sent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                }
                if (parts.size > 1) {
                    sms.sendMultipartTextMessage(target, null, parts, sentIntents, null)
                } else {
                    sms.sendTextMessage(target, null, text, sentIntents[0], null)
                }
            } catch (e: Exception) {
                db.updateStatus(logId, "실패(${e.javaClass.simpleName})")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()
}
