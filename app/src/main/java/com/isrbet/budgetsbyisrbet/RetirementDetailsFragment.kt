package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementDetailsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class RetirementDetailsViews(val code: Int) {
    ALL(0),
    SUMMARY(1),
    TAX(2),
    INCOME(3),
    RRSP(4),
    TFSA(5),
    SAVINGS(6),
    PROPERTY(7),
    PENSION(8),
    LIRA(9);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
    fun compareTo(iAssetType: AssetType) : Boolean {
        return when (code) {
            4 -> iAssetType == AssetType.RRSP
            5 -> iAssetType == AssetType.TFSA
            6 -> iAssetType == AssetType.SAVINGS
            7 -> iAssetType == AssetType.PROPERTY
            9 -> iAssetType == AssetType.LIRA_LIF || iAssetType == AssetType.LIRA_ANNUITY
            else -> false
        }
    }
}

class RetirementDetailsFragment : Fragment() {
    private var _binding: FragmentRetirementDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: RetirementDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRetirementDetailsBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_retirement_details, container, false)
/*        val saveFileLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { result ->
                result?.let { saveFile(it) }
            }
        binding.saveButton.setOnClickListener {
            if (args.scenarioName == "")
                saveFileLauncher.launch("scenario.csv")
            else
                saveFileLauncher.launch(args.scenarioName+".csv")
        } */
        binding.saveButton.setOnClickListener {
            var fName = SpenderViewModel.getSpenderName(args.userID) + " "
            fName += if (args.scenarioName == "")
                "scenario"
            else
                args.scenarioName
            fName += " Retirement Scenario Calculations"
            saveFile2(fName)
        }
        if (gRetirementWorking?.getAssetListCount(AssetType.SAVINGS)!! > 0)
            binding.showSavingsDetailsButton.visibility = View.VISIBLE
        else
            binding.showSavingsDetailsButton.visibility = View.GONE
        if (gRetirementWorking?.getAssetListCount(AssetType.RRSP)!! > 0)
            binding.showRrspDetailsButton.visibility = View.VISIBLE
        else
            binding.showRrspDetailsButton.visibility = View.GONE
        if (gRetirementWorking?.getAssetListCount(AssetType.TFSA)!! > 0)
            binding.showTfsaDetailsButton.visibility = View.VISIBLE
        else
            binding.showTfsaDetailsButton.visibility = View.GONE
        if (gRetirementWorking?.getAssetListCount(AssetType.LIRA_LIF)!! +
            gRetirementWorking?.getAssetListCount(AssetType.LIRA_ANNUITY)!! > 0)
            binding.showLiraDetailsButton.visibility = View.VISIBLE
        else
            binding.showLiraDetailsButton.visibility = View.GONE
        if (gRetirementWorking?.getAssetListCount(AssetType.PROPERTY)!! > 0)
            binding.showPropertyDetailsButton.visibility = View.VISIBLE
        else
            binding.showPropertyDetailsButton.visibility = View.GONE
        if (gRetirementWorking?.getPensionListCount()!! > 0)
            binding.showPensionDetailsButton.visibility = View.VISIBLE
        else
            binding.showPensionDetailsButton.visibility = View.GONE
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSettings.setOnClickListener {
            onViewClicked()
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
                getString(R.string.show_lira_details) -> RetirementDetailsViews.LIRA
                getString(R.string.show_savings_details) -> RetirementDetailsViews.SAVINGS
                getString(R.string.show_property_details) -> RetirementDetailsViews.PROPERTY
                else -> RetirementDetailsViews.ALL
            }
            binding.currentViewTitle.text = String.format(getString(R.string.bracketed), radioButton.text.toString())
            if (DefaultsViewModel.getDefaultViewInRetirementDetails() != whichView)
                DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_IN_RETIREMENT_DETAILS, whichView.ordinal)
            createTableHeader(whichView)
            createTableRows(whichView)
        }
        when (DefaultsViewModel.getDefaultViewInRetirementDetails()) {
            RetirementDetailsViews.SUMMARY -> binding.showSummaryColumnsButton.isChecked = true
            RetirementDetailsViews.INCOME -> binding.showIncomeDetailsButton.isChecked = true
            RetirementDetailsViews.PENSION -> binding.showPensionDetailsButton.isChecked = true
            RetirementDetailsViews.TAX -> binding.showTaxColumnsButton.isChecked = true
            RetirementDetailsViews.RRSP -> binding.showRrspDetailsButton.isChecked = true
            RetirementDetailsViews.TFSA -> binding.showTfsaDetailsButton.isChecked = true
            RetirementDetailsViews.LIRA -> binding.showLiraDetailsButton.isChecked = true
            RetirementDetailsViews.SAVINGS -> binding.showSavingsDetailsButton.isChecked = true
            RetirementDetailsViews.PROPERTY -> binding.showPropertyDetailsButton.isChecked = true
            else -> binding.showAllColumnsButton.isChecked = true
        }
        binding.viewRadioGroup.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeBottom() {
                super.onSwipeBottom()
                binding.viewRadioGroup.visibility = View.GONE
                binding.settingsButtonLinearLayout.visibility = View.VISIBLE
            }
        })
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

    private fun onViewClicked() {
        if (binding.viewRadioGroup.visibility == View.GONE) { // ie expand the section
            binding.viewRadioGroup.visibility = View.VISIBLE
            binding.settingsButtonLinearLayout.visibility = View.GONE
        } else { // ie retract the section
            binding.viewRadioGroup.visibility = View.GONE
            binding.settingsButtonLinearLayout.visibility = View.VISIBLE
        }
    }

    private fun createTableHeader(iWhichView: RetirementDetailsViews) {
        binding.retirementTableRows.removeAllViews()
        val tr = TableRow(requireContext())
        val hexColor = MaterialColors.getColor(
            requireContext(),
            R.attr.colorSecondaryVariant,
            Color.BLACK
        )
        tr.setBackgroundColor(hexColor)
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
            addHeaderCell(tr, String.format(getString(R.string.s_pension),
                getString(R.string.cpp)), true)
        }
        var minAlreadyShown = false
        gRetirementDetailsList[1].pensionIncomes.forEach {
            if (iWhichView == RetirementDetailsViews.ALL ||
                iWhichView == RetirementDetailsViews.INCOME ||
                iWhichView == RetirementDetailsViews.PENSION ||
                iWhichView == RetirementDetailsViews.TAX)
                addHeaderCell(tr, String.format(getString(R.string.s_pension),
                    it.name), true)
        }
        if (iWhichView == RetirementDetailsViews.ALL ||
            iWhichView == RetirementDetailsViews.INCOME ||
            iWhichView == RetirementDetailsViews.TAX) {
            addHeaderCell(tr, getString(R.string.oas), true)
        }
        gRetirementDetailsList[1].assetIncomes.forEach {
            when (it.assetType) {
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
                        addHeaderCell(tr, String.format(getString(R.string.s_withdrawal),
                            getString(R.string.rrsp) + ": " + it.name), true)
                    }
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.RRSP)
                        addHeaderCell(tr, String.format(getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.rrsp) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.RRSP)
                        addHeaderCell(tr, String.format(getString(R.string.s_balance),
                            getString(R.string.rrsp) + ": " + it.name), true)
                }
                AssetType.LIRA_LIF, AssetType.LIRA_ANNUITY -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.LIRA)
                        addHeaderCell(tr, String.format(getString(R.string.s_withdrawal),
                            getString(R.string.lira) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.LIRA)
                        addHeaderCell(tr, String.format(getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.lira) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.LIRA)
                        addHeaderCell(tr, String.format(getString(R.string.s_balance),
                            getString(R.string.lira) + ": " + it.name), true)
                }
                AssetType.TFSA -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.TFSA)
                        addHeaderCell(tr, String.format(getString(R.string.s_withdrawal),
                            getString(R.string.tfsa) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.TFSA)
                        addHeaderCell(tr, String.format(getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.tfsa) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.TFSA)
                    addHeaderCell(tr, String.format(getString(R.string.s_balance),
                        getString(R.string.tfsa) + ": " + it.name), true)
                }
                AssetType.SAVINGS -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.SAVINGS)
                        addHeaderCell(tr, String.format(getString(R.string.s_withdrawal),
                            getString(R.string.savings) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SAVINGS ||
                        (iWhichView == RetirementDetailsViews.TAX &&
                                it.growthIsTaxable()))
                        addHeaderCell(tr, String.format(getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.savings) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.SAVINGS)
                        addHeaderCell(tr, String.format(getString(R.string.s_balance),
                            getString(R.string.savings) + ": " + it.name), true)
                }
                AssetType.PROPERTY -> {
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.INCOME ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(getString(R.string.s_withdrawal),
                            getString(R.string.property) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(getString(R.string.s_growth),
                            it.getGrowthPct(),
                            getString(R.string.property) + ": " + it.name), true)
                    if (iWhichView == RetirementDetailsViews.ALL ||
                        iWhichView == RetirementDetailsViews.SUMMARY ||
                        iWhichView == RetirementDetailsViews.PROPERTY)
                        addHeaderCell(tr, String.format(getString(R.string.s_balance),
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
            val targetAnnualIncome = gRetirementDetailsList[i].getTotalTargetIncome()
            if (iWhichView == RetirementDetailsViews.ALL) {
                addHeaderCell(tr, targetAnnualIncome.toString())
            }
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
                    it.assetType == AssetType.RRSP) {
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
                    iWhichView.compareTo(it.assetType) ||
                    (iWhichView == RetirementDetailsViews.TAX &&
                            it.withdrawalIsTaxable())) {
                    if (it.withdrawalAmount > 0 && it.getEndingBalance() == 0) {
                        addHeaderCell(tr, it.withdrawalAmount.toString(), iBold = false, iRed = true)
                    } else {
                        addHeaderCell(tr, it.withdrawalAmount.toString())
                    }
                }
                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView.compareTo(it.assetType) ||
                    (iWhichView == RetirementDetailsViews.TAX &&
                            it.growthIsTaxable()))
                    if (it.withdrawalAmount > 0 && it.getEndingBalance() == 0) {
                        addHeaderCell(tr, (it.growthThisYear + it.additionalGrowthThisYear).toString(), iBold = false, iRed = true)
                    } else {
                        addHeaderCell(tr, (it.growthThisYear + it.additionalGrowthThisYear).toString())
                    }

                if (iWhichView == RetirementDetailsViews.ALL ||
                    iWhichView == RetirementDetailsViews.SUMMARY ||
                    iWhichView.compareTo(it.assetType)) {
                    if (it.assetType == AssetType.PROPERTY && (it as Property).soldInYear == gRetirementDetailsList[i].year) {
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
    private fun createSpreadsheet(service: Sheets, iFileName: String) {
        val spreadsheetMaker = SpreadsheetMaker()
        val spreadsheet: Spreadsheet = spreadsheetMaker.create(iFileName,
            "Sheet1",
            gRetirementDetailsList)
        GlobalScope.launch {
            service.spreadsheets().create(spreadsheet).execute()
        }
        MyApplication.displayToast(getString(R.string.creating_file))
    }

    private fun saveFile2(iFileName: String) {
        val scopes = listOf(SheetsScopes.SPREADSHEETS)
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = MyApplication.userAccount

        val jsonFactory = JacksonFactory.getDefaultInstance()
        // GoogleNetHttpTransport.newTrustedTransport()
        val httpTransport =  NetHttpTransport()
        val service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(getString(R.string.app_name))
            .build()
        createSpreadsheet(service, iFileName)
    }
/*    private fun saveFile(uri: Uri) {
            requireContext().contentResolver.openOutputStream(uri)?.writer()?.run {
                write("\"${getString(R.string.year)}\"")
                write(",\"${getString(R.string.target_income)}\"")
                write(",\"${getString(R.string.net_income)}\"")
                write(",\"${getString(R.string.maximum_taxable_income)}\"")
                write(",\"${getString(R.string.taxable_income)}\"")
                write(",\"${getString(R.string.gross_income)}\"")
                write(",\"${getString(R.string.tax)}\"")
                write(",\"${getString(R.string.salary)}\"")
                write(",\"${String.format(getString(R.string.s_pension), getString(R.string.cpp))}\"")
                gRetirementDetailsList[1].pensionIncomes.forEach {
                    write(",\"${String.format(getString(R.string.s_pension), it.name)}\"")
                }
                write(",\"${getString(R.string.oas)}\"")
                var minAlreadyShown = false
                gRetirementDetailsList[1].assetIncomes.forEach {
                    when (it.assetType) {
                        AssetType.RRSP -> {
                            if (!minAlreadyShown) {
                                write(",\"${getString(R.string.minimum_rrif_withdrawal)}\"")
                                minAlreadyShown = true
                            }
                            write(",\"${String.format(getString(R.string.s_withdrawal), getString(R.string.rrsp) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_growth), it.getGrowthPct(), getString(R.string.rrsp) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_balance), getString(R.string.rrsp) + ": " + it.name)}\"")
                        }
                        AssetType.LIRA_LIF, AssetType.LIRA_ANNUITY -> {
                            write(",\"${String.format(getString(R.string.s_withdrawal), getString(R.string.lira) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_growth), it.getGrowthPct(), getString(R.string.lira) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_balance), getString(R.string.lira) + ": " + it.name)}\"")
                        }
                        AssetType.TFSA -> {
                            write(",\"${String.format(getString(R.string.s_withdrawal), getString(R.string.tfsa) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_growth), it.getGrowthPct(), getString(R.string.tfsa) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_balance), getString(R.string.tfsa) + ": " + it.name)}\"")
                        }
                        AssetType.SAVINGS -> {
                            write(",\"${String.format(getString(R.string.s_withdrawal), getString(R.string.savings) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_growth),
                                it.getGrowthPct(),
                                getString(R.string.savings) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_balance), getString(R.string.savings) + ": " + it.name)}\"")
                        }
                        AssetType.PROPERTY -> {
                            write(",\"${String.format(getString(R.string.s_withdrawal), getString(R.string.property) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_growth),
                                it.getGrowthPct(),
                                getString(R.string.property) + ": " + it.name)}\"")
                            write(",\"${String.format(getString(R.string.s_balance), getString(R.string.property) + ": " + it.name)}\"")
                        }
                        else -> {} // do nothing
                    }
                }
                write(",\"${getString(R.string.net_worth)}\"")
                write("\n")
                for (i in 0 until gRetirementDetailsList.size) {
                    minAlreadyShown = false
                    write("\"${gRetirementDetailsList[i].year}\"")
                    write(",\"${gRetirementDetailsList[i].getTotalTargetIncome()}\"")
                    write(",\"${gRetirementDetailsList[i].getTotalAvailableIncome()}\"")
                    write(",\"${gRetirementDetailsList[i].getMaximumTaxableIncome()}\"")
                    write(",\"${gRetirementDetailsList[i].getTaxableIncome(gRetirementDetailsList[i].year)}\"")
                    write(",\"${gRetirementDetailsList[i].getTotalGrossIncome(gRetirementDetailsList[i].year)}\"")
                    write(",\"${gRetirementDetailsList[i].getTotalTax()}\"")
                    write(",\"${gRetirementDetailsList[i].getTotalSalary()}\"")
                    write(",\"${gRetirementDetailsList[i].cppIncome}\"")
                    gRetirementDetailsList[i].pensionIncomes.forEach {
                        write(",\"${it.getPensionIncome(gRetirementDetailsList[i].year)}\"")
                    }
                    write(",\"${gRetirementDetailsList[i].oasIncome}\"")
                    gRetirementDetailsList[i].assetIncomes.forEach {
                        if (it.assetType == AssetType.RRSP) {
                            if (!minAlreadyShown) {
                                write(",\"${gRetirementDetailsList[i].getMinimumRRIFWithdrawal(
                                    gRetirementDetailsList[i].getAgeAtStartOfYear(
                                        gRetirementDetailsList[i].year))}\"")
                                minAlreadyShown = true
                            }
                        }
                        write(",\"${it.withdrawalAmount}\"")
                        write(",\"${(it.growthThisYear + it.additionalGrowthThisYear)}\"")
                        write(",\"${it.getEndingBalance()}\"")
                    }
                    write(",\"${gRetirementDetailsList[i].getNetWorth()}\"")
                    write("\n")
                }
                flush()
                close()
            }
        } */
}
