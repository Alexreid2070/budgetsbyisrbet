package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentScheduledPaymentBinding

class ScheduledPaymentFragment : Fragment() {

    private var _binding: FragmentScheduledPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduledPaymentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val rows = ScheduledPaymentViewModel.getCopyOfScheduledPayments()
        val adapter = ScheduledPaymentAdapter(requireContext(), rows)

        binding.scheduledPaymentListView.adapter = adapter

        binding.scheduledPaymentListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = binding.scheduledPaymentListView.getItemAtPosition(position) as ScheduledPayment
                val rtdf = ScheduledPaymentEditDialogFragment.newInstance(itemValue.name)
                rtdf.setDialogFragmentListener(object: ScheduledPaymentEditDialogFragment.ScheduledPaymentEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = ScheduledPaymentAdapter(requireContext(), ScheduledPaymentViewModel.getCopyOfScheduledPayments())
                        binding.scheduledPaymentListView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
            }
        if (rows.size == 0) {
            binding.noInformationText.visibility = View.VISIBLE
            binding.noInformationText.text = getString(R.string.you_have_not_yet_entered_any_scheduled_payments)
        } else {
            binding.noInformationText.visibility = View.GONE
        }
        binding.addFab.setOnClickListener {
            addScheduledPayment()
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
        HintViewModel.showHint(parentFragmentManager, cHINT_SCHEDULED_PAYMENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addScheduledPayment() {
        val rtdf = ScheduledPaymentEditDialogFragment.newInstance("")
        rtdf.setDialogFragmentListener(object: ScheduledPaymentEditDialogFragment.ScheduledPaymentEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = ScheduledPaymentAdapter(requireContext(), ScheduledPaymentViewModel.getCopyOfScheduledPayments())
                val listView: ListView = requireActivity().findViewById(R.id.scheduled_payment_list_view)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        })
        rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
    }
}