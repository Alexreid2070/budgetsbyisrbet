package com.isrbet.budgetsbyisrbet

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import java.text.DecimalFormat
import java.util.*
import kotlin.math.round

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    val args: BudgetFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        BudgetViewModel.showMe()

        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCategoryRadioButtons()
        loadSpenderRadioButtons()
        loadOccurenceRadioButtons()

        setupForEdit()
        var cal = android.icu.util.Calendar.getInstance()
        binding.budgetAddYear.setText(cal.get(Calendar.YEAR).toString())
        (activity as AppCompatActivity).supportActionBar?.title = "Add Budget"
        binding.budgetAddCategoryRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = requireActivity().findViewById(checkedId)
            Log.d("Alex", "clicked on radio group" + checkedId.toString())
            val selectedId = binding.budgetAddCategoryRadioGroup.getCheckedRadioButtonId()
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            addSubCategories(radioButton.getText().toString())
        })
        if (SpenderViewModel.getCount() == 1) {
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

        binding.budgetAddWhoRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (binding.budgetAddYear.text.toString() != "")
                updateInformationFields()
        })

        binding.budgetAddYear.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                if (binding.budgetAddYear.text.toString() != "")
                    updateInformationFields()
            }
        })

        binding.budgetAddMonth.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                if (binding.budgetAddYear.text.toString() != "")
                    updateInformationFields()
            }
        })

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
        var tempDouble: Double = 0.0
        if (binding.budgetAddPreviousAmount.text.toString() != "")
            tempDouble = binding.budgetAddPreviousAmount.text.toString().toDouble()
        if (binding.budgetAddPercentage.text.toString() != "")
            tempDouble = tempDouble * (1 + binding.budgetAddPercentage.text.toString().toDouble()/100)
        val dec = DecimalFormat("#.00")
        binding.budgetAddAmount.setText(dec.format(tempDouble))
    }

    fun setupForEdit() {
        (activity as AppCompatActivity).supportActionBar?.title = "Edit Budget"
        binding.budgetAddSubCategorySpinner.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.budgetAddSubCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
/*        binding.budgetAddCategoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddSubCategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddYear.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddMonth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddWhoRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddOccurenceRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddPercentage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
*/
        binding.budgetAddButtonSave.visibility = VISIBLE
        binding.budgetAddButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }

        binding.budgetAddSubCategorySpinner.isEnabled = true
