package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Spender(var name: String, var split: Int) {
}

class SpenderViewModel : ViewModel() {
    private var spenderListener: ValueEventListener? = null
    private val spenders: MutableList<Spender> = ArrayList()
    var dataUpdatedCallback: SpenderDataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: SpenderViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.spenders.forEach {
                Log.d("Alex", "SM Spender is " + it.name + " and % is " + it.split)
            }
        }
        fun getSpender(pos:Int): Spender? {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos]
            else
                return null
        }
        fun getSpenderName(pos:Int): String {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].name
            else
                return ""
        }
        fun getSpenderSplit(pos:Int): Int {
            if (pos  < singleInstance.spenders.size)
                return singleInstance.spenders[pos].split
            else
                return 0
        }
        fun getSpender(iName:String): Spender? {
            singleInstance.spenders.forEach {
                if (it.name == iName)
                    return it
            }
            return null
        }

        fun getSpenders() : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.spenders.forEach {
                list.add(it.name)
            }
            return list
        }


        fun getCount(): Int {
            if (::singleInstance.isInitialized)
                return singleInstance.spenders.size
            else
                return 0
        }

        fun deleteSpender(iSpender: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(iSpender).removeValue()
/*            val expe =
                getExpenditure(iTransactionID) // this block below ensures that the viewAll view is updated immediately
            val ind = expenditures.indexOf(expe)
            expenditures.removeAt(ind)
  */      }
        fun addSpender(spender: Spender) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Spender").child(spender.name).setValue(spender.split)
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
            singleInstance.dataUpdatedCallback = null
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
        dataUpdatedCallback?.onDataUpdate()
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
                    spenders.add(Spender(it.key.toString(), it.value.toString().toInt()))
                }
                if (spenders.size > 1)
                    spenders.add(Spender("Joint", 100))
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