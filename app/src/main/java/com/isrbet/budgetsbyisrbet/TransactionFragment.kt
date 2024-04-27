package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.icu.text.NumberFormat
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
import com.google.android.material.snackbar.Snackbar
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionBinding
import timber.log.Timber
import java.util.*
import kotlin.math.round


enum class Mode {
    View,
    Edit,
    New
}

class TransactionFragment : Fragment() {
    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionFragmentArgs by navArgs()
    private var transactionMode: Mode = Mode.New
    private var editingKey: String = ""
    private var inExpandMode = false
    private var startingTransactionWhere = ""
    private var startingTransactionCategory = 0
    private var gestureDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transactionMode = if (args.transactionID == "")
            Mode.New
        else
            Mode.View
//        newTransactionMode = args.transactionID == ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)

        binding.transactionAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        inflater.inflate(R.layout.fragment_transaction, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = String.format("${getLocalCurrencySymbol()} ")
        binding.transactionDate.setText(gCurrentDate.toString())

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.transactionDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.transactionDate.setOnClickListener {
            var lDate = MyDate()
            if (binding.transactionDate.text.toString() != "") {
                lDate = MyDate(binding.transactionDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                lDate.getYear(),
                lDate.getMonth()-1,
                lDate.getDay()
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
                    if (transactionMode != Mode.View)
                        binding.slider.isEnabled = true
                }
                radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                    binding.slider.value = 100.0F
                    binding.slider.isEnabled = false
                }
                else -> {
                    binding.slider.value = 0.0F
                    binding.slider.isEnabled = false
                }
            }
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())
            if (transactionMode != Mode.View)
                binding.slider.isEnabled = radioButton.text == getString(R.string.joint)
            else
                binding.slider.isEnabled = false
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
        binding.transactionAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())
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
        binding.buttonSave.setOnClickListener {
            onSaveTransactionButtonClicked()
        }
        binding.buttonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
