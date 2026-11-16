package com.cucho.qhawa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cucho.qhawa.data.environment.EnvironmentHistoryStore
import com.cucho.qhawa.data.environment.EnvironmentalReading
import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentHistoryStoreTest {
    @Test fun keepsEveryRawSampleWhileShowingTheLastSampleOfEachMinute() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = "sensor-test-${System.nanoTime()}.db"
        val start = 1_800_000_000_000L
        val first = EnvironmentalReading(start + 1000, 1.1, 29.6, 72.0, "PM2.5", 1.3)
        val second = first.copy(timestamp = start + 4000, pm = 1.2, pm10 = 1.4)
        val third = first.copy(timestamp = start + 61_000, temperature = null, humidity = null)
        val expired = first.copy(timestamp = first.timestamp - EnvironmentHistoryStore.RETENTION_MS - 1)
        try {
            EnvironmentHistoryStore(context, database).use { store ->
                listOf(expired, first, second, third).forEach(store::save)
            }
            EnvironmentHistoryStore(context, database).use { reopened ->
                assertEquals(listOf(second, third), reopened.load(third.timestamp))
                assertEquals(
                    listOf(first, second, third),
                    buildList { reopened.forEach(0, third.timestamp, ::add) },
                )
            }
        } finally {
            context.deleteDatabase(database)
        }
    }
}
