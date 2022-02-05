package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class Expenditure(
    var date: String = "",
    var amount: Int = 0,
    var category: String = "",
    var subcategory: String = "",
    var note: String = "",
    var paidby: String = "",
    var boughtfor: String = "",
    var type: String = "",
    var bfname1split: Int = 0,
    var bfname2split: Int = 0,
    var mykey: String = ""
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    fun setValue(key: String, value: String) {
        when (key) {
            "key" -> mykey = value.trim()
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "category" -> category = value.trim()
            "subcategory" -> subcategory = value.trim()
            "paidby" -> paidby = value.trim()
            "boughtfor" -> boughtfor = value.trim()
            "bfname1split" -> bfname1split = value.toInt()
            "bfname2split" -> bfname2split = value.toInt()
            "note" -> note = value.trim()
            "type" -> type = value.trim()
            "who" -> {if (paidby == "") paidby = value.trim(); if (boughtfor == "") boughtfor = value.trim() }
            else -> {
                Log.d("Alex", "Unknown field in Expenditures $key $value " + this.toString())
            }
        }
    }

    fun contains(iSubString: String): Boolean {
        val lc = iSubString.lowercase()
        if (amount.toString().lowercase().contains(lc) ||
            category.lowercase().contains(lc) ||
            subcategory.lowercase().contains(lc) ||
            paidby.lowercase().contains(lc) ||
            boughtfor.lowercase().contains(lc) ||
            note.lowercase().contains(lc) ||
            date.contains(lc)
        )
            return true
        else
            return false
    }
}

data class ExpenditureOut(
    var date: String = "", var amount: Int = 0, var category: String = "",
    var subcategory: String = "", var note: String = "", var paidby: String = "", var boughtfor: String = "",
    var bfname1split: Int = 0, var bfname2split: Int = 0, var type: String = ""
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    // doesn't have a key, because we don't want to store the key at Firebase, it'll generate one for us.
    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value
            "amount" -> amount = value.toInt()
            "category" -> category = value
            "subcategory" -> subcategory = value
            "paidby" -> paidby = value
            "boughtfor" -> boughtfor = value
            "bfname1split" -> bfname1split = value.toInt()
            "bfname2split" -> bfname2split = value.toInt()
            "note" -> note = value
            "type" -> type = value
            "who" -> {if (paidby == "") paidby = value.trim(); if (boughtfor == "") boughtfor = value.trim() }
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

        private fun getExpenditures(): MutableList<Expenditure> {
            return singleInstance.expenditures
        }

        fun getExpenditure(i:Int): Expenditure {
            return singleInstance.expenditures[i]
        }
        fun getCount(): Int {
            return singleInstance.expenditures.size
        }

        fun getCopyOfExpenditures(): MutableList<Expenditure> {
            val copy = mutableListOf<Expenditure>()
            copy.addAll(getExpenditures())
            return copy
        }
        fun getTotalDiscretionaryActualsToDate(iCal: Calendar) : Double {
            var tmpTotal = 0.0
            val startDate: String
            val endDate: String
            val month = iCal.get(Calendar.MONTH)+1
            if (month < 10) {
                startDate = iCal.get(Calendar.YEAR).toString() + "-0" + month.toString() + "-00"
                endDate = iCal.get(Calendar.YEAR).toString() + "-0" + month.toString() + "-99"
            } else {
                startDate = iCal.get(Calendar.YEAR).toString() + "-" + month.toString() + "-00"
                endDate = iCal.get(Calendar.YEAR).toString() + "-" + month.toString() + "-99"
            }
            Log.d("Alex", "checking $startDate to $endDate")

            singleInstance.expenditures.forEach {
                val expDiscIndicator = CategoryViewModel.getDiscretionaryIndicator(it.category, it.subcategory)
                if (expDiscIndicator == cDiscTypeDiscretionary && it.date > startDate && it.date < endDate) {
                    tmpTotal += (it.amount / 100.0)
                }
            }
            return tmpTotal
        }

        fun getActualsForPeriod(iCategory: String, iSubCategory: String, iStartPeriod: BudgetMonth, iEndPeriod: BudgetMonth, iWho: String): Double {
            var tTotal = 0.0
            val firstDay = "$iStartPeriod-01"
            val  lastDay = "$iEndPeriod-31"
            loop@ for (expenditure in singleInstance.expenditures) {
                if (expenditure.type != "T" &&
                        expenditure.date >= firstDay &&
                        expenditure.date <= lastDay &&
                        expenditure.category == iCategory &&
                        expenditure.subcategory == iSubCategory &&
                        (expenditure.boughtfor == iWho || iWho == "Joint")) {
                    // this is a transaction to add to our subtotal
//                        if (iWho != "" && iWho != "Joint") // ie want a specific person
//                            tTotal += ((expenditure.amount.toDouble() * SpenderViewModel.getSpenderSplit(iWho))/ 100)
//                        else
                            tTotal += (expenditure.amount.toDouble() / 100)
                    }
            }

            return tTotal
        }

        fun addTransaction(iExpenditure: ExpenditureOut) {
            val key: String
            if (iExpenditure.type == "R")
                key = iExpenditure.note + iExpenditure.date
            else
                key = MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Expenditures").push().key.toString()
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Expenditures").child(key)
                .setValue(iExpenditure)
        }

        fun refresh() {
            singleInstance.loadExpenditures()
        }
        fun clear() {
            if (singleInstance.expListener != null) {
                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Expenditures").orderByChild("date")
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

        fun deleteTransaction(iTransactionID: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Expenditures").child(iTransactionID).removeValue()
            val expe =
                getExpenditure(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = singleInstance.expenditures.indexOf(expe)
            singleInstance.expenditures.removeAt(ind)
        }

        fun getExpenditure(iTransactionID: String): Expenditure? {
            return singleInstance.expenditures.find { it.mykey == iTransactionID }
        }

        fun updateTransaction(iTransactionID: String, iExpenditure: ExpenditureOut) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Expenditures").child(iTransactionID)
                .setValue(iExpenditure)
            val expe =
                getExpenditure(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (expe != null) {
                expe.date = iExpenditure.date
                expe.note = iExpenditure.note
                expe.subcategory = iExpenditure.subcategory
                expe.category = iExpenditure.category
                expe.amount = iExpenditure.amount
                expe.paidby = iExpenditure.paidby
                expe.boughtfor = iExpenditure.boughtfor
                expe.bfname1split = iExpenditure.bfname1split
                expe.bfname2split = iExpenditure.bfname2split
                expe.type = iExpenditure.type
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (expListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Expenditures")
                .orderByChild("date")
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
        Log.d("Alex", "in loadExpenditures for expenditures " + if (dataUpdatedCallback == null) "no callback " else "callback exists" )
        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Expenditures").orderByChild("date")
        expListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                expenditures.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tExp = Expenditure()
                    tExp.setValue("key", element.key.toString())
                    for (child in element.children) {
                        tExp.setValue(child.key.toString(), child.value.toString())
                    }
                    expenditures.add(tExp)
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
            }
        }
        expDBRef.addValueEventListener(expListener as ValueEventListener)
    }
}
