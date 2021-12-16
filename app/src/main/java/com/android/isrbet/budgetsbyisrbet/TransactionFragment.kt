package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import kotlinx.android.synthetic.main.fragment_transaction.*
import java.util.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionBinding
import java.lang.String.format
import java.text.DecimalFormat
import kotlin.math.round
import android.widget.ArrayAdapter
import android.media.MediaPlayer
import androidx.core.content.ContextCompat

class TransactionFragment : Fragment() {
    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionFragmentArgs by navArgs<TransactionFragmentArgs>()
    private var newTransactionMode: Boolean = true
    private var editingKey: String = ""
    var cal = android.icu.util.Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args.transactionID == "") newTransactionMode = true
        else newTransactionMode = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)
        inflater.inflate(R.layout.fragment_transaction, container, false)
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

        binding.buttonSaveTransaction.setOnClickListener {
            onSaveTransactionButtonClicked()
        }

        binding.buttonLoadTransactionFromTdmyspend.setOnClickListener {
            onLoadTransactionButtonClicked()
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
                val str2: String = PerfectDecimal(str, 5, 2)
                if (str2 != str) {
                    editAmountText.setText(str2)
                    editAmountText.setSelection(str2.length)
                }
            }
        })

        binding.categoryRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = requireActivity().findViewById(checkedId)
            Log.d("Alex", "clicked on radio group" + checkedId.toString())
            val selectedId = binding.categoryRadioGroup.getCheckedRadioButtonId()
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            addSubCategories(radioButton.getText().toString())
        })

        if (newTransactionMode) {
            (activity as AppCompatActivity).supportActionBar?.title = "Add Transaction"
            binding.recurringTransactionIndicator.visibility = View.GONE
            binding.recurringTransactionViewLabel.visibility = View.GONE
//            binding.recurringTransactionIndicator.setText("No")
//            binding.recurringTransactionIndicator.isEnabled = false
            if (CustomNotificationListenerService.getExpenseNotificationCount() > 0) {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.VISIBLE
            } else {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            }
            binding.editTextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.editTextAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.editTextNote.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.categoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.inputSubcategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.whoRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.entireInputAmountArea.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        else {
            (activity as AppCompatActivity).supportActionBar?.title = "View Expenditure"
            binding.buttonSaveTransaction.visibility = View.GONE
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.recurringTransactionIndicator.isEnabled = false
            binding.inputSubcategorySpinner.isEnabled = false
            viewTransaction(args.transactionID)
            binding.editTextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.editTextNote.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.categoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.inputSubcategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.whoRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            binding.entireInputAmountArea.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        updateDashboardSummary()

        ExpenditureViewModel.singleInstance.setCallback(object: ExpenditureDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "got a callback that expenditure data was updated")
                updateDashboardSummary()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    fun updateDashboardSummary() {
        val dateNow = android.icu.util.Calendar.getInstance()
        val tomorrow = android.icu.util.Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        val daysInMonth = getDaysInMonth(dateNow)

        val dbPastTitle: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_past_title)
        dbPastTitle.text = "To " + giveMeMyDateFormat(dateNow)

        val dbRemTitle: TextView = requireActivity().findViewById(R.id.dashboard_summary_rem_title)
        dbRemTitle.text = "Rest of month"
//            (dateNow.get(Calendar.MONTH) + 1).toString() + " " + tomorrow.get(Calendar.DATE) + "-" + daysInMonth

        val tvTotalBudgetToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_total_budget_to_date)
        val totalDiscBudgetToDate =
            BudgetViewModel.getTotalDiscretionaryBudgetForMonth(dateNow) * dateNow.get(Calendar.DATE) / daysInMonth
        tvTotalBudgetToDate.text = format("%.2f", totalDiscBudgetToDate)
        val tvDailyBudgetToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_daily_budget_to_date)
        val dailyDiscBudgetToDate =
            round((totalDiscBudgetToDate / dateNow.get(android.icu.util.Calendar.DATE)) * 100) / 100
        tvDailyBudgetToDate.text = format("%.2f", dailyDiscBudgetToDate)

        val tvTotalActualsToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_total_actuals_to_date)
        val totalDiscActualsToDate =
            ExpenditureViewModel.getTotalDiscretionaryActualsToDate(dateNow)
        tvTotalActualsToDate.text = format("%.2f", totalDiscActualsToDate)
        val tvDailyActualsToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_daily_actuals_to_date)
        val dailyDiscActualsToDate =
            round((totalDiscActualsToDate / dateNow.get(android.icu.util.Calendar.DATE)) * 100) / 100
        tvDailyActualsToDate.text = format("%.2f", dailyDiscActualsToDate)

        val tvTotalDeltaToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_total_delta_to_date)
        tvTotalDeltaToDate.text = format("%.2f", (totalDiscBudgetToDate - totalDiscActualsToDate))
        val tvDailyDeltaToDate: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_daily_delta_to_date)
        val dailyDeltaToDate =
            round((totalDiscBudgetToDate - totalDiscActualsToDate) / dateNow.get(android.icu.util.Calendar.DATE) * 100) / 100
        tvDailyDeltaToDate.text = format("%.2f", dailyDeltaToDate)


        val totalBudgetRem =
            ((BudgetViewModel.getTotalDiscretionaryBudgetForMonth(dateNow) - totalDiscActualsToDate))
        val tvTotalBudgetRem: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_total_budget_rem)
        val tvDailyBudgetRem: TextView =
            requireActivity().findViewById(R.id.dashboard_summary_daily_budget_rem)
        tvTotalBudgetRem.text = format("%.2f", totalBudgetRem)
        if (totalBudgetRem > 0) {
            val dailyBudgetRem =
                round((totalBudgetRem / (daysInMonth - dateNow.get(android.icu.util.Calendar.DATE))) * 100) / 100
            tvDailyBudgetRem.text = format("%.2f", dailyBudgetRem)
        } else {
            tvDailyBudgetRem.text = "0.00"
            tvDailyBudgetRem.setTextColor(Color.RED);
        }

        val color = getBudgetColour(totalDiscActualsToDate, totalDiscBudgetToDate)
