package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionBinding
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class TransactionFragment : Fragment() {
    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionFragmentArgs by navArgs()
    private var newTransactionMode: Boolean = true
    private var editingKey: String = ""
    private var inExpandMode = false
    private var cal = gCurrentDate.clone() as android.icu.util.Calendar // Calendar.getInstance()
    private var startingTransactionWhere = ""
    private var startingTransactionCategory = 0
    private var gestureDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newTransactionMode = args.transactionID == ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)

        binding.editTextAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        inflater.inflate(R.layout.fragment_transaction, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = String.format("${getLocalCurrencySymbol()} ")
        binding.editTextDate.setText(giveMeMyDateFormat(cal))

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editTextDate.setText(giveMeMyDateFormat(cal))
            }

        binding.editTextDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.transactionExpandButton.setOnClickListener {
            onExpandClicked()
        }

        binding.boughtForRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            when {
                radioButton.text.toString() == getString(R.string.joint) -> {
                    binding.slider.value = (SpenderViewModel.getSpenderSplit(0)*100).toFloat()
                    if (newTransactionMode)
                        binding.slider.isEnabled = true
                }
                radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                    binding.slider.value = 100.0F
                }
                else -> {
                    binding.slider.value = 0.0F
                }
            }
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())
            if (newTransactionMode)
                binding.slider.isEnabled = radioButton.text == getString(R.string.joint)
        }
        binding.paidByRadioGroup.setOnCheckedChangeListener { _, _ ->
            if (binding.inputBoughtForLabel.visibility == View.GONE) { // need to keep both values in sync
                val selectedId = binding.paidByRadioGroup.checkedRadioButtonId
                val pbRadioButton = requireActivity().findViewById(selectedId) as RadioButton

                val bfRadioGroup = binding.boughtForRadioGroup
                for (i in 0 until bfRadioGroup.childCount) {
                    val o = bfRadioGroup.getChildAt(i)
                    if (o is RadioButton) {
                        if (o.text == pbRadioButton.text) {
                            o.isChecked = true
                        }
                    }
                }
            }
        }
        binding.editTextAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.buttonPrevTransaction.setOnClickListener {
            viewTransaction(TransactionViewModel.getPreviousKey(binding.transactionId.text.toString()))
        }
        binding.buttonNextTransaction.setOnClickListener {
            viewTransaction(TransactionViewModel.getNextKey(binding.transactionId.text.toString()))
        }
        binding.buttonCredit.setOnClickListener {
            creditTransaction(args.transactionID)
        }
        binding.buttonEdit.setOnClickListener {
            if (binding.transactionType.text.toString() == cTRANSACTION_TYPE_TRANSFER) {
                val action =
                    TransactionFragmentDirections.actionTransactionFragmentToTransferFragment()
                action.mode = cMODE_EDIT
                action.transactionID = binding.transactionId.text.toString()
                findNavController().navigate(action)
            }
            else
                editTransaction()
        }
        binding.buttonDelete.setOnClickListener {
            deleteTransaction(args.transactionID)
        }
        binding.buttonSaveTransaction.setOnClickListener {
            onSaveTransactionButtonClicked()
        }
        binding.buttonCancelTransaction.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
//            activity?.onBackPressed()
        }
        if (DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
            binding.buttonLoadTransactionFromTdmyspend.setOnClickListener {
                onLoadTransactionButtonClicked()
            }
        }
        binding.slider.addOnChangeListener { _, _, _ ->
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())
        }
        loadCategoryRadioButtons()
        loadSpenderRadioButtons()

        binding.editTextAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                val str = binding.editTextAmount.text.toString()
                if (str.isEmpty()) return
                val str2: String = perfectDecimal(str, gMaxNumbersBeforeDecimalPlace, gMaxNumbersAfterDecimalPlace)
                if (str2 != str) {
                    binding.editTextAmount.setText(str2)
                    binding.editTextAmount.setSelection(str2.length)
                }
            }
        })

        binding.categoryRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.categoryRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            addSubCategories(radioButton.text.toString(), "")
