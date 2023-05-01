package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetViewAllBinding

class BudgetViewAllFragment : Fragment() {
    private var _binding: FragmentBudgetViewAllBinding? = null
    private val binding get() = _binding!!
    private val args: BudgetViewAllFragmentArgs by navArgs()
    private val currentMonth = MyDate(gCurrentDate.getYear(), gCurrentDate.getMonth(), 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val budgetListObserver = Observer<MutableList<Budget>> {
            if (binding.buttonViewByDate.isChecked) {
                loadRows(0, currentMonth)
            } else {
                val currentCategory =
                    Category(0, binding.budgetCategorySpinner.selectedItem.toString())
                loadRows(currentCategory.id, currentMonth)
            }
        }
        BudgetViewModel.observeList(this, budgetListObserver)
        val categoryListObserver = Observer<MutableList<Category>> {
            if (binding.buttonViewByCategory.isChecked)
                setupCategorySpinner()
        }
        CategoryViewModel.observeList(this, categoryListObserver)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetViewAllBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_budget_view_all, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        if (args.categoryID == "") {
            when (DefaultsViewModel.getDefaultBudgetView()) {
                cBudgetDateView -> binding.buttonViewByDate.isChecked = true
                cBudgetCategoryView -> binding.buttonViewByCategory.isChecked = true
            }
        } else {
                binding.buttonViewByCategory.isChecked = true
        }
        if (binding.buttonViewByDate.isChecked) {
            setupDateSelectors()
            binding.rowBudgetDateHeading.text = getString(R.string.category)
            loadRows(0, currentMonth)
        } else {
            setupCategorySpinner()
            binding.rowBudgetDateHeading.text = getString(R.string.date)
            setCategoryType()
        }

        binding.budgetCategorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.buttonViewByCategory.isChecked) {
                    val selection = parent?.getItemAtPosition(position)
                    setCategoryType()
                    val currentCategory = Category(0, selection as String)
                    loadRows(currentCategory.id, currentMonth)
                    binding.budgetListView.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
                    binding.budgetListView.setSelection(binding.budgetListView.adapter.count - 1)
                }
            }
        }
        if (SpenderViewModel.singleUser()) {
            binding.rowBudgetWhoHeading.visibility = View.GONE
        }
        binding.buttonYearBackward.setOnClickListener {
            moveDates(-12)
        }
        binding.buttonMonthBackward.setOnClickListener {
            if (binding.buttonViewByDate.isChecked)
                moveDates(-1)
            else
                moveCategories(-1)
        }
        binding.buttonMonthForward.setOnClickListener {
            if (binding.buttonViewByDate.isChecked)
                moveDates(1)
            else
                moveCategories(1)
        }
        binding.buttonYearForward.setOnClickListener {
            moveDates(12)
        }

        binding.budgetAddFab.setOnClickListener {
            val currentCategory = Category(0, binding.budgetCategorySpinner.selectedItem.toString())
            val action =
                SettingsTabsFragmentDirections.actionSettingsTabFragmentToBudgetFragment()
            action.categoryID = currentCategory.id.toString()
            findNavController().navigate(action)
        }
        updateFabVisibility()
        binding.buttonViewByCategory.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_BUDGET_VIEW, cBudgetCategoryView)
            binding.buttonYearForward.visibility = View.GONE
            binding.buttonYearBackward.visibility = View.GONE
            binding.rowBudgetDateHeading.text = getString(R.string.date)
            setupCategorySpinner()
            setCategoryType()
        }
        binding.buttonViewByDate.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_BUDGET_VIEW, cBudgetDateView)
            binding.buttonYearForward.visibility = View.VISIBLE
            binding.buttonYearBackward.visibility = View.VISIBLE
            binding.rowBudgetDateHeading.text = getString(R.string.category)
            setupDateSelectors()
            loadRows(0, currentMonth)
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.budget_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
        HintViewModel.showHint(parentFragmentManager, cHINT_BUDGET)
    }

    private fun updateFabVisibility() {
        if (binding.buttonViewByCategory.isChecked) {
            val currentCategory = CategoryViewModel.getCategory(binding.budgetCategorySpinner.selectedItem.toString())
            if (currentCategory?.inUse == true)
                binding.budgetAddFab.visibility = View.VISIBLE
            else
                binding.budgetAddFab.visibility = View.GONE
        }
    }

    private fun setupCategorySpinner() {
        binding.budgetCategorySpinnerLayout.visibility = View.VISIBLE
        binding.categoryTypeLayout.visibility = View.VISIBLE
        binding.rowBudgetIsSingleHeading.visibility = View.VISIBLE
        binding.yearLayout.visibility = View.GONE
        val param = binding.rowBudgetDateHeading.layoutParams as LinearLayout.LayoutParams
        param.weight = 1f
        binding.rowBudgetDateHeading.layoutParams = param
        val categorySpinner = binding.budgetCategorySpinner
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getCombinedCategoriesForSpinner())
        categorySpinner.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.budgetCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.budgetCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)

        if (args.categoryID != "") {
            categorySpinner.setSelection(
                arrayAdapter.getPosition(
                    CategoryViewModel.getCategory(args.categoryID.toInt())?.fullCategoryName()
                )
            )
        }
    }

    private fun setupDateSelectors() {
        binding.budgetCategorySpinnerLayout.visibility = View.GONE
        binding.categoryTypeLayout.visibility = View.GONE
        binding.yearLayout.visibility = View.VISIBLE
        if (args.year == "") {
            binding.dateLabel.text = currentMonth.getMMMYYYY()
        } else {
            val tDate = MyDate(args.year.toInt(), args.month.toInt(), 1)
            binding.dateLabel.text = tDate.getMMMYYYY()
        }
        binding.rowBudgetIsSingleHeading.visibility = View.GONE
        val param = binding.rowBudgetDateHeading.layoutParams as LinearLayout.LayoutParams
        param.weight = 3f
        binding.rowBudgetDateHeading.layoutParams = param
    }

    fun setCategoryType() {
        val category = Category(0, binding.budgetCategorySpinner.selectedItem.toString())
        if (CategoryViewModel.getCategory(category.id)?.discType == cDiscTypeDiscretionary)
            binding.categoryType.text = getString(R.string.discretionary)
        else
            binding.categoryType.text = getString(R.string.non_discretionary)
        if (CategoryViewModel.getCategory(category.id)?.inUse == false) {
            binding.categoryType.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.red))
        } else {
            binding.categoryType.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black))
        }
    }

    fun loadRows(iCategoryID: Int, iDate: MyDate) {
        val noDataText: String
        val rows = if (binding.buttonViewByDate.isChecked) {
            noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                    " ${iDate.getMMMYYYY()}.  " + getString(R.string.click_on_the_add_button_below_to_add_a_budget)
            BudgetViewModel.getBudgetInputRows(iDate)
        } else {
            val cat = CategoryViewModel.getCategory(iCategoryID)
            noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                    " ${cat?.categoryName}-${cat?.subcategoryName}. " +
                    getString(R.string.click_on_the_add_button_below_to_add_a_budget)
            BudgetViewModel.getBudgetInputRows(iCategoryID)
        }
        val adapter = BudgetAdapter(requireContext(), rows)
        binding.budgetListView.adapter = adapter
        binding.budgetListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val itemValue = binding.budgetListView.getItemAtPosition(position) as BudgetInputRow
                val bmDateApplicable = MyDate(itemValue.dateApplicable)
//                val bmDateStart = MyDate(itemValue.dateStarted)
                if (itemValue.dateApplicable == itemValue.dateStarted ||
                    (bmDateApplicable.getMonth() == 1 && itemValue.period == cPeriodYear) //bmDateStart.month == 0)
                ) {// ie only allow edits on "real" entries
                    val amountToSend = itemValue.amount.toDouble()
                    val rtdf = BudgetEditDialogFragment.newInstance(
                        itemValue.key,
                        itemValue.categoryID,
                        bmDateApplicable.toString(),
                        itemValue.period,
                        itemValue.regularity,
                        itemValue.who,
                        amountToSend,
                        itemValue.occurence.toInt()
                    )
/*                    rtdf.setDialogFragmentListener(object :
                        BudgetEditDialogFragment.BudgetEditDialogFragmentListener {
                        override fun onNewDataSaved() {
                            val trows = if (binding.buttonViewByDate.isChecked) {
                                noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                                        " ${gMonthName(iMonth)} $iYear.  " +
                                        getString(R.string.click_on_the_add_button_below_to_add_a_budget)
                                BudgetViewModel.getBudgetInputRows(MyDate(iYear, iMonth, 1))
                            } else {
                                val cat = CategoryViewModel.getCategory(iCategoryID)
                                noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                                        " ${cat?.categoryName}-${cat?.subcategoryName}.  " +
                                        getString(R.string.click_on_the_add_button_below_to_add_a_budget)
                                BudgetViewModel.getBudgetInputRows(iCategoryID)
                            }
                            val tadapter = BudgetAdapter(requireContext(), trows)

                            binding.budgetListView.adapter = tadapter
                            tadapter.notifyDataSetChanged()
                        }
                    }) */
                    rtdf.show(parentFragmentManager, getString(R.string.edit_budget))
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.only_actual_budget_entries_in_bold_are_clickable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        if (rows.size == 0) {
            binding.noInformationText.visibility = View.VISIBLE
            binding.noInformationText.text = noDataText
//            binding.budgetListView.visibility = View.GONE
        } else {
            binding.noInformationText.visibility = View.GONE
//            binding.budgetListView.visibility = View.VISIBLE
        }
    }

    private fun moveDates(iNumOfMonths: Int) {
        currentMonth.increment(cPeriodMonth, iNumOfMonths)
        binding.dateLabel.text = currentMonth.getMMMYYYY()
        loadRows(0, currentMonth)
    }

    private fun moveCategories(iDirection: Int) {
        val currentCategoryPosition = binding.budgetCategorySpinner.selectedItemPosition
        val newCategoryPosition = if (iDirection == -1) {
            if (currentCategoryPosition > 0) {
                currentCategoryPosition - 1
            } else {
                binding.budgetCategorySpinner.adapter.count-1
            }
        } else { // has to be +1
            if (currentCategoryPosition < binding.budgetCategorySpinner.adapter.count-1) {
                currentCategoryPosition + 1
            } else {
                0
            }
        }
        val newCategory = binding.budgetCategorySpinner.getItemAtPosition(newCategoryPosition)
        binding.budgetCategorySpinner.setSelection(newCategoryPosition)
        val currentCategory = Category(0, newCategory.toString())
        loadRows(currentCategory.id, currentMonth)
        updateFabVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}