package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentAdminBinding

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.adminCurrentUser.text = MyApplication.currentUserEmail
        view?.findViewById<Button>(R.id.button_dosomething)?.setOnClickListener { _: View ->
            SpenderViewModel.clear()
            CategoryViewModel.clear()
            DefaultsViewModel.clear()
            BudgetViewModel.clear()
            ChatViewModel.clear()
            ExpenditureViewModel.clear()
            RecurringTransactionViewModel.clear()
            TranslationViewModel.clear()

            SpenderViewModel.refresh()
            //convertCategories()
            CategoryViewModel.refresh()
            DefaultsViewModel.refresh()
            //convertBudgets()
            //convertExpenditures()
            moveExpendituresToDateFolders()
            //convertRecurringTransactions()

//            RecurringTransactionViewModel.refresh()
//            BudgetViewModel.refresh()
//            ExpenditureViewModel.refresh()
//            ChatViewModel.refresh()
//            TranslationViewModel.refresh()
        }
        view?.findViewById<Button>(R.id.button_load_users)?.setOnClickListener { _: View ->
            clearData()
            UserViewModel.loadUsers()
            UserViewModel.setCallback(object: DataUpdatedCallback {
                override fun onDataUpdate() {
                    Log.d("Alex", "got a callback that user data was updated")
                    refreshData()
                }
            })
        }
        binding.adminUser1Uid.setOnClickListener {
            uidClicked(binding.adminUser1Uid.text.toString(), binding.adminUser1Email.text.toString())
        }
        binding.adminUser2Uid.setOnClickListener {
            uidClicked(binding.adminUser2Uid.text.toString(), binding.adminUser2Email.text.toString())
        }
        binding.adminUser3Uid.setOnClickListener {
            uidClicked(binding.adminUser3Uid.text.toString(), binding.adminUser3Email.text.toString())
        }
        binding.adminUser4Uid.setOnClickListener {
            uidClicked(binding.adminUser4Uid.text.toString(), binding.adminUser4Email.text.toString())
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }
    }
    private fun uidClicked(uid: String, email: String) {
        Toast.makeText(activity, "Switching to user $email", Toast.LENGTH_SHORT).show()
        MyApplication.currentUserEmail = email
        UserViewModel.clearCallback()
        Log.d("Alex", "I clicked uid $uid")
        switchTo(uid)
        activity?.onBackPressed()
    }

    fun refreshData() {
        Log.d("Alex", "in user refresh data count is ${UserViewModel.getCount()}")
        binding.adminUser1Email.text = UserViewModel.getUserEmail(0)
        binding.adminUser1Uid.text = UserViewModel.getUserUID(0)
        binding.adminUser2Email.text = UserViewModel.getUserEmail(1)
        binding.adminUser2Uid.text = UserViewModel.getUserUID(1)
        binding.adminUser3Email.text = UserViewModel.getUserEmail(2)
        binding.adminUser3Uid.text = UserViewModel.getUserUID(2)
        binding.adminUser4Email.text = UserViewModel.getUserEmail(3)
        binding.adminUser4Uid.text = UserViewModel.getUserUID(3)
    }
    private fun clearData() {
        binding.adminUser1Email.text = ""
        binding.adminUser1Uid.text = ""
        binding.adminUser2Email.text = ""
        binding.adminUser2Uid.text = ""
        binding.adminUser3Email.text = ""
        binding.adminUser3Uid.text = ""
        binding.adminUser4Email.text = ""
        binding.adminUser4Uid.text = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun convertCategories() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var previousCategoryID = 1000
                dataSnapshot.children.forEach {
                    previousCategoryID += 1
                    val category = it.key.toString()
                    if (!isNumber(category)) {
                        for (subcategoryC in it.children) {
                            val subcategory = subcategoryC.key.toString()
                            val subcategoryType = subcategoryC.value.toString()
                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                                .child(previousCategoryID.toString())
                                .child("Category")
                                .setValue(category)
                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                                .child(previousCategoryID.toString())
                                .child("SubCategory")
                                .setValue(subcategory)
                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                                .child(previousCategoryID.toString())
                                .child("Type")
                                .setValue(subcategoryType)
                        }
                    }
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                        .child(category)
                        .removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Category")
            .addListenerForSingleValueEvent(listener)
    }
    fun convertDefaults() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    if (!isNumber(it.key.toString())) {
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                            .child("0")
                            .child(it.key.toString())
                            .setValue(it.value)
                        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                            .child("1")
                            .child(it.key.toString())
                            .setValue(it.value)
                    }
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Defaults")
                        .child(it.key.toString())
                        .removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
            .addListenerForSingleValueEvent(listener)
    }
    fun convertExpenditures() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var categoryName = ""
                    var subcategoryName = ""
                    var expID = it.key.toString()
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "paidby" -> {
                                val spenderID = SpenderViewModel.getSpenderIndex(child.value.toString())
                                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
                                    .child(expID)
                                    .child("paidby")
                                    .setValue(spenderID)
                            }
                            "boughtfor" -> {
                                val spenderID = SpenderViewModel.getSpenderIndex(child.value.toString())
                                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
                                    .child(expID)
                                    .child("boughtfor")
                                    .setValue(spenderID)
                            }
                            "category" -> {
                                categoryName = child.value.toString()
                            }
                            "subcategory" -> {
                                subcategoryName = child.value.toString()
                            }
                        }
                    }
                    val catID = CategoryViewModel.getID(categoryName, subcategoryName)
                    if (catID == 0)
                        Log.d("Alex", "Unknown category in convertExpenditures $expID $categoryName $subcategoryName")
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
                        .child(expID)
                        .child("category")
                        .setValue(CategoryViewModel.getID(categoryName, subcategoryName))
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
                        .child(expID)
                        .child("subcategory")
                        .removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
            .addListenerForSingleValueEvent(listener)
    }
    fun moveExpendituresToDateFolders() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var amount = 0
                    var bfname1split = 0
                    var boughtfor = 0
                    var category = 0
                    var date = ""
                    var note = ""
                    var paidby = 0
                    var type = ""
                    var expID = it.key.toString()
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "amount" -> amount = child.value.toString().toInt()
                            "bfname1split" -> bfname1split = child.value.toString().toInt()
                            "boughtfor" -> boughtfor = child.value.toString().toInt()
                            "category" -> category = child.value.toString().toInt()
                            "paidby" -> paidby = child.value.toString().toInt()
                            "date" -> date = child.value.toString().trim()
                            "note" -> note = child.value.toString().trim()
                            "type" -> type = if (child.value.toString().trim() == "") "Expense" else child.value.toString().trim()
                        }
                    }
                    if (date == "")
                        Log.d("Alex", "What is this $expID")
                    val bm = BudgetMonth(date)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("amount")
                        .setValue(amount)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("bfname1split")
                        .setValue(bfname1split)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("boughtfor")
                        .setValue(boughtfor)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("category")
                        .setValue(category)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("paidby")
                        .setValue(paidby)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("date")
                        .setValue(date)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("note")
                        .setValue(note)
                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Transactions")
                        .child(bm.year.toString())
                        .child(bm.get2DigitMonth())
                        .child(expID)
                        .child("type")
                        .setValue(type)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
            .addListenerForSingleValueEvent(listener)
    }
    fun convertRecurringTransactions() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var categoryName = ""
                    var subcategoryName = ""
                    var expID = it.key.toString()
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "paidby" -> {
                                if (child.value.toString() != "0" &&
                                        child.value.toString() != "1" &&
                                        child.value.toString() != "2") {
                                    val spenderID =
                                        SpenderViewModel.getSpenderIndex(child.value.toString())
                                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                                        .child(expID)
                                        .child("paidby")
                                        .setValue(spenderID)
                                }
                            }
                            "boughtfor" -> {
                                if (child.value.toString() != "0" &&
                                    child.value.toString() != "1" &&
                                    child.value.toString() != "2") {
                                    val spenderID =
                                        SpenderViewModel.getSpenderIndex(child.value.toString())
                                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                                        .child(expID)
                                        .child("boughtfor")
                                        .setValue(spenderID)
                                }
                            }
                            "category" -> {
                                categoryName = child.value.toString()
                            }
                            "subcategory" -> {
                                subcategoryName = child.value.toString()
                            }
                        }
                    }
                    if (!isNumber(categoryName)) {
                        val id = CategoryViewModel.getID(categoryName, subcategoryName)
                        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                            .child(expID)
                            .child("category")
                            .setValue(CategoryViewModel.getID(categoryName, subcategoryName))
                        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
                            .child(expID)
                            .child("subcategory")
                            .removeValue()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/RecurringTransactions")
            .addListenerForSingleValueEvent(listener)
    }
    fun convertBudgets() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var budgetName = it.key.toString()
                    if (!isNumber(budgetName)) {
                        for (budget in it.children) {
                            var budgetDate = budget.key.toString()
                            for (spenderBudget in budget.children) {
                                var spenderName = spenderBudget.key.toString()
                                if (!isNumber(spenderName))
                                    spenderName =
                                        SpenderViewModel.getSpenderIndex(spenderName).toString()
                                for (row in spenderBudget.children) {
                                    var cat = Category(0, budgetName)
                                    var catID = CategoryViewModel.getID(
                                        cat.categoryName,
                                        cat.subcategoryName
                                    )
                                    if (catID == 0)
                                        Log.d("Alex", "Don't know this category $cat")
                                    MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Budget")
                                        .child(catID.toString())
                                        .child(budgetDate)
                                        .child(spenderName)
                                        .child(row.key.toString())
                                        .setValue(row.value)
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/NewBudget")
            .addListenerForSingleValueEvent(listener)
    }
}
