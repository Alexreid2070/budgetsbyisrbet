package com.android.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.*
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

        if (newTransferMode) {
            (activity as AppCompatActivity).supportActionBar?.title = "Add Transfer"
            binding.editTextDate.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.robin_egg_blue
                )
            )
            binding.editTextAmount.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.robin_egg_blue
                )
            )
            binding.editTextNote.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.robin_egg_blue
                )
            )
            binding.paidByRadioGroup.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.robin_egg_blue
                )
            )
            binding.boughtForRadioGroup.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.robin_egg_blue
                )
            )
            binding.entireInputAmountArea.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = "View Transfer"
            binding.buttonSaveTransfer.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            for (i in 0 until binding.paidByRadioGroup.getChildCount()) {
                (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.boughtForRadioGroup.getChildCount()) {
                (binding.boughtForRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
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
            binding.paidByRadioGroup.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                )
            )
            binding.boughtForRadioGroup.setBackgroundColor(
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
        }
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
                if (menu.getItem(i).getItemId() === R.id.EditTransaction || menu.getItem(i).getItemId() == R.id.DeleteTransaction)
                    menu.getItem(i).setVisible(true)
                else
                    menu.getItem(i).setVisible(false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.EditTransaction) {
            editTransfer(args.transactionID.toString())
            return true
        } else if (item.itemId === R.id.DeleteTransaction) {
            deleteTransfer(args.transactionID.toString())
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
        for (i in 0 until binding.paidByRadioGroup.getChildCount()) {
            (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.boughtForRadioGroup.getChildCount()) {
            (binding.boughtForRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
    }

    private fun deleteTransfer(iTransactionID: String) {
        fun yesClicked() {
            (activity as MainActivity).getMyExpenditureModel().deleteTransaction(iTransactionID)
            Toast.makeText(activity, "Transfer deleted", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            val mp: MediaPlayer = MediaPlayer.create(context, R.raw.short_springy_gun)
            mp.start()
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

            val pbRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.paidByRadioGroup)
            Log.d("Alex", "now in view paidby")
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
            val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
            Log.d("Alex", "now in view boughtfor")
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
            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        // need to reject if all the fields aren't entered
        if (binding.editTextAmount.text.toString() == "") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextAmount)
            return
        }
        if (binding.editTextNote.text.toString() == "") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingNoteError))
            focusAndOpenSoftKeyboard(requireContext(), binding.editTextNote)
            return
        }
        val radioGroupPaidBy = requireActivity().findViewById(R.id.paidByRadioGroup) as RadioGroup
        val radioButtonPaidByChecked = radioGroupPaidBy.checkedRadioButtonId
        val radioButtonPaidBy = requireActivity().findViewById(radioButtonPaidByChecked) as RadioButton

        val radioGroupBoughtFor = requireActivity().findViewById(R.id.boughtForRadioGroup) as RadioGroup
        val radioButtonBoughtForChecked = radioGroupBoughtFor.checkedRadioButtonId
        val radioButtonBoughtFor = requireActivity().findViewById(radioButtonBoughtForChecked) as RadioButton

        var amountDouble : Double
        var amountInt: Int
        amountDouble = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = amountDouble.toInt()
        Log.d("Alex", "text is " + binding.editTextAmount.text + " and double is " + amountDouble.toString() + " and int is " + amountInt.toString())

        if (newTransferMode) {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt, "Transfer", "",
                binding.editTextNote.text.toString(), radioButtonPaidBy.text.toString(),
                radioButtonBoughtFor.text.toString(), "T"
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
                binding.editTextNote.text.toString(), radioButtonPaidBy.text.toString(),
                radioButtonBoughtFor.text.toString(), "T"
            )

            (activity as MainActivity).getMyExpenditureModel().updateTransaction(editingKey, expenditure)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transfer updated", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
        mp.start()
    }

    fun loadSpenderRadioButtons() {
        var ctr: Int
        ctr = 200
        val paidByRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.paidByRadioGroup)
        if (paidByRadioGroup == null) Log.d("Alex", " rg 'paidby' is null")
        else paidByRadioGroup.removeAllViews()

        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            paidByRadioGroup.addView(newRadioButton)
            Log.d("Alex", "Added new 'paidby' radio button " + newRadioButton.text.toString() + " with id " + newRadioButton.id)
            if (newTransferMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default paidby " + spender)
                paidByRadioGroup.check(newRadioButton.id)
            }
        }
        ctr = 200
        val boughtForRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
        if (boughtForRadioGroup == null) Log.d("Alex", " rg 'boughtfor' is null")
        else boughtForRadioGroup.removeAllViews()

        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            boughtForRadioGroup.addView(newRadioButton)
            Log.d("Alex", "Added new 'boughtfor' radio button " + newRadioButton.text.toString() + " with id " + newRadioButton.id)
            if (newTransferMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default boughtfor " + spender)
                boughtForRadioGroup.check(newRadioButton.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}