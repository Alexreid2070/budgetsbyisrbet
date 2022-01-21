package com.isrbet.budgetsbyisrbet

import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith

class GlobalVarsUnitTest {
    @Test
    fun budgetMonthTest() {
        var bm = BudgetMonth(2021,1)
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
        bm = BudgetMonth(2021,0)
        assertEquals(2021, bm.year)
        assertEquals(0, bm.month)
        bm = BudgetMonth(2021)
        assertEquals(2021, bm.year)
        assertEquals(0, bm.month)
        bm = BudgetMonth("2021-01")
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
        bm = BudgetMonth("2021-1")
        assertEquals(2021, bm.year)
        assertEquals(1, bm.month)
        bm = BudgetMonth("2021-12")
        assertEquals(2021, bm.year)
        assertEquals(12, bm.month)
        bm = BudgetMonth("2021")
        assertEquals(2021, bm.year)
        assertEquals(0, bm.month)
        var bm2 = BudgetMonth("2023-03")
        bm.setValue(bm2)
        assertEquals(2023, bm.year)
        assertEquals(3, bm.month)
        bm = BudgetMonth("2021-12")
        assertEquals(true, bm < bm2)
        assertEquals(false, bm2 < bm)
        assertEquals(false, bm == bm2)
        bm.setValue(bm2)
        assertEquals(true, bm == bm2)
        bm.addMonth()
        assertEquals(false, bm < bm2)
        bm.decrementMonth(2)
        assertEquals(true, bm < bm2)
        bm = BudgetMonth(2021,1)
        assertEquals("2021-01", bm.toString())
        bm = BudgetMonth(2021,12)
        assertEquals("2021-12", bm.toString())
        bm = BudgetMonth(2021)
        assertEquals("2021-00", bm.toString())
        var pd = PerfectDecimal("98.7654321", 2, 3)
        assertEquals("98.765", pd)
        pd = PerfectDecimal("98.7654321", 2, 2)
        assertEquals("98.76", pd)
        pd = PerfectDecimal("98.7654321", 2, 1)
        assertEquals("98.7", pd)
        pd = PerfectDecimal("98.4", 2, 3)
        assertEquals("98.4", pd)
    }
}

