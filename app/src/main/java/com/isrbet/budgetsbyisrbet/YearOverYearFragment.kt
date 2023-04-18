package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentYearOverYearBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

private const val cDETAIL = 0
private const val cHEADER = 1
private const val cSUBTOTAL = 2
private const val cGRANDTOTAL = 3

enum class YoyView(val code: Int) {
    ALL(0),
    ACTUALS(1),
    AVERAGE(2),
    BUDGET(3),
    DELTA(4);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
}

class YOYTableRow(iContext: Context): TableRow(iContext) {
    var rowType = cDETAIL
    var yoyView: YoyView = YoyView.ACTUALS
    var isExpanded: Boolean = true
    var isIncludedInTotals: Boolean = true
    var categoryID: Int = -1
    var categoryName: String = ""
}

class YearOverYearFragment : Fragment() {
    private var _binding: FragmentYearOverYearBinding? = null
    private val binding get() = _binding!!
    private var myRows: MutableList<YearOverYearData> = arrayListOf()
    private var mTableLayout: TableLayout? = null
    private var currentlyCollapsed: MutableList<String> = ArrayList()
    private var currentlyExcluded: MutableList<Int> = mutableListOf()
    private var maxYearColumns = 3
    private val yearVisibility: MutableList<Int> = mutableListOf()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setYearVisibility()
        loadRows(false)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYearOverYearBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_year_over_year, container, false)
        return binding.root
    }

    private fun getLatestYear() : Int {
        return TransactionViewModel.getLatestYear() + 1 // this allows space for the average column
    }
    private fun setYearVisibility() {
        maxYearColumns = getAppropriateMaxColumns()
        for (i in 0 until yearVisibility.size)
            yearVisibility.removeAt(0)
        for (i in 0 until (getLatestYear() - TransactionViewModel.getEarliestYear() + 1)) {
            if (i+1 <= maxYearColumns)
                yearVisibility.add(0, View.VISIBLE)
            else
                yearVisibility.add(0, View.GONE)
        }
    }
    private fun getYearVisibility(iYear: Int): Int {
        val ind = iYear - TransactionViewModel.getEarliestYear()
        return yearVisibility[ind]
    }
    private fun getAppropriateMaxColumns() : Int {
        val orientation = resources.configuration.orientation
        val roundingOn = DefaultsViewModel.getDefaultRoundYOY()
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            6 + if (roundingOn) 1 else 0
        } else {
            // In portrait
            3 + if (roundingOn) 1 else 0
        }
    }
    private fun isExcluded(iCategoryID: Int) : Boolean {
        return currentlyExcluded.contains(iCategoryID)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)

        setYearVisibility()
        binding.dollarRadioButton.text = getLocalCurrencySymbol(true)
        mTableLayout = binding.tableRows
        binding.tableRows.isStretchAllColumns = true
        if (SpenderViewModel.singleUser())
            binding.filterWhoLayout.visibility = View.GONE

        when (DefaultsViewModel.getDefaultFilterDiscYOY()) {
            cDiscTypeDiscretionary -> binding.discRadioButton.isChecked = true
            cDiscTypeNondiscretionary -> binding.nonDiscRadioButton.isChecked = true
            else -> binding.allDiscRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultFilterWhoYOY()) {
            "0" -> binding.name1RadioButton.isChecked = true
            "1" -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultDeltaYOY()) {
            "#" -> binding.dollarRadioButton.isChecked = true
            "%" -> binding.percentageRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultRoundYOY()) {
            true -> {
                binding.switchRoundToNearestDollar.isChecked = true
            }
            false -> {
                binding.switchRoundToNearestDollar.isChecked = false
            }
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)

        binding.buttonBackward.setOnClickListener {
            moveBackward()
        }
        binding.buttonForward.setOnClickListener {
            moveForward()
        }
        binding.buttonViewAll.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_ROWS_YOY, YoyView.ALL.code)
            setActionBarTitle()
            loadRows(false)
        }
        binding.buttonViewActuals.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_ROWS_YOY, YoyView.ACTUALS.code)
            setActionBarTitle()
            loadRows(false)
        }
        binding.buttonViewBudgets.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_ROWS_YOY, YoyView.BUDGET.code)
            setActionBarTitle()
            loadRows(false)
        }
        binding.buttonViewAverage.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_ROWS_YOY, YoyView.AVERAGE.code)
            setActionBarTitle()
            loadRows(false)
        }
        binding.buttonViewDelta.setOnClickListener {
            DefaultsViewModel.updateDefaultInt(cDEFAULT_VIEW_ROWS_YOY, YoyView.DELTA.code)
            setActionBarTitle()
            loadRows(false)
        }
        binding.buttonSettings.setOnClickListener {
            onExpandClicked(binding.optionsLinearLayout)
        }
        binding.showDeltaRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.dollarRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_DELTA_YOY, "#")
                    loadRows(false)
                    // do something when radio button 1 is selected
                }
                R.id.percentageRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_DELTA_YOY, "%")
                    loadRows(false)
                }
            }
        }
        binding.switchRoundToNearestDollar.setOnCheckedChangeListener { _, _ ->
            if (binding.switchRoundToNearestDollar.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_YOY, true)
                maxYearColumns = getAppropriateMaxColumns()
                loadRows(false)
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_YOY, false)
                maxYearColumns = getAppropriateMaxColumns()
                loadRows(false)
            }
        }
        binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.discRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_YOY, cDiscTypeDiscretionary)
                    setActionBarTitle()
                    loadRows(true)
                    // do something when radio button 1 is selected
                }
                R.id.nonDiscRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_YOY, cDiscTypeNondiscretionary)
                    setActionBarTitle()
                    loadRows(true)
                }
                R.id.allDiscRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_YOY, "")
                    setActionBarTitle()
                    loadRows(true)
                }
            }
        }
        binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.name1RadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_YOY, "0")
                    setActionBarTitle()
                    loadRows(true)
                    // do something when radio button 1 is selected
                }
                R.id.name2RadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_YOY, "1")
                    setActionBarTitle()
                    loadRows(true)
                }
                R.id.whoAllRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_YOY, "")
                    setActionBarTitle()
                    loadRows(true)
                }
            }
        }
        when (DefaultsViewModel.getDefaultViewRowsYoy()) {
            YoyView.ALL -> binding.buttonViewAll.isChecked = true
            YoyView.ACTUALS -> binding.buttonViewActuals.isChecked = true
            YoyView.AVERAGE -> binding.buttonViewAverage.isChecked = true
            YoyView.BUDGET -> binding.buttonViewBudgets.isChecked = true
            else -> binding.buttonViewDelta.isChecked = true
        }
        binding.optionsLinearLayout.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeBottom() {
                super.onSwipeBottom()
                binding.optionsLinearLayout.visibility = View.GONE
                binding.navButtonLinearLayout.visibility = View.VISIBLE
            }
        })
