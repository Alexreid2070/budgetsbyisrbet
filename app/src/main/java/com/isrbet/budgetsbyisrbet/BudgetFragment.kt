package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import timber.log.Timber
import java.util.*

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val args: BudgetFragmentArgs by navArgs()
    private var prevBudgetAmt = 0.0
    private var cal = android.icu.util.Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)

        binding.budgetAddAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = "${getLocalCurrencySymbol()} "
        cal.set(Calendar.DAY_OF_MONTH, 1)
        binding.startDate.setText(giveMeMyDateFormat(cal))
        binding.regularity.setText("1")
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.startDate.setText(giveMeMyDateFormat(cal))
                updateInformationFields()
            }

        binding.startDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val pSpinner:Spinner = binding.periodSpinner
        val periodValues = listOf(
            getString(R.string.week),
            getString(R.string.month),
            getString(R.string.year))
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodValues
        )
        pSpinner.adapter = arrayAdapter
        pSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.month)))
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.periodSpinner.setBackgroundColor(Color.parseColor(hexColor))

        loadCategoryRadioButtons()
        loadSpenderRadioButtons()
        loadOccurenceRadioButtons()

        setupForEdit()

        binding.budgetAddCategoryRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            addSubCategories(radioButton.text.toString())
        }
        if (SpenderViewModel.singleUser()) {
            binding.budgetAddWhoLabel.visibility = GONE
            binding.budgetAddWhoRadioGroup.visibility = GONE
        }

        binding.budgetAddSubCategorySpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInformationFields()
                setOrPercentageLayout()
            }
        }

        binding.budgetAddWhoRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateInformationFields()
        }

        binding.budgetAddPercentage.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                setAmountBasedOnPercentage()
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })
        setOrPercentageLayout()
    }

    private fun setOrPercentageLayout() {
        val catSelectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val catText = catRadioButton.text.toString()
        val subCatText = binding.budgetAddSubCategorySpinner.selectedItem.toString()

        if (BudgetViewModel.budgetExistsUsingCategory(CategoryViewModel.getID(catText, subCatText)) == 0)
            binding.budgetAddPercentageLayout.visibility = GONE
        else
            binding.budgetAddPercentageLayout.visibility = VISIBLE
    }

    fun setAmountBasedOnPercentage() {
        if (binding.budgetAddPercentage.text.toString() != "") {
            val pctDouble = gNumberFormat.parse(binding.budgetAddPercentage.text.toString()).toDouble()
            prevBudgetAmt *= (1 + pctDouble / 100)
        }
        binding.budgetAddAmount.setText(gDec(prevBudgetAmt))
    }

    private fun setupForEdit() {
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.budgetAddSubCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.budgetAddSubCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)

        binding.periodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.periodSpinner.setPopupBackgroundResource(R.drawable.spinner)

        binding.periodSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInformationFields()
            }
        }
        binding.regularity.setOnClickListener {
            updateInformationFields()
        }

        binding.budgetAddButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }
        binding.budgetAddButtonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.budgetAddSubCategorySpinner.isEnabled = true
