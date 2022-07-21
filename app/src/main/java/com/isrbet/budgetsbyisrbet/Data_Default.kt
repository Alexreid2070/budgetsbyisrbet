@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

const val cDEFAULT_CATEGORY_ID = "Category"
const val cDEFAULT_SPENDER = "Spender"
const val cDEFAULT_SHOWRED = "ShowRed"
const val cDEFAULT_INTEGRATEWITHTDSPEND = "IntegrateWithTDSpend"
const val cDEFAULT_SOUND = "Sound"
const val cDEFAULT_QUOTE = "Quote"
const val cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL = "ShowIndividualAmountsinViewAll"
const val cDEFAULT_SHOW_WHO_IN_VIEW_ALL = "ShowWhoinViewAll"
const val cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL = "ShowCategoryinViewAll"
const val cDEFAULT_SHOW_NOTE_VIEW_ALL = "ShowNoteinViewAll"
const val cDEFAULT_SHOW_DISC_IN_VIEW_ALL = "ShowDiscinViewAll"
const val cDEFAULT_SHOW_TYPE_IN_VIEW_ALL = "ShowTypeinViewAll"
const val cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL = "ShowRunningTotalinViewAll"
const val cDEFAULT_VIEW_PERIOD_DASHBOARD = "ViewPeriodDashboard"
const val cDEFAULT_FILTER_DISC_DASHBOARD = "FilterDiscDashboard"
const val cDEFAULT_FILTER_WHO_DASHBOARD = "FilterWhoDashboard"
const val cDEFAULT_DELTA_DASHBOARD = "DeltaDashboard"
const val cDEFAULT_ROUND_DASHBOARD = "RoundDashboard"
const val cDEFAULT_SHOW_DISC_DASHBOARD = "ShowDiscDashboard"
const val cDEFAULT_BUDGET_VIEW = "BudgetView"
const val cDEFAULT_FILTER_DISC_TRACKER = "FilterDiscTracker"
const val cDEFAULT_FILTER_WHO_TRACKER = "FilterWhoTracker"
const val cDEFAULT_VIEW_BY_TRACKER = "ViewByTracker"
const val cDEFAULT_SHOW_TOTALS_TRACKER = "ShowTotalsTracker"
const val cDEFAULT_SHOW_CURRENCY_SYMBOL = "ShowCurrencySymbol"

