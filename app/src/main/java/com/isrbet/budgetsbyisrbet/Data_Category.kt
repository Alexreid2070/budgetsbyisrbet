package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Category(var id: Int, var categoryName: String, var subcategoryName: String, var discType: String = "",
    var private: Int = 2) {
    var priority = 0
    constructor(id: Int, iFullCategoryName: String) : this(id, iFullCategoryName, iFullCategoryName) {
        val dash = iFullCategoryName.indexOf("-")
        try {
            categoryName = iFullCategoryName.substring(0, dash)
            subcategoryName = iFullCategoryName.substring(dash + 1, iFullCategoryName.length)
            this.id = CategoryViewModel.getID(categoryName, subcategoryName)
        }
        catch (exception: Exception) {
            Log.d("Alex", "caught an exception in Category constructor (missing dash) $iFullCategoryName")
        }
    }
    fun fullCategoryName() : String {
        return if (id == cTRANSFER_CODE)
            "Transfer"
        else
            "$categoryName-$subcategoryName"
    }
    fun iAmAllowedToSeeThisCategory() : Boolean {
        return (private == 2 ||
                private == MyApplication.userIndex)
    }
}

class CategoryViewModel : ViewModel() {
    private var catListener: ValueEventListener? = null
    private val categories: MutableList<Category> = ArrayList()
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: CategoryViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.categories.forEach {
                Log.d("Alex", "ShowMe Category is ${it.id} " + it.categoryName + " subcategory is " + it.subcategoryName + " disc type is " + it.discType)
            }
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getCount() : Int {
            return if (::singleInstance.isInitialized)
                singleInstance.categories.size
            else
                0
        }

        fun getID(iCategory: String, iSubcategory: String): Int {
            if (iCategory == "Transfer")
                return cTRANSFER_CODE

            val cat: Category? = singleInstance.categories.find { it.categoryName.lowercase().trim() == iCategory.lowercase().trim()
                    && it.subcategoryName.lowercase().trim() == iSubcategory.lowercase().trim() }
            return cat?.id ?: 0
        }
        fun getCategory(id: Int): Category? {
            if (id == cTRANSFER_CODE)
                return Category(cTRANSFER_CODE, "Transfer", "", "Discretionary", 2)
            return singleInstance.categories.find { it.id == id }
        }
        fun getDefaultCategory(): Category? {
            val id = DefaultsViewModel.getDefault(cDEFAULT_CATEGORY_ID).toInt()
            return singleInstance.categories.find {it.id == id}
        }

        private fun getNextID(): Int {
            var maxID = 1000
            singleInstance.categories.forEach {
                if (it.id > maxID)
                    maxID = it.id
            }
            return (maxID+1)
        }

        fun isThereAtLeastOneCategoryThatIAmNotAllowedToSee(): Boolean {
            singleInstance.categories.forEach {
                if (!it.iAmAllowedToSeeThisCategory())
                    return true
            }
            return false
        }

        fun updateCategory(id: Int, iCategory: String, iSubcategory: String, iDisctype: String,
                           iPrivate: Int, iLocalOnly: Boolean = false): Category {
            var cat: Category? = singleInstance.categories.find { it.id == id }
            if (cat == null) {
                cat = Category(id, iCategory, iSubcategory, iDisctype, iPrivate)
                cat.id = getNextID()
                singleInstance.categories.add(cat)
                singleInstance.categories.sortWith(compareBy({ it.categoryName }, { it.subcategoryName }))
            } else {
                cat.categoryName = iCategory
                cat.subcategoryName = iSubcategory
                cat.discType = iDisctype
                cat.private = iPrivate
            }
            if (!iLocalOnly) {
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                    .child(cat.id.toString())
                    .child("Category")
                    .setValue(iCategory)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                    .child(cat.id.toString())
                    .child("SubCategory")
                    .setValue(iSubcategory)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                    .child(cat.id.toString())
                    .child("Type")
                    .setValue(iDisctype)
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                    .child(cat.id.toString())
                    .child("Private")
                    .setValue(iPrivate)
            }
            return cat
        }
        fun getFullCategoryName(id: Int): String {
            return if (id == cTRANSFER_CODE)
                "Transfer"
            else {
                val cat: Category? = singleInstance.categories.find { it.id == id }
                cat?.fullCategoryName() ?: ""
            }
        }
        fun deleteCategoryAndSubcategory(id: Int) {
            val cat: Category? = singleInstance.categories.find { it.id == id }
            if (cat != null) {
                val ind = singleInstance.categories.indexOf(cat)
                singleInstance.categories.removeAt(ind)
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Category")
                    .child(id.toString()).removeValue()
                val anyMoreCats: Category? = singleInstance.categories.find { it.categoryName == cat.categoryName }
                if (anyMoreCats == null) {
                    Log.d("Alex", "Just deleted the last category so clean up CategoryDetails too")
                    DefaultsViewModel.deleteCategoryDetail(cat.categoryName)
                }
            }
        }

