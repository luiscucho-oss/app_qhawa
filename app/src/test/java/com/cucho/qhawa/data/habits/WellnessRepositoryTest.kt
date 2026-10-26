package com.cucho.qhawa.data.habits

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
            assertEquals(1, ana.data.first().adviceIndex)
            assertEquals(0L, ana.data.first().adviceAvailableAt)
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

    @Test fun cooldownSurvivesStoreRestartAndAllowsNextAdviceOnlyAfterFiveMinutes() = runBlocking {
        val file = File(folder.root, "restart.preferences_pb")
        var job = SupervisorJob()
        try {
            var store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            var repository = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }
            assertTrue(repository.completeAdvice(now))
            assertFalse(repository.completeAdvice(now + ADVICE_WAIT_MILLIS - 1))
            job.cancelAndJoin()
            job = SupervisorJob()
            store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            repository = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }
            val restored = repository.data.first()
            assertEquals(now + 300_000, restored.adviceAvailableAt)
            assertEquals(1, restored.adviceIndex)
            assertFalse(restored.adviceReady(now + 299_999))
            assertTrue(restored.adviceReady(now + 300_000))
            assertTrue(repository.completeAdvice(now + 300_000))
            assertEquals(2, repository.data.first().adviceCompletions.size)
        } finally { job.cancelAndJoin() }
    }

    @Test fun recordsSelectedLevelAdviceAndRetainsCooldownAndExistingLog() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(folder.root, "pm.preferences_pb") }
        try {
            val repository = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }
            assertTrue(repository.completeAdvice(now))
            val originalLog = repository.data.first().adviceCompletions
            val red = pmAdvice.getValue(PmAdviceLevel.RED)[2]
            assertFalse(repository.completeAdvice(now + 1, red.id))
            val next = now + ADVICE_WAIT_MILLIS
            assertTrue(repository.completeAdvice(next, red.id))
            val restored = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }.data.first()
            assertEquals(originalLog + "$next|${red.id}", restored.adviceCompletions)
            assertEquals(next + ADVICE_WAIT_MILLIS, restored.adviceAvailableAt)
        } finally { job.cancelAndJoin() }
    }

    @Test fun rapidConcurrentCompletionsCannotSkipRecommendationsOrResetCooldown() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(folder.root, "concurrent.preferences_pb") }
        try {
            val repository = WellnessRepository(store, "account:test") { emptyMap<String, Any>() }
            val results = (1..12).map { async { repository.completeAdvice(now) } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(1, repository.data.first().adviceCompletions.size)
            assertEquals(now + ADVICE_WAIT_MILLIS, repository.data.first().adviceAvailableAt)
        } finally { job.cancelAndJoin() }
    }
}