class DefaultsViewModel : ViewModel() {
    private var defaultsListener: ValueEventListener? = null
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    var defaultCategory: Int = 0
    var defaultSpender: Int = -1
    var defaultShowRed: Int = 5
    var defaultIntegrateWithTDSpend: Boolean = false
    var defaultSound: Boolean = true
    var defaultQuote: Boolean = true
    var defaultShowIndividualAmountsInViewAll: Boolean = false
    var defaultShowWhoInViewAll: Boolean = true
    var defaultShowCategoryInViewAll: Boolean = true
    var defaultShowNoteInViewAll: Boolean = true
    var defaultShowDiscInViewAll: Boolean = true
    var defaultShowTypeInViewAll: Boolean = true
    var defaultShowRunningTotalInViewAll: Boolean = false
    var defaultViewPeriodDashboard: String = cPeriodMonth
    var defaultFilterDiscDashboard: String = ""
    var defaultFilterWhoDashboard: String = ""
    var defaultDeltaDashboard: String = "#"
    var defaultRoundDashboard: Boolean = false
    var defaultShowDiscDashboard: Boolean = true
    var defaultBudgetView: String = cBudgetDateView
    val defaultCategoryDetails: MutableList<CategoryDetail> = ArrayList()
    var defaultFilterDiscTracker = cDiscTypeAll
    var defaultFilterWhoTracker = ""
    var defaultViewByTracker = cPeriodMonth
    var defaultShowTotalsTracker: String = "#"
    var defaultShowCurrencySymbol: Boolean = true
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: DefaultsViewModel // used to track static single instance of self

/*        fun showMe() {
            Log.d("Alex", "Default Category/Subcategory is " + CategoryViewModel.getFullCategoryName(singleInstance.defaultCategory))
            Log.d("Alex", "Default Spender is " + singleInstance.defaultSpender)
            Log.d("Alex", "Default ShowRed is " + singleInstance.defaultShowRed)
            Log.d("Alex", "Default IntegrateWithTDSpend is " + singleInstance.defaultIntegrateWithTDSpend)
            Log.d("Alex", "Default sound is " + singleInstance.defaultSound)
            Log.d("Alex", "Default quote is " + singleInstance.defaultQuote)
            Log.d("Alex", "Default showWho is " + singleInstance.defaultShowWhoInViewAll)
            Log.d("Alex", "Default showNote is " + singleInstance.defaultShowNoteInViewAll)
            Log.d("Alex", "Default showDisc is " + singleInstance.defaultShowDiscInViewAll)
            Log.d("Alex", "Default showType is " + singleInstance.defaultShowTypeInViewAll)
            Log.d("Alex", "Default showViewPeriodDashboard is " + singleInstance.defaultViewPeriodDashboard)
            Log.d("Alex", "Default filterDiscDashboard is " + singleInstance.defaultFilterDiscDashboard)
            Log.d("Alex", "Default filterWhoDashboard is " + singleInstance.defaultFilterWhoDashboard)
            Log.d("Alex", "Default deltaDashboard is " + singleInstance.defaultDeltaDashboard)
            Log.d("Alex", "Default roundDashboard is " + singleInstance.defaultRoundDashboard)
            Log.d("Alex", "Default showDiscDashboard is " + singleInstance.defaultShowDiscDashboard)
            Log.d("Alex", "Default budgetView is " + singleInstance.defaultBudgetView)
            Log.d("Alex", "Default filterDiscTracker is " + singleInstance.defaultFilterDiscTracker)
            Log.d("Alex", "Default filterWhoTracker is " + singleInstance.defaultFilterWhoTracker)
            Log.d("Alex", "Default viewByTracker is " + singleInstance.defaultViewByTracker)
            Log.d("Alex", "Default showTotalsTracker is " + singleInstance.defaultShowTotalsTracker)
            Log.d("Alex", "Default showCurrencySymbol is " + singleInstance.defaultShowCurrencySymbol)
        }
        fun showMeCategoryDetails() {
            singleInstance.defaultCategoryDetails.forEach {
                Log.d("Alex", "${it.name} ${it.color} ${it.priority}" )
            }
        } */
        fun getDefaultCategory(): Int {
            return singleInstance.defaultCategory
        }
        fun getDefaultFullCategoryName(): String {
            val cat = CategoryViewModel.getCategory(singleInstance.defaultCategory)
            return "${cat?.categoryName}-${cat?.subcategoryName}"
        }
        fun getDefaultSpender(): Int {
            return singleInstance.defaultSpender
        }
        fun getDefaultShowRed(): Int {
            return singleInstance.defaultShowRed
        }
        fun getDefaultIntegrateWithTDSpend(): Boolean {
            return singleInstance.defaultIntegrateWithTDSpend
        }
        fun getDefaultSound(): Boolean {
            return singleInstance.defaultSound
        }
        fun getDefaultQuote(): Boolean {
            return singleInstance.defaultQuote
        }
        fun getDefaultShowIndividualAmountsInViewAll(): Boolean {
            return singleInstance.defaultShowIndividualAmountsInViewAll
        }
        fun getDefaultShowWhoInViewAll(): Boolean {
            return singleInstance.defaultShowWhoInViewAll
        }
        fun getDefaultShowCategoryInViewAll(): Boolean {
            return singleInstance.defaultShowCategoryInViewAll
        }
        fun getDefaultShowNoteInViewAll(): Boolean {
            return singleInstance.defaultShowNoteInViewAll
        }
        fun getDefaultShowDiscInViewAll(): Boolean {
            return singleInstance.defaultShowDiscInViewAll
        }
        fun getDefaultShowTypeInViewAll(): Boolean {
            return singleInstance.defaultShowTypeInViewAll
        }
        fun getDefaultShowRunningTotalInViewAll(): Boolean {
            return singleInstance.defaultShowRunningTotalInViewAll
        }
        fun getDefaultViewPeriodDashboard(): String {
            return singleInstance.defaultViewPeriodDashboard
        }
        fun getDefaultFilterDiscDashboard(): String {
            return singleInstance.defaultFilterDiscDashboard
        }
        fun getDefaultFilterWhoDashboard(): String {
            return singleInstance.defaultFilterWhoDashboard
        }
        fun getDefaultDeltaDashboard(): String {
            return singleInstance.defaultDeltaDashboard
        }
        fun getDefaultRoundDashboard(): Boolean {
            return singleInstance.defaultRoundDashboard
        }
        fun getDefaultShowDiscDashboard(): Boolean {
            return singleInstance.defaultShowDiscDashboard
        }
        fun getDefaultBudgetView(): String {
            return singleInstance.defaultBudgetView
        }
        fun getDefaultFilterDiscTracker(): String {
            return singleInstance.defaultFilterDiscTracker
        }
        fun getDefaultFilterWhoTracker(): String {
            return singleInstance.defaultFilterWhoTracker
        }
        fun getDefaultViewByTracker(): String {
            return singleInstance.defaultViewByTracker
        }
        fun getDefaultShowTotalsTracker(): String {
            return singleInstance.defaultShowTotalsTracker
        }
        fun getDefaultShowCurrencySymbol(): Boolean {
            return singleInstance.defaultShowCurrencySymbol
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun updateDefaultString(whichOne: String, iValue: String) {
            singleInstance.setLocalString(whichOne, iValue)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                .child(SpenderViewModel.myIndex().toString())
                .child(whichOne).setValue(iValue)
        }
        fun updateDefaultInt(whichOne: String, iValue: Int) {
            singleInstance.setLocalInt(whichOne, iValue)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                .child(SpenderViewModel.myIndex().toString())
                .child(whichOne).setValue(iValue)
        }
        fun updateDefaultBoolean(whichOne: String, iValue: Boolean) {
            singleInstance.setLocalBoolean(whichOne, iValue)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                .child(SpenderViewModel.myIndex().toString())
                .child(whichOne).setValue(if (iValue) "true" else "false")
        }

        fun refresh() {
            singleInstance.loadDefaults()
        }
        fun clear() {
            if (singleInstance.defaultsListener != null) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                    .child(SpenderViewModel.myIndex().toString())
                    .removeEventListener(singleInstance.defaultsListener!!)
                singleInstance.defaultsListener = null
            }
            resetToDefaults()
        }
        private fun resetToDefaults() {
            singleInstance.defaultCategory = 0
            singleInstance.defaultSpender = -1
            singleInstance.defaultShowRed = 5
            singleInstance.defaultIntegrateWithTDSpend = false
            singleInstance.defaultSound = true
            singleInstance.defaultQuote = true
            singleInstance.defaultShowIndividualAmountsInViewAll = false
            singleInstance.defaultShowWhoInViewAll = true
            singleInstance.defaultShowCategoryInViewAll = true
            singleInstance.defaultShowNoteInViewAll = true
            singleInstance.defaultShowDiscInViewAll = true
            singleInstance.defaultShowTypeInViewAll = true
            singleInstance.defaultShowRunningTotalInViewAll = false
            singleInstance.defaultViewPeriodDashboard = cPeriodMonth
            singleInstance.defaultFilterDiscDashboard = ""
            singleInstance.defaultFilterWhoDashboard = ""
            singleInstance.defaultDeltaDashboard = "#"
            singleInstance.defaultRoundDashboard = false
            singleInstance.defaultShowDiscDashboard = true
            singleInstance.defaultBudgetView = cBudgetDateView
            singleInstance.defaultCategoryDetails.clear()
            singleInstance.defaultFilterDiscTracker = cDiscTypeAll
            singleInstance.defaultFilterWhoTracker = ""
            singleInstance.defaultViewByTracker = cPeriodMonth
            singleInstance.defaultShowTotalsTracker = "#"
            singleInstance.defaultShowCurrencySymbol = true
        }

