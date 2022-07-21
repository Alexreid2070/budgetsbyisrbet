@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


data class Hint(
    var fragment: String = "",
    var id: Int = 0,
    var text: String = ""
) {
    fun setValue(key: String, value: String) {
        when (key) {
            "fragment" -> fragment = value.trim()
            "id" -> id = value.toInt()
            "text" -> text = value.trim()
        }
    }
}

data class HintLastShown(val fragment: String, var id: Int, var date: String)

class HintViewModel : ViewModel() {
    private var hintListener: ValueEventListener? = null
    private val hints: MutableList<Hint> = ArrayList()
    private val hintsLastShown: MutableList<HintLastShown> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: HintViewModel // used to track static single instance of self
/*        fun showMe() {
            singleInstance.hints.forEach {
                Log.d("Alex", "SM Hint is " + it.fragment + " " + it.id + " " + it.text)
            }
        } */

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun refresh() {
            singleInstance.loadHints()
        }
        fun clear() {
            if (singleInstance.hintListener != null) {
                MyApplication.databaseref.child("Hints")
                    .removeEventListener(singleInstance.hintListener!!)
                singleInstance.hintListener = null
            }
            singleInstance.hints.clear()
            singleInstance.loaded = false
        }
        @SuppressLint("ClickableViewAccessibility", "InflateParams")
        fun showHint(iParentFragmentManager: FragmentManager, iFragment: String) {
                val hdf = HintDialogFragment.newInstance(iFragment)
                hdf.show(iParentFragmentManager, MyApplication.getString(R.string.show_hint))
        }

        fun getNextHint(iFragment: String, iStartPosition: Int = -1) : Hint? {
            var hls = singleInstance.hintsLastShown.find {it.fragment == iFragment}
            if (hls == null)
                hls = HintLastShown(iFragment, -1, "1999-01-01")
            val startPosition = if (iStartPosition == -1) hls.id else iStartPosition

            singleInstance.hints.forEach {
                if (it.fragment == iFragment &&
                    it.id > startPosition) {
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Hints")
                        .child(iFragment)
                        .child("LastShownID")
                        .setValue(it.id)
                    val dateNow = android.icu.util.Calendar.getInstance()
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Hints")
                        .child(iFragment)
                        .child("LastShownDate")
                        .setValue(giveMeMyDateFormat(dateNow))
                    hls.id = it.id
                    hls.date = giveMeMyDateFormat(dateNow)
                    singleInstance.hintsLastShown.add(hls)
                    return it
                }
            }
            return null
        }
        fun getPreviousHint(iFragment: String, iStartPosition: Int = 99999) : Hint? {
            var hls = singleInstance.hintsLastShown.find {it.fragment == iFragment}
            if (hls == null)
                hls = HintLastShown(iFragment, 99999, "1999-01-01")
            val startPosition = if (iStartPosition == 99999) hls.id else iStartPosition

            singleInstance.hints.asReversed().forEach {
                if (it.fragment == iFragment &&
                    it.id < startPosition) {
                    Log.d("Alex", "returning ${it.id}")
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Hints")
                        .child(iFragment)
                        .child("LastShownID")
                        .setValue(it.id)
                    val dateNow = android.icu.util.Calendar.getInstance()
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Hints")
                        .child(iFragment)
                        .child("LastShownDate")
                        .setValue(giveMeMyDateFormat(dateNow))
                    hls.id = it.id
                    hls.date = giveMeMyDateFormat(dateNow)
                    singleInstance.hintsLastShown.add(hls)
                    return it
                }
            }
            return null
        }
        fun restartHints() {
            singleInstance.hintsLastShown.clear()
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child(SpenderViewModel.myIndex().toString())
                .child("Hints")
                .removeValue()
        }
    }

    init {
        singleInstance = this
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadHints() {
        // Do an asynchronous operation to fetch hints
        hintListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                hints.clear()
                for (element in dataSnapshot.children.toMutableList()) { // for each fragment
                    val tFragmentName = element.key.toString()
                    for (child in element.children) {
                        hints.add(Hint(tFragmentName, child.key.toString().toInt(), child.value.toString()))
                    }
                }
                sortYourself()
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 120.")
            }
        }
        MyApplication.database.getReference("Hints").addValueEventListener(
            hintListener as ValueEventListener
        )
        loadUserHintInfo()
    }

    private fun loadUserHintInfo() {
        val userHintListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (element in dataSnapshot.children.toMutableList()) { // for each fragment hint grouping
                    val tFragmentName = element.key.toString()
                    var lastShownDate = "1999-01-01"
                    var lastShownID = -1
                    for (child in element.children) {
                        when (child.key.toString()) {
                            "LastShownDate" -> lastShownDate = child.value.toString()
                            "LastShownID" -> lastShownID = child.value.toString().toInt()
                        }
                    }
                    hintsLastShown.add(HintLastShown(tFragmentName, lastShownID, lastShownDate))
                }
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 112.")
            }
        }
        MyApplication.databaseref.child("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("Hints")
            .addListenerForSingleValueEvent(userHintListener)
    }

    fun sortYourself() {
        hints.sortWith(compareBy({ it.fragment }, { it.id }))
    }
}
