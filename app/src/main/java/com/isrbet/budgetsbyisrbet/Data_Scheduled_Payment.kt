@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.util.ArrayList
import kotlin.math.round

data class ScheduledPayment(
    var vendor: String = "",
    var note: String = "",
    var amount: Double = 0.0,
    var period: String = "",
    var regularity: Int = 1,
    var nextdate: String = "",
    var lastDate: MyDate = MyDate(2000,1,1),
    var category: Int = 0,
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var split1: Int = 100,
    var activeLoan: Boolean = false,
    var loanFirstPaymentDate: MyDate = MyDate(),
    var loanAmount: Double = 0.0,
    var loanAmortization: Double = 0.0,
    var loanInterestRate: Double = 0.0,
    var actualPayment: Double = 0.0,
    var loanPaymentRegularity: LoanPaymentRegularity = LoanPaymentRegularity.BIWEEKLY,
    var expirationDate: String = "",
    var mykey: String = ""
    ) {
    fun setValue(key: String, value: String) {
        when (key) {
            "key" -> mykey = value.trim()
            "vendor" -> vendor = value.trim()
            "note" -> note = value.trim()
            "amount" -> amount = value.toDouble()
            "nextdate" -> nextdate = value.trim()
            "lastDate" -> lastDate = MyDate(value.trim())
            "period" -> period = value.trim()
            "regularity" -> regularity = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "split1" -> split1 = value.toInt()
            "activeLoan" -> activeLoan = (value.trim() == cTRUE)
            "loanFirstPaymentDate" -> loanFirstPaymentDate = if (value.trim().length >= 4) MyDate(value.trim()) else MyDate()
            "loanAmount" -> loanAmount = value.toDouble()
            "loanAmortization" -> loanAmortization = value.toDouble()
            "loanInterestRate" -> loanInterestRate = value.toDouble()
            "actualPayment" -> actualPayment = value.toDouble()
            "loanPaymentRegularity" ->
                    loanPaymentRegularity = if (value.toIntOrNull() != null)
                    LoanPaymentRegularity.getByValue(value.toInt())!!
                else {
                    try {
                        LoanPaymentRegularity.valueOf(value)
                    } catch (e: IllegalArgumentException) {
                        LoanPaymentRegularity.BIWEEKLY
                    }
                }
            "expirationDate" -> expirationDate = value.trim()
        }
    }
    fun getSplit2(): Int {
        return 100 - split1
    }
    fun getOutstandingLoanAmount(iDate: MyDate, iOwnershipPercentage: Double = 100.0): Int {
        if (activeLoan) {
            val myList = getPaymentList(loanFirstPaymentDate, loanAmortization, loanPaymentRegularity,
                loanInterestRate/100.0, loanAmount, if (actualPayment > 0.0) actualPayment else amount)

            var lastPayment: LoanPayment? = null
            for (loanPayment in myList) {
                if (loanPayment.paymentDate > iDate)
                    break
                lastPayment = loanPayment
            }
            if (lastPayment != null) {
                return round(lastPayment.loanPrincipalRemaining * iOwnershipPercentage / 100.0).toInt()

            }
        }
        return 0
    }
    fun isExpired() : Boolean {
        return (expirationDate != "" &&
                    (expirationDate < gCurrentDate.toString() ||
                     nextdate == "" ||
                     expirationDate < nextdate))
    }

    fun hasPaymentOnThisDate(iDate: String) : Boolean {
        if (lastDate.toString() == iDate)
            return true
        else if ((expirationDate != "" && expirationDate < iDate) ||
            nextdate == "")
            return false
        else if (nextdate == iDate)
            return true
        var tDate = MyDate(nextdate)
        while (tDate.toString() <= iDate) {
            when (period) {
                cPeriodWeek -> {
                    tDate.increment(cPeriodWeek, regularity)
                }
                cPeriodMonth -> {
                    tDate.increment(cPeriodMonth, regularity)
                }
                cPeriodQuarter -> {
                    tDate.increment(cPeriodMonth, regularity * 3)
                }
                cPeriodYear -> {
                    tDate.increment(cPeriodYear, regularity)
                }
            }
            if (tDate.toString() == iDate)
                return true
        }
        return false
    }
}

