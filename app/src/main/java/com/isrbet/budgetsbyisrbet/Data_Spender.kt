package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Spender(var name: String, var email: String, var split: Int, var isActive: Int) {
    constructor(spender: Spender) : this(spender.name, spender.email, spender.split, spender.isActive)
}

class SpenderViewModel : ViewModel() {
    private var spenderListener: ValueEventListener? = null
    private val spenders: MutableList<Spender> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    // The primary user is always at index 0, and the secondary is always at index 1

    companion object {
        lateinit var singleInstance: SpenderViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun showMe() {
            singleInstance.spenders.forEach {
                Log.d("Alex", "SM Spender is ${it.name} ${it.split} ${it.email} ${it.isActive}")
            }
        }
        fun getSpender(index:Int): Spender? {
            return if (index  < singleInstance.spenders.size) {
                Spender(singleInstance.spenders[index])
            } else
                null
        }
        fun getSpenderName(index:Int): String {
            return if (index  < singleInstance.spenders.size)
                singleInstance.spenders[index].name
            else
                ""
        }
        fun getSpenderSplit(index: Int): Int {
            return if (index  < singleInstance.spenders.size && index >= 0)
                singleInstance.spenders[index].split
            else
                0
        }
        fun myIndex(): Int {
            return if (iAmPrimaryUser())
                0
            else
                1
        }
        fun getSpenderIndex(iName:String): Int {
            return when (iName) {
                singleInstance.spenders[0].name -> 0
                singleInstance.spenders[1].name -> 1
                else -> 2
            }
        }
        fun getDefaultSpender() : Int {
            val ind = DefaultsViewModel.getDefault(cDEFAULT_SPENDER).toInt()
            return ind
        }
        fun getDefaultSpenderName() : String {
            val ind = DefaultsViewModel.getDefault(cDEFAULT_SPENDER).toInt()
            return getSpenderName(ind)
        }
        fun updateSpenderSplits(iFirstSplit: Int) {
            singleInstance.spenders[0].split = iFirstSplit
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(
                "0").child("split").setValue(iFirstSplit)
            if (singleInstance.spenders.size > 1) {
                singleInstance.spenders[1].split = 100 - iFirstSplit
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(
                    "1").child("split").setValue(100-iFirstSplit)
            }
        }

        fun getActiveSpenders() : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.spenders.forEach {
                if (it.isActive == 1)
                    list.add(it.name)
            }
            return list
        }

        fun getTotalCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.spenders.size
            else
                0
        }
        fun getActiveCount(): Int {
            return if (::singleInstance.isInitialized) {
                var ctr = 0
                singleInstance.spenders.forEach {
                    if (it.isActive == 1)
                        ctr++
                }
                ctr
            } else
                0
        }

        fun singleUser(): Boolean {
            return getActiveCount() == 1
        }

        fun addLocalSpender(spender: Spender) {
            singleInstance.spenders.add(spender)
        }
        fun addSpender(index: Int, spender: Spender) {
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                .child(index.toString()).child("name").setValue(spender.name)
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                .child(index.toString()).child("email").setValue(spender.email)
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                .child(index.toString()).child("isactive").setValue(spender.isActive)
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                .child(index.toString()).child("split").setValue(spender.split)
    }
        fun updateSpender(index: Int, spender: Spender) {
            if (spender.name != singleInstance.spenders[index].name) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                    .child(index.toString()).child("name").setValue(spender.name)
            }
            if (spender.email != singleInstance.spenders[index].email) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                    .child(index.toString()).child("email").setValue(spender.email)
            }
            if (spender.isActive != singleInstance.spenders[index].isActive) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                    .child(index.toString()).child("isactive").setValue(spender.isActive)
            }
            if (spender.split != singleInstance.spenders[index].split) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Spender")
                    .child(index.toString()).child("split").setValue(spender.split)
            }
        }
        fun refresh() {
            singleInstance.loadSpenders()
        }
        fun clear() {
            if (singleInstance.spenderListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Spender")
                    .removeEventListener(singleInstance.spenderListener!!)
                singleInstance.spenderListener = null
            }
            singleInstance.spenders.clear()
            singleInstance.loaded = false
        }
        fun removeSecondAllowedUser() {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Info").child("SecondUser").removeValue()
        }
        fun saveAsSecondAllowedUser(iEmail: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Info").child("SecondUser").setValue(iEmail)
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (spenderListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Spender")
                .removeEventListener(spenderListener!!)
            spenderListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadSpenders() {
        // Do an asynchronous operation to fetch spenders
        spenderListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                spenders.clear()
                dataSnapshot.children.forEach()
                {
                    var email = ""
                    var isActive = 1
                    var split = 0
                    var name = ""
                    for (spenderRow in it.children) {
                        when (spenderRow.key.toString()) {
                            "name" -> name = spenderRow.value.toString()
                            "email" -> email = spenderRow.value.toString()
                            "isactive" -> isActive = spenderRow.value.toString().toInt()
                            "split" -> split = spenderRow.value.toString().toInt()
                        }
                    }
                    spenders.add(Spender(name, email, split, isActive))
                }
                if (getActiveCount() > 1)
                    spenders.add(Spender("Joint", "", 100, 1))
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 110.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").addValueEventListener(
            spenderListener as ValueEventListener
        )
    }
}