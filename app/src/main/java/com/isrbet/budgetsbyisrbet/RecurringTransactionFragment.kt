package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionBinding

class RecurringTransactionFragment : Fragment() {

    private var _binding: FragmentRecurringTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTransactionBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val rows = RecurringTransactionViewModel.getCopyOfRecurringTransactions()
        val adapter = RecurringTransactionAdapter(requireContext(), rows)

        val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as RecurringTransaction
                val rtdf = RecurringTransactionEditDialogFragment.newInstance(itemValue.name,
                    itemValue.amount, itemValue.period, itemValue.nextdate, itemValue.regularity,
                    itemValue.category,
                    itemValue.paidby,
                    itemValue.boughtfor,
                    itemValue.split1,
                    if (itemValue.activeLoan) "true" else "false",
                    itemValue.loanFirstPaymentDate,
                    itemValue.loanAmount,
                    itemValue.loanAmortization,
                    itemValue.loanInterestRate,
                    itemValue.loanPaymentRegularity,
                    itemValue.loanAcceleratedPaymentAmount
                )
                rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        Log.d("Alex", "in onNewDataSaved")
                        val myAdapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getCopyOfRecurringTransactions())
                        listView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                rtdf.show(parentFragmentManager, "Edit Scheduled Payment")
            }
        if (rows.size == 0) {
            binding.noInformationText.visibility = View.VISIBLE
            binding.noInformationText.text = "You have not yet entered any scheduled payments.  \n\nClick on the Add button below to add a scheduled payment."
        } else {
            binding.noInformationText.visibility = View.GONE
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
        HintViewModel.showHint(parentFragmentManager, "ScheduledPayment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addRecurringTransaction() {
        val split1 = when {
            SpenderViewModel.getDefaultSpender() == 0 -> {
                100
            }
            SpenderViewModel.getDefaultSpender() == 1 -> {
                0
            }
            else -> {
                SpenderViewModel.getSpenderSplit(0)
            }
        }

        val rtdf = RecurringTransactionEditDialogFragment.newInstance("", 0,
            "Month", "2022-01-01", 1, 0,
            SpenderViewModel.getDefaultSpender(),
            SpenderViewModel.getDefaultSpender(),
            split1,
        "false", "", 0, 0, 0, LoanPaymentRegularity.BIWEEKLY, 0)
        rtdf.setDialogFragmentListener(object: RecurringTransactionEditDialogFragment.RecurringTransactionEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = RecurringTransactionAdapter(requireContext(), RecurringTransactionViewModel.getCopyOfRecurringTransactions())
                val listView: ListView = requireActivity().findViewById(R.id.recurring_transaction_list_view)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        })
        rtdf.show(parentFragmentManager, "Edit Scheduled Payment")
    }
}