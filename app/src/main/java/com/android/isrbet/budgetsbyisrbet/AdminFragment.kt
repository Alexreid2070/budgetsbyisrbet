package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentAdminBinding
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
        MyApplication.databaseref.child("Users").child("alexreidandbrentjohnstongmailcom").setValue(1)
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
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Condo-Hydro", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Condo-Insurance", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Condo-Mortgage", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Condo-Property tax", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Hydro", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Insurance", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Internet", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Miscellaneous", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Property tax", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Housing", "Cottage-Renos & Mtce", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Booze", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Cellphones", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Charity", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Cleaning", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Clothing and Personal Care", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Entertainment", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Family & Gifts & Donations", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Fitness", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Health & Dental", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Hobbies", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Home", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Internet", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Kitchener Rangers Hockey", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Miscellaneous", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Restaurants", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Life", "Wedding", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "CAA", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Car payment", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "ETR & Taxi & Park", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Gas", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Insurance", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Licensing", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Maintenance", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Transportation", "Miscellaneous", "Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "BC", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Caucuses", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Cruise", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Denmark & Portugal", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Las Vegas", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Mexico", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Miscellaneous", "Non-Discretionary")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Peru", "Off")
        CategoryViewModel.addCategoryAndSubcategory("Travel", "Road Trip", "Off")

    }
}
