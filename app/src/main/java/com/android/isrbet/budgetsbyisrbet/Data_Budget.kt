package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.util.ArrayList

data class BudgetPeriod(var period: BudgetMonth, var who: String, var amount: Double) {
    fun isAnnualBudget(): Boolean {
        if (period.month == 0)
            return true
        else
            return false
    }
    fun getYear(): Int {
        return period.year
    }
    fun getMonth(): Int {
        return period.month
    }
    fun incrementMonth() {
        period.addMonth(1)
    }
}

data class BudgetAmountResponse(var dateApplicable: BudgetMonth, var who: String, var amount: Double, var dateStarted: BudgetMonth) {
    constructor() : this(BudgetMonth(0,0), "", 0.0, BudgetMonth(0,0))

    fun isAnnualBudget(): Boolean {
        if (dateStarted.month == 0)
            return true
        else
            return false
    }
}

data class Budget(var categoryName: String) {
    val budgetPeriodList: MutableList<BudgetPeriod> = ArrayList()

    fun addBudgetPeriod(period: String, who: String, amount: Double) {
        if (!overlapsWithExistingBudget(period, who))
            budgetPeriodList.add(BudgetPeriod(BudgetMonth(period), who, amount))
    }

    fun getBudgetPeriods(): MutableList<BudgetPeriod> {
        return budgetPeriodList
    }

    fun getPeriod(iPeriod: String, iWho: String): BudgetPeriod? {
        budgetPeriodList.forEach {
            if (it.period.toString() == iPeriod && it.who == iWho)
                return it
        }
        return null
    }

    fun overlapsWithExistingBudget(period: String, who: String): Boolean {
        var tempNewBudget = BudgetPeriod(BudgetMonth(period), who, 0.0)
        if (budgetPeriodList.contains(tempNewBudget))
            return true
        // if incoming is an annual, and there is an existing monthly
        if (tempNewBudget.isAnnualBudget()) {
            var ti: String
            for (i in 1..12) {
                if (i < 10)
                    ti = "0" + i.toString()
                else
                    ti = i.toString()
                budgetPeriodList.forEach {
                    if (it.period.toString() == period + "-" + ti && it.who == who)
                        return true
                }
            }
        }
        // if incoming is a monthly, and there is an existing annual
        if (!tempNewBudget.isAnnualBudget()) {
            budgetPeriodList.forEach {
                if (it.period.toString() == tempNewBudget.getYear().toString() && it.who == who)
                    return true
            }
        }
        return false
    }
}

class BudgetViewModel : ViewModel() {
    lateinit var budgetListener: ValueEventListener
    private val budgets: MutableList<Budget> = ArrayList()
    var dataUpdatedCallback: NewBudgetDataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: BudgetViewModel // used to track static single instance of self
        fun showMe() {
            Log.d("Alex", "SHOW ME budgets " + singleInstance.budgets.size.toString())
            singleInstance.budgets.forEach {
                Log.d("Alex", "SM Budget is " + it)
                it.budgetPeriodList.forEach {
                    Log.d("Alex", "  period " + it.period + " who " + it.who + " amount " + it.amount)
                }
            }
        }

