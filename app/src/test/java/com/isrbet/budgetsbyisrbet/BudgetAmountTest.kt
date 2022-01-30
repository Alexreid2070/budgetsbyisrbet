import com.isrbet.budgetsbyisrbet.*
import org.junit.Assert
import org.junit.Test

class GetBudgetAmountTest {
    @Test
    fun budgetAmountTest() {
        SpenderViewModel.addSpender(Spender("User1", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addSpender(Spender("User2", "alexreid2071@gmail.com", 50, 1))
        SpenderViewModel.addSpender(Spender("Joint", "", 100, 1))
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Mortgage", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Property Taxes", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Booze", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Groceries", "Discretionary")
        BudgetViewModel.updateBudget("Life-Booze", "2021-01", "Alex", 500, cBUDGET_RECURRING)
        var bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Alex", true)
        Assert.assertEquals(5, bm.amount)
        bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Joint", false)
        Assert.assertEquals(5, bm.amount)
        bm = BudgetViewModel.getBudgetAmount("Life-Booze", BudgetMonth("2021-01"), "Brent", true)
        Assert.assertEquals(0, bm.amount)
    }
}