package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentAdminBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.adminCurrentUser.text = MyApplication.currentUserEmail
        getView()?.findViewById<Button>(R.id.button_reinit)?.setOnClickListener {_: View ->
//            processButton()
//            tempProcessButton()
        }
        getView()?.findViewById<Button>(R.id.button_load_users)?.setOnClickListener {_: View ->
            UserViewModel.loadUsers()
            UserViewModel.setCallback(object: UserDataUpdatedCallback {
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

    fun uidClicked(uid: String, email: String) {
        Toast.makeText(activity, "Switching to user " + email, Toast.LENGTH_SHORT).show()
        binding.adminCurrentUser.text = email
        MyApplication.currentUserEmail = email
        UserViewModel.clearCallback()
        Log.d("Alex", "I clicked uid " + uid)
        MyApplication.userUID=uid
        DefaultsViewModel.refresh()
        ExpenditureViewModel.refresh()
        CategoryViewModel.refresh()
        SpenderViewModel.refresh()
        BudgetViewModel.refresh(requireActivity())
        RecurringTransactionViewModel.refresh()
    }

    fun refreshData() {
        binding.adminUser1Email.text = UserViewModel.getUserEmail(0)
        binding.adminUser1Uid.text = UserViewModel.getUserUID(0)
        binding.adminUser2Email.text = UserViewModel.getUserEmail(1)
        binding.adminUser2Uid.text = UserViewModel.getUserUID(1)
        binding.adminUser3Email.text = UserViewModel.getUserEmail(2)
        binding.adminUser3Uid.text = UserViewModel.getUserUID(2)
        binding.adminUser4Email.text = UserViewModel.getUserEmail(3)
        binding.adminUser4Uid.text = UserViewModel.getUserUID(3)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun processButton() {
        var budgetListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach() {
                    var categoryName = it.key.toString()
                    if (categoryName != "Debt-Line of Credit" && categoryName != "Housing-Gas") {
                        it.children.forEach() {
                            var period = it.key.toString()
                            Log.d("Alex", categoryName + " " + period)
                            it.children.forEach() {
                                var who = it.key.toString()
                                var amount = it.value.toString().toInt()
                                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/NewBudget")
                                    .child(categoryName)
                                    .child(period)
                                    .child(who)
                                    .child("amount")
                                    .setValue(amount)
                                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/NewBudget")
                                    .child(categoryName)
                                    .child(period)
                                    .child(who)
                                    .child("occurence")
                                    .setValue(0)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/NewBudget").addValueEventListener(budgetListener)
    }

    fun addCategories() {
//        CategoryViewModel.addCategoryAndSubcategory("Housing", "Gas", "Non-Discretionary")
    }
    fun addTransactions() {
//        ExpenditureViewModel.addTransaction(ExpenditureOut("06-Mar-2020", 6889, "Life", "Travel", "Travel - souvenirs (cusuco)", "Rheannon", "Joint", 16, 84, ""))
         }
    fun addRecurringTransactions() {
//        RecurringTransactionViewModel.addRecurringTransaction(RecurringTransaction("Teksavy", 5983, "Monthly", 1, "20-Jan-2022", "Life", "Internet", "Rheannon", "Rheannon"))
    }
    fun addBudget() {
//        BudgetViewModel.updateBudget("Transportation-Insurance", "2018-11", 9984.0)
    }
}
