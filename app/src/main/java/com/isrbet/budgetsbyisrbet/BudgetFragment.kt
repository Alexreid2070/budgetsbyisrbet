package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import java.util.*

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val args: BudgetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)

        binding.budgetAddAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        inflater.inflate(R.layout.fragment_budget, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = "${getLocalCurrencySymbol()} "
        val startOfMonth = MyDate()
        startOfMonth.setDay(1)
        binding.startDate.setText(startOfMonth.toString())
        binding.regularity.setText("1")
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.startDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.startDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.startDate.text.toString() != "") {
                lcal = MyDate(binding.startDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
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
    }

    private fun setupForEdit() {
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.budgetAddSubCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.budgetAddSubCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)

        binding.periodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.periodSpinner.setPopupBackgroundResource(R.drawable.spinner)

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
    }

    private fun loadCategoryRadioButtons() {
        var ctr = 100
        binding.budgetAddCategoryRadioGroup.removeAllViews()

        val categoryNames = CategoryViewModel.getCategoryNames()

        categoryNames.forEach {
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = it
            newRadioButton.id = ctr++
            binding.budgetAddCategoryRadioGroup.addView(newRadioButton)
        }
        var somethingChecked = false
        for (i in 0 until binding.budgetAddCategoryRadioGroup.childCount) {
            val o = binding.budgetAddCategoryRadioGroup.getChildAt(i)
            if (o is RadioButton && o.text == CategoryViewModel.getCategory(args.categoryID.toInt())?.categoryName) {
                o.isChecked = true
                somethingChecked = true
                addSubCategories(o.text.toString())
            }
        }
        if (!somethingChecked) {
            val o = binding.budgetAddCategoryRadioGroup.getChildAt(0)
            if (o is RadioButton) {
                o.isChecked = true
                addSubCategories(o.text.toString())
            }
        }
    }

    private fun addSubCategories(iCategory: String) {
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        binding.budgetAddSubCategorySpinner.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
        if (args.categoryID != "")
            binding.budgetAddSubCategorySpinner.setSelection(arrayAdapter.getPosition(CategoryViewModel.getCategory(args.categoryID.toInt())?.subcategoryName))
    }

    private fun loadSpenderRadioButtons() {
        var ctr = 200
        binding.budgetAddWhoRadioGroup.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            binding.budgetAddWhoRadioGroup.addView(newRadioButton)
            if (i == SpenderViewModel.getActiveCount()-1)  // ie check the last one
                newRadioButton.isChecked = true
        }
    }

    private fun loadOccurenceRadioButtons() {
        var ctr = 300
        binding.budgetAddOccurenceRadioGroup.removeAllViews()

        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.once)
        newRadioButton.id = ctr++
//        newRadioButton.buttonTintList=
  //          ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.budgetAddOccurenceRadioGroup.addView(newRadioButton)

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.recurring)
        newRadioButton.id = ctr
//        newRadioButton.buttonTintList=
  //          ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.budgetAddOccurenceRadioGroup.addView(newRadioButton)

        val o = binding.budgetAddOccurenceRadioGroup.getChildAt(1) // ie check Recurring
        if (o is RadioButton) {
            o.isChecked = true
        }
    }

    private fun onSaveButtonClicked () {
        val lNumberFormat: NumberFormat = NumberFormat.getInstance()
        val amountDouble = lNumberFormat.parse(binding.budgetAddAmount.text.toString()).toDouble()
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