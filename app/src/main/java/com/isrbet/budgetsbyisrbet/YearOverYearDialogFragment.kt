package com.isrbet.budgetsbyisrbet

import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentYearOverYearDialogBinding

class YearOverYearDialogFragment : DialogFragment() {
    private var _binding: FragmentYearOverYearDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_CATEGORY_ID = "1"
        private var inCategoryID: Int = 0
        fun newInstance(
            categoryID: String
        ): YearOverYearDialogFragment {
            val args = Bundle()

            args.putString(KEY_CATEGORY_ID, categoryID)
            val fragment = YearOverYearDialogFragment()
            fragment.arguments = args
            inCategoryID = categoryID.toInt()
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYearOverYearDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cat = CategoryViewModel.getCategory(inCategoryID)
        binding.title.text = CategoryViewModel.getFullCategoryName(inCategoryID)
        if (cat != null) {
            binding.title.setBackgroundColor(DefaultsViewModel.getCategoryDetail(cat.categoryName).color)
            if (inDarkMode(requireContext()))
                binding.title.setTextColor(R.color.black)
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        val tvParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )

        val yearTV = TextView(requireContext())
        yearTV.gravity = Gravity.CENTER
        yearTV.layoutParams = tvParams
        val averageTV = TextView(requireContext())
        averageTV.text = "Avg per Month"
        averageTV.setTypeface(null, Typeface.BOLD)
        averageTV.setPadding(15, 0, 15, 0)
        averageTV.gravity = Gravity.CENTER
        averageTV.layoutParams = tvParams
        val actualTV = TextView(requireContext())
        actualTV.text = "Actuals"
        actualTV.setTypeface(null, Typeface.BOLD)
        actualTV.setPadding(15, 0, 15, 0)
        actualTV.gravity = Gravity.CENTER
        actualTV.layoutParams = tvParams
        val budgetTV = TextView(requireContext())
        budgetTV.text = "Budget"
        budgetTV.setTypeface(null, Typeface.BOLD)
        budgetTV.setPadding(15, 0, 15, 0)
        budgetTV.gravity = Gravity.CENTER
        budgetTV.layoutParams = tvParams
        val deltaTV = TextView(requireContext())
        deltaTV.text = "Delta"
        deltaTV.setTypeface(null, Typeface.BOLD)
        deltaTV.setPadding(15, 0, 15, 0)
        deltaTV.gravity = Gravity.CENTER
        deltaTV.layoutParams = tvParams
        val tr = TableRow(requireContext())
        tr.addView(yearTV)
        tr.addView(averageTV)
        tr.addView(actualTV)
        tr.addView(budgetTV)
        tr.addView(deltaTV)
        trParams.setMargins(15, 0, 15, 0)
        tr.layoutParams = trParams
        binding.tableRows.addView(tr)

        for (yr in 0 until (TransactionViewModel.getLatestYear() - TransactionViewModel.getEarliestYear() + 1)) {
            val tr = TableRow(requireContext())
            val yearTV = TextView(requireContext())
            yearTV.text = (TransactionViewModel.getEarliestYear() + yr).toString()
            yearTV.setPadding(0, 0, 15, 0)
            yearTV.gravity = Gravity.START
            yearTV.layoutParams = tvParams
            tr.layoutParams = trParams
            tr.addView(yearTV)

            var ttv = gAverageRow?.getChildAt(2+yr) as TextView
            var tvToAdd = TextView(requireContext())
            tvToAdd.text = ttv.text
            tvToAdd.setPadding(15, 0, 15, 0)
            tvToAdd.gravity = Gravity.CENTER
            tvToAdd.layoutParams = tvParams
            tr.addView(tvToAdd)

            ttv = gActualRow?.getChildAt(2+yr) as TextView
            tvToAdd = TextView(requireContext())
            tvToAdd.text = ttv.text
            tvToAdd.setPadding(15, 0, 15, 0)
            tvToAdd.gravity = Gravity.CENTER
            tvToAdd.layoutParams = tvParams
            tr.addView(tvToAdd)

            ttv = gBudgetRow?.getChildAt(2+yr) as TextView
            tvToAdd = TextView(requireContext())
            tvToAdd.text = ttv.text
            tvToAdd.setPadding(15, 0, 15, 0)
            tvToAdd.gravity = Gravity.CENTER
            tvToAdd.layoutParams = tvParams
            tr.addView(tvToAdd)

            ttv = gDeltaRow?.getChildAt(2+yr) as TextView
            tvToAdd = TextView(requireContext())
            tvToAdd.text = ttv.text
            tvToAdd.setPadding(15, 0, 15, 0)
            tvToAdd.gravity = Gravity.CENTER
            tvToAdd.layoutParams = tvParams
            tr.addView(tvToAdd)
            binding.tableRows.addView(tr)
        }
    }
}