package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryBinding
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter
import timber.log.Timber

class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryListObserver = Observer<MutableList<Category>> {
            Timber.tag("Alex").d("observer fired for categories")
            refreshAdapter()
        }
        CategoryViewModel.observeList(this, categoryListObserver)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_category, container, false)
        return binding.root
    }

    private fun refreshAdapter() : CategoryAdapter{
        val adapter = CategoryAdapter(requireContext(), CategoryViewModel.getCategories(true)) { catID ->
            val cdf = CategoryEditDialogFragment.newInstance(catID)
            cdf.show(parentFragmentManager, getString(R.string.edit_category))
        }
        binding.categoryListView.adapter = adapter
        adapter.notifyDataSetChanged()
        return adapter
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        refreshAdapter()
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
        HintViewModel.showHint(parentFragmentManager, cHINT_CATEGORY)
    }

    private fun addCategory() {
        val cdf = CategoryEditDialogFragment.newInstance(0)
        cdf.show(parentFragmentManager, getString(R.string.add_category))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
