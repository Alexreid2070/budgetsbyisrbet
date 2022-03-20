package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

const val cDEFAULT_CATEGORY = "Category"
const val cDEFAULT_SUBCATEGORY = "SubCategory"
const val cDEFAULT_FULLCATEGORYNAME = "FullCategoryName"
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

class DefaultsViewModel : ViewModel() {
    private var defaultsListener: ValueEventListener? = null
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var defaultCategory: Category = Category("","")
    private var defaultSpender: String = "0"
    private var defaultShowRed: String = "5"
    private var defaultIntegrateWithTDSpend: String = "Off"
    private var defaultSound: String = "On"
    private var defaultQuote: String = "On"
    private var defaultShowIndividualAmountsInViewAll: String = "false"
    private var defaultShowWhoInViewAll: String = "true"
    private var defaultShowCategoryInViewAll: String = "true"
    private var defaultShowNoteInViewAll: String = "true"
    private var defaultShowDiscInViewAll: String = "true"
    private var defaultShowTypeInViewAll: String = "true"
    private var defaultShowRunningTotalInViewAll: String = "false"
    private var defaultViewPeriodDashboard: String = "Month"
    private var defaultFilterDiscDashboard: String = ""
    private var defaultFilterWhoDashboard: String = ""
    private var defaultDeltaDashboard: String = "#"
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: DefaultsViewModel // used to track static single instance of self

        fun isEmpty(): Boolean {
            return (isLoaded() &&
                    singleInstance.defaultCategory.categoryName == "" &&
                    singleInstance.defaultCategory.subcategoryName == "" &&
                    singleInstance.defaultSpender == "")
        }

        fun showMe() {
            Log.d("Alex", "Default Category/Subcategory is " + singleInstance.defaultCategory.fullCategoryName())
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
        }
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getDefault(whichOne:String): String {
            when (whichOne) {
                cDEFAULT_CATEGORY -> return singleInstance.defaultCategory.categoryName
                cDEFAULT_SUBCATEGORY -> return singleInstance.defaultCategory.subcategoryName
                cDEFAULT_FULLCATEGORYNAME -> return singleInstance.defaultCategory.fullCategoryName()
                cDEFAULT_SPENDER -> return singleInstance.defaultSpender
                cDEFAULT_SHOWRED -> return singleInstance.defaultShowRed
                cDEFAULT_INTEGRATEWITHTDSPEND -> return singleInstance.defaultIntegrateWithTDSpend
                cDEFAULT_SOUND -> return singleInstance.defaultSound
                cDEFAULT_QUOTE -> return singleInstance.defaultQuote
                cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL -> return singleInstance.defaultShowIndividualAmountsInViewAll
                cDEFAULT_SHOW_WHO_IN_VIEW_ALL -> return singleInstance.defaultShowWhoInViewAll
                cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL -> return singleInstance.defaultShowCategoryInViewAll
                cDEFAULT_SHOW_NOTE_VIEW_ALL -> return singleInstance.defaultShowNoteInViewAll
                cDEFAULT_SHOW_DISC_IN_VIEW_ALL -> return singleInstance.defaultShowDiscInViewAll
                cDEFAULT_SHOW_TYPE_IN_VIEW_ALL -> return singleInstance.defaultShowTypeInViewAll
                cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL -> return singleInstance.defaultShowRunningTotalInViewAll
                cDEFAULT_VIEW_PERIOD_DASHBOARD -> return singleInstance.defaultViewPeriodDashboard
                cDEFAULT_FILTER_DISC_DASHBOARD -> return singleInstance.defaultFilterDiscDashboard
                cDEFAULT_FILTER_WHO_DASHBOARD -> return singleInstance.defaultFilterWhoDashboard
                cDEFAULT_DELTA_DASHBOARD -> return singleInstance.defaultDeltaDashboard
                else -> return ""
            }
        }

        fun updateDefault(whichOne: String, iValue: String) {
            singleInstance.setLocal(whichOne, iValue)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                .child(SpenderViewModel.myIndex().toString())
                .child(whichOne).setValue(iValue)
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
            singleInstance.defaultCategory = Category("","")
            singleInstance.defaultSpender = "0"
            singleInstance.defaultShowRed = "5"
            singleInstance.defaultIntegrateWithTDSpend = "Off"
            singleInstance.defaultSound = "On"
            singleInstance.defaultQuote = "On"
            singleInstance.defaultShowIndividualAmountsInViewAll = "false"
            singleInstance.defaultShowWhoInViewAll = "true"
            singleInstance.defaultShowCategoryInViewAll = "true"
            singleInstance.defaultShowNoteInViewAll = "true"
            singleInstance.defaultShowDiscInViewAll = "true"
            singleInstance.defaultShowTypeInViewAll = "true"
            singleInstance.defaultShowRunningTotalInViewAll = "false"
            singleInstance.defaultViewPeriodDashboard = "Month"
            singleInstance.defaultFilterDiscDashboard = ""
            singleInstance.defaultFilterWhoDashboard = ""
            singleInstance.defaultDeltaDashboard = "#"
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

    fun setLocal(whichOne: String, iValue: String) {
        when (whichOne) {
            cDEFAULT_CATEGORY -> {
                singleInstance.defaultCategory.categoryName = iValue
            }
            cDEFAULT_SUBCATEGORY -> {
                singleInstance.defaultCategory.subcategoryName = iValue
            }
            cDEFAULT_SPENDER -> {
                singleInstance.defaultSpender = iValue
            }
            cDEFAULT_SHOWRED -> {
                singleInstance.defaultShowRed = iValue
            }
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
            else -> {
                Log.d("Alex", "Unknown default " + whichOne + " " + iValue)
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