//            val cat = DefaultsViewModel.getCategoryDetail(radioButton.text.toString())
//            if (cat.color != 0) {
//                colorCategoryArea(cat.color)
//            }
        }

        if (SpenderViewModel.singleUser()) {
            binding.inputPaidByLabel.visibility = View.GONE
            binding.paidByRadioGroup.visibility = View.GONE
            binding.boughtForRadioGroup.visibility = View.GONE
            binding.transactionExpandButton.visibility = View.GONE
        }
        if (SpenderViewModel.singleUser() || newTransactionMode) {
            setExpansionFields(View.GONE)
        }
        if (SpenderViewModel.multipleUsers()) {
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())
        }

        if (newTransactionMode) {
            binding.editTextAmount.isEnabled = true
//            binding.editTextAmount.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
            binding.expansionLayout.visibility = View.GONE
            binding.pageTitle.text = getString(R.string.add_expense)
            binding.inputPaidByLabel.text = getString(R.string.who)
            binding.scheduledPaymentLabel.visibility = View.GONE
            if (CustomNotificationListenerService.getExpenseNotificationCount() > 0 &&
                DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.VISIBLE
            } else {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            }
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
            binding.inputSubcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
            binding.inputSubcategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
            if (SpenderViewModel.multipleUsers()) {
                val selectedId = binding.boughtForRadioGroup.checkedRadioButtonId
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                when (radioButton.text) {
                    getString(R.string.joint) -> {
                        binding.slider.value = (SpenderViewModel.getSpenderSplit(0)*100).toFloat()
                    }
                    SpenderViewModel.getSpenderName(0) -> {
                        binding.slider.value = 100.0F
                    }
                    else -> {
                        binding.slider.value = 0.0F
                    }
                }
            } else {
                binding.slider.value = 100.0F
            }
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())
        }
        else {
            binding.buttonCancelTransaction.visibility = View.GONE
            binding.buttonSaveTransaction.visibility = View.GONE
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
//            binding.editTextAmount.inputType = InputType.TYPE_CLASS_TEXT
            binding.editTextWhere.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.scheduledPaymentLabel.isEnabled = false
            binding.transactionTypeLayout.isEnabled = false
            binding.inputSubcategorySpinner.isEnabled = false
            for (i in 0 until binding.categoryRadioGroup.childCount) {
                (binding.categoryRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.paidByRadioGroup.childCount) {
                (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.boughtForRadioGroup.childCount) {
                (binding.boughtForRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            binding.slider.isEnabled = false
            viewTransaction(args.transactionID)
            binding.editTextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextWhere.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextNote.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.categoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.inputSubcategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.paidByRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.boughtForRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.slider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.entireInputAmountArea.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        gestureDetector = GestureDetectorCompat(requireActivity(), object:
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    if (event2.y > event1.y) {
                        // negative for up, positive for down
                        if (binding.inputBoughtForLabel.visibility == View.GONE) { // ie expand the section
                            inExpandMode = true
                            setExpansionFields(View.VISIBLE)
                        }
                    } else if (event2.y < event1.y) {
                        if (binding.inputBoughtForLabel.visibility == View.VISIBLE) { // ie retract the section
                            inExpandMode = false
                            setExpansionFields(View.GONE)
                        }
                    }
                }
                catch (exception: Exception) {
                    Timber.tag("Alex").d("onFling crashed.  This is happening to Rheannon in production, hopefully this exception handler catches them now...")
                }
                return true
            }
        })
        binding.scrollView.setOnTouchListener { _, p1 ->
            if (p1 != null) {
                gestureDetector?.onTouchEvent(p1)
            }
            false
        }
        binding.editTextAmount.requestFocus()
        HintViewModel.showHint(parentFragmentManager, cHINT_TRANSACTION)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    private fun onExpandClicked() {
        if (binding.inputBoughtForLabel.visibility == View.GONE) { // ie expand the section
            inExpandMode = true
            setExpansionFields(View.VISIBLE)
        } else { // ie retract the section
            inExpandMode = false
            setExpansionFields(View.GONE)
        }
    }

    private fun editTransaction() {
        var currentCategory = ""
        binding.pageTitle.text = getString(R.string.edit_transaction)
        binding.buttonCancelTransaction.visibility = View.VISIBLE
        binding.buttonSaveTransaction.visibility = View.VISIBLE
        binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        binding.expansionLayout.visibility = View.GONE
        binding.editTextDate.isEnabled = true
        val tOldDate = LocalDate.parse(binding.editTextDate.text, DateTimeFormatter.ISO_DATE)
        cal.set(Calendar.YEAR, tOldDate.year)
        cal.set(Calendar.MONTH, tOldDate.monthValue-1)
        cal.set(Calendar.DAY_OF_MONTH, tOldDate.dayOfMonth)
        binding.editTextAmount.isEnabled = true
//        binding.editTextAmount.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
//        binding.editTextAmount.keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")
        binding.editTextWhere.isEnabled = true
        binding.editTextNote.isEnabled = true
        binding.inputSubcategorySpinner.isEnabled = true
        for (i in 0 until binding.categoryRadioGroup.childCount) {
            val button = binding.categoryRadioGroup.getChildAt(i) as RadioButton
            button.isEnabled = true
            if (button.isChecked)
                currentCategory = button.text.toString()
        }
        for (i in 0 until binding.paidByRadioGroup.childCount) {
            (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.boughtForRadioGroup.childCount) {
            (binding.boughtForRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        val selectedId = binding.boughtForRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        if (radioButton.text == getString(R.string.joint)) {
            binding.slider.isEnabled = true
        }

        val currentSubCategory = binding.inputSubcategorySpinner.selectedItem.toString()
        addSubCategories(currentCategory, currentSubCategory)
        if (MyApplication.adminMode) {
            binding.transactionTypeLayout.isEnabled = true
        }
    }

    private fun deleteTransaction(iTransactionID: String) {
        fun yesClicked() {
            TransactionViewModel.deleteTransaction(binding.editTextDate.text.toString(), iTransactionID)
            Toast.makeText(activity, getString(R.string.transaction_deleted), Toast.LENGTH_SHORT).show()
//            requireActivity().onBackPressed()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            MyApplication.playSound(context, R.raw.short_springy_gun)
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.are_you_sure))
            .setMessage(getString(R.string.are_you_sure_that_you_want_to_delete_this_item_NP))
            .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    private fun viewTransaction(iTransactionID: String) {
        val thisTransaction = TransactionViewModel.getTransaction(iTransactionID)
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            if (thisTransaction.type == cTRANSACTION_TYPE_TRANSFER) {
                binding.pageTitle.text = getString(R.string.view_transfer)
                binding.inputCategoryLabel.visibility = View.GONE
                binding.categoryRadioGroup.visibility = View.GONE
                binding.inputSubcategoryLabel.visibility = View.GONE
                binding.inputSpinnerRelativeLayout.visibility = View.GONE
            } else {
                when (thisTransaction.type) {
                    cTRANSACTION_TYPE_SCHEDULED -> binding.pageTitle.text = getString(R.string.view_scheduled_payment)
                    cTRANSACTION_TYPE_EXPENSE -> binding.pageTitle.text = getString(R.string.view_expense)
                    cTRANSACTION_TYPE_CREDIT -> binding.pageTitle.text = getString(R.string.view_credit)
                }
                binding.inputCategoryLabel.visibility = View.VISIBLE
                binding.categoryRadioGroup.visibility = View.VISIBLE
                binding.inputSubcategoryLabel.visibility = View.VISIBLE
                binding.inputSpinnerRelativeLayout.visibility = View.VISIBLE
            }
            if (thisTransaction.type == getString(R.string.credit))
                binding.buttonCredit.visibility = View.GONE
            else
                binding.buttonCredit.visibility = View.VISIBLE
//            val iAmount = thisTransaction.amount
//            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(gDecM(thisTransaction.amount))
            binding.transactionId.text = iTransactionID
            binding.categoryId.text = thisTransaction.category.toString()
            if (MyApplication.adminMode) {
                binding.transactionIdLayout.visibility = View.VISIBLE
                binding.transactionId.visibility = View.VISIBLE
                binding.categoryId.visibility = View.VISIBLE
                binding.transactionTypeLayout.visibility = View.VISIBLE
            }

            val categoryGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
            for (i in 0 until categoryGroup.childCount) {
                val o = categoryGroup.getChildAt(i)
                if (o is RadioButton &&
                    o.text == CategoryViewModel.getCategory(thisTransaction.category)?.categoryName) {
                    o.isChecked = true
                }
            }

            val subCategorySpinner =
                requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
            val subCategoryList: MutableList<String> = ArrayList()
            subCategoryList.add(CategoryViewModel.getCategory(thisTransaction.category)?.subcategoryName.toString())
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                subCategoryList
            )

            subCategorySpinner.adapter = arrayAdapter
            subCategorySpinner.setSelection(arrayAdapter.getPosition(
                CategoryViewModel.getCategory(thisTransaction.category)?.subcategoryName))

            binding.editTextDate.setText(thisTransaction.date)
            binding.editTextWhere.setText(thisTransaction.note)
            binding.editTextNote.setText(thisTransaction.note2)
            binding.scheduledPaymentLabel.visibility = View.VISIBLE
            binding.transactionTypeLayout.visibility = View.VISIBLE
            binding.transactionType.setText(thisTransaction.type)
            if (thisTransaction.type == MyApplication.getString(R.string.scheduled)) {
                binding.scheduledPaymentLabel.text = getString(R.string.this_expense_was_automatically_generated)
                binding.scheduledPaymentLabel.visibility = View.VISIBLE
            } else {
                binding.scheduledPaymentLabel.visibility = View.INVISIBLE
            }

            val pbRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.paidByRadioGroup)
            for (i in 0 until pbRadioGroup.childCount) {
                val o = pbRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.paidby)) {
                        o.isChecked = true
                    }
                }
            }
            val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
            for (i in 0 until bfRadioGroup.childCount) {
                val o = bfRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.boughtfor)) {
                        o.isChecked = true
                    }
                }
            }
            binding.slider.value = thisTransaction.bfname1split.toFloat()
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.editTextAmount.text.toString())

            if ((thisTransaction.boughtfor == 2 && binding.slider.value.toInt() !=
                        (SpenderViewModel.getSpenderSplit(0)*100).toInt()) ||
                thisTransaction.paidby != thisTransaction.boughtfor) {
                inExpandMode = true
                setExpansionFields(View.VISIBLE)
            }
            else {
                inExpandMode = false
                setExpansionFields(View.GONE)
            }
        }
        else { // this doesn't make sense...
            Timber.tag("Alex").d("iTransactionID $iTransactionID was passed for edit but can't find the data")
        }
