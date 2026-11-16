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
    val adviceCompletions: Set<String> = emptySet(),
)

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

    suspend fun completeAdvice(now: Long, adviceId: String) {
        require((dailyAdvice + pmAdvice.values.flatten()).any { it.id == adviceId })
        require(now >= 0)
        durableUpdate { prefs ->
            prefs[adviceLog] = prefs[adviceLog].orEmpty() + "$now|$adviceId"
        }
    }
}
