package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryBinding

class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_category, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())

        val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Category
                val cdf = CategoryEditDialogFragment.newInstance(itemValue.categoryName, itemValue.subcategoryName, itemValue.discType) // what do I pass here? zzz
                cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
                        listView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, "Edit Category")
            }
        binding.expandSettings.setOnClickListener {
            Log.d("Alex", "clicked on Settings")
            findNavController().navigate(R.id.SettingsFragment)
        }
        binding.expandBudgets.setOnClickListener {
            findNavController().navigate(R.id.BudgetViewAllFragment)
        }
        binding.expandRecurringTransactions.setOnClickListener {
            findNavController().navigate(R.id.RecurringTransactionFragment)
        }
        binding.categoryAddFab.setOnClickListener {
            addCategory()
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.category_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = menu.getItem(i).itemId == R.id.AddCategory
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.AddCategory) {
            addCategory()
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun addCategory() {
        val cdf = CategoryEditDialogFragment.newInstance("", "",cDiscTypeOff)
        cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
                val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        })
        cdf.show(parentFragmentManager, "Add Category")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
