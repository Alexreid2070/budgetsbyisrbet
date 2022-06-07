package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.lang.UCharacter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import it.sephiroth.android.library.numberpicker.doOnProgressChanged
import java.util.*

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val args: BudgetFragmentArgs by navArgs()
    private var inAnnualMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)

        val dm = activity?.resources?.displayMetrics
        Log.d("Alex", "width is ${dm?.widthPixels}")
        if (dm?.widthPixels!! <= 600) {
            binding.budgetAddYearLayout.orientation = LinearLayout.VERTICAL
        }
        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = getLocalCurrencySymbol() + " "

        Log.d("Alex", "onviewcreated category id is " + args.categoryID)
        loadCategoryRadioButtons()
        loadSpenderRadioButtons()
        loadOccurenceRadioButtons()

        setupForEdit()
        val cal = android.icu.util.Calendar.getInstance()
        binding.budgetAddYear.progress = cal.get(Calendar.YEAR)
        binding.budgetAddMonth.progress = cal.get(Calendar.MONTH)+1

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

        binding.budgetAddYear.doOnProgressChanged { _, _, _ -> updateInformationFields() }

        binding.budgetAddMonth.doOnProgressChanged { _, _, _ -> updateInformationFields() }

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
        var tempDouble = 0.0
        if (binding.budgetAddPreviousAmount.text.toString() != "")
            tempDouble = getDoubleValue(binding.budgetAddPreviousAmount.text.toString())
        if (binding.budgetAddPercentage.text.toString() != "")
            tempDouble *= (1 + getDoubleValue(binding.budgetAddPercentage.text.toString()) / 100)
        binding.budgetAddAmount.setText(gDec.format(tempDouble))
    }

    @SuppressLint("SetTextI18n")
    private fun setupForEdit() {
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.budgetAddSubCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.budgetAddSubCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)

        binding.regularityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            when {
                radioButton.text.toString() == "Annual" -> {
                    binding.budgetAddMonth.visibility = GONE
                    binding.budgetAddMonthLabel.visibility = GONE
                    inAnnualMode = true
                    updateInformationFields()
                }
                radioButton.text.toString() == "Monthly" -> {
                    binding.budgetAddMonth.visibility = VISIBLE
                    binding.budgetAddMonthLabel.visibility = VISIBLE
                    inAnnualMode = false
                    updateInformationFields()
                }
            }
        }

        binding.budgetAddButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }
        binding.budgetAddButtonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.budgetAddSubCategorySpinner.isEnabled = true
