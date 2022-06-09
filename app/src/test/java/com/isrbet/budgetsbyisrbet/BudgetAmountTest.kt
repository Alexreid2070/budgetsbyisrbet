package com.isrbet.budgetsbyisrbet

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
        val expModel = TransactionViewModel()
        expModel.clearCallback()
        val defModel = DefaultsViewModel()
        defModel.clearCallback()
        SpenderViewModel.addLocalSpender(Spender("Alex", "alexreid2070@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Brent", "alexreid2071@gmail.com", 50, 1))
        SpenderViewModel.addLocalSpender(Spender("Joint", "", 100, 1))
        CategoryViewModel.updateCategory(0, "Housing", "Mortgage", cDiscTypeNondiscretionary, 2, cON,true)
        CategoryViewModel.updateCategory(0, "Housing", "Property Taxes", cDiscTypeNondiscretionary, 2, cON, true)
        CategoryViewModel.updateCategory(0, "Housing", "Renos", cDiscTypeNondiscretionary, 2, cON, true)
        CategoryViewModel.updateCategory(0, "Life", "Booze", cDiscTypeDiscretionary, 2, cON, true)
        CategoryViewModel.updateCategory(0, "Life", "Annual", cDiscTypeDiscretionary, 2, cON, true)
        BudgetViewModel.updateBudget(1001, "2022-01", 2, 100.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(1003, "2022-00", 2, 210.0, cBUDGET_JUST_THIS_MONTH, true)
        BudgetViewModel.updateBudget(1004, "2022-01", 0, 5.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(1005, "2022-00", 0, 10.0, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget(1005, "2022-00", 1, 10.0, cBUDGET_RECURRING, true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 1*100, 1005, "test", "", 1, 1, 0, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 2*100, 1005, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 4*100, 1004, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 4*100, 1005, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 100*100, 1003, "test", "", 2, 2, 50, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-03-15", 100*100, 1003, "test", "", 2, 2, 20, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-03-15", 7*100, 1005, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-04-15", 4*100, 1005, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 100*100, -99, "transfer", "", 1, 2, 0, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 100*100, -99, "transfer", "", 2, 1, 100, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 100*100, -99, "transfer", "", 1, 2, 0, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 100*100, -99, "transfer", "", 2, 1, 100, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-03-15", 100*100, -99, "transfer", "", 1, 2, 0, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-03-15", 100*100, -99, "transfer", "", 2, 1, 100, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-04-15", 100*100, -99, "transfer", "", 1, 2, 0, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-04-15", 100*100, -99, "transfer", "", 2, 1, 100, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-05-15", 100*100, -99, "transfer", "", 1, 2, 0, "Transfer"), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-05-15", 100*100, -99, "transfer", "", 2, 1, 100, "Transfer"), true)

        var ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,1), 0, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,1), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,1), 2, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,2), 0, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,2), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,2), 2, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,2), 0, false)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, BudgetMonth(2022,1), BudgetMonth(2022,2), 2, false)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,1), BudgetMonth(2022,1), 0, true)
        Assert.assertEquals(2.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,1), BudgetMonth(2022,1), 1, true)
        Assert.assertEquals(1.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,1), BudgetMonth(2022,1), 2, true)
        Assert.assertEquals(3.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,2), BudgetMonth(2022,2), 0, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,2), BudgetMonth(2022,2), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,2), BudgetMonth(2022,2), 2, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,3), BudgetMonth(2022,3), 0, true)
        Assert.assertEquals(7.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,3), BudgetMonth(2022,3), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,3), BudgetMonth(2022,3), 2, true)
        Assert.assertEquals(7.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,4), BudgetMonth(2022,4), 0, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,4), BudgetMonth(2022,4), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,4), BudgetMonth(2022,4), 2, true)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,2), BudgetMonth(2022,3), 0, true)
        Assert.assertEquals(11.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2022,2), BudgetMonth(2022,3), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2019,2), BudgetMonth(3333,3), 0, true)
        Assert.assertEquals(17.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2019,2), BudgetMonth(3333,3), 1, true)
        Assert.assertEquals(1.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, BudgetMonth(2019,2), BudgetMonth(3333,3), 2, true)
        Assert.assertEquals(18.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,1), 0, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,2), 0, true)
        Assert.assertEquals(50.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,3), 0, true)
        Assert.assertEquals(70.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,1), 1, true)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,2), 1, true)
        Assert.assertEquals(50.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,3), 1, true)
        Assert.assertEquals(130.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, BudgetMonth(2022,1), BudgetMonth(2022,3), 2, true)
        Assert.assertEquals(200.0, ac, 0.0)

        var bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2020-01"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        var bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2020,1), 1005, 0)
        Assert.assertEquals(0.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,1), 1005, 0)
        Assert.assertEquals(2.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-02"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,2), 1005, 0)
        Assert.assertEquals(4.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-03"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,3), 1005, 0)
        Assert.assertEquals(4.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-04"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(0, bm.dateStarted.month)
        Assert.assertEquals(1, bm.dateApplicable.month)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth(2022,4), 1005, 0)
        Assert.assertEquals(0.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-01"), 1)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1005, 0)
        Assert.assertEquals(2.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1005, 1)
        Assert.assertEquals(1.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1005, BudgetMonth("2022-01"), 2)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1005, 2)
        Assert.assertEquals(3.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(5.0, bm.amount, 0.0)
        var bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(5.0, bm2, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, BudgetMonth("2022-01"), 2)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1004, 2)
        Assert.assertEquals(5.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, BudgetMonth("2022-01"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, BudgetMonth("2022-01"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1004, 1)
        Assert.assertEquals(0.0, bmr, 0.0)

        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(5.0, bm2, 0.0)

        var catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeDiscretionary, 0, "")
        var totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        var totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 0)
        Assert.assertEquals(2.0, totAc, 0.0)
        Assert.assertEquals(7.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeDiscretionary, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 1)
        Assert.assertEquals(1.0, totAc, 0.0)
        Assert.assertEquals(1.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeDiscretionary, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 2)
        Assert.assertEquals(3.0, totAc, 0.0)
        Assert.assertEquals(8.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeNondiscretionary, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 0)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(50.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeNondiscretionary, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 1)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(50.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeNondiscretionary, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 2)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(100.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeAll, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 0)
        Assert.assertEquals(2.0, totAc, 0.0)
        Assert.assertEquals(57.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeAll, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 1)
        Assert.assertEquals(1.0, totAc, 0.0)
        Assert.assertEquals(51.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,1), cDiscTypeAll, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 2)
        Assert.assertEquals(3.0, totAc, 0.0)
        Assert.assertEquals(108.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRange.MONTH, BudgetMonth(2022,2), cDiscTypeDiscretionary, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-02-01", "2022-02-31",
            cDiscTypeDiscretionary, 0)
        Assert.assertEquals(8.0, totAc, 0.0)
        Assert.assertEquals(9.0, totalBudget, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-01"), 0)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1001, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-01"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-01"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1001, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-01"), 2)
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-01"), 2)
        Assert.assertEquals(100.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1001, 2)
        Assert.assertEquals(100.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-02"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-02"), 0)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1001, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-02"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-02"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1001, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1001, BudgetMonth("2022-02"), 2)
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1001, BudgetMonth("2022-02"), 2)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1001, 2)
        Assert.assertEquals(100.0, bmr, 0.0)

        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1003, 0)
        Assert.assertEquals(50.0, bmr, 0.0)

        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1003, 0)
        Assert.assertEquals(20.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1003, 1)
        Assert.assertEquals(55.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1003, 2)
        Assert.assertEquals(75.0, bmr, 0.0)

        CategoryViewModel.updateCategory(0, "Life", "JointBudgetIndependentActuals", cDiscTypeDiscretionary, 2, cON, true)
        BudgetViewModel.updateBudget(1006, "2022-00", 2, 100.0, cBUDGET_RECURRING, true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 5*100, 1006, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-01-15", 6*100, 1006, "test", "", 1, 1, 0, ""), true)
        ac = TransactionViewModel.getActualsForPeriod(1006, BudgetMonth(2022,1), BudgetMonth(2022,1), 0, true)
        Assert.assertEquals(5.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1006, BudgetMonth(2022,1), BudgetMonth(2022,1), 1, true)
        Assert.assertEquals(6.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1006, BudgetMonth(2022,1), BudgetMonth(2022,1), 2, true)
        Assert.assertEquals(11.0, ac, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1006, 0)
        Assert.assertEquals(5.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1006, 1)
        Assert.assertEquals(6.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-01"), 1006, 2)
        Assert.assertEquals(11.0, bmr, 0.0)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 100*100, 1006, "test", "", 0, 0, 100, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-02-15", 50*100, 1006, "test", "", 1, 1, 0, ""), true)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1006, 0)
        Assert.assertEquals(45.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1006, 1)
        Assert.assertEquals(44.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-02"), 1006, 2)
        Assert.assertEquals(89.0, bmr, 0.0)
        TransactionViewModel.addTransaction(TransactionOut("2022-03-15", 25*100, 1005, "test", "", 2, 2, 50, ""), true)
        TransactionViewModel.addTransaction(TransactionOut("2022-04-15", 140*100, 1005, "test", "", 2, 2, 50, ""), true)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1005, 0)
        Assert.assertEquals(4.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1005, 1)
        Assert.assertEquals(9.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-03"), 1005, 2)
        Assert.assertEquals(13.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-04"), 1005, 0)
        Assert.assertEquals(0.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-04"), 1005, 1)
        Assert.assertEquals(0.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(BudgetMonth("2022-04"), 1005, 2)
        Assert.assertEquals(0.0, bmr, 0.0)
    }
}