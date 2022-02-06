package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.lang.Exception
import kotlin.math.round
import java.text.DecimalFormat
import java.util.ArrayList

data class BudgetPeriod(var period: BudgetMonth, var who: String, var amount: Double, var occurence: Int) {
    fun isAnnualBudget(): Boolean {
        if (period.month == 0)
            return true
        else
            return false
    }
    fun getYear(): Int {
        return period.year
    }
}

data class BudgetOut(var amount: Double, var occurence: Int) {
    constructor(bPeriod: BudgetPeriod?) : this(0.0, 0) {
        if (bPeriod != null) {
            amount = bPeriod.amount
            occurence = bPeriod.occurence
        }
    }
}

data class BudgetAmountResponse(var dateApplicable: BudgetMonth, var who: String, var amount: Double, var dateStarted: BudgetMonth, var occurence: Int) {
    constructor() : this(BudgetMonth(0,0), "", 0.0, BudgetMonth(0,0), -1)

    fun isAnnualBudget(): Boolean {
        if (dateStarted.month == 0)
            return true
        else
            return false
    }
}

data class Budget(var categoryName: String) {
    val budgetPeriodList: MutableList<BudgetPeriod> = ArrayList()

    fun addBudgetPeriod(period: String, who: String, amount: Double, occurence: Int) {
        // budgets need to be added in chronological order in order for app to work
        budgetPeriodList.add(BudgetPeriod(BudgetMonth(period), who, amount, occurence))
        budgetPeriodList.sortWith(compareBy({ it.period.year }, { it.period.month }))
    }

