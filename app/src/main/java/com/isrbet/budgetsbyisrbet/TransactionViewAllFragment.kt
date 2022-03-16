package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionViewAllBinding
import com.isrbet.budgetsbyisrbet.MyApplication.Companion.transactionSearchText
import com.l4digital.fastscroll.FastScrollRecyclerView
import java.text.DecimalFormat

class PreviousFilters : ViewModel() {
    var prevCategoryFilter = ""
    var prevSubcategoryFilter = ""
    var prevDiscretionaryFilter = ""
    var prevPaidbyFilter = ""
    var prevBoughtForFilter = ""
    var prevTypeFilter = ""
}

class TransactionViewAllFragment : Fragment() {
    private var _binding: FragmentTransactionViewAllBinding? = null
    private val binding get() = _binding!!
    private val args: TransactionViewAllFragmentArgs by navArgs()
    private val filters: PreviousFilters by viewModels()
    private var accountingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountingMode = args.accountingFlag == "Accounting"
        Log.d("Alex", "accounting mode is $accountingMode")
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("Alex", "onBackPressed")
                transactionSearchText = ""
                isEnabled =
                    false  // without this line there will be a recursive call to OnBackPressed
                activity?.onBackPressed()
            }
        })
//        val inflater = TransitionInflater.from(requireContext())
//        enterTransition = inflater.inflateTransition(R.transition.slide_right)
//        returnTransition = null
//        exitTransition = inflater.inflateTransition(R.transition.slide_left)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionViewAllBinding.inflate(inflater, container, false)
        Log.d("Alex", "onCreateView")
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transaction_view_all, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        Log.d("Alex", "onViewCreated")
        val recyclerView: FastScrollRecyclerView = requireActivity().findViewById(R.id.recycler_view)
        recyclerView.apply {
            // set a LinearLayoutManager to handle Android RecyclerView behavior
            val linearLayoutManager = object : LinearLayoutManager(requireContext(), VERTICAL, false) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)
                    val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
                    val dec = DecimalFormat("#.00")
                    binding.totalAmount.text = dec.format(adapter.currentTotal)
                }
            }
            recyclerView.layoutManager = linearLayoutManager
            val expList = ExpenditureViewModel.getCopyOfExpenditures()
            expList.sortBy { it.date }

            // this nifty line passes a lambda (simple function) to the adapter which is called each time the row is clicked.
            recyclerView.adapter = TransactionRecyclerAdapter(requireContext(), expList, filters) { item ->
                Log.d("Alex", "I clicked item " + item.mykey)
                MyApplication.transactionFirstInList =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                if (item.type == "Transfer") {
                    val action =
                        TransactionViewAllFragmentDirections.actionTransactionViewAllFragmentToTransferFragment()
                            .setTransactionID(item.mykey)
                    this@TransactionViewAllFragment.findNavController().navigate(action)
                } else {
                    val action =
                        TransactionViewAllFragmentDirections.actionTransactionViewAllFragmentToTransactionFragment()
                            .setTransactionID(item.mykey)
                    this@TransactionViewAllFragment.findNavController().navigate(action)
                }
            }
        }
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter

        loadCategoryRadioButtons()
        Log.d("Alex", "onViewCreated after loadCategory")

        binding.transactionAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransactionFragment)
        }
        binding.transactionSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
                adapter.filter.filter(newText)
                if (newText != "") {
                    Log.d("Alex", "setting newText '" + newText +"'")
                    binding.totalLayout.visibility = View.VISIBLE
                    transactionSearchText = newText.toString()
                    binding.transactionSearch.visibility = View.VISIBLE
                }
                return true
            }
        })

        binding.categorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("Alex", "category onItemSelected")
                val selection = parent?.getItemAtPosition(position).toString()
                addSubCategories(selection, filters.prevSubcategoryFilter)
                val categoryFilter = if (selection == "All") "" else selection
                binding.totalLayout.visibility = View.VISIBLE
                if (categoryFilter != filters.prevCategoryFilter) {
                    val adapter: TransactionRecyclerAdapter =
                        recyclerView.adapter as TransactionRecyclerAdapter
                    adapter.setCategoryFilter(categoryFilter)
                    adapter.filterTheList(transactionSearchText)
                    adapter.notifyDataSetChanged()
                    filters.prevCategoryFilter = categoryFilter
                    goToCorrectRow()
                    Log.d("Alex", "onItemSelected Category")
                }
            }
        }
        binding.subcategorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position).toString()
                val subcategoryFilter = if (selection == "All")
                    ""
                else
                    selection
                binding.totalLayout.visibility = View.VISIBLE
                if (subcategoryFilter != filters.prevSubcategoryFilter) {
                    val adapter: TransactionRecyclerAdapter =
                        recyclerView.adapter as TransactionRecyclerAdapter
                    adapter.setSubcategoryFilter(subcategoryFilter)
                    adapter.filterTheList(transactionSearchText)
                    adapter.notifyDataSetChanged()
                    filters.prevSubcategoryFilter = subcategoryFilter
                    goToCorrectRow()
                    Log.d("Alex", "onItemSelected SubCategory")
                }
            }
        }
        binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val discretionaryFilter: String
            when (radioButton.text.toString()) {
                "Disc" -> discretionaryFilter = cDiscTypeDiscretionary
                "Non-Disc" -> discretionaryFilter = cDiscTypeNondiscretionary
                "Off" -> discretionaryFilter = cDiscTypeOff
                else -> discretionaryFilter = ""
            }
            binding.totalLayout.visibility = View.VISIBLE
            if (discretionaryFilter != filters.prevDiscretionaryFilter) {
                val adapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                adapter.setDiscretionaryFilter(discretionaryFilter)
                adapter.filterTheList(transactionSearchText)
                adapter.notifyDataSetChanged()
                filters.prevDiscretionaryFilter = discretionaryFilter
                goToCorrectRow()
            }
        }
        binding.filterPaidByRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val paidbyFilter = if (radioButton.text.toString() == "All")
                ""
            else
                radioButton.text.toString()
            binding.totalLayout.visibility = View.VISIBLE
            if (paidbyFilter != filters.prevPaidbyFilter) {
                val adapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                adapter.setPaidByFilter(paidbyFilter)
                adapter.filterTheList(transactionSearchText)
                adapter.notifyDataSetChanged()
                filters.prevPaidbyFilter = paidbyFilter
                goToCorrectRow()
            }
        }
        binding.filterBoughtForRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val boughtforFilter = if (radioButton.text.toString() == "All")
                ""
            else
                radioButton.text.toString()
            if (boughtforFilter != filters.prevBoughtForFilter) {
                binding.totalLayout.visibility = View.VISIBLE
                val adapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                adapter.setBoughtForFilter(boughtforFilter)
                adapter.filterTheList(transactionSearchText)
                adapter.notifyDataSetChanged()
                filters.prevBoughtForFilter = boughtforFilter
                goToCorrectRow()
            }
        }
        binding.filterTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val typeFilter = if (radioButton.text.toString() == "All")
                ""
            else
                radioButton.text.toString()
            if (typeFilter != filters.prevTypeFilter) {
                binding.totalLayout.visibility = View.VISIBLE
                val adapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                adapter.setTypeFilter(typeFilter)
                adapter.filterTheList(transactionSearchText)
                adapter.notifyDataSetChanged()
                filters.prevTypeFilter = typeFilter
                goToCorrectRow()
            }
        }
        // for some reason "binding.buttonToday.setOnClickListener doesn't work, but the following does
        binding.buttonYearForward.setOnClickListener { _: View ->
                val getNewPosition = adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    "+year"
                )
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    getNewPosition,
                    0
                )
            }

        binding.buttonMonthForward.setOnClickListener  { _: View ->
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    adapter.getPositionOf(
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                        "+month"
                    ), 0
                )
            }

        binding.buttonMonthBackward.setOnClickListener  { _: View ->
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    adapter.getPositionOf(
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                        "-month"
                    ), 0
                )
            }

        binding.buttonYearBackward.setOnClickListener  { _: View ->
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    adapter.getPositionOf(
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                        "-year"
                    ), 0
                )
            }

        binding.RecyclerLinearLayout.setOnTouchListener (object : OnSwipeTouchListener(requireContext()) {
                //        view?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    Log.d("Alex", "swiped left")
                    (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        adapter.getPositionOf(
                            (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                            "+month"
                        ), 0
                    )
                }

                override fun onSwipeRight() {
                    super.onSwipeRight()
                    Log.d("Alex", "swiped right")
                    (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        adapter.getPositionOf(
                            (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                            "-month"
                        ), 0
                    )
                }
            })

        if (transactionSearchText == "")
            binding.transactionSearch.visibility = View.GONE
        else {
            binding.transactionSearch.visibility = View.VISIBLE
            binding.transactionSearch.setQuery(transactionSearchText, false)
        }
        if (SpenderViewModel.singleUser()) {
            binding.whoHeading.visibility = View.GONE
        }
        binding.expandNav.setOnClickListener {
            onExpandClicked(binding.expandNav, binding.navButtonLinearLayout)
        }
        binding.expandView.setOnClickListener {
            onExpandClicked(binding.expandView, binding.viewButtonLinearLayout)
        }
        binding.expandFilter.setOnClickListener {
            onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
        }
        binding.search.setOnClickListener {
            if (binding.transactionSearch.visibility == View.GONE) {
                resetLayout(binding.expandNav, binding.navButtonLinearLayout)
                resetLayout(binding.expandView, binding.viewButtonLinearLayout)
                resetLayout(binding.expandFilter, binding.filterButtonLinearLayout)
                binding.transactionSearch.visibility = View.VISIBLE
                val searchView = binding.transactionSearch
                focusAndOpenSoftKeyboard(requireContext(), searchView)
            } else {
                closeSearch()
            }
        }
        if (SpenderViewModel.getActiveCount() > 1) {
            binding.name1BoughtForRadioButton.text = SpenderViewModel.getSpenderName(0)
            binding.name2BoughtForRadioButton.text = SpenderViewModel.getSpenderName(1)
            binding.name1PaidByRadioButton.text = SpenderViewModel.getSpenderName(0)
            binding.name2PaidByRadioButton.text = SpenderViewModel.getSpenderName(1)
        } else {
            binding.paidbyLinearLayout.visibility = View.GONE
            binding.boughtforLinearLayout.visibility = View.GONE
        }
        if (accountingMode) {
            setViewsToAccounting()
            setFiltersToAccounting()
        } else {
            binding.showCategoryColumns.isChecked = DefaultsViewModel.getDefault(
                cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL
            ) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL) != "true") {
                binding.categoryHeading.visibility = View.GONE
                binding.subCategoryHeading.visibility = View.GONE
            }
            binding.showIndividualAmountsColumns.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL) != "true") {
                binding.percentage1Heading.visibility = View.GONE
                binding.percentage2Heading.visibility = View.GONE
            }
            binding.showWhoColumn.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_WHO_IN_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_WHO_IN_VIEW_ALL) != "true") {
                binding.whoHeading.visibility = View.GONE
            }
            binding.showNoteColumn.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_NOTE_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_NOTE_VIEW_ALL) != "true") {
                binding.noteHeading.visibility = View.GONE
            }
            binding.showDiscColumn.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_DISC_IN_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_DISC_IN_VIEW_ALL) != "true") {
                binding.discHeading.visibility = View.GONE
            }
            binding.showTypeColumn.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL) != "true") {
                binding.typeHeading.visibility = View.GONE
            }
            binding.showRunningTotalColumn.isChecked =
                DefaultsViewModel.getDefault(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL) == "true"
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL) != "true") {
                binding.runningTotalHeading.visibility = View.GONE
            }
        }
        binding.showIndividualAmountsColumns.setOnCheckedChangeListener { _, _ ->
            if (binding.showIndividualAmountsColumns.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL, "true")
                binding.percentage1Heading.visibility = View.VISIBLE
                binding.percentage2Heading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL, "false")
                binding.percentage1Heading.visibility = View.GONE
                binding.percentage2Heading.visibility = View.GONE
            }
            updateView()
        }
        binding.showCategoryColumns.setOnCheckedChangeListener { _, _ ->
            if (binding.showCategoryColumns.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL, "true")
                binding.categoryHeading.visibility = View.VISIBLE
                binding.subCategoryHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL, "false")
                binding.categoryHeading.visibility = View.GONE
                binding.subCategoryHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showWhoColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showWhoColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_WHO_IN_VIEW_ALL, "true")
                binding.whoHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_WHO_IN_VIEW_ALL, "false")
                binding.whoHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showNoteColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showNoteColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_NOTE_VIEW_ALL, "true")
                binding.noteHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_NOTE_VIEW_ALL, "false")
                binding.noteHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showDiscColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showDiscColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_DISC_IN_VIEW_ALL, "true")
                binding.discHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_DISC_IN_VIEW_ALL, "false")
                binding.discHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showTypeColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showTypeColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL, "true")
                binding.typeHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL, "false")
                binding.typeHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showRunningTotalColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showRunningTotalColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL, "true")
                binding.runningTotalHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL, "false")
                binding.runningTotalHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.resetFilterButton.setOnClickListener {
            resetFilters()
            onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
            binding.totalLayout.visibility = View.GONE
            adapter.filterTheList(transactionSearchText)
            adapter.notifyDataSetChanged()
        }
        adapter.filterTheList(transactionSearchText)
        adapter.notifyDataSetChanged()
        goToCorrectRow()
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.transaction_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
    }

    private fun setFiltersToAccounting() {
        resetFilters()
        val recyclerView: FastScrollRecyclerView = requireActivity().findViewById(R.id.recycler_view)
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        adapter.setAccountingFilter(true)
        adapter.filterTheList(transactionSearchText)
        adapter.notifyDataSetChanged()
        goToCorrectRow()
    }

    private fun setViewsToAccounting() {
        binding.showIndividualAmountsColumns.isChecked = true
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL, "true")
        binding.percentage1Heading.visibility = View.VISIBLE
        binding.percentage2Heading.visibility = View.VISIBLE

        binding.showRunningTotalColumn.isChecked = true
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL, "true")
        binding.runningTotalHeading.visibility = View.VISIBLE

        binding.showWhoColumn.isChecked = true
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_WHO_IN_VIEW_ALL, "true")
        binding.whoHeading.visibility = View.VISIBLE

        binding.showCategoryColumns.isChecked = false
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL, "false")
        binding.categoryHeading.visibility = View.GONE
        binding.subCategoryHeading.visibility = View.GONE

        binding.showNoteColumn.isChecked = false
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_NOTE_VIEW_ALL, "false")
        binding.noteHeading.visibility = View.GONE

        binding.showDiscColumn.isChecked = false
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_DISC_IN_VIEW_ALL, "false")
        binding.discHeading.visibility = View.GONE

        binding.showTypeColumn.isChecked = true