data class ScheduledPaymentOut(
    var vendor: String = "",
    var note: String = "",
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
    var loanPaymentRegularity: LoanPaymentRegularity = LoanPaymentRegularity.BIWEEKLY,
    var expirationDate: String = ""
) {
    constructor(sp: ScheduledPayment) : this(
        sp.vendor, sp.note, round(sp.amount*100).toInt(), sp.period, sp.regularity,
        sp.nextdate, sp.lastDate.toString(), sp.category, sp.paidby, sp.boughtfor, sp.split1,
        sp.activeLoan, sp.loanFirstPaymentDate.toString(), round(sp.loanAmount*100).toInt(),
        round(sp.loanAmortization*100).toInt(),round(sp.loanInterestRate*100).toInt(),
        round(sp.actualPayment*100).toInt(),
        sp.loanPaymentRegularity, sp.expirationDate)
}

class ScheduledPaymentViewModel : ViewModel() {
    private var scheduledPaymentListener: ValueEventListener? = null
    private val scheduledPayments: MutableList<ScheduledPayment> = ArrayList()
    val scheduledPaymentsLiveData = MutableLiveData<MutableList<ScheduledPayment>>()
//    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: ScheduledPaymentViewModel // used to track static single instance of self

        fun observeList(iFragment: Fragment, iObserver: androidx.lifecycle.Observer<MutableList<ScheduledPayment>>) {
            singleInstance.scheduledPaymentsLiveData.observe(iFragment, iObserver)
        }

