package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import it.sephiroth.android.library.numberpicker.doOnProgressChanged
import java.text.DecimalFormat
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
        setHasOptionsMenu(true)
        BudgetViewModel.showMe()

        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("Alex", "onviewcreated category id is " + args.categoryID)
        loadCategoryRadioButtons()
        loadSpenderRadioButtons()
        loadOccurenceRadioButtons()

        setupForEdit()
        val cal = android.icu.util.Calendar.getInstance()
/*        binding.budgetAddYear.minValue = 2018
        binding.budgetAddYear.maxValue = 2040
        binding.budgetAddYear.wrapSelectorWheel = true
        binding.budgetAddYear.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS */
        binding.budgetAddYear.progress = cal.get(Calendar.YEAR)
/*        binding.budgetAddMonth.minValue = 1
        binding.budgetAddMonth.maxValue = 12
        binding.budgetAddMonth.wrapSelectorWheel = true
        binding.budgetAddMonth.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS */
        binding.budgetAddMonth.progress = cal.get(Calendar.MONTH)+1

        (activity as AppCompatActivity).supportActionBar?.title = "Add Budget"
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
        //        updateInformationFields()
    }

    fun setAmountBasedOnPercentage() {
        var tempDouble = 0.0
        if (binding.budgetAddPreviousAmount.text.toString() != "")
            tempDouble = binding.budgetAddPreviousAmount.text.toString().toDouble()
        if (binding.budgetAddPercentage.text.toString() != "")
            tempDouble *= (1 + binding.budgetAddPercentage.text.toString().toDouble() / 100)
        val dec = DecimalFormat("#.00")
        binding.budgetAddAmount.setText(dec.format(tempDouble))
    }

    @SuppressLint("SetTextI18n")
    private fun setupForEdit() {
        (activity as AppCompatActivity).supportActionBar?.title = "Edit Budget"
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

        binding.budgetAddButtonSave.visibility = VISIBLE
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
        binding.budgetAddPreviousAmountLayout.visibility = VISIBLE
        binding.budgetAddActualAmountLayout.visibility = VISIBLE
        binding.budgetAddAverageAmountLayout.visibility = VISIBLE
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
        val dec = DecimalFormat("#.00")

        val toCheckAnnual = BudgetMonth(binding.budgetAddYear.progress, 0)
        val annualBudget = BudgetViewModel.budgetExistsForExactPeriod(CategoryViewModel.getID(catText, subCatText),toCheckAnnual, whoId)
        if (annualBudget != 0.0) {
            binding.budgetAddPreviousAmount.text = dec.format(annualBudget)
            binding.budgetAddPreviousAmountLabel2.text = " which was set for "
            binding.budgetAddPreviousAmountDate.text = toCheckAnnual.year.toString() + " (A)"
        } else {
            val tmpPrevAmt = BudgetViewModel.getOriginalBudgetAmount(CategoryViewModel.getID(catText, subCatText), prevMonth, whoId)
            binding.budgetAddPreviousAmount.text = dec.format(tmpPrevAmt.amount)
            if (tmpPrevAmt.dateStarted.year == 9999) { // never explicitly set
                binding.budgetAddPreviousAmountLabel2.text = ".  (No amount explicitly set.)"
                binding.budgetAddPreviousAmountDate.text = ""
            } else {
                binding.budgetAddPreviousAmountLabel2.text = " which was set "
                if (tmpPrevAmt.dateStarted.isAnnualBudget()) // is an annual amount
                    binding.budgetAddPreviousAmountDate.text =
                        tmpPrevAmt.dateStarted.year.toString() + " (A)"
                else
                    binding.budgetAddPreviousAmountDate.text = tmpPrevAmt.dateStarted.toString()
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
        binding.budgetAddActualAmount.text = dec.format(ExpenditureViewModel.getActualsForPeriod(CategoryViewModel.getID(catText, subCatText),
            startOfPeriod, endOfPeriod, whoId))

        val annualActuals = ExpenditureViewModel.getActualsForPeriod(CategoryViewModel.getID(catText, subCatText),
            BudgetMonth(
                binding.budgetAddYear.progress - 1,
                if (inAnnualMode) 1 else binding.budgetAddMonth.progress
            ),
            prevMonth,
            whoId)
        binding.budgetAddTotalAmount.text = dec.format(annualActuals)
        binding.budgetAddAverageAmount.text = dec.format(annualActuals/12)

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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
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
        if (binding.budgetAddAmount.text.toString().toDouble() == 0.0) {
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

        val tempDouble : Double = binding.budgetAddAmount.text.toString().toDouble()
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