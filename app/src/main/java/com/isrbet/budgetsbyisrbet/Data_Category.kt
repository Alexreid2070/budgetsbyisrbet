@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.util.ArrayList

data class Category(var id: Int, var categoryName: String, var subcategoryName: String,
                    var discType: String = "", var private: Int = 2,
                    var inUse: Boolean = true) {
    var priority = 0
    constructor(id: Int, iFullCategoryName: String) : this(id, iFullCategoryName, iFullCategoryName) {
        val dash = iFullCategoryName.indexOf("-")
        try {
            categoryName = iFullCategoryName.substring(0, dash)
            subcategoryName = iFullCategoryName.substring(dash + 1, iFullCategoryName.length)
            this.id = CategoryViewModel.getID(categoryName, subcategoryName)
            val cat = CategoryViewModel.getCategory(this.id)
            this.discType = cat?.discType.toString()
            this.private = cat?.private!!
            this.inUse = cat.inUse == true
        }
        catch (exception: Exception) {
            Timber.tag("Alex").d("caught an exception in Category constructor (missing dash) $iFullCategoryName")
        }
    }
    fun fullCategoryName() : String {
        return if (id == cTRANSFER_CODE)
            MyApplication.getString(R.string.transfer)
        else
            "$categoryName-$subcategoryName"
    }
    fun iAmAllowedToSeeThisCategory() : Boolean {
        return (private == 2 ||
                private == MyApplication.userIndex)
    }
    fun out() : CategoryOut {
        return CategoryOut(categoryName, subcategoryName, discType, private, inUse)
    }
}

data class CategoryOut(var Category: String, var SubCategory: String,
                        var Type: String, var Private: Int,
                        var State: Boolean)


data class CategoryDetail(var name: String, var color: Int, var priority: Int)

class CategoryViewModel : ViewModel() {
    private var catListener: ValueEventListener? = null
    private val categories: MutableList<Category> = ArrayList()
    val categoriesLiveData = MutableLiveData<MutableList<Category>>()
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: CategoryViewModel // used to track static single instance of self

        fun observeList(iFragment: Fragment, iObserver: androidx.lifecycle.Observer<MutableList<Category>>) {
            singleInstance.categoriesLiveData.observe(iFragment, iObserver)
        }
        fun isLoaded():Boolean {
            return if (this::singleInstance.isInitialized) {
                singleInstance.loaded
            } else
                false
        }

        fun getCount() : Int {
            return if (::singleInstance.isInitialized)
                singleInstance.categories.size
            else
                0
        }

        fun getID(iCategory: String, iSubcategory: String): Int {
            if (iCategory == MyApplication.getString(R.string.transfer))
                return cTRANSFER_CODE

            val cat: Category? = singleInstance.categories.find { it.categoryName.lowercase().trim() == iCategory.lowercase().trim()
                    && it.subcategoryName.lowercase().trim() == iSubcategory.lowercase().trim() }
            return cat?.id ?: 0
        }
        fun getCategory(id: Int): Category? {
            if (id == cTRANSFER_CODE)
                return Category(cTRANSFER_CODE, MyApplication.getString(R.string.transfer), "",
                    MyApplication.getString(R.string.discretionary), 2, true)
            return singleInstance.categories.find { it.id == id }
        }
        fun getCategory(fullName: String): Category? {
            val dash = fullName.indexOf("-")
            try {
                val categoryName = fullName.substring(0, dash)
                val subcategoryName = fullName.substring(dash + 1, fullName.length)
                val id = getID(categoryName, subcategoryName)
                return getCategory(id)
            }
            catch (exception: Exception) {
                Timber.tag("Alex").d("caught an exception in getCategory with $fullName")
            }
            return null
        }