//        addSubCategories(args.category)
        binding.budgetAddYear.isEnabled = true
        binding.budgetAddMonth.isEnabled = true
        for (i in 0 until binding.budgetAddWhoRadioGroup.getChildCount()) {
            (binding.budgetAddWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
        }
        for (i in 0 until binding.budgetAddOccurenceRadioGroup.getChildCount()) {
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

    fun updateInformationFields() {
        Log.d("Alex", "In updateInformationFields")
        var prevMonth = BudgetMonth(
            binding.budgetAddYear.text.toString().toInt(),
            if (binding.budgetAddMonth.text.toString() == "") 1 else binding.budgetAddMonth.text.toString().toInt()
        )
        prevMonth.decrementMonth()
        val whoSelectedId = binding.budgetAddWhoRadioGroup.getCheckedRadioButtonId()
        var whoText: String = ""
        val whoRadioButton = requireActivity().findViewById(whoSelectedId) as RadioButton
        whoText = whoRadioButton.text.toString()
        val catSelectedId = binding.budgetAddCategoryRadioGroup.getCheckedRadioButtonId()
        var catText: String = ""
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        catText = catRadioButton.text.toString()
        val subCatText = binding.budgetAddSubCategorySpinner.selectedItem.toString()

        var tmpPrevAmt = BudgetViewModel.getBudgetAmount(catText + "-" + subCatText, prevMonth, whoText, true)

        val dec = DecimalFormat("#.00")
        binding.budgetAddPreviousAmount.text = dec.format(tmpPrevAmt.amount)
        if (tmpPrevAmt.dateStarted.year == 9999) { // never explicitly set
            binding.budgetAddPreviousAmountLabel2.text = ".  (No amount explicitly set.)"
            binding.budgetAddPreviousAmountDate.text = ""
        } else {
            binding.budgetAddPreviousAmountLabel2.text = " which is set for "
            if (tmpPrevAmt.dateStarted.month == 0) // is an annual amount
                binding.budgetAddPreviousAmountDate.text = tmpPrevAmt.dateStarted.year.toString() + " (A)"
            else
                binding.budgetAddPreviousAmountDate.text = tmpPrevAmt.dateStarted.toString()
        }
        binding.budgetAddActualAmount.text = dec.format(ExpenditureViewModel.getActualsForPeriod(catText, subCatText,
            BudgetMonth(
                binding.budgetAddYear.text.toString().toInt(),
                if (binding.budgetAddMonth.text.toString() == "") 1 else binding.budgetAddMonth.text.toString().toInt()
            ),
            BudgetMonth(
                binding.budgetAddYear.text.toString().toInt(),
                if (binding.budgetAddMonth.text.toString() == "") 1 else binding.budgetAddMonth.text.toString().toInt()
            ),
        whoText))

        val annualActuals = ExpenditureViewModel.getActualsForPeriod(catText, subCatText,
            BudgetMonth(
                binding.budgetAddYear.text.toString().toInt()-1,
                if (binding.budgetAddMonth.text.toString() == "") 1 else binding.budgetAddMonth.text.toString().toInt()
            ),
            prevMonth,
            whoText)
        binding.budgetAddTotalAmount.text = dec.format(annualActuals)
        binding.budgetAddAverageAmount.text = dec.format(annualActuals/12)

        if (binding.budgetAddPercentage.text.toString() != "")
            setAmountBasedOnPercentage()
    }

    fun loadCategoryRadioButtons() {
        var ctr: Int = 100
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAddCategoryRadioGroup)
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
            Log.d(
                "Alex",
                "Added new category radio button " + newRadioButton.text.toString() + " with id " + newRadioButton.id
            )
        }
        var somethingChecked: Boolean = false
        for (i in 0 until radioGroup.childCount) {
            val o = radioGroup.getChildAt(i)
            if (o is RadioButton && o.text == args.category) {
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
        var subCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.budgetAddSubCategorySpinner)
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged()
        }
        Log.d("Alex", "subcategory is " + args.subcategory + " and position is " + arrayAdapter.getPosition(args.subcategory))
        if (args.subcategory != "")
            subCategorySpinner.setSelection(arrayAdapter.getPosition(args.subcategory))
    }

    fun loadSpenderRadioButtons() {
        var ctr: Int
        ctr = 200
        val whoRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAdd_whoRadioGroup)
        if (whoRadioGroup == null) Log.d("Alex", " rg 'paidby' is null")
        else whoRadioGroup.removeAllViews()

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
            whoRadioGroup.addView(newRadioButton)
            if (i == SpenderViewModel.getCount()-1)  // ie check the last one
                newRadioButton.isChecked = true
        }
    }

    fun loadOccurenceRadioButtons() {
        var ctr: Int
        ctr = 300
        val occurenceRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.budgetAdd_occurenceRadioGroup)
        if (occurenceRadioGroup == null) Log.d("Alex", " rg 'occurence' is null")
        else occurenceRadioGroup.removeAllViews()

        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.setText(cBUDGET_JUST_THIS_MONTH)
        newRadioButton.id = ctr++
        newRadioButton.buttonTintList=
            ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        occurenceRadioGroup.addView(newRadioButton)

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.setText(cBUDGET_RECURRING)
        newRadioButton.id = ctr++
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
            menu.getItem(i).setVisible(false)
        }
    }

    private fun onSaveButtonClicked () {
        // need to reject if all the fields aren't entered correctly
        if (binding.budgetAddYear.text.toString().toInt() < 2000 || binding.budgetAddYear.text.toString().toInt() > 2100 ) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.invalidYear))
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddYear)
            return
        }
        if (binding.budgetAddMonth.text.toString() != "") {
            if (binding.budgetAddMonth.text.toString()
                    .toInt() < 1 || binding.budgetAddMonth.text.toString().toInt() > 12
            ) {
                showErrorMessage(getParentFragmentManager(), getString(R.string.invalidMonth))
                focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddMonth)
                return
            }
        }
        if (binding.budgetAddAmount.text.toString() == "") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddAmount)
            return
        }
        if (binding.budgetAddAmount.text.toString().toDouble() == 0.0) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
            focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddAmount)
            return
        }
        val selectedId = binding.budgetAddWhoRadioGroup.getCheckedRadioButtonId()
        var whoText: String = ""
        if (SpenderViewModel.getCount() == 1)
            whoText = SpenderViewModel.getSpenderName(0)
        else {
            val whoRadioButton = requireActivity().findViewById(selectedId) as RadioButton
            whoText = whoRadioButton.text.toString()
        }

        val occSelectedId = binding.budgetAddOccurenceRadioGroup.getCheckedRadioButtonId()
        val occRadioButton = requireActivity().findViewById(occSelectedId) as RadioButton
        val occurenceText = occRadioButton.text.toString()

        val catSelectedId = binding.budgetAddCategoryRadioGroup.getCheckedRadioButtonId()
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        val tempCategory: String = catRadioButton.getText().toString() + "-" + binding.budgetAddSubCategorySpinner.selectedItem.toString()
        val tempBudget = BudgetViewModel.getBudget(tempCategory)
        if (tempBudget != null) {
            var chosenMonth: Int = 0
            if (binding.budgetAddMonth.text.toString() != "")
                chosenMonth = binding.budgetAddMonth.text.toString().toInt()

            Log.d("Alex", "year is '" + binding.budgetAddYear.text.toString() + "'")
            if (tempBudget.overlapsWithExistingBudget(
                    BudgetMonth(
                        binding.budgetAddYear.text.toString().toInt(),
                        chosenMonth
                    ).toString(),
                    whoText
                )
            ) {
                showErrorMessage(getParentFragmentManager(), getString(R.string.budgetOverlap))
                focusAndOpenSoftKeyboard(requireContext(), binding.budgetAddMonth)
                return
            }
        }
        val catRadioGroup = binding.budgetAddCategoryRadioGroup
        val radioButtonID = catRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(radioButtonID) as RadioButton
        Log.d("Alex", "Category is " + radioButton.text)

        val subcategorySpinner = binding.budgetAddSubCategorySpinner
        Log.d("Alex", "Sub-category is " + subcategorySpinner.selectedItem.toString())

        var tempDouble : Double
        var amountInt: Int
        tempDouble = round(binding.budgetAddAmount.text.toString().toDouble()*100)
        amountInt = tempDouble.toInt()

        var period = binding.budgetAddYear.text.toString()
        if (binding.budgetAddMonth.text.toString() != "") {
            if (binding.budgetAddMonth.text.toString().toInt() < 10)
                period = period + "-0" + binding.budgetAddMonth.text.toString()
            else
                period = period + "-" + binding.budgetAddMonth.text.toString()
        } else {
            period = period + "-00"
        }

        BudgetViewModel.updateBudget(tempCategory, period, whoText, amountInt, occurenceText)
        binding.budgetAddAmount.setText("")
        binding.budgetAddAmount.requestFocus()
        binding.budgetAddYear.setText("")
        binding.budgetAddMonth.setText("")
        binding.budgetAddPercentage.setText("")
        hideKeyboard(requireContext(), requireView())
        Toast.makeText(activity, "Budget item added", Toast.LENGTH_SHORT).show()
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}