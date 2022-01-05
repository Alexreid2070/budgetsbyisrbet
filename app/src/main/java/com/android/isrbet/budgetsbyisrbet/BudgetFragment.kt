package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
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
        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCategoryRadioButtons()
        loadSpenderRadioButtons()

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
    }

    fun setupForEdit() {
        (activity as AppCompatActivity).supportActionBar?.title = "Edit Budget"
        binding.budgetAddCategoryRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddSubCategorySpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddYear.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddMonth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddWhoRadioGroup.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.budgetAddPercentage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))

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
        binding.budgetAddAmount.isEnabled = true
        binding.budgetAddOrLabel.visibility = VISIBLE
        binding.budgetAddPercentageLayout.visibility = VISIBLE
        binding.budgetAddPreviousAmountLayout.visibility = VISIBLE
        binding.budgetAddActualAmountLayout.visibility = VISIBLE
        binding.budgetAddAverageAmountLayout.visibility = VISIBLE
        binding.budgetAddAmountLabel.text = "Enter new budget amount: "

        binding.budgetAddPercentage.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                var tempDouble = binding.budgetAddPreviousAmount.text.toString().toDouble()
                tempDouble = tempDouble * (1 + binding.budgetAddPercentage.toString().toDouble()/100)
                binding.budgetAddAmount.setText(tempDouble.toString())
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })
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
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            whoRadioGroup.addView(newRadioButton)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).setVisible(false)
        }
    }

    private fun viewBudget(iCategory: String, iSubcategory: String, iPeriod: String, iWho: String) {
        val thisBudget = BudgetViewModel.getBudget(iCategory + "-" + iSubcategory)
        val thisBudgetPeriod = thisBudget?.getPeriod(iPeriod, iWho)
        if (thisBudgetPeriod == null) {
            Toast.makeText(activity, "Attempt to view failed, budget not found", Toast.LENGTH_SHORT).show()
            return
        }
        val iAmount = thisBudgetPeriod.amount
        val dec = DecimalFormat("#.00")
        val formattedAmount = (iAmount/100).toDouble() + (iAmount % 100).toDouble()/100
        binding.budgetAddAmount.setText(dec.format(formattedAmount))

        val categoryGroup = binding.budgetAddCategoryRadioGroup
        for (i in 0 until categoryGroup.childCount) {
            val o = categoryGroup.getChildAt(i)
            if (o is RadioButton && o.text == iCategory) {
                o.isChecked = true
            }
        }

        var subCategorySpinner = binding.budgetAddSubCategorySpinner
        val subCategoryList: MutableList<String> = ArrayList()
        subCategoryList.add(iSubcategory)
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subCategoryList
        )

        subCategorySpinner.adapter = arrayAdapter
        subCategorySpinner.setSelection(arrayAdapter.getPosition(iSubcategory))

        binding.budgetAddYear.setText(thisBudgetPeriod.getYear())
        binding.budgetAddMonth.setText(thisBudgetPeriod.getMonth())

        val whoRadioGroup = binding.budgetAddWhoRadioGroup
        for (i in 0 until whoRadioGroup.childCount) {
            val o = whoRadioGroup.getChildAt(i)
            if (o is RadioButton) {
                if (o.text == iWho) {
                    o.isChecked = true
                }
            }
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
        val selectedId = binding.budgetAddWhoRadioGroup.getCheckedRadioButtonId()
        var whoText: String = ""
        if (SpenderViewModel.getCount() == 1)
            whoText = SpenderViewModel.getSpenderName(0)
        else {
            val whoRadioButton = requireActivity().findViewById(selectedId) as RadioButton
            whoText = whoRadioButton.text.toString()
        }
        val catSelectedId = binding.budgetAddCategoryRadioGroup.getCheckedRadioButtonId()
        val catRadioButton = requireActivity().findViewById(catSelectedId) as RadioButton
        var tempCategory: String = catRadioButton.getText().toString() + "-" + binding.budgetAddSubCategorySpinner.selectedItem.toString()
        var tempBudget = BudgetViewModel.getBudget(tempCategory)
        if (tempBudget != null) {
            val selectedId = binding.budgetAddWhoRadioGroup.getCheckedRadioButtonId()
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

        BudgetViewModel.updateBudget(tempCategory, period, whoText, amountInt)
        binding.budgetAddAmount.setText("")
        binding.budgetAddAmount.requestFocus()
        binding.budgetAddYear.setText("")
        binding.budgetAddMonth.setText("")
        hideKeyboard(requireContext(), requireView())
        Toast.makeText(activity, "Budget item added", Toast.LENGTH_SHORT).show()

        val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
        mp.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}