        fun getCategoryDetails(): MutableList<CategoryDetail> {
            val copy = mutableListOf<CategoryDetail>()
            copy.addAll(singleInstance.defaultCategoryDetails)
            return copy
        }
        fun deleteCategoryDetail(iCatName: String) {
            val cd: CategoryDetail? =
                singleInstance.defaultCategoryDetails.find { it.name == iCatName }
            if (cd != null) {
                val ind = singleInstance.defaultCategoryDetails.indexOf(cd)
                singleInstance.defaultCategoryDetails.removeAt(ind)

                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("CategoryDetails")
                    .child(iCatName)
                    .removeValue()
            }
        }
        fun reorderCategory(fromPriority: Int, toPriority: Int) {
            val minP = minOf(fromPriority, toPriority)
            val maxP = maxOf(fromPriority, toPriority)
            singleInstance.defaultCategoryDetails.forEach {
                if (it.priority in minP..maxP) {
                    when {
                        it.priority == fromPriority -> {
                            setPriority(it.name, toPriority, false)
                        }
                        fromPriority == maxP -> { // items move down
                            setPriority(it.name, it.priority + 1, false)
                        }
                        else -> {
                            setPriority(it.name, it.priority - 1, false)
                        }
                    }
                }
            }
            singleInstance.defaultCategoryDetails.sortWith(compareBy { it.priority })
        }
        private fun giveMeNextAvailablePriority(): Int {
            var maxPriority = -1
            singleInstance.defaultCategoryDetails.forEach {
                if (it.priority >= maxPriority)
                    maxPriority = it.priority
            }
            return maxPriority + 1
        }
        fun getCategoryDetail(iCatName: String): CategoryDetail {
            return singleInstance.defaultCategoryDetails.find { it.name == iCatName }
                ?: CategoryDetail(iCatName, 0, 9999)
        }

