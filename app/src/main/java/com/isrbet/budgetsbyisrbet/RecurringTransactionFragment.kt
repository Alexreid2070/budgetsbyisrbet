package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.AdapterView
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionBinding

class RecurringTransactionFragment : Fragment() {

    private var _binding: FragmentRecurringTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTransactionBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getCopyOfRecurringTransactions())

        val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as RecurringTransaction
                val rtdf = RecurringTransactionEditDialogFragment.newInstance(itemValue.name, itemValue.amount, itemValue.period, itemValue.nextdate, itemValue.regularity, itemValue.category, itemValue.subcategory, itemValue.paidby, itemValue.boughtfor, itemValue.split1, itemValue.split2)
                rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        Log.d("Alex", "in onNewDataSaved")
                        val myAdapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getCopyOfRecurringTransactions())
                        listView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                rtdf.show(parentFragmentManager, "Edit Recurring Transaction")
            }
        binding.expandSettings.setOnClickListener {
            findNavController().navigate(R.id.SettingsFragment)
        }
        binding.expandCategories.setOnClickListener {
            findNavController().navigate(R.id.CategoryFragment)
        }
        binding.expandBudgets.setOnClickListener {
            findNavController().navigate(R.id.BudgetViewAllFragment)
        }
        binding.rtAddFab.setOnClickListener {
            addRecurringTransaction()
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.rt_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible =
                (menu.getItem(i).itemId == R.id.AddRecurringTransaction ||
                 menu.getItem(i).itemId == R.id.ViewRecurringTransactionsFragment)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.AddRecurringTransaction) {
            addRecurringTransaction()
            true
        } else if (item.itemId == R.id.ViewRecurringTransactionsFragment) {
            MyApplication.transactionSearchText = "Recurring"
            view?.findNavController()?.navigate(R.id.ViewTransactionsFragment)
            true
        } else {
            val navController = findNavController()
            item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addRecurringTransaction() {
        val rtdf = RecurringTransactionEditDialogFragment.newInstance("", 0,"Month", "2022-01-01", 1, "", "", DefaultsViewModel.getDefault(
            cDEFAULT_SPENDER), DefaultsViewModel.getDefault(cDEFAULT_SPENDER), 100, 0)
        rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getCopyOfRecurringTransactions())
                val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        })
        rtdf.show(parentFragmentManager, "Edit Recurring Transaction")
    }
}