//        addSubCategories(args.category)
        binding.startDate.isEnabled = true
        for (i in 0 until binding.budgetAddWhoRadioGroup.childCount) {
            (binding.budgetAddWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.budgetAddOccurenceRadioGroup.childCount) {
            (binding.budgetAddOccurenceRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        binding.budgetAddAmount.isEnabled = true
        binding.budgetAddOrLabel.visibility = VISIBLE
        binding.budgetAddPercentageLayout.visibility = VISIBLE
        binding.budgetAddPreviousAmount.visibility = VISIBLE
        binding.budgetAddActualAmount.visibility = VISIBLE
        binding.budgetAddAverageAmount.visibility = VISIBLE
        binding.budgetAddAmountLabel.text = getString(R.string.enter_new_budget_amount)
    }

    fun updateInformationFields() {
        val prevMonth = MyDate(binding.startDate.text.toString())
//        prevMonth.decrementMonth()
        val whoSelectedId = binding.budgetAddWhoRadioGroup.checkedRadioButtonId
        val whoRadioButton = requireActivity().findViewById(whoSelectedId) as RadioButton
        val whoId = SpenderViewModel.getSpenderIndex(whoRadioButton.text.toString())
        val catSelectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val catText = catRadioButton.text.toString()
        val subCatText = binding.budgetAddSubCategorySpinner.selectedItem.toString()

        val toCheckAnnual = MyDate(binding.startDate.text.toString())
        val annualBudget = BudgetViewModel.budgetExistsForExactPeriod(
            CategoryViewModel.getID(catText, subCatText),
            toCheckAnnual,
            whoId
        )
        if (annualBudget != 0.0) {
            binding.budgetAddPreviousAmount.text = String.format(
                "{getString(R.string.current_budget_amount_is)} ${
                    gDecWithCurrency(annualBudget)
                } ${getString(R.string.which_was_set)} ${toCheckAnnual.year} ${getString(R.string.pAp)}."
            )
            prevBudgetAmt = annualBudget
        } else {
            val tmpPrevAmt = BudgetViewModel.getOriginalBudgetAmount(
                CategoryViewModel.getID(
                    catText,
                    subCatText
                ), prevMonth, whoId
            )
            binding.budgetAddPreviousAmount.text = gDecWithCurrency(tmpPrevAmt.amount)
            prevBudgetAmt = tmpPrevAmt.amount
            if (tmpPrevAmt.startDate.year == 9999) { // never explicitly set
                binding.budgetAddPreviousAmount.text = getString(R.string.there_is_no_amount)
            } else {
                binding.budgetAddPreviousAmount.text = String.format(
                    "${getString(R.string.current_budget_amount_is)} ${
                        gDecWithCurrency(tmpPrevAmt.amount)
                    } ${getString(R.string.which_was_set)}"
                )
                prevBudgetAmt = tmpPrevAmt.amount
                if (tmpPrevAmt.isAnnualBudget()) // is an annual amount
                    binding.budgetAddPreviousAmount.text = String.format(
                        "${binding.budgetAddPreviousAmount.text} ${tmpPrevAmt.startDate.year} ${
                            getString(R.string.pAp)
                        }."
                    )
                else
                    binding.budgetAddPreviousAmount.text =
                        String.format("${binding.budgetAddPreviousAmount.text} ${tmpPrevAmt.startDate}.")
            }
        }
        val startOfPeriod = MyDate(binding.startDate.text.toString())
        val endOfPeriod = MyDate(binding.startDate.text.toString())
        Timber.tag("Alex").d("regularity is %s", binding.regularity.text)
        when (binding.periodSpinner.selectedItem.toString()) {
            getString(R.string.week) -> {
                endOfPeriod.increment(cPeriodWeek, -1 * binding.regularity.text.toString().toInt())
            }
            getString(R.string.month) -> {
                endOfPeriod.increment(cPeriodMonth, -1 * binding.regularity.text.toString().toInt())
            }
            getString(R.string.year) -> {
                startOfPeriod.year -= 1
                startOfPeriod.month = 1
                endOfPeriod.year -= 1
                endOfPeriod.month = 12
            }
        }
        binding.budgetAddActualAmount.text = String.format("{getString(R.string.actual_amount_spent_in_previous_period_is)}  ${
                        gDecWithCurrency(
                            TransactionViewModel.getActualsForPeriod(
                                CategoryViewModel.getID(catText, subCatText),
                                startOfPeriod, endOfPeriod, whoId, false
                            )
                        )
                    }.")

        val annualActuals = TransactionViewModel.getActualsForPeriod(
            CategoryViewModel.getID(catText, subCatText),
            startOfPeriod,
            endOfPeriod,
            whoId, false
        )
        binding.budgetAddTotalAmount.text = gDecWithCurrency(annualActuals)
        binding.budgetAddTotalAmount.text = String.format("${getString(R.string.total_spent_in_previous_12_months_is)} ${
                gDecWithCurrency(annualActuals)}.")
        binding.budgetAddAverageAmount.text = String.format("${getString(R.string.average_spent_in_previous_12_months_is)} ${
                gDecWithCurrency(annualActuals / 12)}.")

        if (binding.budgetAddPercentage.text.toString() != "")
            setAmountBasedOnPercentage()
    }

    private fun loadCategoryRadioButtons() {
        var ctr = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAddCategoryRadioGroup)
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
        }
        var somethingChecked = false
        for (i in 0 until radioGroup.childCount) {
            val o = radioGroup.getChildAt(i)
            if (o is RadioButton && o.text == CategoryViewModel.getCategory(args.categoryID.toInt())?.categoryName) {
                o.isChecked = true
                somethingChecked = true
                addSubCategories(o.text.toString())
            }
        }
        if (!somethingChecked) {
            val o = radioGroup.getChildAt(0)
            if (o is RadioButton) {
                o.isChecked = true
                addSubCategories(o.text.toString())
            }
        }
    }

    private fun addSubCategories(iCategory: String) {
        val subCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.budgetAddSubCategorySpinner)
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
        if (args.categoryID != "")
            subCategorySpinner.setSelection(arrayAdapter.getPosition(CategoryViewModel.getCategory(args.categoryID.toInt())?.subcategoryName))
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        val whoRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAdd_whoRadioGroup)
        whoRadioGroup?.removeAllViews()

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
            whoRadioGroup.addView(newRadioButton)
            if (i == SpenderViewModel.getActiveCount()-1)  // ie check the last one
                newRadioButton.isChecked = true
        }
    }

    private fun loadOccurenceRadioButtons() {
        var ctr = 300
        val occurenceRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAdd_occurenceRadioGroup)
        occurenceRadioGroup?.removeAllViews()

        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.once)
        newRadioButton.id = ctr++
        newRadioButton.buttonTintList=
            ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        occurenceRadioGroup.addView(newRadioButton)

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.recurring)
        newRadioButton.id = ctr
        newRadioButton.buttonTintList=
            ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        occurenceRadioGroup.addView(newRadioButton)

        val o = occurenceRadioGroup.getChildAt(1) // ie check Recurring
        if (o is RadioButton) {
            o.isChecked = true
        }
    }

    private fun onSaveButtonClicked () {
        val amountDouble = gNumberFormat.parse(binding.budgetAddAmount.text.toString()).toDouble()
        val selectedId = binding.budgetAddWhoRadioGroup.checkedRadioButtonId
        val whoId = if (SpenderViewModel.singleUser())
            0
        else {
            val whoRadioButton = requireActivity().findViewById(selectedId) as RadioButton
            SpenderViewModel.getSpenderIndex(whoRadioButton.text.toString())
        }

        val occSelectedId = binding.budgetAddOccurenceRadioGroup.checkedRadioButtonId
        val occRadioButton = requireActivity().findViewById(occSelectedId) as RadioButton
        val occurenceText = occRadioButton.text.toString()
        val newOccurenceID = if (occurenceText == getString(R.string.once))
            cBUDGET_JUST_THIS_MONTH
        else
            cBUDGET_RECURRING

        val catSelectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val categoryID = CategoryViewModel.getID(catRadioButton.text.toString(), binding.budgetAddSubCategorySpinner.selectedItem.toString())

        val errorMsg = BudgetViewModel.checkNewBudget("",
            categoryID,
            MyDate(binding.startDate.text.toString()),
            whoId,
            binding.budgetAddAmount.text.toString(),
            binding.periodSpinner.selectedItem.toString(),
            binding.regularity.text.toString())
        if (errorMsg != "") {
            showErrorMessage(parentFragmentManager, errorMsg)
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddAmount)
            return
        }

        BudgetViewModel.addBudget(categoryID, MyDate(binding.startDate.text.toString()),
            whoId, amountDouble, binding.periodSpinner.selectedItem.toString(),
            binding.regularity.text.toString().toInt(), newOccurenceID)
        binding.budgetAddAmount.requestFocus()
        hideKeyboard(requireContext(), requireView())
        Toast.makeText(activity, getString(R.string.budget_item_added), Toast.LENGTH_SHORT).show()
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        requireActivity().onBackPressedDispatcher.onBackPressed()
//        activity?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}