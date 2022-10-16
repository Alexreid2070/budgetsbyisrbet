package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetViewAllBinding
import it.sephiroth.android.library.numberpicker.doOnProgressChanged
import java.util.*

class BudgetViewAllFragment : Fragment() {
    private var _binding: FragmentBudgetViewAllBinding? = null
    private val binding get() = _binding!!
    private val args: BudgetViewAllFragmentArgs by navArgs()

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
        setupCategorySpinner()
        if (binding.buttonViewByDate.isChecked) {
            setupDateSelectors()
            binding.rowBudgetDateHeading.text = getString(R.string.category)
            loadRows(0, binding.budgetAddYear.progress, binding.budgetAddMonth.progress)
        } else {
            binding.rowBudgetDateHeading.text = getString(R.string.date)
            setCategoryType()
        }

        binding.budgetCategorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                setCategoryType()
                val currentCategory = Category(0,selection as String)
                loadRows(currentCategory.id, 0, 0)
                val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
                listView.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
                listView.setSelection(listView.adapter.count - 1)
            }
        }
        if (SpenderViewModel.singleUser()) {
            binding.rowBudgetWhoHeading.visibility = View.GONE
        }
        binding.expandSettings.setOnClickListener {
            findNavController().navigate(R.id.SettingsFragment)
        }
        binding.expandCategories.setOnClickListener {
            findNavController().navigate(R.id.CategoryFragment)
        }
        binding.expandScheduledPayments.setOnClickListener {
            findNavController().navigate(R.id.ScheduledPaymentFragment)
        }
        binding.buttonBackward.setOnClickListener {
            if (binding.buttonViewByDate.isChecked)
                moveDates(-1)
            else
                moveCategories(-1)
        }
        binding.buttonForward.setOnClickListener {
            if (binding.buttonViewByDate.isChecked)
                moveDates(1)
            else
                moveCategories(1)
        }

        binding.budgetAddFab.setOnClickListener {
            val currentCategory = Category(0, binding.budgetCategorySpinner.selectedItem.toString())
            val action =
                BudgetViewAllFragmentDirections.actionBudgetViewAllFragmentToBudgetFragment()
            action.categoryID = currentCategory.id.toString()
            findNavController().navigate(action)
        }
        updateFabVisibility()
        binding.buttonViewByCategory.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_BUDGET_VIEW, cBudgetCategoryView)
            binding.rowBudgetDateHeading.text = getString(R.string.date)
            setupCategorySpinner()
            setCategoryType()
        }
        binding.buttonViewByDate.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_BUDGET_VIEW, cBudgetDateView)
            binding.rowBudgetDateHeading.text = getString(R.string.category)
            setupDateSelectors()
            loadRows(0, binding.budgetAddYear.progress, binding.budgetAddMonth.progress)
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
        val cal = android.icu.util.Calendar.getInstance()
        if (args.year == "") {
            binding.budgetAddYear.progress = cal.get(Calendar.YEAR)
            binding.budgetAddMonth.progress = cal.get(Calendar.MONTH) + 1
        } else {
            binding.budgetAddYear.progress = args.year.toInt()
            binding.budgetAddMonth.progress = args.month.toInt()
        }
        binding.rowBudgetIsSingleHeading.visibility = View.GONE
        val param = binding.rowBudgetDateHeading.layoutParams as LinearLayout.LayoutParams
        param.weight = 3f
        binding.rowBudgetDateHeading.layoutParams = param
        binding.budgetAddYear.doOnProgressChanged { np, _, _ -> loadRows(0, np.progress, binding.budgetAddMonth.progress) }
        binding.budgetAddMonth.doOnProgressChanged { np, _, _ -> loadRows(0, binding.budgetAddYear.progress, np.progress) }
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

    fun loadRows(iCategoryID: Int, iYear: Int, iMonth: Int) {
        var noDataText: String
        val rows = if (binding.buttonViewByDate.isChecked) {
            noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                    " ${gMonthName(iMonth)} $iYear.  " + getString(R.string.click_on_the_add_button_below_to_add_a_budget)
            BudgetViewModel.getBudgetInputRows(BudgetMonth(iYear, iMonth))
        } else {
            val cat = CategoryViewModel.getCategory(iCategoryID)
            noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                    " ${cat?.categoryName}-${cat?.subcategoryName}. " +
                    getString(R.string.click_on_the_add_button_below_to_add_a_budget)
            BudgetViewModel.getBudgetInputRows(iCategoryID)
        }
        val adapter = BudgetAdapter(requireContext(), rows)
        val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
        listView.adapter = adapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val itemValue = listView.getItemAtPosition(position) as BudgetInputRow
                val bmDateApplicable = BudgetMonth(itemValue.dateApplicable)
                val bmDateStart = BudgetMonth(itemValue.dateStarted)
                if (itemValue.dateApplicable == itemValue.dateStarted ||
                    (bmDateApplicable.month == 1 && bmDateStart.month == 0)
                ) {// ie only allow edits on "real" entries
                    val bmDateStarted = BudgetMonth(itemValue.dateStarted)
                    var monthToSend: Int = bmDateApplicable.month
                    if (bmDateStarted.isAnnualBudget())
                        monthToSend = 0
                    val amountToSend = itemValue.amount.toDouble()
                    val rtdf = BudgetDialogFragment.newInstance(
                        itemValue.categoryID,
                        bmDateApplicable.year,
                        monthToSend,
                        itemValue.who,
                        amountToSend,
                        itemValue.occurence.toInt()
                    )
                    rtdf.setDialogFragmentListener(object :
                        BudgetDialogFragment.BudgetEditDialogFragmentListener {
                        override fun onNewDataSaved() {
                            val trows = if (binding.buttonViewByDate.isChecked) {
                                noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                                        " ${gMonthName(iMonth)} $iYear.  " +
                                        getString(R.string.click_on_the_add_button_below_to_add_a_budget)
                                BudgetViewModel.getBudgetInputRows(BudgetMonth(iYear, iMonth))
                            } else {
                                val cat = CategoryViewModel.getCategory(iCategoryID)
                                noDataText = getString(R.string.you_have_not_yet_entered_any_budgets_for) +
                                        " ${cat?.categoryName}-${cat?.subcategoryName}.  " +
                                        getString(R.string.click_on_the_add_button_below_to_add_a_budget)
                                BudgetViewModel.getBudgetInputRows(iCategoryID)
                            }
                            val tadapter = BudgetAdapter(requireContext(), trows)

                            listView.adapter = tadapter
                            tadapter.notifyDataSetChanged()
                        }
                    })
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

    private fun moveDates(iDirection: Int) {
        if (iDirection == 1) {
            if (binding.budgetAddMonth.progress == 12) {
                binding.budgetAddYear.progress = binding.budgetAddYear.progress + 1
                binding.budgetAddMonth.progress = 1
            } else {
                binding.budgetAddMonth.progress = binding.budgetAddMonth.progress + 1
            }
        } else {
            if (binding.budgetAddMonth.progress == 1) {
                binding.budgetAddYear.progress = binding.budgetAddYear.progress - 1
                binding.budgetAddMonth.progress = 12
            } else {
                binding.budgetAddMonth.progress = binding.budgetAddMonth.progress - 1
            }
        }
        loadRows(0, binding.budgetAddYear.progress, binding.budgetAddMonth.progress)
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
        loadRows(currentCategory.id, 0, 0)
        updateFabVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}