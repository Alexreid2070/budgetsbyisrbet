package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentDashboardBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.abs

private const val cDETAIL = 0
private const val cHEADER = 1
private const val cSUBTOTAL = 2
private const val cGRANDTOTAL = 3

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var mTableLayout: TableLayout? = null
    private var currentBudgetMonth: BudgetMonth = BudgetMonth(0,0)
    private var collapsedCategories: MutableList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_dashboard, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.dollarRadioButton.text = getLocalCurrencySymbol(true)
        mTableLayout = binding.tableDashboardRows
        binding.tableDashboardRows.isStretchAllColumns = true
        if (SpenderViewModel.singleUser())
            binding.filterWhoLayout.visibility = View.GONE

        if (currentBudgetMonth.year == 0) {
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = if (DefaultsViewModel.getDefaultViewPeriodDashboard() == cPeriodYear)
                BudgetMonth(dateNow.get(Calendar.YEAR), 0)
            else
                BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
        }
        when (DefaultsViewModel.getDefaultFilterDiscDashboard()) {
            cDiscTypeDiscretionary -> binding.discRadioButton.isChecked = true
            cDiscTypeNondiscretionary -> binding.nonDiscRadioButton.isChecked = true
            else -> binding.allDiscRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultFilterWhoDashboard()) {
            "0" -> binding.name1RadioButton.isChecked = true
            "1" -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultDeltaDashboard()) {
            "#" -> binding.dollarRadioButton.isChecked = true
            "%" -> binding.percentageRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultRoundDashboard()) {
            true -> binding.switchRoundToNearestDollar.isChecked = true
            false -> binding.switchRoundToNearestDollar.isChecked = false
        }
        when (DefaultsViewModel.getDefaultShowDiscDashboard()) {
            true -> binding.showDiscColumn.isChecked = true
            false -> binding.showDiscColumn.isChecked = false
        }
        startLoadData(currentBudgetMonth)
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)

        binding.buttonBackward.setOnClickListener {
            moveBackward()
        }
        binding.buttonForward.setOnClickListener {
            moveForward()
        }
        binding.buttonViewMonth.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_PERIOD_DASHBOARD, cPeriodMonth)
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(currentBudgetMonth.year, dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewYtd.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_PERIOD_DASHBOARD, cPeriodYTD)
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewYear.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_PERIOD_DASHBOARD, cPeriodYear)
            currentBudgetMonth.month = 0
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewAllTime.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_PERIOD_DASHBOARD, cPeriodAllTime)
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.expandNav.setOnClickListener {
            onExpandClicked(binding.expandNav, binding.navButtonLinearLayout)
        }
        binding.expandOptions.setOnClickListener {
            onExpandClicked(binding.expandOptions, binding.optionsButtonLinearLayout)
        }
        binding.expandView.setOnClickListener {
            onExpandClicked(binding.expandView, binding.viewButtonLinearLayout)
        }
        binding.expandFilter.setOnClickListener {
            onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
        }
        binding.showDeltaRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.dollarRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_DELTA_DASHBOARD, "#")
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.percentageRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_DELTA_DASHBOARD, "%")
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        binding.showDiscColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showDiscColumn.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_DISC_DASHBOARD, true)
                startLoadData(currentBudgetMonth)
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_DISC_DASHBOARD, false)
                startLoadData(currentBudgetMonth)
            }
        }
        binding.switchRoundToNearestDollar.setOnCheckedChangeListener { _, _ ->
            if (binding.switchRoundToNearestDollar.isChecked) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_DASHBOARD, true)
                startLoadData(currentBudgetMonth)
            } else {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_ROUND_DASHBOARD, false)
                startLoadData(currentBudgetMonth)
            }
        }
        binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.discRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_DASHBOARD, cDiscTypeDiscretionary)
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.nonDiscRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_DASHBOARD, cDiscTypeNondiscretionary)
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
                R.id.allDiscRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_DASHBOARD, "")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.name1RadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_DASHBOARD, "0")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.name2RadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_DASHBOARD, "1")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
                R.id.whoAllRadioButton -> {
                    DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_DASHBOARD, "")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        when (DefaultsViewModel.getDefaultViewPeriodDashboard()) {
            cPeriodMonth -> binding.buttonViewMonth.isChecked = true
            cPeriodYTD -> binding.buttonViewYtd.isChecked = true
            cPeriodYear -> binding.buttonViewYear.isChecked = true
            cPeriodAllTime -> binding.buttonViewAllTime.isChecked = true
        }
        binding.resetFilterButton.setOnClickListener {
            DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_DASHBOARD, "")
            binding.allDiscRadioButton.isChecked = true
            DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_WHO_DASHBOARD, "")
            binding.whoAllRadioButton.isChecked = true
            onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
        }
        binding.scrollView.setLeftSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                moveForward()
            }
        })
        binding.scrollView.setRightSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                moveBackward()
            }
        })
        setActionBarTitle()
        HintViewModel.showHint(parentFragmentManager, cHINT_DASHBOARD)
    }

    private fun startLoadData(iBudgetMonth: BudgetMonth) {
        val dashboardRows = DashboardRows()
        val viewPeriod = when (DefaultsViewModel.getDefaultViewPeriodDashboard()) {
            cPeriodAllTime -> DateRange.ALLTIME
            cPeriodYTD -> DateRange.YTD
            cPeriodYear -> DateRange.YEAR
            else -> DateRange.MONTH
        }
        val defWho = if (DefaultsViewModel.getDefaultFilterWhoDashboard().toIntOrNull() != null)
            DefaultsViewModel.getDefaultFilterWhoDashboard().toInt() else 2
        val data: MutableList<DashboardData> = dashboardRows.getRows(iBudgetMonth,
            DefaultsViewModel.getDefaultFilterDiscDashboard(),
            defWho,
            viewPeriod)

        val rows = data.size
        var textSpacer: TextView?
        binding.tableHeaderRow.removeAllViews()
        mTableLayout!!.removeAllViews()
        var lastCategory = ""
        var lastCategoryBudgetTotal = 0.0
        var lastCategoryActualTotal = 0.0
        var grandBudgetTotal = 0.0
        var grandActualTotal = 0.0

        // -1 means heading row
        var i = -1
        // do header row
        createViewRow(cHEADER, i++, 0, "", "", "", 0.0, 0.0)

        while (i < rows) {
            var row: DashboardData? = null
            if (i > -1) row = data[i] else {
                textSpacer = TextView(requireContext())
                textSpacer.text = ""
            }
            if (row != null) {
                    if (lastCategory != "" && row.category != lastCategory) {
                        // sub-total row
                        createViewRow(cSUBTOTAL, i, 0, lastCategory, "", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
                        grandBudgetTotal += lastCategoryBudgetTotal
                        grandActualTotal += lastCategoryActualTotal
                        lastCategoryBudgetTotal = 0.0
                        lastCategoryActualTotal = 0.0
                    }

                    lastCategory = row.category
                    createViewRow(cDETAIL, i,
                        CategoryViewModel.getID(row.category, row.subcategory),
                        row.category, row.subcategory, row.discIndicator, row.budgetAmount, row.actualAmount)
                    lastCategoryBudgetTotal += row.budgetAmount
                    lastCategoryActualTotal += row.actualAmount
                    i++
            }
        }

        createViewRow(cSUBTOTAL, i++, 0, lastCategory, "", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
        grandBudgetTotal += lastCategoryBudgetTotal
        grandActualTotal += lastCategoryActualTotal
        createViewRow(cGRANDTOTAL, i, 0, getString(R.string.grand_total), "", "", grandBudgetTotal, grandActualTotal)
        val run = Runnable {
            val tableHeaderRow = binding.tableHeaderRow.getChildAt(0) as TableRow
            val tableDashboardRow = binding.tableDashboardRows.getChildAt(0) as TableRow
            for (ind in 0 until tableHeaderRow.childCount) {
                tableHeaderRow.getChildAt(ind).layoutParams = TableRow.LayoutParams(
                    tableDashboardRow.getChildAt(ind).measuredWidth,
                    tableDashboardRow.getChildAt(ind).measuredHeight
                )
            }
        }
        binding.tableDashboardRows.post(run)
    }

    private fun createViewRow(iRowType: Int, iRowNo: Int, iCategoryID: Int, iCategory: String, iSubcategory: String, iDiscFlag: String, iBudgetAmount: Double, iActualAmount:Double) {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0

        val tv0 = TextView(requireContext())
        tv0.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        tv0.visibility = View.GONE
        tv0.text = iCategory

        val tv1 = TextView(requireContext())
        tv1.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        tv1.gravity = if (iRowType == cDETAIL) Gravity.START else Gravity.END
        tv1.setPadding(15, 15, 0, 15)
        if (iRowType == cHEADER) {
            tv1.text = ""
        } else {
            when (iRowType) {
                cDETAIL -> tv1.text = iSubcategory
                cSUBTOTAL -> {
                    tv1.text = "$iCategory " + getString(R.string.total)
                    tv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24, 0, 0, 0);                }
                else -> tv1.text = iCategory
            }
            if (tv1.text.length > 15) {
                tv1.text = tv1.text.substring(0,15) + "..."
            }
        }
        tv1.tag = getString(R.string.expanded)
        val tv2 = TextView(requireContext())
        tv2.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        if (iRowType == cSUBTOTAL) {
            tv2.gravity = Gravity.CENTER
            tv2.setPadding(0, 0, 0, 0)
        } else {
            tv2.gravity = Gravity.START
            tv2.setPadding(5, 15, 0, 15)
        }
        if (iRowType == cHEADER) {
            tv2.text = getString(R.string.discretionary_short_question)
            tv2.tooltipText = getString(R.string.toolTipDisc)
        } else {
            tv2.text = iDiscFlag
        }
        if (!DefaultsViewModel.getDefaultShowDiscDashboard())
            tv2.visibility = View.GONE

        val tv3 = TextView(requireContext())
        if (iRowType == cHEADER) {
            tv3.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv3.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
        tv3.gravity = Gravity.END
        tv3.setPadding(5, 15, 0, 15)
        if (iRowType == cHEADER) {
            tv3.text = getString(R.string.actual)
            tv3.tooltipText = getString(R.string.toolTipActual)
        } else {
                tv3.text = gDecWithCurrency(iActualAmount,
                    DefaultsViewModel.getDefaultRoundDashboard()
                )
        }
        tv3.tag = iCategoryID

        val tv4 = TextView(requireContext())
        if (iRowType == cHEADER) {
            tv4.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv4.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
        tv4.gravity = Gravity.END
        tv4.setPadding(5, 15, 0, 15)
        if (iRowType == cHEADER) {
            tv4.text = getString(R.string.budget)
            tv4.tooltipText = getString(R.string.toolTipBudgeted)
        } else {
            tv4.text = gDecWithCurrency(iBudgetAmount,
                DefaultsViewModel.getDefaultRoundDashboard()
            )
        }
        tv4.tag = iCategoryID

        val tv5 = TextView(requireContext())
        if (iRowType == cHEADER) {
            tv5.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv5.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
        tv5.gravity = Gravity.END
        tv5.setPadding(5, 15, 0, 15)
        if (iRowType == cHEADER) {
            tv5.text = getString(R.string.delta)
            tv5.tooltipText = getString(R.string.toolTipDelta)
        } else {
                if (DefaultsViewModel.getDefaultDeltaDashboard() == "%") {
                    val percentFormat = java.text.DecimalFormat("# %")
                    when {
                        iActualAmount == 0.0 -> tv5.text = "0 %"
                        iBudgetAmount == 0.0 -> tv5.text = ""
                        else -> {
                            tv5.text = percentFormat.format(iActualAmount / iBudgetAmount)
                        }
                    }
                } else {
                    val diff = BigDecimal(iBudgetAmount - iActualAmount).setScale(2, RoundingMode.HALF_EVEN)
                    val tiny = BigDecimal(0.01)
                    if (diff.abs() < tiny) {
                        tv5.text = gDecWithCurrency(0.0, DefaultsViewModel.getDefaultRoundDashboard())
                    } else
                        tv5.text = gDecWithCurrency(diff.toDouble(), DefaultsViewModel.getDefaultRoundDashboard())
                }
        }
        // add table row
        val tr = TableRow(requireContext())
        tr.id = iRowNo + 1
        tr.tag = iRowType
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        when (iRowType) {
            cSUBTOTAL -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 20)
                tr.setPadding(5, 0, 0, 0)
            }
            cGRANDTOTAL -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 50)
                tr.setPadding(5, 0, 5, 0)
            }
            else -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
                tr.setPadding(5, 0, 0, 0)
            }
        }
        tr.layoutParams = trParams
        if (iRowType == cDETAIL) {
            tv5.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))

            val cat = DefaultsViewModel.getCategoryDetail(iCategory)
            if (cat.color != 0) {
                if (Build.VERSION.SDK_INT >= 29) {
                    tr.setBackgroundResource(R.drawable.row_left_border)
                    tr.background.colorFilter =
                        BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                } else {
                    tr.setBackgroundColor(cat.color)
                    tr.background.alpha = 44
                }
            }
        }
        else if (iRowType == cHEADER) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
        }
        else if (iRowType == cSUBTOTAL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))

            val cat = DefaultsViewModel.getCategoryDetail(iCategory)
            if (cat.color != 0) {
                if (Build.VERSION.SDK_INT >= 29) {
                    tr.setBackgroundResource(R.drawable.row_left_and_bottom_border)
                    tr.background.colorFilter =
                        BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                } else {
                    tr.setBackgroundColor(cat.color)
//                    tr.background.alpha = 44
                }
            }
        }
        else if (iRowType == cGRANDTOTAL) {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            val col = getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true)
            tv1.setTextColor(col)
            tv5.setTextColor(col)
            if (Build.VERSION.SDK_INT >= 29) {
                tr.setBackgroundResource(R.drawable.row_frame)
                tr.background.colorFilter =
                    BlendModeColorFilter(col, BlendMode.SRC_ATOP)
            } else {
                tr.setBackgroundColor(col)
                tr.background.alpha = 44
            }

            if (iActualAmount > iBudgetAmount)
                tv1.text = getString(R.string.over_budget)
            else
                tv1.text = getString(R.string.under_budget)
        }
        tr.addView(tv0)
        tr.addView(tv1)
        tr.addView(tv2)
        tr.addView(tv3)
        tr.addView(tv4)
        tr.addView(tv5)
        if (isInvisible(iCategory)) {
            tv1.tag = getString(R.string.collapsed)
            if (iRowType == cDETAIL) {
                tr.visibility = View.GONE
            }  else if (iRowType == cSUBTOTAL) {
                tv1.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
            }
        } else
            tr.visibility = View.VISIBLE

        if (iRowType != cHEADER && iRowType != cSUBTOTAL && iRowType != cGRANDTOTAL) {
            tv4.setOnClickListener {
                val catID = it.tag.toString()
                // go to Budget
                val action =
                    DashboardFragmentDirections.actionDashboardFragmentToBudgetViewAllFragment()
                action.categoryID = catID
                findNavController().navigate(action)
            }

            tv3.setOnClickListener {
                val catID = it.tag.toString().toInt()
                val cat = CategoryViewModel.getCategory(catID)
                // go to ViewAll with the SubCategory as the search term
                MyApplication.transactionSearchText = cat?.categoryName + " " + cat?.subcategoryName
                if (DefaultsViewModel.getDefaultViewPeriodDashboard() == cPeriodMonth ||
                    DefaultsViewModel.getDefaultViewPeriodDashboard() == cPeriodYear)
                    MyApplication.transactionSearchText = MyApplication.transactionSearchText + " " + currentBudgetMonth.year.toString()
                MyApplication.transactionSearchText = MyApplication.transactionSearchText.replace("...","")
                if (DefaultsViewModel.getDefaultViewPeriodDashboard() == cPeriodMonth) {
                    if (currentBudgetMonth.month < 10)
                        MyApplication.transactionSearchText += "-0" + currentBudgetMonth.month.toString()
                    else
                        MyApplication.transactionSearchText += "-" + currentBudgetMonth.month.toString()
                }
                view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
            }
        } else if (iRowType == cSUBTOTAL) {
            tr.setOnClickListener {
                val tableRow = it as TableRow
                val ttv1 = tableRow.getChildAt(1) as TextView
                var tmpCat = ttv1.text.toString().replace(getString(R.string.total),"")
                tmpCat = tmpCat.replace("...","")
                tmpCat = tmpCat.trim()
                if (ttv1.tag.toString() == getString(R.string.collapsed) ) {
                    ttv1.tag = getString(R.string.expanded)
                    ttv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    refreshRows(tmpCat, View.VISIBLE)
                } else {
                    ttv1.tag = getString(R.string.collapsed)
                    ttv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    refreshRows(tmpCat, View.GONE)
                }
            }
        }
        if (iRowType == cGRANDTOTAL)
            mTableLayout!!.addView(tr,0)
        else {
            if (iRowType == cHEADER)
                binding.tableHeaderRow.addView(tr)
            else
                mTableLayout!!.addView(tr)
        }
        if (iRowType != cHEADER) {
            // add separator row
            val trSep = TableRow(requireContext())
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
        }
    }

    private fun refreshRows(iCategory: String, iVisibility: Int) {
        var firstDetailLine: Int
        var lastDetailLine = 0
        mTableLayout = binding.tableDashboardRows
        var tableRow: TableRow?
        do {
            lastDetailLine += 1
            tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TableRow
            val catTV = tableRow.getChildAt(0) as TextView
            val cat = catTV.text.toString()
        } while (tableRow != null && !(tableRow.tag == cSUBTOTAL && cat == iCategory))
        if (tableRow == null) // no detail rows found
            return
        // found sub-total row, now work backwards
        lastDetailLine -= 1
        tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TableRow
        if (tableRow.tag != cDETAIL)  // ie no details for this category
            return
        firstDetailLine = lastDetailLine
        do {
            firstDetailLine -= 1
            tableRow = mTableLayout!!.getChildAt(firstDetailLine) as TableRow
        } while (tableRow != null && tableRow.tag == cDETAIL)
        firstDetailLine += 1
        // now check if section should be expanded or collapsed
        for (i in firstDetailLine..lastDetailLine) {
            tableRow = mTableLayout!!.getChildAt(i) as TableRow
            tableRow.visibility = iVisibility
        }
        if (iVisibility == View.VISIBLE)
            collapsedCategories.remove(iCategory)
        else
            collapsedCategories.add(iCategory)
    }

    private fun onExpandClicked(button: TextView, layout: LinearLayout) {
        if (layout.visibility == View.GONE) { // ie expand the section
            // first hide all other possible expansions
            resetLayout(binding.expandNav, binding.navButtonLinearLayout)
            resetLayout(binding.expandView, binding.viewButtonLinearLayout)
            resetLayout(binding.expandFilter, binding.filterButtonLinearLayout)
            resetLayout(binding.expandOptions, binding.optionsButtonLinearLayout)
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

    private fun setActionBarTitle() {
        binding.dashboardTitle.text =
                when (DefaultsViewModel.getDefaultViewPeriodDashboard()) {
                    cPeriodAllTime -> getString(R.string.dashboard) + " - " +
                            getString(R.string.all_time)
                    cPeriodYTD -> getString(R.string.dashboard) + " - " +
                            getString(R.string.ytd)
                    cPeriodYear -> getString(R.string.dashboard) + " - " +
                            currentBudgetMonth.year
                    else -> getString(R.string.dashboard) + " - " +
                        gMonthName(currentBudgetMonth.month) + " " + currentBudgetMonth.year
                }
        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefaultFilterDiscDashboard() != "") {
            when (DefaultsViewModel.getDefaultFilterDiscDashboard()) {
                cDiscTypeDiscretionary -> currentFilterIndicator = getString(R.string.discretionary)
                cDiscTypeNondiscretionary -> currentFilterIndicator = getString(R.string.non_discretionary)
            }
        }
        if (DefaultsViewModel.getDefaultFilterWhoDashboard() != "")
            currentFilterIndicator = currentFilterIndicator +
                    (if (currentFilterIndicator == "") "" else " ") +
                    SpenderViewModel.getSpenderName(DefaultsViewModel.getDefaultFilterWhoDashboard().toInt())
        if (currentFilterIndicator == "")
            binding.dashboardSubtitle.visibility = View.GONE
        else {
            binding.dashboardSubtitle.visibility = View.VISIBLE
            binding.dashboardSubtitle.text = "($currentFilterIndicator)"
        }
    }

    private fun moveBackward() {
        if (DefaultsViewModel.getDefaultViewPeriodDashboard() != cPeriodYTD &&
            DefaultsViewModel.getDefaultViewPeriodDashboard() != cPeriodAllTime) {
            if (currentBudgetMonth.month == 0)
                currentBudgetMonth.year--
            else
                currentBudgetMonth.decrementMonth()
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
    }

    private fun moveForward() {
        if (DefaultsViewModel.getDefaultViewPeriodDashboard() != cPeriodYTD &&
                DefaultsViewModel.getDefaultViewPeriodDashboard() != cPeriodAllTime) {
            if (currentBudgetMonth.month == 0)
                currentBudgetMonth.year++
            else
                currentBudgetMonth.addMonth()
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isInvisible(iCat: String): Boolean {
        collapsedCategories.forEach {
            val len = if (it.length > iCat.length) iCat.length else it.length
            if (it.substring(0, len) == iCat.substring(0, len))
                return true
        }
        return false
    }
}


class DashboardData {
    var categoryID = 0
    var category = ""
    var subcategory = ""
    var discIndicator = ""
    var budgetAmount: Double = 0.0
    var actualAmount: Double = 0.0
    var priority = 0
}

class DashboardRows {
    fun getRows(
        iBudgetMonth: BudgetMonth,
        iDiscFlag: String = "",
        iBoughtForFlag: Int = 2,
        iViewPeriod: DateRange = DateRange.MONTH
    ): MutableList<DashboardData> {
        val data: MutableList<DashboardData> = mutableListOf()
        val actualTotals = TransactionViewModel.getCategoryActuals(iBudgetMonth, iViewPeriod, iDiscFlag, iBoughtForFlag)
        var i = 1
        actualTotals.forEach {
            i += 1
            val row = DashboardData()
            val catID = it.id
            row.categoryID = catID
            row.category = CategoryViewModel.getCategory(catID)?.categoryName.toString()
            row.subcategory = CategoryViewModel.getCategory(catID)?.subcategoryName.toString()
            row.priority = DefaultsViewModel.getCategoryDetail(row.category).priority
            val expDiscIndicator = CategoryViewModel.getCategory(catID)?.discType
            row.discIndicator =
                if (expDiscIndicator == cDiscTypeDiscretionary) MyApplication.getString(R.string.disc_short)
                    else MyApplication.getString(R.string.non_disc_short)
            row.actualAmount = it.value
            data.add(row)
        }

        // need to get budget categories for which there are budgets but no actuals; but, skip annual budgets
        val catBudgets = BudgetViewModel.getCategoryBudgets(iViewPeriod, iBudgetMonth, iDiscFlag, iBoughtForFlag, "", true)
        for (budget in catBudgets) {
            val dRow: DashboardData? = data.find { it.categoryID == budget.id }
            if (dRow == null) {
                val row = DashboardData()
                row.categoryID = budget.id
                row.category = CategoryViewModel.getCategory(budget.id)?.categoryName.toString()
                row.subcategory = CategoryViewModel.getCategory(budget.id)?.subcategoryName.toString()
                row.discIndicator =
                    if (CategoryViewModel.getCategory(row.categoryID)?.discType
                        == cDiscTypeDiscretionary
                    ) MyApplication.getString(R.string.disc_short)
                    else MyApplication.getString(R.string.non_disc_short)
                row.actualAmount = 0.0
                row.budgetAmount = budget.value
                row.priority = DefaultsViewModel.getCategoryDetail(row.category).priority
                data.add(row)
            } else {
                dRow.budgetAmount = budget.value
            }
        }

        data.sortWith(compareBy({ it.priority }, { it.category }, { it.subcategory }))
        return data
    }
}

class DashboardScrollView(context: Context?, attrs: AttributeSet?) :
    ScrollView(context, attrs) {
    private val mGestureDetector: GestureDetector
    private var leftSwipeCallback: DataUpdatedCallback? = null
    private var rightSwipeCallback: DataUpdatedCallback? = null
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (!mGestureDetector.onTouchEvent(ev))
            super.onInterceptTouchEvent(ev)
        else
            true
    }
    fun setLeftSwipeCallback(iCallback: DataUpdatedCallback?) {
        leftSwipeCallback = iCallback
    }
    fun setRightSwipeCallback(iCallback: DataUpdatedCallback?) {
        rightSwipeCallback = iCallback
    }

    internal inner class YScrollDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val horizontalScroll = abs(distanceY) <= abs(distanceX)
            return if (horizontalScroll) {
                if (distanceX > 0)
                    leftSwipeCallback?.onDataUpdate()
                else
                    rightSwipeCallback?.onDataUpdate()
                true
            } else
                false
        }
    }

    init {
        mGestureDetector = GestureDetector(context, YScrollDetector())
        setFadingEdgeLength(0)
    }
}
