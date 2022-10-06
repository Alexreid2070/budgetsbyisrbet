package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
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

        setupAssetTypeSpinner()
        setupClickListeners()
        if (assetName == "")
            binding.deleteButton.visibility = View.GONE
        val userDefault = RetirementViewModel.getUserDefault(userID)
        if (userDefault != null) {
            val asset = RetirementViewModel.getWorkingAsset(assetName)
            if (asset != null) {
                binding.title.text = String.format("${AssetType.getText(asset.type)}: ${asset.name}")
                setupAssetTypeSpinner(AssetType.getText(asset.type))
                binding.assetName.setText(asset.name)
                if (asset.type == AssetType.PROPERTY) {
                    val prop = asset as Property
                    binding.assetValue.setText((prop.value / (prop.ownershipPct / 100)).toInt().toString())
                } else
                    binding.assetValue.setText(asset.value.toString())
                binding.switchUseDefaultGrowth.isChecked = asset.useDefaultGrowthPct
                binding.estimatedAnnualGrowth.isEnabled = !binding.switchUseDefaultGrowth.isChecked
                if (asset.type == AssetType.RRSP) {
                    binding.switchWillSellToFinanceRetirement.isChecked = true
                } else {
                    binding.switchWillSellToFinanceRetirement.isChecked =
                        asset.willSellToFinanceRetirement
                }
                if (asset.type == AssetType.SAVINGS) {
                    binding.taxShelteredLayout.visibility = View.VISIBLE
                    binding.switchTaxSheltered.isChecked = (asset as Savings).taxSheltered
                } else {
                    binding.taxShelteredLayout.visibility = View.GONE
                }
                if (binding.switchUseDefaultGrowth.isChecked) {
                    if (asset.type == AssetType.PROPERTY)
                        binding.estimatedAnnualGrowth.setText(userDefault.propertyGrowthRate.toString())
                    else
                        binding.estimatedAnnualGrowth.setText(userDefault.investmentGrowthRate.toString())
                } else {
                    binding.estimatedAnnualGrowth.setText(asset.estimatedGrowthPct.toString())
                }
                binding.annualContribution.setText(asset.annualContribution.toString())
                if (asset.type == AssetType.PROPERTY) {
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
                }
                else if (asset.type == AssetType.RRSP) {
                    val rrsp = asset as RRSP
                    binding.minimizeRRSPTaxLayout.visibility = View.VISIBLE
                    when (rrsp.minimizeTax) {
                        MinimizeTaxEnum.DO_NOT_MINIMIZE -> binding.minimizeRRSPNever.isChecked = true
                        MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE -> binding.minimizeRRSPIfPossible.isChecked = true
                        else -> binding.minimizeRRSPAlways.isChecked = true
                    }
                } else if (asset.type == AssetType.SAVINGS) {
                    binding.taxShelteredLayout.visibility = View.VISIBLE
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
                if (selection == AssetType.getText(AssetType.PROPERTY)) {
                    binding.propertyFields.visibility = View.VISIBLE
                    binding.annualContributionLayout.visibility = View.GONE
                } else {
                    binding.annualContributionLayout.visibility = View.VISIBLE
                    binding.propertyFields.visibility = View.GONE
                }
                if (selection == AssetType.getText(AssetType.RRSP)) {
                    binding.switchWillSellToFinanceRetirement.isEnabled = false
                    binding.switchWillSellToFinanceRetirement.isChecked = true
                    binding.minimizeRRSPTaxLayout.visibility = View.VISIBLE
                } else {
                    if (inDefaultMode)
                        binding.switchWillSellToFinanceRetirement.isEnabled = true
                    binding.minimizeRRSPTaxLayout.visibility = View.GONE
                }
                if (selection == AssetType.getText(AssetType.SAVINGS))
                    binding.taxShelteredLayout.visibility = View.VISIBLE
                else
                    binding.taxShelteredLayout.visibility = View.GONE
                setDefaultGrowthRates()
                binding.title.text = String.format("$selection: ${binding.assetName.text.toString()}")
            }
        }
        if (false) { //(!inDefaultMode) {
            binding.assetTypeSpinner.isEnabled = false
            binding.assetName.isEnabled = false
            binding.assetValue.isEnabled = false
            binding.switchUseDefaultGrowth.isEnabled = false
            binding.switchUseDefaultGrowthAfterSale.isEnabled = false
            binding.switchWillSellToFinanceRetirement.isEnabled = false
            binding.estimatedAnnualGrowth.isEnabled = false
            binding.estimatedAnnualGrowthAfterSale.isEnabled = false
            binding.annualContribution.isEnabled = false
            binding.ownershipPercentage.isEnabled = false
            binding.mortgageDetails.isEnabled = false
            binding.minimizeRRSPRadioGroup.isEnabled = false
            binding.switchTaxSheltered.isEnabled = false
            binding.saveButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
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
            if (RetirementViewModel.getWorkingAsset(binding.assetName.text.toString()) == null) {
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
            }
            val cal = android.icu.util.Calendar.getInstance()
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
                        12 - cal.get(Calendar.MONTH) - 1,
                        distributionOrder,
                        0,
                        minRRSPVal
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
                        12 - cal.get(Calendar.MONTH) - 1,
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
                        12 - cal.get(Calendar.MONTH) - 1,
                        distributionOrder,
                        binding.switchTaxSheltered.isChecked
                    )
                }
                AssetType.getText(AssetType.PROPERTY) -> {
                    asset = Property(
                        RetirementViewModel.getWorkingAssetListCount(),
                        binding.assetName.text.toString(),
                        binding.assetValue.text.toString().toInt(),
                        binding.switchUseDefaultGrowth.isChecked,
                        binding.estimatedAnnualGrowth.text.toString().toDouble(),
                        binding.switchWillSellToFinanceRetirement.isChecked,
                        0,
                        distributionOrder,
                        binding.switchUseDefaultGrowthAfterSale.isChecked,
                        binding.estimatedAnnualGrowthAfterSale.text.toString().toDouble(),
                        binding.mortgageDetails.text.toString(),
                        binding.ownershipPercentage.text.toString().toDouble(),
                        0
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