        fun getBudgetAmount(iCategory: String, iBudgetMonth: BudgetMonth, iWho: String, onlyWho: Boolean): BudgetAmountResponse {
            var tResponse = BudgetAmountResponse()
            tResponse.who = iWho
            if (iBudgetMonth.month == 0) {
//                tBudgetPeriod.incrementMonth()
                tResponse.dateStarted.setValue(iBudgetMonth)
                for (i in 1..12) {
                    var tBudgetMonth = BudgetMonth(iBudgetMonth.year, i)
                    tResponse.dateApplicable.setValue(tBudgetMonth)
                    var tmpBudget = getBudgetAmount(iCategory, tBudgetMonth, iWho, onlyWho)
                    tResponse.amount = tResponse.amount + tmpBudget.amount
                }
                Log.d("Alex", "returning " + tResponse.amount + " for annual amount for iCategory " + iCategory)
                return tResponse
            }
            var tFirstNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), SpenderViewModel.getSpenderName(0), 0.0, BudgetMonth(9999,12))
            var tSecondNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), SpenderViewModel.getSpenderName(1), 0.0, BudgetMonth(9999,12))
            var tJointBudget = BudgetAmountResponse(BudgetMonth(9999,12), "Joint", 0.0, BudgetMonth(9999,12))
            val myBudget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (myBudget != null) {
                    myBudget.budgetPeriodList.forEach {
                        if (it.period.toString() <= iBudgetMonth.toString() && it.who == SpenderViewModel.getSpenderName(0)) {
                            tFirstNameBudget.amount = it.amount/100.0
                            tFirstNameBudget.dateApplicable.setValue(it.period)
                            tFirstNameBudget.dateStarted.setValue(it.period)
                        }
                        if (it.period.toString() <= iBudgetMonth.toString() && it.who == SpenderViewModel.getSpenderName(1)) {
                            tSecondNameBudget.amount = it.amount/100.0
                            tSecondNameBudget.dateApplicable.setValue(it.period)
                            tSecondNameBudget.dateStarted.setValue(it.period)
                        }
                        if (it.period.toString() <= iBudgetMonth.toString() && it.who == "Joint") {
                            tJointBudget.amount = it.amount / 100.0
                            tJointBudget.dateApplicable.setValue(it.period)
                            tJointBudget.dateStarted.setValue(it.period)
                        }
                    }
                }
                if (iWho == SpenderViewModel.getSpenderName(0)) {
                    if (onlyWho) {
                        tResponse.amount = tFirstNameBudget.amount
                        tResponse.dateApplicable.setValue(tFirstNameBudget.dateApplicable)
                        tResponse.dateStarted.setValue(tFirstNameBudget.dateStarted)
                    } else {
                        tResponse.amount =
                            tFirstNameBudget.amount + (tJointBudget.amount * SpenderViewModel.getSpenderSplit(
                                0
                            ) / 100.0)
                        if (tFirstNameBudget.dateStarted < tJointBudget.dateStarted)
                            tResponse.dateStarted.setValue(tFirstNameBudget.dateStarted)
                        else
                            tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    }
                } else if (iWho == SpenderViewModel.getSpenderName(1)) {
                    if (onlyWho) {
                        tResponse.amount = tSecondNameBudget.amount
                        tResponse.dateStarted.setValue(tSecondNameBudget.dateStarted)
                    } else {
                        tResponse.amount =
                            tSecondNameBudget.amount + (tJointBudget.amount * SpenderViewModel.getSpenderSplit(
                                1
                            ) / 100.0)
                        if (tSecondNameBudget.dateStarted < tJointBudget.dateStarted)
                            tResponse.dateStarted.setValue(tSecondNameBudget.dateStarted)
                        else
                            tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    }
                } else {
                    if (onlyWho) {
                        tResponse.amount = tJointBudget.amount
                        tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    } else {
                        tResponse.amount =
                            tFirstNameBudget.amount + tSecondNameBudget.amount + tJointBudget.amount
                        if (tFirstNameBudget.dateStarted < tSecondNameBudget.dateStarted)
                            tResponse.dateStarted.setValue(tFirstNameBudget.dateStarted)
                        else
                            tResponse.dateStarted.setValue(tSecondNameBudget.dateStarted)
                        if (tJointBudget.dateStarted < tResponse.dateStarted)
                            tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    }
                }
            return tResponse
        }

        fun getTotalDiscretionaryBudgetForMonth(iCal: Calendar) : Double {
            var tmpTotal: Double = 0.0
            CategoryViewModel.getCategories().forEach {
                if (it.discType == cDiscTypeDiscretionary) {
                    var bpAmt = getBudgetAmount(
                        it.categoryName + "-" + it.subcategoryName,
                        BudgetMonth(iCal.get(Calendar.YEAR), iCal.get(Calendar.MONTH) + 1),
                        "Joint",
                        false
                    )
                    tmpTotal += bpAmt.amount
                }
            }
            Log.d("Alex", "Discretionary budget for month is " + tmpTotal.toString())
            return tmpTotal
        }

        fun getBudgetCategories(iBudgetMonth: BudgetMonth, iDiscFlag: String): MutableList<String> {
            val myList: MutableList<String> = ArrayList()
            var tBudgetAmount: Double
            singleInstance.budgets.forEach {
                tBudgetAmount = 0.0
                val tCategory = it.categoryName
                it.budgetPeriodList.forEach {
                    if ((iBudgetMonth.month != 0 && it.period.toString() <= iBudgetMonth.toString()) ||
                        (iBudgetMonth.month == 0 && it.period.year <= iBudgetMonth.year)
                    ) {
                        val dash = tCategory.indexOf("-")
                        val catDiscFlag = CategoryViewModel.getDiscretionaryIndicator(
                            tCategory.substring(
                                0,
                                dash
                            ), tCategory.substring(dash + 1, tCategory.length)
                        )
                        if (iDiscFlag == "" || catDiscFlag == iDiscFlag) {
                            tBudgetAmount = it.amount
                        }
                    }
                }
                if (tBudgetAmount > 0) {
                    myList.add(it.categoryName)
                }
            }
            return myList
        }

        fun getBudgetInputRows(iCategory: String, iFilter: String): MutableList<BudgetInputRow> {
            var tList: MutableList<BudgetInputRow> = ArrayList<BudgetInputRow>()
            var isAnnual: String = ""
            CategoryViewModel.getCategories().forEach {
                if (iCategory == it.categoryName + "-" + it.subcategoryName) {
                    if (it.discType != "Off") {
                        val fullCategoryName = it.categoryName + "-" + it.subcategoryName
                        var firstMonth = getFirstMonthOfBudget(fullCategoryName)
                        var lastMonth = getLastMonthOfBudget(fullCategoryName)
                        if (firstMonth != null && lastMonth != null) {
                            if (firstMonth.period.month == 0)
                                firstMonth.period.month = 1
//                            if (lastMonth.period.month < 12)
//                                lastMonth.period.month = 12
                            var monthIterator = firstMonth.period.toString()
                            while (monthIterator <= lastMonth.period.toString()) {
                                for (i in 1..SpenderViewModel.getCount()) {
                                    var spenderName = SpenderViewModel.getSpenderName(i - 1)
                                    var bAmount = getBudgetAmount(
                                        fullCategoryName,
                                        BudgetMonth(monthIterator),
                                        spenderName,
                                        true
                                    )
                                    if (bAmount.dateStarted.month == 0) {
                                        Log.d("Alex", "found isannual Y")
                                        val dec = DecimalFormat("#.00")
                                        isAnnual = "Y (" + dec.format(bAmount.amount) + ")"
                                        bAmount.amount = bAmount.amount / 12.0
                                    } else
                                        isAnnual = ""

                                    val tDateApplicable = BudgetMonth(monthIterator)
                                    if (iFilter == cEXPANDED ||
                                        (iFilter == cCONDENSED && tDateApplicable.toString() == bAmount.dateStarted.toString() ||
                                        (tDateApplicable.month == 1 && bAmount.dateStarted.month == 0))) {
                                        var tRow = BudgetInputRow(
                                            monthIterator,
                                            bAmount.amount.toString(),
                                            bAmount.who,
                                            isAnnual,
                                            bAmount.dateStarted.toString()
                                        )
                                        tList.add(tRow)
                                    }
                                }
                                var bm = BudgetMonth(monthIterator)
                                bm.addMonth(1)
                                monthIterator = bm.toString()
                            }
                        }
                    }
                }
            }
            return tList
        }

        fun deleteBudget(iCategory: String, iPeriod: String, iWho: String) {
            var budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                var pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod }
                val ind = budget.budgetPeriodList.indexOf(pBudget)
                budget.budgetPeriodList.removeAt(ind)
            }

            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget")
                .child(iCategory)
                .child(iPeriod)
                .child(iWho)
                .removeValue()
        }
        fun updateBudget(iCategory: String, iPeriod: String, iWho: String, iAmount: Int) {
            var budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                var pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod }
                if (pBudget != null) {
                    var bm = BudgetMonth(iPeriod)
                    pBudget.period.year = bm.year
                    pBudget.period.month = bm.month
                    pBudget.who = iWho
                    pBudget.amount = iAmount.toDouble()
                }
            }
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget")
                .child(iCategory)
                .child(iPeriod)
                .child(iWho)
                .setValue(iAmount)
        }
        fun getBudget(iCategory: String): Budget? {
            return singleInstance.budgets.find { it.categoryName == iCategory }
        }
        fun getFirstMonthOfBudget(iCategory: String): BudgetPeriod? {
            var budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                if (budget.budgetPeriodList.size > 0)
                    return BudgetPeriod(BudgetMonth(budget.budgetPeriodList[0].period), budget.budgetPeriodList[0].who, budget.budgetPeriodList[0].amount)
                else
                    return null
            }
            else
                return null
        }

        fun getLastMonthOfBudget(iCategory: String): BudgetPeriod? {
            var tMonth = BudgetPeriod(BudgetMonth(0,0), "", 0.0)
            var budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                budget.budgetPeriodList.forEach {
                    if (it.period.toString() > tMonth.period.toString())
                        tMonth = BudgetPeriod(it.period, it.who, it.amount)
                }
            }
            return tMonth
        }

        fun refresh() {
            singleInstance.loadBudgets()
        }

        fun getBudgets(): MutableList<Budget> {
            return singleInstance.budgets
        }
    }

    init {
        BudgetViewModel.singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/NewBudget")
            .removeEventListener(budgetListener)
    }

    fun getBudgetcount(): Int {
        return budgets.size
    }

    fun setCallback(iCallback: NewBudgetDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
        dataUpdatedCallback?.onDataUpdate()
    }

    fun loadBudgets() {
        // Do an asynchronous operation to fetch budgets
        budgetListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                budgets.clear()
                dataSnapshot.children.forEach()
                {
                    lateinit var myB: Budget
                    var found: Boolean = false
                    var i: Int = 0
                    while (!found && i < budgets.size) {
                        if (it.key.toString() == budgets[i].categoryName) {
                            myB = budgets[i]
                            found = true
                        }
                        i++
                    }
                    if (!found) {
                        myB = Budget(it.key.toString())
                        budgets.add(myB)
                    } else
                        Log.d(
                            "Alex",
                            "Not adding budget " + it.key.toString() + " since it is already there"
                        )
                    if (it.key.toString() == "Housing-Condo-Fees")
                        found = found
                    it.children.forEach {
                        var tPeriod: String = it.key.toString()
                        it.children.forEach {
                            budgets[budgets.indexOf(myB)].addBudgetPeriod(
                                tPeriod,
                                it.key.toString(),
                                it.value.toString().toDouble()
                            )
                        }
                    }
                }
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget").addValueEventListener(budgetListener)
    }
}

public interface NewBudgetDataUpdatedCallback  {
    fun onDataUpdate()
}