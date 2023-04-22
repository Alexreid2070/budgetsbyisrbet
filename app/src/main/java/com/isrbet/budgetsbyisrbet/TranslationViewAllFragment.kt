package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.isrbet.budgetsbyisrbet.databinding.FragmentTranslationViewAllBinding

enum class TranslationSortOrder(val code: Int) {
    BEFORE_ASCENDING(1),
    BEFORE_DESCENDING(2),
    AFTER_ASCENDING(3),
    AFTER_DESCENDING(4),
    CATEGORY_ASCENDING(5),
    CATEGORY_DESCENDING(6);
}

class TranslationViewAllFragment : Fragment() {
    private var _binding: FragmentTranslationViewAllBinding? = null
    private val binding get() = _binding!!
    private var currentSortOrder = TranslationSortOrder.BEFORE_ASCENDING

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentTranslationViewAllBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_translation_view_all, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val translationObserver = Observer<MutableList<Translation>> {
            val myAdapter = TranslationAdapter(requireContext(), TranslationViewModel.getTranslations(), currentSortOrder)
            binding.translationListView.adapter = myAdapter
            myAdapter.notifyDataSetChanged()
        }
        TranslationViewModel.observeList(this, translationObserver)
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = TranslationAdapter(requireContext(), TranslationViewModel.getTranslations(), currentSortOrder)

        binding.translationListView.adapter = adapter
        binding.translationListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = binding.translationListView.getItemAtPosition(position) as Translation
                val cdf = TranslationEditDialogFragment.newInstance(itemValue.before,
                    itemValue.after,
                    itemValue.category,
                    itemValue.key) // what do I pass here? zzz
                cdf.setTranslationEditDialogFragmentListener(object: TranslationEditDialogFragment.TranslationEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = TranslationAdapter(requireContext(), TranslationViewModel.getTranslations(), currentSortOrder)
                        binding.translationListView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_translation))
            }

        binding.beforeHeading.setOnClickListener {
            if (currentSortOrder == TranslationSortOrder.BEFORE_ASCENDING) {
                currentSortOrder = TranslationSortOrder.BEFORE_DESCENDING
                binding.beforeHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null)
            } else {
                currentSortOrder = TranslationSortOrder.BEFORE_ASCENDING
                binding.beforeHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null)
            }
            binding.afterHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            binding.categoryHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            (binding.translationListView.adapter as TranslationAdapter).sortBy(currentSortOrder)
            (binding.translationListView.adapter as TranslationAdapter).notifyDataSetChanged()
        }
        binding.afterHeading.setOnClickListener {
            if (currentSortOrder == TranslationSortOrder.AFTER_ASCENDING) {
                currentSortOrder = TranslationSortOrder.AFTER_DESCENDING
                binding.afterHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null)
            } else {
                currentSortOrder = TranslationSortOrder.AFTER_ASCENDING
                binding.afterHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null)
            }
            binding.beforeHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            binding.categoryHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            (binding.translationListView.adapter as TranslationAdapter).sortBy(currentSortOrder)
            (binding.translationListView.adapter as TranslationAdapter).notifyDataSetChanged()
        }
        binding.categoryHeading.setOnClickListener {
            if (currentSortOrder == TranslationSortOrder.CATEGORY_ASCENDING) {
                currentSortOrder = TranslationSortOrder.CATEGORY_DESCENDING
                binding.categoryHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null)
            } else {
                currentSortOrder = TranslationSortOrder.CATEGORY_ASCENDING
                binding.categoryHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null)
            }
            binding.beforeHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            binding.afterHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            (binding.translationListView.adapter as TranslationAdapter).sortBy(currentSortOrder)
            (binding.translationListView.adapter as TranslationAdapter).notifyDataSetChanged()
        }
        binding.searchButton.setOnClickListener {
            if (binding.translationSearch.visibility == View.GONE) {
                val searchView = binding.translationSearch
                binding.translationSearch.visibility = View.VISIBLE
                focusAndOpenSoftKeyboard(requireContext(), searchView)
            } else {
                closeSearch()
            }
        }
        binding.translationSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val ladapter: TranslationAdapter =
                    binding.translationListView.adapter as TranslationAdapter
                ladapter.filter.filter(newText)
                if (newText != "") {
                    binding.translationSearch.visibility = View.VISIBLE
                }
                return true
            }
        })
    }
    private fun closeSearch() {
        if (binding.translationSearch.visibility == View.VISIBLE) {
            val adapter: TranslationAdapter =
                binding.translationListView.adapter as TranslationAdapter
            binding.translationSearch.visibility = View.GONE
            adapter.filter.filter("")
            val searchView = binding.translationSearch
            searchView.setQuery("", false)
            searchView.clearFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
