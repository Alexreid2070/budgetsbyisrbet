package com.isrbet.budgetsbyisrbet

import SettingsEditCategoryDialogFragment
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsEditCategoryBinding

class SettingsCategoryFragment : Fragment() {
    private var _binding: FragmentSettingsEditCategoryBinding? = null
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
        _binding = FragmentSettingsEditCategoryBinding.inflate(inflater, container, false)
        return inflater.inflate(R.layout.fragment_settings_edit_category, container, false)
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = SettingsCategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())

        val listView: ListView = requireActivity().findViewById(R.id.category_list_view)
        listView.setAdapter(adapter)

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {

                // value of item that is clicked
                var itemValue = listView.getItemAtPosition(position) as Category
                var cdf = SettingsEditCategoryDialogFragment.newInstance(itemValue.categoryName, itemValue.subcategoryName, itemValue.discType) // what do I pass here? zzz
                cdf.setSettingsEditCategoryDialogFragmentListener(object: SettingsEditCategoryDialogFragment.SettingsEditCategoryDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val adapter = SettingsCategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
                        listView.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                    }
                })
                cdf.show(getParentFragmentManager(), "Edit Category")

            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.AddCategory)
                menu.getItem(i).setVisible(true)
            else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.AddCategory) {
            addCategory()
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    fun addCategory() {
        var cdf = SettingsEditCategoryDialogFragment.newInstance("", "","Off")
        cdf.setSettingsEditCategoryDialogFragmentListener(object: SettingsEditCategoryDialogFragment.SettingsEditCategoryDialogFragmentListener {
            override fun onNewDataSaved() {
                val adapter = SettingsCategoryAdapter(requireContext(), CategoryViewModel.getCategoriesIncludingOff())
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