//        DefaultsViewModel.updateDefault(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL, "false")
        binding.typeHeading.visibility = View.VISIBLE
        updateView()
    }

    private fun resetFilters() {
        val recyclerView: FastScrollRecyclerView =
            requireActivity().findViewById(R.id.recycler_view)
        val adapter: TransactionRecyclerAdapter =
            recyclerView.adapter as TransactionRecyclerAdapter
        binding.categorySpinner.setSelection(0)
        adapter.setCategoryFilter("")
        binding.subcategorySpinner.setSelection(0)
        adapter.setSubcategoryFilter("")
        binding.allDiscRadioButton.isChecked = true
        adapter.setDiscretionaryFilter("")
        binding.allPaidByRadioButton.isChecked = true
        adapter.setPaidByFilter("")
        binding.allBoughtForRadioButton.isChecked = true
        adapter.setBoughtForFilter("")
        binding.allTypeRadioButton.isChecked = true
        adapter.setTypeFilter("")
    }

    private fun closeSearch() {
        if (binding.transactionSearch.visibility == View.VISIBLE) {
            val recyclerView: FastScrollRecyclerView =
                requireActivity().findViewById(R.id.recycler_view)
            val adapter: TransactionRecyclerAdapter =
                recyclerView.adapter as TransactionRecyclerAdapter
            binding.transactionSearch.visibility = View.GONE
            adapter.filter.filter("")
            // clear filter
            val searchView = binding.transactionSearch
            searchView.setQuery("", false)
            searchView.clearFocus()
            binding.totalLayout.visibility = View.GONE
        }
    }
    private fun updateView() {
        val recyclerView: FastScrollRecyclerView = requireActivity().findViewById(R.id.recycler_view)
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        recyclerView.adapter = null
        recyclerView.adapter = adapter
        goToCorrectRow()
    }

    private fun goToCorrectRow() {
        val recyclerView: FastScrollRecyclerView = requireActivity().findViewById(R.id.recycler_view)
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        if (MyApplication.transactionFirstInList == 0)
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                adapter.getCount() - 1,
                0
            )
        else
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                MyApplication.transactionFirstInList,
                0
            )
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = menu.getItem(i).itemId == R.id.Search
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
/*        val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)

  */
        if (item.itemId == R.id.Search) {
            if (binding.transactionSearch.visibility == View.GONE) {
                binding.transactionSearch.visibility = View.VISIBLE
                val searchView = binding.transactionSearch
                focusAndOpenSoftKeyboard(requireContext(), searchView)
            } else {
                binding.transactionSearch.visibility = View.GONE
                val recyclerView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
                val adapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                adapter.filter.filter("")
                // clear filter
                val searchView = binding.transactionSearch
                searchView.setQuery("", false)
                searchView.clearFocus()
                binding.totalLayout.visibility = View.GONE
            }
        } else if (item.itemId == android.R.id.home) {
            Log.d("Alex", "i want to go home")
            // for some reason "back" doesn't work from this fragment.  This forces it to work.
            requireActivity().onBackPressed()
        } else
            Log.d("Alex", "what was this " + item.itemId)
        return true
    }

    private fun loadCategoryRadioButtons() {
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
        if (radioGroup != null)
            radioGroup.removeAllViews()

        val categoryNames = CategoryViewModel.getCategoryNames()
        categoryNames.add(0,"All")
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)

        binding.categorySpinner.adapter = arrayAdapter
        if (filters.prevCategoryFilter == "")
            binding.categorySpinner.setSelection(0)
        else {
            // set to saved category filter
        }
        addSubCategories("All", filters.prevSubcategoryFilter)
    }
    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        Log.d("Alex", "addSubCategories 1")
        val subcategoryList = CategoryViewModel.getSubcategoriesForSpinner(iCategory)
        subcategoryList.add(0,"All")
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subcategoryList)
        binding.subcategorySpinner.adapter = arrayAdapter
        if (iSubCategory == "")
            binding.subcategorySpinner.setSelection(0)
        else {
            binding.subcategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        }
        arrayAdapter.notifyDataSetChanged()
    }

    private fun onExpandClicked(button: TextView, layout: LinearLayout) {
        if (layout.visibility == View.GONE) { // ie expand the section
            // first hide all other possible expansions
//            closeSearch()
            resetLayout(binding.expandNav, binding.navButtonLinearLayout)
            resetLayout(binding.expandView, binding.viewButtonLinearLayout)
            resetLayout(binding.expandFilter, binding.filterButtonLinearLayout)
            button.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null, null, null)
            button.textSize = 16F
            button.setBackgroundResource(R.drawable.rounded_top_corners)
            layout.visibility = View.VISIBLE
        } else { // ie retract the section
            resetLayout(button, layout)
        }
    }
    private fun resetLayout(button: TextView, layout: LinearLayout) {
        button.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null, null, null)
        button.textSize = 14F
        button.setBackgroundResource(android.R.color.transparent)
        layout.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
        Log.d("Alex", "onPause resetting search text")
//        transactionSearchText = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireContext(), requireView())
        _binding = null
    }

}