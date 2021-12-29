package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class BudgetPeriod(var period: String, var amount: Double) {
}

data class Budget(var categoryName: String) {
    val budgetPeriodList: MutableList<BudgetPeriod> = ArrayList()

    fun addBudgetPeriod(period: String, amount: Double) {
        if (!budgetPeriodList.contains(BudgetPeriod(period, amount)))
            budgetPeriodList.add(BudgetPeriod(period, amount))
    }

    fun getBudgetPeriods(): MutableList<BudgetPeriod> {
        return budgetPeriodList
    }
}

class BudgetViewModel : ViewModel() {
    lateinit var budgetListener: ValueEventListener
    private val budgets: MutableList<Budget> = ArrayList()
    var dataUpdatedCallback: BudgetDataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: BudgetViewModel // used to track static single instance of self
        fun showMe() {
            Log.d("Alex", "SHOW ME budgets " + singleInstance.budgets.size.toString())
            singleInstance.budgets.forEach {
                Log.d("Alex", "SM Budget is " + it)
                it.budgetPeriodList.forEach {
                    Log.d("Alex", "  period " + it.period + " amount " + it.amount)
                }
            }
        }

        fun getBudgetAmount(iCategory: String, iBudgetMonth: BudgetMonth): BudgetPeriod {
            var tBudgetPeriod: BudgetPeriod = BudgetPeriod("", 0.0)
            if (iBudgetMonth.month == 0) {
                var tAmount: Double = 0.0
                for (i in 1..12) {
                    var tBudgetMonth: BudgetMonth = BudgetMonth(iBudgetMonth.year, i)
                    var tmpBudgetPeriod: BudgetPeriod = getBudgetAmount(iCategory, tBudgetMonth)
                    tAmount += (tmpBudgetPeriod.amount)
                }
                tBudgetPeriod = BudgetPeriod(iBudgetMonth.year.toString(), tAmount)
                Log.d("Alex", "returning " + tAmount + " for annual amount for iCategory " + iCategory)
                return tBudgetPeriod
            }
            val myBudget = singleInstance.budgets.find { it.categoryName == iCategory }
            if (myBudget != null) {
                var tAmount: Double = 0.0
                myBudget.budgetPeriodList.forEach {
                    if (it.period <= iBudgetMonth.toString()) {
                        tBudgetPeriod = BudgetPeriod(it.period, (it.amount/100))
                    }
                }
                return tBudgetPeriod
            } else
                return tBudgetPeriod
        }

        fun getTotalDiscretionaryBudgetForMonth(iCal: Calendar) : Double {
            var tmpTotal: Double = 0.0
            CategoryViewModel.getCategories().forEach {
                if (it.discType == cDiscTypeDiscretionary) {
                    var bpAmt = getBudgetAmount(
                        it.categoryName + "-" + it.subcategoryName,
                        BudgetMonth(iCal.get(Calendar.YEAR), iCal.get(Calendar.MONTH) + 1)
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
                    if ((iBudgetMonth.month != 0 && it.period <= iBudgetMonth.toString()) ||
                        (iBudgetMonth.month == 0 && it.period.substring(
                            0,
                            4
                        ) <= iBudgetMonth.toString())
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

        fun getBudgetInputRows(iStartMonth: BudgetMonth): MutableList<BudgetInputRow> {
            var tList: MutableList<BudgetInputRow> = ArrayList<BudgetInputRow>()
            CategoryViewModel.getCategories().forEach {
                val fullCategoryName = it.categoryName + "-" + it.subcategoryName
                val mplus1 = BudgetMonth(iStartMonth.year, iStartMonth.month)
                val mplus2 = BudgetMonth(iStartMonth.year, iStartMonth.month)
                val mplus3 = BudgetMonth(iStartMonth.year, iStartMonth.month)
                mplus1.addMonth()
                mplus2.addMonth(2)
                mplus3.addMonth(3)
                var m1bp = getBudgetAmount(fullCategoryName, iStartMonth)
                var m2bp = getBudgetAmount(fullCategoryName, mplus1)
                var m3bp = getBudgetAmount(fullCategoryName, mplus2)
                var m4bp = getBudgetAmount(fullCategoryName, mplus3)
                if (fullCategoryName == "Housing-Condo-Fees") {
                    Log.d("Alex", fullCategoryName + " " + m1bp + " " + m2bp + " " + m3bp + " " + m4bp)
                }

                var tRow: BudgetInputRow = BudgetInputRow(
                    fullCategoryName,
                    m1bp.amount.toString(),
                    m2bp.amount.toString(),
                    m3bp.amount.toString(),
                    m4bp.amount.toString(),
                    m1bp.period,
                    m2bp.period,
                    m3bp.period,
                    m4bp.period
                )
                tList.add(tRow)
            }
            return tList
        }

        fun deleteBudget(iCategory: String, iMonth: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                .child(iCategory)
                .child(iMonth)
                .removeValue()
        }
        fun updateBudget(iCategory: String, iMonth: String, iAmount: Double) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                .child(iCategory)
                .child(iMonth)
                .setValue(iAmount)
        }
        fun refresh() {
            singleInstance.loadBudgets()
        }
    }

    init {
        BudgetViewModel.singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Budget")
            .removeEventListener(budgetListener)
    }

    fun getBudgets(): MutableList<Budget> {
        return budgets
    }

    fun getBudgetcount(): Int {
        return budgets.size
    }

    fun setCallback(iCallback: BudgetDataUpdatedCallback?) {
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
                    it.children.forEach {
                        budgets[budgets.indexOf(myB)].addBudgetPeriod(
                            it.key.toString(),
                            it.value.toString().toDouble()
                        )
                    }
                }
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget").addValueEventListener(budgetListener)
    }
}

public interface BudgetDataUpdatedCallback  {
    fun onDataUpdate()
}