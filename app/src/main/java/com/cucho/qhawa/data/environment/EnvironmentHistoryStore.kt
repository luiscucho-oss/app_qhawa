package com.cucho.qhawa.data.environment

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

interface ReadingHistory {
    fun save(reading: EnvironmentalReading)
    fun load(now: Long): List<EnvironmentalReading>
}

/** Stores received Wokwi samples locally, one latest sample per minute, for 30 days. */
class EnvironmentHistoryStore(context: Context) : SQLiteOpenHelper(context, "qhawa_wokwi_history.db", null, 1), ReadingHistory {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE samples (minute INTEGER PRIMARY KEY, received_at INTEGER NOT NULL, pm REAL, temperature REAL, humidity REAL, sequence INTEGER, uptime INTEGER, led TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized override fun save(reading: EnvironmentalReading) {
        val values = ContentValues().apply {
            put("minute", reading.timestamp / 60_000)
            put("received_at", reading.timestamp)
            put("pm", reading.pm)
            put("temperature", reading.temperature)
            put("humidity", reading.humidity)
            put("sequence", reading.sequence)
            put("uptime", reading.uptimeMs)
            put("led", reading.demoState)
        }
        writableDatabase.insertWithOnConflict("samples", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        writableDatabase.delete("samples", "received_at < ?", arrayOf((reading.timestamp - RETENTION_MS).toString()))
    }

    @Synchronized override fun load(now: Long): List<EnvironmentalReading> = buildList {
        readableDatabase.rawQuery("SELECT received_at,pm,temperature,humidity,sequence,uptime,led FROM samples WHERE received_at >= ? AND received_at <= ? ORDER BY received_at LIMIT 43201",
            arrayOf((now - RETENTION_MS).toString(), now.toString())).use { cursor ->
            while (cursor.moveToNext()) add(EnvironmentalReading(cursor.getLong(0),
                if (cursor.isNull(1)) null else cursor.getDouble(1),
                if (cursor.isNull(2)) null else cursor.getDouble(2),
                if (cursor.isNull(3)) null else cursor.getDouble(3), "PM2.5", true,
                cursor.getLong(4), cursor.getLong(5), cursor.getString(6)))
        }
    }
    companion object { const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 }
}
