package com.isrbet.budgetsbyisrbet

import CategoryEditDialogFragment
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryBinding

class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())

        val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
        listView.setAdapter(adapter)

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {

                // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Category
                val cdf = CategoryEditDialogFragment.newInstance(itemValue.categoryName, itemValue.subcategoryName, itemValue.discType) // what do I pass here? zzz
                cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
                        listView.setAdapter(myAdapter)
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(getParentFragmentManager(), "Edit Category")

            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() == R.id.AddCategory)
                menu.getItem(i).setVisible(true)
            else
                menu.getItem(i).setVisible(false)
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

    fun addCategory() {
        val cdf = CategoryEditDialogFragment.newInstance("", "","Off")
        cdf.setCategoryEditDialogFragmentListener(object: CategoryEditDialogFragment.CategoryEditDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
                val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
                listView.setAdapter(adapter)
                adapter.notifyDataSetChanged()
            }
        })
        cdf.show(getParentFragmentManager(), "Add Category")

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
