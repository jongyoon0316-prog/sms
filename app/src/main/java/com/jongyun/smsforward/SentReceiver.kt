package com.jongyun.smsforward

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 문자 전송 결과(성공/실패)를 이력에 반영한다. */
class SentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra("logId", -1L)
        if (logId < 0) return
        val db = LogDb(context)

        if (resultCode == Activity.RESULT_OK) {
            if (db.getStatus(logId) == "전송중") db.updateStatus(logId, "전송완료")
        } else {
            val reason = when (resultCode) {
                android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "서비스 불가"
                android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "비행기모드"
                android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "PDU 오류"
                else -> "오류 $resultCode"
            }
            db.updateStatus(logId, "실패($reason)")
        }
    }
}
