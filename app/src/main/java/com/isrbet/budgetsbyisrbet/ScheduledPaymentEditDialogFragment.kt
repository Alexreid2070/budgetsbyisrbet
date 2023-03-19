package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class ScheduledPaymentEditDialogFragment : DialogFragment() {
    interface ScheduledPaymentEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: ScheduledPaymentEditDialogFragmentListener? = null
    private var initialLoad = true
    private var cal: android.icu.util.Calendar = gCurrentDate.clone() as android.icu.util.Calendar // Calendar.getInstance()
    private var lCal = gCurrentDate.clone() as android.icu.util.Calendar // Calendar.getInstance()
    private var currentMode = cMODE_VIEW

    companion object {
        private const val KEY_NAME = "0"
        private var oldName: String = ""
        private var oldSP: ScheduledPayment? = null
        fun newInstance(
            name: String
        ): ScheduledPaymentEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, name)
            val fragment = ScheduledPaymentEditDialogFragment()
            fragment.arguments = args
            oldName = name
            oldSP = ScheduledPaymentViewModel.getScheduledPayment(oldName)
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
        binding.editNewAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.loanAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.amortizationPeriod.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.interestRate.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.setText(getLocalCurrencySymbol() + " ")
        binding.editOldName.setText(oldName)
        binding.editNewName.setText(oldName)

        val categorySpinner:Spinner = binding.editNewCategory
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryNames()
        )
        categorySpinner.adapter = catArrayAdapter

        val paidBySpinner:Spinner = binding.editNewPaidBy
        val paidByArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        paidBySpinner.adapter = paidByArrayAdapter
        val boughtForSpinner:Spinner = binding.editNewBoughtFor
        val boughtForArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        boughtForSpinner.adapter = boughtForArrayAdapter

        if (oldSP == null) {
            binding.editNewAmount.setText("")
            binding.editOldRegularity.text = ""
            binding.editNewRegularity.setText("1")
            categorySpinner.setSelection(0)
            paidBySpinner.setSelection(paidByArrayAdapter.getPosition(SpenderViewModel.getDefaultSpenderName()))
            boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(SpenderViewModel.getDefaultSpenderName()))
        } else {
            if (oldSP?.amount == 0.0) {
                binding.editNewAmount.setText("")
            } else {
                binding.editOldAmount.setText(oldSP?.amount?.let { gDec(it) })
                binding.editNewAmount.setText(oldSP?.amount?.let { gDec(it) })
            }
            binding.editOldRegularity.setText(oldSP?.regularity.toString())
            binding.editNewRegularity.setText(oldSP?.regularity.toString())
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
        binding.editOldPeriod.text = oldSP?.period?.let { getTranslationForPeriod(it) }
        binding.editOldNextDate.text = oldSP?.nextdate
        binding.editOldCategory.text = oldSP?.category?.let {
            CategoryViewModel.getCategory(
                it
            )?.categoryName
        }
        binding.editOldSubcategory.text = oldSP?.category?.let {
            CategoryViewModel.getCategory(
                it
            )?.subcategoryName
        }
        binding.editOldPaidBy.text = oldSP?.paidby?.let { SpenderViewModel.getSpenderName(it) }
        binding.editOldBoughtFor.text = oldSP?.boughtfor?.let { SpenderViewModel.getSpenderName(it) }
        binding.splitSlider.value = oldSP?.split1?.toFloat() ?: 0.0F
        setupClickListeners()
        binding.editNewName.requestFocus()
        binding.editNewAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.editNewAmount.text.toString())
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        val pSpinner:Spinner = binding.editNewPeriodSpinner
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

        if (oldName == "") { // ie this is an add, not an edit
            currentMode = cMODE_ADD
            binding.dialogLinearLayout1.visibility = View.GONE
            binding.editOldAmount.visibility = View.GONE
            binding.editOldPeriod.visibility = View.GONE
            binding.editOldRegularity.visibility = View.GONE
            binding.editOldNextDate.visibility = View.GONE
            binding.currentValueHeader.visibility = View.GONE
            binding.editOldPaidBy.visibility = View.GONE
            binding.editOldBoughtFor.visibility = View.GONE
            binding.dialogButtonDelete.visibility = View.GONE
        } else { // ie it's an edit
            binding.title.visibility = View.GONE
            val tOldDate = LocalDate.parse(oldSP?.nextdate, DateTimeFormatter.ISO_DATE)
            cal.set(Calendar.YEAR, tOldDate.year)
            cal.set(Calendar.MONTH, tOldDate.monthValue-1)
            cal.set(Calendar.DAY_OF_MONTH, tOldDate.dayOfMonth)
            if (oldSP?.activeLoan == true) {
                val tOldLoanDate = LocalDate.parse(oldSP?.loanFirstPaymentDate, DateTimeFormatter.ISO_DATE)
                lCal.set(Calendar.YEAR, tOldLoanDate.year)
                lCal.set(Calendar.MONTH, tOldLoanDate.monthValue - 1)
                lCal.set(Calendar.DAY_OF_MONTH, tOldLoanDate.dayOfMonth)
                binding.loanStartDate.setText(giveMeMyDateFormat(lCal))


                binding.loanAmount.setText(oldSP?.loanAmount?.let { gDec(it) })
                binding.amortizationPeriod.setText(oldSP?.loanAmortization?.let { gDec(it) })
                binding.interestRate.setText(oldSP?.loanInterestRate?.let { gDec(it) })
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
                binding.buttonWeekly.isEnabled = false
                binding.buttonBiweekly.isEnabled = false
                binding.buttonMonthly.isEnabled = false
            } else
                binding.loanDetailsLayout.visibility = View.GONE
        }
        binding.editNewNextDate.setText(giveMeMyDateFormat(cal))

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editNewNextDate.setText(giveMeMyDateFormat(cal))
            }

        binding.editNewNextDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.loanLink.setOnClickListener {
            val action =
                ScheduledPaymentFragmentDirections.actionScheduledPaymentFragmentToLoanFragment()
            action.loanID = binding.editOldName.text.toString()
            findNavController().navigate(action)
            dismiss()
        }
        binding.expandButton.setOnClickListener {
            onExpandClicked()
        }

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.editNewPeriodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.editNewPeriodSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.editNewCategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.editNewCategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editNewSubcategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.editNewSubcategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editNewPaidBy.setBackgroundColor(Color.parseColor(hexColor))
        binding.editNewPaidBy.setPopupBackgroundResource(R.drawable.spinner)
        binding.editNewBoughtFor.setBackgroundColor(Color.parseColor(hexColor))
        binding.editNewBoughtFor.setPopupBackgroundResource(R.drawable.spinner)

        binding.editNewCategory.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                addSubCategories(binding.editNewCategory.selectedItem.toString())
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
        binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.editNewAmount.text.toString())
