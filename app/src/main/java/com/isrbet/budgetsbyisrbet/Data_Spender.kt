package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Spender(var name: String, var email: String, var split: Int, var isActive: Int)

class SpenderViewModel : ViewModel() {
    private var spenderListener: ValueEventListener? = null
    val spenders: MutableList<Spender> = ArrayList()
    var dataUpdatedCallback: SpenderDataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: SpenderViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun showMe() {
            singleInstance.spenders.forEach {
                Log.d("Alex", "SM Spender is " + it.name + " and % is " + it.split)
            }
        }
        fun getSpender(pos:Int, activeOnly:Boolean): Spender? {
            if (pos  < singleInstance.spenders.size) {
                if ((activeOnly && singleInstance.spenders[pos].isActive == 1) || !activeOnly)
                    return singleInstance.spenders[pos]
                else
                    return null
            } else
                return null
        }
        fun getSpenderName(pos:Int): String {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].name
            else
                return ""
        }
        fun getSpenderEmail(pos:Int): String {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].email
            else
                return ""
        }
        fun isActive(pos:Int): Boolean {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].isActive == 1
            else
                return false
        }
        fun getSpenderSplit(pos:Int): Int {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].split
            else
                return 0
        }
        fun getSpenders() : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.spenders.forEach {
                list.add(it.name)
            }
            return list
        }

        fun getTotalCount(): Int {
            if (::singleInstance.isInitialized)
                return singleInstance.spenders.size
            else
                return 0
        }
        fun getActiveCount(): Int {
            if (::singleInstance.isInitialized) {
                var ctr = 0
                singleInstance.spenders.forEach {
                    if (it.isActive == 1)
                        ctr++
                }
                return ctr
            } else
                return 0
        }

        fun singleUser(): Boolean {
            return getActiveCount() == 1
        }

        fun deleteSpender(iSpender: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(iSpender).removeValue()
/*            val expe =
                getExpenditure(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = expenditures.indexOf(expe)
            expenditures.removeAt(ind)
  */      }
        fun addSpender(spender: Spender) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(spender.name).child("email").setValue(spender.email)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(spender.name).child("isactive").setValue(spender.isActive)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(spender.name).child("split").setValue(spender.split)
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
//            singleInstance.dataUpdatedCallback = null
            singleInstance.spenders.clear()
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

    fun setCallback(iCallback: SpenderDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
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
                    val name = it.key.toString()
                    var email = ""
                    var isActive = 1
                    var split = 0
                    it.children.forEach() {
                        when (it.key.toString()) {
                            "email" -> email = it.value.toString()
                            "isactive" -> isActive = it.value.toString().toInt()
                            "split" -> split = it.value.toString().toInt()
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
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").addValueEventListener(
            spenderListener as ValueEventListener
        )
    }
}

interface SpenderDataUpdatedCallback  {
    fun onDataUpdate()
}