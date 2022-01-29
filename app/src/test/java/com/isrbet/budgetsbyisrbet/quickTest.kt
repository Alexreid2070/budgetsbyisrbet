package com.isrbet.budgetsbyisrbet

import org.junit.Test
import org.junit.Assert.*

class QuickTest {
    @Test
    fun test() {
        val bm = BudgetMonth(2021, 1)
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
    }
}
