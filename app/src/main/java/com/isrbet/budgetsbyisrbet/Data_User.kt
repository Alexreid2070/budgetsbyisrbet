package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class AppUser(var email: String, var uid: String)

class AppUserViewModel : ViewModel() {
    lateinit var userListener: ValueEventListener
    private val users: MutableList<AppUser> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: AppUserViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.users.forEach {
                Log.d("Alex", "SM User is " + it.email + " " + it.uid)
            }
        }
        fun getUserEmail(pos:Int): String {
            return if (pos  < singleInstance.users.size)
                singleInstance.users[pos].email
            else
                ""
        }
        fun getUserUID(pos:Int): String {
            return if (pos  < singleInstance.users.size)
                singleInstance.users[pos].uid
            else
                ""
        }

        fun getCount(): Int {
            return if (::singleInstance.isInitialized)
                singleInstance.users.size
            else
                0
        }

        fun setCallback(iCallback: DataUpdatedCallback?) {
            singleInstance.dataUpdatedCallback = iCallback
//            singleInstance.dataUpdatedCallback?.onDataUpdate()
        }
        fun clearCallback() {
            singleInstance.dataUpdatedCallback = null
        }
        fun loadUsers() {
            // Do an asynchronous operation to fetch spenders
            singleInstance.userListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    singleInstance.users.clear()
                    for (element in dataSnapshot.children.toMutableList()) {
                        val uid = element.key.toString()
                        var email = ""
                        for (child in element.children) {
                            if (child.key.toString() == "Info") {
                                for ( nChild in child.children) {
                                    if (nChild.key.toString().toInt() == SpenderViewModel.myIndex()) {
                                        for (pChild in nChild.children) {
                                            if (pChild.key.toString() == "Email")
                                                email = pChild.value.toString()
                                        }
                                    }
                                }
                            }
                        }
                        singleInstance.users.add(AppUser(email, uid))
                    }
                    singleInstance.dataUpdatedCallback?.onDataUpdate()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    MyApplication.displayToast("User authorization failed 101.")
                }
            }
            MyApplication.database.getReference("Users").addValueEventListener(singleInstance.userListener)
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (::userListener.isInitialized)
            MyApplication.databaseref.child("Users")
                .removeEventListener(userListener)
    }
    fun clearCallback() {
        dataUpdatedCallback = null
    }
}