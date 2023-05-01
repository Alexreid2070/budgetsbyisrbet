package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementAssetDialogBinding
import java.util.*

class RetirementAssetDialogFragment : DialogFragment() {
    interface RetirementAssetDialogFragmentListener {
        fun onNewDataSaved()
    }

    private var listener: RetirementAssetDialogFragmentListener? = null
    private var _binding: FragmentRetirementAssetDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_USER_ID = "1"
        private const val KEY_ASSET_NAME = "2"
        private const val KEY_DEFAULT_MODE_ID = "3"
        private var userID: Int = 0
        private var assetName: String = ""
        private var inDefaultMode: Boolean = false
        fun newInstance(
            userIDIn: Int,
            assetNameIn: String,
            defaultModeIn: Boolean
        ): RetirementAssetDialogFragment {
            val args = Bundle()

            args.putString(KEY_USER_ID, userIDIn.toString())
            args.putString(KEY_ASSET_NAME, assetNameIn)
            args.putString(KEY_DEFAULT_MODE_ID, defaultModeIn.toString())
            val fragment = RetirementAssetDialogFragment()
            fragment.arguments = args
            userID = userIDIn
            assetName = assetNameIn
            inDefaultMode = defaultModeIn
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRetirementAssetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.annuityStartDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.annuityStartDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.annuityStartDate.text.toString() != "") {
                lcal = MyDate(binding.annuityStartDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }

        setupAssetTypeSpinner()
        setupClickListeners()
        if (assetName == "")
            binding.deleteButton.visibility = View.GONE
        val userDefault = RetirementViewModel.getUserDefault(userID)
        if (userDefault != null) {
            val asset = RetirementViewModel.getWorkingAsset(assetName)
            if (asset != null) {
                binding.title.text = String.format("${AssetType.getText(asset.assetType)}: ${asset.name}")
                setupAssetTypeSpinner(AssetType.getText(asset.assetType))
                binding.assetName.setText(asset.name)
                if (asset.assetType == AssetType.PROPERTY) {
                    val prop = asset as Property
                    binding.assetValue.setText((prop.getValue() / (prop.ownershipPct / 100)).toInt().toString())
                } else
                    binding.assetValue.setText(asset.getValue().toString())
                binding.switchUseDefaultGrowth.isChecked = asset.useDefaultGrowthPct
                binding.estimatedAnnualGrowth.isEnabled = !binding.switchUseDefaultGrowth.isChecked
                if (asset.assetType == AssetType.RRSP) {
                    binding.switchWillSellToFinanceRetirement.isChecked = true
                } else {
                    binding.switchWillSellToFinanceRetirement.isChecked =
                        asset.willSellToFinanceRetirement
                }
                if (asset.assetType == AssetType.SAVINGS) {
                    binding.switchTaxSheltered.visibility = View.VISIBLE
                    binding.switchTaxSheltered.isChecked = (asset as Savings).taxSheltered
                } else {
                    binding.switchTaxSheltered.visibility = View.GONE
                }
                if (binding.switchUseDefaultGrowth.isChecked) {
                    if (asset.assetType == AssetType.PROPERTY)
                        binding.estimatedAnnualGrowth.setText(userDefault.propertyGrowthRate.toString())
                    else
                        binding.estimatedAnnualGrowth.setText(userDefault.investmentGrowthRate.toString())
                } else {
                    binding.estimatedAnnualGrowth.setText(asset.estimatedGrowthPct.toString())
                }
                binding.annualContribution.setText(asset.annualContribution.toString())
                when (asset.assetType) {
                    AssetType.PROPERTY -> {
                        val prop = asset as Property
                        binding.propertyFields.visibility = View.VISIBLE
                        binding.switchUseDefaultGrowthAfterSale.isChecked = asset.useDefaultGrowthPctAsSavings
                        binding.estimatedAnnualGrowthAfterSale.isEnabled = !binding.switchUseDefaultGrowthAfterSale.isChecked
                        if (binding.switchUseDefaultGrowthAfterSale.isChecked) {
                            binding.estimatedAnnualGrowthAfterSale.setText(userDefault.investmentGrowthRate.toString())
                        } else {
                            binding.estimatedAnnualGrowthAfterSale.setText(asset.estimatedGrowthPctAsSavings.toString())
                        }
                        binding.ownershipPercentage.setText(prop.ownershipPct.toString())
                        binding.mortgageDetails.setText(prop.scheduledPaymentName)
                        binding.switchPrimaryResidence.isChecked = prop.primaryResidence
                        binding.switchWillSellToFinanceRetirement.isChecked = prop.willSellToFinanceRetirement
                        binding.increasedBudget.setText(prop.increasedBudget.toString())
                    }
                    AssetType.RRSP -> {
                        val rrsp = asset as RRSP
                        binding.minimizeRRSPTaxLayout.visibility = View.VISIBLE
                        when (rrsp.minimizeTax) {
                            MinimizeTaxEnum.DO_NOT_MINIMIZE -> binding.minimizeRRSPNever.isChecked = true
                            MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE -> binding.minimizeRRSPIfPossible.isChecked = true
                            else -> binding.minimizeRRSPAlways.isChecked = true
                        }
                    }
                    AssetType.LIRA_LIF -> {
                        val liraLif = asset as LIRALIF
                        binding.minimizeRRSPTaxLayout.visibility = View.VISIBLE
                        when (liraLif.minimizeTax) {
                            MinimizeTaxEnum.DO_NOT_MINIMIZE -> binding.minimizeRRSPNever.isChecked = true
                            MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE -> binding.minimizeRRSPIfPossible.isChecked = true
                            else -> binding.minimizeRRSPAlways.isChecked = true
                        }
                    }
                    AssetType.LIRA_ANNUITY -> {
                        binding.annuityStartDateLayout.visibility = View.VISIBLE
                        binding.annualAmountLayout.visibility = View.VISIBLE
                        binding.annuityStartDate.setText((asset as LIRAANNUITY).pensionStartDate)
                        binding.annualAnnuityAmount.setText(asset.annualAmount.toString())
                    }
                    AssetType.SAVINGS -> {
                        binding.switchTaxSheltered.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            } else {
                setDefaultGrowthRates()
            }
        }

        binding.assetTypeSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                when (selection) {
                    AssetType.getText(AssetType.PROPERTY) -> {
                        binding.propertyFields.visibility = View.VISIBLE
                        binding.annualContributionLayout.visibility = View.GONE
                    }
                    AssetType.getText(AssetType.LIRA_LIF), AssetType.getText(AssetType.LIRA_ANNUITY) -> {
                        binding.annualContributionLayout.visibility = View.GONE
                        binding.propertyFields.visibility = View.GONE
                    }
                    else -> {
                        binding.annualContributionLayout.visibility = View.VISIBLE
                        binding.propertyFields.visibility = View.GONE
                    }
                }
                when (selection) {
                    AssetType.getText(AssetType.RRSP), AssetType.getText(AssetType.LIRA_LIF) -> {
                        binding.switchWillSellToFinanceRetirement.visibility = View.GONE
                        binding.switchWillSellToFinanceRetirement.isChecked = true
                        binding.minimizeRRSPTaxLayout.visibility = View.VISIBLE
                    }
                    AssetType.getText(AssetType.LIRA_ANNUITY) -> {
                        binding.switchWillSellToFinanceRetirement.visibility = View.GONE
                        binding.switchWillSellToFinanceRetirement.isChecked = true
                        binding.minimizeRRSPTaxLayout.visibility = View.GONE
                    }
                    else -> {
                        if (inDefaultMode)
                            binding.switchWillSellToFinanceRetirement.visibility = View.VISIBLE
                        binding.minimizeRRSPTaxLayout.visibility = View.GONE
                    }
                }
                if (selection == AssetType.getText(AssetType.LIRA_ANNUITY)) {
                    binding.annualAmountLayout.visibility = View.VISIBLE
                    binding.annuityStartDateLayout.visibility = View.VISIBLE
                } else {
                    binding.annualAmountLayout.visibility = View.GONE
                    binding.annuityStartDateLayout.visibility = View.GONE
                }
                if (selection == AssetType.getText(AssetType.SAVINGS))
                    binding.switchTaxSheltered.visibility = View.VISIBLE
                else
                    binding.switchTaxSheltered.visibility = View.GONE
                setDefaultGrowthRates()
                binding.title.text = String.format("$selection: ${binding.assetName.text.toString()}")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setDefaultGrowthRates() {
        val userDefault = RetirementViewModel.getUserDefault(userID)
        if (binding.switchUseDefaultGrowth.isChecked) {
            if (binding.assetTypeSpinner.selectedItem.toString() == AssetType.getText(AssetType.PROPERTY))
                binding.estimatedAnnualGrowth.setText(userDefault?.propertyGrowthRate.toString())
            else
                binding.estimatedAnnualGrowth.setText(userDefault?.investmentGrowthRate.toString())
            binding.estimatedAnnualGrowth.isEnabled = false
        }
        if (binding.assetTypeSpinner.selectedItem.toString() == AssetType.getText(AssetType.PROPERTY)) {
            if (binding.switchUseDefaultGrowthAfterSale.isChecked) {
                binding.estimatedAnnualGrowthAfterSale.setText(userDefault?.investmentGrowthRate.toString())
                binding.estimatedAnnualGrowthAfterSale.isEnabled = false
            }
        }
    }

    private fun setupAssetTypeSpinner(iSelection: String = "") {
        val assetTypeList: MutableList<String> = ArrayList()
        assetTypeList.add(AssetType.getText(AssetType.RRSP))
        assetTypeList.add(AssetType.getText(AssetType.LIRA_LIF))
        assetTypeList.add(AssetType.getText(AssetType.LIRA_ANNUITY))
        assetTypeList.add(AssetType.getText(AssetType.TFSA))
        assetTypeList.add(AssetType.getText(AssetType.SAVINGS))
        assetTypeList.add(AssetType.getText(AssetType.PROPERTY))
        val assetArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            assetTypeList
        )
        binding.assetTypeSpinner.adapter = assetArrayAdapter
        if (iSelection == "")
            binding.assetTypeSpinner.setSelection(0)
        else
            binding.assetTypeSpinner.setSelection(assetArrayAdapter.getPosition(iSelection))
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.assetTypeSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.assetTypeSpinner.setPopupBackgroundResource(R.drawable.spinner)
        assetArrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.switchUseDefaultGrowth.setOnClickListener {
            if (binding.switchUseDefaultGrowth.isChecked) {
                binding.estimatedAnnualGrowth.isEnabled = false
                val userDefault = RetirementViewModel.getUserDefault(userID)
                if (binding.assetTypeSpinner.selectedItem.toString() == AssetType.getText(AssetType.PROPERTY))
                    binding.estimatedAnnualGrowth.setText(userDefault?.propertyGrowthRate.toString())
                else
                    binding.estimatedAnnualGrowth.setText(userDefault?.investmentGrowthRate.toString())
            } else {
                binding.estimatedAnnualGrowth.isEnabled = true
                binding.estimatedAnnualGrowth.setText("")
            }
        }
        binding.switchUseDefaultGrowthAfterSale.setOnClickListener {
            if (binding.switchUseDefaultGrowthAfterSale.isChecked) {
                binding.estimatedAnnualGrowthAfterSale.isEnabled = false
                val userDefault = RetirementViewModel.getUserDefault(userID)
                binding.estimatedAnnualGrowthAfterSale.setText(userDefault?.investmentGrowthRate.toString())
            } else {
                binding.estimatedAnnualGrowthAfterSale.isEnabled = true
                binding.estimatedAnnualGrowthAfterSale.setText("")
            }
        }
        binding.switchWillSellToFinanceRetirement.setOnClickListener {
            if (binding.switchWillSellToFinanceRetirement.isChecked) {
                binding.increasedBudgetLayout.visibility = View.VISIBLE
            } else {
                binding.increasedBudget.setText("")
                binding.increasedBudgetLayout.visibility = View.GONE
            }
        }
        binding.saveButton.setOnClickListener {
            if (!textIsSafeForKey(binding.assetName.text.toString())) {
                binding.assetName.error = getString(R.string.field_has_invalid_character)
                focusAndOpenSoftKeyboard(requireContext(), binding.assetName)
                return@setOnClickListener
            }
            if (binding.assetName.text.toString() == "") {
                binding.assetName.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.assetName)
                return@setOnClickListener
            }
            if (assetName == "" && RetirementViewModel.getWorkingAsset(binding.assetName.text.toString()) != null) {
                binding.assetName.error = getString(R.string.name_already_exists)
                focusAndOpenSoftKeyboard(requireContext(), binding.assetName)
                return@setOnClickListener
            }
            if (binding.estimatedAnnualGrowth.text.toString() == "") {
                binding.estimatedAnnualGrowth.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.estimatedAnnualGrowth)
                return@setOnClickListener
            }
            if (binding.assetValue.text.toString() == "") {
                binding.assetValue.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.assetValue)
                return@setOnClickListener
            }
            if (binding.assetTypeSpinner.selectedItem.toString() == AssetType.getText(AssetType.PROPERTY)) {
                if (binding.estimatedAnnualGrowthAfterSale.text.toString() == "") {
                    binding.estimatedAnnualGrowthAfterSale.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.estimatedAnnualGrowthAfterSale)
                    return@setOnClickListener
                }
                if (binding.ownershipPercentage.text.toString() == "") {
                    binding.ownershipPercentage.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.ownershipPercentage)
                    return@setOnClickListener
                }
                if (binding.switchWillSellToFinanceRetirement.isChecked &&
                        binding.increasedBudget.text.toString().toInt() <= 0) {
                    binding.increasedBudget.error = getString(R.string.value_cannot_be_zero)
                    focusAndOpenSoftKeyboard(requireContext(), binding.increasedBudget)
                    return@setOnClickListener
                }
            }
            val oldAsset = RetirementViewModel.getWorkingAsset(assetName)
            val distributionOrder = oldAsset?.distributionOrder ?: RetirementViewModel.getWorkingAssetListCount()

            var asset: Asset? = null
            when (binding.assetTypeSpinner.selectedItem.toString()) {
                AssetType.getText(AssetType.RRSP) -> {
                    val minRRSPVal = if (binding.minimizeRRSPNever.isChecked)
                        MinimizeTaxEnum.DO_NOT_MINIMIZE
                    else if (binding.minimizeRRSPIfPossible.isChecked)
                        MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE
                    else
                        MinimizeTaxEnum.ALWAYS_MINIMIZE

                    asset = RRSP(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                        12 - gCurrentDate.getMonth(),
                        distributionOrder,
                        0,
                        minRRSPVal
                    )
                }
                AssetType.getText(AssetType.LIRA_LIF) -> {
                    val minTaxVal = if (binding.minimizeRRSPNever.isChecked)
                        MinimizeTaxEnum.DO_NOT_MINIMIZE
                    else if (binding.minimizeRRSPIfPossible.isChecked)
                        MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE
                    else
                        MinimizeTaxEnum.ALWAYS_MINIMIZE
                    asset = LIRALIF(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                        12 - gCurrentDate.getMonth(),
                        distributionOrder,
                        0,
                        minTaxVal
                    )
                }
                AssetType.getText(AssetType.LIRA_ANNUITY) -> {
                    asset = LIRAANNUITY(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.annuityStartDate.text.toString(),
                        gCurrentDate.getYear(),
                        binding.annualAnnuityAmount.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        12 - gCurrentDate.getMonth(),
                        distributionOrder
                    )
                }
                AssetType.getText(AssetType.TFSA) -> {
                    asset = TFSA(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                        binding.switchWillSellToFinanceRetirement.isChecked,
                        12 - gCurrentDate.getMonth(),
                        distributionOrder
                    )
                }
                AssetType.getText(AssetType.SAVINGS) -> {
                    asset = Savings(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                        binding.switchWillSellToFinanceRetirement.isChecked,
                        12 - gCurrentDate.getMonth(),
                        distributionOrder,
                        binding.switchTaxSheltered.isChecked
                    )
                }
                AssetType.getText(AssetType.PROPERTY) -> {
                    asset = Property(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        (binding.assetValue.text.toString().toInt() * binding.ownershipPercentage.text.toString().toDouble() / 100.0).toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        binding.switchWillSellToFinanceRetirement.isChecked,
                        0,
                        distributionOrder,
                        if (binding.increasedBudget.text.toString() == "") 0 else binding.increasedBudget.text.toString().toInt(),
                        binding.switchUseDefaultGrowthAfterSale.isChecked,
                        binding.estimatedAnnualGrowthAfterSale.text.toString().toDouble(),
                        binding.mortgageDetails.text.toString(),
                        binding.ownershipPercentage.text.toString().toDouble(),
                        0,
                        binding.switchPrimaryResidence.isChecked
                    )
                }
                else -> {}
            }
            if (asset != null) {
                if (assetName == "") { // adding new
                    RetirementViewModel.addAssetToWorkingList(asset)
                } else { // editing existing asset
                    RetirementViewModel.updateAssetInWorkingList(assetName, asset)
                }
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            }
        }
        binding.deleteButton.setOnClickListener {
            fun yesClicked() {
                RetirementViewModel.deleteAssetFromWorkingList(assetName)
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            }

            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                            binding.assetName.text.toString())
                )
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setRetirementAssetDialogFragmentListener(listener: RetirementAssetDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}