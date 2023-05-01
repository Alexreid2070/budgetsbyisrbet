package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementAdditionalDialogBinding
import java.util.*

enum class AdditionalType(val code: Int) {
    DEPOSIT(1),
    EXPENSE(2);
}

class RetirementAdditionalDialogFragment : DialogFragment() {
    interface RetirementAdditionalDialogFragmentListener {
        fun onNewDataSaved()
    }

    private var listener: RetirementAdditionalDialogFragmentListener? = null
    private var _binding: FragmentRetirementAdditionalDialogBinding? = null
    private val binding get() = _binding!!
    private var myScenario: RetirementData? = null

    companion object {
        private const val KEY_ID = "1"
        private const val KEY_SCENARIO_TYPE = "2"
        private var itemID: Int = -1
        private var scenarioType: RetirementScenarioType = RetirementScenarioType.SCENARIO
        fun newInstance(
            itemIDIn: Int,
            scenarioTypeIn: RetirementScenarioType
        ): RetirementAdditionalDialogFragment {
            val args = Bundle()

            args.putString(KEY_ID, itemIDIn.toString())
            args.putInt(KEY_SCENARIO_TYPE, scenarioTypeIn.code)
            val fragment = RetirementAdditionalDialogFragment()
            fragment.arguments = args
            itemID = itemIDIn
            scenarioType = scenarioTypeIn
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        myScenario = if (scenarioType == RetirementScenarioType.SCENARIO)
            gRetirementWorking
        else
            gRetirementDefaults

        _binding = FragmentRetirementAdditionalDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAssetNameSpinner()
        setupClickListeners()
        if (itemID == -1)
            binding.deleteButton.visibility = View.GONE
        val item = myScenario?.getAdditionalItem(itemID)
        if (item != null) {
            if (item.type == AdditionalType.DEPOSIT) {
                binding.depositButton.isChecked = true
                binding.title.text = getString(R.string.additional_deposit)

            } else {
                binding.expenseButton.isChecked = true
                binding.title.text = getString(R.string.additional_expense)
            }
            binding.name.setText(item.name)
            binding.year.setText(item.year.toString())
            binding.amount.setText(item.amount.toString())
            if (item.type == AdditionalType.DEPOSIT) {
                binding.assetNameLayout.visibility = View.VISIBLE
                setupAssetNameSpinner(item.assetName)
            } else {
                binding.assetNameLayout.visibility = View.GONE
            }
        } else {
            setFieldsAccordingToType(AdditionalType.DEPOSIT)
        }

        binding.typeRadioGroup.setOnCheckedChangeListener { _, _ ->
            if (binding.depositButton.isChecked) {
                setFieldsAccordingToType(AdditionalType.DEPOSIT)
            } else {
                setFieldsAccordingToType(AdditionalType.EXPENSE)
            }
        }
    }

    private fun setFieldsAccordingToType(iType: AdditionalType) {
        if (iType == AdditionalType.DEPOSIT) {
            binding.assetNameLayout.visibility = View.VISIBLE
            setupAssetNameSpinner("")
            binding.title.text = getString(R.string.edit_deposit)
        } else {
            binding.assetNameLayout.visibility = View.GONE
            binding.title.text = getString(R.string.edit_expense)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupAssetNameSpinner(iSelection: String = "") {
        val assetNameList: MutableList<String> = ArrayList()
        for (i in 0 until myScenario?.getAssetListCount()!!) {
            val asset = myScenario?.getAsset(i)
            if (asset != null)
                assetNameList.add(asset.name)
        }
        val assetNameAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            assetNameList
        )
        binding.assetNameSpinner.adapter = assetNameAdapter
        if (iSelection == "")
            binding.assetNameSpinner.setSelection(0)
        else
            binding.assetNameSpinner.setSelection(assetNameAdapter.getPosition(iSelection))
        assetNameAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            if (binding.name.text.toString() == "") {
                binding.year.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.name)
                return@setOnClickListener
            }
            if (binding.year.text.toString() == "") {
                binding.year.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.year)
                return@setOnClickListener
            }
            if (binding.amount.text.toString() == "") {
                binding.amount.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.amount)
                return@setOnClickListener
            }

            val additionalItem = AdditionalItem(
                0,
                AdditionalType.DEPOSIT,
                binding.name.text.toString(),
                binding.year.text.toString().toInt(),
                binding.amount.text.toString().toInt(),
                binding.assetNameSpinner.selectedItem.toString())

            if (binding.expenseButton.isChecked)
                additionalItem.type = AdditionalType.EXPENSE
            if (itemID == -1) { // adding new
                myScenario?.addAdditionalItem(additionalItem)
            } else { // editing existing
                myScenario?.updateAdditionalItem(itemID, additionalItem)
            }
            if (listener != null)
                listener?.onNewDataSaved()
            dismiss()
        }
        binding.deleteButton.setOnClickListener {
            fun yesClicked() {
                myScenario?.deleteAdditionalItem(itemID)
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
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item_NP))
                )
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setRetirementAdditionalDialogFragmentListener(listener: RetirementAdditionalDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}