        fun getCategories(includingOff: Boolean): MutableList<Category> {
            val tList: MutableList<Category>  = ArrayList()

            singleInstance.categories.forEach {
                if ((it.discType != cDiscTypeOff || includingOff) &&
                    it.iAmAllowedToSeeThisCategory()) {
                    tList.add(Category(it.id, it.categoryName, it.subcategoryName, it.discType, it.private))
                    val cat = tList[tList.size - 1]
                    cat.priority = DefaultsViewModel.getCategoryDetail(cat.categoryName).priority
                }
            }
            tList.sortWith(compareBy({ it.priority }, { it.categoryName }))
            return tList
        }

        fun getCategoryNames(iIncludeOff: Boolean = false): MutableList<String> {
            val tmpList: MutableList<String> = ArrayList()
            var prevName = ""
            val origList = getCategories(true)
            origList.forEach {
                if (it.categoryName != prevName && (iIncludeOff || it.discType != cDiscTypeOff)) {
                    tmpList.add(it.categoryName)
                    prevName = it.categoryName
                }
            }
            return tmpList
        }

        fun getCategoryAndSubcategoryList(): MutableList<String> {
            val tmpList: MutableList<String> = ArrayList()
            val origList = getCategories(true)
//            singleInstance.categories.forEach {
            origList.forEach {
                if (it.discType != cDiscTypeOff)
                    tmpList.add(it.fullCategoryName())
            }
            return tmpList
        }

        fun getCombinedCategoriesForSpinner() : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            val origList = getCategories(true)
//            singleInstance.categories.forEach {
            origList.forEach {
//            singleInstance.categories.forEach {
//                if (it.discType != cDiscTypeOff)
                    list.add(it.fullCategoryName())
            }
            return list
        }
        fun getSubcategoriesForSpinner(iCategory: String) : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if ((it.categoryName == iCategory && it.discType != cDiscTypeOff) &&
                        it.iAmAllowedToSeeThisCategory())
                    list.add(it.subcategoryName)
            }
            return list
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
            singleInstance.loaded = false
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

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
//        dataUpdatedCallback?.onDataUpdate()
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadCategories() {
        // Do an asynchronous operation to fetch categories and subcategories
        catListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                categories.clear()
                if (dataSnapshot.exists()) {
                    dataSnapshot.children.forEach()
                    {
                        val categoryID = it.key.toString().toInt()
                        var category = ""
                        var subcategory = ""
                        var disctype = ""
                        var private = 2
                        for (child in it.children) {
                            when (child.key.toString()) {
                                "Category" -> category = child.value.toString().trim()
                                "SubCategory" -> subcategory = child.value.toString().trim()
                                "Type" -> disctype = child.value.toString().trim()
                                "Private" -> {
                                    private = child.value.toString().toInt()
                                }
                            }
                        }
                        categories.add(Category(categoryID, category, subcategory, disctype, private))
                    }
                } else { // first time user
                    MyApplication.database.getReference("Users/"+MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Email")
                        .setValue(MyApplication.userEmail)
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
                categories.sortWith(compareBy({ it.categoryName }, { it.subcategoryName }))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast("User authorization failed 105.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").addValueEventListener(
            catListener as ValueEventListener
        )
    }
}

data class CategoryDetail(var name: String, var color: Int, var priority: Int)