//        addSubCategories(args.category)
        binding.budgetAddYear.isEnabled = true
        binding.budgetAddMonth.isEnabled = true
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
        binding.budgetAddAmountLabel.text = "Enter new budget amount: "
    }

    @SuppressLint("SetTextI18n")
    fun updateInformationFields() {
        Log.d("Alex", "In updateInformationFields")
        val prevMonth = BudgetMonth(
            binding.budgetAddYear.progress,
            if (inAnnualMode) 1 else binding.budgetAddMonth.progress
        )
//        prevMonth.decrementMonth()
        val whoSelectedId = binding.budgetAddWhoRadioGroup.checkedRadioButtonId
        val whoRadioButton = requireActivity().findViewById(whoSelectedId) as RadioButton
        val whoId = SpenderViewModel.getSpenderIndex(whoRadioButton.text.toString())
        val catSelectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val catText = catRadioButton.text.toString()
        val subCatText = binding.budgetAddSubCategorySpinner.selectedItem.toString()

        val toCheckAnnual = BudgetMonth(binding.budgetAddYear.progress, 0)
        val annualBudget = BudgetViewModel.budgetExistsForExactPeriod(CategoryViewModel.getID(catText, subCatText),toCheckAnnual, whoId)
        if (annualBudget != 0.0) {
            binding.budgetAddPreviousAmount.text = "Current budget amount is ${gDecWithCurrency(annualBudget)} which was set for ${toCheckAnnual.year} (A)."
        } else {
            val tmpPrevAmt = BudgetViewModel.getOriginalBudgetAmount(CategoryViewModel.getID(catText, subCatText), prevMonth, whoId)
            binding.budgetAddPreviousAmount.text = gDecWithCurrency(tmpPrevAmt.amount)
            if (tmpPrevAmt.dateStarted.year == 9999) { // never explicitly set
                binding.budgetAddPreviousAmount.text = "There is no budget amount that you have explicitly set."
            } else {
                binding.budgetAddPreviousAmount.text = "Current budget amount is ${gDecWithCurrency(tmpPrevAmt.amount)} which was set "
                if (tmpPrevAmt.dateStarted.isAnnualBudget()) // is an annual amount
                    binding.budgetAddPreviousAmount.text = binding.budgetAddPreviousAmount.text.toString() +
                        tmpPrevAmt.dateStarted.year.toString() + " (A)."
                else
                    binding.budgetAddPreviousAmount.text = binding.budgetAddPreviousAmount.text.toString() + tmpPrevAmt.dateStarted.toString() + "."
            }
        }
        val startOfPeriod = BudgetMonth(binding.budgetAddYear.progress, binding.budgetAddMonth.progress)
        val endOfPeriod = BudgetMonth(binding.budgetAddYear.progress, binding.budgetAddMonth.progress)
        if (inAnnualMode) {
            startOfPeriod.year -= 1
            startOfPeriod.month = 1
            endOfPeriod.year -= 1
            endOfPeriod.month = 12
        } else {
            startOfPeriod.decrementMonth(1)
            endOfPeriod.decrementMonth(1)
        }
        binding.budgetAddActualAmount.text = "Actual amount spent in previous period is ${gDecWithCurrency(TransactionViewModel.getActualsForPeriod(CategoryViewModel.getID(catText, subCatText),
            startOfPeriod, endOfPeriod, whoId, false))}."

        val annualActuals = TransactionViewModel.getActualsForPeriod(CategoryViewModel.getID(catText, subCatText),
            BudgetMonth(
                binding.budgetAddYear.progress - 1,
                if (inAnnualMode) 1 else binding.budgetAddMonth.progress
            ),
            prevMonth,
            whoId, false)
        binding.budgetAddTotalAmount.text = gDecWithCurrency(annualActuals)
        binding.budgetAddTotalAmount.text = "Total spent in previous 12 months is ${gDecWithCurrency(annualActuals)}."
        binding.budgetAddAverageAmount.text = "Average spent in previous 12 months is ${gDecWithCurrency(annualActuals/12)}."

        if (binding.budgetAddPercentage.text.toString() != "")
            setAmountBasedOnPercentage()
    }

    private fun loadCategoryRadioButtons() {
        var ctr = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAddCategoryRadioGroup)
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
        Log.d("Alex", "subcategory is " + CategoryViewModel.getCategory(args.categoryID.toInt())?.subcategoryName + " and position is " + arrayAdapter.getPosition(CategoryViewModel.getCategory(args.categoryID.toInt())?.subcategoryName))
        if (args.categoryID != "")
            subCategorySpinner.setSelection(arrayAdapter.getPosition(CategoryViewModel.getCategory(args.categoryID.toInt())?.subcategoryName))
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        val whoRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAdd_whoRadioGroup)
        if (whoRadioGroup == null) Log.d("Alex", " rg 'paidby' is null")
        else whoRadioGroup.removeAllViews()

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
        if (occurenceRadioGroup == null) Log.d("Alex", " rg 'occurence' is null")
        else occurenceRadioGroup.removeAllViews()

        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = cBUDGET_JUST_THIS_MONTH
        newRadioButton.id = ctr++
        newRadioButton.buttonTintList=
            ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        occurenceRadioGroup.addView(newRadioButton)

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = cBUDGET_RECURRING
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
        // need to reject if all the fields aren't entered correctly
        if (binding.budgetAddAmount.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            binding.budgetAddAmount.error = getString(R.string.missingAmountError)
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddAmount)
            return
        }
        if (getDoubleValue(binding.budgetAddAmount.text.toString()) == 0.0) {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            binding.budgetAddAmount.error = getString(R.string.missingAmountError)
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddAmount)
            return
        }
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

        val catSelectedId = binding.budgetAddCategoryRadioGroup.checkedRadioButtonId
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val tempCategory = CategoryViewModel.getID(catRadioButton.text.toString(), binding.budgetAddSubCategorySpinner.selectedItem.toString())
        val tempBudget = BudgetViewModel.getBudget(tempCategory)
        if (tempBudget != null) {
            var chosenMonth = 0
            if (!inAnnualMode)
                chosenMonth = binding.budgetAddMonth.progress

            Log.d("Alex", "year is '" + binding.budgetAddYear.progress + "'")
            if (tempBudget.overlapsWithExistingBudget(
                    BudgetMonth(
                        binding.budgetAddYear.progress,
                        chosenMonth
                    ).toString(),
                    whoId
                )
            ) {
                showErrorMessage(parentFragmentManager, getString(R.string.budgetOverlap))
                focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddMonth)
                return
            }
        }
        val catRadioGroup = binding.budgetAddCategoryRadioGroup
        val radioButtonID = catRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
        Log.d("Alex", "Category is " + radioButton.text)

        val tempDouble : Double = getDoubleValue(binding.budgetAddAmount.text.toString())
        var period = binding.budgetAddYear.progress.toString()
        period = if (!inAnnualMode) {
            if (binding.budgetAddMonth.progress < 10)
                period + "-0" + binding.budgetAddMonth.progress
            else
                period + binding.budgetAddMonth.progress
        } else {
            "$period-00"
        }

        BudgetViewModel.updateBudget(tempCategory, period, whoId, tempDouble, occurenceText)
//        binding.budgetAddAmount.setText("")
        binding.budgetAddAmount.requestFocus()
//        binding.budgetAddPercentage.setText("")
        hideKeyboard(requireContext(), requireView())
        Toast.makeText(activity, "Budget item added", Toast.LENGTH_SHORT).show()
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        activity?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}