package com.cucho.qhawa.data.environment

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

internal interface ReadingHistory {
    fun save(reading: EnvironmentalReading)
    fun load(now: Long): List<EnvironmentalReading>
    fun forEach(from: Long, to: Long, action: (EnvironmentalReading) -> Unit)
}

/** Raw prototype readings are kept for 30 days. The UI loads the last sample of each minute. */
internal class EnvironmentHistoryStore(
    context: Context,
    databaseName: String = "qhawa_sensor_history.db",
) : SQLiteOpenHelper(context, databaseName, null, 1), ReadingHistory {
    private var lastPrunedMinute: Long? = null

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE samples (
                id INTEGER PRIMARY KEY, received_at INTEGER NOT NULL, minute INTEGER NOT NULL,
                pm25 REAL, pm10 REAL, temperature REAL, humidity REAL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX samples_received ON samples(received_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    override fun save(reading: EnvironmentalReading) {
        val minute = reading.timestamp / 60_000
        val values = ContentValues().apply {
            put("received_at", reading.timestamp)
            put("minute", minute)
            put("pm25", reading.pm)
            put("pm10", reading.pm10)
            put("temperature", reading.temperature)
            put("humidity", reading.humidity)
        }
        writableDatabase.transaction {
            insertOrThrow("samples", null, values)
            if (lastPrunedMinute != minute) {
                delete("samples", "received_at < ?", arrayOf((reading.timestamp - RETENTION_MS).toString()))
            }
        }
        lastPrunedMinute = minute
    }

    override fun load(now: Long): List<EnvironmentalReading> = buildList {
        readableDatabase.rawQuery(
            """
            SELECT received_at, pm25, temperature, humidity, pm10 FROM samples
            WHERE id IN (
                SELECT MAX(id) FROM samples WHERE received_at BETWEEN ? AND ? GROUP BY minute
            ) ORDER BY received_at, id
            """.trimIndent(),
            arrayOf((now - RETENTION_MS).toString(), now.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) add(cursor.reading())
        }
    }

    override fun forEach(from: Long, to: Long, action: (EnvironmentalReading) -> Unit) {
        readableDatabase.rawQuery(
            """
            SELECT received_at, pm25, temperature, humidity, pm10 FROM samples
            WHERE received_at BETWEEN ? AND ? ORDER BY received_at, id
            """.trimIndent(),
            arrayOf(maxOf(from, to - RETENTION_MS).toString(), to.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) action(cursor.reading())
        }
    }

    private fun Cursor.reading() = EnvironmentalReading(
        timestamp = getLong(0),
        pm = nullableDouble(1),
        temperature = nullableDouble(2),
        humidity = nullableDouble(3),
        pmLabel = "PM2.5",
        pm10 = nullableDouble(4),
    )

    private fun Cursor.nullableDouble(index: Int) = if (isNull(index)) null else getDouble(index)

    companion object {
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
