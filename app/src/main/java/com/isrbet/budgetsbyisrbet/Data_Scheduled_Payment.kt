@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList
import kotlin.math.round

data class ScheduledPayment(
    var name: String = "",
    var amount: Double = 0.0,
    var period: String = "",
    var regularity: Int = 1,
    var nextdate: String = "",
    var category: Int = 0,
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var split1: Int = 100,
    var activeLoan: Boolean = false,
    var loanFirstPaymentDate: String = "",
    var loanAmount: Double = 0.0,
    var loanAmortization: Double = 0.0,
    var loanInterestRate: Double = 0.0,
    var loanPaymentRegularity: LoanPaymentRegularity = LoanPaymentRegularity.BIWEEKLY
    ) {
    fun setValue(key: String, value: String) {
        when (key) {
            "name" -> name = value.trim()
            "amount" -> amount = value.toDouble()
            "nextdate" -> nextdate = value.trim()
            "period" -> period = value.trim()
            "regularity" -> regularity = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "split1" -> split1 = value.toInt()
            "activeLoan" -> activeLoan = (value.trim() == cTRUE)
            "loanFirstPaymentDate" -> loanFirstPaymentDate = value.trim()
            "loanAmount" -> loanAmount = value.toDouble()
            "loanAmortization" -> loanAmortization = value.toDouble()
            "loanInterestRate" -> loanInterestRate = value.toDouble()
            "loanPaymentRegularity" ->
                loanPaymentRegularity = if (isNumber(value))
                    LoanPaymentRegularity.getByValue(value.toInt())!!
                else {
                    try {
                        LoanPaymentRegularity.valueOf(value)
                    } catch (e: IllegalArgumentException) {
                        LoanPaymentRegularity.BIWEEKLY
                    }
                }
        }
    }
    fun getSplit2(): Int {
        return 100 - split1
    }
}

class ScheduledPaymentViewModel : ViewModel() {
    private var scheduledPaymentListener: ValueEventListener? = null
    private val scheduledPayments: MutableList<ScheduledPayment> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: ScheduledPaymentViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.scheduledPayments.size
            else
                0
        }

        fun nameExists(iName: String): Boolean {
            val sp = singleInstance.scheduledPayments.find { it.name == iName }
            return (sp != null)
        }

        fun getScheduledPayment(iName: String): ScheduledPayment? {
            return singleInstance.scheduledPayments.find { it.name == iName }
        }
        fun scheduledPaymentExistsUsingCategory(iCategoryID: Int): Int {
            var ctr = 0
            singleInstance.scheduledPayments.forEach {
                if (it.category == iCategoryID) {
                    ctr++
                }
            }
            return ctr
        }
