package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
            doSomething()
        }
        view?.findViewById<Button>(R.id.button_load_users)?.setOnClickListener { _: View ->
            AppUserViewModel.loadUsers()
            AppUserViewModel.setCallback(object: DataUpdatedCallback {
                override fun onDataUpdate() {
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
        Toast.makeText(activity, getString(R.string.switching_to_user) + " $email", Toast.LENGTH_SHORT).show()
        MyApplication.currentUserEmail = email
        AppUserViewModel.clearCallback()
        switchTo(uid)
        activity?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun doSomething() {
    }
}
