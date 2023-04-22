package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.isrbet.budgetsbyisrbet.MyApplication.Companion.transactionSearchText
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionViewAllBinding
import com.l4digital.fastscroll.FastScrollRecyclerView
import timber.log.Timber
import java.util.*

class PreviousFilters : ViewModel() {
    var prevCategoryFilter = ""
    var prevSubcategoryFilter = ""
    var prevDiscretionaryFilter = ""
    var prevPaidbyFilter = -1
    var prevBoughtForFilter = -1
    var prevTypeFilter = ""
    var prevRTKeyFilter = ""
    var dateRangeFilter: Pair<String, String> = Pair("", "")
}

enum class TransactionSortOrder {
    DATE_ASCENDING,
    DATE_DESCENDING,
    AMOUNT_ASCENDING,
    AMOUNT_DESCENDING,
    CATEGORY_ASCENDING,
    CATEGORY_DESCENDING,
    WHO_ASCENDING,
    WHO_DESCENDING,
    NOTE_ASCENDING,
    NOTE_DESCENDING,
    TYPE_ASCENDING,
    TYPE_DESCENDING;
}

enum class SortOrderDirection {
    ASCENDING,
    DESCENDING
}

const val cACCOUNTING_FILTER = "Accounting"
const val cSCHEDULED_PAYMENT_FILTER = "Scheduled Payment"

class TransactionViewAllFragment : Fragment() {
    private var _binding: FragmentTransactionViewAllBinding? = null
    private val binding get() = _binding!!
    private val args: TransactionViewAllFragmentArgs by navArgs()
    private val filters: PreviousFilters by viewModels()
    private var filterMode = ""
    private var currentSortOrder = TransactionSortOrder.DATE_ASCENDING

    private fun inAccountingMode(): Boolean {
        return filterMode == cACCOUNTING_FILTER
    }
    private fun inScheduledPaymentMode(): Boolean {
        return filterMode == cSCHEDULED_PAYMENT_FILTER
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterMode = args.filterMode
        filters.prevRTKeyFilter = args.rtKey
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                transactionSearchText = ""
                isEnabled =
                    false  // without this line there will be a recursive call to OnBackPressed
//                activity?.onBackPressed()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        })
//        val inflater = TransitionInflater.from(requireContext())
//        enterTransition = inflater.inflateTransition(R.transition.slide_right)
//        returnTransition = null
//        exitTransition = inflater.inflateTransition(R.transition.slide_left)
/*        val transactionListObserver = Observer<MutableList<Transaction>> { newValue ->
            Log.d("Alex", "list has changed!!")
            setupRecycler()
        }
        TransactionViewModel.observeList(this, transactionListObserver) */
    }

