package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentScheduledPaymentEditDialogBinding

class ScheduledPaymentEditDialogFragment : DialogFragment() {
/*    interface ScheduledPaymentEditDialogFragmentListener {

        fun onNewDataSaved()
    } */
//    private var listener: ScheduledPaymentEditDialogFragmentListener? = null
    private var initialLoad = true
    private var currentMode = cMODE_VIEW

    companion object {
        private const val KEY_NAME = "0"
        private var oldKey: String = ""
        private var oldSP: ScheduledPayment? = null
        fun newInstance(
            key: String
        ): ScheduledPaymentEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, key)
            val fragment = ScheduledPaymentEditDialogFragment()
            fragment.arguments = args
            oldKey = key
            oldSP = ScheduledPaymentViewModel.getScheduledPayment(oldKey)
            return fragment
        }
    }

    private var _binding: FragmentScheduledPaymentEditDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduledPaymentEditDialogBinding.inflate(inflater, container, false)
        binding.newAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.loanAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.amortizationPeriod.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.interestRate.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.actualLoanPaymentAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text =
            String.format(getString(R.string.trailing_space), getLocalCurrencySymbol())

        val categorySpinner:Spinner = binding.newCategory
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryNames()
        )
        categorySpinner.adapter = catArrayAdapter

        val paidBySpinner:Spinner = binding.newPaidBy
        val paidByArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        paidBySpinner.adapter = paidByArrayAdapter
        val boughtForSpinner:Spinner = binding.newBoughtFor
        val boughtForArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        boughtForSpinner.adapter = boughtForArrayAdapter

        if (oldSP == null) {
            binding.linkLayout.visibility = View.GONE
            binding.lastPaymentLayout.visibility = View.GONE
            binding.newNextDate.setText(gCurrentDate.toString())
            binding.newAmount.setText("")
            binding.oldRegularity.text = ""
            binding.newRegularity.setText("1")
            categorySpinner.setSelection(0)
            paidBySpinner.setSelection(paidByArrayAdapter.getPosition(SpenderViewModel.getDefaultSpenderName()))
            boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(SpenderViewModel.getDefaultSpenderName()))
        } else {
            val numberOfGenerated = TransactionViewModel.getRTCount(oldSP?.mykey.toString())
            binding.linkMessage.setText(String.format(getString(R.string.there_are_recurring_transactions),
                numberOfGenerated))
            if (numberOfGenerated > 0) {
                binding.dialogButtonDelete.visibility = View.GONE
            }
            binding.linkMessage.setOnClickListener {
                val action =
                    SettingsTabsFragmentDirections.actionSettingsTabFragmentToTransactionViewAllFragment()
                action.filterMode = cSCHEDULED_PAYMENT_FILTER
                action.rtKey = oldSP?.mykey.toString()
                findNavController().navigate(action)
                dismiss()
                MyApplication.displayToast(String.format(getString(R.string.these_are_the_scheduled_transactions),
                    oldSP?.vendor.toString()))
            }
            binding.oldVendor.text = oldSP?.vendor
            binding.newVendor.setText(oldSP?.vendor)
            binding.oldNote.text = oldSP?.note
            binding.newNote.setText(oldSP?.note)
            binding.oldLastDate.text = oldSP?.lastDate.toString()
            binding.oldNextDate.text = oldSP?.nextdate.toString()
            binding.newNextDate.setText(oldSP?.nextdate.toString())
            binding.oldExpirationDate.text = oldSP?.expirationDate.toString()
            if (oldSP?.isExpired() == true) {
                binding.expirationDateLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                binding.oldExpirationDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
            binding.newExpirationDate.setText(oldSP?.expirationDate.toString())
            if (oldSP?.amount == 0.0) {
                binding.newAmount.setText("")
            } else {
                binding.oldAmount.text = oldSP?.amount?.let { gDec(it) }
                binding.newAmount.setText(oldSP?.amount?.let { gDec(it) })
            }
            binding.oldRegularity.text = oldSP?.regularity.toString()
            binding.newRegularity.setText(oldSP?.regularity.toString())
            if (oldSP?.category == 0)
                categorySpinner.setSelection(0)
            else {
                categorySpinner.setSelection(catArrayAdapter.getPosition(oldSP?.category?.let {
                    CategoryViewModel.getCategory(
                        it
                    )?.categoryName
                }))
            }

            paidBySpinner.setSelection(paidByArrayAdapter.getPosition(oldSP?.paidby?.let {
                SpenderViewModel.getSpenderName(
                    it
                )
            }))
            boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(oldSP?.boughtfor?.let {
                SpenderViewModel.getSpenderName(
                    it
                )
            }))
        }
        binding.oldPeriod.text = oldSP?.period?.let { getTranslationForPeriod(1, it) }
        binding.oldExpirationDate.text = oldSP?.expirationDate.toString()
        binding.oldNextDate.text = oldSP?.nextdate.toString()
        binding.oldCategory.text = oldSP?.category?.let {
            CategoryViewModel.getCategory(
                it
            )?.categoryName
        }
        binding.oldSubcategory.text = oldSP?.category?.let {
            CategoryViewModel.getCategory(
                it
            )?.subcategoryName
        }
        binding.oldPaidBy.text = oldSP?.paidby?.let { SpenderViewModel.getSpenderName(it) }
        binding.oldBoughtFor.text = oldSP?.boughtfor?.let { SpenderViewModel.getSpenderName(it) }
        binding.splitSlider.value = oldSP?.split1?.toFloat() ?: 0.0F
        setupClickListeners()
        binding.newVendor.requestFocus()
        binding.newAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.newAmount.text.toString())
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        val pSpinner:Spinner = binding.newPeriodSpinner
        val periodValues = listOf(
            getString(R.string.week),
            getString(R.string.month),
            getString(R.string.quarter),
            getString(R.string.year))
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodValues
        )
        pSpinner.adapter = arrayAdapter
        when (oldSP?.period) {
            cPeriodWeek -> pSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.week)))
            cPeriodQuarter -> pSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.quarter)))
            cPeriodYear -> pSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.year)))
            else -> pSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.month)))
        }

        if (oldKey == "") { // ie this is an add, not an edit
            currentMode = cMODE_ADD
            binding.dialogLinearLayout1.visibility = View.GONE
            binding.oldVendor.visibility = View.GONE
            binding.oldNote.visibility = View.GONE
            binding.oldAmount.visibility = View.GONE
            binding.oldPeriod.visibility = View.GONE
            binding.oldRegularity.visibility = View.GONE
            binding.oldNextDate.visibility = View.GONE
            binding.oldExpirationDate.visibility = View.GONE
            binding.currentValueHeader.visibility = View.GONE
            binding.oldPaidBy.visibility = View.GONE
            binding.oldBoughtFor.visibility = View.GONE
            binding.dialogButtonDelete.visibility = View.GONE
            binding.loanStartDate.setText(gCurrentDate.toString())
        } else { // ie it's an edit
            binding.title.visibility = View.GONE
            if (oldSP?.activeLoan == true) {
                binding.loanStartDate.setText(oldSP?.loanFirstPaymentDate.toString())
                binding.loanAmount.setText(oldSP?.loanAmount?.let { gDec(it) })
                binding.amortizationPeriod.setText(oldSP?.loanAmortization?.let { gDec(it) })
                binding.interestRate.setText(oldSP?.loanInterestRate?.let { gDec(it) })
                binding.actualLoanPaymentAmount.setText(oldSP?.actualPayment?.let { gDec(it) })
                when (oldSP?.loanPaymentRegularity) {
                    LoanPaymentRegularity.WEEKLY -> binding.buttonWeekly.isChecked = true
                    LoanPaymentRegularity.BIWEEKLY -> binding.buttonBiweekly.isChecked = true
                    else -> binding.buttonMonthly.isChecked = true
                }
                binding.loanLink.visibility = View.VISIBLE
            }
            binding.loanSwitch.isChecked = (oldSP?.activeLoan == true)
            binding.loanSwitch.isEnabled = false
            if (!binding.loanSwitch.isChecked)
                binding.loanLink.visibility = View.INVISIBLE
            if (oldSP?.activeLoan == true) {
                binding.loanDetailsLayout.visibility = View.VISIBLE
                binding.loanStartDate.isEnabled = false
                binding.loanAmount.isEnabled = false
                binding.amortizationPeriod.isEnabled = false
                binding.interestRate.isEnabled = false
                binding.actualLoanPaymentAmount.isEnabled = false
                binding.buttonWeekly.isEnabled = false
                binding.buttonBiweekly.isEnabled = false
                binding.buttonMonthly.isEnabled = false
            } else
                binding.loanDetailsLayout.visibility = View.GONE
        }

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.newNextDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.newNextDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.newNextDate.text.toString() != "") {
                lcal = MyDate(binding.newNextDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }

        val expDateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.newExpirationDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.newExpirationDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.newExpirationDate.text.toString() != "") {
                lcal = MyDate(binding.newExpirationDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), expDateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }
        binding.loanLink.setOnClickListener {
            val action =
                SettingsTabsFragmentDirections.actionSettingsTabFragmentToLoanFragment()
            action.loanID = oldKey
            findNavController().navigate(action)
            dismiss()
        }
        binding.expandButton.setOnClickListener {
            onExpandClicked()
        }

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.newPeriodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.newPeriodSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.newCategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.newCategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.newSubcategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.newSubcategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.newPaidBy.setBackgroundColor(Color.parseColor(hexColor))
        binding.newPaidBy.setPopupBackgroundResource(R.drawable.spinner)
        binding.newBoughtFor.setBackgroundColor(Color.parseColor(hexColor))
        binding.newBoughtFor.setPopupBackgroundResource(R.drawable.spinner)

        binding.newCategory.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                addSubCategories(binding.newCategory.selectedItem.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        if (SpenderViewModel.singleUser()) {
            binding.dialogLinearLayoutPaidby.visibility = View.GONE
            binding.dialogLinearLayoutBoughtfor.visibility = View.GONE
            binding.splitSlider.visibility = View.GONE
            binding.dialogSplit.visibility = View.GONE
            binding.dialogLinearLayoutSplitSlider.visibility = View.GONE
            binding.expandButton.visibility = View.GONE
            setExpansionFields(View.GONE)
        }
        binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.newAmount.text.toString())
        binding.newBoughtFor.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val boughtFor = binding.newBoughtFor.selectedItem.toString()
                when (boughtFor) {
                    getString(R.string.joint) -> {
//                        if (!initialLoad)
                           binding.splitSlider.value = (SpenderViewModel.getSpenderSplit(0) * 100).toFloat()
                        binding.splitSlider.isEnabled = true
                    }
                    SpenderViewModel.getSpenderName(0) -> {
                        binding.splitSlider.value = 100.0F
                    }
                    else -> {
                        binding.splitSlider.value = 0.0F
                    }
                }
                binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.newAmount.text.toString())
                initialLoad = false
                binding.splitSlider.isEnabled = boughtFor == getString(R.string.joint)
            }
        }
        binding.newPaidBy.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (binding.boughtForLabel.visibility == View.GONE) { // need to keep both values in sync
                        val paidByName = binding.newPaidBy.selectedItem.toString()
                        boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(paidByName))
                    }
                }
            }
        binding.splitSlider.addOnChangeListener { _, _, _ ->
            binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.newAmount.text.toString())
        }
        if (oldSP == null)
            binding.splitSlider.value = 0.0F
        else
            binding.splitSlider.value = oldSP?.split1?.toFloat()!!
        binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.newAmount.text.toString())

        if ((oldSP?.boughtfor == 2 && binding.splitSlider.value.toInt() != (SpenderViewModel.getSpenderSplit(0) * 100).toInt()) ||
            oldSP?.paidby != oldSP?.boughtfor) {
            setExpansionFields(View.VISIBLE)
        }
        else {
            setExpansionFields(View.GONE)
        }
        if (oldKey != "" && currentMode == cMODE_VIEW) {
            binding.dialogButtonSave.text = getString(R.string.edit)
            binding.dialogButtonSave.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_edit_24), null, null)
            if (oldSP?.isExpired() == true) {
                binding.spHeader.text = String.format(getString(R.string.expired), oldSP?.vendor)
                binding.spHeader.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            } else
                binding.spHeader.text = oldSP?.vendor
            binding.newVendor.visibility = View.GONE
            binding.newNote.visibility = View.GONE
            binding.currentValueHeader.visibility = View.GONE
            binding.headerPrefix.visibility = View.GONE
            binding.newValueHeader.visibility = View.GONE
            binding.amountLayout.visibility = View.GONE
            binding.dateLayout.visibility = View.GONE
            binding.expirationDateLayout.visibility = View.GONE
            binding.regularityLayout.visibility = View.GONE
            binding.periodSpinnerRelativeLayout.visibility = View.GONE
            binding.categorySpinnerRelativeLayout.visibility = View.GONE
            binding.subCategorySpinnerRelativeLayout.visibility = View.GONE
            binding.paidBySpinnerRelativeLayout.visibility = View.GONE
            binding.boughtForSpinnerRelativeLayout.visibility = View.GONE
            binding.splitSlider.visibility = View.GONE
        }

        val loanDateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.loanStartDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.loanStartDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.loanStartDate.text.toString() != "") {
                lcal = MyDate(binding.loanStartDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), loanDateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }
        when (binding.newBoughtFor.selectedItem.toString()) {
            getString(R.string.joint) -> {
                binding.splitSlider.value = (SpenderViewModel.getSpenderSplit(0) * 100).toFloat()
                binding.splitSlider.isEnabled = true
            }
            SpenderViewModel.getSpenderName(0) -> {
                binding.splitSlider.value = 100.0F
            }
            else -> {
                binding.splitSlider.value = 0.0F
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

    private fun onExpandClicked() {
        if (binding.boughtForLabel.visibility == View.GONE) { // ie expand the section
            setExpansionFields(View.VISIBLE)
        } else { // ie retract the section
            setExpansionFields(View.GONE)
        }
    }

    private fun setExpansionFields(iView: Int) {
        if (iView == View.GONE) {
            binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
            binding.paidByLabel.text = getString(R.string.who)
            binding.paidByLabel.tooltipText = getString(R.string.toolTipWhoInput)
        } else {
            binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
            binding.paidByLabel.text = getString(R.string.paid_by)
            binding.paidByLabel.tooltipText = getString(R.string.toolTipPaidBy)
        }
        binding.boughtForLabel.visibility = iView
        binding.dialogLinearLayoutBoughtfor.visibility = iView
        binding.dialogLinearLayoutSplitSlider.visibility = iView
        binding.dialogSplit.visibility = iView
    }

    private fun addSubCategories(iCategory: String) {
        val subCategorySpinner = binding.newSubcategory
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        if (oldSP?.category?.let { CategoryViewModel.getCategory(it)?.subcategoryName } == "")
            subCategorySpinner.setSelection(0)
        else
            subCategorySpinner.setSelection(arrayAdapter.getPosition(oldSP?.category?.let {
                CategoryViewModel.getCategory(
                    it
                )?.subcategoryName
            }))

        arrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.dialogButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }

        binding.loanSwitch.setOnClickListener {
            if (binding.loanSwitch.isChecked)
                binding.loanDetailsLayout.visibility = View.VISIBLE
            else
                binding.loanDetailsLayout.visibility = View.GONE
        }
        binding.dialogButtonDelete.setOnClickListener {
            fun yesClicked() {
                ScheduledPaymentViewModel.deleteScheduledPayment(oldKey)
/*                if (listener != null) {
                    listener?.onNewDataSaved()
                } */
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                    binding.newVendor.text.toString()))
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.dialogButtonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun onSaveButtonClicked() {
        val lNumberFormat: NumberFormat = NumberFormat.getInstance()
        if (binding.newVendor.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
            focusAndOpenSoftKeyboard(requireContext(), binding.newVendor)
            return
        }
        if (binding.newAmount.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
            focusAndOpenSoftKeyboard(requireContext(), binding.newAmount)
            return
        }
        if (lNumberFormat.parse(binding.newAmount.text.toString()).toDouble() == 0.0) {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_zero))
            focusAndOpenSoftKeyboard(requireContext(), binding.newAmount)
            return
        }
        if (binding.newRegularity.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_zero))
            focusAndOpenSoftKeyboard(requireContext(), binding.newAmount)
            return
        }

        if (binding.loanSwitch.isChecked) {
            if (binding.loanAmount.text.toString() == "") {
                binding.loanAmount.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.loanAmount)
                return
            }
            if (binding.amortizationPeriod.text.toString() == "") {
                binding.amortizationPeriod.error = getString(R.string.value_cannot_be_zero)
                focusAndOpenSoftKeyboard(requireContext(), binding.amortizationPeriod)
                return
            }
            if (binding.interestRate.text.toString() == "") {
                binding.interestRate.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.interestRate)
                return
            }
        }
        val rtSpinner:Spinner = binding.newPeriodSpinner
