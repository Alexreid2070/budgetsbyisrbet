package com.isrbet.budgetsbyisrbet

import org.junit.Assert
import org.junit.Test

class ViewModelTest {
    @Test
    fun budgetAmountTest() {
        val spenderModel = SpenderViewModel()
        spenderModel.clearCallback()
        val catModel = CategoryViewModel()
        catModel.clearCallback()
        val budgetModel = BudgetViewModel()
        budgetModel.clearCallback()
        val expModel = ExpenditureViewModel()
        expModel.clearCallback()

        SpenderViewModel.addLocalSpender(Spender("User1", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("User2", "alexreid2071@gmail.com", 50, 0))
        SpenderViewModel.addLocalSpender(Spender("Joint", "test.com", 100, 0))
        Assert.assertEquals(1, SpenderViewModel.getActiveCount())
        Assert.assertEquals(true, SpenderViewModel.singleUser())
        var c = Category("Life-Groceries")
        Assert.assertEquals("Life", c.categoryName)
        Assert.assertEquals("Groceries", c.subcategoryName)
    }
}