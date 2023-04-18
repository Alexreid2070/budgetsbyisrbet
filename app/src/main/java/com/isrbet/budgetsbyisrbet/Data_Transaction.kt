@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round
import kotlin.math.roundToInt

data class Transaction(
    var date: MyDate = MyDate(),
    var amount: Double = 0.0,
    var category: Int = 0,
    var note: String = "",
    var note2: String = "",
    var paidby: Int = -1,
    var boughtfor: Int = -1,
    var type: String = cTRANSACTION_TYPE_EXPENSE,
    var bfname1split: Int = 0,
    var mykey: String = ""
) {
    constructor(iTransactionOut: TransactionOut, iKey: String) : this(
        MyDate(iTransactionOut.date),
        iTransactionOut.amount / 100.0,
        iTransactionOut.category,
        iTransactionOut.note,
        iTransactionOut.note2,
        iTransactionOut.paidby,
        iTransactionOut.boughtfor,
        iTransactionOut.type,
        iTransactionOut.bfname1split,
        iKey
    )

    // amount is stored as original amount * 100 due to floating point issues at Firebase
    fun setValue(key: String, value: String) {
        when (key) {
            "key" -> mykey = value.trim()
            "date" -> date = MyDate(value.trim())
            "amount" -> amount = value.toDouble()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "note" -> note = value.trim()
            "note2" -> note2 = value.trim()
            "type" -> type = value.trim()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
            else -> {
                if (key != "bfname2split") Timber.tag("Alex").d("Unknown field in Transactions $key $value $this")
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
                note2.lowercase().contains(lc) ||
                date.toString().contains(lc) ||
                type.lowercase().contains(lc) ||
                (MyApplication.adminMode && mykey.lowercase().contains(lc))
    }
    fun getAmountByUser(iWho: Int, iRound: Boolean = true): Double {
        when (iWho) {
            0 -> {
                when (boughtfor) {
                    0 -> return amount
                    1 -> return 0.0
                    2 -> return if (iRound) round(amount * bfname1split) / 100.0
                        else amount * bfname1split / 100.0
                }
            }
            1 -> {
                when (boughtfor) {
                    0 -> return 0.0
                    1 -> return amount
                    2 -> return if (iRound) amount - round(amount * bfname1split) / 100.0
                        else amount - (amount * bfname1split / 100.0)
                }
            }
            else -> return amount
        }
        return 0.0
    }
}

data class TransactionOut(
    var date: String = "", var amount: Int = 0, var category: Int = 0,
    var note: String = "", var note2: String = "", var paidby: Int = -1,
    var boughtfor: Int = -1,
    var bfname1split: Int = 0, var type: String = cTRANSACTION_TYPE_EXPENSE
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    // doesn't have a key, because we don't want to store the key at Firebase, it'll generate one for us.
/*    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "category" -> category = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "note" -> note = value.trim()
            "note2" -> note2 = value.trim()
            "type" -> type = value.trim()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
        }
    } */
    fun getAmountByUser(iWho: Int, iRound: Boolean = true): Double {
        Timber.tag("Alex").d("getAmountByUser who $iWho rnd $iRound boughtfor$boughtfor amt $amount split $bfname1split")
        when (iWho) {
            0 -> {
                when (boughtfor) {
                    0 -> return amount/100.0
                    1 -> return 0.0
                    2 -> return if (iRound) round(amount/100.0 * bfname1split) / 100.0
                    else amount/100.0 * bfname1split / 100.0
                }
            }
            1 -> {
                when (boughtfor) {
                    0 -> return 0.0
                    1 -> return amount/100.0
                    2 -> return if (iRound) amount - round(amount/100.0 * bfname1split) / 100.0
                    else amount / 100.0 - (amount / 100.0 * bfname1split / 100.0)
                }
            }
            else -> return amount/100.0
        }
        return 0.0
    }
}

data class TransferOut(
    var date: String = "", var amount: Int = 0,
    var paidby: Int = -1, var boughtfor: Int = -1,
    var bfname1split: Int = 0,
    var note: String = "", var note2: String = "",
    var category: Int = cTRANSFER_CODE,
    var type: String = cTRANSACTION_TYPE_TRANSFER
) {
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    // doesn't have a key, because we don't want to store the key at Firebase, it'll generate one for us.
/*    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value.trim()
            "amount" -> amount = value.toInt()
            "paidby" -> paidby = value.toInt()
            "boughtfor" -> boughtfor = value.toInt()
            "bfname1split" -> bfname1split = value.toInt()
            "who" -> {if (paidby == -1) paidby = value.toInt(); if (boughtfor == -1) boughtfor = value.toInt() }
        }
    }*/
}

class TransactionViewModel : ViewModel() {
//    private var transListener: ValueEventListener? = null
    private var firstLoadListener: ValueEventListener? = null
    private var childListener: ChildEventListener? = null
    private val transactions: MutableList<Transaction> = mutableListOf()
    val transactionsLiveData = MutableLiveData<MutableList<Transaction>>()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false
    val actualsSummary: MutableList<ActualRow> = mutableListOf()

    companion object {
        lateinit var singleInstance: TransactionViewModel // used to track static single instance of self

/*        fun observeList(iFragment: Fragment, iObserver: androidx.lifecycle.Observer<MutableList<Transaction>>) {
            singleInstance.transactionsLiveData.observe(iFragment, iObserver)
        } */
        fun doSomething() {
            singleInstance.transactions.forEach {
                val transactionOut = TransactionOut(it.date.toString(),
                    round(it.amount*100).toInt(),
                    it.category,
                    it.note,
                    it.note2,
                    it.paidby,
                    it.boughtfor,
                    it.bfname1split,
                    it.type)
                val key = MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
                    .push().key.toString()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
                    .child(key)
                    .setValue(transactionOut)
            }
        }
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getTransaction(iTransactionID: String): Transaction? {
            return singleInstance.transactions.find { it.mykey == iTransactionID }
        }

        fun transactionExistsUsingCategory(iCategoryID: Int): Int {
            return singleInstance.transactions.filter { it.category == iCategoryID }.size
/*            var ctr = 0
            singleInstance.transactions.forEach {
                if (it.category == iCategoryID) {
                    ctr++
                }
            }
            return ctr */
        }
        fun getTransaction(i:Int): Transaction {
            return singleInstance.transactions[i]
        }
        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.transactions.size
            else
                0
        }

        fun getCopyOfTransactions(): MutableList<Transaction> {
            val copy = mutableListOf<Transaction>()
            copy.addAll(singleInstance.transactions)

            if (CategoryViewModel.isThereAtLeastOneCategoryThatIAmNotAllowedToSee()) {
                for (i in copy.indices.reversed()) {
                    val cat = CategoryViewModel.getCategory(copy[i].category)
                    if (cat?.iAmAllowedToSeeThisCategory() != true) {
                        copy.removeAt(i)
                    }
                }
            }
            return copy
        }

        fun getTotalActualsForRange(iStartMonth: MyDate, iEndMonth: MyDate,
                                    iDiscType: String, iWho: Int) : Double {
            var tmpTotal = 0.0

            for (category in CategoryViewModel.getCategories(true)) {
                if ((category.discType == iDiscType || iDiscType == cDiscTypeAll)
                    && category.iAmAllowedToSeeThisCategory()) {
                        tmpTotal += getActualsForPeriod(
                            category.id,
                            iStartMonth,
                            iEndMonth,
                            iWho)
                }
            }
            return (tmpTotal * 100.0).roundToInt() / 100.0
        }

        fun getCategoryActuals(iMonth: MyDate, iPeriod: DateRangeEnum,
                               iDiscFlag: String, iWhoFlag: Int) : ArrayList<DataObject> {
            val tList: ArrayList<DataObject> = ArrayList()
            val startDate: MyDate
            val endDate: MyDate
            when (iPeriod) {
                DateRangeEnum.ALLTIME -> {
                    startDate = MyDate(getEarliestYear(),1,1)
                    endDate = MyDate(getLatestYear(),12,31)
                }
                DateRangeEnum.YTD -> {
                    startDate = MyDate(iMonth.getYear(),1,1)
                    endDate = iMonth.getLastDayOfMonth()
                }
                DateRangeEnum.YEAR -> {
                    startDate = MyDate(iMonth.getYear(),1,1)
                    endDate = MyDate(iMonth.getYear(),12,31)
                }
                else -> { // iPeriod == DateRangeEnum.MONTH
                    startDate = iMonth.getFirstOfMonth()
                    endDate = iMonth.getLastDayOfMonth()
                }
            }

            for (category in CategoryViewModel.getCategories(true)) {
                if ((category.discType == iDiscFlag || iDiscFlag == cDiscTypeAll ||
                            iDiscFlag == "")
                    && category.iAmAllowedToSeeThisCategory()) {
                    val tempTotal = getActualsForPeriod(
                        category.id,
                        startDate,
                        endDate,
                        iWhoFlag)

                    if (tempTotal != 0.0) {
                        val dobj: DataObject? = tList.find { it.id == category.id }
                        if (dobj == null) {
                            val pri = CategoryViewModel.getCategoryPriority(category.id).toString() +
                                    CategoryViewModel.getCategory(category.id)?.subcategoryName
                            val col = CategoryViewModel.getCategoryColour(category.id)
                            tList.add(DataObject(category.id, category.categoryName,
                                tempTotal, pri,col))
                        } else
                            dobj.value += tempTotal
                    }
                }
            }

            tList.sortWith(compareBy { it.priority })

            tList.forEach {
                if (iWhoFlag == 0) {
                    // this below will round up the total for user 0, and down for user 1 so that no cents are missing
                    val t = ((it.value + .001) * 1000).toInt()
                    val td = (t / 10.0).roundToInt()
                    it.value = td / 100.0
                } else
                    it.value = round(it.value * 100.0) / 100.0
            }
            return tList
        }

        data class AnnualCategoryTotal(var catID: Int, var year: Int, var value: Double)
        fun getAnnualCategoryActuals(iDiscFlag: String, iWhoFlag: Int) : ArrayList<AnnualCategoryTotal> {
            val tList: ArrayList<AnnualCategoryTotal> = ArrayList()

            val categories = CategoryViewModel.getCategories(true)
            for (category in categories) {
                if (category.iAmAllowedToSeeThisCategory() &&
                    category.discType == iDiscFlag || iDiscFlag == "" || iDiscFlag == cDiscTypeAll) {
                    for (year in getEarliestYear() until getLatestYear() + 1) {
                        var tempTotal = 0.0
                        for (userID in 0 until 2) {
                            if (iWhoFlag == userID || iWhoFlag == 2) {
                                val userActual = singleInstance.actualsSummary.find {
                                    category.id == it.categoryID &&
                                            year == it.year &&
                                            it.who == userID
                                }
                                if (userActual != null) {
                                    tempTotal += userActual.getAnnualAmount()
                                }
                            }
                        }
                        tList.add(AnnualCategoryTotal(category.id, year, tempTotal))
                    }
                }
            }
            return tList
        }

        fun getActualsForPeriod(iCategoryID: Int, iStartPeriod: MyDate, iEndPeriod: MyDate, iWho: Int): Double {
            var tempTotal = 0.0
            val cat = CategoryViewModel.getCategory(iCategoryID)
            if (cat?.iAmAllowedToSeeThisCategory() == true) {
                for (year in iStartPeriod.getYear() until iEndPeriod.getYear() + 1) {
                    for (userID in 0 until 2) {
                        if (iWho == userID || iWho == 2) {
                            val userActual = singleInstance.actualsSummary.find {
                                it.categoryID == iCategoryID &&
                                        it.year == year &&
                                        it.who == userID
                            }
                            if (userActual != null) {
                                tempTotal += if (iStartPeriod.getYear() == iEndPeriod.getYear()) {
                                    userActual.getRangeTotal(
                                        iStartPeriod.getMonth(),
                                        iEndPeriod.getMonth()
                                    )
                                } else if (year == iStartPeriod.getYear()) {
                                    userActual.getRangeTotal(iStartPeriod.getMonth(), 12)
                                } else if (year == iEndPeriod.getYear()) {
                                    userActual.getRangeTotal(1, iEndPeriod.getMonth())
                                } else {
                                    userActual.getAnnualAmount()
                                }
                            }
                        }
                    }
                }
            }
            return tempTotal
        }
        fun refresh() {
            singleInstance.loadTransactions()
        }
        fun clear() {
            if (singleInstance.firstLoadListener != null) {
                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/TransactionsNew")
                    .removeEventListener(singleInstance.firstLoadListener!!)
                singleInstance.firstLoadListener = null
            }
            if (singleInstance.childListener != null) {
                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/TransactionsNew")
                    .removeEventListener(singleInstance.childListener!!)
                singleInstance.childListener = null
            }
            singleInstance.transactions.clear()
            singleInstance.actualsSummary.clear()
            singleInstance.loaded = false
        }

        fun getPreviousKey(iKey: String): String {
            val exp = singleInstance.transactions.find { it.mykey == iKey }
            var ind = singleInstance.transactions.indexOf(exp)
            val startingInd = ind
            var found = false
            while (!found) {
                if (ind == 0)
                    ind = singleInstance.transactions.size
                ind -=1
                val cat = CategoryViewModel.getCategory(singleInstance.transactions[ind].category)
                if (ind == startingInd || cat?.iAmAllowedToSeeThisCategory() == true)
                    found = true
            }
            return singleInstance.transactions[ind].mykey
        }
        fun getNextKey(iKey: String): String {
            val exp = singleInstance.transactions.find { it.mykey == iKey }
            var ind = singleInstance.transactions.indexOf(exp)
            val startingInd = ind
            var found = false
            while (!found) {
                if (ind == singleInstance.transactions.size-1)
                    ind = -1
                ind +=1
                val cat = CategoryViewModel.getCategory(singleInstance.transactions[ind].category)
                if (ind == startingInd || cat?.iAmAllowedToSeeThisCategory() == true)
                    found = true
            }
            return singleInstance.transactions[ind].mykey
        }
        fun getPreviousTransferKey(iKey: String): String {
            val exp = singleInstance.transactions.find { it.mykey == iKey }
            val ind = singleInstance.transactions.indexOf(exp)
            for (i in ind-1 downTo 0)
                if (singleInstance.transactions[i].type == cTRANSACTION_TYPE_TRANSFER)
                    return singleInstance.transactions[i].mykey
            // if we get here, we didn't find a transfer in the rest of the transaction list, so start at the beginning
            for (i in singleInstance.transactions.size-1 downTo ind)
                if (singleInstance.transactions[i].type == cTRANSACTION_TYPE_TRANSFER)
                    return singleInstance.transactions[i].mykey
            return iKey
        }
        fun getNextTransferKey(iKey: String): String {
            val exp = singleInstance.transactions.find { it.mykey == iKey }
            val ind = singleInstance.transactions.indexOf(exp)
            for (i in ind+1 until singleInstance.transactions.size)
                if (singleInstance.transactions[i].type == cTRANSACTION_TYPE_TRANSFER)
                    return singleInstance.transactions[i].mykey
            // if we get here, we didn't find a transfer in the rest of the transaction list, so start at the beginning
            for (i in 0 until singleInstance.transactions.size)
                if (singleInstance.transactions[i].type == cTRANSACTION_TYPE_TRANSFER)
                    return singleInstance.transactions[i].mykey
            return iKey
        }

        fun addTransactionLocal(iTransaction: Transaction, iNotifyLive: Boolean = true) {
            val transaction = getTransaction(iTransaction.mykey)
            if (transaction != null &&
                transaction == iTransaction) {
                return
            }
            updateTransactionLocal(iTransaction, iNotifyLive)
//            singleInstance.transactions.add(iTransaction)
            //          singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }, {it.type}))
            //        singleInstance.transactionsLiveData.value = singleInstance.transactions
        }
        fun addTransactionDatabase(iTransactionOut: TransactionOut) {
            Timber.tag("Alex").d("addTransactionDatabase $iTransactionOut")
            adjustActualsSummary(MyDate(iTransactionOut.date), iTransactionOut.category, 0, iTransactionOut.getAmountByUser(0, false))
            adjustActualsSummary(MyDate(iTransactionOut.date), iTransactionOut.category, 1, iTransactionOut.getAmountByUser(1, false))
            val key: String = if (iTransactionOut.type == cTRANSACTION_TYPE_SCHEDULED)
                iTransactionOut.note + iTransactionOut.date + "R"
            else
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
                    .push().key.toString()
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
                .child(key)
                .setValue(iTransactionOut)
//            singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }, {it.type}))
        }

        fun deleteTransactionDatabase(iKey: String) {
//            val bm = MyDate(date)
            val trans =
                getTransaction(iKey) // this block below ensures that the viewAll view is updated immediately
            if (trans != null) {
                adjustActualsSummary(trans.date, trans.category, 0, -trans.getAmountByUser(0, false))
                adjustActualsSummary(trans.date, trans.category, 1, -trans.getAmountByUser(1, false))
            }
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TransactionsNew")
//                .child(bm.year.toString())
//                .child(bm.get2DigitMonth())
                .child(iKey).removeValue()
/*            val trans =
                getTransaction(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = singleInstance.transactions.indexOf(trans)
            singleInstance.transactions.removeAt(ind) */
        }
        fun deleteTransactionLocal(iKey: String) {
//            val bm = MyDate(date)
//            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
//                .child(bm.year.toString())
//                .child(bm.get2DigitMonth())
//                .child(iTransactionID).removeValue()
            val trans =
                getTransaction(iKey) // this block below ensures that the viewAll view is updated immediately
            if (trans != null) {
                val ind = singleInstance.transactions.indexOf(trans)
                if (ind != -1) {
                    singleInstance.transactions.removeAt(ind)
                    singleInstance.transactionsLiveData.value = singleInstance.transactions
                }
            }
        }

        fun updateTransactionLocal(iTransaction: Transaction, iNotifyLive: Boolean = true) {
            // this is new one
            val transaction = getTransaction(iTransaction.mykey)
            if (transaction == iTransaction) { // ie all contents are structurally equal
                return
            }
            if (transaction == null) {
                singleInstance.transactions.add(iTransaction)
            } else {
                transaction.date = MyDate(iTransaction.date)
                transaction.note = iTransaction.note
                transaction.note2 = iTransaction.note2
                transaction.category = iTransaction.category
                transaction.amount = iTransaction.amount
                transaction.paidby = iTransaction.paidby
                transaction.boughtfor = iTransaction.boughtfor
                transaction.bfname1split = iTransaction.bfname1split
                transaction.type = iTransaction.type
            }
            singleInstance.transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
            if (iNotifyLive)
                singleInstance.transactionsLiveData.value = singleInstance.transactions
        }

        fun updateTransactionDatabase(iKey: String, iTransactionOut: TransactionOut) {
//            val bm = MyDate(iTransactionOut.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TransactionsNew")
//                .child(bm.year.toString())
//                .child(bm.get2DigitMonth())
                .child(iKey)
                .setValue(iTransactionOut)
            val transaction =
                getTransaction(iKey)  // this block below ensures that the viewAll view is updated immediately
            if (transaction != null) {
                adjustActualsSummary(transaction.date, transaction.category, 0, -transaction.getAmountByUser(0, false))
                adjustActualsSummary(transaction.date, transaction.category, 1, -transaction.getAmountByUser(1, false))
                adjustActualsSummary(MyDate(iTransactionOut.date), iTransactionOut.category, 0, iTransactionOut.getAmountByUser(0, false))
                adjustActualsSummary(MyDate(iTransactionOut.date), iTransactionOut.category, 1, iTransactionOut.getAmountByUser(1, false))

                transaction.date = MyDate(iTransactionOut.date)
                transaction.note = iTransactionOut.note
                transaction.note2 = iTransactionOut.note2
                transaction.category = iTransactionOut.category
                transaction.amount = iTransactionOut.amount/100.0
                transaction.paidby = iTransactionOut.paidby
                transaction.boughtfor = iTransactionOut.boughtfor
                transaction.bfname1split = iTransactionOut.bfname1split
                transaction.type = iTransactionOut.type
            }
            singleInstance.transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
            singleInstance.transactionsLiveData.value = singleInstance.transactions
        }
        fun addTransfer(iTransfer: TransferOut, iLocalOnly: Boolean = false) {
            if (iLocalOnly) {
                singleInstance.transactions.add(
                    Transaction(MyDate(iTransfer.date),
                        iTransfer.amount/100.0, cTRANSFER_CODE, "", "", iTransfer.paidby,
                        iTransfer.boughtfor, iTransfer.type, iTransfer.bfname1split,
                        cTRANSACTION_TYPE_TRANSFER)
                )
            } else {
//                val bm = MyDate(iTransfer.date)
                val key = MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
//                        "/" + bm.year.toString() + "/" + bm.get2DigitMonth())
                    .push().key.toString()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/TransactionsNew")
//                    .child(bm.year.toString())
//                    .child(bm.get2DigitMonth())
                    .child(key)
                    .setValue(iTransfer)
            }
            singleInstance.transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
        }
        fun updateTransfer(iTransactionID: String, iTransfer: TransferOut) {
//            val bm = MyDate(iTransfer.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TransactionsNew")
//                .child(bm.year.toString())
//                .child(bm.get2DigitMonth())
                .child(iTransactionID)
                .setValue(iTransfer)
            val transaction =
                getTransaction(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (transaction != null) {
                transaction.date = MyDate(iTransfer.date)
                transaction.amount = iTransfer.amount/100.0
                transaction.paidby = iTransfer.paidby
                transaction.boughtfor = iTransfer.boughtfor
                transaction.bfname1split = iTransfer.bfname1split
                transaction.note2 = iTransfer.note2
            }
            singleInstance.transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
        }
        fun getEarliestYear() : Int {
            // since we know that this table is sorted on date, we simply return the first date
            return if (singleInstance.transactions.size == 0) {
                gCurrentDate.getYear()
            } else singleInstance.transactions[0].date.getYear()
        }
        fun getEarliestMonth() : Int {
            // since we know that this table is sorted on date, we simply return the first date
            return if (singleInstance.transactions.size == 0) {
                gCurrentDate.getMonth()
            } else singleInstance.transactions[0].date.getMonth()
        }
        fun getLatestYear() : Int {
            // since we know that this table is sorted on date, we simply return the last date
            return if (singleInstance.transactions.size == 0) {
                gCurrentDate.getYear()
            } else singleInstance.transactions[singleInstance.transactions.size-1].date.getYear()
        }
/*        fun getLatestMonth() : Int {
            // since we know that this table is sorted on date, we simply return the last date
            return if (singleInstance.transactions.size == 0) {
                gCurrentDate.getMonth()
            } else singleInstance.transactions[singleInstance.transactions.size-1].date.getMonth()
        } */
private fun adjustActualsSummary(iDate: MyDate, iCategoryID: Int, iWho: Int, iAdjustment: Double) {
            val actualRow = singleInstance.actualsSummary.find {
                it.categoryID == iCategoryID &&
                it.year == iDate.getYear() &&
                it.who == iWho
            }
            if (actualRow == null) {
                Timber.tag("Alex").d("adjustActualsSummary: added new actualsSummary row $iDate $iCategoryID $iWho $iAdjustment")
                val newActualRow = ActualRow(
                    iDate.getYear(),
                    iCategoryID,
                    iWho)
                newActualRow.amounts[iDate.getMonth()-1] = iAdjustment
                singleInstance.actualsSummary.add(newActualRow)
            } else {
                Timber.tag("Alex").d("adjustActualsSummary: adjusted $iAdjustment to $iDate $iCategoryID $iWho")
                actualRow.amounts[iDate.getMonth()-1] += iAdjustment
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (firstLoadListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/TransactionsNew")
                .removeEventListener(firstLoadListener!!)
            firstLoadListener = null
        }
        if (childListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/TransactionsNew")
                .removeEventListener(childListener!!)
            childListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

/*    fun loadTransactionsbkp() {
        // Do an asynchronous operation to fetch transactions
        val start = System.currentTimeMillis()
        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Transactions")
        firstLoadListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val start2 = System.currentTimeMillis()

                transactions.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    for (year in element.children) {
                        for (month in year.children) {
                            val tExp = Transaction()
                            tExp.setValue("key", month.key.toString())
                            for (child in month.children) {
                                if (child.key.toString() == "amount")
                                    tExp.setValue(
                                        child.key.toString(),
                                        (child.value.toString().toInt() / 100.0).toString()
                                    )
                                else
                                    tExp.setValue(child.key.toString(), child.value.toString())
                            }
                            transactions.add(tExp)
                        }
                    }
                }
                val start3 = System.currentTimeMillis()
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
                transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
                val end = System.currentTimeMillis()
                Timber.tag("Alex").d("loadTransactions time is ${end - start} ms, ${end - start2}, ${end - start3}")
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 108.")
            }
        }
        expDBRef.addValueEventListener(firstLoadListener as ValueEventListener)
    } */
    fun loadTransactions() {
        // Do an asynchronous operation to fetch transactions
        val start = System.currentTimeMillis()
        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/TransactionsNew")
        firstLoadListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                transactions.clear()
                Timber.tag("Alex").d("Found ${dataSnapshot.childrenCount} transactions")
                for (element in dataSnapshot.children.toMutableList()) {
                    val transactionOut = element.getValue<TransactionOut>()
                    if (transactionOut != null) {
                        val myTr = Transaction(transactionOut, element.key!!)
                        transactions.add(myTr)
                    }
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
                transactions.sortWith(compareBy({ it.date.toString() }, { it.note }, {it.type}))
                val end = System.currentTimeMillis()
                Timber.tag("Alex").d("loadTransactions time is ${end - start} ms")

                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/TransactionsNew")
                    .removeEventListener(singleInstance.firstLoadListener!!)
                singleInstance.firstLoadListener = null
                Timber.tag("Alex").d("number of transactions is now ${singleInstance.transactions.size}")
                loadTransactionsOngoing()
                createActualsTable()
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 108.")
            }
        }
        expDBRef.addValueEventListener(firstLoadListener as ValueEventListener)
    }
    fun loadTransactionsOngoing() {
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // this function is called on startup, and when a transaction is added
                val transactionOut = dataSnapshot.getValue<TransactionOut>()
                if (transactionOut != null) {
                        val myTr = Transaction(transactionOut, dataSnapshot.key!!)
                        addTransactionLocal(myTr)
                } else {
                    Timber.tag("Alex").d( "onChildAdded couldn't convert ${dataSnapshot.key.toString()}")
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val transactionOut = dataSnapshot.getValue<TransactionOut>()
                if (transactionOut != null) {
                    val myTr = Transaction(transactionOut, dataSnapshot.key!!)
                    updateTransactionLocal(myTr)
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                deleteTransactionLocal(dataSnapshot.key!!)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Timber.tag("Alex").d("onChildMoved:%s", dataSnapshot.key!!)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.tag("Alex").d("loadTransactions:onCancelled ${databaseError.toException()}")
            }
        }
        MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/TransactionsNew")
            .addChildEventListener(childEventListener)
        childListener = childEventListener

    }

    data class ActualRow(val year:Int, val categoryID: Int, val who: Int) {
        val amounts = DoubleArray(12) {0.0}
        fun getAnnualAmount(): Double {
            return amounts.sum()
        }
        fun getRangeTotal(iStartMonth: Int, iEndMonth: Int) : Double {
            var tDouble = 0.0
            for (i in iStartMonth-1 until iEndMonth)
                tDouble += amounts[i]
            return tDouble
        }
    }

    fun createActualsTable() {
        Timber.tag("Alex").d("CreateActualsTable start")
        actualsSummary.clear()
        var firstOne = true

        for (transaction in singleInstance.transactions) {
            if (transaction.type != cTRANSACTION_TYPE_TRANSFER) {
                if (firstOne) {
                    for (user in 0 until 2) {
                        if (transaction.boughtfor == user || transaction.boughtfor == 2) {
                            val actualRow = ActualRow(
                                transaction.date.getYear(),
                                transaction.category,
                                user)
                            actualRow.amounts[transaction.date.getMonth()-1] = transaction.getAmountByUser(user)
                            actualsSummary.add(actualRow)
                        }
                    }
                    firstOne = false
                } else {
                    for (user in 0 until 2) {
                        if (transaction.boughtfor == user || transaction.boughtfor == 2) {
                            val oldActualRow = actualsSummary.find {
                                transaction.category == it.categoryID &&
                                    transaction.date.getYear() == it.year &&
                                    it.who == user
                            }
                            if (oldActualRow == null) {
                                val newActualRow = ActualRow(
                                    transaction.date.getYear(),
                                    transaction.category,
                                    user)
                                newActualRow.amounts[transaction.date.getMonth()-1] = transaction.getAmountByUser(user)
                                actualsSummary.add(newActualRow)
                            } else {
                                oldActualRow.amounts[transaction.date.getMonth()-1] += transaction.getAmountByUser(user)
                            }
                        }
                    }
                }
            }
        }
    }
}
