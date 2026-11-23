package com.jongyun.smsforward

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LogItem(
    val id: Long,
    val time: Long,
    val sender: String,
    val body: String,
    val target: String,
    val status: String
)

/** 포워딩 이력 저장소 (SQLite) */
class LogDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "forward_log.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE log(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "time INTEGER NOT NULL," +
                "sender TEXT," +
                "body TEXT," +
                "target TEXT," +
                "status TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insert(time: Long, sender: String, body: String, target: String, status: String): Long {
        val cv = ContentValues().apply {
            put("time", time)
            put("sender", sender)
            put("body", body)
            put("target", target)
            put("status", status)
        }
        return writableDatabase.insert("log", null, cv)
    }

    fun getStatus(id: Long): String? {
        readableDatabase.rawQuery("SELECT status FROM log WHERE id=?", arrayOf(id.toString())).use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun updateStatus(id: Long, status: String) {
        val cv = ContentValues().apply { put("status", status) }
        writableDatabase.update("log", cv, "id=?", arrayOf(id.toString()))
    }

    fun all(limit: Int = 500): List<LogItem> {
        val list = ArrayList<LogItem>()
        readableDatabase.rawQuery(
            "SELECT id,time,sender,body,target,status FROM log ORDER BY id DESC LIMIT $limit", null
        ).use {
            while (it.moveToNext()) {
                list.add(
                    LogItem(
                        it.getLong(0), it.getLong(1),
                        it.getString(2) ?: "", it.getString(3) ?: "",
                        it.getString(4) ?: "", it.getString(5) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun clear() {
        writableDatabase.delete("log", null, null)
    }
}
