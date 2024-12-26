package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.TransactionViewModel.Companion.AnnualTripCategoryTotal
import com.isrbet.budgetsbyisrbet.databinding.FragmentTripTrackerBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*
import kotlin.random.Random

private const val cHEADER = 0
private const val cDETAIL = 1
private const val cTOTAL = 2

private const val cSortByText = 0
private const val cSortByDate = 1

class TripTableRow(iContext: Context): TableRow(iContext) {
    var rowType = cDETAIL
    var categoryID: Int = -1
    var categoryName: String = ""
    var tripCategory: TripExpenseType = TripExpenseType.UNKNOWN
}

class TripTrackerFragment : Fragment() {
    private var _binding: FragmentTripTrackerBinding? = null
    private val binding get() = _binding!!
    private var myRows: MutableList<TripTrackerData> = arrayListOf()
    private var mTableLayout: TableLayout? = null
    private var sortByMethod = cSortByText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripTrackerBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_trip_tracker, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)

        mTableLayout = binding.tableRows
        binding.tableRows.isStretchAllColumns = true
        if (SpenderViewModel.singleUser())
            binding.filterWhoLayout.visibility = View.GONE

        when (DefaultsViewModel.getDefaultFilterWhoTrip()) {
            0 -> binding.name1RadioButton.isChecked = true
            1 -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultRoundTrip()) {
            true -> {
                binding.switchRoundToNearestDollar.isChecked = true
            }
            false -> {
                binding.switchRoundToNearestDollar.isChecked = false
            }
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)
        
        addTripCategories()

        binding.tripCategorySpinner.onItemSelectedListener =
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
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_CATEGORY_TRIP, position)
                    Timber.tag("Alex").d("Setting spinner to " + TripExpenseType.getText(TripExpenseType.fromInt(position)))
                    loadRows(false)
                    setTitle()
                }
            }

        binding.sortByText.setOnClickListener {
            sortByMethod = cSortByText
            loadRows(false)
        }

        binding.sortByDate.setOnClickListener {
            sortByMethod = cSortByDate
            loadRows(false)
        }
        binding.collapseAll.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_CATEGORY_TRIP, TripExpenseType.TOTAL.ordinal)
            loadRows(false)
            setTitle()
        }
        binding.expandAll.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_CATEGORY_TRIP, TripExpenseType.ALL.ordinal)
            loadRows(false)
            setTitle()
        }
        binding.buttonSettings.setOnClickListener {
            onExpandClicked(binding.optionsLinearLayout)
        }
        binding.switchRoundToNearestDollar.setOnCheckedChangeListener { _, _ ->
            if (binding.switchRoundToNearestDollar.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_TRIP, true)
                loadRows(false)
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_TRIP, false)
                loadRows(false)
            }
        }

        binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.name1RadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRIP, 0)
                    setTitle()
                    loadRows(true)
                    // do something when radio button 1 is selected
                }
                R.id.name2RadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRIP, 1)
                    setTitle()
                    loadRows(true)
                }
                R.id.whoAllRadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRIP, 2)
                    setTitle()
                    loadRows(true)
                }
            }
        }
        binding.optionsLinearLayout.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeBottom() {
                super.onSwipeBottom()
                binding.optionsLinearLayout.visibility = View.GONE
                binding.settingsLinearLayout.visibility = View.VISIBLE
            }
        })
        loadRows(true)
        setTitle()
    }

    private fun loadRows(iRefreshRows: Boolean = false) = runBlocking {
        launch {
            var grandTotal = 0.0
            var grandTotalNumOfDays = 0
            val viewRows = DefaultsViewModel.getDefaultFilterCategoryTrip()
            if (iRefreshRows) {
                val TripRows = TripTrackerRows()
                val defWho = DefaultsViewModel.getDefaultFilterWhoTrip()
                myRows = TripRows.getRows(defWho)
            }
            if (sortByMethod == cSortByText) {
                myRows.sortWith(compareBy({ it.category.lowercase() }, { it.subcategory.lowercase() },
                    { it.tripCategory }))

            } else {
                myRows.sortWith(compareBy({ CategoryViewModel.getCategory(it.categoryID)?.tripStartDate.toString() },
                    { CategoryViewModel.getCategory(it.categoryID)?.fullCategoryName() },
                    { it.tripCategory}))
            }
            binding.tableHeader.removeAllViews()
            mTableLayout!!.removeAllViews()

            // do header row
            createViewRow(cHEADER, -1, TripExpenseType.ALL, 0, TripExpenseType.ALL, 0.0, 0, 0.0)

            var i = 0
            Timber.tag("Alex").d("myRows has ${myRows.size} rows")
            for (row in myRows) {
                val catID = CategoryViewModel.getID(row.category, row.subcategory)

//                private fun createViewRow(iRowType: Int, iRowNo: Int, defaultsView: Int,
  //                                        iCategoryID: Int, iCategory: String, iTripCategory: Int,
    //                                      iTotalAmount: Double, iNumOfDays: Int, iAvgAmount: Double) {

                createViewRow(cDETAIL, i, viewRows,
                    catID, row.tripCategory,
                    row.totalAmount, row.numberOfDays, row.avgAmount)
//                if (row.tripCategory == TripExpenseType.ALL ||
  //                  (viewRows != TripExpenseType.ALL && row.tripCategory == viewRows)) {
                if (viewRows == row.tripCategory ||
                    (viewRows == TripExpenseType.TOTAL && row.tripCategory == TripExpenseType.ALL)) {
                    grandTotal += row.totalAmount
                    grandTotalNumOfDays += row.numberOfDays
                }
            }

            createViewRow(
                cTOTAL,
                i++,
                viewRows,
                0,
                TripExpenseType.UNKNOWN,
                grandTotal,
                grandTotalNumOfDays,
                grandTotal/grandTotalNumOfDays
            )
            // add spacer row.  For some reason the bottom row rightmost cell doesn't display without this
            val tempTableHeaderRow = binding.tableHeader.getChildAt(0) as TableRow
            val tr = TripTableRow(requireContext())
            for (c in 0 until tempTableHeaderRow.childCount)
                tr.addView(TextView(requireContext()))
            binding.tableRows.addView(tr)

            val run = Runnable {
                val tableHeaderRow = binding.tableHeader.getChildAt(0) as TableRow
                var firstVisibleRow = -1
                for (ind in 0 until binding.tableRows.childCount) {
                    val tRow = binding.tableRows.getChildAt(ind) as TripTableRow
                    if (tRow.visibility == View.VISIBLE) {
                        firstVisibleRow = ind
                        break
                    }
                }
/*
                if (firstVisibleRow > -1) {
                    val tableRow = binding.tableRows.getChildAt(firstVisibleRow) as TripTableRow
                    for (ind in 0 until 2) { // don't do it for amount columns, since they're already weighted
                        if (tableHeaderRow.getChildAt(ind).visibility == View.VISIBLE) {
                            if (tableHeaderRow.getChildAt(ind).measuredWidth > tableRow.getChildAt(ind).measuredWidth) {
                                tableRow.getChildAt(ind).layoutParams = TableRow.LayoutParams(
                                    tableHeaderRow.getChildAt(ind).measuredWidth,
                                    tableRow.getChildAt(ind).measuredHeight)
                            } else {
                                tableHeaderRow.getChildAt(ind).layoutParams = TableRow.LayoutParams(
                                    tableRow.getChildAt(ind).measuredWidth,
                                    tableHeaderRow.getChildAt(ind).measuredHeight)
                                (tableHeaderRow.getChildAt(ind) as TextView).width = tableRow.getChildAt(ind).measuredWidth
                            }
                        }
                    }
                }*/
            }
            binding.tableRows.post(run)
        }
    }

    private fun createViewRow(iRowType: Int, iRowNo: Int, defaultsView: TripExpenseType,
                              iCategoryID: Int, iTripCategory: TripExpenseType,
                              iTotalAmount: Double, iNumOfDays: Int, iAvgAmount: Double) {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0

        val tv1 = TextView(requireContext())
        tv1.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        tv1.visibility = View.GONE
        tv1.text = iCategoryID.toString()

        val tv2 = TextView(requireContext())
        tv2.layoutParams = TableRow.LayoutParams(
            0, TableRow.LayoutParams.WRAP_CONTENT, 7F)
        tv2.gravity = if (iRowType == cDETAIL) (Gravity.START or Gravity.CENTER_VERTICAL)
        else (Gravity.END or Gravity.CENTER_VERTICAL)
        tv2.setPadding(15, 0, 0, 5)
        if (iRowType == cDETAIL) {
            val cat = CategoryViewModel.getCategory(iCategoryID)
            if (cat != null && iTripCategory == TripExpenseType.ALL) {
                val textCol = MaterialColors.getColor(requireContext(), R.attr.textOnBackground, Color.BLACK)
                val sCat = cat.fullCategoryName().setFontSizeForPath(cat.fullCategoryName().length, 44, textCol)
                val dates = String.format("\n(%s to %s)", cat.tripStartDate, cat.tripFinishDate)
                val sSubtitle = dates.setFontSizeForPath(dates.length, 30,
                    MaterialColors.getColor(
                        requireContext(),
                        R.attr.colorPrimary,
                        Color.BLACK
                    ))
                sCat.setSpan(StyleSpan(Typeface.BOLD), 0, sCat.length, 0)
                sSubtitle.setSpan(StyleSpan(Typeface.NORMAL), 0, sSubtitle.length, 0)
                tv2.text = TextUtils.concat(sCat, sSubtitle)
            } else if (cat != null && defaultsView != TripExpenseType.ALL) {
                tv2.text = String.format("%s", cat.fullCategoryName())
            } else
                tv2.text = String.format("   %s", TripExpenseType.getText(iTripCategory))
        } else if (iRowType == cTOTAL)
            tv2.text = "Total"
        tv2.tag = iTripCategory.ordinal
        tv2.setOnClickListener {
            val myParent = (it as TextView).parent as TripTableRow
            val tCategory = TripExpenseType.fromInt(it.tag.toString().toInt())
            val cat = CategoryViewModel.getCategory(myParent.categoryID)
            // go to ViewAll with the SubCategory as the search term
            if (tCategory == TripExpenseType.ALL)
                MyApplication.transactionSearchText = "${cat?.categoryName} ${cat?.subcategoryName}"
            else
                MyApplication.transactionSearchText = "${cat?.categoryName} ${cat?.subcategoryName} ${TripExpenseType.getText(tCategory)}"
            view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
        }

        val tv3 = TextView(requireContext())
        tv3.layoutParams = TableRow.LayoutParams(
            0, TableRow.LayoutParams.WRAP_CONTENT, 3F
        )
        tv3.setPadding(15, 0, 0, 5)
        if (iRowType == cHEADER) {
            tv3.text = "Total"
            tv3.gravity = (Gravity.END or Gravity.BOTTOM)
        } else {
            tv3.text = gDecWithCurrency(iTotalAmount,DefaultsViewModel.getDefaultRoundTrip())
            tv3.gravity = (Gravity.END or Gravity.CENTER_VERTICAL)
        }
        tv3.tag = iTripCategory.ordinal
        tv3.setOnClickListener {
            val myParent = (it as TextView).parent as TripTableRow
            val tCategory = TripExpenseType.fromInt(it.tag.toString().toInt())
            val cat = CategoryViewModel.getCategory(myParent.categoryID)
            // go to ViewAll with the SubCategory as the search term
            if (tCategory == TripExpenseType.ALL)
                MyApplication.transactionSearchText = "${cat?.categoryName} ${cat?.subcategoryName}"
            else
                MyApplication.transactionSearchText = "${cat?.categoryName} ${cat?.subcategoryName} ${TripExpenseType.getText(tCategory)}"
            view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
        }

        val tv4 = TextView(requireContext())
        tv4.layoutParams = TableRow.LayoutParams(
            0, TableRow.LayoutParams.WRAP_CONTENT, 2F
        )
        tv4.setPadding(15, 0, 0, 5)
        if (iRowType == cHEADER) {
            tv4.text = "# of Days"
            tv4.gravity = (Gravity.END or Gravity.BOTTOM)
        } else {
            tv4.text = iNumOfDays.toString()
            tv4.gravity = (Gravity.END or Gravity.CENTER_VERTICAL)
        }

        val tv5 = TextView(requireContext())
        tv5.layoutParams = TableRow.LayoutParams(
            0, TableRow.LayoutParams.WRAP_CONTENT, 3F
        )
        tv5.setPadding(15, 0, 0, 5)
        if (iRowType == cHEADER) {
            tv5.text = "Avg"
            tv5.gravity = (Gravity.END or Gravity.BOTTOM)
        } else {
            tv5.text = gDecWithCurrency(iAvgAmount,DefaultsViewModel.getDefaultRoundTrip())
            tv5.gravity = (Gravity.END or Gravity.CENTER_VERTICAL)
        }

        // add table row
        val tr = TripTableRow(requireContext())
        tr.rowType = iRowType
        tr.categoryID = iCategoryID
        tr.tripCategory = iTripCategory

        tr.id = iRowNo + 1
        tr.addView(tv1)
        tr.addView(tv2)
        tr.addView(tv3)
        tr.addView(tv4)
        tr.addView(tv5)

        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        tr.setPadding(5, 0, 5, 0)
        tr.layoutParams = trParams
        if (iRowType == cDETAIL && iTripCategory == TripExpenseType.ALL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
        }
        else if (iRowType == cHEADER) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
        }
        else if (iRowType == cTOTAL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            val hexColor = MaterialColors.getColor(
                requireContext(),
                R.attr.colorOnBackground,
                Color.BLACK
            )
            tr.setBackgroundResource(R.drawable.row_frame)
            tr.background.colorFilter =
                BlendModeColorFilter(hexColor, BlendMode.SRC_ATOP)
        }

        if (iRowType == cDETAIL) {
            if (defaultsView == iTripCategory || defaultsView == TripExpenseType.ALL ||
                (defaultsView == TripExpenseType.TOTAL && iTripCategory == TripExpenseType.ALL)) {
                tr.visibility = View.VISIBLE
            } else {
                tr.visibility = View.GONE
            }
        } else
            tr.visibility = View.VISIBLE

        if (iRowType == cHEADER) {
            binding.tableHeader.addView(tr)
        } else
            mTableLayout!!.addView(tr)
    }

    private fun refreshRows(iCategory: String, iVisibility: Int) {
        var firstDetailLine: Int
        var lastDetailLine = 0
        mTableLayout = binding.tableRows
        var tableRow: TripTableRow?
        do {
            lastDetailLine += 1
            tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TripTableRow
            val catIDTV = tableRow.getChildAt(0) as TextView
            val catID = catIDTV.text.toString().toInt()
            val cat = CategoryViewModel.getCategory(catID)?.categoryName
        } while (tableRow != null)
        if (tableRow == null) // no detail rows found
            return
        // found sub-total row, now work backwards
        lastDetailLine -= 1
        tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TripTableRow
        if (tableRow.rowType != cDETAIL)  // ie no details for this category
            return
        firstDetailLine = lastDetailLine
        do {
            firstDetailLine -= 1
            tableRow = mTableLayout!!.getChildAt(firstDetailLine) as TripTableRow
        } while (tableRow != null && tableRow.rowType == cDETAIL)
        firstDetailLine += 1
        // now check if section should be expanded or collapsed
        for (i in firstDetailLine..lastDetailLine) {
            tableRow = mTableLayout!!.getChildAt(i) as TripTableRow
/*            if (iVisibility == View.VISIBLE) {
                if (defView == TripView.ALL) {
                    tableRow.visibility = View.VISIBLE
                } else if (tableRow.TripView == defView){
                    tableRow.visibility = View.VISIBLE
                }
            } else */
                tableRow.visibility = iVisibility
        }
    }

    private fun addTripCategories() {
        val tripCategoryList: MutableList<String> = ArrayList()
        for (i in 0 until TripExpenseType.UNKNOWN.ordinal+1) {
            tripCategoryList.add(i, TripExpenseType.getText(TripExpenseType.fromInt(i)))
        }

        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tripCategoryList
        )
        binding.tripCategorySpinner.adapter = arrayAdapter
        binding.tripCategorySpinner.setSelection(DefaultsViewModel.getDefaultFilterCategoryTrip().ordinal)
    }

    private fun onExpandClicked(layout: LinearLayout) {
        if (layout.visibility == View.GONE) { // ie expand the section
            // first hide all other possible expansions
            resetLayout(binding.settingsLinearLayout)
            resetLayout(binding.optionsLinearLayout)
            layout.visibility = View.VISIBLE
        } else { // ie retract the section
            resetLayout(layout)
        }
    }

    private fun setTitle() {
        var title = "Trip Tracker"

        var currentFilterIndicator = if (DefaultsViewModel.getDefaultFilterCategoryTrip() == TripExpenseType.ALL) {
            SpenderViewModel.getSpenderName(DefaultsViewModel.getDefaultFilterWhoTrip())
        } else {
            String.format(
                "%s-%s",
                SpenderViewModel.getSpenderName(DefaultsViewModel.getDefaultFilterWhoTrip()),
                TripExpenseType.getText(DefaultsViewModel.getDefaultFilterCategoryTrip())
            )
        }
        val sep =  "\n"
        val subtitle = if (currentFilterIndicator == "")
            ""
        else {
            String.format("$sep($currentFilterIndicator)")
        }

        val textCol = MaterialColors.getColor(requireContext(), R.attr.textOnBackground, Color.BLACK)
        val sTitle = title.setFontSizeForPath(title.length, 65, textCol)
        val sSubtitle = subtitle.setFontSizeForPath(subtitle.length, 50,
            MaterialColors.getColor(
                requireContext(),
                R.attr.colorPrimary,
                Color.BLACK
            ))
        sTitle.setSpan(StyleSpan(Typeface.BOLD), 0, sTitle.length, 0)
        sSubtitle.setSpan(StyleSpan(Typeface.BOLD), 0, sSubtitle.length, 0)
        binding.title.text = TextUtils.concat(sTitle, sSubtitle)
    }

    private fun resetLayout(layout: LinearLayout) {
        layout.visibility = View.GONE
    }
}


