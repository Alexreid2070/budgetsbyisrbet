package com.isrbet.budgetsbyisrbet

import android.icu.text.NumberFormat
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.lang.Exception
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round

data class BudgetPeriod(var startDate: MyDate,
                        var who: Int = 2,
                        var amount: Double = 0.0,
                        var period: String = cPeriodMonth,
                        var regularity: Int = 1,  // ie each period
                        var occurence: Int = 0, // ie recurring
                        var applicableDate: MyDate = MyDate(startDate),
                        var key: String = "") {
    fun isAnnualBudget(): Boolean {
        return (period == cPeriodYear)
    }
    operator fun compareTo(iBP: BudgetPeriod): Int {
        return startDate.compareTo(iBP.startDate)
    }

    private fun getMinimalEndDate() : MyDate {
        return MyDate(startDate).increment(period, regularity)
    }

    fun getEndDate() : MyDate {
        return if (occurence == 0)
            MyDate(9999, 12, 31)
        else {
            getMinimalEndDate()
        }
    }

    fun getBudgetAmount(iBudgetMonth: MyDate) : Double? {
        val tStartDate = startDate.getYYYYMM()
        val tiBudgetMonth = iBudgetMonth.getYYYYMM()

        if (occurence == 0 && tStartDate <= tiBudgetMonth) { // occurence of 0 means recurring
            // this is a recurring budget, but regularity may not be every month; there could be multiple weeks of budget in a month
            val tDate = MyDate(startDate)
            if (period == cPeriodYear) {
                if (regularity == 1) {
                    return amount
                } else {
                    while (tDate.getYear() < iBudgetMonth.getYear()) {
                        tDate.increment(period, regularity)
                    }
                    if (tDate.getYear() == iBudgetMonth.getYear())
                        return amount
                }
            } else if (period == cPeriodMonth) {
                if (regularity == 1) {
                    return amount
                } else {
                    while (tDate < iBudgetMonth) {
                        tDate.increment(period, regularity)
                    }
                    if (tDate.getYear() == iBudgetMonth.getYear() && tDate.getMonth() == iBudgetMonth.getMonth())
                        return amount
                }
            } else { // weekly budget, there could be multiple budget occurrences in the chosen month
                val daysBetween = ChronoUnit.DAYS.between(startDate.theDate, iBudgetMonth.theDate)
                val numOfPeriodsBetween = (daysBetween / (7.0*regularity)).toInt()
                tDate.increment(period, numOfPeriodsBetween*regularity)  // this will get us very close to the desired date.
                //       I limit the use of "increment" because it's a performance hog.
                while (tDate < iBudgetMonth) {
                    tDate.increment(period, regularity)
                }
                var tAmount = 0.0
                while (tDate.getYear() == iBudgetMonth.getYear() && tDate.getMonth() == iBudgetMonth.getMonth()) {
                    tAmount += amount
                    tDate.increment(period, regularity)
                }
                return tAmount
            }
        }
        if (occurence == 1 && // only occurs once
            (period == cPeriodYear && startDate.getYear() == iBudgetMonth.getYear()) ||  // it's an annual budget
                ((period == cPeriodMonth || period == cPeriodWeek) &&
                        startDate.getYear() == iBudgetMonth.getYear() &&  // it's a monthly budget
                        startDate.getMonth() == iBudgetMonth.getMonth())) {
            return amount
        }
        return null
    }
}

data class BudgetPeriodOut(var amount: Double,
                     var period: String = cPeriodMonth,
                     var regularity: Int = 1,
                     var occurence: Int,
                     var startDate: String,
                     var who: Int)

data class Budget(var categoryID: Int) {
    val budgetPeriodList: MutableList<BudgetPeriod> = ArrayList()

    fun addBudgetPeriod(iNewBudgetPeriod: BudgetPeriod) {
        // budgets need to be added in chronological order in order for app to work
        budgetPeriodList.add(iNewBudgetPeriod)
        budgetPeriodList.sortWith(compareBy({ it.startDate.toString() }, { it.who }))
    }

    fun overlapsWithExistingBudget(iKey: String, iStartDate: MyDate, iWho: Int): Boolean {
        budgetPeriodList.forEach {
            if (it.startDate == iStartDate && it.who == iWho && it.key != iKey) {
                return true
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
                    Log.d("Alex", "  date " + child.startDate + " period " + child.period + " reg " + child.regularity + " who " + child.who + " amount " + child.amount)
                }
            }
        } */