/*        if (inExpandMode)
            setExpansionFields(View.VISIBLE)
        else
            setExpansionFields(View.GONE) */
    }

    private fun setExpansionFields(iView: Int) {
        if (iView == View.GONE) {
            binding.transactionExpandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
            binding.inputPaidByLabel.text = getString(R.string.who)
            binding.inputPaidByLabel.tooltipText = getString(R.string.toolTipWhoInput)
        } else {
            binding.transactionExpandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
            binding.inputPaidByLabel.text = getString(R.string.paid_by)
            binding.inputPaidByLabel.tooltipText = getString(R.string.toolTipPaidBy)
        }
        binding.inputBoughtForLabel.visibility = iView
        binding.boughtForRadioGroup.visibility = iView
        binding.sliderLayout.visibility = iView
        binding.splitText.visibility = iView
    }

    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        val subCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        subCategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        arrayAdapter.notifyDataSetChanged()
    }

    private fun onLoadTransactionButtonClicked() {
        val notification = CustomNotificationListenerService.getTransactionFromNotificationAndDeleteIt()
        if (notification == null) {
            Toast.makeText(activity,
                "Having issue with this TD notification.  Logged in DB.  Please inform Alex.  You'll have to handle this TD notification manually.",
                Toast.LENGTH_SHORT).show()
            return
        }
//        binding.editTextAmount.setText("${notification.amount}")
        binding.editTextAmount.setText(gDecM(notification.amount))
        val iWhere = notification.where.lowercase()
        startingTransactionWhere = iWhere
        startingTransactionCategory = if (binding.categoryId.text.toString() == "") 0
            else binding.categoryId.toString().toInt()
        val translation = TranslationViewModel.getTranslation(iWhere)
        if (translation == null) {
            val words = iWhere.split(" ")
            var newStr = ""
            words.forEach { s ->
                newStr += s.replaceFirstChar { it.uppercase() } + " "
            }
            binding.editTextWhere.setText(newStr)
        } else {
            binding.translatedWhereMessage.visibility = View.VISIBLE
            binding.translatedWhereMessage.text = String.format("${getString(R.string.translated_from)} $startingTransactionWhere")
            binding.editTextWhere.setText(translation.after)

            // find current category ID
            val categoryGroup = requireActivity().findViewById(R.id.categoryRadioGroup) as RadioGroup
            val radioButtonID = categoryGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
            val subcategorySpinner = requireActivity().findViewById(R.id.inputSubcategorySpinner) as Spinner
            val currentCatID = CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString())
            if (translation.category != 0 && currentCatID != translation.category) {
                for (i in 0 until categoryGroup.childCount) {
                    val o = categoryGroup.getChildAt(i)
                    if (o is RadioButton &&
                        o.text == CategoryViewModel.getCategory(translation.category)?.categoryName) {
                        o.isChecked = true
                    }
                }
                val category = CategoryViewModel.getCategory(translation.category)
                if (category != null)
                    addSubCategories(category.categoryName, category.subcategoryName)
/*                val subCategorySpinner =
                    requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
                val subCategoryList: MutableList<String> = ArrayList()
                subCategoryList.add(CategoryViewModel.getCategory(translation.category)?.subcategoryName.toString())
                val arrayAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    subCategoryList
                )
                subCategorySpinner.adapter = arrayAdapter
                subCategorySpinner.setSelection(arrayAdapter.getPosition(
                    CategoryViewModel.getCategory(translation.category)?.subcategoryName)) */
                Toast.makeText(activity, getString(R.string.category_has_been_updated), Toast.LENGTH_LONG).show()
            }
        }
