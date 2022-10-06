package com.isrbet.budgetsbyisrbet

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementDetailsBinding

enum class RetirementDetailsViews(val code: Int) {
    ALL(0),
    SUMMARY(1),
    TAX(2),
    INCOME(3),
    RRSP(4),
    TFSA(5),
    SAVINGS(6),
    PROPERTY(7),
    PENSION(8);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
    fun compareTo(iAssetType: AssetType) : Boolean {
        return when (code) {
            4 -> iAssetType == AssetType.RRSP
            5 -> iAssetType == AssetType.TFSA
            6 -> iAssetType == AssetType.SAVINGS
            7 -> iAssetType == AssetType.PROPERTY
            else -> false
        }
    }
}

class RetirementDetailsFragment : Fragment() {
    private var _binding: FragmentRetirementDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRetirementDetailsBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_retirement_details, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.expandView.setOnClickListener {
            onViewClicked(false)
        }
        binding.viewRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.viewRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton

            val whichView = when (radioButton.text.toString()) {
                getString(R.string.show_summary_columns) -> RetirementDetailsViews.SUMMARY
                getString(R.string.show_tax_columns) -> RetirementDetailsViews.TAX
                getString(R.string.show_income_details) -> RetirementDetailsViews.INCOME
                getString(R.string.show_pension_details) -> RetirementDetailsViews.PENSION
                getString(R.string.show_rrsp_details) -> RetirementDetailsViews.RRSP
                getString(R.string.show_tfsa_details) -> RetirementDetailsViews.TFSA
                getString(R.string.show_savings_details) -> RetirementDetailsViews.SAVINGS
                getString(R.string.show_property_details) -> RetirementDetailsViews.PROPERTY
                else -> RetirementDetailsViews.ALL
            }
            if (DefaultsViewModel.getDefaultViewInRetirementDetails() != whichView)
                DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_IN_RETIREMENT_DETAILS, whichView.ordinal)
            createTableHeader(whichView)
            createTableRows(whichView)
            onViewClicked(true)
        }
        when (DefaultsViewModel.getDefaultViewInRetirementDetails()) {
            RetirementDetailsViews.SUMMARY -> binding.showSummaryColumnsButton.isChecked = true
            RetirementDetailsViews.INCOME -> binding.showIncomeDetailsButton.isChecked = true
            RetirementDetailsViews.PENSION -> binding.showPensionDetailsButton.isChecked = true
            RetirementDetailsViews.TAX -> binding.showTaxColumnsButton.isChecked = true
            RetirementDetailsViews.RRSP -> binding.showRrspDetailsButton.isChecked = true
            RetirementDetailsViews.TFSA -> binding.showTfsaDetailsButton.isChecked = true
            RetirementDetailsViews.SAVINGS -> binding.showSavingsDetailsButton.isChecked = true
            RetirementDetailsViews.PROPERTY -> binding.showPropertyDetailsButton.isChecked = true
            else -> binding.showAllColumnsButton.isChecked = true
        }
    }

    private fun addHeaderCell(iTableRow: TableRow, iHeaderString: String,
                              iBold: Boolean = false,
                              iRed: Boolean = false) {
        val tv0 = TextView(requireContext())
        tv0.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT)
        tv0.text = iHeaderString
        tv0.gravity = (Gravity.BOTTOM or Gravity.CENTER)
        tv0.setPadding(10, 10, 10, 10)
        if (iBold)
            tv0.setTypeface(null, Typeface.BOLD)
        if (iRed)
            tv0.setTextColor(ContextCompat.getColor(requireContext(), R.color.darkRed))
        iTableRow.addView(tv0)
    }

    private fun onViewClicked(iAlwaysClose: Boolean) {
        if (binding.viewRadioGroup.visibility == View.GONE && !iAlwaysClose) { // ie expand the section
            binding.viewRadioGroup.visibility = View.VISIBLE
            binding.stickyLabel.visibility = View.VISIBLE
            binding.expandView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null, null, null)
            binding.expandView.textSize = 16F
            binding.expandView.setBackgroundResource(R.drawable.rounded_top_corners)
        } else { // ie retract the section
            binding.viewRadioGroup.visibility = View.GONE
            binding.stickyLabel.visibility = View.GONE
            binding.expandView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null, null, null)
            binding.expandView.setBackgroundResource(android.R.color.transparent)
            binding.expandView.textSize = 14F
        }
    }

    private fun createTableHeader(iWhichView: RetirementDetailsViews) {
        binding.retirementTableRows.removeAllViews()
        val tr = TableRow(requireContext())
//        tr.id = iTemp
//        tr.tag = "Test"
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
//        trParams.setMargins(5, 0, 5, 0)
        tr.setPadding(5, 5, 5, 0)
        tr.layoutParams = trParams
        addHeaderCell(tr, getString(R.string.year), true)
        if (iWhichView == RetirementDetailsViews.ALL)
            addHeaderCell(tr, getString(R.string.target_income), true)
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.TAX ||
            iWhichView == RetirementDetailsViews.SUMMARY)
            addHeaderCell(tr, getString(R.string.net_income), true)
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.TAX) {
            addHeaderCell(tr, getString(R.string.maximum_taxable_income), true)
            addHeaderCell(tr, getString(R.string.taxable_income), true)
            addHeaderCell(tr, getString(R.string.gross_income), true)
            addHeaderCell(tr, getString(R.string.tax), true)
        }
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.INCOME) {
            addHeaderCell(tr, getString(R.string.salary), true)
        }
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.INCOME ||
            iWhichView == RetirementDetailsViews.TAX) {
            addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_pension),
                getString(R.string.cpp)), true)
        }
        var minAlreadyShown = false
        gRetirementDetailsList[1].pensionIncomes.forEach {
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.INCOME ||
                iWhichView == RetirementDetailsViews.PENSION ||
                iWhichView == RetirementDetailsViews.TAX)
                addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_pension),
                    it.name), true)
        }
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.INCOME ||
            iWhichView == RetirementDetailsViews.TAX) {
            addHeaderCell(tr, getString(R.string.oas), true)
        }
        gRetirementDetailsList[1].assetIncomes.forEach {
            when (it.type) {
                AssetType.RRSP -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.RRSP) {
                        if (!minAlreadyShown) {
                            addHeaderCell(tr, getString(R.string.minimum_rrif_withdrawal), true)
                            minAlreadyShown = true
                        }
                    }
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.RRSP ||
                        iWhichView == RetirementDetailsViews.TAX) {
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_withdrawal),
                            getString(R.string.rrsp) + ": " + it.name), true)
                    }
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.RRSP)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.rrsp) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.RRSP)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_balance),
                            getString(R.string.rrsp) + ": " + it.name), true)
                }
                AssetType.TFSA -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.TFSA)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_withdrawal),
                            getString(R.string.tfsa) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.TFSA)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.tfsa) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.TFSA)
                    addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_balance),
                        getString(R.string.tfsa) + ": " + it.name), true)
                }
                AssetType.SAVINGS -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.SAVINGS)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_withdrawal),
                            getString(R.string.savings) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SAVINGS ||
                        (iWhichView == RetirementDetailsViews.TAX &&
                                it.growthIsTaxable()))
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.savings) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.SAVINGS)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_balance),
                            getString(R.string.savings) + ": " + it.name), true)
                }
                AssetType.PROPERTY -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_withdrawal),
                            getString(R.string.property) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.property) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(MyApplication.getString(R.string.s_balance),
                            getString(R.string.property) + ": " + it.name), true)
                }
                else -> {} // do nothing
            }
        }
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.SUMMARY)
            addHeaderCell(tr, getString(R.string.net_worth), true)
        binding.retirementTableRows.addView(tr)
    }

    private fun createTableRows(iWhichView: RetirementDetailsViews) {
        for (i in 0 until gRetirementDetailsList.size) {
            val tr = TableRow(requireContext())
            val trParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
//            trParams.setMargins(0, 0, 0, 0)
            tr.setPadding(5, 0, 0, 0)
            tr.layoutParams = trParams

            addHeaderCell(tr, gRetirementDetailsList[i].year.toString())
            val targetAnnualIncome = gRetirementDetailsList[i].targetAnnualIncome
            if (iWhichView == RetirementDetailsViews.ALL)
                addHeaderCell(tr, targetAnnualIncome.toString())
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.TAX ||
                iWhichView == RetirementDetailsViews.SUMMARY) {
                val totalNetIncome = gRetirementDetailsList[i].getTotalAvailableIncome()
                if (totalNetIncome < targetAnnualIncome)
                    addHeaderCell(tr, totalNetIncome.toString(), iBold = false, iRed = true) // show red
                else
                    addHeaderCell(tr, totalNetIncome.toString())
            }
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.TAX) {
                addHeaderCell(tr, gRetirementDetailsList[i].getMaximumTaxableIncome().toString())
                addHeaderCell(tr, gRetirementDetailsList[i].getTaxableIncome(gRetirementDetailsList[i].year).toString())
                addHeaderCell(tr, gRetirementDetailsList[i].getTotalGrossIncome(gRetirementDetailsList[i].year).toString())
                addHeaderCell(tr, gRetirementDetailsList[i].getTotalTax().toString())
            }
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.INCOME) {
                addHeaderCell(tr, gRetirementDetailsList[i].getTotalSalary().toString())
            }
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.INCOME ||
                iWhichView == RetirementDetailsViews.TAX) {
                addHeaderCell(tr, gRetirementDetailsList[i].cppIncome.toString())
            }
            var minAlreadyShown = false
            gRetirementDetailsList[i].pensionIncomes.forEach {
                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView == RetirementDetailsViews.INCOME ||
                    iWhichView == RetirementDetailsViews.PENSION ||
                    iWhichView == RetirementDetailsViews.TAX)
                    addHeaderCell(tr, it.getPensionIncome(gRetirementDetailsList[i].year).toString())
            }
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.INCOME ||
                iWhichView == RetirementDetailsViews.TAX) {
                addHeaderCell(tr, gRetirementDetailsList[i].oasIncome.toString())
            }
            gRetirementDetailsList[i].assetIncomes.forEach {
                if ((iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView == RetirementDetailsViews.INCOME ||
                    iWhichView == RetirementDetailsViews.RRSP) &&
                    it.type == AssetType.RRSP) {
                    if (!minAlreadyShown) {
                        addHeaderCell(tr, gRetirementDetailsList[i].getMinimumRRIFWithdrawal(
                            gRetirementDetailsList[i].getAgeAtStartOfYear(
                                gRetirementDetailsList[i].year)
                        ).toString())
                        minAlreadyShown = true
                    }
                }
                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView == RetirementDetailsViews.INCOME ||
                    iWhichView.compareTo(it.type) ||
                    (iWhichView == RetirementDetailsViews.TAX &&
                            it.withdrawalIsTaxable())) {
                    if (it.withdrawalAmount > 0 && it.getEndingBalance() == 0) {
                        addHeaderCell(tr, it.withdrawalAmount.toString(), iBold = false, iRed = true)
                    } else {
                        addHeaderCell(tr, it.withdrawalAmount.toString())
                    }
                }
                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView.compareTo(it.type) ||
                    (iWhichView == RetirementDetailsViews.TAX &&
                            it.growthIsTaxable()))
                    if (it.withdrawalAmount > 0 && it.getEndingBalance() == 0) {
                        addHeaderCell(tr, it.growthThisYear.toString(), iBold = false, iRed = true)
                    } else {
                        addHeaderCell(tr, it.growthThisYear.toString())
                    }

                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView == RetirementDetailsViews.SUMMARY ||
                    iWhichView.compareTo(it.type)) {
                    if (it.type == AssetType.PROPERTY && (it as Property).soldInYear == gRetirementDetailsList[i].year) {
                        addHeaderCell(tr, it.getEndingBalance().toString(), true)
                    } else {
                        if (it.withdrawalAmount > 0 && it.getEndingBalance() == 0)
                            addHeaderCell(tr, it.getEndingBalance().toString(), iBold = false, iRed = true)
                        else
                            addHeaderCell(tr, it.getEndingBalance().toString())
                    }
                }
            }
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.SUMMARY)
                addHeaderCell(tr, gRetirementDetailsList[i].getNetWorth().toString())
            binding.retirementTableRows.addView(tr)
        }
    }
}