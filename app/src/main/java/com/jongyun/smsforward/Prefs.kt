package com.jongyun.smsforward

import android.content.Context

/** 포워딩 대상 번호 2개 + 사용 여부 저장 */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    var number1: String
        get() = sp.getString("n1", "") ?: ""
        set(v) = sp.edit().putString("n1", v).apply()

    var number2: String
        get() = sp.getString("n2", "") ?: ""
        set(v) = sp.edit().putString("n2", v).apply()

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    fun targets(): List<String> = listOf(number1, number2).filter { it.isNotBlank() }
}