        fun getCategoryBudgets(iPeriod: DateRangeEnum, iBudgetMonth: MyDate, iDiscType: String, iWho: Int,
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

        fun getCalculatedBudgetAmount(iPeriod: DateRangeEnum, iBudgetMonth: MyDate, iCategoryID: Int, iWhoToLookup: Int): Double {
            var tBudgetAmount = 0.0
            if (iPeriod == DateRangeEnum.ALLTIME) {
                val dateNow = MyDate.now()
                val lastAnnualYear = if (dateNow.getMonth() == 12) // ie Dec
                    dateNow.getYear()
                else
                    dateNow.getYear() - 1
                // add annual budgets for previous years, and current year if it is Dec
                val earliestYear = TransactionViewModel.getEarliestYear()
                for (i in earliestYear until lastAnnualYear+1) {
                    tBudgetAmount += getCalculatedBudgetAmount(
                        MyDate(i),
                        iCategoryID,
                        iWhoToLookup
                    )
                }
                // then add YTD for current year (if it's not Dec)
                if (dateNow.getMonth() != 12) {
                    for (i in 1 until iBudgetMonth.getMonth() + 1) {
                        tBudgetAmount += getCalculatedBudgetAmount(
                            MyDate(iBudgetMonth.getYear(), i, 1),
                            iCategoryID,
                            iWhoToLookup
                        )
                    }
                }
            } else if (iPeriod == DateRangeEnum.YTD) {
                for (i in 1 until iBudgetMonth.getMonth() + 1) {
                    tBudgetAmount += getCalculatedBudgetAmount(
                        MyDate(iBudgetMonth.getYear(), i, 1),
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

        fun getCalculatedBudgetAmount(iBudgetMonth: MyDate, iCategoryID: Int, iWhoToLookup: Int): Double {
            // if iWhoToLookup is blank, then will calculate for all Spenders
            // if iWhoToLookup is a specific name, then include the right % of Joint
            var tBudgetAmount = 0.0

            // get original budgets for all 3 possible users
            val budgetForPeriodUser0 = if (iWhoToLookup != 1)
                getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 0)
            else
                BudgetPeriod(MyDate(gCurrentDate))
            val budgetForPeriodUser1 = if (iWhoToLookup != 0 && SpenderViewModel.getActiveCount() > 1)
                getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 1)
            else
                BudgetPeriod(MyDate(gCurrentDate))
            val budgetForPeriodUser2 = if (SpenderViewModel.getActiveCount() > 1)
                getOriginalBudgetAmount(iCategoryID, iBudgetMonth, 2)
            else
                BudgetPeriod(MyDate(gCurrentDate))

            val actualsForUser0PriorMonths = if (iWhoToLookup != 1 &&
                    iBudgetMonth.getMonth() != 1 &&
                    !iBudgetMonth.representsYear &&
                    (budgetForPeriodUser0.period == cPeriodYear  ||
                        budgetForPeriodUser2.period == cPeriodYear))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    MyDate(iBudgetMonth.getYear(), 1, 1),
                    MyDate(iBudgetMonth.getYear(), iBudgetMonth.getMonth() - 1, 1),
                    0)
                else
                    0.0 // ie not relevant

            val actualsForUser0ThisMonth = if (iWhoToLookup != 1 &&
                !iBudgetMonth.representsYear &&
                (budgetForPeriodUser0.period == cPeriodYear ||
                        budgetForPeriodUser2.period == cPeriodYear)) { // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    iBudgetMonth,
                    iBudgetMonth,
                    0
                )
            } else
                0.0 // ie not relevant
            val actualsForUser1PriorMonths = if (iWhoToLookup != 0 &&
                iBudgetMonth.getMonth() != 1 &&
                !iBudgetMonth.representsYear &&
                (budgetForPeriodUser1.period == cPeriodYear ||
                    budgetForPeriodUser2.period == cPeriodYear))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    MyDate(iBudgetMonth.getYear(), 1, 1),
                    MyDate(iBudgetMonth.getYear(), iBudgetMonth.getMonth() - 1, 1),
                    1)
            else
                0.0 // ie not relevant

            val actualsForUser1ThisMonth = if (iWhoToLookup != 0 &&
                !iBudgetMonth.representsYear &&
                (budgetForPeriodUser1.period == cPeriodYear  ||
                        budgetForPeriodUser2.period == cPeriodYear))  // ie not an annual view, and is an annual budget
                TransactionViewModel.getActualsForPeriod(
                    iCategoryID,
                    iBudgetMonth,
                    iBudgetMonth,
                    1)
            else
                0.0 // ie not relevant

            if (iWhoToLookup == 0 || iWhoToLookup == 2) {
                var budgetRemaining = 0.0
                var totalAnnualBudget = 0.0
                if (!iBudgetMonth.representsYear && budgetForPeriodUser0.period == cPeriodYear) { // ie not an annual view, and is an annual budget
                    // handle user 0's annual budget
                    totalAnnualBudget = budgetForPeriodUser0.amount
                } else { // else just add user 0's non-annual budget
                    tBudgetAmount += budgetForPeriodUser0.amount
                }
                if (!iBudgetMonth.representsYear && budgetForPeriodUser2.period == cPeriodYear) { // ie not an annual view, and is an annual budget
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
                if (!iBudgetMonth.representsYear && budgetForPeriodUser1.period == cPeriodYear) { // ie not an annual view, and is an annual budget
                    // handle user 0's annual budget
                    totalAnnualBudget = budgetForPeriodUser1.amount
                } else { // else just add user 0's non-annual budget
                    tBudgetAmount += budgetForPeriodUser1.amount
                }
                if (!iBudgetMonth.representsYear && budgetForPeriodUser2.period == cPeriodYear) { // ie not an annual view, and is an annual budget
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

        fun getOriginalBudgetAmount(iCategoryID: Int, iBudgetMonth: MyDate, iWho: Int): BudgetPeriod {
            val tResponse = BudgetPeriod(MyDate(gCurrentDate))
            tResponse.applicableDate = MyDate(iBudgetMonth)
            tResponse.who = iWho
            if (iBudgetMonth.representsYear) {
                tResponse.startDate = MyDate(iBudgetMonth)
                loop@for (i in 1..12) {
                    val tBudgetMonth = MyDate(iBudgetMonth.getYear(), i, 1)
                    tResponse.applicableDate = MyDate(tBudgetMonth)
                    val  tmpBudget = getOriginalBudgetAmount(iCategoryID, tBudgetMonth, iWho)
                    tResponse.amount = tResponse.amount + tmpBudget.amount
                    if (tmpBudget.isAnnualBudget()) { // ie it's an annual amount, so stop counting each month
                        break@loop
                    }
                }
                return tResponse
            }
            val tUserBudgets = arrayOf (
                BudgetPeriod(MyDate(9999, 12, 1)),
                BudgetPeriod(MyDate(9999, 12, 1)),
                BudgetPeriod(MyDate(9999, 12, 1))
                    )
            val myBudget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (myBudget != null) {
                val tList = myBudget.budgetPeriodList.filter { it.who == iWho &&
                        it.startDate.getYYYYMM() <= iBudgetMonth.getYYYYMM()}
                if (tList.isNotEmpty()) {
                    var foundRecurringBudget = false
                    for (i in tList.size - 1 downTo 0) {
                        if ((tList[i].occurence == 1 &&
                            ((tList[i].period != cPeriodYear && tList[i].startDate.getYear() == iBudgetMonth.getYear() && tList[i].startDate.getMonth() == iBudgetMonth.getMonth()) ||
                                (tList[i].period == cPeriodYear && tList[i].startDate.getYear() == iBudgetMonth.getYear()))) ||
                            (tList[i].occurence == 0)) {
                            val tAmount = tList[i].getBudgetAmount(iBudgetMonth)
                            if (tAmount != null &&
                                ((!foundRecurringBudget && tList[i].occurence == 0) ||
                                        tList[i].occurence == 1)) {
                                tUserBudgets[iWho].amount += tAmount
                                tUserBudgets[iWho].applicableDate = MyDate(tList[i].startDate)
                                tUserBudgets[iWho].startDate = MyDate(tList[i].startDate)
                                tUserBudgets[iWho].occurence = tList[i].occurence
                                tUserBudgets[iWho].regularity = tList[i].regularity
                                tUserBudgets[iWho].period = tList[i].period
                                if (tList[i].startDate.getDay() == 1)
                                    break // for
                            }
                            if (tList[i].occurence == 0 &&
                                tList[i].startDate < iBudgetMonth) // ie if it's a weekly budget, need to keep looking for possible monthly
                                foundRecurringBudget = true
                        }
                    }
                }
            }

            tResponse.amount = tUserBudgets[iWho].amount
            tResponse.applicableDate = MyDate(tUserBudgets[iWho].applicableDate)
            tResponse.startDate = MyDate(tUserBudgets[iWho].startDate)
            tResponse.occurence = tUserBudgets[iWho].occurence
            tResponse.regularity = tUserBudgets[iWho].regularity
            tResponse.period = tUserBudgets[iWho].period

            if (tResponse.applicableDate.representsYear)
                tResponse.applicableDate.setMonth(1)
            return tResponse
        }

        fun getBudgetInputRows(iBudgetMonth: MyDate): MutableList<BudgetInputRow> {
            val myList: MutableList<BudgetInputRow> = ArrayList()
            for (budget in singleInstance.budgets) {
                val categoryID = budget.categoryID
                val cat = CategoryViewModel.getCategory(categoryID)
                if ((cat?.inUse == true) && cat.iAmAllowedToSeeThisCategory()) {
                    for ( userIndex in 0 until SpenderViewModel.getActiveCount()) {
                        for (i in budget.budgetPeriodList.size -1 downTo  0 ) {
                            if (budget.budgetPeriodList[i].who == userIndex) {
                                if (budget.budgetPeriodList[i].startDate <= iBudgetMonth ||
                                    (budget.budgetPeriodList[i].period == cPeriodWeek &&
                                            budget.budgetPeriodList[i].startDate.getYYYYMM() <= iBudgetMonth.getYYYYMM())) {
                                    //                                val tAmt = budget.getBudgetPeriodForMonth(DateRange(iBudgetMonth, MyDate(iBudgetMonth.year, iBudgetMonth.month, 31)))
                                    if (budget.budgetPeriodList[i].getEndDate() > iBudgetMonth) {
                                        myList.add(
                                            BudgetInputRow(
                                                budget.budgetPeriodList[i].key,
                                                categoryID,
                                                budget.budgetPeriodList[i].startDate.toString(),
                                                budget.budgetPeriodList[i].amount.toString(),
                                                budget.budgetPeriodList[i].who,
                                                budget.budgetPeriodList[i].occurence.toString(),
                                                budget.budgetPeriodList[i].startDate.toString(),
                                                budget.budgetPeriodList[i].period,
                                                budget.budgetPeriodList[i].regularity,
                                                cBudgetDateView
                                            )
                                        )
                                        break // ie stop looking for latest budget
                                    }
                                }
                            }
                        }
                    }
                }
            }
            myList.sortWith(compareBy({ it.categoryPriority }, { it.subcategory }))
            return myList
        }

        fun getBudgetInputRows(iCategoryID: Int): MutableList<BudgetInputRow> {
            val tList: MutableList<BudgetInputRow> = ArrayList()

            val budgets = getBudget(iCategoryID)
            if (budgets != null) {
                if (budgets.budgetPeriodList.size != 0) {
                    for (b in budgets.budgetPeriodList) {
                        val tRow = BudgetInputRow(
                            b.key,
                            iCategoryID,
                            b.startDate.toString(),
                            b.amount.toString(),
                            b.who,
                            b.occurence.toString(),
                            b.startDate.toString(),
                            b.period,
                            b.regularity,
                            cBudgetCategoryView
                        )
                        tList.add(tRow)
                    }
                }
            }
/*            CategoryViewModel.getCategories(true).forEach {
                if (iCategoryID == it.id) {
//                    if (it.discType != cDiscTypeOff) {
//                        val firstMonth = getFirstMonthOfBudget(iCategoryID)
  //                      val lastMonth = getLastMonthOfBudget(iCategoryID)
   //                 Log.d("Alex", "firstMonth is $firstMonth and last is $lastMonth")

/*                        if (firstMonth != null) {
/*                            if (firstMonth.startDate.month == 0)
                                firstMonth.startDate.month = 1
                            if (lastMonth.startDate.month == 0)
                                lastMonth.startDate.month = 1 */
                            var monthIterator = firstMonth.startDate.toString()
                            while (monthIterator <= lastMonth.startDate.toString()) {
                                for (i in 0 until SpenderViewModel.getActiveCount()) {
                                    val  bAmount = getOriginalBudgetAmount(
                                        iCategoryID,
                                        MyDate(monthIterator),
                                        i
                                    )
/*                                    isAnnual = if (bAmount.isAnnualBudget()) {
                                        MyApplication.getString(R.string.yes_short)
                                    } else
                                        "" */

                                    val tDateApplicable = MyDate(monthIterator)
                                    if ( tDateApplicable.toString() == bAmount.startDate.toString() ||
                                        (tDateApplicable.month == 1 && bAmount.isAnnualBudget())
                                        && tDateApplicable.year == bAmount.startDate.year) {
                                        val tRow = BudgetInputRow(
                                            iCategoryID,
                                            monthIterator,
                                            bAmount.amount.toString(),
                                            bAmount.who,
                                            bAmount.occurence.toString(),
                                            bAmount.startDate.toString(),
                                            bAmount.period,
                                            bAmount.regularity,
                                            cBudgetCategoryView
                                        )
                                        tList.add(tRow)
                                    }
                                }
                                val bm = MyDate(monthIterator)
                                bm.increment(cPeriodMonth, 1)
                                monthIterator = bm.toString()
                            }
//                        }
                    } */
                }
            } */
            return tList
        }

/*        fun deleteBudget(iCategoryID: Int, iDate: MyDate, iWho: Int) {
            val  budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget != null) {
                val  pBudget = budget.budgetPeriodList.find { it.startDate.compareTo(iDate) == 0 && it.who == iWho }
                val ind = budget.budgetPeriodList.indexOf(pBudget)
                budget.budgetPeriodList.removeAt(ind)
            }

            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                .child(iCategoryID.toString())
                .child(iDate.toString())
                .child(iWho.toString())
                .removeValue()
        } */
        fun deleteBudget(iCategoryID: Int, iKey: String) {
        Timber.tag("Alex").d("deleting $iKey from $iCategoryID")
            val  budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget != null) {
                val  pBudget = budget.budgetPeriodList.find { it.key == iKey }
                val ind = budget.budgetPeriodList.indexOf(pBudget)
                budget.budgetPeriodList.removeAt(ind)
            }

            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/BudgetNew")
                .child(iCategoryID.toString())
                .child(iKey)
                .removeValue()
        }
/*        fun updateBudget(iCategoryID: Int, iStartDate: MyDate, iWho: Int, iAmount: Double,
                         iPeriod: String, iRegularity: Int, iOccurence: Int, iLocalOnly: Boolean = false) {
            var budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget == null) { // first budget being added for this Category
                budget = Budget(iCategoryID)
                singleInstance.budgets.add(budget)
            }
            val pBudget = budget.budgetPeriodList.find { it.startDate.compareTo(iStartDate) == 0 && it.who == iWho}
            if (pBudget != null) {
                pBudget.startDate.year = iStartDate.year
                pBudget.startDate.month = iStartDate.month
                pBudget.who = iWho
                pBudget.amount = iAmount
                pBudget.occurence = iOccurence
            } else {
                budget.addBudgetPeriod(BudgetPeriod(
                    iStartDate,
                    iWho,
                    iAmount,
                    iPeriod,
                    iRegularity,
                    iOccurence
                ))
            }

            val budgetOut = BudgetPeriodOut(round(iAmount*100), iPeriod, iRegularity, iOccurence, iStartDate.toString(), iWho)
            if (!iLocalOnly)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                    .child(iCategoryID.toString())
                    .child(iStartDate.toString())
                    .child(iWho.toString())
                    .setValue(budgetOut)
        } */
        fun updateBudget(iKey: String, iCategoryID: Int, iStartDate: MyDate, iWho: Int, iAmount: Double,
                         iPeriod: String, iRegularity: Int, iOccurence: Int, iLocalOnly: Boolean = false) {
            var budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget == null) { // first budget being added for this Category
                budget = Budget(iCategoryID)
                singleInstance.budgets.add(budget)
            }
            val pBudget = budget.budgetPeriodList.find { it.key == iKey}
            if (pBudget != null) {
                pBudget.startDate = MyDate(iStartDate)
                pBudget.who = iWho
                pBudget.amount = iAmount
                pBudget.occurence = iOccurence
                pBudget.period = iPeriod
                pBudget.regularity = iRegularity
            } else {
                budget.addBudgetPeriod(BudgetPeriod(
                    iStartDate,
                    iWho,
                    iAmount,
                    iPeriod,
                    iRegularity,
                    iOccurence
                ))
            }