    private fun setupRecycler() {
        val recyclerView: FastScrollRecyclerView = binding.transactionViewAllRecyclerView
//            requireActivity().findViewById(R.id.transaction_view_all_recycler_view)
        recyclerView.apply {
            // set a LinearLayoutManager to handle Android RecyclerView behavior
            val linearLayoutManager =
                object : LinearLayoutManager(requireContext(), VERTICAL, false) {
                    override fun onLayoutCompleted(state: RecyclerView.State?) {
                        super.onLayoutCompleted(state)
                        val adapter: TransactionRecyclerAdapter =
                            recyclerView.adapter as TransactionRecyclerAdapter
                        binding.totalAmount.text = gDecWithCurrency(adapter.currentTotal)
                    }
                }
            recyclerView.layoutManager = linearLayoutManager
            // fyi I have done a time check on how long it takes to copy the list.  It took 0ms to copy a list of 7000.
            // so this is definitely not a perf issue
            val expList = TransactionViewModel.getCopyOfTransactions()
//            expList.sortBy { it.date }
            if (expList.size == 0) {
                binding.noInformationText.visibility = View.VISIBLE
                binding.noInformationText.text = getString(R.string.you_have_not_yet_entered_any_transactions)
            } else {
                binding.noInformationText.visibility = View.GONE
            }


            // this nifty line passes a lambda (simple function) to the adapter which is called each time the row is clicked.
            recyclerView.adapter =
                TransactionRecyclerAdapter(requireContext(), expList, filters, currentSortOrder) { item ->
                    MyApplication.transactionFirstInList =
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (item.type == cTRANSACTION_TYPE_TRANSFER) {
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
            binding.totalAmount.text = gDecWithCurrency((recyclerView.adapter as TransactionRecyclerAdapter).currentTotal)
        }
        goToCorrectRow()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionViewAllBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transaction_view_all, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        if (inScheduledPaymentMode())
            currentSortOrder = TransactionSortOrder.NOTE_ASCENDING
        setupRecycler()
        val recyclerView: FastScrollRecyclerView = binding.transactionViewAllRecyclerView
//            requireActivity().findViewById(R.id.transaction_view_all_recycler_view)
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        loadCategoryRadioButtons()
        if (SpenderViewModel.singleUser()) {
            binding.showIndividualAmountsColumns.visibility = View.GONE
            binding.showWhoColumn.visibility = View.GONE
            binding.showRunningTotalColumn.visibility = View.GONE
        }

        binding.transactionAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransactionFragment)
        }
        binding.selectDateRange.setOnClickListener {
            selectDateRangeFilter()
        }
        binding.transactionSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val ladapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                ladapter.filter.filter(newText)
                if (newText != "") {
                    binding.totalLayout.visibility = View.VISIBLE
                    transactionSearchText = newText.toString()
                    binding.transactionSearch.visibility = View.VISIBLE
                }
                transactionSearchText = newText.toString()
                return true
            }
        })

        binding.categorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selection = parent?.getItemAtPosition(position).toString()
                    addSubCategories(selection, filters.prevSubcategoryFilter)
                    val categoryFilter = if (selection == getString(R.string.all)) "" else selection
                    binding.totalLayout.visibility = View.VISIBLE
                    if (categoryFilter != filters.prevCategoryFilter) {
                        val ladapter: TransactionRecyclerAdapter =
                            recyclerView.adapter as TransactionRecyclerAdapter
                        ladapter.setCategoryFilter(categoryFilter)
                        ladapter.filterTheList(transactionSearchText)
                        ladapter.notifyDataSetChanged()
                        filters.prevCategoryFilter = categoryFilter
                        goToCorrectRow()
                        setFilterTitle()
                    }
                }
            }
        binding.subcategorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selection = parent?.getItemAtPosition(position).toString()
                    val subcategoryFilter = if (selection == getString(R.string.all))
                        ""
                    else
                        selection
                    binding.totalLayout.visibility = View.VISIBLE
                    if (subcategoryFilter != filters.prevSubcategoryFilter) {
                        val ladapter: TransactionRecyclerAdapter =
                            recyclerView.adapter as TransactionRecyclerAdapter
                        ladapter.setSubcategoryFilter(subcategoryFilter)
                        ladapter.filterTheList(transactionSearchText)
                        ladapter.notifyDataSetChanged()
                        filters.prevSubcategoryFilter = subcategoryFilter
                        goToCorrectRow()
                        setFilterTitle()
                    }
                }
            }
        binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val discretionaryFilter: String = when (radioButton.text.toString()) {
                getString(R.string.disc) -> cDiscTypeDiscretionary
                getString(R.string.non_disc) -> cDiscTypeNondiscretionary
                else -> ""
            }
            binding.totalLayout.visibility = View.VISIBLE
            if (discretionaryFilter != filters.prevDiscretionaryFilter) {
                val ladapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                ladapter.setDiscretionaryFilter(discretionaryFilter)
                ladapter.filterTheList(transactionSearchText)
                ladapter.notifyDataSetChanged()
                filters.prevDiscretionaryFilter = discretionaryFilter
                goToCorrectRow()
                setFilterTitle()
            }
        }
        binding.filterPaidByRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val paidbyFilter = if (radioButton.text.toString() == getString(R.string.all))
                -1
            else
                SpenderViewModel.getSpenderIndex(radioButton.text.toString())
            binding.totalLayout.visibility = View.VISIBLE
            if (paidbyFilter != filters.prevPaidbyFilter) {
                val ladapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                ladapter.setPaidByFilter(paidbyFilter)
                ladapter.filterTheList(transactionSearchText)
                ladapter.notifyDataSetChanged()
                filters.prevPaidbyFilter = paidbyFilter
                goToCorrectRow()
                setFilterTitle()
            }
        }
        binding.filterBoughtForRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val boughtforFilter = if (radioButton.text.toString() == getString(R.string.all))
                -1
            else
                SpenderViewModel.getSpenderIndex(radioButton.text.toString())
            if (boughtforFilter != filters.prevBoughtForFilter) {
                binding.totalLayout.visibility = View.VISIBLE
                val ladapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                ladapter.setBoughtForFilter(boughtforFilter)
                ladapter.filterTheList(transactionSearchText)
                ladapter.notifyDataSetChanged()
                filters.prevBoughtForFilter = boughtforFilter
                goToCorrectRow()
                setFilterTitle()
            }
        }
        binding.filterTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = requireActivity().findViewById(checkedId) as RadioButton
            val typeFilter = when (radioButton.text.toString()) {
                getString(R.string.expense) -> cTRANSACTION_TYPE_EXPENSE
                getString(R.string.credit) -> cTRANSACTION_TYPE_CREDIT
                getString(R.string.scheduled) -> cTRANSACTION_TYPE_SCHEDULED
                getString(R.string.transfer) -> cTRANSACTION_TYPE_TRANSFER
                else -> ""
            }
            if (typeFilter != filters.prevTypeFilter) {
                binding.totalLayout.visibility = View.VISIBLE
                val ladapter: TransactionRecyclerAdapter =
                    recyclerView.adapter as TransactionRecyclerAdapter
                ladapter.setTypeFilter(typeFilter)
                ladapter.filterTheList(transactionSearchText)
                ladapter.notifyDataSetChanged()
                filters.prevTypeFilter = typeFilter
                goToCorrectRow()
                setFilterTitle()
            }
        }
        binding.buttonYearForward.setOnClickListener {
            val getNewPosition = adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                cNEXT_YEAR
            )
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                getNewPosition,
                0
            )
        }

        binding.buttonMonthForward.setOnClickListener {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    cNEXT_MONTH
                ), 0
            )
        }

        binding.buttonMonthBackward.setOnClickListener {

            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    cPREV_MONTH
                ), 0
            )
        }

        binding.buttonYearBackward.setOnClickListener {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    cPREV_YEAR
                ), 0
            )
        }

        binding.buttonSettings.setOnClickListener {
            if (binding.expandedLabelLayout.visibility == View.GONE &&
                    binding.expandedViewColumnLayout.visibility == View.GONE &&
                    binding.expandedFilterLayout.visibility == View.GONE) {
                binding.expandedLabelLayout.visibility = View.VISIBLE
                binding.expandedFilterLayout.visibility = View.VISIBLE
                val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.menuColor, Color.BLACK), cOpacity)
                binding.expandedFilterLayout.setBackgroundColor(Color.parseColor(hexColor))
                binding.expandedFilterLayout.setBackgroundResource(R.drawable.rounded_top_corners)
                val hexColor2 = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK), cOpacity)
                binding.expandedViewColumnLabel.setBackgroundColor(Color.parseColor(hexColor2))
                binding.expandedViewColumnLayout.visibility = View.GONE
                binding.navButtonLinearLayout.visibility = View.GONE
            } else {
                binding.expandedLabelLayout.visibility = View.GONE
                binding.expandedViewColumnLayout.visibility = View.GONE
                binding.expandedFilterLayout.visibility = View.GONE
                binding.navButtonLinearLayout.visibility = View.VISIBLE
            }
        }

        binding.expandedViewColumnLabel.setOnClickListener {
            if (binding.expandedViewColumnLayout.visibility == View.GONE) {
                showExpandedViewColumnArea()
            }
        }
        binding.expandedFilterLabel.setOnClickListener {
            if (binding.expandedFilterLayout.visibility == View.GONE) {
                showExpandedFilterArea()
            }
        }
        binding.columnHeadingLayout.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            //        view?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    adapter.getPositionOf(
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                        cNEXT_MONTH
                    ), 0
                )
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    adapter.getPositionOf(
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                        cPREV_MONTH
                    ), 0
                )
            }
        })

        binding.expandedViewColumnLayout.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeBottom() {
                super.onSwipeBottom()
                binding.expandedLabelLayout.visibility = View.GONE
                binding.expandedViewColumnLayout.visibility = View.GONE
                binding.navButtonLinearLayout.visibility = View.VISIBLE
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                showExpandedFilterArea()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                showExpandedFilterArea()
            }
        })
        binding.expandedFilterLayout.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeBottom() {
                super.onSwipeBottom()
                binding.expandedLabelLayout.visibility = View.GONE
                binding.expandedFilterLayout.visibility = View.GONE
                binding.navButtonLinearLayout.visibility = View.VISIBLE
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                showExpandedViewColumnArea()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                showExpandedViewColumnArea()
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
        binding.searchButton.setOnClickListener {
            if (binding.transactionSearch.visibility == View.GONE) {
                binding.expandedLabelLayout.visibility = View.GONE
                binding.expandedViewColumnLayout.visibility = View.GONE
                binding.expandedFilterLayout.visibility = View.GONE
                binding.transactionSearch.visibility = View.VISIBLE
                binding.navButtonLinearLayout.visibility = View.VISIBLE
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
        if (inAccountingMode()) {
            setViewsToAccounting()
            runFilters()
            binding.percentage1Heading.text = SpenderViewModel.getSpenderName(0)
            binding.percentage2Heading.text = SpenderViewModel.getSpenderName(1)
            binding.runningTotalHeading.tooltipText = String.format(
                getString(R.string.the_amount_that_x_owes_y), SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1)
            )
            if (SpenderViewModel.getSpenderName(0)
                    .substring(0, 1) == SpenderViewModel.getSpenderName(1).substring(0, 1)
            )
                binding.runningTotalHeading.text = "1->2"
            else
                binding.runningTotalHeading.text = String.format(
                    getString(R.string.n_arrow_n),
                    SpenderViewModel.getSpenderName(0).substring(0, 1),
                    SpenderViewModel.getSpenderName(1).substring(0, 1)
                )
        } else if (inScheduledPaymentMode()) {
            setViewsToScheduledPayments()
            runFilters()
        } else {
            setViewsToDefault()
            if (args.categoryID != "")
                setCategoryFilter(args.categoryID.toInt())
        }
        binding.showIndividualAmountsColumns.setOnCheckedChangeListener { _, _ ->
            if (binding.showIndividualAmountsColumns.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL, true)
                binding.percentage1Heading.visibility = View.VISIBLE
                binding.percentage2Heading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_INDIVIDUAL_AMOUNTS_IN_VIEW_ALL, false)
                binding.percentage1Heading.visibility = View.GONE
                binding.percentage2Heading.visibility = View.GONE
            }
            updateView()
        }
        binding.showCategoryColumns.setOnCheckedChangeListener { _, _ ->
            if (binding.showCategoryColumns.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL, true)
                binding.categoryHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_CATEGORY_IN_VIEW_ALL, false)
                binding.categoryHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showWhoColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showWhoColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_WHO_IN_VIEW_ALL, true)
                binding.whoHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_WHO_IN_VIEW_ALL, false)
                binding.whoHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showNoteColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showNoteColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_NOTE_VIEW_ALL, true)
                binding.noteHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_NOTE_VIEW_ALL, false)
                binding.noteHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showDiscColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showDiscColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_DISC_IN_VIEW_ALL, true)
                binding.discHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_DISC_IN_VIEW_ALL, false)
                binding.discHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showTypeColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showTypeColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL, true)
                binding.typeHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_TYPE_IN_VIEW_ALL, false)
                binding.typeHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.showRunningTotalColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showRunningTotalColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL, true)
                binding.runningTotalHeading.visibility = View.VISIBLE
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_RUNNING_TOTAL_IN_VIEW_ALL, false)
                binding.runningTotalHeading.visibility = View.GONE
            }
            updateView()
        }
        binding.resetFilterButton.setOnClickListener {
            reset()
        }
        binding.filterOffButton.setOnClickListener {
            reset()
        }

        binding.dateHeading.setOnClickListener {
            when (currentSortOrder) {
                TransactionSortOrder.DATE_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.DATE_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.DATE_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
                TransactionSortOrder.AMOUNT_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.AMOUNT_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.AMOUNT_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.AMOUNT_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
                TransactionSortOrder.CATEGORY_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.CATEGORY_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.CATEGORY_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.CATEGORY_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
                TransactionSortOrder.WHO_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.WHO_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.WHO_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.WHO_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
                TransactionSortOrder.NOTE_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.NOTE_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.NOTE_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.NOTE_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
                TransactionSortOrder.TYPE_ASCENDING -> {
                    currentSortOrder = TransactionSortOrder.TYPE_DESCENDING
                    adjustColumnHeadings(SortOrderDirection.DESCENDING)
                }
                TransactionSortOrder.TYPE_DESCENDING -> {
                    currentSortOrder = TransactionSortOrder.TYPE_ASCENDING
                    adjustColumnHeadings(SortOrderDirection.ASCENDING)
                }
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        binding.amountHeading.setOnClickListener {
            if (currentSortOrder == TransactionSortOrder.AMOUNT_ASCENDING ||
                    currentSortOrder == TransactionSortOrder.AMOUNT_DESCENDING) {
                currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            } else {
                currentSortOrder = TransactionSortOrder.AMOUNT_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        binding.categoryHeading.setOnClickListener {
            if (currentSortOrder == TransactionSortOrder.CATEGORY_ASCENDING ||
                currentSortOrder == TransactionSortOrder.CATEGORY_DESCENDING) {
                currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            } else {
                currentSortOrder = TransactionSortOrder.CATEGORY_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        binding.whoHeading.setOnClickListener {
            if (currentSortOrder == TransactionSortOrder.WHO_ASCENDING ||
                currentSortOrder == TransactionSortOrder.WHO_DESCENDING) {
                currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            } else {
                currentSortOrder = TransactionSortOrder.WHO_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        binding.noteHeading.setOnClickListener {
            if (currentSortOrder == TransactionSortOrder.NOTE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.NOTE_DESCENDING) {
                currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            } else {
                currentSortOrder = TransactionSortOrder.NOTE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        binding.typeHeading.setOnClickListener {
            if (currentSortOrder == TransactionSortOrder.TYPE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.TYPE_DESCENDING) {
                currentSortOrder = TransactionSortOrder.DATE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            } else {
                currentSortOrder = TransactionSortOrder.TYPE_ASCENDING
                adjustColumnHeadings(SortOrderDirection.ASCENDING)
            }
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).sortBy(currentSortOrder)
            (binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter).notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        if (currentSortOrder == TransactionSortOrder.DATE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.AMOUNT_ASCENDING ||
            currentSortOrder == TransactionSortOrder.CATEGORY_ASCENDING ||
            currentSortOrder == TransactionSortOrder.TYPE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.NOTE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.WHO_ASCENDING)
            adjustColumnHeadings(SortOrderDirection.ASCENDING)
        else
            adjustColumnHeadings(SortOrderDirection.DESCENDING)
        //      adapter.filterTheList(transactionSearchText)
    //    adapter.notifyDataSetChanged()
      //  goToCorrectRow()
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.transaction_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
        HintViewModel.showHint(parentFragmentManager, cHINT_TRANSACTION_VIEW_ALL)
        setFilterTitle()
    }

    private fun reset() {
        resetFilters()
        filterMode = ""
        binding.expandedViewColumnLayout.visibility = View.GONE
        binding.transactionSearch.visibility = View.GONE
        binding.navButtonLinearLayout.visibility = View.VISIBLE
        binding.expandedFilterLayout.visibility = View.GONE
        binding.expandedLabelLayout.visibility = View.GONE
        binding.totalLayout.visibility = View.GONE
        binding.filterLayout.visibility = View.GONE

        val recyclerView: FastScrollRecyclerView = binding.transactionViewAllRecyclerView
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        adapter.setFilter(filterMode)
        adapter.filterTheList(transactionSearchText)
        adapter.notifyDataSetChanged()
        setViewsToDefault()
        goToCorrectRow()
    }
    private fun setFilterTitle() {
        binding.filterText.text = ""
        if (inAccountingMode()) {
            binding.filterLayout.visibility = View.VISIBLE
            binding.filterText.text = getString(R.string.accounting_filter_is_on)
        } else if (inScheduledPaymentMode()) {
            binding.filterLayout.visibility = View.VISIBLE
            binding.filterText.text = String.format(getString(R.string.scheduled_payment_filter_is_on), filters.prevRTKeyFilter)
        } else {
            var tempString = ""
            if (filters.prevCategoryFilter != "") {
                tempString += " ${filters.prevCategoryFilter}"
            }
            if (filters.prevSubcategoryFilter != "") {
                tempString += " ${filters.prevSubcategoryFilter}"
            }
            if (filters.prevDiscretionaryFilter != "") {
                tempString += " ${filters.prevDiscretionaryFilter}"
            }
            if (filters.prevPaidbyFilter != -1) {
                tempString += " ${SpenderViewModel.getSpenderName(filters.prevPaidbyFilter)}"
            }
            if (filters.prevBoughtForFilter != -1) {
                tempString += " ${SpenderViewModel.getSpenderName(filters.prevBoughtForFilter)}"
            }
            if (filters.prevTypeFilter != "") {
                tempString += " ${filters.prevTypeFilter}"
            }
            if (filters.dateRangeFilter.first != "") {
                tempString += " ${filters.dateRangeFilter.first}:${filters.dateRangeFilter.second}"
            }

            if (tempString == "") {
                binding.filterLayout.visibility = View.GONE
                binding.filterText.text = ""
            } else {
                tempString = "${getString(R.string.filtered_by)} $tempString"
                binding.filterLayout.visibility = View.VISIBLE
                binding.filterText.text = tempString
            }
        }
    }
    private fun showExpandedViewColumnArea() {
        binding.expandedViewColumnLayout.visibility = View.VISIBLE
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.menuColor, Color.BLACK), cOpacity)
        binding.expandedViewColumnLabel.setBackgroundColor(Color.parseColor(hexColor))
        binding.expandedViewColumnLabel.setBackgroundResource(R.drawable.rounded_top_corners)
        val hexColor2 = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK), cOpacity)
        binding.expandedFilterLabel.setBackgroundColor(Color.parseColor(hexColor2))
        binding.expandedFilterLayout.visibility = View.GONE
    }
    private fun showExpandedFilterArea() {
        binding.expandedFilterLayout.visibility = View.VISIBLE
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.menuColor, Color.BLACK), cOpacity)
        binding.expandedFilterLabel.setBackgroundColor(Color.parseColor(hexColor))
        binding.expandedFilterLabel.setBackgroundResource(R.drawable.rounded_top_corners)
        val hexColor2 = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK), cOpacity)
        binding.expandedViewColumnLabel.setBackgroundColor(Color.parseColor(hexColor2))
        binding.expandedViewColumnLayout.visibility = View.GONE
    }
    private fun adjustColumnHeadings(iSortOrderDirection: SortOrderDirection) {
        when (iSortOrderDirection) {
            SortOrderDirection.ASCENDING -> {
                binding.dateHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_less_24), null)
            }
            SortOrderDirection.DESCENDING -> {
                binding.dateHeading.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_expand_more_24), null)
            }
        }
        binding.dateHeading.text = getString(R.string.date)
        binding.amountHeading.text = getString(R.string.amt)
        binding.categoryHeading.text = getString(R.string.category)
        binding.whoHeading.text = getString(R.string.who)
        binding.noteHeading.text = getString(R.string.where)
        binding.typeHeading.text = getString(R.string.type)

        when (currentSortOrder) {
            TransactionSortOrder.CATEGORY_ASCENDING, TransactionSortOrder.CATEGORY_DESCENDING -> {
                binding.dateHeading.text = getString(R.string.category)
                binding.categoryHeading.text = getString(R.string.date)
            }
            TransactionSortOrder.WHO_ASCENDING, TransactionSortOrder.WHO_DESCENDING -> {
                binding.dateHeading.text = getString(R.string.who)
                binding.whoHeading.text = getString(R.string.date)
            }
            TransactionSortOrder.NOTE_ASCENDING, TransactionSortOrder.NOTE_DESCENDING -> {
                binding.dateHeading.text = getString(R.string.where)
                binding.noteHeading.text = getString(R.string.date)
            }
            TransactionSortOrder.TYPE_ASCENDING, TransactionSortOrder.TYPE_DESCENDING -> {
                binding.dateHeading.text = getString(R.string.type)
                binding.typeHeading.text = getString(R.string.date)
            }
            TransactionSortOrder.AMOUNT_ASCENDING, TransactionSortOrder.AMOUNT_DESCENDING -> {
                binding.dateHeading.text = getString(R.string.amount)
                binding.amountHeading.text = getString(R.string.date)
            }
            else -> {}
        }
    }

    private fun setViewsToDefault() {
        binding.showCategoryColumns.isChecked = DefaultsViewModel.getDefaultShowCategoryInViewAll()
        if (!DefaultsViewModel.getDefaultShowCategoryInViewAll()) {
            binding.categoryHeading.visibility = View.GONE
        }
        binding.showIndividualAmountsColumns.isChecked = DefaultsViewModel.getDefaultShowIndividualAmountsInViewAll()
        if (!DefaultsViewModel.getDefaultShowIndividualAmountsInViewAll()) {
            binding.percentage1Heading.visibility = View.GONE
            binding.percentage2Heading.visibility = View.GONE
        }
        binding.showWhoColumn.isChecked = DefaultsViewModel.getDefaultShowWhoInViewAll()
        if (!DefaultsViewModel.getDefaultShowWhoInViewAll()) {
            binding.whoHeading.visibility = View.GONE
        }
        binding.showNoteColumn.isChecked = DefaultsViewModel.getDefaultShowNoteInViewAll()
        if (!DefaultsViewModel.getDefaultShowNoteInViewAll()) {
            binding.noteHeading.visibility = View.GONE
        }
        binding.showDiscColumn.isChecked = DefaultsViewModel.getDefaultShowDiscInViewAll()
        if (!DefaultsViewModel.getDefaultShowDiscInViewAll()) {
            binding.discHeading.visibility = View.GONE
        }
        binding.showTypeColumn.isChecked = DefaultsViewModel.getDefaultShowTypeInViewAll()
        if (!DefaultsViewModel.getDefaultShowTypeInViewAll()) {
            binding.typeHeading.visibility = View.GONE
        }
        binding.showRunningTotalColumn.isChecked = DefaultsViewModel.getDefaultShowRunningTotalInViewAll()
        if (!DefaultsViewModel.getDefaultShowRunningTotalInViewAll()) {
            binding.runningTotalHeading.visibility = View.GONE
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun runFilters() {
        resetFilters()
        val adapter: TransactionRecyclerAdapter = binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
        adapter.setFilter(filterMode)
        adapter.filterTheList(transactionSearchText)
        adapter.notifyDataSetChanged()
        goToCorrectRow()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setCategoryFilter(iCategoryID: Int) {
        val adapter: TransactionRecyclerAdapter = binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
        adapter.setCategoryIDFilter(iCategoryID)
        adapter.filterTheList(transactionSearchText)
        adapter.notifyDataSetChanged()
        goToCorrectRow()
    }
    private fun setViewsToAccounting() {
        binding.showIndividualAmountsColumns.isChecked = true
        binding.percentage1Heading.visibility = View.VISIBLE
        binding.percentage2Heading.visibility = View.VISIBLE

        binding.showRunningTotalColumn.isChecked = true
        binding.runningTotalHeading.visibility = View.VISIBLE

        binding.showWhoColumn.isChecked = true
        binding.whoHeading.visibility = View.VISIBLE

        binding.showCategoryColumns.isChecked = false
        binding.categoryHeading.visibility = View.GONE

        binding.showNoteColumn.isChecked = false
        binding.noteHeading.visibility = View.GONE

        binding.showDiscColumn.isChecked = false
        binding.discHeading.visibility = View.GONE

        binding.showTypeColumn.isChecked = true
        binding.typeHeading.visibility = View.VISIBLE
        updateView()
    }

    private fun setViewsToScheduledPayments() {
        binding.showIndividualAmountsColumns.isChecked = false
        binding.percentage1Heading.visibility = View.GONE
        binding.percentage2Heading.visibility = View.GONE

        binding.showRunningTotalColumn.isChecked = false
        binding.runningTotalHeading.visibility = View.GONE

        binding.showWhoColumn.isChecked = true
        binding.whoHeading.visibility = View.VISIBLE

        binding.showCategoryColumns.isChecked = true
        binding.categoryHeading.visibility = View.VISIBLE

        binding.showNoteColumn.isChecked = true
        binding.noteHeading.visibility = View.VISIBLE

        binding.showDiscColumn.isChecked = false
        binding.discHeading.visibility = View.GONE

        binding.showTypeColumn.isChecked = false
        binding.typeHeading.visibility = View.GONE
        updateView()
    }

    private fun resetFilters() {
        val adapter: TransactionRecyclerAdapter =
            binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
        adapter.setCategoryIDFilter(0)
        binding.categorySpinner.setSelection(0)
        adapter.setCategoryFilter("")
        binding.subcategorySpinner.setSelection(0)
        adapter.setSubcategoryFilter("")
        binding.allDiscRadioButton.isChecked = true
        adapter.setDiscretionaryFilter("")
        binding.allPaidByRadioButton.isChecked = true
        adapter.setPaidByFilter(-1)
        binding.allBoughtForRadioButton.isChecked = true
        adapter.setBoughtForFilter(-1)
        binding.allTypeRadioButton.isChecked = true
        adapter.setTypeFilter("")
        adapter.setDateRangeFilter(Pair("",""))
        setFilterTitle()
    }

    private fun closeSearch() {
        if (binding.transactionSearch.visibility == View.VISIBLE) {
            val adapter: TransactionRecyclerAdapter =
                binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
            binding.transactionSearch.visibility = View.GONE
            adapter.filter.filter("")
            // clear filter
            val searchView = binding.transactionSearch
            searchView.setQuery("", false)
            searchView.clearFocus()
            binding.totalLayout.visibility = View.GONE
            MyApplication.transactionFirstInList = cLAST_ROW
            goToCorrectRow()
        }
    }
    private fun updateView() {
        val adapter: TransactionRecyclerAdapter = binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
        binding.transactionViewAllRecyclerView.adapter = null
        binding.transactionViewAllRecyclerView.adapter = adapter
        goToCorrectRow()
    }

    private fun goToCorrectRow() {
        val recyclerView: FastScrollRecyclerView = requireActivity().findViewById(R.id.transaction_view_all_recycler_view)
        val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        if (MyApplication.transactionFirstInList == cLAST_ROW)
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

/*    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                val recyclerView: RecyclerView = requireActivity().findViewById(R.id.transaction_view_all_recycler_view)
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
            // for some reason "back" doesn't work from this fragment.  This forces it to work.
            requireActivity().onBackPressed()
        }
        return true
    } */

    private fun loadCategoryRadioButtons() {
        val radioGroup = requireActivity().findViewById<RadioGroup>(R.id.categoryRadioGroup)
        radioGroup?.removeAllViews()

        val categoryNames = CategoryViewModel.getCategoryNames()
        categoryNames.add(0,getString(R.string.all))
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)

        binding.categorySpinner.adapter = arrayAdapter
        if (filters.prevCategoryFilter == "")
            binding.categorySpinner.setSelection(0)
        else {
            // set to saved category filter
        }
        addSubCategories(getString(R.string.all), filters.prevSubcategoryFilter)
    }
    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        val subcategoryList = CategoryViewModel.getSubcategoriesForSpinner(iCategory)
        subcategoryList.add(0, getString(R.string.all))
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subcategoryList)
        binding.subcategorySpinner.adapter = arrayAdapter
        if (iSubCategory == "")
            binding.subcategorySpinner.setSelection(0)
        else {
            binding.subcategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
//        transactionSearchText = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireContext(), requireView())
        _binding = null
    }
    private fun selectDateRangeFilter() {
        val datePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates")
                .build()
        datePicker.show(parentFragmentManager, "Alex")
        datePicker.addOnPositiveButtonClickListener {
            val dateformatYYYYMMDD = SimpleDateFormat("yyyy-MM-dd")
            dateformatYYYYMMDD.timeZone = TimeZone.getTimeZone("UTC")
            var startDate: StringBuilder? = StringBuilder(dateformatYYYYMMDD.format(it.first))
            var endDate: StringBuilder? = StringBuilder(dateformatYYYYMMDD.format(it.second))
            filters.dateRangeFilter = Pair(startDate.toString(), endDate.toString())

            binding.totalLayout.visibility = View.VISIBLE
            val ladapter: TransactionRecyclerAdapter =
                binding.transactionViewAllRecyclerView.adapter as TransactionRecyclerAdapter
            ladapter.setDateRangeFilter(filters.dateRangeFilter)
            ladapter.filterTheList(transactionSearchText)
            ladapter.notifyDataSetChanged()
            goToCorrectRow()
            setFilterTitle()

            binding.expandedLabelLayout.visibility = View.GONE
            binding.expandedViewColumnLayout.visibility = View.GONE
            binding.expandedFilterLayout.visibility = View.GONE
            binding.navButtonLinearLayout.visibility = View.VISIBLE
        }
    }
}