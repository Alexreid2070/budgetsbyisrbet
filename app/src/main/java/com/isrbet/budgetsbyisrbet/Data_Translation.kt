@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class Translation(var before: String, var after: String,
                       var category: Int, var key: String) {
    fun contains(iSubString: String): Boolean {
        val lc = iSubString.lowercase()
        return before.lowercase().contains(lc) ||
                after.lowercase().contains(lc) ||
                CategoryViewModel.getFullCategoryName(category).lowercase().contains(lc)
    }
}
data class TranslationOut(var before: String, var after: String, var category: Int)

class TranslationViewModel : ViewModel() {
    private var transListener: ValueEventListener? = null
    private val translations: MutableList<Translation> = ArrayList()
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: TranslationViewModel // used to track static single instance of self
        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getTranslations():MutableList<Translation> {
            val copy = mutableListOf<Translation>()
            copy.addAll(singleInstance.translations)
            return copy
        }

/*        fun showMe() {
            singleInstance.translations.forEach {
                Log.d("Alex", "Translation is '" + it.before + "' to '" + it.after + "'")
            }
        }
        fun exists(iString: String): Boolean {
            if (::singleInstance.isInitialized) {
                singleInstance.translations.forEach {
                    if (it.before == iString)
                        return true
                }
            }
            return false
        } */
        fun getTranslation(iBefore: String): Translation? {
            if (::singleInstance.isInitialized) {
                singleInstance.translations.forEach {
                    if (it.before == iBefore)
                        return it
                }
                // then look for translations where the before text includes after text
                singleInstance.translations.forEach {
                    if (iBefore.contains(it.after, true)) // 'before' string contains an after
                        return it
                }
            }
            return null
        }
        fun deleteTranslation(iKey: String) {
            if (::singleInstance.isInitialized) {
                val tr: Translation? = singleInstance.translations.find { it.key == iKey }
                val ind = singleInstance.translations.indexOf(tr)
                singleInstance.translations.removeAt(ind)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").child(iKey).removeValue()
                }
        }
        fun updateTranslation(iKey: String, iBefore: String, iAfter: String, iCategory: Int) {
            if (::singleInstance.isInitialized) {
                if (iCategory == 0)
                    MyApplication.displayToast("Why is category 0 when updating Translation?")
                singleInstance.translations.forEach {
                    if (it.key == iKey || iBefore == it.before) {
                        it.after = iAfter
                        it.category = iCategory
                        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Translation")
                            .child(it.key).child("after").setValue(iAfter)
                        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Translation")
                            .child(it.key).child("category").setValue(iCategory)
                        return
                    }
                }
                // didn't find it, so add it
                val trans = TranslationOut(iBefore, iAfter, iCategory)
                val key = MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").push().key.toString()
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").child(key).setValue(trans)
            }
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

    fun clearCallback() {
        loaded = false
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
                    var category = 0
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "before" -> before = child.value.toString()
                            "after" -> after = child.value.toString()
                            "category" -> category = child.value.toString().toInt()
                        }
                    }
                    translations.add(Translation(before, after, category, mKey))
                }
                singleInstance.loaded = true
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 111.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Translation").addValueEventListener(
            transListener as ValueEventListener
        )
    }
}