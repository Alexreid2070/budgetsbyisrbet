package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    lateinit var expListener: ValueEventListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        getView()?.findViewById<Button>(R.id.button_reinit)?.setOnClickListener {view: View ->
            processButton()
        }
    }

    fun processButton() {
        MyApplication.userUID="3yvcaxXaASQLQu9pc6EQWp6h57q2"
        ExpenditureViewModel.getExpenditures().forEach() {
            if (it.category == "Transfer") {
                var eOut = ExpenditureOut(it.date, it.amount, it.category, it.subcategory, it.note, it.paidby, it.boughtfor, "T")
                MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Expenditures").child(it.mykey)
                    .setValue(eOut)
            }
        }
    }

    fun copyData() {
//        val expDBRef = MyApplication.databaseref.child("Users/"+MyApplication.userUID+"/Expenditures").orderByChild("date")
        val expDBRef = MyApplication.databaseref.child("Users/alexreidandbrentjohnstongmailcom/RecurringTransactions")
        var key: String = ""
        expListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (element in dataSnapshot.children.toMutableList()) {
                    key = element.key.toString()
                    for (child in element.children) {
                        MyApplication.databaseref.child("Users/AgcnEPqB4zbDJUHME3Z29gejcyu1/RecurringTransactions/"+key).child(child.key.toString()).setValue(child.value)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        expDBRef.addValueEventListener(expListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        MyApplication.databaseref.child("Users/alexreidandbrentjohnstongmailcom/Spender").removeEventListener(expListener)
        _binding = null
    }

    fun addCategories() {
//        CategoryViewModel.addCategoryAndSubcategory("Housing", "Gas", "Non-Discretionary")
    }
    fun addTransactions() {
//        ExpenditureViewModel.addTransaction(ExpenditureOut("07-Jan-2022", 8392, "Housing", "Gas", "enbridge", "Rheannon", "Rheannon", "") )

    }
    fun addRecurringTransactions() {
//        RecurringTransactionViewModel.addRecurringTransaction(RecurringTransaction("Teksavy", 5983, "Monthly", 1, "20-Jan-2022", "Life", "Internet", "Rheannon", "Rheannon"))
    }
    fun addBudget() {
//        BudgetViewModel.updateBudget("Transportation-Insurance", "2018-11", 9984.0)
    }
}
