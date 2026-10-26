package com.cucho.qhawa.data.habits

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class WellnessCatalogTest {
    private fun date(year: Int, month: Int, day: Int) = Calendar.getInstance().apply { clear(); set(year, month, day, 12, 0) }.timeInMillis

    @Test fun actualMonthHandlesLeapYearsAndAssignmentIsStableAndDifferentEveryDay() {
        assertEquals(29, monthDays(date(2028, Calendar.FEBRUARY, 10)).size)
        assertEquals(28, monthDays(date(2027, Calendar.FEBRUARY, 10)).size)
        val october = monthDays(date(2026, Calendar.OCTOBER, 26))
        assertEquals(31, october.size)
        assertEquals(31, october.map { dailyHabit(it).id }.distinct().size)
        october.forEach { assertEquals(dailyHabit(it), dailyHabit(it + 1000)) }
    }

    @Test fun progressCountsCalendarWeekMonthAndIgnoresFutureLegacyRecords() {
        val now = date(2026, Calendar.OCTOBER, 26) // Lunes.
        val completed = setOf(dateKey(now), dateKey(dayBefore(now, 1)), dateKey(dayBefore(now, -1)), "2026-09-30")
        assertEquals(WellnessProgress(true, 1, 2, 31), wellnessProgress(completed, now))
        assertEquals(WellnessProgress(false, 0, 0, 31), wellnessProgress(emptySet(), now))
    }
}
