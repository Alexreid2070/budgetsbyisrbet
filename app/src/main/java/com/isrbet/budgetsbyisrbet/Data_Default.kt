@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*

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
const val cDEFAULT_VIEW_IN_RETIREMENT_DETAILS = "ViewInRetirementDetails"

const val cDEFAULT_CATEGORY_VALUE = 0
const val cDEFAULT_SPENDER_VALUE = -1
const val cDEFAULT_SHOW_RED_VALUE = 5
const val cDEFAULT_INTEGRATE_WITH_TDSPEND_VALUE = false
const val cDEFAULT_SOUND_VALUE = true
const val cDEFAULT_QUOTE_VALUE = true
const val cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL_VALUE = false
const val cDEFAULT_SHOW_WHO_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_SHOW_NOTE_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_SHOW_DISC_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_SHOW_TYPE_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL_VALUE = true
const val cDEFAULT_VIEW_PERIOD_DASHBOARD_VALUE = cPeriodMonth
const val cDEFAULT_FILTER_DISC_DASHBOARD_VALUE = ""
const val cDEFAULT_FILTER_WHO_DASHBOARD_VALUE = ""
const val cDEFAULT_DELTA_DASHBOARD_VALUE = "#"
const val cDEFAULT_ROUND_DASHBOARD_VALUE = false
const val cDEFAULT_SHOW_DISC_DASHBOARD_VALUE = true
const val cDEFAULT_BUDGET_VIEW_VALUE = cBudgetDateView
const val cDEFAULT_FILTER_DISC_TRACKER_VALUE = cDiscTypeAll
const val cDEFAULT_FILTER_WHO_TRACKER_VALUE = ""
const val cDEFAULT_VIEW_BY_TRACKER_VALUE = cPeriodMonth
const val cDEFAULT_SHOW_TOTALS_TRACKER_VALUE = "#"
const val cDEFAULT_SHOW_CURRENCY_SYMBOL_VALUE = true

