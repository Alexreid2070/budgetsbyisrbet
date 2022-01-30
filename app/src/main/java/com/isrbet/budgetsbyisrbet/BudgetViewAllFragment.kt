package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetViewAllBinding

class BudgetViewAllFragment : Fragment() {
    private var _binding: FragmentBudgetViewAllBinding? = null
    private val binding get() = _binding!!
    private var currentFilter: String = cCONDENSED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetViewAllBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        inflater.inflate(R.layout.fragment_budget_view_all, container, false)
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            when (menu.getItem(i).itemId) {
                R.id.AddBudget -> {
                    menu.getItem(i).isVisible = true
                }
                R.id.FilterExpanded -> {
                    menu.getItem(i).isVisible = true
                    menu.getItem(i).isChecked = currentFilter == cEXPANDED
                }
                R.id.FilterCondensed -> {
                    menu.getItem(i).isVisible = true
                    menu.getItem(i).isChecked = currentFilter == cCONDENSED
                }
                R.id.FilterTitle -> {
                    menu.getItem(i).isVisible = true
                }
                R.id.Previous -> {
                    menu.getItem(i).isVisible = true
                }
                R.id.Next -> {
                    menu.getItem(i).isVisible = true
                }
                else -> menu.getItem(i).isVisible = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.AddBudget) {
            val currentCategory = binding.budgetCategorySpinner.selectedItem.toString()
            var cat = ""
            var subcat = ""
            val dash = currentCategory.indexOf("-")
            if (dash > -1) {
                cat = currentCategory.substring(0,dash)
                subcat = currentCategory.substring(dash + 1, currentCategory.length)
                Log.d("Alex", "cat is '$cat'")
                Log.d("Alex", "subcat is '$subcat'")
            }
            val action = BudgetViewAllFragmentDirections.actionBudgetViewAllFragmentToBudgetFragment()
            action.category = cat
            action.subcategory = subcat
            findNavController().navigate(action)

//            findNavController().navigate(R.id.action_BudgetViewAllFragment_to_BudgetFragment)
            return true
        } else if (item.itemId == R.id.FilterExpanded) {
            if (currentFilter != cEXPANDED) {
                item.isChecked = true
                currentFilter = cEXPANDED
            }
            activity?.invalidateOptionsMenu()
            loadRows(binding.budgetCategorySpinner.selectedItem.toString())
            return true
        } else if (item.itemId == R.id.FilterCondensed) {
            if (currentFilter != cCONDENSED) {
                item.isChecked = true
                currentFilter = cCONDENSED
            }
            activity?.invalidateOptionsMenu()
            loadRows(binding.budgetCategorySpinner.selectedItem.toString())
            return true
        } else if (item.itemId == R.id.Previous) {
            moveCategories(-1)
            return true
        } else if (item.itemId == R.id.Next) {
            moveCategories(1)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = "Budgets"

        val categorySpinner = binding.budgetCategorySpinner
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getCombinedCategoriesForSpinner())
        categorySpinner.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.budgetCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))

        categorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                loadRows(selection as String)
                val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
                Log.d("Alex", "scrolling to " + (listView.adapter.count-1))
                listView.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
//                listView.smoothScrollToPosition(listView.adapter.count-1)
                listView.setSelection(listView.adapter.count - 1)
//                listView.isStackFromBottom = true
//                listView.isStackFromBottom = true
            }
        }

        if (SpenderViewModel.singleUser()) {
            binding.rowBudgetWhoHeading.visibility = View.GONE
        }
    }

    fun loadRows(iCategory: String) {
        Log.d("Alex", "in load rows, iCategory is '$iCategory'")
        val adapter = BudgetAdapter(requireContext(), BudgetViewModel.getBudgetInputRows(iCategory, currentFilter))
        val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
        listView.adapter = adapter

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {
                val itemValue = listView.getItemAtPosition(position) as BudgetInputRow
                val bmDateApplicable = BudgetMonth(itemValue.dateApplicable)
                val bmDateStart = BudgetMonth(itemValue.dateStarted)
                if (itemValue.dateApplicable == itemValue.dateStarted ||
                        (bmDateApplicable.month == 1 && bmDateStart.month == 0)) {// ie only allow edits on "real" entries
                    val bmDateStarted = BudgetMonth(itemValue.dateStarted)
                    var monthToSend: Int = bmDateApplicable.month
                    if (bmDateStarted.month == 0)
                        monthToSend = 0
                    val amountToSend = if (monthToSend == 0)
                        itemValue.amount.toDouble() * 12
                    else
                        itemValue.amount.toDouble()
                    val rtdf = BudgetDialogFragment.newInstance(
                        binding.budgetCategorySpinner.selectedItem.toString(),
                        bmDateApplicable.year,
                        monthToSend,
                        itemValue.who,
                        amountToSend,
                        itemValue.occurence.toInt()
                    )
                    rtdf.setDialogFragmentListener(object :
                        BudgetDialogFragment.BudgetEditDialogFragmentListener {
                        override fun onNewDataSaved() {
                            Log.d("Alex", "in onNewDataSaved")
                            val tadapter = BudgetAdapter(
                                requireContext(),
                                BudgetViewModel.getBudgetInputRows(iCategory, currentFilter)
                            )
                            listView.adapter = tadapter
                            tadapter.notifyDataSetChanged()
                        }
                    })
                    rtdf.show(parentFragmentManager, "Edit Budget")
                } else {
                    Toast.makeText(requireActivity(), "Only actual budget entries (in bold) are clickable.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun moveCategories(iDirection: Int) {
        val currentCategoryPosition = binding.budgetCategorySpinner.selectedItemPosition
        val newCategoryPosition = if (iDirection == -1) {
            if (currentCategoryPosition > 0) {
                currentCategoryPosition - 1
            } else {
                0
            }
        } else { // has to be +1
            if (currentCategoryPosition < binding.budgetCategorySpinner.adapter.count-1) {
                currentCategoryPosition + 1
            } else {
                binding.budgetCategorySpinner.adapter.count-1
            }
        }
        val newCategory = binding.budgetCategorySpinner.getItemAtPosition(newCategoryPosition)
        binding.budgetCategorySpinner.setSelection(newCategoryPosition)
        loadRows(newCategory.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}