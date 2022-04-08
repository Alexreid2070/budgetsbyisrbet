package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import java.util.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionBinding
import kotlin.math.round
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.color.MaterialColors

class TransactionFragment : Fragment() {
    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionFragmentArgs by navArgs()
    private var newTransactionMode: Boolean = true
    private var editingKey: String = ""
    private var inExpandMode = false
    private var cal = android.icu.util.Calendar.getInstance()
    private var startingTransactionNote = ""
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

        inflater.inflate(R.layout.fragment_transaction, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                radioButton.text.toString() == "Joint" -> {
                    binding.transactionBoughtForName1Split.text =
                        SpenderViewModel.getSpenderSplit(0).toString()
                    binding.transactionBoughtForName2Split.text =
                        SpenderViewModel.getSpenderSplit(1).toString()
                    binding.slider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
                    if (newTransactionMode)
                        binding.slider.isEnabled = true
                }
                radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                    binding.transactionBoughtForName1Split.text = "100"
                    binding.transactionBoughtForName2Split.text = "0"
                    binding.slider.value = 100.0F
                }
                else -> {
                    binding.transactionBoughtForName1Split.text = "0"
                    binding.transactionBoughtForName2Split.text = "100"
                    binding.slider.value = 0.0F
                }
            }
            if (newTransactionMode)
                binding.slider.isEnabled = radioButton.text == "Joint"
        }
        binding.paidByRadioGroup.setOnCheckedChangeListener { _, _ ->
            if (binding.inputBoughtForLabel.visibility == View.GONE) { // need to keep both values in sync
                val selectedId = binding.paidByRadioGroup.checkedRadioButtonId
                val pbRadioButton = requireActivity().findViewById(selectedId) as RadioButton

                val bfRadioGroup =
                    requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
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
                if (binding.editTextAmount.text.toString() == "") {
                    binding.transactionName1Amount.text = "0"
                    binding.transactionName2Amount.text = "0"
                } else {
                    val amount1 = binding.editTextAmount.text.toString().toDouble() * binding.slider.value.toInt() / 100.0
                    binding.transactionName1Amount.text = gDec.format(amount1)
                    binding.transactionName2Amount.text =
                        gDec.format(binding.editTextAmount.text.toString().toDouble() - amount1)
                }
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.buttonPrevTransaction.setOnClickListener {
            viewTransaction(ExpenditureViewModel.getPreviousKey(binding.transactionId.text.toString()))
        }
        binding.buttonNextTransaction.setOnClickListener {
            viewTransaction(ExpenditureViewModel.getNextKey(binding.transactionId.text.toString()))
        }
        binding.buttonEdit.setOnClickListener {
            if (binding.transactionType.text.toString() == "Transfer") {
                val action =
                    TransactionFragmentDirections.actionTransactionFragmentToTransferFragment()
                action.mode = "edit"
                action.transactionID = binding.transactionId.text.toString()
                findNavController().navigate(action)
            }
            else
                editTransaction(args.transactionID)
        }
        binding.buttonDelete.setOnClickListener {
            deleteTransaction(args.transactionID)
        }
        binding.buttonSaveTransaction.setOnClickListener {
            onSaveTransactionButtonClicked()
        }
        binding.buttonCancelTransaction.setOnClickListener {
            activity?.onBackPressed()
        }
        if (DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) == "On") {
            binding.buttonLoadTransactionFromTdmyspend.setOnClickListener {
                onLoadTransactionButtonClicked()
            }
        }
        binding.slider.addOnChangeListener { _, _, _ ->
            binding.transactionBoughtForName1Split.text = binding.slider.value.toInt().toString()
            binding.transactionBoughtForName2Split.text = (100-binding.slider.value.toInt()).toString()
            if (binding.editTextAmount.text.toString() == "") {
                binding.transactionName1Amount.text = "0"
                binding.transactionName2Amount.text = "0"
            } else {
                val amount1 = binding.editTextAmount.text.toString().toDouble() * binding.slider.value.toInt() / 100.0
                binding.transactionName1Amount.text = gDec.format(amount1)
                binding.transactionName2Amount.text =
                    gDec.format(binding.editTextAmount.text.toString().toDouble() - amount1)
            }
        }
        loadCategoryRadioButtons()
        loadSpenderRadioButtons()

        val editAmountText = binding.editTextAmount
        editAmountText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                val str = editAmountText.text.toString()
                if (str.isEmpty()) return
                val str2: String = perfectDecimal(str, 5, 2)
                if (str2 != str) {
                    editAmountText.setText(str2)
                    editAmountText.setSelection(str2.length)
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
        if (!SpenderViewModel.singleUser()) {
            binding.transactionBoughtForName1Label.text = SpenderViewModel.getSpenderName(0)
            binding.transactionBoughtForName2Label.text = SpenderViewModel.getSpenderName(1)
        }

        if (newTransactionMode) {
            binding.expansionLayout.visibility = View.GONE
            binding.pageTitle.text = "Add Expense"
            binding.inputPaidByLabel.text = "Who:"
            binding.recurringTransactionLabel.visibility = View.GONE
            if (CustomNotificationListenerService.getExpenseNotificationCount() > 0 &&
                DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) == "On") {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.VISIBLE
            } else {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            }
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
            binding.inputSubcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
            binding.inputSubcategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
            if (!SpenderViewModel.singleUser()) {
                val selectedId = binding.boughtForRadioGroup.checkedRadioButtonId
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                when (radioButton.text) {
                    "Joint" -> {
                        binding.transactionBoughtForName1Split.text =
                            SpenderViewModel.getSpenderSplit(0).toString()
                        binding.transactionBoughtForName2Split.text =
                            SpenderViewModel.getSpenderSplit(1).toString()
                        binding.slider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
                    }
                    SpenderViewModel.getSpenderName(0) -> {
                        binding.transactionBoughtForName1Split.text = "100"
                        binding.transactionBoughtForName2Split.text = "0"
                        binding.slider.value = 100.0F
                    }
                    else -> {
                        binding.transactionBoughtForName1Split.text = "0"
                        binding.transactionBoughtForName2Split.text = "100"
                        binding.slider.value = 0.0F
                    }
                }
                binding.transactionName1Amount.text = "0"
                binding.transactionName2Amount.text = "0"
            } else {
                binding.transactionBoughtForName1Split.text = "100"
                binding.transactionBoughtForName2Split.text = "0"
                binding.slider.value = 100.0F
                binding.transactionName1Amount.text = "0"
                binding.transactionName2Amount.text = "0"
            }
        }
        else {
            binding.buttonCancelTransaction.visibility = View.GONE
            binding.buttonSaveTransaction.visibility = View.GONE
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.recurringTransactionLabel.isEnabled = false
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
//            binding.transactionBoughtForName1Split.isEnabled = false
//            binding.transactionBoughtForName2Split.isEnabled = false
            binding.slider.isEnabled = false
            viewTransaction(args.transactionID)
            binding.editTextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextNote.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.categoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.inputSubcategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.paidByRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.boughtForRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
//            binding.transactionBoughtForName1Split.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
//            binding.transactionBoughtForName2Split.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
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
                if (event2.y > event1.y) {
                    // negative for up, positive for down
                    Log.d("Alex", "swiped down ")
                    if (binding.inputBoughtForLabel.visibility == View.GONE) { // ie expand the section
                        inExpandMode = true
                        setExpansionFields(View.VISIBLE)
                    }
                } else if (event2.y < event1.y) {
                    Log.d("Alex", "swiped up ")
                    if (binding.inputBoughtForLabel.visibility == View.VISIBLE) { // ie retract the section
                        inExpandMode = false
                        setExpansionFields(View.GONE)
                    }
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
        HintViewModel.showHint(requireContext(), binding.editTextAmount, "Transaction")
    }

/*    private fun colorCategoryArea(iColor:Int) {
//        binding.inputCategoryLabel.setBackgroundColor(ColorUtils.setAlphaComponent(iColor, 20))
//        binding.categoryRadioGroup.setBackgroundColor(ColorUtils.setAlphaComponent(iColor, 20))
//        binding.inputSpinnerLayout.setBackgroundColor(ColorUtils.setAlphaComponent(iColor, 20))
//        binding.inputSubcategorySpinner.setBackgroundColor(ColorUtils.setAlphaComponent(iColor, 20))
        binding.inputCategoryLabel.setBackgroundColor(iColor)
        binding.categoryRadioGroup.setBackgroundColor(iColor)
        binding.inputSpinnerLayout.setBackgroundColor(iColor)
        binding.inputSubcategorySpinner.setBackgroundColor(iColor)
    } */
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

    @SuppressLint("SetTextI18n")
    private fun editTransaction(iTransactionID: String) {
        Log.d("Alex", "editing $iTransactionID")
        var currentCategory = ""
        Log.d("Alex", "clicked edit")
        binding.pageTitle.text = "Edit Transaction"
        binding.buttonCancelTransaction.visibility = View.VISIBLE
        binding.buttonSaveTransaction.visibility = View.VISIBLE
        binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        binding.expansionLayout.visibility = View.GONE
        binding.editTextDate.isEnabled = true
        binding.editTextAmount.isEnabled = true
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
        if (radioButton.text == "Joint") {
//            binding.transactionBoughtForName1Split.isEnabled = true
//            binding.transactionBoughtForName2Split.isEnabled = true
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
            ExpenditureViewModel.deleteTransaction(binding.editTextDate.text.toString(), iTransactionID)
            Toast.makeText(activity, "Transaction deleted", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            MyApplication.playSound(context, R.raw.short_springy_gun)
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Are you sure that you want to delete this transaction?")
            .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun viewTransaction(iTransactionID: String) {
        val thisTransaction = ExpenditureViewModel.getExpenditure(iTransactionID)
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            if (thisTransaction.type == "Transfer") {
                binding.pageTitle.text = "View Transfer"
                binding.inputCategoryLabel.visibility = View.GONE
                binding.categoryRadioGroup.visibility = View.GONE
                binding.inputSubcategoryLabel.visibility = View.GONE
                binding.inputSpinnerRelativeLayout.visibility = View.GONE
            } else {
                binding.pageTitle.text = "View Expense"
                binding.inputCategoryLabel.visibility = View.VISIBLE
                binding.categoryRadioGroup.visibility = View.VISIBLE
                binding.inputSubcategoryLabel.visibility = View.VISIBLE
                binding.inputSpinnerRelativeLayout.visibility = View.VISIBLE
            }
            val iAmount = thisTransaction.amount
            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(gDec.format(formattedAmount))
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
            binding.editTextNote.setText(thisTransaction.note)
            binding.recurringTransactionLabel.visibility = View.VISIBLE
            binding.transactionTypeLayout.visibility = View.VISIBLE
            binding.transactionType.setText(thisTransaction.type)
            if (thisTransaction.type == "Recurring") {
                binding.recurringTransactionLabel.text = "This recurring transaction was automatically generated."
                binding.recurringTransactionLabel.visibility = View.VISIBLE
            } else {
                binding.recurringTransactionLabel.visibility = View.INVISIBLE
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
            binding.transactionBoughtForName1Split.text = thisTransaction.bfname1split.toString()
            binding.slider.value = thisTransaction.bfname1split.toFloat()
            binding.transactionBoughtForName2Split.text = thisTransaction.getSplit2().toString()

            binding.transactionName1Amount.text = gDec.format(thisTransaction.amount/100.0 * thisTransaction.bfname1split / 100.0)
            binding.transactionName2Amount.text = gDec.format(thisTransaction.amount/100.0 * thisTransaction.getSplit2() / 100.0)

            if ((thisTransaction.boughtfor == 2 && binding.slider.value.toInt() != SpenderViewModel.getSpenderSplit(0)) ||
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
            Log.d("Alex",
                "iTransactionID $iTransactionID was passed for edit but can't find the data"
            )
        }
/*        if (inExpandMode)
            setExpansionFields(View.VISIBLE)
        else
            setExpansionFields(View.GONE) */
    }

    @SuppressLint("SetTextI18n")
    private fun setExpansionFields(iView: Int) {
        if (iView == View.GONE) {
            binding.transactionExpandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
            binding.inputPaidByLabel.text = "Who:"
        } else {
            binding.transactionExpandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
            binding.inputPaidByLabel.text = "Paid by:"
        }
        binding.inputBoughtForLabel.visibility = iView
        binding.boughtForRadioGroup.visibility = iView
        binding.sliderLayout.visibility = iView
        binding.transactionBoughtForNameLayout.visibility = iView
        binding.transactionBoughtForNameLayout2.visibility = iView
    }

    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        val subCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
/*        if (newTransactionMode) {
            if (arrayAdapter != null) {
                subCategorySpinner.setSelection(
                    arrayAdapter.getPosition(
                        DefaultsViewModel.getDefault(
                            cDEFAULT_SUBCATEGORY)))
            }
        } */
        subCategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        arrayAdapter.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    private fun onLoadTransactionButtonClicked() {
        val notification = CustomNotificationListenerService.getTransactionFromNotificationAndDeleteIt()
        if (notification == null) {
            Toast.makeText(activity,
                "Having issue with this TD notification.  Logged in DB.  Please inform Alex.  You'll have to handle this TD notification manually.",
                Toast.LENGTH_SHORT).show()
            return
        }
        binding.editTextAmount.setText(notification.amount.toString())
        val iNote = notification.note.lowercase()
        startingTransactionNote = iNote
        val translatedNote = TranslationViewModel.getTranslation(iNote)
        if (iNote == translatedNote) {
            val words = iNote.split(" ")
            var newStr = ""
            words.forEach { s ->
                newStr += s.replaceFirstChar { it.uppercase() } + " "
            }
            binding.editTextNote.setText(newStr)
        } else {
            binding.translatedNoteMessage.visibility = View.VISIBLE
            binding.translatedNoteMessage.text = "Translated from: $startingTransactionNote"
            binding.editTextNote.setText(translatedNote)
        }
        if (CustomNotificationListenerService.getExpenseNotificationCount() == 0) {
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        } else {
            // there are more notifications, but we don't want the user to click the button again until the current data is saved
            binding.buttonLoadTransactionFromTdmyspend.isEnabled = false
        }
    }

    private fun onSaveTransactionButtonClicked () {
        if (!textIsSafeForValue(binding.editTextNote.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.editTextNote.error="The text contains unsafe characters!"
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.editTextAmount.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            binding.editTextAmount.error=getString(R.string.missingAmountError)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }
        if (binding.editTextNote.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingNoteError))
            binding.editTextNote.error=getString(R.string.missingNoteError)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        val radioGroup = requireActivity().findViewById(R.id.categoryRadioGroup) as RadioGroup
        val radioButtonID = radioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
        Log.d("Alex", "Category is " + radioButton.text)

        val subcategorySpinner = requireActivity().findViewById(R.id.inputSubcategorySpinner) as Spinner
        Log.d("Alex", "Sub-category is " + subcategorySpinner.selectedItem.toString())
        Log.d("Alex", "Note is " + binding.editTextNote.text)

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
            binding.editTextAmount.error = "You are attempting to add to your private category, so transaction must be in your name."
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }

        val amountInt: Int
        val tempDouble : Double = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = tempDouble.toInt()
        val bfName1Split = binding.transactionBoughtForName1Split.text.toString().toInt()

        if (startingTransactionNote != "" && startingTransactionNote != binding.editTextNote.text.toString().trim()) {
            // ie the user loaded the transaction from a TD MySpend, and then edited the Note.  We
            // want to keep track of this 'translation' and use it going forward
            if (!TranslationViewModel.exists(startingTransactionNote))
                TranslationViewModel.addTranslation(startingTransactionNote, binding.editTextNote.text.toString().trim())
        }
        startingTransactionNote = ""
        binding.translatedNoteMessage.visibility = View.GONE
        binding.translatedNoteMessage.text = ""
        if (newTransactionMode) {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt, CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString()),
                binding.editTextNote.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                bfName1Split
            )
            ExpenditureViewModel.addTransaction(expenditure)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transaction added", Toast.LENGTH_SHORT).show()

            if (CustomNotificationListenerService.getExpenseNotificationCount() != 0) {
                binding.buttonLoadTransactionFromTdmyspend.isEnabled = true
            }
        } else {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt,
                CategoryViewModel.getID(radioButton.text.toString(), subcategorySpinner.selectedItem.toString()),
                binding.editTextNote.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                bfName1Split, binding.transactionType.text.toString()
            )

           ExpenditureViewModel.updateTransaction(editingKey, expenditure)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transaction updated", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }

    private fun loadCategoryRadioButtons() {
        var ctr = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
        if (radioGroup == null) Log.d("Alex", " rg is null")
        else radioGroup.removeAllViews()

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
        if (paidByRadioGroup == null) Log.d("Alex", " rg 'paidby' is null")
        else paidByRadioGroup.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            Log.d("Alex", "looking at paid by ${spender?.name} ($newTransactionMode ${SpenderViewModel.getDefaultSpenderName()}")
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
                Log.d("Alex", "paid by ${spender.name}")
            }
        }
        ctr = 200
        val boughtForRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
        if (boughtForRadioGroup == null) Log.d("Alex", " rg 'boughtfor' is null")
        else boughtForRadioGroup.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            Log.d("Alex", "looking at bought for ${spender?.name}")
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
                Log.d("Alex", "bought for ${spender.name}")
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