//        if (CustomNotificationListenerService.getExpenseNotificationCount() == 0) {
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
//        } else {
//            // there are more notifications, but we don't want the user to click the button again until the current data is saved
//            binding.buttonLoadTransactionFromTdmyspend.isEnabled = false
//        }
    }

    private fun onSaveTransactionButtonClicked () {
        if (!textIsSafeForValue(binding.editTextWhere.text.toString())) {
            binding.editTextWhere.error = getString(R.string.field_has_invalid_character)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextWhere)
            return
        }
        if (!textIsSafeForValue(binding.editTextNote.text.toString())) {
            binding.editTextNote.error = getString(R.string.field_has_invalid_character)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.editTextAmount.text.toString() == "") {
            binding.editTextAmount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }
        if (binding.editTextWhere.text.toString() == "") {
            binding.editTextWhere.error=getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextWhere)
            return
        }
        val radioGroup = requireActivity().findViewById(R.id.categoryRadioGroup) as RadioGroup
        val radioButtonID = radioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton

        val subcategorySpinner = requireActivity().findViewById(R.id.inputSubcategorySpinner) as Spinner

        val radioGroupPaidBy = requireActivity().findViewById(R.id.paidByRadioGroup) as RadioGroup
        val radioButtonPaidByChecked = radioGroupPaidBy.checkedRadioButtonId
        val radioButtonPaidBy = requireActivity().findViewById(radioButtonPaidByChecked) as RadioButton

        val radioGroupBoughtFor = requireActivity().findViewById(R.id.boughtForRadioGroup) as RadioGroup
        val radioButtonBoughtForChecked = radioGroupBoughtFor.checkedRadioButtonId
        val radioButtonBoughtFor = requireActivity().findViewById(radioButtonBoughtForChecked) as RadioButton

        val chosenCatID = CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString())
        val chosenCat = CategoryViewModel.getCategory(chosenCatID)
        val paidByID = SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString())
        val boughtForID = SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString())

        if (chosenCat?.private != 2 &&
            (paidByID != MyApplication.userIndex) &&
            (boughtForID != MyApplication.userIndex)) {
            binding.editTextAmount.error = getString(R.string.you_are_attempting_to_add_to_your_private_category)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }

        val amountD = gNumberFormat.parse(binding.editTextAmount.text.toString()).toDouble()

        if ((startingTransactionWhere != "" && startingTransactionWhere != binding.editTextWhere.text.toString().trim()) ||
            (startingTransactionCategory != 0 && startingTransactionCategory != chosenCatID)) {
            // ie the user loaded the transaction from a TD MySpend, and then edited the Where.  We
            // want to keep track of this 'translation' and use it going forward
//            if (!TranslationViewModel.exists(startingTransactionWhere))
                TranslationViewModel.updateTranslation("", startingTransactionWhere,
                    binding.editTextWhere.text.toString().trim(),
                    chosenCatID)
        }
        startingTransactionWhere = ""
        startingTransactionCategory = 0
        binding.translatedWhereMessage.visibility = View.GONE
        binding.translatedWhereMessage.text = ""
        if (newTransactionMode) {
            val transactionOut = TransactionOut(
                binding.editTextDate.text.toString(),
                round(amountD * 100).toInt(),
                CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString()),
                binding.editTextWhere.text.toString().trim(),
                binding.editTextNote.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                binding.slider.value.toInt()
            )
            TransactionViewModel.addTransaction(transactionOut)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextWhere.setText("")
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            val actuals = TransactionViewModel.getActualsForPeriod(chosenCatID,
                MyDate(binding.editTextDate.text.toString()),
                MyDate(binding.editTextDate.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                true)
            val budget = BudgetViewModel.getCalculatedBudgetAmount(DateRangeEnum.MONTH,
                MyDate(binding.editTextDate.text.toString()),
                chosenCatID,
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()))

            Toast.makeText(activity, String.format(getString(R.string.transaction_added),
                radioButtonBoughtFor.text.toString(),
                gDecWithCurrency(actuals), gDecWithCurrency(budget)), Toast.LENGTH_LONG).show()

            if (CustomNotificationListenerService.getExpenseNotificationCount() != 0) {
                binding.buttonLoadTransactionFromTdmyspend.isEnabled = true
            }
        } else {
            val transactionOut = TransactionOut(
                binding.editTextDate.text.toString(),
                round(amountD * 100).toInt(),
                CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString()),
                binding.editTextWhere.text.toString().trim(),
                binding.editTextNote.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                binding.slider.value.toInt(),
                binding.transactionType.text.toString()
            )

           TransactionViewModel.updateTransaction(editingKey, transactionOut)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, getString(R.string.transaction_updated), Toast.LENGTH_SHORT).show()
        }
