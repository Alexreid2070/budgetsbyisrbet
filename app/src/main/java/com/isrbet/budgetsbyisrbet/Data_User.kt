@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class AppUser(var email: String, var uid: String, var primary: String, var secondary: String)

class AppUserViewModel : ViewModel() {
    lateinit var userListener: ValueEventListener
    private val users: MutableList<AppUser> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: AppUserViewModel // used to track static single instance of self
/*        fun showMe() {
            singleInstance.users.forEach {
                Log.d("Alex", "SM User is " + it.email + " " + it.uid)
            }
        } */
        fun getUsers(): MutableList<AppUser> {
            return singleInstance.users
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
                        var primary = ""
                        var secondary = ""
                        for (child in element.children) {
                            when (child.key.toString()) {
                                "Email" -> email = child.value.toString()
                                "Primary" -> primary = child.value.toString()
                                "Secondary" -> secondary = child.value.toString()
                            }
                        }
                        singleInstance.users.add(AppUser(email, uid, primary, secondary))
                    }
                    singleInstance.users.sortBy { it.email }
                    singleInstance.dataUpdatedCallback?.onDataUpdate()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 101.")
                }
            }
            MyApplication.database.getReference("Userkeys").addValueEventListener(singleInstance.userListener)
        }

        fun addUserKey() {
            MyApplication.database.getReference("Userkeys")
                .child(MyApplication.originalUserUID)
                .child("Email").setValue(MyApplication.userEmail)
        }
        fun addPrimary(iEmail: String) {
            MyApplication.database.getReference("Userkeys/" + MyApplication.originalUserUID)
                .child("Primary").setValue(iEmail)
        }
        fun addSecondary(iEmail: String) {
            MyApplication.database.getReference("Userkeys/" + MyApplication.originalUserUID)
                .child("Secondary").setValue(iEmail)
        }
        fun removePrimary() {
            MyApplication.database.getReference("Userkeys/" + MyApplication.originalUserUID)
                .child("Primary").removeValue()
        }
        fun removeSecondary() {
            MyApplication.database.getReference("Userkeys/" + MyApplication.originalUserUID)
                .child("Secondary").removeValue()
        }
        fun getPrimaryEmail(iUID: String) : String {
            singleInstance.users.forEach {
                if (it.uid == iUID)
                    return it.email
            }
            return ""
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (::userListener.isInitialized)
            MyApplication.databaseref.child("Userkeys")
                .removeEventListener(userListener)
    }
    fun clearCallback() {
        dataUpdatedCallback = null
    }
}