package com.isrbet.budgetsbyisrbet

import org.junit.Test
import org.junit.Assert.*

class QuickTest {
    @Test
    fun test() {
        var bm = MyDate(2021, 1, 1)
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
        bm = MyDate("2022-02-23")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = MyDate("2022-02")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = MyDate("2022-2")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = MyDate("2022-2-23")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        var bp = BudgetPeriod(MyDate(2022,1,1), 0, 100.0, cPeriodWeek, 2, 0, MyDate(2022,1,1))
        bp.getBudgetAmount(MyDate(2021,1,1))?.let { assertEquals(0.0, it.toDouble(), 0.0) }
        bp.getBudgetAmount(MyDate(2022,1,1))?.let { assertEquals(0.0, it.toDouble(), 0.0) }
    }
}
