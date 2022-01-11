package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetViewAllBinding

class BudgetViewAllFragment : Fragment() {
    private var _binding: FragmentBudgetViewAllBinding? = null
    private val binding get() = _binding!!
    private var currentFilter: String = cCONDENSED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetViewAllBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        inflater.inflate(R.layout.fragment_budget_view_all, container, false)
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.AddBudget) {
                menu.getItem(i).setVisible(true)
            } else if (menu.getItem(i).getItemId() === R.id.FilterExpanded) {
                menu.getItem(i).setVisible(true)
                if (currentFilter == cEXPANDED)
                    menu.getItem(i).setChecked(true)
                else
                    menu.getItem(i).setChecked(false)
            } else if (menu.getItem(i).getItemId() === R.id.FilterCondensed) {
                menu.getItem(i).setVisible(true)
                if (currentFilter == cCONDENSED)
                    menu.getItem(i).setChecked(true)
                else
                    menu.getItem(i).setChecked(false)
            } else if (menu.getItem(i).getItemId() == R.id.FilterTitle) {
                menu.getItem(i).setVisible(true)
            } else if (menu.getItem(i).getItemId() == R.id.Previous) {
                menu.getItem(i).setVisible(true)
            } else if (menu.getItem(i).getItemId() == R.id.Next) {
                menu.getItem(i).setVisible(true)
            } else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.AddBudget) {
            val currentCategory = binding.budgetCategorySpinner.selectedItem.toString()
            var cat: String = ""
            var subcat: String = ""
            val dash = currentCategory.indexOf("-")
            if (dash > -1) {
                cat = currentCategory.substring(0,dash)
                subcat = currentCategory.substring(dash + 1, currentCategory.length)
                Log.d("Alex", "cat is '" + cat + "'")
                Log.d("Alex", "subcat is '" + subcat + "'")
            }
            val action = BudgetViewAllFragmentDirections.actionBudgetViewAllFragmentToBudgetFragment()
            action.category = cat
            action.subcategory = subcat
            findNavController().navigate(action)

//            findNavController().navigate(R.id.action_BudgetViewAllFragment_to_BudgetFragment)
            return true
        } else if (item.itemId === R.id.FilterExpanded) {
            if (currentFilter == cEXPANDED) {
            } else {
                item.setChecked(true)
                currentFilter = cEXPANDED
            }
            activity?.invalidateOptionsMenu()
            loadRows(binding.budgetCategorySpinner.selectedItem.toString())
            return true
        } else if (item.itemId === R.id.FilterCondensed) {
            if (currentFilter == cCONDENSED) {
            } else {
                item.setChecked(true)
                currentFilter = cCONDENSED
            }
            activity?.invalidateOptionsMenu()
            loadRows(binding.budgetCategorySpinner.selectedItem.toString())
            return true
        } else if (item.itemId === R.id.Previous) {
            moveCategories(-1)
            return true
        } else if (item.itemId === R.id.Next) {
            moveCategories(1)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = "View Budgets"

        var categorySpinner = binding.budgetCategorySpinner
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getCombinedCategoriesForSpinner())
        categorySpinner.adapter = arrayAdapter
        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged()
        }
        binding.budgetCategorySpinner.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))

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

        if (SpenderViewModel.getCount() == 1) {
            binding.rowBudgetWhoHeading.visibility = View.GONE
        }
    }

    fun loadRows(iCategory: String) {
        Log.d("Alex", "in load rows, iCategory is '" + iCategory + "'")
        val adapter = BudgetAdapter(requireContext(), BudgetViewModel.getBudgetInputRows(iCategory, currentFilter))
        val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
        listView.setAdapter(adapter)

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {
                var itemValue = listView.getItemAtPosition(position) as BudgetInputRow
                var bmDateApplicable = BudgetMonth(itemValue.dateApplicable)
                var bmDateStart = BudgetMonth(itemValue.dateStarted)
                if (itemValue.dateApplicable == itemValue.dateStarted ||
                        (bmDateApplicable.month == 1 && bmDateStart.month == 0)) {// ie only allow edits on "real" entries
                    var bmDateApplicable = BudgetMonth(itemValue.dateApplicable)
                    var bmDateStarted = BudgetMonth(itemValue.dateStarted)
                    var monthToSend: Int = bmDateApplicable.month
                    if (bmDateStarted.month == 0)
                        monthToSend = 0
                    var amountToSend: Double = 0.0
                    if (monthToSend == 0)
                        amountToSend = itemValue.amount.toDouble() * 12
                    else
                        amountToSend = itemValue.amount.toDouble()
                    var rtdf = BudgetDialogFragment.newInstance(
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
                            val adapter = BudgetAdapter(
                                requireContext(),
                                BudgetViewModel.getBudgetInputRows(iCategory, currentFilter)
                            )
                            listView.setAdapter(adapter)
                            adapter.notifyDataSetChanged()
                        }
                    })
                    rtdf.show(getParentFragmentManager(), "Edit Budget")
                } else {
                    Toast.makeText(requireActivity(), "Only actual budget entries (in bold) are clickable.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun moveCategories(iDirection: Int) {
        val currentCategoryPosition = binding.budgetCategorySpinner.selectedItemPosition
        var newCategoryPosition: Int = currentCategoryPosition
        if (iDirection == -1) {
            if (currentCategoryPosition > 0) {
                newCategoryPosition = currentCategoryPosition - 1
            } else {
                newCategoryPosition = 0
            }
        } else { // has to be +1
            if (currentCategoryPosition < binding.budgetCategorySpinner.adapter.count-1) {
                newCategoryPosition = currentCategoryPosition + 1
            } else {
                newCategoryPosition = binding.budgetCategorySpinner.adapter.count-1
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