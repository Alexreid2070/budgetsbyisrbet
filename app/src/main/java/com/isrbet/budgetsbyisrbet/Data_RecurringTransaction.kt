package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class RecurringTransaction(
    var name: String = "",
    var amount: Int = 0,
    var period: String = "",
    var regularity: Int = 1,
    var nextdate: String = "",
    var category: Int = 0,
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var split1: Int = 100
) {
    fun setValue(key: String, value: String) {
        when (key) {
            "name" -> name = value.trim()
            "amount" -> amount = value.toInt()
            "nextdate" -> nextdate = value.trim()
            "period" -> period = value.trim()
            "regularity" -> regularity = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "split1" -> split1 = value.toInt()
        }
    }
    fun getSplit2(): Int {
        return 100 - split1
    }
}

class RecurringTransactionViewModel : ViewModel() {
    private var recurringTransactionListener: ValueEventListener? = null
    private val recurringTransactions: MutableList<RecurringTransaction> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: RecurringTransactionViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.recurringTransactions.size
            else
                0
        }

        fun nameExists(iName: String): Boolean {
            val rt = singleInstance.recurringTransactions.find { it.name == iName }
            return (rt != null)
        }

        fun recurringTransactionExistsUsingCategory(iCategoryID: Int): Int {
            var ctr = 0
            singleInstance.recurringTransactions.forEach {
                if (it.category == iCategoryID) {
                    ctr++
                }
            }
            return ctr
        }
        fun showMe() {
            singleInstance.recurringTransactions.forEach {
                Log.d("Alex", "SM Recurring Transaction is " + it.name + " amount " + it.amount + " regularity " + it.regularity + " period " + it.period + " lg " + it.nextdate)
            }
        }

        private fun getRecurringTransactions(): MutableList<RecurringTransaction> {
            return singleInstance.recurringTransactions
        }
        fun getCopyOfRecurringTransactions(): MutableList<RecurringTransaction> {
            val copy = mutableListOf<RecurringTransaction>()
            copy.addAll(getRecurringTransactions())
            return copy
        }
        fun deleteRecurringTransactionFromFirebase(iTransactionID: String) {
            // this block below ensures that the viewAll view is updated immediately
            val rt: RecurringTransaction? = singleInstance.recurringTransactions.find { it.name == iTransactionID }
            val ind = singleInstance.recurringTransactions.indexOf(rt)
            singleInstance.recurringTransactions.removeAt(ind)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").child(iTransactionID).removeValue()
        }

        fun addRecurringTransaction(iRecurringTransaction: RecurringTransaction) {
            // I need to add the new RT to the internal list so that the Adapter can be updated immediately, rather than waiting for the firebase sync.
            // also, if I don't add locally right away, the app crashes because of a sync issue
            singleInstance.recurringTransactions.add(iRecurringTransaction)
            singleInstance.recurringTransactions.sortWith(compareBy { it.name })
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").child(iRecurringTransaction.name).setValue(iRecurringTransaction)
        }
        fun updateRecurringTransaction(iName: String, iAmount: Int, iPeriod: String, iNextDate: String, iRegularity: Int,
                                       iCategoryID: Int, iPaidBy: Int, iBoughtFor: Int,
                                        iSplit1: Int) {
            val myRT = singleInstance.recurringTransactions.find{ it.name == iName }
            if (myRT != null) {
                myRT.amount = iAmount
                myRT.period = iPeriod
                myRT.regularity = iRegularity
                myRT.nextdate = iNextDate
                myRT.category = iCategoryID
                myRT.paidby = iPaidBy
                myRT.boughtfor = iBoughtFor
                myRT.split1 = iSplit1
            }
        }
        fun updateRecurringTransactionStringField(iName: String, iField: String, iValue: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iName)
                .child(iField)
                .setValue(iValue)
        }
        fun updateRecurringTransactionIntField(iName: String, iField: String, iValue: Int) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iName)
                .child(iField)
                .setValue(iValue)
        }
        fun refresh() {
            singleInstance.loadRecurringTransactions()
        }
        fun clear() {
            if (singleInstance.recurringTransactionListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/RecurringTransactions")
                    .removeEventListener(singleInstance.recurringTransactionListener!!)
                singleInstance.recurringTransactionListener = null
            }
            singleInstance.recurringTransactions.clear()
            singleInstance.loaded = false
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (recurringTransactionListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/RecurringTransactions")
                .removeEventListener(recurringTransactionListener!!)
            recurringTransactionListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadRecurringTransactions(mainActivity: MainActivity? = null) {
        // Do an asynchronous operation to fetch recurring transactions
        recurringTransactionListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                recurringTransactions.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tRecurringTransaction = RecurringTransaction()
                    tRecurringTransaction.setValue("name", element.key.toString())
                    for (child in element.children) {
                        tRecurringTransaction.setValue(child.key.toString(), child.value.toString())
                    }
                    recurringTransactions.add(tRecurringTransaction)
                }
                // now that recurring transaction settings are loaded, we need to review them to determine if any Expenditures are needed
                val dateNow = giveMeMyDateFormat(Calendar.getInstance())
                recurringTransactions.forEach {
                    if (it.nextdate <= dateNow) {
                        val newNextDate = Calendar.getInstance()
                        newNextDate.set(it.nextdate.substring(0,4).toInt(), it.nextdate.substring(5,7).toInt()-1, it.nextdate.substring(8,10).toInt())
                        // Reset nextDate
                        when (it.period) {
                            cPeriodWeek -> {
                                newNextDate.add(Calendar.WEEK_OF_YEAR, it.regularity)
                            }
                            cPeriodMonth -> {
                                newNextDate.add(Calendar.MONTH, it.regularity)
                            }
                            cPeriodQuarter -> {
                                newNextDate.add(Calendar.MONTH, it.regularity*3)
                            }
                            cPeriodYear -> {
                                newNextDate.add(Calendar.YEAR, it.regularity)
                            }
                        }
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").child(it.name).child("nextdate").setValue(giveMeMyDateFormat(newNextDate))
                        // add transaction
                        Log.d("Alex", "Adding a transaction")
                        val nextDate = getNextBusinessDate(it.nextdate)
                        ExpenditureViewModel.addTransaction(ExpenditureOut(nextDate, it.amount,
                            it.category, it.name, it.paidby, it.boughtfor,
                            it.split1, "Recurring"))
                        if (mainActivity != null)
                            Toast.makeText(mainActivity, "Recurring transaction was added for : $nextDate " + CategoryViewModel.getFullCategoryName(it.category) + " " + it.name, Toast.LENGTH_SHORT).show()
                    }
                }
                sortYourself()
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast("User authorization failed 109.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").addValueEventListener(
            recurringTransactionListener as ValueEventListener
        )
    }

    fun sortYourself() {
        recurringTransactions.sortBy { it.nextdate }
    }
}