//        activity?.onBackPressed()
        requireActivity().onBackPressedDispatcher.onBackPressed()
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }

    private fun creditTransaction(iTransactionID: String) {
        val df = TransactionCreditDialogFragment.newInstance(iTransactionID)
        df.show(parentFragmentManager, getString(R.string.credit_expense))
    }

    private fun loadCategoryRadioButtons() {
        var ctr = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
        radioGroup?.removeAllViews()

        val categoryNames = CategoryViewModel.getCategoryNames()

        categoryNames.forEach {
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = it
            newRadioButton.id = ctr++
            radioGroup.addView(newRadioButton)
            if (newTransactionMode && newRadioButton.text.toString() == CategoryViewModel.getDefaultCategory()?.categoryName) {
                radioGroup.check(newRadioButton.id)
//                val cat = DefaultsViewModel.getCategoryDetail(newRadioButton.text.toString())
//                if (cat.color != 0) {
//                    colorCategoryArea(cat.color)
//                }
            }
        }
        addSubCategories(CategoryViewModel.getDefaultCategory()?.categoryName.toString(),
            CategoryViewModel.getDefaultCategory()?.subcategoryName.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        val paidByRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.paidByRadioGroup)
        paidByRadioGroup?.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            paidByRadioGroup.addView(newRadioButton)
            if (newTransactionMode && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
                paidByRadioGroup.check(newRadioButton.id)
            }
        }
        ctr = 200
        val boughtForRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
        boughtForRadioGroup?.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            boughtForRadioGroup.addView(newRadioButton)
            if (newTransactionMode && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
                boughtForRadioGroup.check(newRadioButton.id)
            }
        }
    }
}

class TransactionDialogFragment(private var iMessage: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(iMessage)
                .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
