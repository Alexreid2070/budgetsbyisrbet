package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.lang.Exception
import java.util.*
import kotlin.math.round

data class BudgetPeriod(var period: BudgetMonth, var who: Int, var amount: Double, var occurence: Int) {
    fun isAnnualBudget(): Boolean {
        return (period.month == 0)
    }
    fun getYear(): Int {
        return period.year
    }
}

data class BudgetOut(var amount: Double, var occurence: Int)

data class BudgetAmountResponse(
    var dateApplicable: BudgetMonth,
    var who: Int,
    var amount: Double,
    var dateStarted: BudgetMonth,
    var occurence: Int) {
    constructor() : this(BudgetMonth(-1,-1), -1, 0.0, BudgetMonth(-1,-1), -1)
}

data class Budget(var categoryID: Int) {
    val budgetPeriodList: MutableList<BudgetPeriod> = ArrayList()

    fun addBudgetPeriod(period: String, who: Int, amount: Double, occurence: Int) {
        // budgets need to be added in chronological order in order for app to work
        budgetPeriodList.add(BudgetPeriod(BudgetMonth(period), who, amount, occurence))
        budgetPeriodList.sortWith(compareBy({ it.period.year }, { it.period.month }))
    }

    fun overlapsWithExistingBudget(period: String, who: Int): Boolean {
        val pBudget = budgetPeriodList.find { it.period.toString() == period && it.who == who }
        if (pBudget != null) { // ie found it
            return true  // ie it overlaps
        }
        // if incoming is an annual, and there is an existing monthly
        val tempNewBudget = BudgetPeriod(BudgetMonth(period), who, 0.0, 0)
        if (tempNewBudget.isAnnualBudget()) {
            var ti: String
            for (i in 1..12) {
                ti = if (i < 10)
                    "0$i"
                else
                    i.toString()
                budgetPeriodList.forEach {
                    if (it.period.toString() == tempNewBudget.getYear().toString() + "-" + ti && it.who == who)
                        return true
                }
            }
        }
        // if incoming is a monthly, and there is an existing annual
        if (!tempNewBudget.isAnnualBudget()) {
            budgetPeriodList.forEach {
                if (it.period.toString().substring(0,4) == tempNewBudget.getYear().toString()
                    && it.period.toString().substring(5,7) == "00"
                    && it.who == who) {
                    return true
                }
            }
        }
        return false
    }
}

@Suppress("HardCodedStringLiteral")
class BudgetViewModel : ViewModel() {
    private var budgetListener: ValueEventListener? = null
    private val budgets: MutableList<Budget> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: BudgetViewModel // used to track static single instance of self

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.budgets.size
            else
                0
        }
        fun budgetExistsUsingCategory(iCategoryID: Int): Int {
            var ctr = 0
            singleInstance.budgets.forEach {
                if (it.categoryID == iCategoryID) {
                    ctr++
                }
            }
            return ctr
        }