//        var somethingChanged = false
        val amountDouble = lNumberFormat.parse(binding.newAmount.text.toString()).toDouble()

        if (currentMode == cMODE_VIEW) { // change to "edit"
            binding.spHeader.visibility = View.GONE
            binding.linkLayout.visibility = View.GONE
            binding.newVendor.visibility = View.VISIBLE
            binding.newNote.visibility = View.VISIBLE
            binding.currentValueHeader.visibility = View.VISIBLE
            binding.headerPrefix.visibility = View.VISIBLE
            binding.newValueHeader.visibility = View.VISIBLE
            binding.amountLayout.visibility = View.VISIBLE
            binding.dateLayout.visibility = View.VISIBLE
            binding.expirationDateLayout.visibility = View.VISIBLE
            binding.regularityLayout.visibility = View.VISIBLE
            binding.periodSpinnerRelativeLayout.visibility = View.VISIBLE
            binding.categorySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.subCategorySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.paidBySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.boughtForSpinnerRelativeLayout.visibility = View.VISIBLE
            binding.splitSlider.visibility = View.VISIBLE
            binding.splitSlider.isEnabled = true
            binding.dialogButtonSave.text = getString(R.string.save)
            binding.dialogButtonSave.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_save_24), null, null)
            binding.dialogButtonDelete.visibility = View.GONE
            binding.loanSwitch.isEnabled = true
            binding.loanStartDate.isEnabled = true
            binding.loanAmount.isEnabled = true
            binding.amortizationPeriod.isEnabled = true
            binding.interestRate.isEnabled = true
            binding.actualLoanPaymentAmount.isEnabled = true
            binding.buttonWeekly.isEnabled = true
            binding.buttonBiweekly.isEnabled = true
            binding.buttonMonthly.isEnabled = true
            currentMode = cMODE_EDIT
            return
        }  else { // else, continue below already in edit, so save...
            if (binding.newNextDate.text.toString() == "" &&
                binding.newExpirationDate.text.toString() == "") {
                showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
                focusAndOpenSoftKeyboard(requireContext(), binding.newNextDate)
                return
            }
            if (binding.newNextDate.text.toString() != "" &&
                binding.newExpirationDate.text.toString() != "" &&
                binding.newExpirationDate.text.toString() < gCurrentDate.toString()) {
                showErrorMessage(parentFragmentManager, getString(R.string.cannot_be_set_if_expired))
                focusAndOpenSoftKeyboard(requireContext(), binding.newNextDate)
                return
            }
            if (binding.newNextDate.text.toString() == "" &&
                binding.newExpirationDate.text.toString() > gCurrentDate.toString()) {
                showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
                focusAndOpenSoftKeyboard(requireContext(), binding.newNextDate)
                return
            }
        }

        val freq = when {
            binding.buttonWeekly.isChecked -> LoanPaymentRegularity.WEEKLY
            binding.buttonBiweekly.isChecked -> LoanPaymentRegularity.BIWEEKLY
            else -> LoanPaymentRegularity.MONTHLY
        }
        val chosenPeriod =
            when (rtSpinner.selectedItem.toString()) {
                getString(R.string.week) -> cPeriodWeek
                getString(R.string.quarter) -> cPeriodQuarter
                getString(R.string.year) -> cPeriodYear
                else -> cPeriodMonth
            }
        if (oldKey != "") {
            val oldLastDate: MyDate = oldSP?.lastDate!!
            val tSP = ScheduledPayment(
                binding.newVendor.text.toString().trim(),
                binding.newNote.text.toString().trim(),
                lNumberFormat.parse(binding.newAmount.text.toString()).toDouble(),
                chosenPeriod,
                binding.newRegularity.text.toString().toInt(),
                binding.newNextDate.text.toString(),
                oldLastDate,
                CategoryViewModel.getID(binding.newCategory.selectedItem.toString(),
                    binding.newSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.newPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.newBoughtFor.selectedItem.toString()),
                binding.splitSlider.value.toInt(),
                binding.loanSwitch.isChecked,
                if (binding.loanSwitch.isChecked) MyDate(binding.loanStartDate.text.toString()) else MyDate(),
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.interestRate.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.actualLoanPaymentAmount.text.toString()).toDouble()) else 0.0,
                freq,
                binding.newExpirationDate.text.toString(),
                oldKey
            )
            ScheduledPaymentViewModel.updateScheduledPayment(tSP)
            dismiss()
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        } else { // ie this is an add
            val sp = ScheduledPayment(
                binding.newVendor.text.toString().trim(),
                binding.newNote.text.toString().trim(),
                amountDouble,
                chosenPeriod, binding.newRegularity.text.toString().toInt(),
                binding.newNextDate.text.toString(),
                MyDate(),
                CategoryViewModel.getID(binding.newCategory.selectedItem.toString(),
                binding.newSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.newPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.newBoughtFor.selectedItem.toString()),
                binding.splitSlider.value.toInt(),
                binding.loanSwitch.isChecked,
                if (binding.loanSwitch.isChecked) MyDate(binding.loanStartDate.text.toString()) else MyDate(),
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.interestRate.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (lNumberFormat.parse(binding.actualLoanPaymentAmount.text.toString()).toDouble()) else 0.0,
                freq,
                binding.newExpirationDate.text.toString()
                )
            ScheduledPaymentViewModel.addScheduledPayment(sp)
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}