    fun overlapsWithExistingBudget(period: String, who: String): Boolean {
        val pBudget = budgetPeriodList.find { it.period.toString() == period && it.who == who }
        if (pBudget != null) { // ie found it
            Log.d("Alex", "Found overlap " + period + " and " + pBudget.toString() )
            return true  // ie it overlaps
        }
        // if incoming is an annual, and there is an existing monthly
        val tempNewBudget = BudgetPeriod(BudgetMonth(period), who, 0.0, 0)
        if (tempNewBudget.isAnnualBudget()) {
            var ti: String
            for (i in 1..12) {
                if (i < 10)
                    ti = "0" + i.toString()
                else
                    ti = i.toString()
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
                    Log.d("Alex", "Found overlap with annual " + it.period.toString().substring(0,4) + " and " + tempNewBudget.getYear().toString() )
                    return true
                }
            }
        }
        return false
    }
}

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
            return singleInstance.budgets.size
        }

        fun showMe() {
            Log.d("Alex", "SHOW ME budgets " + singleInstance.budgets.size.toString())
            singleInstance.budgets.forEach {
                Log.d("Alex", "SM Budget category is '" + it.categoryName + "'")
                it.budgetPeriodList.forEach {
                    Log.d("Alex", "  period " + it.period + " who " + it.who + " amount " + it.amount)
                }
            }
        }

        fun getBudgetAmount(iCategory: String, iBudgetMonth: BudgetMonth, iWho: String): BudgetAmountResponse {
            val tResponse = BudgetAmountResponse()
            tResponse.dateApplicable.setValue(iBudgetMonth)
            tResponse.who = iWho
            if (iBudgetMonth.month == 0) {
                tResponse.dateStarted.setValue(iBudgetMonth)
                loop@for (i in 1..12) {
                    val tBudgetMonth = BudgetMonth(iBudgetMonth.year, i)
                    tResponse.dateApplicable.setValue(tBudgetMonth)
                    val  tmpBudget = getBudgetAmount(iCategory, tBudgetMonth, iWho)
                    tResponse.amount = tResponse.amount + tmpBudget.amount
                    if (tmpBudget.dateStarted.month == 0) { // ie it's an annual amount, so stop counting each month
                        break@loop
                    }
                }
                Log.d("Alex", "returning " + tResponse + " for annual amount for iCategory " + iCategory)
                return tResponse
            }
            val tFirstNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), SpenderViewModel.getSpenderName(0), 0.0, BudgetMonth(9999,12), -1)
            val tSecondNameBudget = BudgetAmountResponse(BudgetMonth(9999,12), SpenderViewModel.getSpenderName(1), 0.0, BudgetMonth(9999,12), -1)
            val tJointBudget = BudgetAmountResponse(BudgetMonth(9999,12), "Joint", 0.0, BudgetMonth(9999,12), -1)
            val myBudget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (myBudget != null) {
                myBudget.budgetPeriodList.forEach {
                    val tBudget = BudgetMonth(it.period)
                    if (it.who == SpenderViewModel.getSpenderName(0)) {
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
                    if (it.who == SpenderViewModel.getSpenderName(1)) {
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
                    if (it.who == "Joint") {
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
            }
            if (iWho == SpenderViewModel.getSpenderName(0)) {
                tResponse.amount = tFirstNameBudget.amount
                tResponse.dateApplicable.setValue(tFirstNameBudget.dateApplicable)
                tResponse.dateStarted.setValue(tFirstNameBudget.dateStarted)
                tResponse.occurence = tFirstNameBudget.occurence
            } else if (iWho == SpenderViewModel.getSpenderName(1)) {
                tResponse.amount = tSecondNameBudget.amount
                tResponse.dateApplicable.setValue(tSecondNameBudget.dateApplicable)
                tResponse.dateStarted.setValue(tSecondNameBudget.dateStarted)
                tResponse.occurence = tSecondNameBudget.occurence
            } else {
                    tResponse.amount = tJointBudget.amount
                    tResponse.dateApplicable.setValue(tJointBudget.dateApplicable)
                    tResponse.dateStarted.setValue(tJointBudget.dateStarted)
                    tResponse.occurence = tJointBudget.occurence
            }
            return tResponse
        }

        fun getTotalDiscretionaryBudgetForMonth(iCal: Calendar) : Double {
            var tmpTotal = 0.0
            CategoryViewModel.getCategories().forEach {
                if (it.discType == cDiscTypeDiscretionary) {
                    for (i in 0 until SpenderViewModel.getTotalCount()) {
                        if (SpenderViewModel.isActive(i)) {
                            val bpAmt = getBudgetAmount(
                                it.categoryName + "-" + it.subcategoryName,
                                BudgetMonth(iCal.get(Calendar.YEAR), iCal.get(Calendar.MONTH) + 1),
                                SpenderViewModel.getSpenderName(i)
                            )
                            if (bpAmt.dateStarted.month == 0) { // annual budget, so need to look at actuals as well
                                val lookupStartDate = BudgetMonth(iCal.get(Calendar.YEAR), 0)
                                val lookupEndDate =
                                    BudgetMonth(
                                        iCal.get(Calendar.YEAR),
                                        iCal.get(Calendar.MONTH) + 1
                                    )
                                val actualsThisMonth = ExpenditureViewModel.getActualsForPeriod(
                                    it.categoryName, it.subcategoryName,
                                    lookupEndDate, lookupEndDate, SpenderViewModel.getSpenderName(i)
                                )
                                var actualsEarlierThisYear = 0.0
                                if (lookupEndDate.month != 1)
                                    actualsEarlierThisYear =
                                        ExpenditureViewModel.getActualsForPeriod(
                                            it.categoryName, it.subcategoryName,
                                            lookupStartDate, lookupEndDate, SpenderViewModel.getSpenderName(i)
                                        )
                                val budgetRemaining =
                                    if (bpAmt.amount - actualsEarlierThisYear > 0.0) bpAmt.amount - actualsEarlierThisYear else 0.0
                                tmpTotal += if (actualsThisMonth < budgetRemaining) actualsThisMonth else budgetRemaining
                            } else {
                                tmpTotal += bpAmt.amount
                            }
                        }
                    }
                }
            }
            Log.d("Alex", "Discretionary budget for month is $tmpTotal")
            return tmpTotal
        }

        // goal of getBudgetCategories is to return list of categories that have budgets for the period indicated;
        // does not include Annuals
        fun getBudgetCategories(iBudgetMonth: BudgetMonth, iDiscFlag: String): MutableList<String> {
            val myList: MutableList<String> = ArrayList()
            var tBudgetAmount: Double
            singleInstance.budgets.forEach {
                tBudgetAmount = 0.0
                var tCategory = it.categoryName
                if (tCategory == "Cottage-Hydro")
                    tCategory = tCategory
                it.budgetPeriodList.forEach {
                    if (it.period.month == 0 && iBudgetMonth.month != 0) {
                        tBudgetAmount = 0.0
                    } else if ((iBudgetMonth.month != 0 && it.occurence == 0 && it.period.toString() <= iBudgetMonth.toString()) ||
                        (iBudgetMonth.month != 0 && it.occurence == 1 && it.period.toString() == iBudgetMonth.toString()) ||
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
            val tList: MutableList<BudgetInputRow> = ArrayList<BudgetInputRow>()
            var isAnnual = ""
            CategoryViewModel.getCategories().forEach {
                if (iCategory == it.categoryName + "-" + it.subcategoryName) {
                    if (it.discType != "Off") {
                        val fullCategoryName = it.categoryName + "-" + it.subcategoryName
                        val firstMonth = getFirstMonthOfBudget(fullCategoryName)
                        val lastMonth = getLastMonthOfBudget(fullCategoryName)
                        if (firstMonth != null && lastMonth != null) {
                            if (firstMonth.period.month == 0)
                                firstMonth.period.month = 1
                            if (lastMonth.period.month == 0)
                                lastMonth.period.month = 1
                            var monthIterator = firstMonth.period.toString()
                            while (monthIterator <= lastMonth.period.toString()) {
                                for (i in 0 until SpenderViewModel.getActiveCount()) {
                                    val spenderName = SpenderViewModel.getSpenderName(i)
                                    val  bAmount = getBudgetAmount(
                                        fullCategoryName,
                                        BudgetMonth(monthIterator),
                                        spenderName
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
                                        val tRow = BudgetInputRow(
                                            monthIterator,
                                            bAmount.amount.toString(),
                                            bAmount.who,
                                            bAmount.occurence.toString(),
                                            isAnnual,
                                            bAmount.dateStarted.toString()
                                        )
                                        tList.add(tRow)
                                    }
                                }
                                val bm = BudgetMonth(monthIterator)
                                bm.addMonth(1)
                                monthIterator = bm.toString()
                            }
                        }
                    }
                }
            }
            Log.d("Alex", "tlist count is " + tList.size)
            return tList
        }

        fun deleteBudget(iCategory: String, iPeriod: String, iWho: String) {
            val  budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                val  pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod && it.who == iWho }
                val ind = budget.budgetPeriodList.indexOf(pBudget)
                budget.budgetPeriodList.removeAt(ind)
            }

            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget")
                .child(iCategory)
                .child(iPeriod)
                .child(iWho)
                .removeValue()
        }
        fun updateBudget(iCategory: String, iPeriod: String, iWho: String, iAmount: Double, iOccurence: String, iLocalOnly: Boolean = false) {
            var budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget == null) { // first budget being added for this Category
                budget = Budget(iCategory)
                singleInstance.budgets.add(budget)
            }
                val pBudget = budget.budgetPeriodList.find { it.period.toString() == iPeriod && it.who == iWho}
                if (pBudget != null) {
                    val bm = BudgetMonth(iPeriod)
                    pBudget.period.year = bm.year
                    pBudget.period.month = bm.month
                    pBudget.who = iWho
                    pBudget.amount = iAmount
                    pBudget.occurence = if (iOccurence == cBUDGET_JUST_THIS_MONTH)  1 else 0
                } else {
                    budget.addBudgetPeriod(
                        iPeriod,
                        iWho,
                        iAmount,
                        if (iOccurence == cBUDGET_JUST_THIS_MONTH) 1 else 0
                    )
                }
            val budgetOut = BudgetOut(round(iAmount*100), if (iOccurence == cBUDGET_JUST_THIS_MONTH) 1 else 0)
            if (!iLocalOnly)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget")
                    .child(iCategory)
                    .child(iPeriod)
                    .child(iWho)
                    .setValue(budgetOut)
        }
        fun getBudget(iCategory: String): Budget? {
            return singleInstance.budgets.find { it.categoryName == iCategory }
        }
        private fun getFirstMonthOfBudget(iCategory: String): BudgetPeriod? {
            var tMonth = BudgetPeriod(BudgetMonth(9999,0), "", 0.0,0)
            val budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
/*                if (budget.budgetPeriodList.size > 0)
                    return BudgetPeriod(BudgetMonth(budget.budgetPeriodList[0].period), budget.budgetPeriodList[0].who, budget.budgetPeriodList[0].amount)
                else
                    return null */
                budget.budgetPeriodList.forEach {
                    if (it.period.toString() < tMonth.period.toString())
                        tMonth = BudgetPeriod(BudgetMonth(it.period), it.who, it.amount, it.occurence)
                }
                return tMonth
            }
            else
                return null
        }

        private fun getLastMonthOfBudget(iCategory: String): BudgetPeriod {
            var tMonth = BudgetPeriod(BudgetMonth(0,0), "", 0.0 ,0)
            val budget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (budget != null) {
                budget.budgetPeriodList.forEach {
                    if (it.period.toString() > tMonth.period.toString())
                        tMonth = BudgetPeriod(BudgetMonth(it.period), it.who, it.amount, it.occurence)
                }
            }
            return tMonth
        }

        fun budgetExistsForExactPeriod(iCategory: String, iBudgetMonth: BudgetMonth, iWho: String): Double {
            val myBudget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (myBudget != null) {
                myBudget.budgetPeriodList.forEach {
                    if (it.who == iWho) {
                        if (it.period.toString() == iBudgetMonth.toString())
                            return it.amount
                    }
                }
            }
            return 0.0
        }

        fun refresh() {
            singleInstance.loadBudgets()
        }

        fun clear() {
            if (singleInstance.budgetListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/NewBudget")
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
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/NewBudget")
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
                    it.children.forEach {
                        val tPeriod: String = it.key.toString()
                        for (element in it.children.toMutableList()) {
                            try {
                                val tWho: String = element.key.toString()
                                val tBudgetOut = BudgetOut(0.0, 0)
                                for (child in element.children) {
                                    if (child.key == "amount")
                                        tBudgetOut.amount = child.value.toString().toDouble() / 100.0
                                    else if (child.key == "occurence")
                                        tBudgetOut.occurence = child.value.toString().toInt()
                                    else
                                        Log.d(
                                            "Alex",
                                            "What is this???? (" + child.key + " " + child.value + " in budget load..."
                                        )
                                }
                                budgets[budgets.indexOf(myB)].addBudgetPeriod(
                                    tPeriod,
                                    tWho,
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
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget").addValueEventListener(
            budgetListener as ValueEventListener
        )
    }
}

interface DataUpdatedCallback  {
    fun onDataUpdate()
}