/*        binding.scrollView.setLeftSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                moveForward()
            }
        })
        binding.scrollView.setRightSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                moveBackward()
            }
        }) */
        loadRows(true)
        setActionBarTitle()
    }

    private fun loadRows(iRefreshRows: Boolean = false) = runBlocking {
        launch {
            val viewRows = DefaultsViewModel.getDefaultViewRowsYoy()
            if (iRefreshRows) {
                val yoyRows = YearOverYearRows()
                val defWho = if (DefaultsViewModel.getDefaultFilterWhoYOY().toIntOrNull() != null)
                    DefaultsViewModel.getDefaultFilterWhoYOY().toInt() else 2
                myRows = yoyRows.getRows(
                    DefaultsViewModel.getDefaultFilterDiscYOY(),
                    defWho
                )
            }
            binding.tableHeader.removeAllViews()
            mTableLayout!!.removeAllViews()
            var lastCategory = ""
            var lastCategoryID = 0
            val lastCategoryTotals: MutableList<Double> = mutableListOf()
            val grandTotals: MutableList<Double> = mutableListOf()
            for (i in TransactionViewModel.getEarliestYear() until getLatestYear() + 1) {
                lastCategoryTotals.add(0.0)
                grandTotals.add(0.0)
            }

            // do header row
            createViewRow(cHEADER, -1, viewRows, YoyView.ALL, 0, "", "", null)

            var i = 0
            for (row in myRows) {
                if (lastCategory != "" && row.category != lastCategory) {
                    // sub-total row
                    createViewRow(
                        cSUBTOTAL,
                        i,
                        viewRows,
                        row.yoyEnum,
                        lastCategoryID,
                        lastCategory,
                        "",
                        lastCategoryTotals
                    )
                    for (c in 0 until row.amounts.size) {
                        grandTotals[c] += lastCategoryTotals[c]
                        lastCategoryTotals[c] = 0.0
                    }
                }

                lastCategory = row.category
                lastCategoryID = row.categoryID
                val catID = CategoryViewModel.getID(row.category, row.subcategory)
                createViewRow(
                    cDETAIL, i, viewRows, row.yoyEnum, catID,
                    row.category, row.subcategory, row.amounts
                )
                if (row.yoyEnum == viewRows && !isExcluded(catID)) {
                    for (c in 0 until row.amounts.size) {
                        lastCategoryTotals[c] += row.amounts[c]
                    }
                    i++
                }
            }

            createViewRow(
                cSUBTOTAL,
                i++,
                viewRows,
                viewRows,
                lastCategoryID,
                lastCategory,
                "",
                lastCategoryTotals
            )
            for (c in 0 until grandTotals.size) {
                grandTotals[c] += lastCategoryTotals[c]
            }
            if (viewRows != YoyView.ALL)
                createViewRow(
                    cGRANDTOTAL,
                    i++,
                    viewRows,
                    viewRows,
                    0,
                    getString(R.string.total),
                    "",
                    grandTotals
                )
            // add spacer row.  For some reason the bottom row rightmost cell doesn't display without this
            val tempTableHeaderRow = binding.tableHeader.getChildAt(0) as TableRow
            val tr = YOYTableRow(requireContext())
            for (c in 0 until tempTableHeaderRow.childCount)
                tr.addView(TextView(requireContext()))
            binding.tableRows.addView(tr)

           val run = Runnable {
               val tableHeaderRow = binding.tableHeader.getChildAt(0) as TableRow
               var firstVisibleRow = -1
               for (ind in 0 until binding.tableRows.childCount) {
                   val tRow = binding.tableRows.getChildAt(ind) as TableRow
                   if (tRow.isVisible)
                       firstVisibleRow = ind
               }

               if (firstVisibleRow > -1) {
                   val tableRow = binding.tableRows.getChildAt(firstVisibleRow) as TableRow
                    for (ind in 0 until tableHeaderRow.childCount) {
                        if (tableHeaderRow.getChildAt(ind).visibility == View.VISIBLE) {
                            if (tableHeaderRow.getChildAt(ind).measuredWidth > tableRow.getChildAt(ind).measuredWidth) {
                                tableRow.getChildAt(ind).layoutParams = TableRow.LayoutParams(
                                    tableHeaderRow.getChildAt(ind).measuredWidth,
                                    tableRow.getChildAt(ind).measuredHeight)
                            } else {
                                tableHeaderRow.getChildAt(ind).layoutParams = TableRow.LayoutParams(
                                    tableRow.getChildAt(ind).measuredWidth,
                                    tableHeaderRow.getChildAt(ind).measuredHeight)
                            }
                        }
                    }
                }
            }
           binding.tableRows.post(run)
        }
    }

    private fun createViewRow(iRowType: Int, iRowNo: Int, defaultsView: YoyView, iYoyType: YoyView,
                              iCategoryID: Int, iCategory: String,
                              iSubcategory: String, iAmounts: MutableList<Double>?) {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0
        var numYearsShown = 0

        val tv1 = TextView(requireContext())
        tv1.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        tv1.visibility = View.GONE
        tv1.text = iCategoryID.toString()
//        tv1.tag = iYoyType.code

        val tv2 = TextView(requireContext())
        tv2.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        tv2.gravity = if (iRowType == cDETAIL) (Gravity.START or Gravity.CENTER_VERTICAL)
            else (Gravity.END or Gravity.CENTER_VERTICAL)
        tv2.setPadding(15, 15, 0, 15)
//        tv2.tag = if (isCollapsed(iCategory)) getString(R.string.collapsed) else getString(R.string.expanded)
        if (iRowType == cHEADER) {
/*            tv2.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_baseline_arrow_left_24, 0)
            tv2.text = ""
            tv2.setOnClickListener {
                moveBackward()
            } */
        } else {
            when (iRowType) {
                cDETAIL -> {
                    if ((defaultsView == YoyView.ALL && iYoyType == YoyView.ALL) ||
                                defaultsView != YoyView.ALL) {
                        tv2.text = iSubcategory
                    } else {
                        tv2.text = iYoyType.toString()
                    }
                }
                cSUBTOTAL -> {
                    tv2.text = String.format("$iCategory ${getString(R.string.total)}")
                    if (isCollapsed(iCategory)) {
                        tv2.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    } else {
                        tv2.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    }
                }
                else -> tv2.text = iCategory
            }
            if (tv2.text.length > 15) {
                tv2.text = String.format("${tv2.text.substring(0,15)}...")
            }
        }

        val tvAmounts: MutableList<TextView> = arrayListOf()
        for (i in 0 until (getLatestYear() - TransactionViewModel.getEarliestYear() + 1)) {
            tvAmounts.add(TextView(requireContext()))
            if (i == getLatestYear() - TransactionViewModel.getEarliestYear() &&
                    iRowType != cHEADER) {
                tvAmounts[i].layoutParams = TableRow.LayoutParams(
                    0,
                    //TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1F
                )
                tvAmounts[i].setPadding(5, 15, 40, 15)
            } else {
                tvAmounts[i].layoutParams = TableRow.LayoutParams(
                    0,
//                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1F
                )
                tvAmounts[i].setPadding(5, 15, 20, 15)
            }
            tvAmounts[i].gravity = (Gravity.END or Gravity.CENTER_VERTICAL)
            if (iRowType == cHEADER) {
                if (i == getLatestYear() - TransactionViewModel.getEarliestYear()) // ie it's the last column, Average
                    tvAmounts[i].text = underlined("Avg")
                else
                    tvAmounts[i].text = underlined(String.format("%d",TransactionViewModel.getEarliestYear() + i))
/*                if (i == getIndexOfFirstYearVisible() && i != 0) {
                    tvAmounts[i].setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_arrow_left_24, 0, 0, 0)
                    tvAmounts[i].setOnClickListener {
                        moveBackward()
                    }
                } else if (i == getIndexOfLastYearVisible() && i != getLatestYear() - TransactionViewModel.getEarliestYear()) {
                    tvAmounts[i].setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, R.drawable.ic_baseline_arrow_right_24, 0)
                    tvAmounts[i].setOnClickListener {
                        moveForward()
                    }
                } else {
                    tvAmounts[i].setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, 0, 0)
                } */
            } else {
                if (defaultsView == YoyView.DELTA && DefaultsViewModel.getDefaultDeltaYOY() == "%") {
                    val percentFormat = java.text.DecimalFormat("# %")
                    when {
                        (iAmounts?.get(i) ?: 0.0) == 0.0 -> tvAmounts[i].text = "0 %"
                        else -> {
                            tvAmounts[i].text = percentFormat.format(iAmounts?.get(i) ?: 0.0)
                        }
                    }
                } else if (iRowType == cDETAIL && iYoyType == YoyView.ALL) {
                    tvAmounts[i].text = ""
                } else {
                        tvAmounts[i].text = gDecWithCurrency(
                            iAmounts?.get(i) ?: 0.0,
                            DefaultsViewModel.getDefaultRoundYOY()
                        )
                }
            }
            if ((iRowType != cHEADER && defaultsView == YoyView.DELTA) ||
                (iRowType == cDETAIL && tv2.text == YoyView.DELTA.toString() && defaultsView == YoyView.ALL)){
                tvAmounts[i].setTextColor(getBudgetColour(requireContext(),
                    0.0,iAmounts?.get(i) ?: 0.0, true))
            }
//            if (iRowType == cDETAIL && tv1.tag == YoyView.BUDGET.code) {
            if (iRowType == cDETAIL && iYoyType == YoyView.BUDGET) {
                tvAmounts[i].setOnClickListener {
                    val myParent = (it as TextView).parent as YOYTableRow
                    val action =
                        DashboardTabsFragmentDirections.actionDashboardTabsFragmentToBudgetViewAllFragment()
                    action.categoryID = myParent.categoryID.toString()
                    view?.findNavController()?.navigate(action)
                }
            } else if (iRowType == cDETAIL && (iYoyType == YoyView.ACTUALS || iYoyType == YoyView.AVERAGE)) {
                tvAmounts[i].setOnClickListener {
                    val myParent = (it as TextView).parent as YOYTableRow
                    val year = it.tag.toString().toInt()
                    val cat = CategoryViewModel.getCategory(myParent.categoryID)
                    // go to ViewAll with the SubCategory as the search term
                    MyApplication.transactionSearchText = "${cat?.categoryName} ${cat?.subcategoryName} $year"
                    view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
                }
            }
            tvAmounts[i].tag = i + TransactionViewModel.getEarliestYear()
            numYearsShown += 1
//            if (numYearsShown > maxYearColumns)
                tvAmounts[i].visibility = getYearVisibility(i + TransactionViewModel.getEarliestYear())
        }

        // add table row
        val tr = YOYTableRow(requireContext())
        tr.rowType = iRowType
        tr.yoyView = iYoyType
        tr.categoryID = iCategoryID
        tr.categoryName = iCategory

        tr.id = iRowNo + 1
//        tr.tag = iRowType
        tr.addView(tv1)
        tr.addView(tv2)
        for (i in 0 until tvAmounts.size)
            tr.addView(tvAmounts[i])

        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        tvAmounts[tvAmounts.size - 1].setTypeface(null, Typeface.BOLD)
        when (iRowType) {
            cSUBTOTAL -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 20)
                tr.setPadding(5, 15, 5, 15)
            }
            cGRANDTOTAL -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 50)
                tr.setPadding(5, 15, 5, 15)
            }
            else -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
                tr.setPadding(5, 15, 5, 15)
            }
        }
        tr.layoutParams = trParams
        if (iRowType == cDETAIL) {
            colorDetailRow(tr, iCategoryID)
/*            val cat = DefaultsViewModel.getCategoryDetail(iCategory)
                if (Build.VERSION.SDK_INT >= 29) {
                    tr.setBackgroundResource(R.drawable.row_left_border)
                    if (cat.color != 0)
                        tr.background.colorFilter =
                            BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                } else {
                    if (cat.color != 0)
                        tr.setBackgroundColor(cat.color)
                    tr.background.alpha = 44
                } */
            if (iYoyType == YoyView.ALL) {
                tv2.setTypeface(null, Typeface.BOLD)
            }
        }
        else if (iRowType == cHEADER) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            for (i in 0 until tvAmounts.size) {
                tvAmounts[i].setTypeface(null, Typeface.BOLD)
                tvAmounts[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
            }
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
        }
        else if (iRowType == cSUBTOTAL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) // 14F is default
            for (i in 0 until tvAmounts.size)
                tvAmounts[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)

            val cat = DefaultsViewModel.getCategoryDetail(iCategory)
            if (cat.color != 0) {
//                if (Build.VERSION.SDK_INT >= 29) {
                tr.setBackgroundResource(R.drawable.row_left_and_bottom_border)
                tr.background.colorFilter =
                    BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
/*                } else {
                    tr.setBackgroundColor(cat.color)
//                    tr.background.alpha = 44
                }
            } else if (Build.VERSION.SDK_INT >= 29) { */
            } else {
                tr.setBackgroundResource(R.drawable.row_left_and_bottom_border)
                val hexColor = MaterialColors.getColor(requireContext(), R.attr.colorPrimary, Color.BLACK)
                tr.background.colorFilter =
                    BlendModeColorFilter(hexColor, BlendMode.SRC_ATOP)
            }
            if (inDarkMode(requireContext())) {
                val hexColor = MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK)
                tv2.setTextColor(hexColor)
                for (i in 0 until tvAmounts.size)
                    tvAmounts[i].setTextColor(hexColor)
            }
        }
        else if (iRowType == cGRANDTOTAL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            for (i in 0 until tvAmounts.size)
                tvAmounts[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            val hexColor = MaterialColors.getColor(
                requireContext(),
                R.attr.colorOnBackground,
                Color.BLACK
            )
            tr.setBackgroundResource(R.drawable.row_frame)
            tr.background.colorFilter =
                BlendModeColorFilter(hexColor, BlendMode.SRC_ATOP)
        }

        if (isCollapsed(iCategory)) {
            tr.isExpanded = false
//            tv2.tag = getString(R.string.collapsed)
            if (iRowType == cDETAIL) {
                tr.visibility = View.GONE
            }  else if (iRowType == cSUBTOTAL) {
                tv1.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
            }
        } else if (iRowType == cDETAIL) {
            if (defaultsView == YoyView.ALL || defaultsView == iYoyType) {
                tr.visibility = View.VISIBLE
            } else {
                tr.visibility = View.GONE
            }
        } else
            tr.visibility = View.VISIBLE
        if (iRowType == cSUBTOTAL) {
            tr.setOnClickListener {
                val tableRow = it as YOYTableRow
                val ltv2 = tableRow.getChildAt(1) as TextView
                var tmpCat = ltv2.text.toString().replace(getString(R.string.total),"")
                tmpCat = tmpCat.replace("...","")
                tmpCat = tmpCat.trim()
                if (!tr.isExpanded) {
//                if (ltv2.tag.toString() == getString(R.string.collapsed) ) {
  //                  ltv2.tag = getString(R.string.expanded)
                    tr.isExpanded = true
                    ltv2.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    refreshRows(tmpCat, View.VISIBLE)
                } else {
//                    ltv2.tag = getString(R.string.collapsed)
                    tr.isExpanded = false
                    ltv2.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    refreshRows(tmpCat, View.GONE)
                }
            }
        }
        if (iRowType == cGRANDTOTAL)
            mTableLayout!!.addView(tr,1)
        else {
            if (iRowType == cHEADER) {
                binding.tableHeader.addView(tr)
//                mTableLayout!!.addView(tr)
            } else
                mTableLayout!!.addView(tr)
        }
/*        if (iRowType != cHEADER) {
            // add separator row
            val trSep = YOYTableRow(requireContext())
            val trParamsSep = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            trParamsSep.setMargins(
                leftRowMargin,
                topRowMargin,
                rightRowMargin,
                bottomRowMargin
            )
            trSep.layoutParams = trParamsSep
        } */
        if (iRowType == cDETAIL) {
            tr.setOnClickListener { ltr ->
                val myRow: YOYTableRow = ltr as YOYTableRow
                if (isExcluded(iCategoryID))
                    currentlyExcluded.remove(iCategoryID)
                else
                    currentlyExcluded.add(iCategoryID)
                colorDetailRow(myRow, iCategoryID)
                loadRows(false)
            }
            tr.setOnLongClickListener { thistr ->
                val myTr:YOYTableRow = thistr as YOYTableRow
                val thisCategoryTV = myTr.getChildAt(0) as TextView
                val thisCategory = thisCategoryTV.text.toString().toInt()
                val myTable = binding.tableRows
                for (i in 0 until myTable.size) {
                    val ltr = myTable.getChildAt(i) as YOYTableRow
                    val ltv = ltr.getChildAt(0) as TextView
                    if (ltv.text.toString().toInt() == thisCategory) {
                        gActualRow = myTable.getChildAt(i+1) as YOYTableRow
                        gAverageRow = myTable.getChildAt(i+2) as YOYTableRow
                        gBudgetRow = myTable.getChildAt(i+3) as YOYTableRow
                        gDeltaRow = myTable.getChildAt(i+4) as YOYTableRow
                        break
                    }
                }
                val yoydf = YearOverYearDialogFragment.newInstance(iCategoryID.toString())
                yoydf.show(parentFragmentManager, "YOYDialog")
                true
            }
        }
    }

    private fun colorDetailRow(iRow: YOYTableRow, iCategoryID: Int) {
        val categ = CategoryViewModel.getCategory(iCategoryID) ?: return
        if (isExcluded(iCategoryID)) {
            iRow.isIncludedInTotals = false
            val tv2 = iRow.getChildAt(1) as TextView
            tv2.text = String.format(getString(R.string.exclude_row), tv2.text)
            val hexColor = MaterialColors.getColor(
                requireContext(),
                R.attr.background,
                Color.BLACK
            )
            iRow.setBackgroundResource(R.drawable.row_left_and_right_border)
            iRow.background.colorFilter =
                BlendModeColorFilter(hexColor, BlendMode.SRC_ATOP)
        } else {
            iRow.isIncludedInTotals = true
            val cat = DefaultsViewModel.getCategoryDetail(categ.categoryName)
            val hexColor = if (cat.color == 0) {
                MaterialColors.getColor(
                    requireContext(),
                    R.attr.colorPrimary,
                    Color.BLACK
                )
            } else {
                cat.color
            }
            if (cat.color == 0) {
                    iRow.setBackgroundResource(R.drawable.row_left_border_no_fill)
            } else {
                if (inDarkMode(requireContext()))
                    iRow.setBackgroundResource(R.drawable.row_left_border_no_fill)
                else
                    iRow.setBackgroundResource(R.drawable.row_left_border)
            }
            iRow.background.colorFilter =
                BlendModeColorFilter(hexColor, BlendMode.SRC_ATOP)
        }
    }

    private fun refreshRows(iCategory: String, iVisibility: Int) {
        var firstDetailLine: Int
        var lastDetailLine = 0
        mTableLayout = binding.tableRows
        var tableRow: YOYTableRow?
        do {
            lastDetailLine += 1
            tableRow = mTableLayout!!.getChildAt(lastDetailLine) as YOYTableRow
            val catIDTV = tableRow.getChildAt(0) as TextView
            val catID = catIDTV.text.toString().toInt()
            val cat = CategoryViewModel.getCategory(catID)?.categoryName
        } while (tableRow != null && !(tableRow.rowType == cSUBTOTAL && cat == iCategory))
        if (tableRow == null) // no detail rows found
            return
        // found sub-total row, now work backwards
        lastDetailLine -= 1
        tableRow = mTableLayout!!.getChildAt(lastDetailLine) as YOYTableRow
        if (tableRow.rowType != cDETAIL)  // ie no details for this category
            return
        firstDetailLine = lastDetailLine
        do {
            firstDetailLine -= 1
            tableRow = mTableLayout!!.getChildAt(firstDetailLine) as YOYTableRow
        } while (tableRow != null && tableRow.rowType == cDETAIL)
        firstDetailLine += 1
        // now check if section should be expanded or collapsed
        val defView = DefaultsViewModel.getDefaultViewRowsYoy()
        for (i in firstDetailLine..lastDetailLine) {
            tableRow = mTableLayout!!.getChildAt(i) as YOYTableRow
            if (iVisibility == View.VISIBLE) {
                if (defView == YoyView.ALL) {
                    tableRow.visibility = View.VISIBLE
                } else if (tableRow.yoyView == defView){
                    tableRow.visibility = View.VISIBLE
                }
            } else
                tableRow.visibility = iVisibility
        }
        if (iVisibility == View.VISIBLE)
            currentlyCollapsed.remove(iCategory)
        else
            currentlyCollapsed.add(iCategory)
    }

    private fun onExpandClicked(layout: LinearLayout) {
        if (layout.visibility == View.GONE) { // ie expand the section
            // first hide all other possible expansions
            resetLayout(binding.navButtonLinearLayout)
            resetLayout(binding.optionsLinearLayout)
            layout.visibility = View.VISIBLE
        } else { // ie retract the section
            resetLayout(layout)
        }
    }
    private fun resetLayout(layout: LinearLayout) {
        layout.visibility = View.GONE
    }

    private fun setActionBarTitle() {
        var title = when (DefaultsViewModel.getDefaultViewRowsYoy()) {
            YoyView.ALL -> "All Views"
            YoyView.ACTUALS -> "Actuals Per Year"
            YoyView.BUDGET -> "Budgets Per Year"
            YoyView.AVERAGE -> "Average Actuals Per Month"
            else -> "Delta Per Year"
        }
        title = "$title "

        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefaultFilterDiscYOY() != "") {
            when (DefaultsViewModel.getDefaultFilterDiscYOY()) {
                cDiscTypeDiscretionary -> currentFilterIndicator = getString(R.string.discretionary)
                cDiscTypeNondiscretionary -> currentFilterIndicator = getString(R.string.non_discretionary)
            }
        }
        if (DefaultsViewModel.getDefaultFilterWhoYOY() != "")
            currentFilterIndicator = currentFilterIndicator +
                    (if (currentFilterIndicator == "") "" else " ") +
                    SpenderViewModel.getSpenderName(DefaultsViewModel.getDefaultFilterWhoYOY().toInt())
        val sep =  if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            ""
        else
            "\n"
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

    private fun moveBackward() {
        var firstYear = 0
        for (i in 0 until yearVisibility.size) {
            if (yearVisibility[i] == View.VISIBLE) {
                firstYear = i
                break
            }
        }
        if (firstYear == 0) {
            Toast.makeText(requireActivity(), String.format(getString(R.string.there_is_no_data_before_date), TransactionViewModel.getEarliestYear())
                , Toast.LENGTH_SHORT).show()
        } else {
            yearVisibility[firstYear + maxYearColumns - 1] = View.GONE
            for (i in firstYear-1 until firstYear-1+maxYearColumns) {
                if (i < yearVisibility.size)
                    yearVisibility[i] = View.VISIBLE
            }
            loadRows(false)
        }
    }

    private fun moveForward() {
        var lastYear = yearVisibility.size-1
        for (i in yearVisibility.size - 1 downTo 0) {
            if (yearVisibility[i] == View.VISIBLE) {
                lastYear = i
                break
            }
        }
        if (lastYear == yearVisibility.size-1) {
            Toast.makeText(requireActivity(), String.format(getString(R.string.there_is_no_data_after_date), TransactionViewModel.getLatestYear())
                , Toast.LENGTH_SHORT).show()
        } else {
            yearVisibility[lastYear - maxYearColumns + 1] = View.GONE
            for (i in lastYear+1 downTo lastYear-maxYearColumns+2) {
                if (i >= 0)
                    yearVisibility[i] = View.VISIBLE
            }
            loadRows(false)
        }
    }

    private fun isCollapsed(iCat: String): Boolean {
        currentlyCollapsed.forEach {
            val len = if (it.length > iCat.length) iCat.length else it.length
            if (it.substring(0, len) == iCat.substring(0, len))
                return true
        }
        return false
    }
}


