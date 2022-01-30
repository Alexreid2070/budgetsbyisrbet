package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Category(var categoryName: String, var subcategoryName: String, var discType: String)

class CategoryViewModel : ViewModel() {
    private var catListener: ValueEventListener? = null
    private val categories: MutableList<Category> = ArrayList()
    var dataUpdatedCallback: CategoryDataUpdatedCallback? = null

    companion object {
        lateinit var singleInstance: CategoryViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.categories.forEach {
                Log.d("Alex", "ShowMe Category is " + it.categoryName + " subcategory is " + it.subcategoryName + " disc type is " + it.discType)
            }
        }

        fun getCount() : Int {
            return if (::singleInstance.isInitialized)
                singleInstance.categories.size
            else
                0
        }

        fun getDiscretionaryIndicator(iCategory: String, iSubcategory: String): String {
            val cat: Category? = singleInstance.categories.find { it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            return cat?.discType ?: ""
        }

        fun updateCategory(iCategory: String, iSubcategory: String, iDisctype: String) {
            val cat: Category? = singleInstance.categories.find { it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            if (cat == null) {
                addCategoryAndSubcategory(iCategory, iSubcategory, iDisctype)
            } else {
                cat.discType = iDisctype
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).setValue(iDisctype)
            }
        }

        fun deleteCategoryAndSubcategory(iCategory: String, iSubcategory: String) {
            // this block below ensures that the viewAll view is updated immediately
            val cat: Category? = singleInstance.categories.find { it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            val ind = singleInstance.categories.indexOf(cat)
            singleInstance.categories.removeAt(ind)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).removeValue()
        }

        fun addCategoryAndSubcategory(iCategory: String, iSubcategory: String, iDisctype: String) {
            // I need to add the new cat to the internal list so that the Adapter can be updated immediately, rather than waiting for the firebase sync.
            val cat = Category(iCategory, iSubcategory, iDisctype)
            singleInstance.categories.add(cat)
            singleInstance.categories.sortWith(compareBy({ it.categoryName }, { it.subcategoryName }))
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).setValue(iDisctype)
        }

        fun getCategories(): MutableList<Category> { // returns only "on" categories
            val tmpList: MutableList<Category> = ArrayList()
            for (category in singleInstance.categories) {
                if (category.discType != cDiscTypeOff)
                    tmpList.add(category)
            }
            return tmpList
        }

        fun getCategoriesIncludingOff(): MutableList<Category> {
            var tList: MutableList<Category>  = ArrayList()

            singleInstance.categories.forEach {
                tList.add(Category(it.categoryName, it.subcategoryName, it.discType))
            }
            return tList
        }

        fun getCategoryNames(): MutableList<String> {
            val tmpList: MutableList<String> = ArrayList()
            var prevName = ""
            singleInstance.categories.forEach {
                if (it.categoryName != prevName && it.discType != cDiscTypeOff) {
                    tmpList.add(it.categoryName)
                    prevName = it.categoryName
                }
            }
            return tmpList
        }

        fun getCategoryAndSubcategoryList(): MutableList<String> {
            val tmpList: MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if (it.discType != cDiscTypeOff)
                    tmpList.add(it.categoryName + "-" + it.subcategoryName)
            }
            return tmpList
        }

        fun getCombinedCategoriesForSpinner() : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if (it.discType != "Off")
                    list.add(it.categoryName + "-" + it.subcategoryName)
            }
            return list
        }
        fun getSubcategoriesForSpinner(iCategory: String) : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if (it.categoryName == iCategory && it.discType != cDiscTypeOff)
                    list.add(it.subcategoryName)
            }
            return list
        }

        fun setDiscType(iCategory: String, iSubcategory: String, iDisctype: String) {
            val myCat = singleInstance.categories.find{ it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            if (myCat != null) {
                myCat.discType = iDisctype
            }
        }

        fun refresh() {
            singleInstance.loadCategories()
        }
        fun clear() {
            if (singleInstance.catListener != null) {
                MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Category")
                    .removeEventListener(singleInstance.catListener!!)
                singleInstance.catListener = null
            }
//            singleInstance.dataUpdatedCallback = null
            singleInstance.categories.clear()
        }
    }
    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (catListener != null) {
            MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/Category")
                .removeEventListener(catListener!!)
            catListener = null
        }
    }

    fun setCallback(iCallback: CategoryDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadCategories() {
        // Do an asynchronous operation to fetch categories and subcategories
        Log.d("Alex", "in loadCategories for categories " + if (dataUpdatedCallback == null) "no callback " else "callback exists" )
        catListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    categories.clear()
                    dataSnapshot.children.forEach()
                    {
                        val myC: String = it.key.toString()
                        it.children.forEach {
                            categories.add(Category(myC, it.key.toString(), it.value.toString()))
                        }
                    }
                    dataUpdatedCallback?.onDataUpdate()
                } else { // first time user
                    MyApplication.database.getReference("Users/"+MyApplication.userUID)
                        .child("Info")
                        .child("Email")
                        .setValue(MyApplication.userEmail)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").addValueEventListener(
            catListener as ValueEventListener
        )
    }
}

interface CategoryDataUpdatedCallback  {
    fun onDataUpdate()
}