            val budgetOut = BudgetPeriodOut(round(iAmount*100), iPeriod, iRegularity, iOccurence, iStartDate.toString(), iWho)
            if (!iLocalOnly)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/BudgetNew")
                    .child(iCategoryID.toString())
                    .child(iKey)
                    .setValue(budgetOut)
        }
        fun addBudget(iCategoryID: Int, iStartDate: MyDate, iWho: Int, iAmount: Double,
                         iPeriod: String, iRegularity: Int, iOccurence: Int, iLocalOnly: Boolean = false) {
            var budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget == null) { // first budget being added for this Category
                budget = Budget(iCategoryID)
                singleInstance.budgets.add(budget)
            }

            budget.addBudgetPeriod(BudgetPeriod(
                iStartDate,
                iWho,
                iAmount,
                iPeriod,
                iRegularity,
                iOccurence
            ))

            val budgetOut = BudgetPeriodOut(round(iAmount*100), iPeriod, iRegularity, iOccurence, iStartDate.toString(), iWho)
            if (!iLocalOnly) {
                val budgetKey: String =
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/BudgetNew" +
                            "/" + iCategoryID.toString())
                        .push().key.toString()

                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/BudgetNew")
                    .child(iCategoryID.toString())
                    .child(budgetKey)
                    .setValue(budgetOut)
            }
        }

        fun checkNewBudget(iOldBudgetKey: String, iCategoryID: Int, iStartDate: MyDate, iWho: Int, iAmount: String,
                         iPeriod: String, iRegularity: String) : String {
            // Make sure there are values set for all fields, but it's impossible to not have
            // category, date, who, occurence, or period set
            if (iAmount == "") {
                return MyApplication.getString(R.string.value_cannot_be_blank)
            }
            val lNumberFormat: NumberFormat = NumberFormat.getInstance()
            val amountDouble = lNumberFormat.parse(iAmount).toDouble()
            if (amountDouble == 0.0) {
                return MyApplication.getString(R.string.value_cannot_be_zero)
            }
            val regularityInt: Int
            try {
                regularityInt = Integer.parseInt(iRegularity)
                if (regularityInt < 1) {
                    return MyApplication.getString(R.string.regularity_must_be_positive)
                }
            }
            catch (exception: Exception) {
                return MyApplication.getString(R.string.regularity_must_be_positive)
            }

            if (iPeriod == cPeriodYear && (iStartDate.getMonth() != 1 || iStartDate.getDay() != 1)) {
                return MyApplication.getString(R.string.annual_budgets_must_start_on_Jan_1)
            }
            if (iPeriod == cPeriodMonth && iStartDate.getDay() != 1) {
                return MyApplication.getString(R.string.monthly_budgets_must_start_on_1)
            }

            val budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            if (budget != null) {
                if (budget.overlapsWithExistingBudget(
                        iOldBudgetKey,
                        iStartDate,
                        iWho)) {
                    return MyApplication.getString(R.string.budgetOverlap)
                }
            }
            return ""
        }
        private fun getBudget(iCategoryID: Int): Budget? {
            return singleInstance.budgets.find { it.categoryID == iCategoryID }
        }
