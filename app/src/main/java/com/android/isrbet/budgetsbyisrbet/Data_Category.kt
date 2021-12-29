package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Category(var categoryName: String, var subcategoryName: String, var discType: String) {
}

class CategoryViewModel : ViewModel() {
    lateinit var catListener: ValueEventListener
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
            if (::singleInstance.isInitialized)
                return singleInstance.categories.size
            else
                return 0
        }

        fun getDiscretionaryIndicator(iCategory: String, iSubcategory: String): String {
            val cat: Category? = singleInstance.categories.find { it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            if (cat != null)
                return cat.discType
            else
                return ""
        }

        fun deleteCategoryAndSubcategory(iCategory: String, iSubcategory: String) {
            // this block below ensures that the viewAll view is updated immediately
            val cat: Category? = CategoryViewModel.singleInstance.categories.find { it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            val ind = CategoryViewModel.singleInstance.categories.indexOf(cat)
            CategoryViewModel.singleInstance.categories.removeAt(ind)
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).removeValue()
        }

        fun addCategoryAndSubcategory(iCategory: String, iSubcategory: String, iDisctype: String) {
            // I need to add the new cat to the internal list so that the Adapter can be updated immediately, rather than waiting for the firebase sync.
            val cat = Category(iCategory, iSubcategory, iDisctype)
            CategoryViewModel.singleInstance.categories.add(cat)
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
            return singleInstance.categories
        }

        fun getCategoryNames(): MutableList<String> {
            val tmpList: MutableList<String> = ArrayList()
            var prevName: String = ""
            singleInstance.categories.forEach {
                if (it.categoryName != prevName) {
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

        fun getSubcategoriesForSpinner(iCategory: String) : MutableList<String> {
            var list : MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if (it.categoryName == iCategory && it.discType != cDiscTypeOff)
                    list.add(it.subcategoryName)
            }
            return list
        }

        fun setDiscType(iCategory: String, iSubcategory: String, iDisctype: String) {
            var myCat = singleInstance.categories.find{ it.categoryName == iCategory && it.subcategoryName == iSubcategory }
            if (myCat != null) {
                myCat.discType = iDisctype
            }
        }

        fun deleteCategory(iCategory: String, iSubcategory: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).removeValue()
        }

        fun updateCategory(iCategory: String, iSubcategory: String, iDisctype: String) {
            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").child(iCategory).child(iSubcategory).setValue(iDisctype)
        }
        fun refresh() {
            singleInstance.loadCategories()
        }
    }
    init {
        singleInstance = this
        Log.d("Alex", "assigning singleInstance Category")
    }

    override fun onCleared() {
        super.onCleared()
        MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Category")
            .removeEventListener(catListener)
    }

    fun setCallback(iCallback: CategoryDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadCategories() {
        // Do an asynchronous operation to fetch categories and subcategories
        Log.d("Alex", "in loadCategories for categories")
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
                    MyApplication.database.getReference("Users/"+MyApplication.userUID).child("Email").setValue(MyApplication.userEmail)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").addValueEventListener(catListener)
    }
}

public interface CategoryDataUpdatedCallback  {
    fun onDataUpdate()
}