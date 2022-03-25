package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*

data class Expenditure(
    var date: String = "",
    var amount: Int = 0,
    var category: Int = 0,
    var note: String = "",
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var type: String = "Expense",
    var bfname1split: Int = 0,
    var mykey: String = ""
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    fun setValue(key: String, value: String) {
        when (key) {
            "key" -> mykey = value.trim()
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "note" -> note = value.trim()
            "type" -> type = value.trim()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
            else -> {
                if (key != "bfname2split") Log.d("Alex", "Unknown field in Expenditures $key $value $this")
            }
        }
    }

    fun contains(iSubString: String): Boolean {
        val lc = iSubString.lowercase()
        return amount.toString().lowercase().contains(lc) ||
                CategoryViewModel.getFullCategoryName(category).lowercase().contains(lc) ||
                SpenderViewModel.getSpenderName(paidby).lowercase().contains(lc) ||
                SpenderViewModel.getSpenderName(boughtfor).lowercase().contains(lc) ||
                note.lowercase().contains(lc) ||
                date.contains(lc) ||
                type.lowercase().contains(lc)
    }
    fun getSplit2(): Int {
        return 100 - bfname1split
    }
}

data class ExpenditureOut(
    var date: String = "", var amount: Int = 0, var category: Int = 0,
    var note: String = "", var paidby: Int = -1, var boughtfor: Int = -1,
    var bfname1split: Int = 0, var type: String = "Expense"
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    // doesn't have a key, because we don't want to store the key at Firebase, it'll generate one for us.
    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "note" -> note = value.trim()
            "type" -> type = value.trim()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
        }
    }
}

data class TransferOut(
    var date: String = "", var amount: Int = 0,
    var paidby: Int = -1, var boughtfor: Int = -1,
    var bfname1split: Int = 0,
    var category: String = "Transfer", var note: String = "",
    var type: String = "Transfer"
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    // doesn't have a key, because we don't want to store the key at Firebase, it'll generate one for us.
    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
        }
    }
}

class ExpenditureViewModel : ViewModel() {
    private var expListener: ValueEventListener? = null
    private val expenditures: MutableList<Expenditure> = mutableListOf()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: ExpenditureViewModel // used to track static single instance of self

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun expenditureExistsUsingCategory(iCategoryID: Int): Int {
            var ctr = 0
            singleInstance.expenditures.forEach {
                if (it.category == iCategoryID) {
                    ctr++
                }
            }
            return ctr
        }
        private fun getExpenditures(): MutableList<Expenditure> {
            return singleInstance.expenditures
        }