/*        private fun getFirstMonthOfBudget(iCategoryID: Int): BudgetPeriod? {
            var tMonth = BudgetPeriod(MyDate(9999,0, 1))
            val budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            return if (budget != null) {
                budget.budgetPeriodList.forEach {
                    if (it.startDate.toString() < tMonth.startDate.toString())
                        tMonth = BudgetPeriod(it.startDate, it.who, it.amount, it.period, it.regularity, it.occurence)
                }
                tMonth
            } else
                null
        }

        private fun getLastMonthOfBudget(iCategoryID: Int): BudgetPeriod {
            var tMonth = BudgetPeriod(MyDate(0,0,0))
            val budget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            Log.d("Alex", "there are ${budget?.budgetPeriodList?.size} items")
            budget?.budgetPeriodList?.forEach {
                Log.d("Alex", "Item: ${it.startDate}")
            }
            budget?.budgetPeriodList?.forEach {
                Log.d("Alex", "checking if ${it.startDate} > ${tMonth.startDate}")
                if (it.startDate.toString() > tMonth.startDate.toString())
                    tMonth = BudgetPeriod(it.startDate, it.who, it.amount, it.period, it.regularity, it.occurence)
            }
            return tMonth
        } */

        fun budgetExistsForExactPeriod(iCategoryID: Int, iBudgetMonth: MyDate, iWho: Int): Double {
            val myBudget = singleInstance.budgets.find { it.categoryID == iCategoryID }
            myBudget?.budgetPeriodList?.forEach {
                if (it.who == iWho) {
                    if (it.startDate.toString() == iBudgetMonth.toString())
                        return it.amount
                }
            }
            return 0.0
        }

        fun refresh() {
            singleInstance.loadBudgetNews()
        }

        fun clear() {
            if (singleInstance.budgetListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/BudgetNew")
                    .removeEventListener(singleInstance.budgetListener!!)
                singleInstance.budgetListener = null
            }
//            singleInstance.dataUpdatedCallback = null
            singleInstance.budgets.clear()
            singleInstance.loaded = false
        }

        fun migrateBudgets() {
            singleInstance.budgets.forEach {
                val cat = it.categoryID
                for (child in it.budgetPeriodList) {
                    val budgetOut = BudgetPeriodOut(round(child.amount*100), child.period,
                        child.regularity, child.occurence, child.startDate.toString(), child.who)
                    val budgetKey: String =
                        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/BudgetNew" +
                                "/" + cat.toString())
                            .push().key.toString()
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/BudgetNew")
                        .child(cat.toString())
                        .child(budgetKey)
                        .setValue(budgetOut)
                }
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        Timber.tag("Alex").d( "WHY IS BUDGET onCleared being called??")
        super.onCleared()
        if (budgetListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/BudgetNew")
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
                        val tStartDate = MyDate(budget.key.toString())
                        for (element in budget.children.toMutableList()) {
                            try {
                                val tWho = element.key.toString().toInt()

                                val tBudgetOut = BudgetPeriodOut(0.0,
                                    if (budget.key.toString().substring(5,7) == "00") cPeriodYear else cPeriodMonth,
                                    1,
                                    0,
                                     "2000-01-01",
                                0)
                                for (child in element.children) {
                                    when (child.key) {
                                        "amount" -> tBudgetOut.amount = child.value.toString().toDouble() / 100.0
                                        "period" -> tBudgetOut.period = child.value.toString()
                                        "regularity" -> tBudgetOut.regularity = child.value.toString().toInt()
                                        "occurence" -> tBudgetOut.occurence = child.value.toString().toInt()
                                    }
                                }
                                budgets[budgets.indexOf(myB)].addBudgetPeriod(BudgetPeriod(
                                    tStartDate,
                                    tWho,
                                    tBudgetOut.amount,
                                    tBudgetOut.period,
                                    tBudgetOut.regularity,
                                    tBudgetOut.occurence
                                ))
                            } catch (exception: Exception) {
                            }
                        }
                    }
                }
                loaded = true