        fun getCategoryPriority(id: Int): Int {
            val cat = singleInstance.categories.find { it.id == id }
            val cd = cat?.categoryName?.let { DefaultsViewModel.getCategoryDetail(it) }
            return cd?.priority ?: 99
        }
        fun getCategoryColour(id: Int): Int {
            val cat = singleInstance.categories.find { it.id == id }
            val cd = cat?.categoryName?.let { DefaultsViewModel.getCategoryDetail(it) }
            return cd?.color ?: 0
        }
        fun getDefaultCategory(): Category? {
            val id = DefaultsViewModel.getDefaultCategory()
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
        fun getFullCategoryName(id: Int): String {
            return if (id == cTRANSFER_CODE)
                MyApplication.getString(R.string.transfer)
            else {
                val cat: Category? = singleInstance.categories.find { it.id == id }
                cat?.fullCategoryName() ?: ""
            }
        }

        fun getCategories(includingOff: Boolean): MutableList<Category> {
            val tList: MutableList<Category>  = ArrayList()

            singleInstance.categories.forEach {
                if ((it.inUse || includingOff) &&
                    it.iAmAllowedToSeeThisCategory()) {
                    tList.add(Category(it.id, it.categoryName, it.subcategoryName, it.discType, it.private, it.inUse))
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
                if (it.categoryName != prevName && (iIncludeOff || it.inUse)) {
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
                if (it.inUse)
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
        fun getSubcategoriesForSpinner(iCategory: String, iSubCategory: String = "") : MutableList<String> {
            val list : MutableList<String> = ArrayList()
            singleInstance.categories.forEach {
                if (it.categoryName == iCategory && it.iAmAllowedToSeeThisCategory()) {
                    if (it.inUse || it.subcategoryName == iSubCategory) {
                        list.add(it.subcategoryName)
                    }
                }
            }
            return list
        }

        fun updateCategory(id: Int, iCategory: String, iSubcategory: String, iDisctype: String,
                           iPrivate: Int, iInUse: Boolean, iLocalOnly: Boolean = false): Category {
            var cat: Category? = singleInstance.categories.find { it.id == id }
            if (cat == null) {
                cat = Category(id, iCategory, iSubcategory, iDisctype, iPrivate, iInUse)
                cat.id = getNextID()
                if (iLocalOnly) {
                    singleInstance.categories.add(cat)
                    singleInstance.categories.sortWith(compareBy({ it.categoryName }, { it.subcategoryName }))
                }
            } else {
                cat.categoryName = iCategory
                cat.subcategoryName = iSubcategory
                cat.discType = iDisctype
                cat.private = iPrivate
                cat.inUse = iInUse
            }
            if (!iLocalOnly) {
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                    .child(cat.id.toString())
                    .setValue(cat.out())
            }
            return cat
        }
        fun deleteCategoryAndSubcategory(id: Int) {
            val cat: Category? = singleInstance.categories.find { it.id == id }
            if (cat != null) {
//                val ind = singleInstance.categories.indexOf(cat)
//                singleInstance.categories.removeAt(ind)
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Category")
                    .child(id.toString()).removeValue()
                val anyMoreCats: Category? = singleInstance.categories.find { it.categoryName == cat.categoryName }
                if (anyMoreCats == null) {
                    Timber.tag("Alex").d("Just deleted the last category so clean up CategoryDetails too")
                    DefaultsViewModel.deleteCategoryDetail(cat.categoryName)
                }
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
                        var inUse = ""
                        for (child in it.children) {
                            when (child.key.toString().lowercase()) {
                                "category" -> category = child.value.toString().trim()
                                "subcategory" -> subcategory = child.value.toString().trim()
                                "type" -> disctype = child.value.toString().trim()
                                "state" -> inUse = child.value.toString().lowercase().trim()
                                "private" -> private = child.value.toString().toInt()
                            }
                        }
                        categories.add(Category(categoryID, category, subcategory, disctype, private, inUse != cFALSE))
                    }
                } else { // first time user
                    MyApplication.database.getReference("Users/"+MyApplication.userUID)
                        .child("Info")
                        .child(SpenderViewModel.myIndex().toString())
                        .child("Email")
                        .setValue(MyApplication.userEmail)
                }
                singleInstance.loaded = true
                categories.sortWith(compareBy({ it.categoryName }, { it.subcategoryName }))
                singleInstance.categoriesLiveData.value = singleInstance.categories
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 105.")
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category").addValueEventListener(
            catListener as ValueEventListener
        )
    }
}
