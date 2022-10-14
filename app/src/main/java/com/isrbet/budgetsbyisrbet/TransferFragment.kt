package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransferBinding
import java.util.*
import kotlin.math.round

class TransferFragment : Fragment() {
    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!
    private val args: TransferFragmentArgs by navArgs()
    private var newTransferMode: Boolean = true
    private var editingKey: String = ""

    private var cal: android.icu.util.Calendar = android.icu.util.Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newTransferMode = args.transactionID == ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        binding.editTextAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transfer, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextDate.setText(giveMeMyDateFormat(cal))
        binding.currencySymbol.text = getLocalCurrencySymbol() + " "

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

        binding.buttonSave.setOnClickListener {
            onSaveButtonClicked()
        }
        binding.buttonCancel.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.splitSlider.addOnChangeListener { _, _, _ ->
            binding.splitText.text = getSplitText(binding.splitSlider.value.toInt(), binding.editTextAmount.text.toString())
        }
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
                binding.splitText.text = getSplitText(binding.splitSlider.value.toInt(), binding.editTextAmount.text.toString())
            }
        })

        if (SpenderViewModel.multipleUsers()) {
            binding.splitSlider.value = (SpenderViewModel.getSpenderSplit(0)*100).toFloat()
            binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0)*100).toInt(), binding.editTextAmount.text.toString())
        }
        if (newTransferMode) {
            binding.pageTitle.text = getString(R.string.add_transfer)
            binding.expansionLayout.visibility = View.GONE
            if (SpenderViewModel.multipleUsers()) {
                var button = binding.fromRadioGroup.getChildAt(0) as RadioButton
                button.isChecked = true
                button = binding.toRadioGroup.getChildAt(1) as RadioButton
                button.isChecked = true

                binding.splitLayout.visibility = View.GONE

                val selectedId = binding.toRadioGroup.checkedRadioButtonId
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                when {
                    radioButton.text.toString() == getString(R.string.joint) -> {
                        binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0)*100).toInt(), binding.editTextAmount.text.toString())
                    }
                    radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                        binding.splitText.text = getSplitText(100, binding.editTextAmount.text.toString())
                        binding.splitSlider.isEnabled = false
                    }
                    else -> {
                        binding.splitText.text = getSplitText(0, binding.editTextAmount.text.toString())
                        binding.splitSlider.isEnabled = false
                    }
                }
            }
        } else {
            binding.pageTitle.text = getString(R.string.view_transfer)
            binding.buttonSave.visibility = View.GONE
            binding.buttonCancel.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.splitSlider.isEnabled = false
            for (i in 0 until binding.fromRadioGroup.childCount) {
                (binding.fromRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.toRadioGroup.childCount) {
                (binding.toRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            viewTransfer(args.transactionID)
            binding.editTextDate.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.editTextAmount.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.editTextNote.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.fromRadioGroup.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.toRadioGroup.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.entireInputAmountArea.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            binding.splitText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
        }
        binding.editTextAmount.requestFocus()

//        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
//        binding.splitText.setBackgroundColor(Color.parseColor(hexColor))
        binding.toRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            when {
                radioButton.text.toString() == getString(R.string.joint) -> {
                    binding.splitSlider.value = (SpenderViewModel.getSpenderSplit(0)*100).toFloat()
                    binding.splitText.text = getSplitText(binding.splitSlider.value.toInt(), binding.editTextAmount.text.toString())
                }
                radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                    binding.splitSlider.value = 100.0F
                    binding.splitText.text = getSplitText(100, binding.editTextAmount.text.toString())
                }
                else -> {
                    binding.splitSlider.value = 0.0F
                    binding.splitText.text = getSplitText(0, binding.editTextAmount.text.toString())
                }
            }
            if (radioButton.text == getString(R.string.joint)) {
                binding.splitSlider.isEnabled = true
                binding.splitLayout.visibility = View.VISIBLE
            } else {
                binding.splitSlider.isEnabled = false
                binding.splitLayout.visibility = View.GONE
            }
        }
        binding.buttonPrevTransfer.setOnClickListener {
            viewTransfer(TransactionViewModel.getPreviousTransferKey(binding.transactionId.text.toString()))
        }
        binding.buttonNextTransfer.setOnClickListener {
            viewTransfer(TransactionViewModel.getNextTransferKey(binding.transactionId.text.toString()))
        }
        binding.buttonEdit.setOnClickListener {
            editTransfer()
        }
        binding.buttonDelete.setOnClickListener {
            deleteTransfer(args.transactionID)
        }
        if (args.mode == cMODE_EDIT)
            editTransfer()
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    private fun editTransfer() {
        binding.pageTitle.text = getString(R.string.edit_transfer)
        binding.expansionLayout.visibility = View.GONE
        binding.buttonEdit.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
        binding.buttonCancel.visibility = View.VISIBLE
        binding.buttonSave.visibility = View.VISIBLE
        binding.editTextDate.isEnabled = true
        binding.editTextAmount.isEnabled = true
        binding.editTextNote.isEnabled = true
        for (i in 0 until binding.fromRadioGroup.childCount) {
            (binding.fromRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.toRadioGroup.childCount) {
            (binding.toRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        val selectedId = binding.toRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        if (radioButton.text == getString(R.string.joint)) {
            binding.splitSlider.isEnabled = true
        }
    }

    private fun deleteTransfer(iTransactionID: String) {
        fun yesClicked() {
            TransactionViewModel.deleteTransaction(binding.editTextDate.text.toString(), iTransactionID)
            Toast.makeText(activity, getString(R.string.transfer_deleted), Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
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

    private fun viewTransfer(iTransactionID: String) {
        val thisTransaction = TransactionViewModel.getTransaction(iTransactionID)
        binding.transactionId.text = iTransactionID
        binding.categoryId.text = thisTransaction?.category.toString()
        if (MyApplication.adminMode) {
            binding.transactionIdLayout.visibility = View.VISIBLE
            binding.transactionId.visibility = View.VISIBLE
            binding.categoryId.visibility = View.VISIBLE
        }
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

//            val iAmount = thisTransaction.amount
//            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(gDec(thisTransaction.amount))
            binding.editTextDate.setText(thisTransaction.date)
            binding.editTextNote.setText(thisTransaction.note2)
            binding.splitSlider.value = thisTransaction.bfname1split.toFloat()
            binding.splitText.text = getSplitText(thisTransaction.bfname1split, thisTransaction.amount.toString())
            if (thisTransaction.boughtfor == 2) {
                binding.splitLayout.visibility = View.VISIBLE
            } else {
                binding.splitLayout.visibility = View.GONE
            }
            val pbRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.fromRadioGroup)
            for (i in 0 until pbRadioGroup.childCount) {
                val o = pbRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.paidby)) {
                        o.isChecked = true
                    }
                }
            }
            val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.toRadioGroup)
            for (i in 0 until bfRadioGroup.childCount) {
                val o = bfRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(thisTransaction.boughtfor)) {
                        o.isChecked = true
                    }
                }
            }
        }
    }

    private fun onSaveButtonClicked () {
        if (!textIsSafeForValue(binding.editTextNote.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.editTextNote.error = getString(R.string.field_has_invalid_character)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.editTextAmount.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            binding.editTextAmount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }
        if (binding.editTextNote.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingNoteError))
            binding.editTextNote.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        val fromRadioGroup = requireActivity().findViewById(R.id.fromRadioGroup) as RadioGroup
        val fromRadioButtonChecked = fromRadioGroup.checkedRadioButtonId
        val fromRadioButton = requireActivity().findViewById(fromRadioButtonChecked) as RadioButton

        val toRadioGroup = requireActivity().findViewById(R.id.toRadioGroup) as RadioGroup
        val toRadioGroupChecked = toRadioGroup.checkedRadioButtonId
        val toRadioButton = requireActivity().findViewById(toRadioGroupChecked) as RadioButton
        if (fromRadioButton.text.toString() == toRadioButton.text.toString()) {
            binding.editTextNote.error = getString(R.string.toFromCantBeSame)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }

        val amountDouble = gNumberFormat.parse(binding.editTextAmount.text.toString()).toDouble()

        if (newTransferMode) {
            val transfer = TransferOut(
                binding.editTextDate.text.toString(),
                (amountDouble * 100.0).toInt(),
                SpenderViewModel.getSpenderIndex(fromRadioButton.text.toString()),
                SpenderViewModel.getSpenderIndex(toRadioButton.text.toString()),
                binding.splitSlider.value.toInt(),
                "",
                binding.editTextNote.text.toString()
            )
            TransactionViewModel.addTransaction(transfer)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, getString(R.string.transfer_added), Toast.LENGTH_SHORT).show()

        } else {
            val transfer = TransferOut(
                binding.editTextDate.text.toString(),
                (amountDouble * 100.0).toInt(),
                SpenderViewModel.getSpenderIndex(fromRadioButton.text.toString()),
                SpenderViewModel.getSpenderIndex(toRadioButton.text.toString()),
                binding.splitSlider.value.toInt(),
                "",
                binding.editTextNote.text.toString()
            )

            TransactionViewModel.updateTransaction(editingKey, transfer)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, getString(R.string.transfer_updated), Toast.LENGTH_SHORT).show()
        }
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        activity?.onBackPressed()
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        val fromRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.fromRadioGroup)
        fromRadioGroup?.removeAllViews()

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
            fromRadioGroup.addView(newRadioButton)
            if (newTransferMode && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
                fromRadioGroup.check(newRadioButton.id)
            }
        }
        ctr = 200
        val toRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.toRadioGroup)
        toRadioGroup?.removeAllViews()

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
            toRadioGroup.addView(newRadioButton)
            if (newTransferMode && spender?.name == SpenderViewModel.getDefaultSpenderName()) {
                toRadioGroup.check(newRadioButton.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}