        fun isLoaded():Boolean {
            return if (this::singleInstance.isInitialized) {
                singleInstance.loaded
            } else
                false
        }

        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.scheduledPayments.size
            else
                0
        }

        fun getScheduledPayment(iKey: String): ScheduledPayment? {
            return singleInstance.scheduledPayments.find { it.mykey == iKey }
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
  /*      fun showMe() {
            singleInstance.scheduledPayments.forEach {
                Timber.tag("Alex").d("SM Scheduled Payment is " + it.name + " amount " + it.amount + " regularity " + it.regularity + " period " + it.period + " lg " + it.nextdate +
                " loanAmount " + it.loanAmount + " firDt " + it.loanFirstPaymentDate)
            }
        } */

        fun getCopyOfScheduledPayments(): MutableList<ScheduledPayment> {
            val copy = mutableListOf<ScheduledPayment>()
            copy.addAll(singleInstance.scheduledPayments)
            return copy
        }

        fun getActiveLoanSPs(): MutableList<String> {
            val myList = mutableListOf<String>()
            singleInstance.scheduledPayments.forEach {
                if (it.activeLoan) {
                    myList.add(it.mykey)
                }
            }
            return myList
        }
        fun deleteScheduledPayment(iKey: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(iKey).removeValue()
        }

        fun addScheduledPayment(iScheduledPayment: ScheduledPayment) {
            val spOut = ScheduledPaymentOut(iScheduledPayment)
            val key = MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                    .push().key.toString()
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                .child(key).setValue(spOut)
        }
        fun updateScheduledPayment(iScheduledPayment: ScheduledPayment) {
            val mySP = singleInstance.scheduledPayments.find{ it.mykey == iScheduledPayment.mykey }
            if (mySP != null) {
                val spOut = ScheduledPaymentOut(iScheduledPayment)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                    .child(mySP.mykey).setValue(spOut)
            }
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
        fun generateScheduledPayments() {
            // now that recurring transaction settings are loaded, we need to review them to determine if any Transactions are needed
            singleInstance.scheduledPayments.forEach {
                if (!it.isExpired()) {
                    while (it.nextdate <= gCurrentDate.toString() &&
                        (it.expirationDate == "" || it.nextdate <= it.expirationDate)) {
                        val newNextDate = MyDate(it.nextdate)
                        it.lastDate = MyDate(it.nextdate)
                        // Reset nextDate
                        when (it.period) {
                            cPeriodWeek -> {
                                newNextDate.increment(cPeriodWeek, it.regularity)
                            }
                            cPeriodMonth -> {
                                newNextDate.increment(cPeriodMonth, it.regularity)
                            }
                            cPeriodQuarter -> {
                                newNextDate.increment(cPeriodMonth, it.regularity*3)
                            }
                            cPeriodYear -> {
                                newNextDate.increment(cPeriodYear, it.regularity)
                            }
                        }
                        if (it.expirationDate != "" && newNextDate.toString() > it.expirationDate) {
                            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                                .child(it.mykey).child("nextdate").setValue("")
                        } else {
                            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                                .child(it.mykey).child("nextdate").setValue(newNextDate.toString())
                        }
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/RecurringTransactions")
                            .child(it.mykey).child("lastDate").setValue(it.lastDate.toString())
                        // add transaction
                        val nextDate = getNextBusinessDate(MyDate(it.nextdate))
                        val nextDayIsBusinessDay = MyDate(it.nextdate) == nextDate
                        it.nextdate = newNextDate.toString()
                        Timber.tag("Alex").d("Adding transaction amount is ${it.amount}")
                        TransactionViewModel.addTransactionDatabase(TransactionOut(nextDate.toString(),
                            round(it.amount*100).toInt(),
                            it.category, it.vendor, it.note, it.paidby, it.boughtfor,
                            it.split1, cTRANSACTION_TYPE_SCHEDULED, it.mykey))
                        val outstandingLoanAmount = if (it.activeLoan) it.getOutstandingLoanAmount(
                            gCurrentDate) else 0
                        var tempString =  MyApplication.getString(R.string.scheduled_payment_was_added_for) + " ${it.vendor}"
                        if (it.note != "")
                            tempString += " (${it.note})"
                        tempString += " ${gDecWithCurrency(it.amount)} " +
                                CategoryViewModel.getFullCategoryName(it.category)
                        if (nextDayIsBusinessDay)
                            tempString += " " + MyApplication.getString(R.string.on_next_business_day)
                        tempString += " $nextDate"
                        if (outstandingLoanAmount > 0) tempString += " (outstanding loan amount ${
                            gDecWithCurrency(outstandingLoanAmount)})."
                        MyApplication.displayToast(tempString)
                    }
                }
            }
        }
        fun getScheduledPaymentsInNextDays(iDays: Int) : String {
            if (iDays <= 0)
                return ""
            val today = MyDate(gCurrentDate).toString()
            val tDate = MyDate(gCurrentDate)
            tDate.increment(cPeriodDay, iDays)
            var tReply = ""
            val todayString = String.format("%s %d",
                gShortMonthName(gCurrentDate.getMonth()),
                gCurrentDate.getDay())

            var tLoanText = ""
            singleInstance.scheduledPayments.forEach { sp ->
                val tempString = if (sp.note == "") sp.vendor
                else String.format("${sp.vendor} (${sp.note})")
                tLoanText = if (sp.activeLoan)
                    "\n  (Loan balance will be \$${sp.getOutstandingLoanAmount(tDate)} after payment)."
                else
                    ""
                if (sp.lastDate.toString() == today ||
                        sp.nextdate == today) {
                    tReply = if (tReply == "") {
                        "--\$${gDecM(sp.amount)} for $tempString due today, $todayString.$tLoanText"
                    } else {
                        "--\$${gDecM(sp.amount)} for $tempString due today, $todayString.$tLoanText\n$tReply"
                    }
                } else if (sp.nextdate != "" && sp.nextdate <= tDate.toString()) {
                    val myNextDate = MyDate(sp.nextdate)
                    tReply = if (tReply == "") {
                        "--\$${gDecM(sp.amount)} for $tempString due ${gShortMonthName(myNextDate.getMonth())} ${myNextDate.getDay()}.$tLoanText"
                    } else
                        "$tReply\n--\$${gDecM(sp.amount)} for $tempString due ${gShortMonthName(myNextDate.getMonth())} ${myNextDate.getDay()}.$tLoanText"
                }
            }

            return tReply
        }
        fun getScheduledPaymentsOnDate(iDate: String) : MutableList<ScheduledPayment> {
            val tList: MutableList<ScheduledPayment> = arrayListOf()
            singleInstance.scheduledPayments.forEach {
                if (it.hasPaymentOnThisDate(iDate))
                    tList.add(it)
            }
            return tList
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

/*    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }
*/
    fun loadScheduledPayments() {
        // Do an asynchronous operation to fetch scheduled payments pka recurring transactions
        scheduledPaymentListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                scheduledPayments.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tScheduledPayment = ScheduledPayment()
                    tScheduledPayment.setValue("key", element.key.toString())
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
                    if (tScheduledPayment.vendor == "")
                        tScheduledPayment.vendor = tScheduledPayment.mykey
                    if (tScheduledPayment.mykey != "lastDate")
                        scheduledPayments.add(tScheduledPayment)
                }
                sortYourself()
                singleInstance.loaded = true
//                dataUpdatedCallback?.onDataUpdate()
                singleInstance.scheduledPaymentsLiveData.value = singleInstance.scheduledPayments
                generateScheduledPayments()
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
        scheduledPayments.sortWith(compareBy({ it.isExpired() }, { it.nextdate }, { it.vendor }))
    }
}