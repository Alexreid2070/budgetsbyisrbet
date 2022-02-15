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
        val expModel = ExpenditureViewModel()
        expModel.clearCallback()
        SpenderViewModel.addLocalSpender(Spender("Alex", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Brent", "alexreid2071@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Joint", "", 100, 1))
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Mortgage", "Non-Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Property Taxes", "Non-Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Life", "Booze", "Discretionary", true)
        CategoryViewModel.addCategoryAndSubcategory("Life", "Annual", "Discretionary", true)
        BudgetViewModel.updateBudget(Category("Life-Booze"), "2022-01", "Alex", 5.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(Category("Life-Annual"), "2022-00", "Alex", 10.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(Category("Life-Annual"), "2022-00", "Brent", 10.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(Category("Housing-Mortgage"), "2022-01", "Joint", 100.0, cBUDGET_RECURRING, true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-02-15", 4*100, "Life", "Booze", "test", "Alex", "Alex", 100, 0, ""), true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-01-15", 1*100, "Life", "Annual", "test", "Brent", "Brent", 0, 100, ""), true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-01-15", 2*100, "Life", "Annual", "test", "Alex", "Alex", 100, 0, ""), true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-02-15", 4*100, "Life", "Annual", "test", "Alex", "Alex", 100, 0, ""), true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-03-15", 7*100, "Life", "Annual", "test", "Alex", "Alex", 100, 0, ""), true)
        ExpenditureViewModel.addTransaction(ExpenditureOut("2022-04-15", 4*100, "Life", "Annual", "test", "Alex", "Alex", 100, 0, ""), true)

        var ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Alex")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,1), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,2), "Alex")
        Assert.assertEquals(4.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,2), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,2), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life", "Booze"), BudgetMonth(2022,1), BudgetMonth(2022,2), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Alex")
        Assert.assertEquals(2.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Brent")
        Assert.assertEquals(1.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,1), BudgetMonth(2022,1), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,1), BudgetMonth(2022,1), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,2), "Alex")
        Assert.assertEquals(4.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,2), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,2), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,2), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,3), BudgetMonth(2022,3), "Alex")
        Assert.assertEquals(7.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,3), BudgetMonth(2022,3), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,3), BudgetMonth(2022,3), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,3), BudgetMonth(2022,3), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,4), BudgetMonth(2022,4), "Alex")
        Assert.assertEquals(4.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,4), BudgetMonth(2022,4), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,4), BudgetMonth(2022,4), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,4), BudgetMonth(2022,4), "")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,3), "Alex")
        Assert.assertEquals(11.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,3), "Brent")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,3), "Joint")
        Assert.assertEquals(0.0, ac, 0.0)
        ac = ExpenditureViewModel.getActualsForPeriod(Category("Life-Annual"), BudgetMonth(2022,2), BudgetMonth(2022,3), "")
        Assert.assertEquals(0.0, ac, 0.0)

        var bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2020-01"), "Alex")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        var bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2020,1), Category("Life-Annual"), "Alex")
        Assert.assertEquals(0.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,1), Category("Life-Annual"), "Alex")
        Assert.assertEquals(2.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-02"), "Alex")
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,2), Category("Life-Annual"), "Alex")
        Assert.assertEquals(4.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-03"), "Alex")
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,3), Category("Life-Annual"), "Alex")
        Assert.assertEquals(4.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-04"), "Alex")
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,4), Category("Life-Annual"), "Alex")
        Assert.assertEquals(0.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-01"), "Brent")
        Assert.assertEquals(10.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life-Annual"), "Brent")
        Assert.assertEquals(1.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-01"), "Joint")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life-Annual"), "Joint")
        Assert.assertEquals(0.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Annual"), BudgetMonth("2022-01"), "")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life-Annual"), "")
        Assert.assertEquals(3.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Booze"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(5.0, bm.amount, 0.0)
        var bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Life-Booze"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(5.0, bm2, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Booze"), BudgetMonth("2022-01"), "Joint")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life", "Booze"), "Joint")
        Assert.assertEquals(0.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Booze"), BudgetMonth("2022-01"), "")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life", "Booze"), "")
        Assert.assertEquals(5.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Life-Booze"), BudgetMonth("2022-01"), "Brent")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Life-Booze"), BudgetMonth("2022-01"), "Brent")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Life", "Booze"), "Brent")
        Assert.assertEquals(0.0, bmr, 0.0)

        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Life-Booze"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(5.0, bm2, 0.0)

        var totBu = BudgetViewModel.getTotalCalculatedBudgetForMonth(BudgetMonth(2022,1),cDiscTypeDiscretionary)
        var totAc = ExpenditureViewModel.getTotalDiscretionaryActualsToDate(BudgetMonth(2022,1))
        Assert.assertEquals(3.0, totAc, 0.0)
        Assert.assertEquals(8.0, totBu, 0.0)

        totBu = BudgetViewModel.getTotalCalculatedBudgetForMonth(BudgetMonth(2022,2),cDiscTypeDiscretionary)
        totAc = ExpenditureViewModel.getTotalDiscretionaryActualsToDate(BudgetMonth(2022,2))
        Assert.assertEquals(8.0, totAc, 0.0)
        Assert.assertEquals(9.0, totBu, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Alex")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Housing-Mortgage"), "Alex")
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Brent")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Brent")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Housing-Mortgage"), "Brent")
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Joint")
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-01"), "Joint")
        Assert.assertEquals(100.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Housing-Mortgage"), "Joint")
        Assert.assertEquals(100.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), Category("Housing-Mortgage"), "")
        Assert.assertEquals(100.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Alex")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Alex")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), Category("Housing-Mortgage"), "Alex")
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Brent")
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Brent")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), Category("Housing-Mortgage"), "Brent")
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Joint")
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(Category("Housing-Mortgage"), BudgetMonth("2022-02"), "Joint")
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), Category("Housing-Mortgage"), "Joint")
        Assert.assertEquals(100.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), Category("Housing-Mortgage"), "")
        Assert.assertEquals(100.0, bmr, 0.0)
    }
}