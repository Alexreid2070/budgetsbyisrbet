package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.AdapterView
import android.widget.ListView
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionBinding

class RecurringTransactionFragment : Fragment() {

    private var _binding: FragmentRecurringTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecurringTransactionBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
        val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getRecurringTransactions())

        val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
        listView.setAdapter(adapter)

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {

                // value of item that is clicked
                var itemValue = listView.getItemAtPosition(position) as RecurringTransaction
                var rtdf = RecurringTransactionEditDialogFragment.newInstance(itemValue.name, itemValue.amount, itemValue.period, itemValue.nextdate, itemValue.regularity, itemValue.category, itemValue.subcategory, itemValue.paidby, itemValue.boughtfor)
                rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        Log.d("Alex", "in onNewDataSaved")
                        val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getRecurringTransactions())
                        listView.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                    }
                })
                rtdf.show(getParentFragmentManager(), "Edit Recurring Transaction")
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.AddRecurringTransaction)
                menu.getItem(i).setVisible(true)
            else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.AddRecurringTransaction) {
            addRecurringTransaction()
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun addRecurringTransaction() {
        var rtdf = RecurringTransactionEditDialogFragment.newInstance("", 0,"Month", "2022-01-01", 1, "", "", DefaultsViewModel.getDefault(
            cDEFAULT_SPENDER), DefaultsViewModel.getDefault(cDEFAULT_SPENDER))
        rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getRecurringTransactions())
                val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
                listView.setAdapter(adapter)
                adapter.notifyDataSetChanged()
            }
        })
        rtdf.show(getParentFragmentManager(), "Edit Recurring Transaction")
    }
}