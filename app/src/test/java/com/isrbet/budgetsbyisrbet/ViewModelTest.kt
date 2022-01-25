package com.isrbet.budgetsbyisrbet

import org.junit.Assert
import org.junit.Test

class ViewModelTest {
    @Test
    fun budgetAmountTest() {
        SpenderViewModel.singleInstance.spenders.add(Spender("User1", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.singleInstance.spenders.add(Spender("User2", "alexreid2071@gmail.com", 50, 0))
        SpenderViewModel.singleInstance.spenders.add(Spender("Joint", "test.com", 100, 0))
        Assert.assertEquals(1, SpenderViewModel.singleUser())
    }
}