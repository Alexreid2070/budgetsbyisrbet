package com.isrbet.budgetsbyisrbet

import org.junit.Test

import org.junit.Assert.*

class GlobalVarsUnitTest {
    @Test
    fun budgetMonthTest() {
        var bm = MyDate(2021,1,1)
        assertEquals(2021, bm.getYear())
        assertEquals(1, bm.getYear())
        bm = MyDate(2021,1,1)
        assertEquals(2021, bm.getYear())
        assertEquals(0, bm.getMonth())
        bm = MyDate(2021,1,1)
        assertEquals(2021, bm.getYear())
        assertEquals(0, bm.getMonth())
        bm = MyDate("2021-01")
        assertEquals(2021, bm.getYear())
        assertEquals(1, bm.getMonth())
        bm = MyDate("2021-1")
        assertEquals(2021, bm.getYear())
        assertEquals(1, bm.getMonth())
        bm = MyDate("2021-12")
        assertEquals(2021, bm.getYear())
        assertEquals(12, bm.getMonth())
        bm = MyDate("2021")
        assertEquals(2021, bm.getYear())
        assertEquals(0, bm.getMonth())
        val bm2 = MyDate("2023-03")
        bm = bm2
        assertEquals(2023, bm.getYear())
        assertEquals(3, bm.getMonth())
        bm = MyDate("2021-12")
        assertEquals(true, bm < bm2)
        assertEquals(false, bm2 < bm)
        assertEquals(false, bm == bm2)
        bm.increment(cPeriodMonth, 1)
        assertEquals(false, bm < bm2)
        bm.increment(cPeriodMonth, -2)
        assertEquals(true, bm < bm2)
        bm = MyDate(2021,1,1)
        assertEquals("2021-01", bm.toString())
        bm = MyDate(2021,12,1)
        assertEquals("2021-12", bm.toString())
        bm = MyDate(2021,1,1)
        assertEquals("2021-00", bm.toString())
        var pd = perfectDecimal("98.7654321", 2, 3)
        assertEquals("98.765", pd)
        pd = perfectDecimal("98.7654321", 2, 2)
        assertEquals("98.76", pd)
        pd = perfectDecimal("98.7654321", 2, 1)
        assertEquals("98.7", pd)
        pd = perfectDecimal("98.4", 2, 3)
        assertEquals("98.4", pd)
    }
}