//                showMe()
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
    fun loadBudgetNews() {
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
                        val tKey = budget.key.toString()
                        val tBudgetOut = BudgetPeriodOut(0.0,
                            cPeriodMonth,
                            1,
                            0,
                            "2000-01-01",
                            0)
                        try {
                            for (child in budget.children) {
                                when (child.key) {
                                    "startDate" -> tBudgetOut.startDate = child.value.toString()
                                    "who" -> tBudgetOut.who = child.value.toString().toInt()
                                    "amount" -> tBudgetOut.amount = child.value.toString().toDouble() / 100.0
                                    "period" -> tBudgetOut.period = child.value.toString()
                                    "regularity" -> tBudgetOut.regularity = child.value.toString().toInt()
                                    "occurence" -> tBudgetOut.occurence = child.value.toString().toInt()
                                }
                            }
                            budgets[budgets.indexOf(myB)].addBudgetPeriod(BudgetPeriod(
                                MyDate(tBudgetOut.startDate),
                                tBudgetOut.who,
                                tBudgetOut.amount,
                                tBudgetOut.period,
                                tBudgetOut.regularity,
                                tBudgetOut.occurence,
                                MyDate(tBudgetOut.startDate),
                                tKey
                            ))
                        } catch (exception: Exception) {
                        }
                    }
                }
                loaded = true
//                showMe()
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 104.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/BudgetNew").addValueEventListener(
            budgetListener as ValueEventListener
        )
    }
}

interface DataUpdatedCallback  {
    fun onDataUpdate()
}