/*        fun showMe() {
            Log.d("Alex", "SHOW ME budgets " + singleInstance.budgets.size.toString())
            singleInstance.budgets.forEach {
                Log.d("Alex", "SM Budget category is '" + CategoryViewModel.getFullCategoryName(it.categoryID) + "'")
                for (child in it.budgetPeriodList) {
                    Log.d("Alex", "  period " + child.period + " who " + child.who + " amount " + child.amount)
                }
            }
        } */

        fun getCategoryBudgets(iPeriod: DateRange, iBudgetMonth: BudgetMonth, iDiscType: String, iWho: Int,
        iSpecificCategory: String, iAllSubcategories: Boolean = false) : ArrayList<DataObject> {
            val tList: ArrayList<DataObject> = ArrayList()
            var prevCategory = ""
            var prevCategoryID = 0
            var totalBudget = 0.0
            CategoryViewModel.getCategories(true).forEach {
                if (iSpecificCategory == "" ||
                    (it.categoryName == iSpecificCategory)) {
                    if (it.discType == iDiscType || iDiscType == "" || iDiscType == cDiscTypeAll) {
                        val budget = getCalculatedBudgetAmount(
                            iPeriod,
                            iBudgetMonth,
                            it.id,
                            iWho
                        )
    //                    val budget = getTotalCalculatedBudgetForMonthForCategory(it.id, iBudgetMonth, iDiscType, iWho)
                        val nameToCheck = if (iSpecificCategory == "" && !iAllSubcategories) it.categoryName else it.subcategoryName
                        if (budget != 0.0) {
                            if (prevCategory != "" && prevCategory != nameToCheck) {
                                // ie not the first row, and this was a change in category
                                tList.add(DataObject(prevCategoryID, prevCategory, totalBudget,
                                    DefaultsViewModel.getCategoryDetail(prevCategory).priority.toString(), 0))
                                totalBudget = 0.0
                            }
                            totalBudget += budget
                            prevCategory = nameToCheck
                            prevCategoryID = it.id
                        }
                    }
                }
            }
            if (totalBudget != 0.0) {
                tList.add(DataObject(prevCategoryID, prevCategory, totalBudget,
                    DefaultsViewModel.getCategoryDetail(prevCategory).priority.toString(), 0))
            }
            return  tList
        }

        private fun getCalculatedBudgetAmount(iPeriod: DateRange, iBudgetMonth: BudgetMonth, iCategoryID: Int, iWhoToLookup: Int): Double {
            var tBudgetAmount = 0.0
            if (iPeriod == DateRange.ALLTIME) {
                val dateNow = android.icu.util.Calendar.getInstance()
                val lastAnnualYear = if (dateNow.get(Calendar.MONTH) == 11) // ie Dec
                    dateNow.get(Calendar.YEAR)
                else
                    dateNow.get(Calendar.YEAR)-1
                // add annual budgets for previous years, and current year if it is Dec
                val earliestYear = TransactionViewModel.getEarliestYear()
                for (i in earliestYear until lastAnnualYear+1) {
                    tBudgetAmount += getCalculatedBudgetAmount(
                        BudgetMonth(i, 0),
                        iCategoryID,
                        iWhoToLookup
                    )
                }
                // then add YTD for current year (if it's not Dec)
                if (dateNow.get(Calendar.MONTH) != 11) {
                    for (i in 1 until iBudgetMonth.month + 1) {
                        tBudgetAmount += getCalculatedBudgetAmount(
                            BudgetMonth(
                                iBudgetMonth.year,
                                i
                            ),
                            iCategoryID,
                            iWhoToLookup
                        )
                    }
                }
            } else if (iPeriod == DateRange.YTD) {
                for (i in 1 until iBudgetMonth.month + 1) {
                    tBudgetAmount += getCalculatedBudgetAmount(
                        BudgetMonth(
                            iBudgetMonth.year,
                            i
                        ),
                        iCategoryID,
                        iWhoToLookup)
                }
            } else {
                tBudgetAmount = getCalculatedBudgetAmount(
                    iBudgetMonth,
                    iCategoryID,
                    iWhoToLookup
                )
            }
            return tBudgetAmount
        }

        fun getCalculatedBudgetAmount(iBudgetMonth: BudgetMonth, iCategoryID: Int, iWhoToLookup: Int): Double {
            // if iWhoToLookup is blank, then will calculate for all Spenders
            // if iWhoToLookup is a specific name, then include the right % of Joint
            var tBudgetAmount = 0.0

            // get original budgets for all 3 possible users
            val budgetForPeriodUser0 = getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 0)
            val budgetForPeriodUser1 = if (SpenderViewModel.getActiveCount() > 1)
                getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 1)
            else
                BudgetAmountResponse()
            val budgetForPeriodUser2 = if (SpenderViewModel.getActiveCount() > 1)
                getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 2)
            else
                BudgetAmountResponse()

            val actualsForUser0PriorMonths = if (iBudgetMonth.month != 1 &&
                    iBudgetMonth.month != 0 &&
                    (budgetForPeriodUser0.dateStarted.month == 0  ||
                        budgetForPeriodUser2.dateStarted.month == 0))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    BudgetMonth(iBudgetMonth.year, 1),
                    BudgetMonth(iBudgetMonth.year, iBudgetMonth.month - 1),
                    0,
                    true )
                else
                    0.0 // ie not relevant

            val actualsForUser0ThisMonth = if (iBudgetMonth.month != 0 &&
                (budgetForPeriodUser0.dateStarted.month == 0 ||
                        budgetForPeriodUser2.dateStarted.month == 0))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    iBudgetMonth,
                    iBudgetMonth,
                    0,
                    true )
            else
                0.0 // ie not relevant
            val actualsForUser1PriorMonths = if (iBudgetMonth.month != 1 &&
                iBudgetMonth.month != 0 &&
                (budgetForPeriodUser1.dateStarted.month == 0 ||
                    budgetForPeriodUser2.dateStarted.month == 0))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    BudgetMonth(iBudgetMonth.year, 1),
                    BudgetMonth(iBudgetMonth.year, iBudgetMonth.month - 1),
                    1,
                    true )
            else
                0.0 // ie not relevant

            val actualsForUser1ThisMonth = if (iBudgetMonth.month != 0 &&
                (budgetForPeriodUser1.dateStarted.month == 0  ||
                        budgetForPeriodUser2.dateStarted.month == 0))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    iBudgetMonth,
                    iBudgetMonth,
                    1,
                    true )
            else
                0.0 // ie not relevant

            if (iWhoToLookup == 0 || iWhoToLookup == 2) {
                var budgetRemaining = 0.0
                var totalAnnualBudget = 0.0
                if (iBudgetMonth.month != 0 && budgetForPeriodUser0.dateStarted.month == 0) { // ie not an annual view, and is an annual budget
                    // handle user 0's annual budget
                    totalAnnualBudget = budgetForPeriodUser0.amount
                } else { // else just add user 0's non-annual budget
                    tBudgetAmount += budgetForPeriodUser0.amount
                }
                if (iBudgetMonth.month != 0 && budgetForPeriodUser2.dateStarted.month == 0) { // ie not an annual view, and is an annual budget
                    // handle user 2's annual budget
                    totalAnnualBudget += budgetForPeriodUser2.amount * SpenderViewModel.getSpenderSplit(0)
                } else { // else just add user 2's non-annual budget
                    tBudgetAmount += budgetForPeriodUser2.amount * SpenderViewModel.getSpenderSplit(0)
                }
                if (totalAnnualBudget > 0.0) {
                    if (totalAnnualBudget - actualsForUser0PriorMonths > 0.0)
                        budgetRemaining = totalAnnualBudget - actualsForUser0PriorMonths
                    tBudgetAmount +=
                        if (actualsForUser0ThisMonth < budgetRemaining) actualsForUser0ThisMonth else budgetRemaining
                }
            }
            if (iWhoToLookup == 1  || iWhoToLookup == 2) {
                var budgetRemaining = 0.0
                var totalAnnualBudget = 0.0
                if (iBudgetMonth.month != 0 && budgetForPeriodUser1.dateStarted.month == 0) { // ie not an annual view, and is an annual budget
                    // handle user 0's annual budget
                    totalAnnualBudget = budgetForPeriodUser1.amount
                } else { // else just add user 0's non-annual budget
                    tBudgetAmount += budgetForPeriodUser1.amount
                }
                if (iBudgetMonth.month != 0 && budgetForPeriodUser2.dateStarted.month == 0) { // ie not an annual view, and is an annual budget
                    // handle user 2's annual budget
                    totalAnnualBudget += budgetForPeriodUser2.amount * SpenderViewModel.getSpenderSplit(1)
                } else { // else just add user 2's non-annual budget
                    tBudgetAmount += budgetForPeriodUser2.amount * SpenderViewModel.getSpenderSplit(1)
                }
                if (totalAnnualBudget > 0.0) {
                    if (totalAnnualBudget - actualsForUser1PriorMonths > 0.0)
                        budgetRemaining = totalAnnualBudget - actualsForUser1PriorMonths
                    tBudgetAmount +=
                        if (actualsForUser1ThisMonth < budgetRemaining) actualsForUser1ThisMonth else budgetRemaining
                }
            }

            return tBudgetAmount
        }

        fun getOriginalBudgetAmount(iCategoryID: Int, iBudgetMonth: BudgetMonth, iWho: Int): BudgetAmountResponse {
            val tResponse = BudgetAmountResponse()
            tResponse.dateApplicable.setValue(iBudgetMonth)
            tResponse.who = iWho
            if (iBudgetMonth.month == 0) {
                tResponse.dateStarted.setValue(iBudgetMonth)
                loop@for (i in 1..12) {
                    val tBudgetMonth = BudgetMonth(iBudgetMonth.year, i)
                    tResponse.dateApplicable.setValue(tBudgetMonth)
                    val  tmpBudget = getOriginalBudgetAmount(iCategoryID, tBudgetMonth, iWho)
                    tResponse.amount = tResponse.amount + tmpBudget.amount
                    if (tmpBudget.dateStarted.isAnnualBudget()) { // ie it's an annual amount, so stop counting each month
                        break@loop
                    }
                }
                return tResponse
            }
            val tFirstNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), 0, 0.0, BudgetMonth(9999,12), -1)
            val tSecondNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), 1, 0.0, BudgetMonth(9999,12), -1)
            val tJointBudget = BudgetAmountResponse(BudgetMonth(9999,12), 2, 0.0, BudgetMonth(9999,12), -1)
            val myBudget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            myBudget?.budgetPeriodList?.forEach {
                val tBudget = BudgetMonth(it.period)
                if (it.who == 0) {
                    if ((it.occurence == 0 && it.period.toString() <= iBudgetMonth.toString()) ||  // occurence of 0 means recurring
                        (it.occurence == 1 &&  // it's a non-recurring budget
                                (tBudget.month == 0 && tBudget.year == iBudgetMonth.year) ||  // it's an annual budget
                                (tBudget.month != 0 && tBudget.toString() == iBudgetMonth.toString())  // it's a monthly budget
                                )
                    ) {  // occurence of 1 means single
                        tFirstNameBudget.amount = it.amount
                        tFirstNameBudget.dateApplicable.setValue(it.period)
                        tFirstNameBudget.dateStarted.setValue(it.period)
                        tFirstNameBudget.occurence = it.occurence
                    }
                }
                if (it.who == 1) {
                    if ((it.occurence == 0 && it.period.toString() <= iBudgetMonth.toString()) ||  // occurence of 0 means recurring
                        (it.occurence == 1 &&  // it's a non-recurring budget
                                (tBudget.month == 0 && tBudget.year == iBudgetMonth.year) ||  // it's an annual budget
                                (tBudget.month != 0 && tBudget.toString() == iBudgetMonth.toString())  // it's a monthly budget
                                )
                    ) {  // occurence of 1 means single
                        tSecondNameBudget.amount = it.amount
                        tSecondNameBudget.dateApplicable.setValue(it.period)
                        tSecondNameBudget.dateStarted.setValue(it.period)
                        tSecondNameBudget.occurence = it.occurence
                    }
                }
                if (it.who == 2) { // ie Joint
                    if ((it.occurence == 0 && it.period.toString() <= iBudgetMonth.toString()) ||  // occurence of 0 means recurring
                        (it.occurence == 1 &&  // it's a non-recurring budget
                                (tBudget.month == 0 && tBudget.year == iBudgetMonth.year) ||  // it's an annual budget
                                (tBudget.month != 0 && tBudget.toString() == iBudgetMonth.toString())  // it's a monthly budget
                                )
                    ) {  // occurence of 1 means single
                        tJointBudget.amount = it.amount
                        tJointBudget.dateApplicable.setValue(it.period)
                        tJointBudget.dateStarted.setValue(it.period)
                        tJointBudget.occurence = it.occurence
                    }
                }
            }
            when (iWho) {
                0 -> {
                    tResponse.amount = tFirstNameBudget.amount
                    tResponse.dateApplicable.setValue(tFirstNameBudget.dateApplicable)
                    tResponse.dateStarted.setValue(tFirstNameBudget.dateStarted)
                    tResponse.occurence = tFirstNameBudget.occurence
                }
                1 -> {
                    tResponse.amount = tSecondNameBudget.amount
                    tResponse.dateApplicable.setValue(tSecondNameBudget.dateApplicable)
                    tResponse.dateStarted.setValue(tSecondNameBudget.dateStarted)
                    tResponse.occurence = tSecondNameBudget.occurence
                }
                else -> {
                    tResponse.amount = tJointBudget.amount
                    tResponse.dateApplicable.setValue(tJointBudget.dateApplicable)
                    tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    tResponse.occurence = tJointBudget.occurence
                }
            }
            if (tResponse.dateApplicable.month == 0)
                tResponse.dateApplicable.month = 1
            return tResponse
        }

        fun getBudgetInputRows(iBudgetMonth: BudgetMonth): MutableList<BudgetInputRow> {
            val myList: MutableList<BudgetInputRow> = ArrayList()
            singleInstance.budgets.forEach {
                val categoryID = it.categoryID
                val bir : Array<BudgetInputRow?> = Array(3) {null}
                var isAnnual: String
                val cat = CategoryViewModel.getCategory(categoryID)
                if ((cat?.inUse == true) && cat.iAmAllowedToSeeThisCategory()) {
                    for (budget in it.budgetPeriodList) {
                        if ((budget.occurence == 0 && budget.period.toString() <= iBudgetMonth.toString()) ||
                            (budget.occurence == 1 && (budget.period.toString() == iBudgetMonth.toString() ||
                                    budget.period.month == 0 && budget.period.year == iBudgetMonth.year))
                        ) {
                            isAnnual = if (budget.period.isAnnualBudget()) {
                                MyApplication.getString(R.string.yes_short)
                            } else
                                ""
                            bir[budget.who] = BudgetInputRow(
                                categoryID,
                                budget.period.toString(),
                                budget.amount.toString(),
                                budget.who,
                                budget.occurence.toString(),
                                isAnnual,
                                budget.period.toString(),
                                cBudgetDateView)
                        }
                    }
                    for (i in 0 until 3) {
                        if (bir[i] != null) {
                            bir[i]?.let { it1 -> myList.add(it1) }
                        }
                    }
                }
            }
            myList.sortWith(compareBy({ it.categoryPriority }, { it.subcategory }))
            return myList
        }

        fun getBudgetInputRows(iCategoryID: Int): MutableList<BudgetInputRow> {
            val tList: MutableList<BudgetInputRow> = ArrayList<BudgetInputRow>()
            var isAnnual: String
            CategoryViewModel.getCategories(true).forEach {
                if (iCategoryID == it.id) {
//                    if (it.discType != cDiscTypeOff) {
                        val firstMonth = getFirstMonthOfBudget(iCategoryID)
                        val lastMonth = getLastMonthOfBudget(iCategoryID)
                        if (firstMonth != null) {
                            if (firstMonth.period.month == 0)
                                firstMonth.period.month = 1
                            if (lastMonth.period.month == 0)
                                lastMonth.period.month = 1
                            var monthIterator = firstMonth.period.toString()
                            while (monthIterator <= lastMonth.period.toString()) {
                                for (i in 0 until SpenderViewModel.getActiveCount()) {
                                    val  bAmount = getOriginalBudgetAmount(
                                        iCategoryID,
                                        BudgetMonth(monthIterator),
                                        i
                                    )
                                    isAnnual = if (bAmount.dateStarted.isAnnualBudget()) {
                                        MyApplication.getString(R.string.yes_short)
                                    } else
                                        ""

                                    val tDateApplicable = BudgetMonth(monthIterator)
                                    if ( tDateApplicable.toString() == bAmount.dateStarted.toString() ||
                                        (tDateApplicable.month == 1 && bAmount.dateStarted.isAnnualBudget())
                                        && tDateApplicable.year == bAmount.dateStarted.year) {
                                        val tRow = BudgetInputRow(
                                            iCategoryID,
                                            monthIterator,
                                            bAmount.amount.toString(),
                                            bAmount.who,
                                            bAmount.occurence.toString(),
                                            isAnnual,
                                            bAmount.dateStarted.toString(),
                                            cBudgetCategoryView
                                        )
                                        tList.add(tRow)
                                    }
                                }
                                val bm = BudgetMonth(monthIterator)
                                bm.addMonth(1)
                                monthIterator = bm.toString()
                            }
//                        }
                    }
                }
            }
            return tList
        }

        fun deleteBudget(iCategoryID: Int, iPeriod: String, iWho: Int) {
            val  budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget != null) {
                val  pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod && it.who == iWho }
                val ind = budget.budgetPeriodList.indexOf(pBudget)
                budget.budgetPeriodList.removeAt(ind)
            }

            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                .child(iCategoryID.toString())
                .child(iPeriod)
                .child(iWho.toString())
                .removeValue()
        }
        fun updateBudget(iCategoryID: Int, iPeriod: String, iWho: Int, iAmount: Double, iOccurence: Int, iLocalOnly: Boolean = false) {
            var budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget == null) { // first budget being added for this Category
                budget = Budget(iCategoryID)
                singleInstance.budgets.add(budget)
            }
                val pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod && it.who == iWho}
                if (pBudget != null) {
                    val bm = BudgetMonth(iPeriod)
                    pBudget.period.year = bm.year
                    pBudget.period.month = bm.month
                    pBudget.who = iWho
                    pBudget.amount = iAmount
                    pBudget.occurence = iOccurence
                } else {
                    budget.addBudgetPeriod(
                        iPeriod,
                        iWho,
                        iAmount,
                        iOccurence
                    )
                }
            val budgetOut = BudgetOut(round(iAmount*100), iOccurence)
            if (!iLocalOnly)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                    .child(iCategoryID.toString())
                    .child(iPeriod)
                    .child(iWho.toString())
                    .setValue(budgetOut)
        }
        fun getBudget(iCategoryID: Int): Budget? {
            return singleInstance.budgets.find { it.categoryID == iCategoryID }
        }
        private fun getFirstMonthOfBudget(iCategoryID: Int): BudgetPeriod? {
            var tMonth = BudgetPeriod(BudgetMonth(9999,0), -1, 0.0,0)
            val budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            return if (budget != null) {
                budget.budgetPeriodList.forEach {
                    if (it.period.toString() < tMonth.period.toString())
                        tMonth = BudgetPeriod(BudgetMonth(it.period), it.who, it.amount, it.occurence)
                }
                tMonth
            } else
                null
        }

        private fun getLastMonthOfBudget(iCategoryID: Int): BudgetPeriod {
            var tMonth = BudgetPeriod(BudgetMonth(0,0), -1, 0.0 ,0)
            val budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            budget?.budgetPeriodList?.forEach {
                if (it.period.toString() > tMonth.period.toString())
                    tMonth = BudgetPeriod(BudgetMonth(it.period), it.who, it.amount, it.occurence)
            }
            return tMonth
        }

        fun budgetExistsForExactPeriod(iCategoryID: Int, iBudgetMonth: BudgetMonth, iWho: Int): Double {
            val myBudget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            myBudget?.budgetPeriodList?.forEach {
                if (it.who == iWho) {
                    if (it.period.toString() == iBudgetMonth.toString())
                        return it.amount
                }
            }
            return 0.0
        }

        fun refresh() {
            singleInstance.loadBudgets()
        }

        fun clear() {
            if (singleInstance.budgetListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Budget")
                    .removeEventListener(singleInstance.budgetListener!!)
                singleInstance.budgetListener = null
            }
//            singleInstance.dataUpdatedCallback = null
            singleInstance.budgets.clear()
            singleInstance.loaded = false
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (budgetListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Budget")
                .removeEventListener(budgetListener!!)
            budgetListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadBudgets() {
        // Do an asynchronous operation to fetch budgets
        budgetListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                budgets.clear()
                dataSnapshot.children.forEach()
                {
                    lateinit var myB: Budget
                    var found = false
                    var i = 0
                    while (!found && i < budgets.size) {
                        if (it.key.toString() == budgets[i].categoryID.toString()) {
                            myB = budgets[i]
                            found = true
                        }
                        i++
                    }
                    if (!found) {
                        myB = Budget(it.key.toString().toInt())
                        budgets.add(myB)
                    }
                    for (budget in it.children) {
                        val tPeriod: String = budget.key.toString()
                        for (element in budget.children.toMutableList()) {
                            try {
                                val nWho = element.key.toString().toInt()

                                val tBudgetOut = BudgetOut(0.0, 0)
                                for (child in element.children) {
                                    when (child.key) {
                                        "amount" -> tBudgetOut.amount = child.value.toString().toDouble() / 100.0
                                        "occurence" -> tBudgetOut.occurence = child.value.toString().toInt()
                                    }
                                }
                                budgets[budgets.indexOf(myB)].addBudgetPeriod(
                                    tPeriod,
                                    nWho,
                                    tBudgetOut.amount,
                                    tBudgetOut.occurence
                                )
                            } catch (exception: Exception) {
                            }
                        }
                    }
                }
                loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 104.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget").addValueEventListener(
            budgetListener as ValueEventListener
        )
    }
}

interface DataUpdatedCallback  {
    fun onDataUpdate()
}