        fun getExpenditure(i:Int): Expenditure {
            return singleInstance.expenditures[i]
        }
        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.expenditures.size
            else
                0
        }

        fun getCopyOfExpenditures(): MutableList<Expenditure> {
            val copy = mutableListOf<Expenditure>()
            copy.addAll(getExpenditures())
            return copy
        }
        fun getTotalDiscretionaryActualsToDate(iBudgetMonth: BudgetMonth) : Double {
            var tmpTotal = 0.0
            val startDate: String
            val endDate: String
            if (iBudgetMonth.month < 10) {
                startDate = iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-00"
                endDate = iBudgetMonth.year.toString() +  "-0" + iBudgetMonth.month.toString() + "-99"
            } else {
                startDate = iBudgetMonth.year.toString() +  "-" + iBudgetMonth.month.toString() + "-00"
                endDate = iBudgetMonth.year.toString() +  "-" + iBudgetMonth.month.toString() + "-99"
            }

            singleInstance.expenditures.forEach {
                val expDiscIndicator = CategoryViewModel.getCategory(it.category)?.discType
                if (expDiscIndicator == cDiscTypeDiscretionary && it.date > startDate && it.date < endDate) {
                    tmpTotal += (it.amount / 100.0)
                }
            }
            return tmpTotal
        }

        fun getCategoryActuals(iBudgetMonth: BudgetMonth) : ArrayList<DataObject> {
            val tList: ArrayList<DataObject> = ArrayList()
            var prevCategory = ""
            var totalActuals = 0.0
            CategoryViewModel.getCategories().forEach {
                if (prevCategory != "" && prevCategory != it.categoryName) {
                    // ie not the first row, and this was a change in category
                    Log.d("Alex", "change from " + prevCategory + " to " + it.categoryName)
                    tList.add(DataObject(prevCategory, totalActuals, 0))
                    totalActuals = 0.0
                }
                var actual: Double
                for (i in 0 until SpenderViewModel.getTotalCount()) {
                    actual = getActualsForPeriod(it.id,
                        iBudgetMonth, iBudgetMonth,
                        i
                    )
                    totalActuals += actual
                }
                prevCategory = it.categoryName
            }
            return  tList
        }

        fun getActualsForPeriod(iCategoryID: Int, iStartPeriod: BudgetMonth, iEndPeriod: BudgetMonth, iWho: Int, iSubWho: Int = -1): Double {
            var tTotal = 0.0
            val firstDay = "$iStartPeriod-01"
            val  lastDay = "$iEndPeriod-31"
            loop@ for (expenditure in singleInstance.expenditures) {
                if (expenditure.type != "Transfer" &&
                        expenditure.date >= firstDay &&
                        expenditure.date <= lastDay &&
                        expenditure.category == iCategoryID &&
                        (expenditure.boughtfor == iWho)) { // || iWho == "Joint")) {
                    // this is a transaction to add to our subtotal
//                        if (iWho != "" && iWho != "Joint") // ie want a specific person
//                            tTotal += ((expenditure.amount.toDouble() * SpenderViewModel.getSpenderSplit(iWho))/ 100)
//                        else
                    tTotal += if (iWho == 2 && iSubWho != -1) {
                        if (iSubWho == 0)
                            (expenditure.amount.toDouble() / 100 * expenditure.bfname1split /100)
                        else
                            (expenditure.amount.toDouble() / 100 * expenditure.getSplit2() /100)
                    } else {
                        (expenditure.amount.toDouble() / 100)
                    }
                }
            }
            return tTotal
        }

        fun addTransaction(iExpenditure: ExpenditureOut, iLocalOnly: Boolean = false) {
            if (iLocalOnly) {
                singleInstance.expenditures.add(Expenditure(iExpenditure.date, iExpenditure.amount, iExpenditure.category,
                    iExpenditure.note, iExpenditure.paidby, iExpenditure.boughtfor, iExpenditure.type, iExpenditure.bfname1split))
            } else {
                val bm = BudgetMonth(iExpenditure.date)
                val key: String = if (iExpenditure.type == "Recurring")
                    iExpenditure.note + iExpenditure.date + "R"
                else
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions" +
                    "/" + bm.year.toString() + "/" + bm.get2DigitMonth())
                        .push().key.toString()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                    .child(bm.year.toString())
                    .child(bm.get2DigitMonth())
                    .child(key)
                    .setValue(iExpenditure)
            }
            singleInstance.expenditures.sortWith(compareBy({ it.date }, { it.note }))
        }

        fun addTransaction(iTransfer: TransferOut, iLocalOnly: Boolean = false) {
            if (iLocalOnly) {
                singleInstance.expenditures.add(Expenditure(iTransfer.date,
                    iTransfer.amount, cTRANSFER_CODE, "", iTransfer.paidby,
                    iTransfer.boughtfor, iTransfer.type, iTransfer.bfname1split,
                    "Transfer"))
            } else {
                val bm = BudgetMonth(iTransfer.date)
                val key = MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions" +
                        "/" + bm.year.toString() + "/" + bm.get2DigitMonth())
                        .push().key.toString()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                    .child(bm.year.toString())
                    .child(bm.get2DigitMonth())
                    .child(key)
                    .setValue(iTransfer)
            }
            singleInstance.expenditures.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun refresh() {
            singleInstance.loadExpenditures()
        }
        fun clear() {
            if (singleInstance.expListener != null) {
                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Transactions")
                    .removeEventListener(singleInstance.expListener!!)
                singleInstance.expListener = null
            }
            singleInstance.expenditures.clear()
            singleInstance.loaded = false
        }

        fun getPreviousKey(iKey: String): String {
            val exp = singleInstance.expenditures.find { it.mykey == iKey }
            var ind = singleInstance.expenditures.indexOf(exp)
            Log.d("Alex", "iKey is " + iKey + " and ind is " + ind + " and exp is " + exp.toString())
            if (ind == 0)
                ind = singleInstance.expenditures.size
            Log.d("Alex", "ind is " + ind + " and prev is " + (ind-1))
            return singleInstance.expenditures[ind-1].mykey
        }
        fun getNextKey(iKey: String): String {
            val exp = singleInstance.expenditures.find { it.mykey == iKey }
            var ind = singleInstance.expenditures.indexOf(exp)
            Log.d("Alex", "iKey is " + iKey + " and ind is " + ind + " and exp is " + exp.toString())
            if (ind == singleInstance.expenditures.size-1)
                ind = -1
            Log.d("Alex", "ind is " + ind + " and next is " + (ind+1))
            return singleInstance.expenditures[ind+1].mykey
        }
        fun getPreviousTransferKey(iKey: String): String {
            val exp = singleInstance.expenditures.find { it.mykey == iKey }
            val ind = singleInstance.expenditures.indexOf(exp)
            for (i in ind-1 downTo 0)
                if (singleInstance.expenditures[i].type == "Transfer")
                    return singleInstance.expenditures[i].mykey
            // if we get here, we didn't find a transfer in the rest of the expenditure list, so start at the beginning
            for (i in singleInstance.expenditures.size-1 downTo ind)
                if (singleInstance.expenditures[i].type == "Transfer")
                    return singleInstance.expenditures[i].mykey
            return iKey
        }
        fun getNextTransferKey(iKey: String): String {
            val exp = singleInstance.expenditures.find { it.mykey == iKey }
            val ind = singleInstance.expenditures.indexOf(exp)
            for (i in ind+1 until singleInstance.expenditures.size)
                if (singleInstance.expenditures[i].type == "Transfer")
                    return singleInstance.expenditures[i].mykey
            // if we get here, we didn't find a transfer in the rest of the expenditure list, so start at the beginning
            for (i in 0 until singleInstance.expenditures.size)
                if (singleInstance.expenditures[i].type == "Transfer")
                    return singleInstance.expenditures[i].mykey
            return iKey
        }

        fun deleteTransaction(date: String, iTransactionID: String) {
            val bm = BudgetMonth(date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID).removeValue()
            val expe =
                getExpenditure(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = singleInstance.expenditures.indexOf(expe)
            singleInstance.expenditures.removeAt(ind)
        }

        fun getExpenditure(iTransactionID: String): Expenditure? {
            return singleInstance.expenditures.find { it.mykey == iTransactionID }
        }

        fun updateTransaction(iTransactionID: String, iExpenditure: ExpenditureOut) {
            val bm = BudgetMonth(iExpenditure.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID)
                .setValue(iExpenditure)
            val expe =
                getExpenditure(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (expe != null) {
                expe.date = iExpenditure.date
                expe.note = iExpenditure.note
                expe.category = iExpenditure.category
                expe.amount = iExpenditure.amount
                expe.paidby = iExpenditure.paidby
                expe.boughtfor = iExpenditure.boughtfor
                expe.bfname1split = iExpenditure.bfname1split
                expe.type = iExpenditure.type
            }
            singleInstance.expenditures.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun updateTransaction(iTransactionID: String, iTransfer: TransferOut) {
            val bm = BudgetMonth(iTransfer.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID)
                .setValue(iTransfer)
            val expe =
                getExpenditure(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (expe != null) {
                expe.date = iTransfer.date
                expe.amount = iTransfer.amount
                expe.paidby = iTransfer.paidby
                expe.boughtfor = iTransfer.boughtfor
                expe.bfname1split = iTransfer.bfname1split
            }
            singleInstance.expenditures.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun getEarliestYear() : Int {
            // since we know that this table is sorted on date, we simply return the first date
            return if (singleInstance.expenditures.size == 0)
                0
            else singleInstance.expenditures[0].date.substring(0,4).toInt()
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (expListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Transactions")
                .removeEventListener(expListener!!)
            expListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadExpenditures() {
        // Do an asynchronous operation to fetch expenditures
        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Transactions")
        expListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                expenditures.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    for (year in element.children) {
                        for (month in year.children) {
                            val tExp = Expenditure()
                            tExp.setValue("key", month.key.toString())
                            for (child in month.children) {
                                tExp.setValue(child.key.toString(), child.value.toString())
                            }
                            expenditures.add(tExp)
                        }
                    }
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
                expenditures.sortWith(compareBy({ it.date }, { it.note }))
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast("User authorization failed 108.")
            }
        }
        expDBRef.addValueEventListener(expListener as ValueEventListener)
    }
}