        fun setColour(iCatName: String, iColour: Int, iUpdateLocalOnly: Boolean) {
            val cat: CategoryDetail? = singleInstance.defaultCategoryDetails.find { it.name == iCatName }
            if (cat == null) {
                singleInstance.defaultCategoryDetails.add(CategoryDetail(iCatName, iColour, giveMeNextAvailablePriority()))
            } else {
                cat.color = iColour
            }
            if (!iUpdateLocalOnly) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("CategoryDetails")
                    .child(iCatName)
                    .child("colour")
                    .setValue(iColour)
            }
        }
        fun setPriority(iCatName: String, iPriority: Int, iUpdateLocalOnly: Boolean) {
            val cat: CategoryDetail? = singleInstance.defaultCategoryDetails.find { it.name == iCatName }
            if (cat == null) {
                singleInstance.defaultCategoryDetails.add(CategoryDetail(iCatName, 0, iPriority))
            } else {
                cat.priority = iPriority
            }
            if (!iUpdateLocalOnly) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("CategoryDetails")
                    .child(iCatName)
                    .child("priority")
                    .setValue(iPriority)
            }
        }
        fun confirmCategoryDetailsListIsComplete() { // ie add any missing ones, should only happen at first transition to this new functionality
            val catList = CategoryViewModel.getCategoryNames()
            for (cat in catList) {
                val cd: CategoryDetail? = singleInstance.defaultCategoryDetails.find { it.name == cat }
                if (cd == null) {
                    setPriority(cat,
                        if (singleInstance.defaultCategoryDetails.size == 0) 99 else giveMeNextAvailablePriority(), // this will force a renumbering and load the priorities in the db
                        false)
                    setColour(cat, 0, false)
                }
            }
            singleInstance.defaultCategoryDetails.sortWith(compareBy { it.priority })
            for (i in 0 until singleInstance.defaultCategoryDetails.size) {
                if (singleInstance.defaultCategoryDetails[i].priority != i) {
                    setPriority(singleInstance.defaultCategoryDetails[i].name, i, false)
                }
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (defaultsListener != null) {
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                .child(SpenderViewModel.myIndex().toString())
                .removeEventListener(defaultsListener!!)
            defaultsListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }
    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun setLocalString(whichOne: String, iValue: String) {
        when (whichOne) {
            cDEFAULT_VIEW_PERIOD_DASHBOARD -> {
                singleInstance.defaultViewPeriodDashboard = iValue
            }
            cDEFAULT_FILTER_DISC_DASHBOARD -> {
                singleInstance.defaultFilterDiscDashboard = iValue
            }
            cDEFAULT_FILTER_WHO_DASHBOARD -> {
                singleInstance.defaultFilterWhoDashboard = iValue
            }
            cDEFAULT_DELTA_DASHBOARD -> {
                singleInstance.defaultDeltaDashboard = iValue
            }
            cDEFAULT_BUDGET_VIEW -> {
                singleInstance.defaultBudgetView = iValue
            }

            cDEFAULT_FILTER_DISC_TRACKER -> {
                singleInstance.defaultFilterDiscTracker = iValue
            }
            cDEFAULT_FILTER_WHO_TRACKER -> {
                singleInstance.defaultFilterWhoTracker = iValue
            }
            cDEFAULT_VIEW_BY_TRACKER -> {
                singleInstance.defaultViewByTracker = iValue
            }
            cDEFAULT_SHOW_TOTALS_TRACKER -> {
                singleInstance.defaultShowTotalsTracker = iValue
            }
        }
    }
    fun setLocalInt(whichOne: String, iValue: Int) {
        when (whichOne) {
            cDEFAULT_SHOWRED -> {
                singleInstance.defaultShowRed = iValue
            }
            cDEFAULT_CATEGORY_ID -> {
                if (isNumber(iValue.toString()))
                    singleInstance.defaultCategory = iValue
            }
            cDEFAULT_SPENDER -> {
                singleInstance.defaultSpender = iValue
            }
        }
    }
    fun setLocalBoolean(whichOne: String, iValue: Boolean) {
        when (whichOne) {
            cDEFAULT_INTEGRATEWITHTDSPEND -> {
                singleInstance.defaultIntegrateWithTDSpend = iValue
            }
            cDEFAULT_SOUND -> {
                singleInstance.defaultSound = iValue
            }
            cDEFAULT_QUOTE -> {
                singleInstance.defaultQuote = iValue
            }
            cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL -> {
                singleInstance.defaultShowIndividualAmountsInViewAll = iValue
            }
            cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL -> {
                singleInstance.defaultShowCategoryInViewAll = iValue
            }
            cDEFAULT_SHOW_WHO_IN_VIEW_ALL -> {
                singleInstance.defaultShowWhoInViewAll = iValue
            }
            cDEFAULT_SHOW_NOTE_VIEW_ALL -> {
                singleInstance.defaultShowNoteInViewAll = iValue
            }
            cDEFAULT_SHOW_DISC_IN_VIEW_ALL -> {
                singleInstance.defaultShowDiscInViewAll = iValue
            }
            cDEFAULT_SHOW_TYPE_IN_VIEW_ALL -> {
                singleInstance.defaultShowTypeInViewAll = iValue
            }
            cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL -> {
                singleInstance.defaultShowRunningTotalInViewAll = iValue
            }
            cDEFAULT_ROUND_DASHBOARD -> {
                singleInstance.defaultRoundDashboard = iValue
            }
            cDEFAULT_SHOW_DISC_DASHBOARD -> {
                singleInstance.defaultShowDiscDashboard = iValue
            }
            cDEFAULT_SHOW_CURRENCY_SYMBOL -> {
                singleInstance.defaultShowCurrencySymbol = iValue
            }
        }
    }
    fun setLocal(whichOne: String, iValue: String) {
        when (whichOne) {
            cDEFAULT_VIEW_PERIOD_DASHBOARD -> {
                singleInstance.defaultViewPeriodDashboard = iValue
            }
            cDEFAULT_FILTER_DISC_DASHBOARD -> {
                singleInstance.defaultFilterDiscDashboard = iValue
            }
            cDEFAULT_FILTER_WHO_DASHBOARD -> {
                singleInstance.defaultFilterWhoDashboard = iValue
            }
            cDEFAULT_DELTA_DASHBOARD -> {
                singleInstance.defaultDeltaDashboard = iValue
            }
            cDEFAULT_BUDGET_VIEW -> {
                singleInstance.defaultBudgetView = iValue
            }
            cDEFAULT_FILTER_DISC_TRACKER -> {
                singleInstance.defaultFilterDiscTracker = iValue
            }
            cDEFAULT_FILTER_WHO_TRACKER -> {
                singleInstance.defaultFilterWhoTracker = iValue
            }
            cDEFAULT_VIEW_BY_TRACKER -> {
                singleInstance.defaultViewByTracker = iValue
            }
            cDEFAULT_SHOW_TOTALS_TRACKER -> {
                singleInstance.defaultShowTotalsTracker = iValue
            }
            cDEFAULT_CATEGORY_ID -> {
                if (isNumber(iValue))
                    singleInstance.defaultCategory = iValue.toInt()
            }
            cDEFAULT_SPENDER -> {
                if (isNumber(iValue))
                    singleInstance.defaultSpender = iValue.toInt()
            }
            cDEFAULT_INTEGRATEWITHTDSPEND -> {
                singleInstance.defaultIntegrateWithTDSpend = (iValue == cTRUE)
            }
            cDEFAULT_SOUND -> {
                singleInstance.defaultSound = (iValue == cTRUE)
            }
            cDEFAULT_QUOTE -> {
                singleInstance.defaultQuote = (iValue == cTRUE)
                Log.d("Alex", "setting sound to $iValue")
            }
            cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL -> {
                singleInstance.defaultShowIndividualAmountsInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL -> {
                singleInstance.defaultShowCategoryInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_WHO_IN_VIEW_ALL -> {
                singleInstance.defaultShowWhoInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_NOTE_VIEW_ALL -> {
                singleInstance.defaultShowNoteInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_DISC_IN_VIEW_ALL -> {
                singleInstance.defaultShowDiscInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_TYPE_IN_VIEW_ALL -> {
                singleInstance.defaultShowTypeInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL -> {
                singleInstance.defaultShowRunningTotalInViewAll = (iValue == cTRUE)
            }
            cDEFAULT_ROUND_DASHBOARD -> {
                singleInstance.defaultRoundDashboard = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_DISC_DASHBOARD -> {
                singleInstance.defaultShowDiscDashboard = (iValue == cTRUE)
            }
            cDEFAULT_SHOW_CURRENCY_SYMBOL -> {
                singleInstance.defaultShowCurrencySymbol = (iValue == cTRUE)
            }
        }
    }
    fun loadDefaults() {
        singleInstance.defaultsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                clear()
                // Get Post object and use the values to update the UI
                dataSnapshot.children.forEach()
                {
                    if (it.key.toString() == "CategoryDetails") {
                        for (cat in it.children.toMutableList()) {
                            val catName = cat.key.toString()
                            for (def in cat.children.toMutableList()) {
                                when (def.key.toString()) {
                                    "colour" -> setColour(catName, def.value.toString().toInt(), true)
                                    "priority" -> setPriority(catName, def.value.toString().toInt(), true)
                                }
                            }
                        }
                    } else
                        setLocal(it.key.toString(), it.value.toString())
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast("User authorization failed 107.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
            .child(SpenderViewModel.myIndex().toString())
            .addValueEventListener(
                singleInstance.defaultsListener as ValueEventListener
            )
    }
}