/*        fun showMe() {
            singleInstance.scheduledPayments.forEach {
                Log.d("Alex", "SM Scheduled Payment is " + it.name + " amount " + it.amount + " regularity " + it.regularity + " period " + it.period + " lg " + it.nextdate)
            }
        } */

        private fun getScheduledPayments(): MutableList<ScheduledPayment> {
            return singleInstance.scheduledPayments
        }
        fun getCopyOfScheduledPayments(): MutableList<ScheduledPayment> {
            val copy = mutableListOf<ScheduledPayment>()
            copy.addAll(getScheduledPayments())
            singleInstance.scheduledPayments.sortWith(compareBy { it.nextdate })
            return copy
        }

        fun getActiveLoanSPs(): MutableList<String> {
            val myList = mutableListOf<String>()
            singleInstance.scheduledPayments.forEach {
                if (it.activeLoan) {
                    myList.add(it.name)
                }
            }
            return myList
        }
        fun deleteScheduledPaymentFromFirebase(iTransactionID: String) {
            // this block below ensures that the viewAll view is updated immediately
            val sp: ScheduledPayment? = singleInstance.scheduledPayments.find { it.name == iTransactionID }
            val ind = singleInstance.scheduledPayments.indexOf(sp)
            singleInstance.scheduledPayments.removeAt(ind)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iTransactionID).removeValue()
        }

        fun addScheduledPayment(iScheduledPayment: ScheduledPayment) {
            // I need to add the new SP to the internal list so that the Adapter can be updated immediately, rather than waiting for the firebase sync.
            // also, if I don't add locally right away, the app crashes because of a sync issue
            singleInstance.scheduledPayments.add(iScheduledPayment)
            singleInstance.scheduledPayments.sortWith(compareBy { it.name })
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name).setValue(iScheduledPayment)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name)
                .child("amount")
                .setValue(round(iScheduledPayment.amount * 100))
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name)
                .child("loanAmount")
                .setValue(round(iScheduledPayment.loanAmount * 100))
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name)
                .child("loanAmortization")
                .setValue(round(iScheduledPayment.loanAmortization * 100))
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name)
                .child("loanInterestRate")
                .setValue(round(iScheduledPayment.loanInterestRate * 100))
        }
        fun updateScheduledPayment(iName: String, iAmount: Double, iPeriod: String, iNextDate: String, iRegularity: Int,
                                       iCategoryID: Int, iPaidBy: Int, iBoughtFor: Int,
                                        iSplit1: Int, iActiveLoan: Boolean, iLoanStartDate: String,
                                        iLoanAmount: Double, iLoanAmortization: Double, iLoanInterestRate: Double,
                                        iLoanPaymentRegularity: LoanPaymentRegularity) {
            val mySP = singleInstance.scheduledPayments.find{ it.name == iName }
            if (mySP != null) {
                mySP.amount = iAmount
                mySP.period = iPeriod
                mySP.regularity = iRegularity
                mySP.nextdate = iNextDate
                mySP.category = iCategoryID
                mySP.paidby = iPaidBy
                mySP.boughtfor = iBoughtFor
                mySP.split1 = iSplit1
                mySP.activeLoan = iActiveLoan
                mySP.loanFirstPaymentDate = iLoanStartDate
                mySP.loanAmount = iLoanAmount
                mySP.loanAmortization = iLoanAmortization
                mySP.loanInterestRate = iLoanInterestRate
                mySP.loanPaymentRegularity = iLoanPaymentRegularity
            }
        }
        fun updateScheduledPaymentStringField(iName: String, iField: String, iValue: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iName)
                .child(iField)
                .setValue(iValue)
        }
        fun updateScheduledPaymentIntField(iName: String, iField: String, iValue: Int) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iName)
                .child(iField)
                .setValue(iValue)
        }
        fun updateScheduledPaymentDoubleField(iName: String, iField: String, iValue: Double) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iName)
                .child(iField)
                .setValue(round(iValue * 100))
        }
        fun refresh() {
            singleInstance.loadScheduledPayments()
        }
        fun clear() {
            if (singleInstance.scheduledPaymentListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/RecurringTransactions")
                    .removeEventListener(singleInstance.scheduledPaymentListener!!)
                singleInstance.scheduledPaymentListener = null
            }
            singleInstance.scheduledPayments.clear()
            singleInstance.loaded = false
        }
        fun generateScheduledPayments(mainActivity: MainActivity) {
            // now that recurring transaction settings are loaded, we need to review them to determine if any Transactions are needed
            val dateNow = giveMeMyDateFormat(Calendar.getInstance())
            singleInstance.scheduledPayments.forEach {
                while (it.nextdate <= dateNow) {
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
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                        .child(it.name).child("nextdate").setValue(giveMeMyDateFormat(newNextDate))
                    // add transaction
                    val nextDate = getNextBusinessDate(it.nextdate)
                    val nextDayIsBusinessDay = it.nextdate == nextDate
                    it.nextdate = giveMeMyDateFormat(newNextDate)
                    TransactionViewModel.addTransaction(TransactionOut(nextDate,
                        round(it.amount*100).toInt(),
                        it.category, it.name, "", it.paidby, it.boughtfor,
                        it.split1, cTRANSACTION_TYPE_SCHEDULED))
                    if (nextDayIsBusinessDay)
                        Toast.makeText(mainActivity, MyApplication.getString(R.string.scheduled_payment_was_added_for) +
                            " ${it.name} ${gDecWithCurrency(it.amount)} " + CategoryViewModel.getFullCategoryName(it.category) + " $nextDate", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(mainActivity, MyApplication.getString(R.string.scheduled_payment_was_added_for) +
                            " ${it.name} ${gDecWithCurrency(it.amount)} " + CategoryViewModel.getFullCategoryName(it.category) + " " +
                            MyApplication.getString(R.string.on_next_business_day) + nextDate, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (scheduledPaymentListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/RecurringTransactions")
                .removeEventListener(scheduledPaymentListener!!)
            scheduledPaymentListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadScheduledPayments() {
        // Do an asynchronous operation to fetch schduled payments pka recurring transactions
        scheduledPaymentListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                scheduledPayments.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tScheduledPayment = ScheduledPayment()
                    tScheduledPayment.setValue("name", element.key.toString())
                    for (child in element.children) {
                        when (child.key.toString()) {
                            "amount" -> tScheduledPayment.setValue(
                                child.key.toString(),
                                (child.value.toString().toInt() / 100.0).toString()
                            )
                            "loanAmount" -> tScheduledPayment.setValue(
                                child.key.toString(),
                                (child.value.toString().toInt() / 100.0).toString()
                            )
                            "loanAmortization" -> tScheduledPayment.setValue(
                                child.key.toString(),
                                (child.value.toString().toInt() / 100.0).toString()
                            )
                            "loanInterestRate" -> tScheduledPayment.setValue(
                                child.key.toString(),
                                (child.value.toString().toInt() / 100.0).toString()
                            )
                            else -> tScheduledPayment.setValue(
                                child.key.toString(),
                                child.value.toString()
                            )
                        }
                    }
                    scheduledPayments.add(tScheduledPayment)
                }
                sortYourself()
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 109.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions").addValueEventListener(
            scheduledPaymentListener as ValueEventListener
        )
    }

    fun sortYourself() {
        scheduledPayments.sortBy { it.nextdate }
    }
}