data class YearOverYearData(
    var categoryID: Int = 0,
    var yoyEnum: YoyView = YoyView.ALL,
    var category: String = "",
    var subcategory: String = "",
    var priority: Int = 0,
    var amounts: MutableList<Double> = arrayListOf()) {
    constructor(numOfYears: Int) : this() {
        for (i in 0 until numOfYears)
            amounts.add(0.0)
    }
}

class YearOverYearRows {
    fun getRows(
        iDiscFlag: String = "",
        iBoughtForFlag: Int = 2
    ): MutableList<YearOverYearData> {
        val firstYear = TransactionViewModel.getEarliestYear()
        val firstMonth = TransactionViewModel.getEarliestMonth()
        val lastYear = gCurrentDate.getYear() + 1
        val lastMonth = gCurrentDate.getMonth()
        val data: MutableList<YearOverYearData> = mutableListOf()
        val categories = CategoryViewModel.getCategories(true)
        categories.forEach {
            if (it.discType == iDiscFlag || iDiscFlag == "" || iDiscFlag == cDiscTypeAll) {
                for (e in YoyView.ALL.code until YoyView.DELTA.code + 1) {
                    val row = YearOverYearData(lastYear - firstYear + 1)
                    row.categoryID = it.id
                    row.yoyEnum = YoyView.getByValue(e)!!
                    row.category = it.categoryName
                    row.subcategory = it.subcategoryName
                    row.priority = it.priority
                    data.add(row)
                }
            }
        }  // at this point we have a complete list of categories, with 5 rows each

        val actualTotals = TransactionViewModel.getAnnualCategoryActuals(iDiscFlag, iBoughtForFlag)
        var i = 1
        for (actualTotal in actualTotals) {
            i += 1
            val row = data.find { it.categoryID == actualTotal.catID && it.yoyEnum == YoyView.ACTUALS }
            if (row != null) {
                row.amounts[actualTotal.year - firstYear] = actualTotal.value
            }
        }

        var lastActuals: MutableList<Double> = arrayListOf()
        var lastBudgets: MutableList<Double> = arrayListOf()
        for (dataRow in data) {
            when (dataRow.yoyEnum) {
                YoyView.ACTUALS -> {
                    lastActuals = dataRow.amounts
                    var tempTotal = 0.0
                    var tempNumOfMonths = 0.0
                    for (c in 0 until dataRow.amounts.size-1) {
                        tempNumOfMonths += when (c) {
                            0 -> ((12 - firstMonth + 1)/12.0)
                            dataRow.amounts.size - 2 -> (lastMonth/12.0)
                            else -> 12/12.0
                        }
                        tempTotal += dataRow.amounts[c]
                    }
                    dataRow.amounts[dataRow.amounts.size-1] = tempTotal / tempNumOfMonths
                }
                YoyView.BUDGET -> {
                    var tempTotal = 0.0
                    for (year in 0 until (lastYear - firstYear )) {
                        // iPeriod: DateRangeEnum, iBudgetMonth: MyDate, iCategoryID: Int, iWhoToLookup: Int

                        dataRow.amounts[year] = BudgetViewModel.getCalculatedBudgetAmount(
                            DateRangeEnum.YEAR,
                            MyDate(year + firstYear),
                            dataRow.categoryID,
                            iBoughtForFlag)
                        tempTotal += dataRow.amounts[year]
                    }
                    dataRow.amounts[dataRow.amounts.size-1] = tempTotal /  (lastYear - firstYear)
                    lastBudgets = dataRow.amounts
                }
                YoyView.DELTA -> {
                    var tempTotal = 0.0
                    var tempNumOfMonths = 0.0
                    for (year in 0 until (lastYear - firstYear )) {
                        if (DefaultsViewModel.getDefaultDeltaYOY() == "%") {
                            if (lastBudgets[year] == 0.0)
                                dataRow.amounts[year] = 0.0
                            else {
                                dataRow.amounts[year] = lastActuals[year] / lastBudgets[year]
                            }
                        } else {
                            dataRow.amounts[year] = lastBudgets[year] - lastActuals[year]
                        }
                        tempNumOfMonths += when (year) {
                            0 -> ((12 - firstMonth + 1)/12.0)
                            dataRow.amounts.size - 2 -> (lastMonth/12.0)
                            else -> 12/12.0
                        }
                        tempTotal += dataRow.amounts[year]
                    }
                    dataRow.amounts[dataRow.amounts.size-1] = tempTotal / tempNumOfMonths
                }
                YoyView.AVERAGE -> {
                    var tempTotal = 0.0
                    for (year in 0 until (lastYear - firstYear)) {
                        dataRow.amounts[year] =
                            when (year + firstYear) {
                                firstYear -> lastActuals[year] / (12 - firstMonth + 1)
                                lastYear -> lastActuals[year] / lastMonth
                                else -> lastActuals[year] / 12
                            }
                        tempTotal += dataRow.amounts[year]
                    }
                    dataRow.amounts[dataRow.amounts.size-1] = tempTotal / (lastYear - firstYear)
                }
                else -> {
                }
            }
        }

        data.sortWith(compareBy({ it.priority }, { it.category }, { it.subcategory }))
        return data
    }
}