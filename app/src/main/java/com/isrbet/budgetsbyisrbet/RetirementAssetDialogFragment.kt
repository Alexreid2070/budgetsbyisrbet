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
        private const val KEY_ASSET_ID = "2"
        private var userID: Int = 0
        private var assetID: Int = 0
        fun newInstance(
            userIDIn: Int,
            assetIDIn: Int
        ): RetirementAssetDialogFragment {
            val args = Bundle()

            args.putString(KEY_USER_ID, userIDIn.toString())
            args.putString(KEY_ASSET_ID, assetIDIn.toString())
            val fragment = RetirementAssetDialogFragment()
            fragment.arguments = args
            userID = userIDIn
            assetID = assetIDIn
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
        if (assetID == -1)
            binding.deleteButton.visibility = View.GONE
        val userDefault = RetirementViewModel.getUserDefault(userID)
        if (userDefault != null) {
            val asset = userDefault.assets.find {it.id == assetID}
            if (asset != null) {
                binding.title.text = AssetType.getText(asset.type) + ": " + asset.name
                setupAssetTypeSpinner(AssetType.getText(asset.type))
                binding.assetName.setText(asset.name)
                if (asset.type == AssetType.PROPERTY) {
                    val prop = asset as Property
                    binding.assetValue.setText((prop.value / (prop.ownershipPct / 100)).toInt().toString())
                } else
                    binding.assetValue.setText(asset.value.toString())
                binding.estimatedAnnualGrowth.setText(asset.estimatedGrowthPct.toString())
                binding.annualContribution.setText(asset.annualContribution.toString())
                if (asset.type == AssetType.PROPERTY) {
                    val prop = asset as Property
                    binding.propertyFields.visibility = View.VISIBLE
                    binding.estimatedAnnualGrowthAfterSale.setText(prop.estimatedGrowthPctAsSavings.toString())
                    binding.willSellToFinanceRetirement.isChecked = prop.willSellToFinanceRetirement
                    binding.ownershipPercentage.setText(prop.ownershipPct.toString())
                    binding.mortgageDetails.setText(prop.scheduledPaymentName)
                }
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
                binding.title.text = selection.toString() + ": " + binding.assetName.text.toString()
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
            val userDefault = RetirementViewModel.getUserDefault(userID)
            if (userDefault != null) {
                val cal = android.icu.util.Calendar.getInstance()
                val oldAsset = userDefault.assets.find { it.id == assetID }
                val distributionOrder = oldAsset?.distributionOrder ?: userDefault.assets.count()

                var asset: Asset? = null
                when (binding.assetTypeSpinner.selectedItem.toString()) {
                    AssetType.getText(AssetType.RRSP) -> {
                        asset = RRSP(
                            userDefault.assets.size,
                            binding.assetName.text.toString(),
                            binding.assetValue.text.toString().toInt(),
                            binding.estimatedAnnualGrowth.text.toString().toDouble(),
                            if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                            12 - cal.get(Calendar.MONTH) - 1,
                            distributionOrder
                        )
                    }
                    AssetType.getText(AssetType.TFSA) -> {
                        asset = TFSA(
                            userDefault.assets.size,
                            binding.assetName.text.toString(),
                            binding.assetValue.text.toString().toInt(),
                            binding.estimatedAnnualGrowth.text.toString().toDouble(),
                            if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                            12 - cal.get(Calendar.MONTH) - 1,
                            distributionOrder
                        )
                    }
                    AssetType.getText(AssetType.SAVINGS) -> {
                        asset = Savings(
                            userDefault.assets.size,
                            binding.assetName.text.toString(),
                            binding.assetValue.text.toString().toInt(),
                            binding.estimatedAnnualGrowth.text.toString().toDouble(),
                            if (binding.annualContribution.text.toString() == "") 0 else binding.annualContribution.text.toString().toInt(),
                            12 - cal.get(Calendar.MONTH) - 1,
                            distributionOrder
                        )
                    }
                    AssetType.getText(AssetType.PROPERTY) -> {
                        asset = Property(
                            userDefault.assets.size,
                            binding.assetName.text.toString(),
                            binding.assetValue.text.toString().toInt(),
                            binding.estimatedAnnualGrowth.text.toString().toDouble(),
                            0,
                            distributionOrder,
                            binding.estimatedAnnualGrowthAfterSale.text.toString().toDouble(),
                            binding.willSellToFinanceRetirement.isChecked,
                            binding.mortgageDetails.text.toString(),
                            binding.ownershipPercentage.text.toString().toDouble(),
                            0
                        )
                    }
                    else -> {}
                }
                if (asset != null) {
                    if (assetID == -1) { // adding new
                        userDefault.addAsset(asset)
                        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                        if (listener != null)
                            listener?.onNewDataSaved()
                        dismiss()
                    } else { // editing existing asset
                        userDefault.updateAsset(assetID, asset)
                        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                            if (listener != null)
                                listener?.onNewDataSaved()
                        dismiss()
                    }
                }
            }
        }
        binding.deleteButton.setOnClickListener {
            fun yesClicked() {
                val userDefault = RetirementViewModel.getUserDefault(userID)
                if (userDefault != null) {
                    userDefault.deleteAsset(assetID)
                    if (listener != null) {
                        listener?.onNewDataSaved()
                    }
                    MyApplication.playSound(context, R.raw.short_springy_gun)
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