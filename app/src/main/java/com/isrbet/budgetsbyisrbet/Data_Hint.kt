package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
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
    private val hints: MutableList<Hint> = ArrayList()
    private val hintsLastShown: MutableList<HintLastShown> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: HintViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.hints.forEach {
                Log.d("Alex", "SM Hint is " + it.fragment + " " + it.id + " " + it.text)
            }
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun refresh() {
            singleInstance.loadHints()
        }
        fun clear() {
            singleInstance.hints.clear()
            singleInstance.loaded = false
        }
        @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "InflateParams")
        fun showHint(iContext: Context, iView: View, iFragment: String) {
            Log.d("Alex", "Showing $iFragment hint")
            var hls = singleInstance.hintsLastShown.find {it.fragment == iFragment}
            if (hls == null)
                hls = HintLastShown(iFragment, -1, "1999-01-01")
            val tHint = getNextHint(hls)
            if (tHint != null) {
/*                AlertDialog.Builder(iContext)
                    .setTitle("Hint!")
                    .setMessage(tHint.text)
                    .setPositiveButton(android.R.string.ok) { _, _ ->  }
                    .show() */

                val inflater = LayoutInflater.from(iContext)
                val popupView = inflater?.inflate(R.layout.hint_layout, null)
                val tv = popupView?.findViewById(R.id.tooltip_text) as TextView
                tv.text = tHint.text
                val width = LinearLayout.LayoutParams.WRAP_CONTENT
                val height = LinearLayout.LayoutParams.WRAP_CONTENT
                val focusable = true // lets taps outside the popup also dismiss it
                val popupWindow = PopupWindow(popupView, width, height, focusable)

                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window token
                popupWindow.showAtLocation(iView, Gravity.CENTER, 0, 0)

                // dismiss the popup window when touched
                popupView.setOnTouchListener { _, _ ->
                    popupWindow.dismiss()
                    true
                }
                MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                    .child("Info")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("Hints")
                    .child(iFragment)
                    .child("LastShownID")
                    .setValue(tHint.id)
                hls.id = tHint.id
                val dateNow = android.icu.util.Calendar.getInstance()
                MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                    .child("Info")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("Hints")
                    .child(iFragment)
                    .child("LastShownDate")
                    .setValue(giveMeMyDateFormat(dateNow))
                hls.date = giveMeMyDateFormat(dateNow)
            }
        }
        private fun getNextHint(iHintLastShown: HintLastShown) : Hint? {
            singleInstance.hints.forEach {
                if (it.fragment == iHintLastShown.fragment &&
                        it.id > iHintLastShown.id) {
                    return it
                }
            }
            return null
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
        // Do an asynchronous operation to fetch chats
        val listener = object : ValueEventListener {
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
                MyApplication.displayToast("User authorization failed 120.")
            }
        }
        MyApplication.databaseref.child("Hints")
                .addListenerForSingleValueEvent(listener)
        loadUserHintInfo()
    }

    private fun loadUserHintInfo() {
        val hintListener = object : ValueEventListener {
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
                MyApplication.displayToast("User authorization failed 112.")
            }
        }
        MyApplication.databaseref.child("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("Hints")
            .addListenerForSingleValueEvent(hintListener)
    }

    fun sortYourself() {
        hints.sortWith(compareBy({ it.fragment }, { it.id }))
    }
}