//            activity?.onBackPressed()
        }
        if (DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
            binding.buttonLoadTransactionFromBankNotification.setOnClickListener {
                onLoadTransactionButtonClicked()
            }
        }
        binding.slider.addOnChangeListener { _, _, _ ->
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())
        }
        loadCategoryRadioButtons()
        loadSpenderRadioButtons()
        loadInsurableRadioButtons()

        binding.transactionAmount.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                val str = binding.transactionAmount.text.toString()
                if (str.isEmpty()) return
                val str2: String = perfectDecimal(str, gMaxNumbersBeforeDecimalPlace, gMaxNumbersAfterDecimalPlace)
                if (str2 != str) {
                    binding.transactionAmount.setText(str2)
                    binding.transactionAmount.setSelection(str2.length)
                }
            }
        })

        binding.categoryRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.categoryRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            var subCategory = ""
            val cd = CategoryViewModel.getCategoryDefault(radioButton.text.toString())
            val subCat = CategoryViewModel.getCategory(cd)
            if (subCat != null)
                subCategory = subCat.subcategoryName
            addSubCategories(radioButton.text.toString(), subCategory)
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
        if (SpenderViewModel.singleUser() || transactionMode == Mode.New) {
            setExpansionFields(View.GONE)
        }
        if (SpenderViewModel.multipleUsers()) {
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())
        }

        if (transactionMode == Mode.New) {
//            binding.slider.isEnabled = true
            binding.transactionAmount.isEnabled = true
//            binding.transactionAmount.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
            binding.expansionLayout.visibility = View.GONE
            binding.pageTitle.text = getString(R.string.add_expense)
            binding.inputPaidByLabel.text = getString(R.string.who)
            binding.scheduledPaymentLabel.visibility = View.GONE
            if (CustomNotificationListenerService.getExpenseNotificationCount() > 0 &&
                DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
                binding.buttonLoadTransactionFromBankNotification.visibility = View.VISIBLE
            } else {
                binding.buttonLoadTransactionFromBankNotification.visibility = View.GONE
            }
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
            binding.subcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
            binding.subcategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
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
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())
        } else {
            binding.slider.isEnabled = false
            binding.buttonViewLinearLayout.visibility = View.VISIBLE
            binding.buttonCancel.visibility = View.GONE
            binding.buttonSave.visibility = View.GONE
            binding.buttonLoadTransactionFromBankNotification.visibility = View.GONE
            binding.transactionDate.isEnabled = false
            binding.transactionAmount.isEnabled = false
//            binding.transactionAmount.inputType = InputType.TYPE_CLASS_TEXT
            binding.where.isEnabled = false
            binding.note.isEnabled = false
            binding.scheduledPaymentLabel.isEnabled = false
            binding.transactionType.isEnabled = false
            binding.subcategorySpinner.isEnabled = false
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
            binding.insurableSwitch.isEnabled = false
            binding.reimbursementAmount0.isEnabled = false
            binding.reimbursementAmount1.isEnabled = false
            viewTransaction(args.transactionID)
            for (i in 0 until binding.paidTo0RadioGroup.childCount) {
                (binding.paidTo0RadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.paidTo1RadioGroup.childCount) {
                (binding.paidTo1RadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
            binding.transactionDate.setBackgroundColor(Color.parseColor(hexColor))
            binding.transactionAmount.setBackgroundColor(Color.parseColor(hexColor))
            binding.where.setBackgroundColor(Color.parseColor(hexColor))
            binding.note.setBackgroundColor(Color.parseColor(hexColor))
            binding.categoryRadioGroup.setBackgroundColor(Color.parseColor(hexColor))
            binding.subcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
            binding.paidByRadioGroup.setBackgroundColor(Color.parseColor(hexColor))
            binding.boughtForRadioGroup.setBackgroundColor(Color.parseColor(hexColor))
            binding.slider.setBackgroundColor(Color.parseColor(hexColor))
            binding.reimbursementAmount0.setBackgroundColor(Color.parseColor(hexColor))
            binding.reimbursementAmount1.setBackgroundColor(Color.parseColor(hexColor))
            binding.paidTo0RadioGroup.setBackgroundColor(Color.parseColor(hexColor))
            binding.paidTo1RadioGroup.setBackgroundColor(Color.parseColor(hexColor))
//            binding.entireInputAmountArea.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        gestureDetector = GestureDetectorCompat(requireActivity(), object:
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent?,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    if (event1 == null)
                        return false
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
        binding.transactionAmount.requestFocus()
        HintViewModel.showHint(parentFragmentManager, cHINT_TRANSACTION)

        binding.transactionView.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            //        view?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                if (transactionMode == Mode.View) {
                    super.onSwipeLeft()
                    viewTransaction(TransactionViewModel.getNextKey(binding.transactionId.text.toString()))
                }
            }

            override fun onSwipeRight() {
                if (transactionMode == Mode.View) {
                    super.onSwipeRight()
                    viewTransaction(TransactionViewModel.getPreviousKey(binding.transactionId.text.toString()))
                }
            }
        })

        binding.insurableSwitch.setOnCheckedChangeListener { _, _ ->
            setupInsurableFields()
        }
    }

    private fun setupInsurableFields() {
        if (binding.insurableSwitch.isChecked) {
            if (SpenderViewModel.singleUser()) {
                binding.reimbursedAmount0Layout.visibility = View.VISIBLE
                binding.paidToLabel0.visibility = View.GONE
                binding.paidTo0RadioGroup.visibility = View.GONE
//                loadInsurableRadioButtons()
                binding.reimbursementAmount0.hint = String.format(getString(R.string.name_reimbursement), SpenderViewModel.getSpenderName(0))
            } else {
                binding.reimbursedAmount0Layout.visibility = View.VISIBLE
                binding.reimbursedAmount1Layout.visibility = View.VISIBLE
//                loadInsurableRadioButtons()
                binding.reimbursementAmount0.hint = String.format(getString(R.string.name_reimbursement), SpenderViewModel.getSpenderName(0))
                binding.reimbursementAmount1.hint = String.format(getString(R.string.name_reimbursement), SpenderViewModel.getSpenderName(1))
            }
        } else {
            binding.reimbursedAmount0Layout.visibility = View.GONE
            binding.reimbursedAmount1Layout.visibility = View.GONE
        }
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
        transactionMode = Mode.Edit
        var currentCategory = ""
//        binding.slider.isEnabled = true
        binding.buttonViewLinearLayout.visibility = View.GONE
        binding.pageTitle.text = getString(R.string.edit_transaction)
        binding.buttonCancel.visibility = View.VISIBLE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonLoadTransactionFromBankNotification.visibility = View.GONE
        binding.expansionLayout.visibility = View.GONE
        binding.transactionDate.isEnabled = true
        binding.transactionAmount.isEnabled = true
//        binding.transactionAmount.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
//        binding.transactionAmount.keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")
        binding.where.isEnabled = true
        binding.note.isEnabled = true
        binding.subcategorySpinner.isEnabled = true
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
        if (radioButton.text == getString(R.string.joint) &&
                transactionMode != Mode.View) {
            binding.slider.isEnabled = true
        }
        binding.insurableSwitch.isEnabled = true
        binding.reimbursementAmount0.isEnabled = true
        binding.reimbursementAmount1.isEnabled = true
        for (i in 0 until binding.paidTo0RadioGroup.childCount) {
            (binding.paidTo0RadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.paidTo1RadioGroup.childCount) {
            (binding.paidTo1RadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }

        val currentSubCategory = binding.subcategorySpinner.selectedItem.toString()
        addSubCategories(currentCategory, currentSubCategory)
        if (MyApplication.adminMode) {
            binding.transactionType.isEnabled = true
        }
    }

    private fun deleteTransaction(iTransactionID: String) {
        fun yesClicked() {
            TransactionViewModel.deleteTransactionDatabase(iTransactionID)
            TransactionViewModel.deleteTransactionLocal(iTransactionID)
            Toast.makeText(activity, getString(R.string.transaction_deleted), Toast.LENGTH_SHORT).show()
            MyApplication.playSound(requireContext(), R.raw.short_springy_gun)
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
        transactionMode = Mode.View
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
            binding.transactionAmount.setText(gDecM(thisTransaction.amount))
            binding.transactionId.text = iTransactionID
            binding.categoryId.text = thisTransaction.category.toString()
//            if (thisTransaction.insurable) {
                binding.insurableSwitch.isChecked = thisTransaction.insurable
                setupInsurableFields()
                binding.reimbursementAmount0.setText(gDecM(thisTransaction.reimbursementAmount0))
                binding.reimbursementAmount1.setText(gDecM(thisTransaction.reimbursementAmount1))
                for (i in 0 until binding.paidTo0RadioGroup.childCount) {
                    val o = binding.paidTo0RadioGroup.getChildAt(i)
                    if (o is RadioButton) {
                        Timber.tag("Alex").d("o1.text is ${o.text} and name is ${SpenderViewModel.getSpenderName(thisTransaction.paidTo0)}")
                        Timber.tag("Alex").d("Equals? ${o.text == SpenderViewModel.getSpenderName(thisTransaction.paidTo0)}")
                        o.isChecked = o.text == SpenderViewModel.getSpenderName(thisTransaction.paidTo0)
                    }
                }
                for (i in 0 until binding.paidTo1RadioGroup.childCount) {
                    val o = binding.paidTo1RadioGroup.getChildAt(i)
                    if (o is RadioButton) {
                        Timber.tag("Alex").d("o2.text is ${o.text} and name is ${SpenderViewModel.getSpenderName(thisTransaction.paidTo1)}")
                        Timber.tag("Alex").d("Equals? ${o.text == SpenderViewModel.getSpenderName(thisTransaction.paidTo1)}")
                        o.isChecked = o.text == SpenderViewModel.getSpenderName(thisTransaction.paidTo1)
                    }
                }
//            }
            if (MyApplication.adminMode) {
                binding.transactionIdLayout.visibility = View.VISIBLE
                binding.transactionId.visibility = View.VISIBLE
                binding.categoryId.visibility = View.VISIBLE
                binding.transactionType.visibility = View.VISIBLE
                binding.rtKey.visibility = View.VISIBLE
            }

            for (i in 0 until binding.categoryRadioGroup.childCount) {
                val o = binding.categoryRadioGroup.getChildAt(i)
                if (o is RadioButton &&
                    o.text == CategoryViewModel.getCategory(thisTransaction.category)?.categoryName) {
                    o.isChecked = true
                }
            }

            val subCategoryList: MutableList<String> = ArrayList()
            subCategoryList.add(CategoryViewModel.getCategory(thisTransaction.category)?.subcategoryName.toString())
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                subCategoryList
            )

            binding.subcategorySpinner.adapter = arrayAdapter
            binding.subcategorySpinner.setSelection(arrayAdapter.getPosition(
                CategoryViewModel.getCategory(thisTransaction.category)?.subcategoryName))

            binding.transactionDate.setText(thisTransaction.date.toString())
            binding.where.setText(thisTransaction.note)
            binding.note.setText(thisTransaction.note2)
            binding.scheduledPaymentLabel.visibility = View.VISIBLE
            binding.transactionType.visibility = View.VISIBLE
            binding.transactionType.text = thisTransaction.type
            binding.rtKey.text = thisTransaction.rtkey
            if (thisTransaction.type == getString(R.string.scheduled)) {
                binding.scheduledPaymentLabel.text = getString(R.string.this_expense_was_automatically_generated)
                binding.scheduledPaymentLabel.visibility = View.VISIBLE
            } else {
                binding.scheduledPaymentLabel.visibility = View.INVISIBLE
            }

            for (i in 0 until binding.paidByRadioGroup.childCount) {
                val o = binding.paidByRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.paidby)) {
                        o.isChecked = true
                    }
                }
            }
            for (i in 0 until binding.boughtForRadioGroup.childCount) {
                val o = binding.boughtForRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.boughtfor)) {
                        o.isChecked = true
                    }
                }
            }
            binding.slider.value = thisTransaction.bfname1split.toFloat()
            binding.splitText.text = getSplitText(binding.slider.value.toInt(), binding.transactionAmount.text.toString())

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
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            CategoryViewModel.getSubcategoriesForSpinner(iCategory, iSubCategory))
        binding.subcategorySpinner.adapter = arrayAdapter
        binding.subcategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        arrayAdapter.notifyDataSetChanged()
    }

    private fun onLoadTransactionButtonClicked() {
        val notification = CustomNotificationListenerService.getTransactionFromNotificationAndDeleteIt()
        if (notification == null) {
            Toast.makeText(activity,
                "Having issue with this banking notification.  Logged in DB.  Please inform Alex.  You'll have to handle this banking notification manually.",
                Toast.LENGTH_SHORT).show()
            return
        }
//        binding.transactionAmount.setText("${notification.amount}")
        binding.transactionAmount.setText(gDecM(notification.amount))
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
            binding.where.setText(newStr)
        } else {
            binding.translatedWhereMessage.visibility = View.VISIBLE
            binding.translatedWhereMessage.text = String.format("${getString(R.string.translated_from)} $startingTransactionWhere")
            binding.where.setText(translation.after)

            // find current category ID
            val categoryGroup = requireActivity().findViewById(R.id.categoryRadioGroup) as RadioGroup
            val radioButtonID = categoryGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
            val currentCatID = CategoryViewModel.getID(radioButton.text.toString(), binding.subcategorySpinner.selectedItem.toString())
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
                Toast.makeText(activity, getString(R.string.category_has_been_updated), Toast.LENGTH_LONG).show()
            }
        }
            binding.buttonLoadTransactionFromBankNotification.visibility = View.GONE
    }

    private fun onSaveTransactionButtonClicked () {
        if (!textIsSafeForValue(binding.where.text.toString())) {
            binding.where.error = getString(R.string.field_has_invalid_character)
            focusAndOpenSoftKeyboard(requireContext(), binding.where)
            return
        }
        if (!textIsSafeForValue(binding.note.text.toString())) {
            binding.note.error = getString(R.string.field_has_invalid_character)
            focusAndOpenSoftKeyboard(requireContext(), binding.note)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.transactionAmount.text.toString() == "") {
            binding.transactionAmount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.transactionAmount)
            return
        }
        if (binding.where.text.toString() == "") {
            binding.where.error=getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.where)
            return
        }
        if (binding.insurableSwitch.isChecked) {
            if (SpenderViewModel.singleUser()) {
                val b = binding.paidTo0RadioGroup.getChildAt(0)
                binding.paidTo0RadioGroup.check(b.id)
            } else {
                if (binding.reimbursementAmount0.text.toString() != "") {
                    val tAmount = binding.reimbursementAmount0.text.toString().toDoubleOrNull()!!
                    val checked = binding.paidTo0RadioGroup.checkedRadioButtonId
                    if (checked == -1 && tAmount != 0.0) {
                        binding.reimbursementAmount0.error =
                            getString(R.string.must_indicate_account)
                        focusAndOpenSoftKeyboard(requireContext(), binding.reimbursementAmount0)
                        return
                    }
                }
                if (binding.reimbursementAmount1.text.toString() != "") {
                    val tAmount = binding.reimbursementAmount1.text.toString().toDoubleOrNull()!!
                    val checked = binding.paidTo1RadioGroup.checkedRadioButtonId
                    if (checked == -1 && tAmount != 0.0) {
                        binding.reimbursementAmount1.error =
                            getString(R.string.must_indicate_account)
                        focusAndOpenSoftKeyboard(requireContext(), binding.reimbursementAmount1)
                        return
                    }
                }
            }
        }
        val lNumberFormat: NumberFormat = NumberFormat.getInstance()
        val amountD = lNumberFormat.parse(binding.transactionAmount.text.toString()).toDouble()
        val amountR0 = if (binding.reimbursementAmount0.text.toString() == "") 0.0 else
            lNumberFormat.parse(binding.reimbursementAmount0.text.toString()).toDouble()
        val amountR1 = if (binding.reimbursementAmount1.text.toString() == "") 0.0 else
            lNumberFormat.parse(binding.reimbursementAmount1.text.toString()).toDouble()

        val catRadioButtonID = binding.categoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catRadioButtonID) as RadioButton
        val radioButtonPaidByChecked = binding.paidByRadioGroup.checkedRadioButtonId
        val radioButtonPaidBy = requireActivity().findViewById(radioButtonPaidByChecked) as RadioButton
        val radioButtonBoughtForChecked = binding.boughtForRadioGroup.checkedRadioButtonId
        val radioButtonBoughtFor = requireActivity().findViewById(radioButtonBoughtForChecked) as RadioButton
        val radioButtonPaidTo0Checked = binding.paidTo0RadioGroup.checkedRadioButtonId
        val radioButtonPaidTo0 = if (radioButtonPaidTo0Checked == -1) -1 else {
            val rb = requireActivity().findViewById(radioButtonPaidTo0Checked) as RadioButton
            Timber.tag("Alex").d("0 rb.text is '${rb.text} and index is ${SpenderViewModel.getSpenderIndex(rb.text.toString())}")
            if (amountR0 == 0.0)
                -1
            else
                SpenderViewModel.getSpenderIndex(rb.text.toString())
        }
        val radioButtonPaidTo1Checked = binding.paidTo1RadioGroup.checkedRadioButtonId
        val radioButtonPaidTo1 = if (radioButtonPaidTo1Checked == -1) -1 else {
            val rb = requireActivity().findViewById(radioButtonPaidTo1Checked) as RadioButton
            Timber.tag("Alex").d("1 rb.text is '${rb.text} and index is ${SpenderViewModel.getSpenderIndex(rb.text.toString())}")
            if (amountR1 == 0.0)
                -1
            else
                SpenderViewModel.getSpenderIndex(rb.text.toString())
        }


        val chosenCatID = CategoryViewModel.getID(catRadioButton.text.toString(), binding.subcategorySpinner.selectedItem.toString())
        val chosenCat = CategoryViewModel.getCategory(chosenCatID)
        val paidByID = SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString())
        val boughtForID = SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString())

        if (chosenCat?.private != 2 &&
            (paidByID != MyApplication.userIndex) &&
            (boughtForID != MyApplication.userIndex)) {
            binding.transactionAmount.error = getString(R.string.you_are_attempting_to_add_to_your_private_category)
            focusAndOpenSoftKeyboard(requireContext(), binding.transactionAmount)
            return
        }

        if ((startingTransactionWhere != "" && startingTransactionWhere != binding.where.text.toString().trim()) ||
            (startingTransactionCategory != 0 && startingTransactionCategory != chosenCatID)) {
            // ie the user loaded the transaction from a TD MySpend, and then edited the Where.  We
            // want to keep track of this 'translation' and use it going forward
//            if (!TranslationViewModel.exists(startingTransactionWhere))
                TranslationViewModel.updateTranslation("", startingTransactionWhere,
                    binding.where.text.toString().trim(),
                    chosenCatID)
        }
        startingTransactionWhere = ""
        startingTransactionCategory = 0
        binding.translatedWhereMessage.visibility = View.GONE
        binding.translatedWhereMessage.text = ""
        if (transactionMode == Mode.New) {
            val transactionOut = TransactionOut(
                binding.transactionDate.text.toString(),
                round(amountD * 100).toInt(),
                CategoryViewModel.getID(catRadioButton.text.toString(), binding.subcategorySpinner.selectedItem.toString()),
                binding.where.text.toString().trim(),
                binding.note.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                binding.slider.value.toInt(),
                cTRANSACTION_TYPE_EXPENSE,
                "",
                binding.insurableSwitch.isChecked,
                if (binding.insurableSwitch.isChecked) round(amountR0 * 100).toInt() else 0,
                if (binding.insurableSwitch.isChecked) radioButtonPaidTo0 else -1,
                if (binding.insurableSwitch.isChecked) round(amountR1 * 100).toInt() else 0,
                if (binding.insurableSwitch.isChecked) radioButtonPaidTo1 else -1
            )
            binding.transactionAmount.setText("")
            binding.transactionAmount.requestFocus()
            binding.where.setText("")
            binding.note.setText("")
            hideKeyboard(requireContext(), requireView())
            TransactionViewModel.addTransactionDatabase(transactionOut)

            val actuals = TransactionViewModel.getActualsForPeriod(chosenCatID,
                MyDate(binding.transactionDate.text.toString()),
                MyDate(binding.transactionDate.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()))
            val budgetMonth = MyDate(binding.transactionDate.text.toString())
            budgetMonth.setDay(1)
            val budget = BudgetViewModel.getCalculatedBudgetAmount(DateRangeEnum.MONTH,
                budgetMonth,
                chosenCatID,
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()))

