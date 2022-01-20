package com.isrbet.budgetsbyisrbet

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
import androidx.appcompat.app.AppCompatActivity
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

    var cal = android.icu.util.Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args.transactionID == "") newTransferMode = true
        else newTransferMode = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transfer, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextDate.setText(giveMeMyDateFormat(cal))

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
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

        binding.buttonSaveTransfer.setOnClickListener() {
            onSaveButtonClicked()
        }

        loadSpenderRadioButtons()
        val editAmountText = binding.editTextAmount
        editAmountText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                val str = editAmountText.text.toString()
                if (str.isEmpty()) return
                val str2: String = PerfectDecimal(str, 5, 2)
                if (str2 != str) {
                    editAmountText.setText(str2)
                    editAmountText.setSelection(str2.length)
                }
            }
        })

        if (SpenderViewModel.getCount() > 1) {
            binding.splitName1Label.text = SpenderViewModel.getSpenderName(0)
            binding.splitName2Label.text = SpenderViewModel.getSpenderName(1)
        }
        if (newTransferMode) {
            (activity as AppCompatActivity).supportActionBar?.title = "Add Transfer"
            if (SpenderViewModel.getCount() > 1) {
                var button = binding.fromRadioGroup.getChildAt(0) as RadioButton
                button.isChecked = true
                button = binding.toRadioGroup.getChildAt(1) as RadioButton
                button.isChecked = true
            }
            if (SpenderViewModel.getCount() > 1) {
                val selectedId = binding.toRadioGroup.getCheckedRadioButtonId()
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                if (radioButton.getText().toString() == "Joint") {
                    binding.splitName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                    binding.splitName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
                } else if (radioButton.getText().toString() == SpenderViewModel.getSpenderName(0)) {
                    binding.splitName1Split.setText("100")
                    binding.splitName2Split.setText("0")
                    binding.splitName1Split.isEnabled = false
                    binding.splitName2Split.isEnabled = false
                } else {
                    binding.splitName1Split.setText("0")
                    binding.splitName2Split.setText("100")
                    binding.splitName1Split.isEnabled = false
                    binding.splitName2Split.isEnabled = false
                }
            }
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = "View Transfer"
            binding.buttonSaveTransfer.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.splitName1Split.isEnabled = false
            binding.splitName2Split.isEnabled = false
            for (i in 0 until binding.fromRadioGroup.getChildCount()) {
                (binding.fromRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.toRadioGroup.getChildCount()) {
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
        binding.toRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            Log.d("Alex", "clicked on radio group" + radioButton.getText().toString())
            if (radioButton.getText().toString() == "Joint") {
                binding.splitName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                binding.splitName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
            } else if (radioButton.getText().toString() == SpenderViewModel.getSpenderName(0)) {
                binding.splitName1Split.setText("100")
                binding.splitName2Split.setText("0")
            } else {
                binding.splitName1Split.setText("0")
                binding.splitName2Split.setText("100")
            }
            if (radioButton.text == "Joint") {
                binding.splitName1Split.isEnabled = true
                binding.splitName2Split.isEnabled = true
            } else {
                binding.splitName1Split.isEnabled = false
                binding.splitName2Split.isEnabled = false
            }
        })
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (newTransferMode) {
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).getItemId() === R.id.ViewTransfersFragment)
                    menu.getItem(i).setVisible(true)
                else
                    menu.getItem(i).setVisible(false)
            }
        }
        else { // in view mode
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).getItemId() === R.id.Edit || menu.getItem(i).getItemId() == R.id.Delete)
                    menu.getItem(i).setVisible(true)
                else
                    menu.getItem(i).setVisible(false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.Edit) {
            editTransfer(args.transactionID.toString())
            return true
        } else if (item.itemId === R.id.Delete) {
            deleteTransfer(args.transactionID.toString())
            return true
        } else if (item.itemId === R.id.ViewTransfersFragment) {
            MyApplication.transactionSearchText = "Transfer"
            view?.findNavController()?.navigate(R.id.ViewTransactionsFragment)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }
    private fun editTransfer(iTransactionID: String) {
        Log.d("Alex", "clicked edit")
        binding.buttonSaveTransfer.visibility = View.VISIBLE
        binding.editTextDate.isEnabled = true
        binding.editTextAmount.isEnabled = true
        binding.editTextNote.isEnabled = true
        for (i in 0 until binding.fromRadioGroup.getChildCount()) {
            (binding.fromRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.toRadioGroup.getChildCount()) {
            (binding.toRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        val selectedId = binding.toRadioGroup.getCheckedRadioButtonId()
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        if (radioButton.text == "Joint") {
            binding.splitName1Split.isEnabled = true
            binding.splitName2Split.isEnabled = true
        }
    }

    private fun deleteTransfer(iTransactionID: String) {
        fun yesClicked() {
            (activity as MainActivity).getMyExpenditureModel().deleteTransaction(iTransactionID)
            Toast.makeText(activity, "Transfer deleted", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            MyApplication.playSound(context, R.raw.short_springy_gun)
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Are you sure that you want to delete this transfer?")
            .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
            .show()
    }

    private fun viewTransfer(iTransactionID: String) {
        val thisTransaction = (activity as MainActivity).getMyExpenditureModel().getExpenditure(iTransactionID)
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            val iAmount = thisTransaction.amount
            val dec = DecimalFormat("#.00")
            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(dec.format(formattedAmount))
            binding.editTextDate.setText(thisTransaction.date)
            binding.editTextNote.setText(thisTransaction.note)
            var tSplit = thisTransaction.bfname1split
            Log.d("Alex", "found split 1 " + tSplit)
            var dec2 = DecimalFormat("#.##")
            var formattedAmount2 = (tSplit/100).toDouble() + (tSplit % 100).toDouble()/100
            binding.splitName1Split.setText(dec2.format(formattedAmount2))

            tSplit = thisTransaction.bfname2split
            Log.d("Alex", "found split 2 " + tSplit)
            formattedAmount2 = (tSplit/100).toDouble() + (tSplit % 100).toDouble()/100
            binding.splitName2Split.setText(dec2.format(formattedAmount2))

            val pbRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.fromRadioGroup)
            Log.d("Alex", "now in view from")
            for (i in 0 until pbRadioGroup.childCount) {
                val o = pbRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    Log.d("Alex", "o.text is " + o.text)
                    if (o.text == thisTransaction.paidby) {
                        Log.d("Alex", "match")
                        o.isChecked = true
                    }
                }
            }
            val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.toRadioGroup)
            Log.d("Alex", "now in view to")
            for (i in 0 until bfRadioGroup.childCount) {
                val o = bfRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    Log.d("Alex", "o.text is " + o.text)
                    if (o.text == thisTransaction.boughtfor) {
                        Log.d("Alex", "match")
                        o.isChecked = true
                    }
                }
            }
        }
        else { // this doesn't make sense...
            Log.d("Alex", "iTransactionID "+ iTransactionID + " was passed for edit but can't find the data")
        }
    }

    fun onSaveButtonClicked () {
        if (!textIsSafe(binding.editTextNote.text.toString())) {
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
        var totalSplit = binding.splitName1Split.text.toString().toDouble() +
                binding.splitName2Split.text.toString().toDouble()
        if (totalSplit != 100.0) {
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

        var amountDouble : Double
        var amountInt: Int
        amountDouble = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = amountDouble.toInt()
        Log.d("Alex", "text is " + binding.editTextAmount.text + " and double is " + amountDouble.toString() + " and int is " + amountInt.toString())

        var bfName1Split: Int
        amountDouble = round(binding.splitName1Split.text.toString().toDouble()*100)
        bfName1Split = amountDouble.toInt()
        var bfName2Split: Int
        amountDouble = round(binding.splitName2Split.text.toString().toDouble()*100)
        bfName2Split = amountDouble.toInt()

        if (newTransferMode) {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt, "Transfer", "",
                binding.editTextNote.text.toString(), fromRadioButton.text.toString(),
                toRadioButton.text.toString(), bfName1Split, bfName2Split, "T"
            )
            ExpenditureViewModel.addTransaction(expenditure)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transfer added", Toast.LENGTH_SHORT).show()

        } else {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt, "Transfer", "",
                binding.editTextNote.text.toString(), fromRadioButton.text.toString(),
                toRadioButton.text.toString(), bfName1Split, bfName2Split,"T"
            )

            (activity as MainActivity).getMyExpenditureModel().updateTransaction(editingKey, expenditure)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transfer updated", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }

    fun loadSpenderRadioButtons() {
        var ctr: Int
        ctr = 200
        val fromRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.fromRadioGroup)
        if (fromRadioGroup == null) Log.d("Alex", " rg 'from' is null")
        else fromRadioGroup.removeAllViews()

        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            fromRadioGroup.addView(newRadioButton)
            if (newTransferMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default from " + spender)
                fromRadioGroup.check(newRadioButton.id)
            }
        }
        ctr = 200
        val toRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.toRadioGroup)
        if (toRadioGroup == null) Log.d("Alex", " rg 'to' is null")
        else toRadioGroup.removeAllViews()

        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            toRadioGroup.addView(newRadioButton)
            if (newTransferMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default to " + spender)
                toRadioGroup.check(newRadioButton.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}