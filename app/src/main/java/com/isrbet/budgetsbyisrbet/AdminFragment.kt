package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
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

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.adminCurrentUser.text = MyApplication.currentUserEmail
        view?.findViewById<Button>(R.id.button_dosomething)?.setOnClickListener { _: View ->
            // doSomething()
        }
        view?.findViewById<Button>(R.id.button_load_users)?.setOnClickListener { _: View ->
            AppUserViewModel.loadUsers()
            AppUserViewModel.setCallback(object: DataUpdatedCallback {
                override fun onDataUpdate() {
                    Log.d("Alex", "got a callback that user data was updated")
                    addUsersToList()
                }
            })
        }
        addUsersToList()
    }

    private fun addUsersToList() {
        val adapter = UserAdapter(requireContext(), AppUserViewModel.getUsers())

        val listView: ListView = requireActivity().findViewById(R.id.user_list)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as AppUser
                uidClicked(itemValue.uid, itemValue.email)
            }
    }

    private fun uidClicked(uid: String, email: String) {
        Toast.makeText(activity, "Switching to user $email", Toast.LENGTH_SHORT).show()
        MyApplication.currentUserEmail = email
        AppUserViewModel.clearCallback()
        Log.d("Alex", "I clicked uid $uid")
        switchTo(uid)
        activity?.onBackPressed()
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
                    val category = it.key.toString()
                    if (!isNumber(category)) {
                        for (subcategoryC in it.children) {
                            previousCategoryID += 1
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
/*                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Category")
                                .child(category)
                                .child(subcategory)
                                .removeValue() */
                        }
                    }
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
                var i = 0
                Log.d("Alex", "There are ${dataSnapshot.children.count()} expenditures to convert")
                dataSnapshot.children.forEach {
                    i += 1
                    var categoryName = ""
                    var subcategoryName = ""
                    val expID = it.key.toString()

                    var amount = 0
                    var bfname1split = 0
                    var boughtfor = 0
                    var category = 0
                    var date = ""
                    var note = ""
                    var paidby = 0
                    var type = ""
                    for (child in it.children) {
                        when (child.key.toString()) {
                            "amount" -> amount = child.value.toString().toInt()
                            "bfname1split" -> bfname1split = child.value.toString().toInt()
                            "boughtfor" -> {
                                boughtfor = SpenderViewModel.getSpenderIndex(child.value.toString())
                            }
                            "category" -> categoryName = child.value.toString()
                            "subcategory" -> subcategoryName = child.value.toString()
                            "paidby" -> {
                                paidby = SpenderViewModel.getSpenderIndex(child.value.toString())
                            }
                            "date" -> date = child.value.toString().trim()
                            "note" -> note = child.value.toString().trim()
                            "type" -> type = if (child.value.toString().trim() == "") "Expense" else child.value.toString().trim()
                        }
                    }

                    if (type == "Transfer")
                        category = -99
                    else
                        category = CategoryViewModel.getID(categoryName, subcategoryName)
                    if (category == 0)
                        Log.d("Alex", "Unknown category in convertExpenditures $expID $categoryName $subcategoryName")

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
                        Log.d(
                            "Alex",
                            "Completed transaction $i of ${dataSnapshot.children.count()}"
                        )
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
            .addListenerForSingleValueEvent(listener)
    }
    fun whatsBeasProblem() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var total = 0
                dataSnapshot.children.forEach {
                    var paidby = ""
                    var boughtfor = ""
                    var amount = 0
                    val key = it.key.toString()
                    for (element in it.children) {
                        when (element.key.toString()) {
                            "paidby" -> paidby = element.value.toString()
                            "boughtfor" -> boughtfor = element.value.toString()
                            "amount" -> amount = element.value.toString().toInt()
                        }
                    }
                    if (paidby == "Beatrice" && boughtfor == "Beatrice") {
                        Log.d("Alex", "Exp amount for $key is $amount")
                        total += amount
                    }
                }
                Log.d("Alex", "Total is $total")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/RecZvLAdKGalU9OAtP2GHDGFr0r1/Expenditures")
            .addListenerForSingleValueEvent(listener)
    }

    fun fixRheannon() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var i = 0
                dataSnapshot.children.forEach {
                    val y = it.key.toString()
                    for (month in it.children) {
                        val m = month.key.toString()
                        for (exp in month.children) {
                            i += 1
                            var bfname1split = 0
                            val key = exp.key.toString()
                            for (element in exp.children) {
                                when (element.key.toString()) {
                                    "bfname1split" -> bfname1split =
                                        element.value.toString().toInt()
                                }
                            }
                            Log.d(
                                "Alex",
                                "($i) Prev split was $bfname1split and new is " + (100 - bfname1split) + " $y $m $key"
                            )
                            MyApplication.database.getReference("Users/3yvcaxXaASQLQu9pc6EQWp6h57q2/Transactions")
                                .child(y)
                                .child(m)
                                .child(key)
                                .child("bfname1split")
                                .setValue(100-bfname1split)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/3yvcaxXaASQLQu9pc6EQWp6h57q2/Transactions")
            .addListenerForSingleValueEvent(listener)
    }
    fun checkExpenditures() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    if (!TransactionViewModel.exists(it.key.toString()))
                        Log.d("Alex", "Can't find " + it.key.toString())
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
                var i = 0
                dataSnapshot.children.forEach {
                    i += 1
                    var amount = 0
                    var bfname1split = 0
                    var boughtfor = 0
                    var category = 0
                    var date = ""
                    var note = ""
                    var paidby = 0
                    var type = ""
                    val expID = it.key.toString()
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
                    Log.d("Alex", "Completed transaction $i of ${dataSnapshot.children.count()}")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                MyApplication.displayToast("User authorization failed 102.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures2")
            .addListenerForSingleValueEvent(listener)
    }
    fun convertRecurringTransactions() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var categoryName = ""
                    var subcategoryName = ""
                    val expID = it.key.toString()
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
                    val budgetName = it.key.toString()
                    if (!isNumber(budgetName)) {
                        for (budget in it.children) {
                            val budgetDate = budget.key.toString()
                            for (spenderBudget in budget.children) {
                                var spenderName = spenderBudget.key.toString()
                                if (!isNumber(spenderName))
                                    spenderName =
                                        SpenderViewModel.getSpenderIndex(spenderName).toString()
                                for (row in spenderBudget.children) {
                                    val cat = Category(0, budgetName)
                                    val catID = CategoryViewModel.getID(
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