/*            Toast.makeText(activity, String.format(getString(R.string.transaction_added),
                radioButtonBoughtFor.text.toString(),
                gDecWithCurrency(actuals),
                gDecWithCurrency(budget)), Toast.LENGTH_LONG).show()*/

            val trView = binding.transactionView
            val trDate = MyDate(binding.transactionDate.text.toString())

            val amountString: String =
                NumberFormat.getCurrencyInstance(Locale("en", "US")).format(amountD)

            val snackbar = Snackbar.make(trView,
                String.format(getString(R.string.transaction_added),
                    NumberFormat.getCurrencyInstance(Locale("en", "US")).format(amountD),
                    trDate.getMonthName(),
                    NumberFormat.getCurrencyInstance(Locale("en", "US")).format(actuals),
                    NumberFormat.getCurrencyInstance(Locale("en", "US")).format(budget)),
                Snackbar.LENGTH_LONG).setDuration(7000)
            snackbar.setAction("X") {
                snackbar.dismiss()
            }
            snackbar.setActionTextColor(Color.RED)
            val snackbarView = snackbar.view
            val textView =
                snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            val snackbarActionView =
                snackbarView.findViewById(com.google.android.material.R.id.snackbar_action) as TextView
            textView.setTextColor(Color.BLACK)
            if (actuals > budget) {
                snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                snackbarActionView.setTextColor(Color.BLACK)
            } else {
                snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                snackbarActionView.setTextColor(Color.RED)
            }
            textView.textSize = 16f
            snackbar.show()

            if (CustomNotificationListenerService.getExpenseNotificationCount() != 0) {
                binding.buttonLoadTransactionFromBankNotification.isEnabled = true
            }
        } else {
            val transactionOut = TransactionOut(
                binding.transactionDate.text.toString(),
                round(amountD * 100).toInt(),
                CategoryViewModel.getID(catRadioButton.text.toString(), binding.subcategorySpinner.selectedItem.toString()),
                binding.where.text.toString().trim(),
                binding.note.text.toString().trim(),
                SpenderViewModel.getSpenderIndex(radioButtonPaidBy.text.toString()),
                SpenderViewModel.getSpenderIndex(radioButtonBoughtFor.text.toString()),
                binding.slider.value.toInt(),
                binding.transactionType.text.toString(),
                binding.rtKey.text.toString(),
                binding.insurableSwitch.isChecked,
                if (binding.insurableSwitch.isChecked) round(amountR0 * 100).toInt() else 0,
                if (binding.insurableSwitch.isChecked) radioButtonPaidTo0 else -1,
                if (binding.insurableSwitch.isChecked) round(amountR1 * 100).toInt() else 0,
                if (binding.insurableSwitch.isChecked) radioButtonPaidTo1 else -1
            )

           TransactionViewModel.updateTransactionDatabase(editingKey, transactionOut)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, getString(R.string.transaction_updated), Toast.LENGTH_SHORT).show()
        }
