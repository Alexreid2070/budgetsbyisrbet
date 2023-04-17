package com.isrbet.budgetsbyisrbet

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetBudgetAmountTest {
    @Before
    fun prepareForTesting() {
        gCurrentDate = MyDate(2023, 4, 9)
    }
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
        CategoryViewModel.updateCategory(0, "Housing", "Mortgage", cDiscTypeNondiscretionary, 2, true,true)
        CategoryViewModel.updateCategory(0, "Housing", "Property Taxes", cDiscTypeNondiscretionary, 2, true, true)
        CategoryViewModel.updateCategory(0, "Housing", "Renos", cDiscTypeNondiscretionary, 2, true, true)
        CategoryViewModel.updateCategory(0, "Life", "Booze", cDiscTypeDiscretionary, 2, true, true)
        CategoryViewModel.updateCategory(0, "Life", "Annual", cDiscTypeDiscretionary, 2, true, true)
        BudgetViewModel.updateBudget("A", 1002, MyDate("2022-01-01"), 2, 100.0, cPeriodMonth, 1, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget("B", 1003, MyDate("2022-01-01"), 2, 210.0, cPeriodMonth, 1, cBUDGET_JUST_THIS_MONTH, true)
        BudgetViewModel.updateBudget("C", 1004, MyDate("2022-01-01"), 0, 5.0, cPeriodMonth, 1, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget("D", 1005, MyDate("2022-01-01"), 0, 10.0, cPeriodMonth, 1, cBUDGET_RECURRING, true)
        BudgetViewModel.updateBudget("E", 1005, MyDate(2022), 1, 10.0, cPeriodYear, 1, cBUDGET_RECURRING, true)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 1.0, 1005, "test", "", 1, 1, cTRANSACTION_TYPE_EXPENSE, 0, "D"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 2.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "E"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 100.0, 1003, "test", "", 2, 2, cTRANSACTION_TYPE_EXPENSE, 50, "A"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 4.0, 1004, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "C"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 4.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "F"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 100.0, 1003, "test", "", 2, 2, cTRANSACTION_TYPE_EXPENSE, 20, "B"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 7.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "G"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-04-15"), 4.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "H"), false)
/*        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 100.0, -99, "transfer", "", 1, 2, cTRANSACTION_TYPE_EXPENSE, 0, "TransferA"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 100.0, -99, "transfer", "", 2, 1, cTRANSACTION_TYPE_EXPENSE, 100, "TransferB"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 100.0, -99, "transfer", "", 1, 2, cTRANSACTION_TYPE_EXPENSE, 0, "TransferC"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 100.0, -99, "transfer", "", 2, 1, cTRANSACTION_TYPE_EXPENSE, 100, "TransferD"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 100.0, -99, "transfer", "", 1, 2, cTRANSACTION_TYPE_EXPENSE, 0, "TransferE"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 100.0, -99, "transfer", "", 2, 1, cTRANSACTION_TYPE_EXPENSE, 100, "TransferF"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-04-15"), 100.0, -99, "transfer", "", 1, 2, cTRANSACTION_TYPE_EXPENSE, 0, "TransferG"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-04-15"), 100.0, -99, "transfer", "", 2, 1, cTRANSACTION_TYPE_EXPENSE, 100, "TransferH"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-05-15"), 100.0, -99, "transfer", "", 1, 2, cTRANSACTION_TYPE_EXPENSE, 0, "TransferI"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-05-15"), 100.0, -99, "transfer", "", 2, 1, cTRANSACTION_TYPE_EXPENSE, 100, "TransferJ"), false)
*/
        var ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,2,1), 0)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,1,1), 0)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,1,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,1,1), 2)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,2,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1004, MyDate(2022,1,1), MyDate(2022,2,1), 2)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,1,1), MyDate(2022,1,1), 0)
        Assert.assertEquals(2.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,1,1), MyDate(2022,1,1), 1)
        Assert.assertEquals(1.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,1,1), MyDate(2022,1,1), 2)
        Assert.assertEquals(3.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,2, 1), MyDate(2022,2,1), 0)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,2, 1), MyDate(2022,2,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,2, 1), MyDate(2022,2,1), 2)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3, 1), MyDate(2022,3,1), 0)
        Assert.assertEquals(7.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3,1), MyDate(2022,3,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3,1), MyDate(2022,3,1), 2)
        Assert.assertEquals(7.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,4,1), MyDate(2022,4,1), 0)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,4,1), MyDate(2022,4,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,4,1), MyDate(2022,4,1), 2)
        Assert.assertEquals(4.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,2,1), MyDate(2022,3,1), 0)
        Assert.assertEquals(11.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,2,1), MyDate(2022,3,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2019,2,1), MyDate(3333,3,1), 0)
        Assert.assertEquals(17.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2019,2,1), MyDate(3333,3,1), 1)
        Assert.assertEquals(1.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2019,2,1), MyDate(3333,3,1), 2)
        Assert.assertEquals(18.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,1,1), 0)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,2,1), 0)
        Assert.assertEquals(50.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,3,1), 0)
        Assert.assertEquals(70.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,1,1), 1)
        Assert.assertEquals(0.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,2,1), 1)
        Assert.assertEquals(50.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,3,1), 1)
        Assert.assertEquals(130.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1003, MyDate(2022,1,1), MyDate(2022,3,1), 2)
        Assert.assertEquals(200.0, ac, 0.0)

        var bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2020-01-01"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        var bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate(2020,1,1), 1005, 0)
        Assert.assertEquals(0.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-01-01"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(1, bm.startDate.getMonth())
        Assert.assertEquals(1, bm.applicableDate.getMonth())
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,1,1), MyDate(2022,1,1), 0)
        Assert.assertEquals(2.0, ac, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate(2022,1,1), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-02-01"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(1, bm.startDate.getMonth())
        Assert.assertEquals(1, bm.applicableDate.getMonth())
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate(2022,2,1), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-03-01"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(1, bm.startDate.getMonth())
        Assert.assertEquals(1, bm.applicableDate.getMonth())
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate(2022,3,1), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-04-01"), 0)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        Assert.assertEquals(1, bm.startDate.getMonth())
        Assert.assertEquals(1, bm.applicableDate.getMonth())
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate(2022,4,1), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-01-01"), 1)
        Assert.assertEquals(10.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1005, 1)
        Assert.assertEquals(1.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1005, MyDate("2022-01-01"), 2)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1005, 2)
        Assert.assertEquals(11.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, MyDate("2022-01-01"), 0)
        Assert.assertEquals(5.0, bm.amount, 0.0)
        var bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, MyDate("2022-01-01"), 0)
        Assert.assertEquals(5.0, bm2, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, MyDate("2022-01-01"), 2)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1004, 2)
        Assert.assertEquals(5.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1004, MyDate("2022-01-01"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, MyDate("2022-01-01"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1004, 1)
        Assert.assertEquals(0.0, bmr, 0.0)

        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1004, MyDate("2022-01-01"), 0)
        Assert.assertEquals(5.0, bm2, 0.0)

        var catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeDiscretionary, 0, "")
        var totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        var totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 0)
        Assert.assertEquals(2.0, totAc, 0.0)
        Assert.assertEquals(15.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeDiscretionary, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 1)
        Assert.assertEquals(1.0, totAc, 0.0)
        Assert.assertEquals(1.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeDiscretionary, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeDiscretionary, 2)
        Assert.assertEquals(3.0, totAc, 0.0)
        Assert.assertEquals(16.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeNondiscretionary, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 0)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(155.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeNondiscretionary, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 1)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(155.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeNondiscretionary, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeNondiscretionary, 2)
        Assert.assertEquals(0.0, totAc, 0.0)
        Assert.assertEquals(310.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeAll, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 0)
        Assert.assertEquals(2.0, totAc, 0.0)
        Assert.assertEquals(170.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeAll, 1, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 1)
        Assert.assertEquals(1.0, totAc, 0.0)
        Assert.assertEquals(156.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,1,1), cDiscTypeAll, 2, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-01-01", "2022-01-31",
            cDiscTypeAll, 2)
        Assert.assertEquals(3.0, totAc, 0.0)
        Assert.assertEquals(326.0, totalBudget, 0.0)

        catBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, MyDate(2022,2,1), cDiscTypeDiscretionary, 0, "")
        totalBudget = 0.0
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }
        totAc = TransactionViewModel.getTotalActualsForRange("2022-02-01", "2022-02-31",
            cDiscTypeDiscretionary, 0)
        Assert.assertEquals(8.0, totAc, 0.0)
        Assert.assertEquals(15.0, totalBudget, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-01-01"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-01-01"), 0)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1002, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-01-01"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-01-01"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1002, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-01-01"), 2)
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-01-01"), 2)
        Assert.assertEquals(100.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1002, 2)
        Assert.assertEquals(100.0, bmr, 0.0)

        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-02-01"), 0)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-02-01"), 0)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1002, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-02-01"), 1)
        Assert.assertEquals(0.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-02-01"), 1)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1002, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bm = BudgetViewModel.getOriginalBudgetAmount(1002, MyDate("2022-02-01"), 2)
        Assert.assertEquals(100.0, bm.amount, 0.0)
        bm2 = BudgetViewModel.budgetExistsForExactPeriod(1002, MyDate("2022-02-01"), 2)
        Assert.assertEquals(0.0, bm2, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1002, 2)
        Assert.assertEquals(100.0, bmr, 0.0)

        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1003, 0)
        Assert.assertEquals(0.0, bmr, 0.0)

        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1003, 0)
        Assert.assertEquals(0.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1003, 1)
        Assert.assertEquals(0.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1003, 2)
        Assert.assertEquals(0.0, bmr, 0.0)

        CategoryViewModel.updateCategory(0, "Life", "JointBudgetIndependentActuals", cDiscTypeDiscretionary, 2, true, true)
        BudgetViewModel.updateBudget("F", 1006, MyDate("2022-01-01"),2, 100.0, cPeriodMonth, 1, cBUDGET_RECURRING, true)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 5.0, 1006, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "AA"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 6.0, 1006, "test", "", 1, 1, cTRANSACTION_TYPE_EXPENSE, 0, "AB"), false)
        ac = TransactionViewModel.getActualsForPeriod(1006, MyDate(2022,1,1), MyDate(2022,1,1), 0)
        Assert.assertEquals(5.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1006, MyDate(2022,1,1), MyDate(2022,1,1), 1)
        Assert.assertEquals(6.0, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1006, MyDate(2022,1,1), MyDate(2022,1,1), 2)
        Assert.assertEquals(11.0, ac, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1006, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1006, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-01-01"), 1006, 2)
        Assert.assertEquals(100.0, bmr, 0.0)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 100.0,1006, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "AC"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 50.0, 1006, "test", "", 1, 1, cTRANSACTION_TYPE_EXPENSE, 0, "AD"), false)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1006, 0)
        Assert.assertEquals(50.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1006, 1)
        Assert.assertEquals(50.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-02-01"), 1006, 2)
        Assert.assertEquals(100.0, bmr, 0.0)

//        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 1.0, 1005, "test", "", 1, 1, cTRANSACTION_TYPE_EXPENSE, 0, "D"), false)
//        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-01-15"), 2.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "E"), false)
//        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-02-15"), 4.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "F"), false)
//        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 7.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "G"), false)
//        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-04-15"), 4.0, 1005, "test", "", 0, 0, cTRANSACTION_TYPE_EXPENSE, 100, "H"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-03-15"), 25.0, 1005, "test", "", 2, 2, cTRANSACTION_TYPE_EXPENSE, 50, "AE"), false)
        TransactionViewModel.addTransactionLocal(Transaction(MyDate("2022-04-15"), 140.0, 1005, "test", "", 2, 2, cTRANSACTION_TYPE_EXPENSE, 50, "AF"), false)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1005, 1)
        Assert.assertEquals(9.0, bmr, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3,1), MyDate(2022,3,1), 0)
        Assert.assertEquals(19.5, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3,1), MyDate(2022,3,1), 1)
        Assert.assertEquals(12.5, ac, 0.0)
        ac = TransactionViewModel.getActualsForPeriod(1005, MyDate(2022,3,1), MyDate(2022,3,1), 2)
        Assert.assertEquals(32.0, ac, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-03-01"), 1005, 2)
        Assert.assertEquals(19.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-04-01"), 1005, 0)
        Assert.assertEquals(10.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-04-01"), 1005, 1)
        Assert.assertEquals(0.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-04-01"), 1005, 2)
        Assert.assertEquals(10.0, bmr, 0.0)

        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-05-01"), 1002, 2)
        Assert.assertEquals(100.0, bmr, 0.0)
        BudgetViewModel.updateBudget("A2", 1002, MyDate("2022-05-06"), 2, 20.0, cPeriodWeek, 2, cBUDGET_RECURRING, true)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-05-01"), 1002, 2)
        Assert.assertEquals(140.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-06-01"), 1002, 2)
        Assert.assertEquals(40.0, bmr, 0.0)
        bmr = BudgetViewModel.getCalculatedBudgetAmount(MyDate("2022-07-01"), 1002, 2)
        Assert.assertEquals(60.0, bmr, 0.0)

    }
}