data class TripTrackerData(
    var categoryID: Int = 0,
    var category: String = "",
    var subcategory: String = "",
    var tripCategory: TripExpenseType = TripExpenseType.ALL,
    var totalAmount: Double = 0.0,
    var numberOfDays: Int = 1,
    var avgAmount: Double = 0.0)

class TripTrackerRows {
    fun getRows(
        iBoughtForFlag: Int = 2,
    ): MutableList<TripTrackerData> {
        val data: MutableList<TripTrackerData> = mutableListOf()
        val categories = CategoryViewModel.getCategories(true)
        categories.forEach {
            if (it.tripTracker) {
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.ALL, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.ACCOMMODATION, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.CASH, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.ENTERTAINMENT, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.FOOD, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.PACKAGE_TOUR, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.TRANSPORTATION, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.OTHER, 0.0, it.getNumberOfTripDays(), 0.0))
                data.add(TripTrackerData(it.id, it.categoryName, it.subcategoryName, TripExpenseType.UNKNOWN, 0.0, it.getNumberOfTripDays(), 0.0))
            }
        }  // at this point we have a complete list of tripTracker categories

        // add cat totals
        val actualTotals = TransactionViewModel.getAnnualTripCategoryActuals(iBoughtForFlag)

        var i = 1
        for (actualTotal in actualTotals) {
            i += 1
            val row = data.find { it.categoryID == actualTotal.catID &&
                    it.tripCategory == actualTotal.tripCategoryID }
            if (row != null) {
                row.totalAmount = actualTotal.value
                val cat = CategoryViewModel.getCategory(actualTotal.catID)
                if (cat != null)
                    row.numberOfDays = cat.getNumberOfTripDays()
                row.avgAmount = actualTotal.value / row.numberOfDays
                Timber.tag("Alex").d("Added data to row $i ${cat?.subcategoryName} ${actualTotal.value}")
            }
            else
                Timber.tag("Alex").d("Did not add data to row $i ${actualTotal.catID} ${actualTotal.value}")

            val rowAll = data.find { it.categoryID == actualTotal.catID &&
                    it.tripCategory == TripExpenseType.ALL }
            if (rowAll != null) {
                rowAll.totalAmount += actualTotal.value
                rowAll.avgAmount = rowAll.totalAmount / rowAll.numberOfDays
            }
        }

        for (i in data.size - 1 downTo 0) {
            if (data[i].tripCategory == TripExpenseType.UNKNOWN && data[i].totalAmount == 0.0) {
                data.removeAt(i)
            }
        }
        return data
    }
}