/*        tvTotalDeltaToDate.setTextColor(color)
        tvDailyDeltaToDate.setTextColor(color)
        tvTotalBudgetRem.setTextColor(color) */
        tvTotalDeltaToDate.setBackgroundColor(color)
        tvDailyDeltaToDate.setBackgroundColor(color)
        tvTotalBudgetRem.setBackgroundColor(color)
        tvDailyBudgetRem.setBackgroundColor(color)
    }
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (newTransactionMode) {
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).getItemId() === R.id.ViewTransactionsFragment)
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
            editTransaction(args.transactionID.toString())
            return true
        } else if (item.itemId === R.id.DeleteTransaction) {
            deleteTransaction(args.transactionID.toString())
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun editTransaction(iTransactionID: String) {
        Log.d("Alex", "clicked edit")
        binding.buttonSaveTransaction.visibility = View.VISIBLE
        binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        binding.editTextDate.isEnabled = true
        binding.editTextAmount.isEnabled = true
        binding.editTextNote.isEnabled = true
        binding.inputSubcategorySpinner.isEnabled = true
    }

    private fun deleteTransaction(iTransactionID: String) {
        fun yesClicked() {
            (activity as MainActivity).getMyExpenditureModel().deleteTransaction(iTransactionID)
            Toast.makeText(activity, "Transaction deleted", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Are you sure that you want to delete this transaction?")
            .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
            .show()
    }

    private fun viewTransaction(iTransactionID: String) {
        val thisTransaction = (activity as MainActivity).getMyExpenditureModel().getExpenditure(iTransactionID)
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            val iAmount = thisTransaction.amount
            val dec = DecimalFormat("#.00")
            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(dec.format(formattedAmount))

            val categoryGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
            for (i in 0 until categoryGroup.childCount) {
                val o = categoryGroup.getChildAt(i)
                if (o is RadioButton && o.text == thisTransaction.category) {
                    o.isChecked = true
                }
            }

            var subCategorySpinner =
                requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
            val subCategoryList: MutableList<String> = ArrayList()
            Log.d("Alex","adding subcategory to view " + thisTransaction.subcategory)
            subCategoryList.add(thisTransaction.subcategory)
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                subCategoryList
            )

            subCategorySpinner.adapter = arrayAdapter
            subCategorySpinner.setSelection(arrayAdapter.getPosition(thisTransaction.subcategory))

            binding.editTextDate.setText(thisTransaction.date)
            binding.editTextNote.setText(thisTransaction.note)
            binding.recurringTransactionIndicator.setText(thisTransaction.type)

            val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.whoRadioGroup)
            Log.d("Alex", "now in view who")
            for (i in 0 until radioGroup.childCount) {
                val o = radioGroup.getChildAt(i)
                if (o is RadioButton) {
                    Log.d("Alex", "o.text is " + o.text)
                    if (o.text == thisTransaction.who) {
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

    private fun addSubCategories(iCategory: String) {
        var subCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.inputSubcategorySpinner)
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        Log.d("Alex", "There are " + subCategorySpinner.adapter.count.toString())
        if (newTransactionMode)
            if (arrayAdapter != null) {
                subCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefault(
                    cDEFAULT_SUBCATEGORY)))
            }
        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged()
        }
    }

    private fun onLoadTransactionButtonClicked() {
        val notification = CustomNotificationListenerService.getTransactionFromNotificationAndDeleteIt()
        binding.editTextAmount.setText(notification.amount.toString())
        var iNote = notification.note.lowercase()
        iNote.replaceFirstChar { it.uppercase() }
        binding.editTextNote.setText(iNote)

        if (CustomNotificationListenerService.getExpenseNotificationCount() == 0) {
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        } else {
            // there are more notifications, but we don't want the user to click the button again until the current data is saved
            binding.buttonLoadTransactionFromTdmyspend.isEnabled = false
        }
    }

    private fun onSaveTransactionButtonClicked () {
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
        val radioGroup = requireActivity().findViewById(R.id.categoryRadioGroup) as RadioGroup
        val radioButtonID = radioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
        Log.d("Alex", "Category is " + radioButton.text)

        val subcategorySpinner = requireActivity().findViewById(R.id.inputSubcategorySpinner) as Spinner
        Log.d("Alex", "Sub-category is " + subcategorySpinner.selectedItem.toString())
        Log.d("Alex", "Note is " + binding.editTextNote.text)

        val radioGroup2 = requireActivity().findViewById(R.id.whoRadioGroup) as RadioGroup
        val radioButtonID2 = radioGroup2.checkedRadioButtonId
        val radioButton2 = requireActivity().findViewById(radioButtonID2) as RadioButton

        var amountDouble : Double
        var amountInt: Int
        amountDouble = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = amountDouble.toInt()
        Log.d("Alex", "text is " + binding.editTextAmount.text + " and double is " + amountDouble.toString() + " and int is " + amountInt.toString())

        if (newTransactionMode) {
            val expenditure = ExpenditureOut(
                editTextDate.text.toString(),
                amountInt, radioButton.text.toString(), subcategorySpinner.selectedItem.toString(),
                binding.editTextNote.text.toString(), radioButton2.text.toString()
            )
            ExpenditureViewModel.addTransaction(expenditure)
            binding.editTextAmount.setText("")
            binding.editTextAmount.requestFocus()
            binding.editTextNote.setText("")
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transaction added", Toast.LENGTH_SHORT).show()
            updateDashboardSummary()

            if (CustomNotificationListenerService.getExpenseNotificationCount() != 0) {
                binding.buttonLoadTransactionFromTdmyspend.isEnabled = true
            }
        } else {
            val expenditure = ExpenditureOut(
                editTextDate.text.toString(),
                amountInt, radioButton.text.toString(), subcategorySpinner.selectedItem.toString(),
                binding.editTextNote.text.toString(), radioButton2.text.toString()
            )

            (activity as MainActivity).getMyExpenditureModel().updateTransaction(editingKey, expenditure)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transaction updated", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
        mp.start()
    }

    fun loadCategoryRadioButtons() {
        var ctr: Int = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
        if (radioGroup == null) Log.d("Alex", " rg is null")
        else radioGroup.removeAllViews()

        var categoryNames = CategoryViewModel.getCategoryNames()

        categoryNames.forEach {
            var newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.setText(it.toString())
            newRadioButton.id = ctr++
            radioGroup.addView(newRadioButton)
            Log.d(
                "Alex",
                "Added new category radio button " + newRadioButton.text.toString() + " with id " + newRadioButton.id
            )
            if (newTransactionMode && newRadioButton.text.toString() == DefaultsViewModel.getDefault(
                    cDEFAULT_CATEGORY)) {
                Log.d("Alex", "found default " + newRadioButton.text.toString())
                radioGroup.check(newRadioButton.id)
            }
        }
        addSubCategories(DefaultsViewModel.getDefault(cDEFAULT_CATEGORY))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ExpenditureViewModel.singleInstance.setCallback(null)
        _binding = null
    }

    fun loadSpenderRadioButtons() {
        var ctr: Int
        ctr = 200
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.whoRadioGroup)
        if (radioGroup == null) Log.d("Alex", " rg 'who' is null")
        else radioGroup.removeAllViews()

        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            radioGroup.addView(newRadioButton)
            Log.d("Alex", "Added new 'who' radio button " + newRadioButton.text.toString() + " with id " + newRadioButton.id)
            if (newTransactionMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default spender " + spender)
                radioGroup.check(newRadioButton.id)
            }
        }
    }
}

class TransactionDialogFragment(var iMessage: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(iMessage)
                .setPositiveButton(getString(R.string.ok),
                    DialogInterface.OnClickListener { dialog, id ->
                    })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
