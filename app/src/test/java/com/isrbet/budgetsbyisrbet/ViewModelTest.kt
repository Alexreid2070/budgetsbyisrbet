package com.isrbet.budgetsbyisrbet

import org.junit.Assert
import org.junit.Test

class ViewModelTest {
    @Test
    fun budgetAmountTest() {
        SpenderViewModel.addLocalSpender(Spender("User1", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("User2", "alexreid2071@gmail.com", 50, 0))
        SpenderViewModel.addLocalSpender(Spender("Joint", "test.com", 100, 0))
        Assert.assertEquals(1, SpenderViewModel.singleUser())
        var c = Category("Life-Groceries")
        Assert.assertEquals("Life", c.categoryName)
        Assert.assertEquals("Groceries", c.subcategoryName)
    }
}