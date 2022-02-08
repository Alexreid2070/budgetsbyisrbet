package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class Translation(var before: String, var after: String, var key: String)
data class TranslationOut(var before: String, var after: String) {
    constructor(t: Translation) : this(t.before, t.after)
}

class TranslationViewModel : ViewModel() {
    private var transListener: ValueEventListener? = null
    private val translations: MutableList<Translation> = ArrayList()
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: TranslationViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun showMe() {
            singleInstance.translations.forEach {
                Log.d("Alex", "Translation is '" + it.before + "' to '" + it.after + "'")
            }
        }
        fun getTranslation(iBefore: String): String {
            if (::singleInstance.isInitialized) {
                singleInstance.translations.forEach {
                    if (it.before == iBefore)
                        return it.after
                }
            }
            return iBefore
        }
        fun deleteTranslation(iBefore: String) {
            if (::singleInstance.isInitialized) {
                singleInstance.translations.forEach {
                    if (it.before == iBefore) {
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").child(it.key).removeValue()
                    }
                }
            }
        }
        fun addTranslation(iBefore: String, iAfter: String) {
            val trans = TranslationOut(iBefore, iAfter)
            Log.d("Alex", "Adding translation $iBefore $iAfter")
            val key = MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").push().key.toString()
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").child(key).setValue(trans)
        }
        fun refresh() {
            singleInstance.loadTranslations()
        }
        fun clear() {
            if (singleInstance.transListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Translation")
                    .removeEventListener(singleInstance.transListener!!)
                singleInstance.transListener = null
            }
            singleInstance.translations.clear()
            singleInstance.loaded = false
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (transListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Translation")
                .removeEventListener(transListener!!)
            transListener = null
        }
    }

    fun loadTranslations() {
        // Do an asynchronous operation to fetch spenders
        transListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                translations.clear()
                dataSnapshot.children.forEach()
                {
                    val mKey = it.key.toString()
                    var before = ""
                    var after = ""
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "before" -> before = child.value.toString()
                            "after" -> after = child.value.toString()
                        }
                    }
                    translations.add(Translation(before, after, mKey))
                }
                singleInstance.loaded = true
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").addValueEventListener(
            transListener as ValueEventListener
        )
    }
}