//        activity?.onBackPressed()
        MyApplication.playSound(requireContext(), R.raw.impact_jaw_breaker)
        requireActivity().onBackPressedDispatcher.onBackPressed()
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
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = it
            newRadioButton.id = ctr++
            radioGroup.addView(newRadioButton)
            if (transactionMode == Mode.New && newRadioButton.text.toString() == CategoryViewModel.getDefaultCategory()?.categoryName) {
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
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            paidByRadioGroup.addView(newRadioButton)
            if (transactionMode == Mode.New && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
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
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            boughtForRadioGroup.addView(newRadioButton)
            if (transactionMode == Mode.New && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
                boughtForRadioGroup.check(newRadioButton.id)
            }
        }
    }

    private fun loadInsurableRadioButtons() {
        Timber.tag("Alex").d("in loadINsurableRadioButtons")
        var ctr = 300
        val paidTo0RG = requireActivity().findViewById<RadioGroup>(R.id.paidTo0RadioGroup)
        paidTo0RG?.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            if (i == 0 || i == 2) {
                val spender = SpenderViewModel.getSpender(i)
                val newRadioButton = RadioButton(requireContext())
                newRadioButton.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                newRadioButton.text = spender?.name
                newRadioButton.id = ctr++
                if (transactionMode == Mode.New && i == 0)
                    newRadioButton.isChecked = true
                paidTo0RG.addView(newRadioButton)
            }
        }
        ctr = 400
        val paidTo1RG = requireActivity().findViewById<RadioGroup>(R.id.paidTo1RadioGroup)
        paidTo1RG?.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            if (i == 1 || i == 2) {
                val spender = SpenderViewModel.getSpender(i)
                val newRadioButton = RadioButton(requireContext())
                newRadioButton.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                newRadioButton.text = spender?.name
                newRadioButton.id = ctr++
                if (transactionMode == Mode.New && i == 1)
                    newRadioButton.isChecked = true
                paidTo1RG.addView(newRadioButton)
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