/*        binding.dialogSplit.text = String.format(getString(R.string.split_is_x_pct_d_for_name1_and_z_pct_d_for_name2),
            SpenderViewModel.getSpenderSplit(0),
            0,
            SpenderViewModel.getSpenderName(0),
            SpenderViewModel.getSpenderSplit(1),
            0,
            SpenderViewModel.getSpenderName(1)) */
        binding.editNewBoughtFor.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val boughtFor = binding.editNewBoughtFor.selectedItem.toString()
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
                binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.editNewAmount.text.toString())
                initialLoad = false
                binding.splitSlider.isEnabled = boughtFor == getString(R.string.joint)
            }
        }
        binding.editNewPaidBy.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (binding.boughtForLabel.visibility == View.GONE) { // need to keep both values in sync
                        val paidByName = binding.editNewPaidBy.selectedItem.toString()
                        boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(paidByName))
                    }
                }
            }
        binding.splitSlider.addOnChangeListener { _, _, _ ->
/*            binding.dialogSplit.text = String.format(getString(R.string.split_is_x_pct_d_for_name1_and_z_pct_d_for_name2),
                binding.splitSlider.value.toInt(),
                amount1,
                SpenderViewModel.getSpenderName(0),
                100 - binding.splitSlider.value.toInt(),
                amount2,
                SpenderViewModel.getSpenderName(1)) */
            binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.editNewAmount.text.toString())
        }
        if (oldSP == null)
            binding.splitSlider.value = 0.0F
        else
            binding.splitSlider.value = oldSP?.split1?.toFloat()!!
        binding.dialogSplit.text = getSplitText(binding.splitSlider.value.toInt(), binding.editNewAmount.text.toString())

        if ((oldSP?.boughtfor == 2 && binding.splitSlider.value.toInt() != (SpenderViewModel.getSpenderSplit(0) * 100).toInt()) ||
            oldSP?.paidby != oldSP?.boughtfor) {
            setExpansionFields(View.VISIBLE)
        }
        else {
            setExpansionFields(View.GONE)
        }
        if (oldName != "" && currentMode == cMODE_VIEW) {
            binding.dialogButtonSave.text = getString(R.string.edit)
            binding.dialogButtonSave.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_edit_24), null, null)
            binding.viewHeader.text = oldName
            binding.currentValueHeader.visibility = View.GONE
            binding.headerPrefix.visibility = View.GONE
            binding.dialogLinearLayout2.visibility = View.GONE
            binding.newValueHeader.visibility = View.GONE
            binding.nameLayout.visibility = View.GONE
            binding.amountLayout.visibility = View.GONE
            binding.dateLayout.visibility = View.GONE
            binding.regularityLayout.visibility = View.GONE
            binding.periodSpinnerRelativeLayout.visibility = View.GONE
            binding.categorySpinnerRelativeLayout.visibility = View.GONE
            binding.subCategorySpinnerRelativeLayout.visibility = View.GONE
            binding.paidBySpinnerRelativeLayout.visibility = View.GONE
            binding.boughtForSpinnerRelativeLayout.visibility = View.GONE
            binding.splitSlider.visibility = View.GONE
