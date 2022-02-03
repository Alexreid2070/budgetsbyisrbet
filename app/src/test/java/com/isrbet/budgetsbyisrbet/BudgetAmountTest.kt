import com.isrbet.budgetsbyisrbet.*
import org.junit.Assert
import org.junit.Test

class GetBudgetAmountTest {
    @Test
    fun budgetAmountTest() {
        val spenderModel = SpenderViewModel()
        spenderModel.clearCallback()
        val catModel = CategoryViewModel()
        catModel.clearCallback()
        val budgetModel = BudgetViewModel()
        budgetModel.clearCallback()
        SpenderViewModel.addLocalSpender(Spender("Alex", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Brent", "alexreid2071@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Joint", "", 100, 1))
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Mortgage", "Non-Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Property Taxes", "Non-Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Life", "Booze", "Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Life", "Groceries", "Discretionary", true)
        BudgetViewModel.updateBudget("Life-Booze", "2021-01", "Alex", 500, cBUDGET_RECURRING, true)
        var bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Alex")
        Assert.assertEquals(5.0, bm.amount, 0.0)
        bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Joint")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Brent")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        var bm2 = BudgetViewModel.budgetExistsForExactPeriod("Life-Booze", BudgetMonth("2021-01"), "Alex")
        Assert.assertEquals(5.0, bm2, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod("Life-Booze", BudgetMonth("2021-01"), "Brent")
        Assert.assertEquals(0.0, bm2, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod("Life-Booze", BudgetMonth("2021-02"), "Alex")
        Assert.assertEquals(0.0, bm2, 0.0)
    }
}