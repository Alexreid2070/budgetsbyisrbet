package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

const val cDEFAULT_CATEGORY = "Category"
const val cDEFAULT_SUBCATEGORY = "SubCategory"
const val cDEFAULT_SPENDER = "Spender"
const val cDEFAULT_SHOWRED = "ShowRed"
const val cDEFAULT_INTEGRATEWITHTDSPEND = "IntegrateWithTDSpend"

class DefaultsViewModel : ViewModel() {
    lateinit var defaultsListener: ValueEventListener
    var dataUpdatedCallback: DefaultsDataUpdatedCallback? = null
    var defaultCategory: String = ""
    var defaultSubCategory: String = ""
    var defaultSpender: String = ""
    var defaultShowRed: String = "5"
    var defaultIntegrateWithTDSpend: String = "No"

    companion object {
        lateinit var singleInstance: DefaultsViewModel // used to track static single instance of self
        fun showMe() {
            Log.d("Alex", "Default Category/Subcategory is " + singleInstance.defaultCategory + "/" + singleInstance.defaultSubCategory)
            Log.d("Alex", "Default Spender is " + singleInstance.defaultSpender)
            Log.d("Alex", "Default ShowRed is " + singleInstance.defaultShowRed)
            Log.d("Alex", "Default IntegrateWithTDSpend is " + singleInstance.defaultIntegrateWithTDSpend)
        }
        fun getDefault(whichOne:String): String {
            if (whichOne == cDEFAULT_CATEGORY)
                return singleInstance.defaultCategory
            else if (whichOne == cDEFAULT_SUBCATEGORY)
                return singleInstance.defaultSubCategory
            else if (whichOne == cDEFAULT_SPENDER)
                return singleInstance.defaultSpender
            else if (whichOne == cDEFAULT_SHOWRED)
                return singleInstance.defaultShowRed
            else if (whichOne == cDEFAULT_INTEGRATEWITHTDSPEND)
                return singleInstance.defaultIntegrateWithTDSpend
            else
                return ""
        }

        fun updateDefault(whichOne: String, iValue: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults").child(whichOne).setValue(iValue)
        }

        fun refresh() {
            singleInstance.loadDefaults()
        }
    }

    init {
        DefaultsViewModel.singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults").removeEventListener(defaultsListener)
    }

    fun setCallback(iCallback: DefaultsDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
        dataUpdatedCallback?.onDataUpdate()
    }
    fun loadDefaults() {
        singleInstance.defaultsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                Log.d("Alex", "onDataChange " + dataSnapshot.toString() + " username " + MyApplication.userUID)
                dataSnapshot.children.forEach()
                {
                    Log.d("Alex", "processing " + it.key.toString() + " " + it.value.toString())
                    when (it.key.toString()) {
                        cDEFAULT_CATEGORY -> {
                            singleInstance.defaultCategory = it.value.toString()
                        }
                        cDEFAULT_SUBCATEGORY-> {
                            singleInstance.defaultSubCategory = it.value.toString()
                        }
                        cDEFAULT_SPENDER -> {
                            singleInstance.defaultSpender = it.value.toString()
                        }
                        cDEFAULT_SHOWRED-> {
                            singleInstance.defaultShowRed = it.value.toString().toString()
                        }
                        cDEFAULT_INTEGRATEWITHTDSPEND-> {
                            singleInstance.defaultIntegrateWithTDSpend = it.value.toString()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
//        MyApplication.database.getReference("Users").child(MyApplication.useruid)
//            .child("Defaults").addValueEventListener(singleInstance.defaultsListener)
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults").addValueEventListener(singleInstance.defaultsListener)
        showMe()
    }
}

public interface DefaultsDataUpdatedCallback  {
    fun onDataUpdate()
}