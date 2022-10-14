@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

data class Transaction(
    var date: String = "",
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
    // amount is stored as original amount * 100 due to floating point issues at Firebase
    fun setValue(key: String, value: String) {
        when (key) {
            "key" -> mykey = value.trim()
            "date" -> date = value.trim()
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
                if (key != "bfname2split") Log.d("Alex", "Unknown field in Transactions $key $value $this")
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
                date.contains(lc) ||
                type.lowercase().contains(lc) ||
                (MyApplication.adminMode && mykey.lowercase().contains(lc))
    }
    fun getSplit2(): Int {
        return 100 - bfname1split
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
    fun setValue(key: String, value: String) {
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

class TransactionViewModel : ViewModel() {
    private var transListener: ValueEventListener? = null
    private val transactions: MutableList<Transaction> = mutableListOf()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: TransactionViewModel // used to track static single instance of self

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun transactionExistsUsingCategory(iCategoryID: Int): Int {
            var ctr = 0
            singleInstance.transactions.forEach {
                if (it.category == iCategoryID) {
                    ctr++
                }
            }
            return ctr
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

        fun getTotalActualsForRange(iStartMonth: String, iEndMonth: String,
                                    iDiscType: String, iWho: Int) : Double {
            var tmpTotal = 0.0

            singleInstance.transactions.forEach {
                val cat = CategoryViewModel.getCategory(it.category)
                if (it.type != cTRANSACTION_TYPE_TRANSFER
                    && it.date > iStartMonth && it.date < iEndMonth &&
                    (cat?.discType == iDiscType || iDiscType == cDiscTypeAll) &&
                    (it.boughtfor == iWho || it.boughtfor == 2 || iWho == 2 || iWho == -1) &&
                        cat?.iAmAllowedToSeeThisCategory() == true) {
                        tmpTotal += it.getAmountByUser(iWho)
                }
            }
            return (tmpTotal * 100.0).roundToInt() / 100.0
        }

        fun getCategoryActuals(iMonth: BudgetMonth, iPeriod: DateRange,
                               iDiscFlag: String, iWhoFlag: Int) : ArrayList<DataObject> {
            val tList: ArrayList<DataObject> = ArrayList()
            val startDate: String
            val endDate: String
            if (iPeriod == DateRange.ALLTIME) {
                startDate = "0000-00-00"
                endDate = "9999-99-99"
            } else if (iPeriod == DateRange.YTD) {
                startDate = iMonth.year.toString() + "-00-00"
                endDate = if (iMonth.month < 10) {
                    iMonth.year.toString() + "-0" + iMonth.month.toString() + "-99"
                } else {
                    iMonth.year.toString() + "-" + iMonth.month.toString() + "-99"
                }
            } else if (iPeriod == DateRange.YEAR) {
                startDate = iMonth.year.toString() + "-00-00"
                endDate = iMonth.year.toString() + "-99-99"
            } else { // iPeriod == DateRange.MONTH
                if (iMonth.month < 10) {
                    startDate =
                        iMonth.year.toString() + "-0" + iMonth.month.toString() + "-00"
                    endDate =
                        iMonth.year.toString() + "-0" + iMonth.month.toString() + "-99"
                } else {
                    startDate =
                        iMonth.year.toString() + "-" + iMonth.month.toString() + "-00"
                    endDate = iMonth.year.toString() + "-" + iMonth.month.toString() + "-99"
                }
            }

            for (i in 0 until getCount()) {
                val transaction = getTransaction(i)
                val cat = CategoryViewModel.getCategory(transaction.category)
                if (cat?.iAmAllowedToSeeThisCategory() == true) {
                        if (transaction.date > startDate && transaction.date < endDate) {
                            if (transaction.type != cTRANSACTION_TYPE_TRANSFER) {
                                val expDiscIndicator =
                                    CategoryViewModel.getCategory(transaction.category)?.discType
                                if (iDiscFlag == "" ||
                                    iDiscFlag == MyApplication.getString(R.string.all) ||
                                    iDiscFlag == expDiscIndicator) {
                                    if (iWhoFlag == 2 || transaction.boughtfor == iWhoFlag || transaction.boughtfor == 2) {
                                        // this is a transaction to add to our subtotal
                                        val amountToAdd = transaction.getAmountByUser(iWhoFlag)
                                        val dobj: DataObject? = tList.find { it.id == cat.id }
                                        if (dobj == null) {
                                            val pri = CategoryViewModel.getCategoryPriority(transaction.category).toString() +
                                                    CategoryViewModel.getCategory(transaction.category)?.subcategoryName
                                            val col = CategoryViewModel.getCategoryColour(transaction.category)
                                            tList.add(DataObject(cat.id, cat.categoryName,
                                                amountToAdd, pri,col))
                                        } else
                                            dobj.value += amountToAdd
                                    }
                                }
                            }
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
//                val cat = CategoryViewModel.getCategory(it.id)
//                Log.d("Alex", "tList: ${it.label} ${cat?.subcategoryName} ${it.value}")
            }

            return tList
        }

        fun getActualsForPeriod(iCategoryID: Int, iStartPeriod: BudgetMonth, iEndPeriod: BudgetMonth, iWho: Int, includeRelated: Boolean): Double {
            var tTotal = 0.0
            val firstDay = "$iStartPeriod-01"
            val  lastDay = "$iEndPeriod-31"
//            Log.d("Alex","getActualsForPeriod $iStartPeriod $iEndPeriod $iWho $includeRelated")
            loop@ for (transaction in singleInstance.transactions) {
                val cat = CategoryViewModel.getCategory(transaction.category)
                if (cat?.iAmAllowedToSeeThisCategory() == true) {
                    if (transaction.type != cTRANSACTION_TYPE_TRANSFER &&
                        transaction.date >= firstDay &&
                        transaction.date <= lastDay &&
                        transaction.category == iCategoryID &&
                        (transaction.boughtfor == iWho ||
                                (iWho == 2 && includeRelated) ||  // this picks up that we're looking for all Joint expenses and for a specific individual
                                (transaction.boughtfor == 2 && includeRelated))  // this picks up that we want all joint expenses regardless of who they're bought for
                    ) {
                        tTotal += when (iWho) {
                            0 -> transaction.getAmountByUser(0)
                            1 -> transaction.getAmountByUser(1)
                            else -> transaction.getAmountByUser(2)
                        }
                    }
                }
            }
            return (tTotal * 100.0).roundToInt() / 100.0
        }

        fun addTransaction(iTransactionOut: TransactionOut, iLocalOnly: Boolean = false) {
//            if (iLocalOnly) {
                singleInstance.transactions.add(Transaction(iTransactionOut.date,
                    iTransactionOut.amount/100.0, iTransactionOut.category,
                    iTransactionOut.note, iTransactionOut.note2,
                    iTransactionOut.paidby, iTransactionOut.boughtfor,
                    iTransactionOut.type, iTransactionOut.bfname1split))
//            } else {
            if (!iLocalOnly) {
                val bm = BudgetMonth(iTransactionOut.date)
                val key: String = if (iTransactionOut.type == cTRANSACTION_TYPE_SCHEDULED)
                    iTransactionOut.note + iTransactionOut.date + "R"
                else
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions" +
                    "/" + bm.year.toString() + "/" + bm.get2DigitMonth())
                        .push().key.toString()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                    .child(bm.year.toString())
                    .child(bm.get2DigitMonth())
                    .child(key)
                    .setValue(iTransactionOut)
            }
            singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }))
        }

        fun addTransaction(iTransfer: TransferOut, iLocalOnly: Boolean = false) {
            if (iLocalOnly) {
                singleInstance.transactions.add(
                    Transaction(iTransfer.date,
                    iTransfer.amount/100.0, cTRANSFER_CODE, "", "", iTransfer.paidby,
                    iTransfer.boughtfor, iTransfer.type, iTransfer.bfname1split,
                        cTRANSACTION_TYPE_TRANSFER)
                )
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
            singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun refresh() {
            singleInstance.loadTransactions()
        }
        fun clear() {
            if (singleInstance.transListener != null) {
                MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Transactions")
                    .removeEventListener(singleInstance.transListener!!)
                singleInstance.transListener = null
            }
            singleInstance.transactions.clear()
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

        fun deleteTransaction(date: String, iTransactionID: String) {
            val bm = BudgetMonth(date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID).removeValue()
            val trans =
                getTransaction(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = singleInstance.transactions.indexOf(trans)
            singleInstance.transactions.removeAt(ind)
        }

        fun getTransaction(iTransactionID: String): Transaction? {
            return singleInstance.transactions.find { it.mykey == iTransactionID }
        }

        fun updateTransaction(iTransactionID: String, iTransactionOut: TransactionOut) {
            val bm = BudgetMonth(iTransactionOut.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID)
                .setValue(iTransactionOut)
            val transaction =
                getTransaction(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (transaction != null) {
                transaction.date = iTransactionOut.date
                transaction.note = iTransactionOut.note
                transaction.note2 = iTransactionOut.note2
                transaction.category = iTransactionOut.category
                transaction.amount = iTransactionOut.amount/100.0
                transaction.paidby = iTransactionOut.paidby
                transaction.boughtfor = iTransactionOut.boughtfor
                transaction.bfname1split = iTransactionOut.bfname1split
                transaction.type = iTransactionOut.type
            }
            singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun updateTransaction(iTransactionID: String, iTransfer: TransferOut) {
            Log.d("Alex", "updateTransaction note is ${iTransfer.note2}")
            val bm = BudgetMonth(iTransfer.date)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Transactions")
                .child(bm.year.toString())
                .child(bm.get2DigitMonth())
                .child(iTransactionID)
                .setValue(iTransfer)
            val transaction =
                getTransaction(iTransactionID)  // this block below ensures that the viewAll view is updated immediately
            if (transaction != null) {
                transaction.date = iTransfer.date
                transaction.amount = iTransfer.amount/100.0
                transaction.paidby = iTransfer.paidby
                transaction.boughtfor = iTransfer.boughtfor
                transaction.bfname1split = iTransfer.bfname1split
                transaction.note2 = iTransfer.note2
            }
            singleInstance.transactions.sortWith(compareBy({ it.date }, { it.note }))
        }
        fun getEarliestYear() : Int {
            // since we know that this table is sorted on date, we simply return the first date
            return if (singleInstance.transactions.size == 0) {
                val dateNow = Calendar.getInstance()
                dateNow.get(Calendar.YEAR)
            } else singleInstance.transactions[0].date.substring(0,4).toInt()
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (transListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Transactions")
                .removeEventListener(transListener!!)
            transListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadTransactions() {
        // Do an asynchronous operation to fetch transactions
        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Transactions")
        transListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
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
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
                transactions.sortWith(compareBy({ it.date }, { it.note }))
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 108.")
            }
        }
        expDBRef.addValueEventListener(transListener as ValueEventListener)
    }
}
