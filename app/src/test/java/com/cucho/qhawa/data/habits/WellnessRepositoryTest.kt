package com.cucho.qhawa.data.habits

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import java.security.MessageDigest
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WellnessRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 26, 12, 0) }.timeInMillis

    @Test fun importsLegacyRecordsOnceWithoutOverwritingNewProgressAndIsolatesUsers() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(folder.root, "migration.preferences_pb") }
        val today = dateKey(now)
        val yesterday = dateKey(dayBefore(now, 1))
        val legacy = mutableMapOf<String, Any>(
            "completed:account:ana" to setOf(yesterday),
            "completed:account:ana:$yesterday" to setOf("review", "aerosols"),
        )
        try {
            val ana = WellnessRepository(store, "account:ana") { legacy }
            assertEquals(setOf(yesterday), ana.data.first().completedDays)
            assertEquals(1, ana.data.first().adviceCompletions.size)
            assertTrue(ana.completeHabit(today, now))
            legacy["completed:account:ana"] = emptySet<String>()
            assertEquals(setOf(yesterday, today), WellnessRepository(store, "account:ana") { legacy }.data.first().completedDays)
            assertTrue(WellnessRepository(store, "account:otro") { legacy }.data.first().completedDays.isEmpty())
            assertTrue(WellnessRepository(store, "demo:ana") { legacy }.data.first().completedDays.isEmpty())
            assertTrue(store.data.first().asMap().any { it.key.name.contains("legacy_actions:$yesterday") && it.value == setOf("review", "aerosols") })
        } finally { job.cancelAndJoin() }
    }

    @Test fun habitCompletionIsIdempotentPreservesAssignmentAndRejectsFutureDates() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(folder.root, "habits.preferences_pb") }
        try {
            val repository = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }
            assertFalse(repository.completeHabit(dateKey(dayBefore(now, -1)), now))
            assertFalse(repository.completeHabit("2026-02-30", now))
            assertTrue(repository.completeHabit(dateKey(now), now))
            assertFalse(repository.completeHabit(dateKey(now), now))
            val snapshot = repository.data.first()
            assertEquals(setOf(dateKey(now)), snapshot.completedDays)
            assertEquals(dailyHabit(now).id, snapshot.assignments[dateKey(now)])
        } finally { job.cancelAndJoin() }
    }

    @Test fun consecutiveAdviceAreAvailableImmediatelyEvenWithAnOldCooldown() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(folder.root, "advice.preferences_pb") }
        try {
            val user = "account:test"
            val prefix = MessageDigest.getInstance("SHA-256").digest(user.toByteArray()).joinToString("") { "%02x".format(it) }
            store.edit { it[longPreferencesKey("$prefix:advice_available_at")] = now + 300_000 }
            val first = dailyAdvice.first().id
            val second = pmAdvice.getValue(PmAdviceLevel.RED).first().id
            val repository = WellnessRepository(store, user) { emptyMap<String, Any>() }
            repository.completeAdvice(now, first)
            repository.completeAdvice(now, second)
            repository.completeAdvice(now, first)
            val restored = WellnessRepository(store, user) { emptyMap<String, Any>() }.data.first()
            assertEquals(setOf("$now|$first", "$now|$second"), restored.adviceCompletions)
        } finally { job.cancelAndJoin() }
    }
}
