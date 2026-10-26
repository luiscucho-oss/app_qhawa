package com.cucho.qhawa.data.habits

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private val Context.wellnessDataStore by preferencesDataStore(name = "qhawa_wellness_v1")

data class WellnessSnapshot(
    val completedDays: Set<String> = emptySet(),
    val assignments: Map<String, String> = emptyMap(),
    val adviceIndex: Int = 0,
    val adviceAvailableAt: Long = 0,
    val adviceCompletions: Set<String> = emptySet(),
) {
    fun adviceReady(now: Long) = now >= adviceAvailableAt
    fun remainingMillis(now: Long) = (adviceAvailableAt - now).coerceAtLeast(0)
}

/** Un DataStore de proceso; claves separadas para cada cuenta/demo. Sin dependencia de MQTT. */
class WellnessRepository internal constructor(
    private val store: DataStore<Preferences>,
    private val userKey: String,
    private val legacy: () -> Map<String, *>,
) {
    constructor(context: Context, userKey: String) : this(
        context.applicationContext.wellnessDataStore,
        userKey,
        { context.applicationContext.getSharedPreferences("qhawa_habits", Context.MODE_PRIVATE).all },
    )

    private val prefix = MessageDigest.getInstance("SHA-256").digest(userKey.toByteArray()).joinToString("") { "%02x".format(it) }
    private val migrated = booleanPreferencesKey("$prefix:migrated")
    private val days = stringSetPreferencesKey("$prefix:days")
    private val adviceIndex = intPreferencesKey("$prefix:advice_index")
    private val availableAt = longPreferencesKey("$prefix:advice_available_at")
    private val adviceLog = stringSetPreferencesKey("$prefix:advice_log")
    private val assignments = stringSetPreferencesKey("$prefix:assignments")

    /** Importación por usuario, idempotente y atómica; no borra las preferencias originales. */
    private suspend fun migrateUser() {
        store.edit { prefs ->
            if (prefs[migrated] == true) return@edit
            val old = legacy()
            val oldKey = "completed:$userKey"
            val dates = (old[oldKey] as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            prefs[days] = prefs[days].orEmpty() + dates
            val retainedAssignments = prefs[assignments].orEmpty().toMutableSet()
            dates.forEach { date ->
                if (retainedAssignments.none { it.startsWith("$date|") }) {
                    val actions = (old["$oldKey:$date"] as? Set<*>)?.filterIsInstance<String>().orEmpty()
                    retainedAssignments += "$date|${if ("review" in actions || "aerosols" !in actions) "review" else "damp_cloth"}"
                }
            }
            prefs[assignments] = retainedAssignments
            val importedAdvice = mutableSetOf<String>()
            old.forEach { (key, value) ->
                if (key.startsWith("$oldKey:")) {
                    val date = key.removePrefix("$oldKey:")
                    val actions = (value as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
                    prefs[stringSetPreferencesKey("$prefix:legacy_actions:$date")] = actions
                    if ("aerosols" in actions) importedAdvice += "$date|legacy_aerosols"
                }
            }
            prefs[adviceLog] = prefs[adviceLog].orEmpty() + importedAdvice
            if (prefs[adviceIndex] == null) prefs[adviceIndex] = importedAdvice.size % dailyAdvice.size
            // El formato anterior no guardaba la hora: no se inventa una espera histórica.
            prefs[migrated] = true
        }
    }

    val data: Flow<WellnessSnapshot> = flow {
        migrateUser()
        emitAll(store.data.map { prefs ->
            WellnessSnapshot(
                completedDays = prefs[days].orEmpty().toSet(),
                assignments = prefs[assignments].orEmpty().mapNotNull {
                    val parts = it.split('|', limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap(),
                adviceIndex = (prefs[adviceIndex] ?: 0).mod(dailyAdvice.size),
                adviceAvailableAt = prefs[availableAt] ?: 0,
                adviceCompletions = prefs[adviceLog].orEmpty().toSet(),
            )
        })
    }

    /** Una acción iniciada termina de guardarse aunque se abandone su pantalla. */
    private suspend fun durableUpdate(block: (MutablePreferences) -> Unit) = withContext(NonCancellable) {
        migrateUser()
        store.edit { block(it) }
    }

    suspend fun completeHabit(date: String, now: Long): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        val time = runCatching { format.parse(date)?.time }.getOrNull() ?: return false
        if (dateKey(time) != date || date > dateKey(now)) return false
        var accepted = false
        durableUpdate { prefs ->
            if (date in prefs[days].orEmpty()) return@durableUpdate
            val assigned = prefs[assignments].orEmpty()
            if (assigned.none { it.startsWith("$date|") }) prefs[assignments] = assigned + "$date|${dailyHabit(time).id}"
            prefs[days] = prefs[days].orEmpty() + date
            accepted = true
        }
        return accepted
    }

    suspend fun completeAdvice(now: Long, adviceId: String? = null): Boolean {
        require(adviceId == null || (dailyAdvice + pmAdvice.values.flatten()).any { it.id == adviceId })
        require(now >= 0 && now <= Long.MAX_VALUE - ADVICE_WAIT_MILLIS)
        var accepted = false
        durableUpdate { prefs ->
            if (now < (prefs[availableAt] ?: 0)) return@durableUpdate
            val index = (prefs[adviceIndex] ?: 0).mod(dailyAdvice.size)
            prefs[adviceLog] = prefs[adviceLog].orEmpty() + "$now|${adviceId ?: dailyAdvice[index].id}"
            prefs[adviceIndex] = (index + 1) % dailyAdvice.size
            prefs[availableAt] = now + ADVICE_WAIT_MILLIS
            accepted = true
        }
        return accepted
    }
}
