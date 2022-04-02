package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionEditDialogBinding
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class RecurringTransactionEditDialogFragment : DialogFragment() {
    interface RecurringTransactionEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: RecurringTransactionEditDialogFragmentListener? = null
    private var initialLoad = true
    private var cal: android.icu.util.Calendar = android.icu.util.Calendar.getInstance()
    private var currentMode = "View"

    companion object {
        private const val KEY_NAME = "KEY_NAME"
        private const val KEY_AMOUNT = "KEY_AMOUNT"
        private const val KEY_PERIOD = "KEY_PERIOD"
        private const val KEY_NEXTDATE = "KEY_NEXT_DATE"
        private const val KEY_REGULARITY = "KEY_REGULARITY"
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"
        private const val KEY_PAIDBY = "KEY_PAIDBY"
        private const val KEY_BOUGHTFOR = "KEY_BOUGHTFOR"
        private const val KEY_SPLIT1 = "KEY_SPLIT1"
        private var oldName: String = ""
        private var oldPeriod: String = ""
        private var oldAmount: Int = 0
        private var oldDate: String = ""
        private var oldRegularity: Int = 0
        private var oldCategoryID: Int = 0
        private var oldPaidBy: Int = 0
        private var oldBoughtFor: Int = 0
        private var oldSplit1: Int = 0
        fun newInstance(
            name: String,
            amount: Int,
            period: String,
            nextdate: String,
            regularity: Int,
            categoryID: Int,
            paidby: Int,
            boughtfor: Int,
            split1: Int
        ): RecurringTransactionEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, name)
            args.putString(KEY_AMOUNT, amount.toString())
            args.putString(KEY_PERIOD, period)
            args.putString(KEY_NEXTDATE, nextdate)
            args.putString(KEY_REGULARITY, regularity.toString())
            args.putString(KEY_CATEGORY_ID, categoryID.toString())
            args.putString(KEY_PAIDBY, paidby.toString())
            args.putString(KEY_BOUGHTFOR, boughtfor.toString())
            args.putString(KEY_SPLIT1, split1.toString())
            val fragment = RecurringTransactionEditDialogFragment()
            fragment.arguments = args
            oldName = name
            oldAmount = amount
            oldPeriod = period
            oldRegularity = regularity
            oldDate = nextdate
            oldCategoryID = categoryID
            oldPaidBy = paidby
            oldBoughtFor = boughtfor
            oldSplit1 = split1
            return fragment
        }
    }

    private var _binding: FragmentRecurringTransactionEditDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTransactionEditDialogBinding.inflate(inflater, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_recurring_transaction_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editRtOldName.text = oldName
        binding.editRtNewName.setText(oldName)

        if (oldAmount == 0) {
            binding.editRtNewAmount.setText("")
        } else {
            val dec = DecimalFormat("#.00")
            val formattedAmount = (oldAmount / 100).toDouble() + (oldAmount % 100).toDouble() / 100
            binding.editRtOldAmount.text = dec.format(formattedAmount)
            binding.editRtNewAmount.setText(dec.format(formattedAmount))
        }
        binding.editRtOldPeriod.text = oldPeriod
        binding.editRtOldRegularity.text = oldRegularity.toString()
        binding.editRtNewRegularity.setText(oldRegularity.toString())
        binding.editRtOldNextDate.text = oldDate
        binding.editRtOldCategory.text = CategoryViewModel.getCategory(
            oldCategoryID)?.categoryName
        binding.editRtOldSubcategory.text = CategoryViewModel.getCategory(
            oldCategoryID)?.subcategoryName
        binding.editRtOldPaidBy.text = SpenderViewModel.getSpenderName(oldPaidBy)
        binding.editRtOldBoughtFor.text = SpenderViewModel.getSpenderName(oldBoughtFor)
        setupClickListeners()
        binding.editRtNewName.requestFocus()

        val pSpinner:Spinner = binding.editRtNewPeriodSpinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PeriodValues
        )
        pSpinner.adapter = arrayAdapter
        pSpinner.setSelection(arrayAdapter.getPosition(oldPeriod))

        val categorySpinner:Spinner = binding.editRtNewCategory
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryNames()
        )
        categorySpinner.adapter = catArrayAdapter
        if (oldCategoryID == 0)
            categorySpinner.setSelection(0)
        else
            categorySpinner.setSelection(catArrayAdapter.getPosition(CategoryViewModel.getCategory(
                oldCategoryID)?.categoryName))

        val paidBySpinner:Spinner = binding.editRtNewPaidBy
        val paidByArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        paidBySpinner.adapter = paidByArrayAdapter
        paidBySpinner.setSelection(paidByArrayAdapter.getPosition(SpenderViewModel.getSpenderName(
            oldPaidBy)))

        val boughtForSpinner:Spinner = binding.editRtNewBoughtFor
        val boughtForArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getActiveSpenders()
        )
        boughtForSpinner.adapter = boughtForArrayAdapter
        boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(SpenderViewModel.getSpenderName(
            oldBoughtFor)))

        if (oldName == "") { // ie this is an add, not an edit
            currentMode = "Add"
            binding.rtDialogLinearLayout1.visibility = View.GONE
            binding.editRtOldAmount.visibility = View.GONE
            binding.editRtOldPeriod.visibility = View.GONE
            binding.editRtOldRegularity.visibility = View.GONE
            binding.editRtOldNextDate.visibility = View.GONE
            binding.rtCurrentValueHeader.visibility = View.GONE
            binding.editRtOldPaidBy.visibility = View.GONE
            binding.editRtOldBoughtFor.visibility = View.GONE
            binding.rtDialogButtonDelete.visibility = View.GONE
        } else { // ie it's an edit
            binding.rtTitle.visibility = View.GONE
            val tOldDate = LocalDate.parse(oldDate, DateTimeFormatter.ISO_DATE)
            cal.set(Calendar.YEAR, tOldDate.year)
            cal.set(Calendar.MONTH, tOldDate.monthValue-1)
            cal.set(Calendar.DAY_OF_MONTH, tOldDate.dayOfMonth)
        }
        binding.editRtNewNextDate.setText(giveMeMyDateFormat(cal))

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editRtNewNextDate.setText(giveMeMyDateFormat(cal))
            }

        binding.editRtNewNextDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.editRtNewPeriodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.editRtNewPeriodSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewCategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.editRtNewCategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewSubcategory.setBackgroundColor(Color.parseColor(hexColor))
        binding.editRtNewSubcategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewPaidBy.setBackgroundColor(Color.parseColor(hexColor))
        binding.editRtNewPaidBy.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewBoughtFor.setBackgroundColor(Color.parseColor(hexColor))
        binding.editRtNewBoughtFor.setPopupBackgroundResource(R.drawable.spinner)

        binding.editRtNewCategory.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                addSubCategories(binding.editRtNewCategory.selectedItem.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        if (SpenderViewModel.singleUser()) {
            binding.rtDialogLinearLayoutPaidby.visibility = View.INVISIBLE
            binding.rtDialogLinearLayoutBoughtfor.visibility = View.INVISIBLE
            binding.splitSlider.visibility = View.INVISIBLE
            binding.rtDialogLinearLayoutSplit.visibility = View.INVISIBLE
            binding.rtDialogLinearLayoutSplitSlider.visibility = View.INVISIBLE
        }
        binding.transactionBoughtForName1Label.text = SpenderViewModel.getSpenderName(0)
        binding.transactionBoughtForName2Label.text = SpenderViewModel.getSpenderName(1)
        binding.editRtNewBoughtFor.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            @SuppressLint("SetTextI18n")
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val boughtFor = binding.editRtNewBoughtFor.selectedItem.toString()
                when (boughtFor) {
                    "Joint" -> {
                        binding.transactionBoughtForName1Split.text =
                            SpenderViewModel.getSpenderSplit(0).toString()
                        binding.transactionBoughtForName2Split.text =
                            SpenderViewModel.getSpenderSplit(1).toString()
                        Log.d("Alex", "initialLoad is $initialLoad")
                        if (!initialLoad)
                           binding.splitSlider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
                        binding.transactionBoughtForName1Split.text = binding.splitSlider.value.toInt().toString()
                        binding.transactionBoughtForName2Split.text =
                            (100 - binding.transactionBoughtForName1Split.text.toString().toInt()).toString()
                        binding.splitSlider.isEnabled = true
                    }
                    SpenderViewModel.getSpenderName(0) -> {
                        binding.transactionBoughtForName1Split.text = "100"
                        binding.transactionBoughtForName2Split.text = "0"
                        binding.splitSlider.value = 100.0F
                    }
                    else -> {
                        binding.transactionBoughtForName1Split.text = "0"
                        binding.transactionBoughtForName2Split.text = "100"
                        binding.splitSlider.value = 0.0F
                    }
                }
                initialLoad = false
                binding.splitSlider.isEnabled = boughtFor == "Joint"
            }
        }
        binding.splitSlider.addOnChangeListener { _, _, _ ->
            binding.transactionBoughtForName1Split.text = binding.splitSlider.value.toInt().toString()
            binding.transactionBoughtForName2Split.text = (100-binding.splitSlider.value.toInt()).toString()
        }
        binding.splitSlider.value = oldSplit1.toFloat()

        if (oldName != "" && currentMode == "View") {
            binding.rtDialogButtonSave.text = "Edit"
            binding.viewHeader.text = oldName
            binding.rtCurrentValueHeader.visibility = View.GONE
            binding.headerPrefix.visibility = View.GONE
            binding.rtDialogLinearLayout2.visibility = View.GONE
            binding.rtNewValueHeader.visibility = View.GONE
            binding.nameLayout.visibility = View.GONE
            binding.amountLayout.visibility = View.GONE
            binding.dateLayout.visibility = View.GONE
            binding.regularityLayout.visibility = View.GONE
            binding.rtPeriodSpinnerRelativeLayout.visibility = View.GONE
            binding.rtCategorySpinnerRelativeLayout.visibility = View.GONE
            binding.rtSubCategorySpinnerRelativeLayout.visibility = View.GONE
            binding.rtPaidBySpinnerRelativeLayout.visibility = View.GONE
            binding.rtBoughtForSpinnerRelativeLayout.visibility = View.GONE
            binding.splitSlider.visibility = View.GONE
            binding.transactionBoughtForName1Split.text = oldSplit1.toString()
            binding.transactionBoughtForName2Split.text = (100 - oldSplit1).toString()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addSubCategories(iCategory: String) {
        val subCategorySpinner = binding.editRtNewSubcategory
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        if (CategoryViewModel.getCategory(oldCategoryID)?.subcategoryName == "")
            subCategorySpinner.setSelection(0)
        else
            subCategorySpinner.setSelection(arrayAdapter.getPosition(CategoryViewModel.getCategory(
                oldCategoryID)?.subcategoryName))

        arrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.rtDialogButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }

        binding.rtDialogButtonDelete.setOnClickListener {
            fun yesClicked() {
                RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(binding.editRtOldName.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to delete this recurring transaction(" + binding.editRtOldName.text.toString() + ")?")
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.rtDialogButtonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun onSaveButtonClicked() {
        val rtSpinner:Spinner = binding.editRtNewPeriodSpinner
        var somethingChanged = false
        val amountInt: Int = round(binding.editRtNewAmount.text.toString().toDouble()*100).toInt()

        if (currentMode == "View") { // change to "edit"
            binding.viewHeader.visibility = View.GONE
            binding.rtCurrentValueHeader.visibility = View.VISIBLE
            binding.headerPrefix.visibility = View.VISIBLE
            binding.rtDialogLinearLayout2.visibility = View.VISIBLE
            binding.rtNewValueHeader.visibility = View.VISIBLE
            binding.nameLayout.visibility = View.VISIBLE
            binding.amountLayout.visibility = View.VISIBLE
            binding.dateLayout.visibility = View.VISIBLE
            binding.regularityLayout.visibility = View.VISIBLE
            binding.rtPeriodSpinnerRelativeLayout.visibility = View.VISIBLE
            binding.rtCategorySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.rtSubCategorySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.rtPaidBySpinnerRelativeLayout.visibility = View.VISIBLE
            binding.rtBoughtForSpinnerRelativeLayout.visibility = View.VISIBLE
            binding.splitSlider.visibility = View.VISIBLE
            binding.rtDialogButtonSave.text = "Save"
            binding.rtDialogButtonDelete.visibility = View.GONE
            currentMode = "Edit"
            return
        }  // else, continue below already in edit, so save...


        if (!textIsSafeForKey(binding.editRtNewName.text.toString())) {
            showErrorMessage(parentFragmentManager, "The text contains unsafe characters.  They must be removed.")
            focusAndOpenSoftKeyboard(requireContext(), binding.editRtNewName)
            return
        }

        if (oldName == binding.editRtNewName.text.toString()) {
            if (oldAmount != amountInt) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "amount", amountInt)
                somethingChanged = true
            }
            if (oldPeriod != rtSpinner.selectedItem.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "period", rtSpinner.selectedItem.toString())
                somethingChanged = true
            }
            if (oldDate != binding.editRtNewNextDate.text.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "nextdate", binding.editRtNewNextDate.text.toString())
                somethingChanged = true
            }
            if (CategoryViewModel.getCategory(oldCategoryID)?.categoryName != binding.editRtNewCategory.selectedItem.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName,
                    "category",
                    CategoryViewModel.getID(binding.editRtNewCategory.selectedItem.toString(),
                    binding.editRtNewSubcategory.selectedItem.toString()))
                somethingChanged = true
            }
            if (CategoryViewModel.getCategory(oldCategoryID)?.subcategoryName != binding.editRtNewSubcategory.selectedItem.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName,
                    "category",
                CategoryViewModel.getID(binding.editRtNewCategory.selectedItem.toString(),
                    binding.editRtNewSubcategory.selectedItem.toString()))
                somethingChanged = true
            }
            if (SpenderViewModel.getSpenderName(oldPaidBy) != binding.editRtNewPaidBy.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName,
                    "paidby", SpenderViewModel.getSpenderIndex(binding.editRtNewPaidBy.selectedItem.toString()))
                somethingChanged = true
            }
            if (SpenderViewModel.getSpenderName(oldBoughtFor) != binding.editRtNewBoughtFor.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName,
                    "boughtfor", SpenderViewModel.getSpenderIndex(binding.editRtNewBoughtFor.selectedItem.toString()))
                somethingChanged = true
            }
            if (oldRegularity != binding.editRtNewRegularity.text.toString().toInt()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "regularity", binding.editRtNewRegularity.text.toString().toInt())
                somethingChanged = true
            }
            if (oldSplit1 != binding.splitSlider.value.toInt()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "split1", binding.splitSlider.value.toInt())
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "split2", 100-binding.splitSlider.value.toInt())
                somethingChanged = true
            }
            if (somethingChanged) {
                RecurringTransactionViewModel.updateRecurringTransaction(
                    oldName,
                    amountInt,
                    rtSpinner.selectedItem.toString(),
                    binding.editRtNewNextDate.text.toString(),
                    binding.editRtNewRegularity.text.toString().toInt(),
                    CategoryViewModel.getID(binding.editRtNewCategory.selectedItem.toString(),
                    binding.editRtNewSubcategory.selectedItem.toString()),
                    SpenderViewModel.getSpenderIndex(binding.editRtNewPaidBy.selectedItem.toString()),
                    SpenderViewModel.getSpenderIndex(binding.editRtNewBoughtFor.selectedItem.toString()),
                    binding.splitSlider.value.toInt()
                )
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        } else if (oldName == "") { // ie this is an add
            if (RecurringTransactionViewModel.nameExists(binding.editRtNewName.text.toString().trim())) {
                showErrorMessage(parentFragmentManager, "This name is already in use.")
                focusAndOpenSoftKeyboard(requireContext(), binding.editRtNewName)
                return
            }
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(),
                amountInt, rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(),
                binding.editRtNewNextDate.text.toString(),
                CategoryViewModel.getID(binding.editRtNewCategory.selectedItem.toString(),
                binding.editRtNewSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editRtNewPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editRtNewBoughtFor.selectedItem.toString()),
                binding.transactionBoughtForName1Split.text.toString().toInt())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        } else if (oldName != binding.editRtNewName.text.toString()) {
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(), amountInt,
                rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(),
                binding.editRtNewNextDate.text.toString(),
                CategoryViewModel.getID(binding.editRtNewCategory.selectedItem.toString(),
                binding.editRtNewSubcategory.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editRtNewPaidBy.selectedItem.toString()),
                SpenderViewModel.getSpenderIndex(binding.editRtNewBoughtFor.selectedItem.toString()),
                binding.transactionBoughtForName1Split.text.toString().toInt())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(oldName)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }
    }

    fun setDialogFragmentListener(listener: RecurringTransactionEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}