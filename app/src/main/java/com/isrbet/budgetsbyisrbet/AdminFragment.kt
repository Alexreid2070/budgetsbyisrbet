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
        view?.findViewById<Button>(R.id.button_reinit)?.setOnClickListener { _: View ->
     //       processButton()
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
        binding.adminCurrentUser.text = email
        MyApplication.currentUserEmail = email
        UserViewModel.clearCallback()
        Log.d("Alex", "I clicked uid $uid")
        MyApplication.userUID=uid
        DefaultsViewModel.refresh()
        ExpenditureViewModel.refresh()
        CategoryViewModel.refresh()
        SpenderViewModel.refresh()
        BudgetViewModel.refresh()
        RecurringTransactionViewModel.refresh()
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

    fun processButton() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    var split1 = 0
                    var split2 = 0
                    val key = it.key.toString()
                    it.children.forEach {
                        if (it.key.toString() == "bfname1split") {
                            split1 = it.value.toString().toDouble().toInt()
                        }
                        if (it.key.toString() == "bfname2split") {
                            split2 = it.value.toString().toDouble().toInt()
                        }
                    }
                    if (split1 + split2 != 100) {
                        Log.d("Alex", "Bad expenditure " + it.toString())
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Expenditures")
            .addValueEventListener(listener)
    }
}
