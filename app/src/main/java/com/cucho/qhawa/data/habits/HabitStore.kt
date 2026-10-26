package com.cucho.qhawa.data.habits

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun dateKey(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(time)
fun dayBefore(time: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = time
    add(Calendar.DAY_OF_YEAR, -days)
}.timeInMillis

fun habitStreak(completed: Set<String>, now: Long): Int {
    var offset = if (dateKey(now) in completed) 0 else 1
    var count = 0
    while (dateKey(dayBefore(now, offset)) in completed) { count++; offset++ }
    return count
}

/** Local records isolated by account; no password or environmental data is stored here. */
class HabitStore(context: Context, userId: String) {
    private val preferences = context.getSharedPreferences("qhawa_habits", Context.MODE_PRIVATE)
    private val key = "completed:$userId"
    fun read(): Set<String> = preferences.getStringSet(key, emptySet())?.toSet().orEmpty()
    fun actions(time: Long): Set<String> = preferences.getStringSet("$key:${dateKey(time)}", emptySet())?.toSet().orEmpty()
    fun complete(time: Long, action: String): Set<String> = (read() + dateKey(time)).also {
        preferences.edit().putStringSet(key, it)
            .putStringSet("$key:${dateKey(time)}", actions(time) + action).apply()
    }
}
