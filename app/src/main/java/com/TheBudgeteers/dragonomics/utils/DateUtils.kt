package com.TheBudgeteers.dragonomics.utils

import java.util.Calendar

// Utility object for date-related calculations.
// Mainly used to get month ranges and month names.

object DateUtils {

    // Get the start and end timestamps for the current month
    fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return getMonthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    // Get the start and end timestamps for a given month/year
    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1) // end of month
        val endOfMonth = cal.timeInMillis

        return startOfMonth to endOfMonth
    }

    // Get month name and year (e.g. "October 2025")
    fun getMonthName(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault()) + " " + year
    }
}
