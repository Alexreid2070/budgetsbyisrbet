package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
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
    var category: String = "",
    var subcategory: String = "",
    var paidby: String = "",
    var boughtfor: String = ""
) {
    fun setValue(key: String, value: String) {
        when (key) {
            "name" -> name = value.trim()
            "amount" -> amount = value.toInt()
            "nextdate" -> nextdate = value.trim()
            "period" -> period = value.trim()
            "regularity" -> regularity = value.toInt()
            "category" -> category = value.trim()
            "subcategory" -> subcategory = value.trim()
            "paidby" -> paidby = value.trim()
            "boughtfor" -> boughtfor = value.trim()
        }
    }
}


class RecurringTransactionViewModel : ViewModel() {
    lateinit var recurringTransactionListener: ValueEventListener
    private val recurringTransactions: MutableList<RecurringTransaction> = ArrayList()

    companion object {
        lateinit var singleInstance: RecurringTransactionViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.recurringTransactions.forEach {
                Log.d("Alex", "SM Recurring Transaction is " + it.name + " amount " + it.amount + " regularity " + it.regularity + " period " + it.period + " lg " + it.nextdate)
            }
        }

        fun getRecurringTransactions(): MutableList<RecurringTransaction> {
            return singleInstance.recurringTransactions
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
            RecurringTransactionViewModel.singleInstance.recurringTransactions.sortWith(compareBy({it.name}))
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").child(iRecurringTransaction.name).setValue(iRecurringTransaction)
        }
        fun updateRecurringTransaction(iName: String, iAmount: Int, iPeriod: String, iNextDate: String, iRegularity: Int, iCategory: String, iSubcategory: String, iPaidBy: String, iBoughtFor: String) {
            var myRT = RecurringTransactionViewModel.singleInstance.recurringTransactions.find{ it.name == iName }
            if (myRT != null) {
                myRT.amount = iAmount
                myRT.period = iPeriod
                myRT.regularity = iRegularity
                myRT.nextdate = iNextDate
                myRT.category = iCategory
                myRT.subcategory = iSubcategory
                myRT.paidby = iPaidBy
                myRT.boughtfor = iBoughtFor
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
    }

    init {
        RecurringTransactionViewModel.singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/RecurringTransactions")
            .removeEventListener(recurringTransactionListener)
    }

    fun loadRecurringTransactions(mainActivity: MainActivity) {
        // Do an asynchronous operation to fetch recurring transactions
        recurringTransactionListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                recurringTransactions.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    var tRecurringTransaction = RecurringTransaction()
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
                        var newNextDate = Calendar.getInstance()
                        newNextDate.set(it.nextdate.substring(0,4).toInt(), it.nextdate.substring(5,7).toInt()-1, it.nextdate.substring(8,10).toInt())
                        Log.d("Alex", "newNextDate is " + giveMeMyDateFormat(newNextDate))
                        // Reset nextDate
                        if (it.period == cPeriodWeek) {
                            newNextDate.add(Calendar.WEEK_OF_YEAR, it.regularity)
                        } else if (it.period == cPeriodMonth) {
                            newNextDate.add(Calendar.MONTH, it.regularity)
                        } else if (it.period == cPeriodQuarter) {
                            newNextDate.add(Calendar.MONTH, it.regularity*3)
                        } else if (it.period == cPeriodYear) {
                            newNextDate.add(Calendar.YEAR, it.regularity)
                        }
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").child(it.name).child("nextdate").setValue(giveMeMyDateFormat(newNextDate))
                        // add transaction
                        Log.d("Alex", "Adding a transaction")
                        ExpenditureViewModel.addTransaction(ExpenditureOut(it.nextdate, it.amount, it.category, it.subcategory, it.name, it.paidby, it.boughtfor, "R"))
                        Toast.makeText(mainActivity, "Recurring transaction was added: " + it.category + " " + it.subcategory + " " + it.name, Toast.LENGTH_SHORT).show()
                    }
                }
                sortYourself()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").addValueEventListener(recurringTransactionListener)
    }

    fun sortYourself() {
        recurringTransactions.sortBy { it.nextdate }
    }
}