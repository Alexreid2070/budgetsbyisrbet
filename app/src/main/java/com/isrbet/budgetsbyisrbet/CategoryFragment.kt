package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryBinding
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter

class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_category, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategories(true))

        val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
        listView.adapter = adapter
        if (SpenderViewModel.twoDistinctUsers())
            binding.privacyHeading.visibility = View.VISIBLE
        else
            binding.privacyHeading.visibility = View.GONE


        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Category
                val cdf = CategoryEditDialogFragment.newInstance(itemValue.id.toString(),
                    itemValue.categoryName, itemValue.subcategoryName, itemValue.discType,
                    if (itemValue.private != 2) "true" else "false",
                    itemValue.state)
                cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategories(true))
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

        binding.categoryFab.setMenuListener(object : SimpleMenuListenerAdapter() {
            override fun onMenuItemSelected(menuItem: MenuItem?): Boolean {
                return when (menuItem?.itemId) {
                    R.id.action_color -> {
                        findNavController().navigate(R.id.CategoryDetailsFragment)
                        true
                    }
                    R.id.action_add -> {
                        addCategory()
                        true
                    }
                    else -> false
                }
            }
        })
        if (SpenderViewModel.twoDistinctUsers())
            binding.privacyHeading.visibility = View.VISIBLE
        else
            binding.privacyHeading.visibility = View.GONE
        HintViewModel.showHint(parentFragmentManager, "Category")
    }

    private fun addCategory() {
        val cdf = CategoryEditDialogFragment.newInstance("0", "", "",
            cDiscTypeDiscretionary, "false", cON)
        cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategories(true))
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
