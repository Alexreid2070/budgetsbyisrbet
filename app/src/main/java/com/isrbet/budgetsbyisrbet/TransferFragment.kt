package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransferBinding
import java.text.DecimalFormat
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
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transfer, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
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

        binding.buttonSaveTransfer.setOnClickListener {
            onSaveButtonClicked()
        }
        binding.buttonCancelTransfer.setOnClickListener {
            activity?.onBackPressed()
        }

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

        if (!SpenderViewModel.singleUser()) {
            binding.splitName1Label.text = SpenderViewModel.getSpenderName(0)
            binding.splitName2Label.text = SpenderViewModel.getSpenderName(1)
        }
        if (newTransferMode) {
            binding.pageTitle.text = "Add Transfer"
            binding.expansionLayout.visibility = View.GONE
            if (!SpenderViewModel.singleUser()) {
                var button = binding.fromRadioGroup.getChildAt(0) as RadioButton
                button.isChecked = true
                button = binding.toRadioGroup.getChildAt(1) as RadioButton
                button.isChecked = true

                binding.splitLayout.visibility = View.GONE

                val selectedId = binding.toRadioGroup.checkedRadioButtonId
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                when {
                    radioButton.text.toString() == "Joint" -> {
                        binding.splitName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                        binding.splitName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
                    }
                    radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                        binding.splitName1Split.setText("100")
                        binding.splitName2Split.setText("0")
                        binding.splitName1Split.isEnabled = false
                        binding.splitName2Split.isEnabled = false
                    }
                    else -> {
                        binding.splitName1Split.setText("0")
                        binding.splitName2Split.setText("100")
                        binding.splitName1Split.isEnabled = false
                        binding.splitName2Split.isEnabled = false
                    }
                }
            }
        } else {
            binding.pageTitle.text = "View Transfer"
            binding.buttonSaveTransfer.visibility = View.GONE
            binding.buttonCancelTransfer.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.splitName1Split.isEnabled = false
            binding.splitName2Split.isEnabled = false
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
            binding.splitName1Split.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.splitName2Split.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
        }
        binding.editTextAmount.requestFocus()

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.splitName1Split.setBackgroundColor(Color.parseColor(hexColor))
        binding.splitName2Split.setBackgroundColor(Color.parseColor(hexColor))
        binding.toRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            when {
                radioButton.text.toString() == "Joint" -> {
                    binding.splitName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                    binding.splitName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
                }
                radioButton.text.toString() == SpenderViewModel.getSpenderName(0) -> {
                    binding.splitName1Split.setText("100")
                    binding.splitName2Split.setText("0")
                }
                else -> {
                    binding.splitName1Split.setText("0")
                    binding.splitName2Split.setText("100")
                }
            }
            if (radioButton.text == "Joint") {
                binding.splitName1Split.isEnabled = true
                binding.splitName2Split.isEnabled = true
                binding.splitLayout.visibility = View.VISIBLE
            } else {
                binding.splitName1Split.isEnabled = false
                binding.splitName2Split.isEnabled = false
                binding.splitLayout.visibility = View.GONE
            }
        }
        binding.buttonPrevTransfer.setOnClickListener {
            viewTransfer(ExpenditureViewModel.getPreviousTransferKey(binding.transactionId.text.toString()))
        }
        binding.buttonNextTransfer.setOnClickListener {
            viewTransfer(ExpenditureViewModel.getNextTransferKey(binding.transactionId.text.toString()))
        }
        binding.buttonEdit.setOnClickListener {
            editTransfer(args.transactionID)
        }
        binding.buttonDelete.setOnClickListener {
            deleteTransfer(args.transactionID)
        }
        if (args.mode == "edit")
            editTransfer(args.transactionID)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    @SuppressLint("SetTextI18n")
    private fun editTransfer(iTransactionID: String) {
        Log.d("Alex", "clicked on $iTransactionID")
        binding.pageTitle.text = "Edit Transfer"
        binding.expansionLayout.visibility = View.GONE
        binding.buttonEdit.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
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
        if (radioButton.text == "Joint") {
            binding.splitName1Split.isEnabled = true
            binding.splitName2Split.isEnabled = true
        }
    }

    private fun deleteTransfer(iTransactionID: String) {
        fun yesClicked() {
            ExpenditureViewModel.deleteTransaction(binding.editTextDate.text.toString(), iTransactionID)
            Toast.makeText(activity, "Transfer deleted", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            MyApplication.playSound(context, R.raw.short_springy_gun)
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Are you sure that you want to delete this transfer?")
            .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    private fun viewTransfer(iTransactionID: String) {
        Log.d("Alex", "viewTransfer key $iTransactionID")
        val thisTransaction = ExpenditureViewModel.getExpenditure(iTransactionID)
        binding.transactionId.text = iTransactionID
        binding.categoryId.text = thisTransaction?.category.toString()
        if (MyApplication.adminMode) {
            binding.transactionIdLayout.visibility = View.VISIBLE
            binding.transactionId.visibility = View.VISIBLE
            binding.categoryId.visibility = View.VISIBLE
        }
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            val iAmount = thisTransaction.amount
            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(gDec.format(formattedAmount))
            binding.editTextDate.setText(thisTransaction.date)
            binding.editTextNote.setText(thisTransaction.note)
            binding.splitName1Split.setText(thisTransaction.bfname1split.toString())
            binding.splitName2Split.setText(thisTransaction.getSplit2().toString())
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
        else { // this doesn't make sense...
            Log.d("Alex",
                "iTransactionID $iTransactionID was passed for edit but can't find the data"
            )
        }
    }

    private fun onSaveButtonClicked () {
        if (!textIsSafeForValue(binding.editTextNote.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.editTextNote.error = "The text contains unsafe characters."
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.editTextAmount.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            binding.editTextAmount.error = getString(R.string.missingAmountError)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }
        if (binding.editTextNote.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingNoteError))
            binding.editTextNote.error = getString(R.string.missingNoteError)
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        val totalSplit = binding.splitName1Split.text.toString().toInt() +
                binding.splitName2Split.text.toString().toInt()
        if (totalSplit != 100) {
            binding.splitName1Split.error=getString(R.string.splitMustEqual100)
            focusAndOpenSoftKeyboard(requireContext(), binding.splitName1Split)
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

        val amountInt: Int
        val amountDouble : Double = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = amountDouble.toInt()
        Log.d("Alex", "text is " + binding.editTextAmount.text + " and double is " + amountDouble.toString() + " and int is " + amountInt.toString())

        if (newTransferMode) {
            val transfer = TransferOut(
                binding.editTextDate.text.toString(),
                amountInt,
                SpenderViewModel.getSpenderIndex(fromRadioButton.text.toString()),
                SpenderViewModel.getSpenderIndex(toRadioButton.text.toString()),
                binding.splitName1Split.text.toString().toInt()
            )
            ExpenditureViewModel.addTransaction(transfer)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transfer added", Toast.LENGTH_SHORT).show()

        } else {
            val transfer = TransferOut(
                binding.editTextDate.text.toString(),
                amountInt,
                SpenderViewModel.getSpenderIndex(fromRadioButton.text.toString()),
                SpenderViewModel.getSpenderIndex(toRadioButton.text.toString()),
                binding.splitName1Split.text.toString().toInt()
            )

            ExpenditureViewModel.updateTransaction(editingKey, transfer)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transfer updated", Toast.LENGTH_SHORT).show()
        }
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        activity?.onBackPressed()
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        val fromRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.fromRadioGroup)
        if (fromRadioGroup == null) Log.d("Alex", " rg 'from' is null")
        else fromRadioGroup.removeAllViews()

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
        if (toRadioGroup == null) Log.d("Alex", " rg 'to' is null")
        else toRadioGroup.removeAllViews()

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