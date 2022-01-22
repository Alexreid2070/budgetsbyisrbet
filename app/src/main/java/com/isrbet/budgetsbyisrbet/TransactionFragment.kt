package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
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
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors

class TransactionFragment : Fragment() {
    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionFragmentArgs by navArgs()
    private var newTransactionMode: Boolean = true
    private var editingKey: String = ""
    private var inExpandMode = false
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

        binding.paidByRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, _ ->
            if (binding.inputBoughtForLabel.visibility == View.GONE) { // need to keep both values in sync
                val selectedId = binding.paidByRadioGroup.getCheckedRadioButtonId()
                val pbRadioButton = requireActivity().findViewById(selectedId) as RadioButton

                val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
                for (i in 0 until bfRadioGroup.childCount) {
                    val o = bfRadioGroup.getChildAt(i)
                    if (o is RadioButton) {
                        if (o.text == pbRadioButton.text) {
                            o.isChecked = true
                        }
                    }
                }
            }
        })

        binding.buttonPrevTransaction.setOnClickListener {
            viewTransaction(ExpenditureViewModel.getPreviousKey(binding.transactionId.text.toString()))
        }
        binding.buttonNextTransaction.setOnClickListener {
            viewTransaction(ExpenditureViewModel.getNextKey(binding.transactionId.text.toString()))
        }
        binding.buttonSaveTransaction.setOnClickListener {
            onSaveTransactionButtonClicked()
        }
        binding.buttonLoadTransactionFromTdmyspend.setOnClickListener {
            onLoadTransactionButtonClicked()
        }
        binding.slider.addOnChangeListener { slider, value, fromUser ->
            binding.transactionBoughtForName1Split.setText(binding.slider.value.toInt().toString())
            binding.transactionBoughtForName2Split.setText((100-binding.slider.value.toInt()).toString())
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

        binding.categoryRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedId ->
            Log.d("Alex", "clicked on radio group" + checkedId.toString())
            val selectedId = binding.categoryRadioGroup.getCheckedRadioButtonId()
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            addSubCategories(radioButton.getText().toString(), "")
        })

        if (SpenderViewModel.getCount() == 1) {
            binding.inputPaidByLabel.visibility = View.GONE
            binding.paidByRadioGroup.visibility = View.GONE
            binding.transactionExpandButton.visibility = View.GONE
        }
        if (SpenderViewModel.getCount() == 1 || newTransactionMode) {
            setExpansionFields(View.GONE)
        }
        if (SpenderViewModel.getCount() > 1) {
            binding.transactionBoughtForName1Label.text = SpenderViewModel.getSpenderName(0)
            binding.transactionBoughtForName2Label.text = SpenderViewModel.getSpenderName(1)
        }

        if (newTransactionMode) {
            (activity as AppCompatActivity).supportActionBar?.title = "Add Transaction"
            binding.inputPaidByLabel.text = "Who:"
            binding.recurringTransactionLabel.visibility = View.GONE
            if (CustomNotificationListenerService.getExpenseNotificationCount() > 0) {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.VISIBLE
            } else {
                binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            }
            binding.buttonPrevTransaction.visibility = View.GONE
            binding.buttonNextTransaction.visibility = View.GONE
//            binding.editTextDate.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
            binding.inputSubcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
            binding.inputSubcategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
//            binding.transactionBoughtForName1Split.setBackgroundColor(Color.parseColor(hexColor))
//            binding.transactionBoughtForName2Split.setBackgroundColor(Color.parseColor(hexColor))
/*            binding.inputSpinnerRelativeLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.paidByRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            binding.boughtForRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
            */
            if (SpenderViewModel.getCount() > 1) {
                val selectedId = binding.boughtForRadioGroup.getCheckedRadioButtonId()
                val radioButton = requireActivity().findViewById(selectedId) as RadioButton
                Log.d("Alex", "radio.button.text is '" + radioButton.text + "'")
                if (radioButton.text == "Joint") {
                    binding.transactionBoughtForName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                    binding.transactionBoughtForName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
                    binding.slider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
                } else if (radioButton.text == SpenderViewModel.getSpenderName(0)) {
                    binding.transactionBoughtForName1Split.setText("100")
                    binding.transactionBoughtForName2Split.setText("0")
                    binding.slider.value = 100.0F
                } else {
                    binding.transactionBoughtForName1Split.setText("0")
                    binding.transactionBoughtForName2Split.setText("100")
                    binding.slider.value = 0.0F
                }
            } else {
                binding.transactionBoughtForName1Split.setText("100")
                binding.transactionBoughtForName2Split.setText("0")
                binding.slider.value = 100.0F
            }
        }
        else {
            (activity as AppCompatActivity).supportActionBar?.title = "View Expenditure"
            binding.buttonSaveTransaction.visibility = View.GONE
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
            binding.dashboardLayout.visibility = View.GONE
            binding.editTextDate.isEnabled = false
            binding.editTextAmount.isEnabled = false
            binding.editTextNote.isEnabled = false
            binding.recurringTransactionLabel.isEnabled = false
            binding.inputSubcategorySpinner.isEnabled = false
            for (i in 0 until binding.categoryRadioGroup.getChildCount()) {
                (binding.categoryRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.paidByRadioGroup.getChildCount()) {
                (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
            for (i in 0 until binding.boughtForRadioGroup.getChildCount()) {
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

        updateDashboardSummary()

        ExpenditureViewModel.singleInstance.setCallback(object: ExpenditureDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "got a callback that expenditure data was updated")
                updateDashboardSummary()
            }
        })
        binding.editTextAmount.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
    }

    fun onExpandClicked() {

        if (binding.inputBoughtForLabel.visibility == View.GONE) { // ie expand the section
            inExpandMode = true
            setExpansionFields(View.VISIBLE)
            binding.boughtForRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedId ->
                val radioButton = requireActivity().findViewById(checkedId) as RadioButton
                Log.d("Alex", "clicked on radio group" + radioButton.getText().toString())
                if (radioButton.getText().toString() == "Joint") {
                    binding.transactionBoughtForName1Split.setText(SpenderViewModel.getSpenderSplit(0).toString())
                    binding.transactionBoughtForName2Split.setText(SpenderViewModel.getSpenderSplit(1).toString())
                    binding.slider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
                    binding.slider.isEnabled = true
                } else if (radioButton.getText().toString() == SpenderViewModel.getSpenderName(0)) {
                    binding.transactionBoughtForName1Split.setText("100")
                    binding.transactionBoughtForName2Split.setText("0")
                    binding.slider.value = 100.0F
                } else {
                    binding.transactionBoughtForName1Split.setText("0")
                    binding.transactionBoughtForName2Split.setText("100")
                    binding.slider.value = 0.0F
                }
                if (radioButton.text == "Joint") {
//                    binding.transactionBoughtForName1Split.isEnabled = true
//                    binding.transactionBoughtForName2Split.isEnabled = true
                    binding.slider.isEnabled = true
                } else {
 //                   binding.transactionBoughtForName1Split.isEnabled = false
//                    binding.transactionBoughtForName2Split.isEnabled = false
                    binding.slider.isEnabled = false
                }
            })

        } else { // ie retract the section
            inExpandMode = false
            setExpansionFields(View.GONE)
        }
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

        val color = getBudgetColour(requireContext(), totalDiscActualsToDate, totalDiscBudgetToDate)
        tvTotalDeltaToDate.setBackgroundColor(color)
        tvDailyDeltaToDate.setBackgroundColor(color)
        tvTotalBudgetRem.setBackgroundColor(color)
        tvDailyBudgetRem.setBackgroundColor(color)
    }
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (newTransactionMode) {
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).getItemId() == R.id.ViewTransactionsFragment)
                    menu.getItem(i).setVisible(true)
                else
                    menu.getItem(i).setVisible(false)
            }
        }
        else { // in view mode
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).getItemId() == R.id.Edit || menu.getItem(i).getItemId() == R.id.Delete)
                    menu.getItem(i).setVisible(true)
                else
                    menu.getItem(i).setVisible(false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.Edit) {
            editTransaction(args.transactionID.toString())
            return true
        } else if (item.itemId == R.id.Delete) {
            deleteTransaction(args.transactionID.toString())
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun editTransaction(iTransactionID: String) {
        Log.d("Alex", "editing " + iTransactionID)
        var currentCategory = ""
        var currentSubCategory = ""
        Log.d("Alex", "clicked edit")
        (activity as AppCompatActivity).supportActionBar?.title = "Edit Transaction"
        binding.buttonSaveTransaction.visibility = View.VISIBLE
        binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        binding.buttonPrevTransaction.visibility = View.GONE
        binding.buttonNextTransaction.visibility = View.GONE
        binding.editTextDate.isEnabled = true
        binding.editTextAmount.isEnabled = true
        binding.editTextNote.isEnabled = true
        binding.inputSubcategorySpinner.isEnabled = true
        for (i in 0 until binding.categoryRadioGroup.getChildCount()) {
            var button = binding.categoryRadioGroup.getChildAt(i) as RadioButton
            button.isEnabled = true
            if (button.isChecked)
                currentCategory = button.text.toString()
        }
        for (i in 0 until binding.paidByRadioGroup.getChildCount()) {
            (binding.paidByRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.boughtForRadioGroup.getChildCount()) {
            (binding.boughtForRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        val selectedId = binding.boughtForRadioGroup.getCheckedRadioButtonId()
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        if (radioButton.text == "Joint") {
//            binding.transactionBoughtForName1Split.isEnabled = true
//            binding.transactionBoughtForName2Split.isEnabled = true
            binding.slider.isEnabled = true
        }

        currentSubCategory = binding.inputSubcategorySpinner.selectedItem.toString()
        addSubCategories(currentCategory, currentSubCategory)
    }

    private fun deleteTransaction(iTransactionID: String) {
        fun yesClicked() {
            (activity as MainActivity).getMyExpenditureModel().deleteTransaction(iTransactionID)
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

    private fun viewTransaction(iTransactionID: String) {
        val thisTransaction = (activity as MainActivity).getMyExpenditureModel().getExpenditure(iTransactionID)
        if (thisTransaction != null) {  //
            editingKey = iTransactionID

            val iAmount = thisTransaction.amount
            val dec = DecimalFormat("#.00")
            val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
            binding.editTextAmount.setText(dec.format(formattedAmount))
            binding.transactionId.text = iTransactionID
            if (MyApplication.adminMode) {
                binding.transactionIdLayout.visibility = View.VISIBLE
                binding.transactionId.visibility = View.VISIBLE
            }

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
            binding.recurringTransactionLabel.visibility = View.VISIBLE
            if (thisTransaction.type == "R") {
                binding.recurringTransactionLabel.setText("This recurring transaction was automatically generated.")
                binding.recurringTransactionLabel.visibility = View.VISIBLE
            } else {
                binding.recurringTransactionLabel.visibility = View.INVISIBLE
            }

            val pbRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.paidByRadioGroup)
            Log.d("Alex", "now in view paidby")
            for (i in 0 until pbRadioGroup.childCount) {
                val o = pbRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == thisTransaction.paidby) {
                        o.isChecked = true
                    }
                }
            }
            val bfRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.boughtForRadioGroup)
            Log.d("Alex", "now in view boughtfor")
            for (i in 0 until bfRadioGroup.childCount) {
                val o = bfRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == thisTransaction.boughtfor) {
                        o.isChecked = true
                    }
                }
            }
//            var tSplit = thisTransaction.bfname1split
//            Log.d("Alex", "found split 1 " + tSplit)
//            var dec2 = DecimalFormat("#.##")
//            var formattedAmount2 = (tSplit/100).toDouble() + (tSplit % 100).toDouble()/100
            binding.transactionBoughtForName1Split.setText(thisTransaction.bfname1split.toString())
            binding.slider.value = thisTransaction.bfname1split.toFloat()

//            tSplit = thisTransaction.bfname2split
//            Log.d("Alex", "found split 2 " + tSplit)
//            formattedAmount2 = (tSplit/100).toDouble() + (tSplit % 100).toDouble()/100
            binding.transactionBoughtForName2Split.setText(thisTransaction.bfname2split.toString())

            if (thisTransaction.paidby == thisTransaction.boughtfor) {
                setExpansionFields(View.GONE)
            } else {
                setExpansionFields(View.VISIBLE)
            }
        }
        else { // this doesn't make sense...
            Log.d("Alex", "iTransactionID "+ iTransactionID + " was passed for edit but can't find the data")
        }
        if (inExpandMode)
            setExpansionFields(View.VISIBLE)
        else
            setExpansionFields(View.GONE)
    }

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
/*        binding.transactionBoughtForName1Preamble.visibility = iView
        binding.transactionBoughtForName1Label.visibility = iView
        binding.transactionBoughtForName1Split.visibility = iView
        binding.transactionBoughtForName1Suffix.visibility = iView
        binding.transactionBoughtForName2Preamble.visibility = iView
        binding.transactionBoughtForName2Label.visibility = iView
        binding.transactionBoughtForName2Split.visibility = iView
        binding.transactionBoughtForName2Suffix.visibility = iView
        binding.sliderLayout.visibility = iView */
    }

    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        var subCategorySpinner =
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

    private fun onLoadTransactionButtonClicked() {
        val notification = CustomNotificationListenerService.getTransactionFromNotificationAndDeleteIt()
        if (notification == null) {
            Toast.makeText(activity,
                "Having issue with this TD notification.  Logged in DB.  Please inform Alex.  You'll have to handle this TD notification manually.",
                Toast.LENGTH_SHORT).show()
            return
        }
        binding.editTextAmount.setText(notification.amount.toString())
        var iNote = notification.note.lowercase()
        val words = iNote.split(" ")
        var newStr = ""
        words.forEach {
            newStr += it.replaceFirstChar { it.uppercase() } + " "
        }
        binding.editTextNote.setText(newStr)

        if (CustomNotificationListenerService.getExpenseNotificationCount() == 0) {
            binding.buttonLoadTransactionFromTdmyspend.visibility = View.GONE
        } else {
            // there are more notifications, but we don't want the user to click the button again until the current data is saved
            binding.buttonLoadTransactionFromTdmyspend.isEnabled = false
        }
    }

    private fun onSaveTransactionButtonClicked () {
        if (!textIsSafe(binding.editTextNote.text.toString())) {
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

        var tempDouble : Double
        var amountInt: Int
        tempDouble = round(binding.editTextAmount.text.toString().toDouble()*100)
        amountInt = tempDouble.toInt()
        SpenderViewModel.showMe()
        Log.d("Alex", "Splits are ${binding.transactionBoughtForName1Split.text} and ${binding.transactionBoughtForName2Split.text}")
        val bfName1Split = binding.transactionBoughtForName1Split.text.toString().toInt()
        val bfName2Split = binding.transactionBoughtForName2Split.text.toString().toInt()

        if (newTransactionMode) {
            val expenditure = ExpenditureOut(
                binding.editTextDate.text.toString(),
                amountInt, radioButton.text.toString(), subcategorySpinner.selectedItem.toString(),
                binding.editTextNote.text.toString(), radioButtonPaidBy.text.toString(),
                radioButtonBoughtFor.text.toString(), bfName1Split, bfName2Split
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
                binding.editTextDate.text.toString(),
                amountInt, radioButton.text.toString(), subcategorySpinner.selectedItem.toString(),
                binding.editTextNote.text.toString(), radioButtonPaidBy.text.toString(),
                radioButtonBoughtFor.text.toString(), bfName1Split, bfName2Split
            )

            (activity as MainActivity).getMyExpenditureModel().updateTransaction(editingKey, expenditure)
            hideKeyboard(requireContext(), requireView())
            Toast.makeText(activity, "Transaction updated", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
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
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.setText(it.toString())
            newRadioButton.id = ctr++
            radioGroup.addView(newRadioButton)
            if (newTransactionMode && newRadioButton.text.toString() == DefaultsViewModel.getDefault(
                    cDEFAULT_CATEGORY)) {
                Log.d("Alex", "found default " + newRadioButton.text.toString())
                radioGroup.check(newRadioButton.id)
            }
        }
        addSubCategories(DefaultsViewModel.getDefault(cDEFAULT_CATEGORY), DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ExpenditureViewModel.singleInstance.setCallback(null)
        _binding = null
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
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            paidByRadioGroup.addView(newRadioButton)
            if (newTransactionMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
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
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            boughtForRadioGroup.addView(newRadioButton)
            if (newTransactionMode && spender?.name == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default boughtfor " + spender)
                boughtForRadioGroup.check(newRadioButton.id)
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
                    DialogInterface.OnClickListener { _, _ ->
                    })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
