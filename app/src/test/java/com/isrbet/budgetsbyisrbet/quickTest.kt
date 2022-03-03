package com.isrbet.budgetsbyisrbet

import org.junit.Test
import org.junit.Assert.*

class QuickTest {
    @Test
    fun test() {
        var bm = BudgetMonth(2021, 1)
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
        bm = BudgetMonth("2022-02-23")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = BudgetMonth("2022-02")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = BudgetMonth("2022-2")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
        bm = BudgetMonth("2022-2-23")
        assertEquals(2022, bm.year)
        assertEquals(2, bm.month)
    }
}