class DefaultsViewModel : ViewModel() {
    private var defaultsListener: ValueEventListener? = null
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false
    var defaultCategory: Int = cDEFAULT_CATEGORY_VALUE
    var defaultSpender: Int = cDEFAULT_SPENDER_VALUE
    var defaultShowRed: Int = cDEFAULT_SHOW_RED_VALUE
    var defaultIntegrateWithTDSpend: Boolean = cDEFAULT_INTEGRATE_WITH_TDSPEND_VALUE
    var defaultSound: Boolean = cDEFAULT_SOUND_VALUE
    var defaultQuote: Boolean = cDEFAULT_QUOTE_VALUE
    var defaultShowIndividualAmountsInViewAll: Boolean = cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL_VALUE
    var defaultShowWhoInViewAll: Boolean = cDEFAULT_SHOW_WHO_IN_VIEW_ALL_VALUE
    var defaultShowCategoryInViewAll: Boolean = cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL_VALUE
    var defaultShowNoteInViewAll: Boolean = cDEFAULT_SHOW_NOTE_IN_VIEW_ALL_VALUE
    var defaultShowDiscInViewAll: Boolean = cDEFAULT_SHOW_DISC_IN_VIEW_ALL_VALUE
    var defaultShowTypeInViewAll: Boolean = cDEFAULT_SHOW_TYPE_IN_VIEW_ALL_VALUE
    var defaultShowRunningTotalInViewAll: Boolean = cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL_VALUE
    var defaultViewPeriodDashboard: String = cDEFAULT_VIEW_PERIOD_DASHBOARD_VALUE
    var defaultFilterDiscDashboard: String = cDEFAULT_FILTER_DISC_DASHBOARD_VALUE
    var defaultFilterWhoDashboard: String = cDEFAULT_FILTER_WHO_DASHBOARD_VALUE
    var defaultDeltaDashboard: String = cDEFAULT_DELTA_DASHBOARD_VALUE
    var defaultRoundDashboard: Boolean = cDEFAULT_ROUND_DASHBOARD_VALUE
    var defaultShowDiscDashboard: Boolean = cDEFAULT_SHOW_DISC_DASHBOARD_VALUE
    var defaultBudgetView: String = cDEFAULT_BUDGET_VIEW_VALUE
    var defaultFilterDiscTracker = cDEFAULT_FILTER_DISC_TRACKER_VALUE
    var defaultFilterWhoTracker = cDEFAULT_FILTER_WHO_TRACKER_VALUE
    var defaultViewByTracker = cDEFAULT_VIEW_BY_TRACKER_VALUE
    var defaultShowTotalsTracker: String = cDEFAULT_SHOW_TOTALS_TRACKER_VALUE
    var defaultShowCurrencySymbol: Boolean = cDEFAULT_SHOW_CURRENCY_SYMBOL_VALUE
    var defaultViewInRetirementDetails = RetirementDetailsViews.ALL
    val defaultCategoryDetails: MutableList<CategoryDetail> = ArrayList()

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
        fun getDefaultViewInRetirementDetails(): RetirementDetailsViews {
            return singleInstance.defaultViewInRetirementDetails
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
            singleInstance.defaultCategory = cDEFAULT_CATEGORY_VALUE
            singleInstance.defaultSpender = cDEFAULT_SPENDER_VALUE
            singleInstance.defaultShowRed = cDEFAULT_SHOW_RED_VALUE
            singleInstance.defaultIntegrateWithTDSpend = cDEFAULT_INTEGRATE_WITH_TDSPEND_VALUE
            singleInstance.defaultSound = cDEFAULT_SOUND_VALUE
            singleInstance.defaultQuote = cDEFAULT_QUOTE_VALUE
            singleInstance.defaultShowIndividualAmountsInViewAll = cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowWhoInViewAll = cDEFAULT_SHOW_WHO_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowCategoryInViewAll = cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowNoteInViewAll = cDEFAULT_SHOW_NOTE_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowDiscInViewAll = cDEFAULT_SHOW_DISC_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowTypeInViewAll = cDEFAULT_SHOW_TYPE_IN_VIEW_ALL_VALUE
            singleInstance.defaultShowRunningTotalInViewAll = cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL_VALUE
            singleInstance.defaultViewPeriodDashboard = cDEFAULT_VIEW_PERIOD_DASHBOARD_VALUE
            singleInstance.defaultFilterDiscDashboard = cDEFAULT_FILTER_DISC_DASHBOARD_VALUE
            singleInstance.defaultFilterWhoDashboard = cDEFAULT_FILTER_WHO_DASHBOARD_VALUE
            singleInstance.defaultDeltaDashboard = cDEFAULT_DELTA_DASHBOARD_VALUE
            singleInstance.defaultRoundDashboard = cDEFAULT_ROUND_DASHBOARD_VALUE
            singleInstance.defaultShowDiscDashboard = cDEFAULT_SHOW_DISC_DASHBOARD_VALUE
            singleInstance.defaultBudgetView = cDEFAULT_BUDGET_VIEW_VALUE
            singleInstance.defaultFilterDiscTracker = cDEFAULT_FILTER_DISC_TRACKER_VALUE
            singleInstance.defaultFilterWhoTracker = cDEFAULT_FILTER_WHO_TRACKER_VALUE
            singleInstance.defaultViewByTracker = cDEFAULT_VIEW_BY_TRACKER_VALUE
            singleInstance.defaultShowTotalsTracker = cDEFAULT_SHOW_TOTALS_TRACKER_VALUE
            singleInstance.defaultShowCurrencySymbol = cDEFAULT_SHOW_CURRENCY_SYMBOL_VALUE
            singleInstance.defaultViewInRetirementDetails = RetirementDetailsViews.ALL
            singleInstance.defaultCategoryDetails.clear()
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
        Log.d("Alex", "WHY IS DEFAULT onCleared being called??")
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
            cDEFAULT_VIEW_IN_RETIREMENT_DETAILS -> {
                singleInstance.defaultViewInRetirementDetails = try {
                    RetirementDetailsViews.getByValue(iValue)!!
                } catch (e: IllegalArgumentException) {
                    RetirementDetailsViews.ALL
                }
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
            cDEFAULT_VIEW_IN_RETIREMENT_DETAILS -> {
                singleInstance.defaultViewInRetirementDetails = if (isNumber(iValue))
                    RetirementDetailsViews.getByValue(iValue.toInt())!!
                else {
                    try {
                        RetirementDetailsViews.valueOf(iValue)
                    } catch (e: IllegalArgumentException) {
                        RetirementDetailsViews.ALL
                    }
                }
            }
        }
    }
    fun loadDefaults() {
        singleInstance.defaultsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                clear()
                RetirementViewModel.clearDefaults()
                // Get Post object and use the values to update the UI
                for (defaultRow in dataSnapshot.children.toMutableList()) {
                    if (defaultRow.key.toString() == "CategoryDetails") {
                        for (cat in defaultRow.children.toMutableList()) {
                            val catName = cat.key.toString()
                            for (def in cat.children.toMutableList()) {
                                when (def.key.toString()) {
                                    "colour" -> setColour(catName, def.value.toString().toInt(), true)
                                    "priority" -> setPriority(catName, def.value.toString().toInt(), true)
                                }
                            }
                        }
                    } else if (defaultRow.key.toString() == "Retirement") {
                        for (retUser in defaultRow.children.toMutableList()) {
//                            val userId = retUser.key.toString().toInt()
                            val retData = RetirementData.create(retUser.children.toMutableList())
                            RetirementViewModel.updateRetirementDefault(retData, true)
                        }
                    } else
                        setLocal(defaultRow.key.toString(), defaultRow.value.toString())
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
