package com.cucho.qhawa.data.habits

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HabitStreakTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 1, 12, 0) }.timeInMillis
    @Test fun countsAcrossYearBoundaryAndDoesNotDuplicateDays() {
        val dates = setOf(dateKey(now), dateKey(now), dateKey(dayBefore(now, 1)), dateKey(dayBefore(now, 2)))
        assertEquals(3, habitStreak(dates, now))
    }
    @Test fun preservesYesterdayStreakWhileTodayIsPending() {
        assertEquals(2, habitStreak(setOf(dateKey(dayBefore(now, 1)), dateKey(dayBefore(now, 2))), now))
    }
    @Test fun gapBreaksStreakAndFutureActionsDoNotCount() {
        assertEquals(0, habitStreak(setOf(dateKey(dayBefore(now, 2)), dateKey(dayBefore(now, -1))), now))
    }
}