/*            binding.dialogSplit.text = String.format(getString(R.string.split_is_x_pct_d_for_name1_and_z_pct_d_for_name2),
                oldSplit1,
                SpenderViewModel.getSpenderName(0),
                100 - oldSplit1,
                SpenderViewModel.getSpenderName(1)) */
        }

        binding.loanStartDate.setText(giveMeMyDateFormat(lCal))
        val loanDateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                lCal.set(Calendar.YEAR, year)
                lCal.set(Calendar.MONTH, monthOfYear)
                lCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.loanStartDate.setText(giveMeMyDateFormat(lCal))
            }

        binding.loanStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), loanDateSetListener,
                lCal.get(Calendar.YEAR),
                lCal.get(Calendar.MONTH),
                lCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        when (binding.editNewBoughtFor.selectedItem.toString()) {
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
        val subCategorySpinner = binding.editNewSubcategory
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
                ScheduledPaymentViewModel.deleteScheduledPaymentFromFirebase(binding.editOldName.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                    binding.editOldName.text.toString()))
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.dialogButtonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun onSaveButtonClicked() {
        if (!textIsSafeForKey(binding.editNewName.text.toString())) {
            showErrorMessage(parentFragmentManager, getString(R.string.the_text_contains_unsafe_characters))
            focusAndOpenSoftKeyboard(requireContext(), binding.editNewName)
            return
        }
        if (binding.editNewName.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
            focusAndOpenSoftKeyboard(requireContext(), binding.editNewName)
            return
        }
        if (binding.editNewAmount.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_blank))
            focusAndOpenSoftKeyboard(requireContext(), binding.editNewAmount)
            return
        }
        if (gNumberFormat.parse(binding.editNewAmount.text.toString()).toDouble() == 0.0) {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_zero))
            focusAndOpenSoftKeyboard(requireContext(), binding.editNewAmount)
            return
        }
        if (binding.editNewRegularity.text.toString() == "") {
            showErrorMessage(parentFragmentManager, getString(R.string.value_cannot_be_zero))
            focusAndOpenSoftKeyboard(requireContext(), binding.editNewAmount)
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
        val rtSpinner:Spinner = binding.editNewPeriodSpinner
        var somethingChanged = false
        var amountDouble = gNumberFormat.parse(binding.editNewAmount.text.toString()).toDouble()
        var amountInt: Int = round(amountDouble * 100).toInt()

        if (currentMode == cMODE_VIEW) { // change to "edit"
            binding.viewHeader.visibility = View.GONE
            binding.currentValueHeader.visibility = View.VISIBLE
            binding.headerPrefix.visibility = View.VISIBLE
            binding.dialogLinearLayout2.visibility = View.VISIBLE
            binding.newValueHeader.visibility = View.VISIBLE
            binding.nameLayout.visibility = View.VISIBLE
            binding.amountLayout.visibility = View.VISIBLE
            binding.dateLayout.visibility = View.VISIBLE
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
            binding.buttonWeekly.isEnabled = true
            binding.buttonBiweekly.isEnabled = true
            binding.buttonMonthly.isEnabled = true
            currentMode = cMODE_EDIT
            return
        }  // else, continue below already in edit, so save...

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
        if (oldName == binding.editNewName.text.toString()) {
            if (oldSP?.amount != amountDouble) {
                ScheduledPaymentViewModel.updateScheduledPaymentDoubleField(oldName, "amount", amountDouble)
                somethingChanged = true
            }
            if (oldSP?.period != chosenPeriod) {
                ScheduledPaymentViewModel.updateScheduledPaymentStringField(oldName, "period", chosenPeriod)
                somethingChanged = true
            }
            if (oldSP?.nextdate != binding.editNewNextDate.text.toString()) {
                ScheduledPaymentViewModel.updateScheduledPaymentStringField(oldName, "nextdate", binding.editNewNextDate.text.toString())
                somethingChanged = true
            }
            if (oldSP?.category?.let { CategoryViewModel.getCategory(it)?.categoryName } != binding.editNewCategory.selectedItem.toString()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName,
                    "category",
                    CategoryViewModel.getID(binding.editNewCategory.selectedItem.toString(),
                    binding.editNewSubcategory.selectedItem.toString()))
                somethingChanged = true
            }
            if (oldSP?.category?.let { CategoryViewModel.getCategory(it)?.subcategoryName } != binding.editNewSubcategory.selectedItem.toString()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName,
                    "category",
                CategoryViewModel.getID(binding.editNewCategory.selectedItem.toString(),
                    binding.editNewSubcategory.selectedItem.toString()))
                somethingChanged = true
            }
            if (oldSP?.paidby?.let { SpenderViewModel.getSpenderName(it) } != binding.editNewPaidBy.selectedItem.toString()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName,
                    "paidby", SpenderViewModel.getSpenderIndex(binding.editNewPaidBy.selectedItem.toString()))
                somethingChanged = true
            }
            if (oldSP?.boughtfor?.let { SpenderViewModel.getSpenderName(it) } != binding.editNewBoughtFor.toString()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName,
                    "boughtfor", SpenderViewModel.getSpenderIndex(binding.editNewBoughtFor.selectedItem.toString()))
                somethingChanged = true
            }
            if (oldSP?.regularity != binding.editNewRegularity.text.toString().toInt()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName, "regularity", binding.editNewRegularity.text.toString().toInt())
                somethingChanged = true
            }
            if (oldSP?.split1 != binding.splitSlider.value.toInt()) {
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName, "split1", binding.splitSlider.value.toInt())
                ScheduledPaymentViewModel.updateScheduledPaymentIntField(oldName, "split2", 100-binding.splitSlider.value.toInt())
                somethingChanged = true
            }
            if (oldSP?.activeLoan != binding.loanSwitch.isChecked) {
                    ScheduledPaymentViewModel.updateScheduledPaymentStringField(oldName, "activeLoan",
                        if (binding.loanSwitch.isChecked) cTRUE else cFALSE)
                somethingChanged = true
            }
            if (binding.loanSwitch.isChecked) {
                if (oldSP?.loanFirstPaymentDate != binding.loanStartDate.text.toString()) {
                    ScheduledPaymentViewModel.updateScheduledPaymentStringField(
                        oldName,
                        "loanFirstPaymentDate",
                        binding.loanStartDate.text.toString()
                    )
                    somethingChanged = true
                }
                amountDouble = gNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()
                amountInt = round(amountDouble * 100).toInt()
                if (oldSP?.loanAmount != amountDouble) {
                    ScheduledPaymentViewModel.updateScheduledPaymentDoubleField(
                        oldName,
                        "loanAmount",
                        amountDouble
                    )
                    somethingChanged = true
                }
                amountDouble = gNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()
                amountInt = round(amountDouble * 100).toInt()
                if (oldSP?.loanAmortization != amountDouble) {
                    ScheduledPaymentViewModel.updateScheduledPaymentDoubleField(
                        oldName,
                        "loanAmortization",
                        amountDouble
                    )
                    somethingChanged = true
                }
                amountDouble = gNumberFormat.parse(binding.interestRate.text.toString()).toDouble()
                amountInt = round(amountDouble * 100).toInt()
                if (oldSP?.loanInterestRate != amountDouble) {
                    ScheduledPaymentViewModel.updateScheduledPaymentDoubleField(
                        oldName,
                        "loanInterestRate",
                        amountDouble
                    )
                    somethingChanged = true
                }
                if (oldSP?.loanPaymentRegularity != freq) {
                    ScheduledPaymentViewModel.updateScheduledPaymentStringField(
                        oldName, "loanPaymentRegularity",
                        freq.name
                    )
                    somethingChanged = true
                }
            }
            if (somethingChanged) {
                ScheduledPaymentViewModel.updateScheduledPayment(
                    oldName,
                    round(gNumberFormat.parse(binding.editNewAmount.text.toString()).toDouble()),
                    chosenPeriod,
                    binding.editNewNextDate.text.toString(),
                    binding.editNewRegularity.text.toString().toInt(),
                    CategoryViewModel.getID(binding.editNewCategory.selectedItem.toString(),
                    binding.editNewSubcategory.selectedItem.toString()),
                    SpenderViewModel.getSpenderIndex(binding.editNewPaidBy.selectedItem.toString()),
                    SpenderViewModel.getSpenderIndex(binding.editNewBoughtFor.selectedItem.toString()),
                    binding.splitSlider.value.toInt(),
                    binding.loanSwitch.isChecked,
                    if (binding.loanSwitch.isChecked) binding.loanStartDate.text.toString() else "",
                    if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()) else 0.0,
                    if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()) else 0.0,
                    if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.interestRate.text.toString()).toDouble()) else 0.0,
                    freq
                )
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        } else if (oldName == "") { // ie this is an add
            if (ScheduledPaymentViewModel.nameExists(binding.editNewName.text.toString().trim())) {
                showErrorMessage(parentFragmentManager,
                    getString(R.string.this_name_is_already_in_use))
                focusAndOpenSoftKeyboard(requireContext(), binding.editNewName)
                return
            }
            val sp = ScheduledPayment(binding.editNewName.text.toString().trim(),
                amountDouble,
                chosenPeriod, binding.editNewRegularity.text.toString().toInt(),
                binding.editNewNextDate.text.toString(),
                CategoryViewModel.getID(binding.editNewCategory.selectedItem.toString(),
                binding.editNewSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editNewPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editNewBoughtFor.selectedItem.toString()),
                binding.splitSlider.value.toInt(),
                binding.loanSwitch.isChecked,
                if (binding.loanSwitch.isChecked) binding.loanStartDate.text.toString() else "",
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.interestRate.text.toString()).toDouble()) else 0.0,
                freq
                )
            ScheduledPaymentViewModel.addScheduledPayment(sp)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        } else if (oldName != binding.editNewName.text.toString()) {
            val sp = ScheduledPayment(binding.editNewName.text.toString().trim(),
                gNumberFormat.parse(binding.editNewAmount.text.toString()).toDouble(),
                chosenPeriod, binding.editNewRegularity.text.toString().toInt(),
                binding.editNewNextDate.text.toString(),
                CategoryViewModel.getID(binding.editNewCategory.selectedItem.toString(),
                binding.editNewSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editNewPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editNewBoughtFor.selectedItem.toString()),
                binding.splitSlider.value.toInt(),
                binding.loanSwitch.isChecked,
                if (binding.loanSwitch.isChecked) binding.loanStartDate.text.toString() else "",
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble()) else 0.0,
                if (binding.loanSwitch.isChecked) (gNumberFormat.parse(binding.interestRate.text.toString()).toDouble()) else 0.0,
                freq
                )
            ScheduledPaymentViewModel.addScheduledPayment(sp)
            ScheduledPaymentViewModel.deleteScheduledPaymentFromFirebase(oldName)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }
        ScheduledPaymentViewModel.generateScheduledPayments(activity as MainActivity)
    }

    fun setDialogFragmentListener(listener: ScheduledPaymentEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}