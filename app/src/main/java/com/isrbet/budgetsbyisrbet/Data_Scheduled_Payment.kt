@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.util.Log
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
    var lastDate: String = "",
    var category: Int = 0,
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var split1: Int = 100,
    var activeLoan: Boolean = false,
    var loanFirstPaymentDate: String = "",
    var loanAmount: Double = 0.0,
    var loanAmortization: Double = 0.0,
    var loanInterestRate: Double = 0.0,
    var actualPayment: Double = 0.0,
    var loanPaymentRegularity: LoanPaymentRegularity = LoanPaymentRegularity.BIWEEKLY
    ) {
    fun setValue(key: String, value: String) {
        when (key) {
            "name" -> name = value.trim()
            "amount" -> amount = value.toDouble()
            "nextdate" -> nextdate = value.trim()
            "lastDate" -> lastDate = value.trim()
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
            "actualPayment" -> actualPayment = value.toDouble()
            "loanPaymentRegularity" ->
//                loanPaymentRegularity = if (isNumber(value))
                    loanPaymentRegularity = if (value.toIntOrNull() != null)
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
    fun getOutstandingLoanAmount(iDate: Calendar, iOwnershipPercentage: Double = 100.0): Int {
        if (activeLoan) {
            val paymentsPerYear = when (loanPaymentRegularity) {
                LoanPaymentRegularity.WEEKLY -> 52
                LoanPaymentRegularity.BIWEEKLY -> 26
                LoanPaymentRegularity.MONTHLY -> 12
            }
            var principalOwing = loanAmount
            val paymentDate = gCurrentDate.clone() as Calendar // Calendar.getInstance()
            paymentDate.set(loanFirstPaymentDate.substring(0,4).toInt(), loanFirstPaymentDate.substring(5,7).toInt()-1, loanFirstPaymentDate.substring(8,10).toInt())
            do {
                val interestInThisPeriod = principalOwing * (loanInterestRate / 100.0 / paymentsPerYear)
                principalOwing -= (amount - interestInThisPeriod)
                when (loanPaymentRegularity) {
                    LoanPaymentRegularity.WEEKLY -> paymentDate.add(Calendar.DAY_OF_MONTH, 7)
                    LoanPaymentRegularity.BIWEEKLY -> paymentDate.add(Calendar.DAY_OF_MONTH, 14)
                    LoanPaymentRegularity.MONTHLY -> paymentDate.add(Calendar.MONTH, 1)
                }
            } while (principalOwing > 0.0 && paymentDate < iDate)
            if (principalOwing > 0.0)
                return round(principalOwing * iOwnershipPercentage / 100.0).toInt()
        }
    return 0
    }
}

data class ScheduledPaymentOut(
    var name: String = "",
    var amount: Int = 0,
    var period: String = "",
    var regularity: Int = 1,
    var nextdate: String = "",
    var lastDate: String = "",
    var category: Int = 0,
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var split1: Int = 100,
    var activeLoan: Boolean = false,
    var loanFirstPaymentDate: String = "",
    var loanAmount: Int = 0,
    var loanAmortization: Int = 0,
    var loanInterestRate: Int = 0,
    var actualPayment: Int = 0,
    var loanPaymentRegularity: LoanPaymentRegularity = LoanPaymentRegularity.BIWEEKLY
) {
    constructor(sp: ScheduledPayment) : this(
        sp.name, round(sp.amount*100).toInt(), sp.period, sp.regularity,
        sp.nextdate, sp.lastDate, sp.category, sp.paidby, sp.boughtfor, sp.split1,
        sp.activeLoan, sp.loanFirstPaymentDate, round(sp.loanAmount*100).toInt(),
        round(sp.loanAmortization*100).toInt(),round(sp.loanInterestRate*100).toInt(),
        round(sp.actualPayment*100).toInt(),
        sp.loanPaymentRegularity)
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
            val spOut = ScheduledPaymentOut(iScheduledPayment)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iScheduledPayment.name).setValue(spOut)
        }
        fun updateScheduledPayment(iName: String, iAmount: Double, iPeriod: String, iNextDate: String, iRegularity: Int,
                                       iCategoryID: Int, iPaidBy: Int, iBoughtFor: Int,
                                        iSplit1: Int, iActiveLoan: Boolean, iLoanStartDate: String,
                                        iLoanAmount: Double, iLoanAmortization: Double, iLoanInterestRate: Double,
                                        iActualPayment: Double,
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
                mySP.actualPayment = iActualPayment
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
                .setValue(round(iValue * 100).toInt())
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
            Log.d("Alex", "generateScheduledPayments")
            val dateNow = giveMeMyDateFormat(gCurrentDate)
            singleInstance.scheduledPayments.forEach {
                Log.d("Alex", "comparing ${it.name} nextdate ${it.nextdate} $dateNow")
                while (it.nextdate <= dateNow) {
                    val newNextDate = gCurrentDate.clone() as Calendar // Calendar.getInstance()
                    it.lastDate = it.nextdate
                    Log.d("Alex", "in while setting lastdate to ${it.lastDate}")
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
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                        .child(it.name).child("lastDate").setValue(it.lastDate)
                    // add transaction
                    val nextDate = getNextBusinessDate(it.nextdate)
                    val nextDayIsBusinessDay = it.nextdate == nextDate
                    it.nextdate = giveMeMyDateFormat(newNextDate)
                    TransactionViewModel.addTransactionDatabase(TransactionOut(nextDate,
                        round(it.amount*100).toInt(),
                        it.category, it.name, "", it.paidby, it.boughtfor,
                        it.split1, cTRANSACTION_TYPE_SCHEDULED))
                    val outstandingLoanAmount = if (it.activeLoan) it.getOutstandingLoanAmount(
                        gCurrentDate) else 0
                    if (nextDayIsBusinessDay)
                        Toast.makeText(mainActivity, MyApplication.getString(R.string.scheduled_payment_was_added_for) +
                            " ${it.name} ${gDecWithCurrency(it.amount)} " +
                                CategoryViewModel.getFullCategoryName(it.category) +
                                " $nextDate" +
                            if (outstandingLoanAmount > 0) " Outstanding loan amount ${
                                gDecWithCurrency(outstandingLoanAmount)}" else "",
                            Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(mainActivity, MyApplication.getString(R.string.scheduled_payment_was_added_for) +
                            " ${it.name} ${gDecWithCurrency(it.amount)} " +
                                CategoryViewModel.getFullCategoryName(it.category) + " " +
                            MyApplication.getString(R.string.on_next_business_day) + nextDate +
                            if (outstandingLoanAmount > 0) " Outstanding loan amount ${
                                gDecWithCurrency(outstandingLoanAmount)}" else "",
                            Toast.LENGTH_SHORT).show()
                    Log.d("Alex", "last date now ${it.lastDate} and next date now ${it.nextdate}")
                }
            }
        }
        fun getScheduledPaymentsInNextDays(iDays: Int) : String {
            val today = MyDate(gCurrentDate).toString()
            val tDate = MyDate(gCurrentDate)
            tDate.increment(cPeriodDay, iDays)
            var tReply = ""

            singleInstance.scheduledPayments.forEach { sp ->
                Log.d("Alex", "Comparing ${sp.name} ${sp.lastDate} ${today} ${sp.lastDate == today} ${sp.nextdate} ${tDate.toString()} ${sp.nextdate <= tDate.toString()}")
                if (sp.lastDate == today) {
                    tReply = if (tReply == "") {
                        "\$${gDecM(sp.amount)} for ${sp.name} due today."
                    } else {
                        "\$${gDecM(sp.amount)} for ${sp.name} due today.\n$tReply"
                    }
                } else if (sp.nextdate <= tDate.toString()) {
                    val myNextDate = MyDate(sp.nextdate)
                    tReply = if (tReply == "") {
                        "\$${gDecM(sp.amount)} for ${sp.name} due ${gShortMonthName(myNextDate.month)} ${myNextDate.day}."
                    } else
                        "$tReply\n\$${gDecM(sp.amount)} for ${sp.name} due ${gShortMonthName(myNextDate.month)} ${myNextDate.day}."
                }
            }
            return tReply
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
        // Do an asynchronous operation to fetch scheduled payments pka recurring transactions
        scheduledPaymentListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                scheduledPayments.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tScheduledPayment = ScheduledPayment()
                    tScheduledPayment.setValue("name", element.key.toString())
                    for (child in element.children) {
                        when (child.key.toString()) {
                            "amount" -> {
                                tScheduledPayment.setValue(
                                    child.key.toString(),
                                    (child.value.toString().toInt() / 100.0).toString()
                                )
                            }
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
                            "actualPayment" -> tScheduledPayment.setValue(
                                child.key.toString(),
                                (child.value.toString().toInt() / 100.0).toString()
                            )
                            else -> {
                                tScheduledPayment.setValue(
                                    child.key.toString(),
                                    child.value.toString()
                                )
                            }
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