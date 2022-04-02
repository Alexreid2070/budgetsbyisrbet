package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
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
import kotlin.math.round

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
        mTableLayout = binding.tableDashboardRows
        binding.tableDashboardRows.isStretchAllColumns = true

        if (currentBudgetMonth.year == 0) {
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "Year")
                BudgetMonth(dateNow.get(Calendar.YEAR), 0)
            else
                BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_DASHBOARD)) {
            cDiscTypeDiscretionary -> binding.discRadioButton.isChecked = true
            cDiscTypeNondiscretionary -> binding.nonDiscRadioButton.isChecked = true
            else -> binding.allDiscRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_DASHBOARD)) {
            "0" -> binding.name1RadioButton.isChecked = true
            "1" -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_DELTA_DASHBOARD)) {
            "#" -> binding.dollarRadioButton.isChecked = true
            "%" -> binding.percentageRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_ROUND_DASHBOARD)) {
            "true" -> binding.switchRoundToNearestDollar.isChecked = true
            "false" -> binding.switchRoundToNearestDollar.isChecked = false
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_SHOW_DISC_DASHBOARD)) {
            "true" -> binding.showDiscColumn.isChecked = true
            "false" -> binding.showDiscColumn.isChecked = false
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
            DefaultsViewModel.updateDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD, "Month")
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(currentBudgetMonth.year, dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewYtd.setOnClickListener {
            DefaultsViewModel.updateDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD, "YTD")
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewYear.setOnClickListener {
            DefaultsViewModel.updateDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD, "Year")
            currentBudgetMonth.month = 0
            setActionBarTitle()
            startLoadData(currentBudgetMonth)
        }
        binding.buttonViewAllTime.setOnClickListener {
            DefaultsViewModel.updateDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD, "All-Time")
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
                    DefaultsViewModel.updateDefault(cDEFAULT_DELTA_DASHBOARD, "#")
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.percentageRadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_DELTA_DASHBOARD, "%")
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        binding.showDiscColumn.setOnCheckedChangeListener { _, _ ->
            if (binding.showDiscColumn.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_DISC_DASHBOARD, "true")
                startLoadData(currentBudgetMonth)
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_SHOW_DISC_DASHBOARD, "false")
                startLoadData(currentBudgetMonth)
            }
        }
        binding.switchRoundToNearestDollar.setOnCheckedChangeListener { _, _ ->
            if (binding.switchRoundToNearestDollar.isChecked) {
                DefaultsViewModel.updateDefault(cDEFAULT_ROUND_DASHBOARD, "true")
                startLoadData(currentBudgetMonth)
            } else {
                DefaultsViewModel.updateDefault(cDEFAULT_ROUND_DASHBOARD, "false")
                startLoadData(currentBudgetMonth)
            }
        }
        binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.discRadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_DASHBOARD, cDiscTypeDiscretionary)
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.nonDiscRadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_DASHBOARD, cDiscTypeNondiscretionary)
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
                R.id.allDiscRadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_DASHBOARD, "")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.name1RadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_DASHBOARD, "0")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                    // do something when radio button 1 is selected
                }
                R.id.name2RadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_DASHBOARD, "1")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
                R.id.whoAllRadioButton -> {
                    DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_DASHBOARD, "")
                    setActionBarTitle()
                    startLoadData(currentBudgetMonth)
                }
            }
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD)) {
            "Month" -> binding.buttonViewMonth.isChecked = true
            "YTD" -> binding.buttonViewYtd.isChecked = true
            "Year" -> binding.buttonViewYear.isChecked = true
            "All-Time" -> binding.buttonViewAllTime.isChecked = true
        }
        binding.resetFilterButton.setOnClickListener {
            DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_DASHBOARD, "")
            binding.allDiscRadioButton.isChecked = true
            DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_DASHBOARD, "")
            binding.whoAllRadioButton.isChecked = true
            onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
        }
        binding.scrollView.setLeftSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "in left swipe  callback")
                moveForward()
            }
        })
        binding.scrollView.setRightSwipeCallback(object: DataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "in right swipe  callback")
                moveBackward()
            }
        })
        setActionBarTitle()
        HintViewModel.showHint(requireContext(), binding.resetFilterButton, "Dashboard")
    }

    private fun startLoadData(iBudgetMonth: BudgetMonth) {
        Log.d("Alex", "startLoadData filters for $iBudgetMonth")
        val dashboardRows = DashboardRows()
        val data: MutableList<DashboardData> = dashboardRows.getRows(iBudgetMonth,
            DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_DASHBOARD),
            DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_DASHBOARD),
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD))

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
        createViewRow("Header", i++, 0, "", "", "", 0.0, 0.0)

        while (i < rows) {
            var row: DashboardData? = null
            if (i > -1) row = data[i] else {
                textSpacer = TextView(requireContext())
                textSpacer.text = ""
            }

            if (row != null && lastCategory != "" && row.category != lastCategory) {
                // sub-total row
                createViewRow("Sub-total", i, 0, lastCategory, "", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
                grandBudgetTotal += lastCategoryBudgetTotal
                grandActualTotal += lastCategoryActualTotal
                lastCategoryBudgetTotal = 0.0
                lastCategoryActualTotal = 0.0
            }

            if (row != null) {
                lastCategory = row.category
                createViewRow("Detail", i,
                    CategoryViewModel.getID(row.category, row.subcategory),
                    row.category, row.subcategory, row.discIndicator, row.budgetAmount, row.actualAmount)
                lastCategoryBudgetTotal += row.budgetAmount
                lastCategoryActualTotal += row.actualAmount
            }
            i++
        }

        createViewRow("Sub-total", i++, 0, lastCategory, "", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
        grandBudgetTotal += lastCategoryBudgetTotal
        grandActualTotal += lastCategoryActualTotal
        createViewRow("Grand total", i++, 0, "Grand Total", "", "", grandBudgetTotal, grandActualTotal)
        // delta row
        if (grandActualTotal > grandBudgetTotal) {
            createViewRow("Delta", i, 0, "Delta", "", "", 0.0, grandActualTotal-grandBudgetTotal)
        }
        else {
            createViewRow("Delta", i, 0, "Delta", "", "", grandBudgetTotal-grandActualTotal,0.0)
        }
        val run = Runnable {
            val tableHeaderRow = binding.tableHeaderRow.getChildAt(0) as TableRow
            val tableDashboardRow = binding.tableDashboardRows.getChildAt(0) as TableRow
            for (i in 0 until tableHeaderRow.childCount) {
                tableHeaderRow.getChildAt(i).layoutParams = TableRow.LayoutParams(
                    tableDashboardRow.getChildAt(i).measuredWidth,
                    tableDashboardRow.getChildAt(i).measuredHeight
                )
                Log.d(
                    "Alex",
                    "set width to " + tableDashboardRow.getChildAt(i).measuredWidth + " and height to " + tableDashboardRow.getChildAt(
                        i
                    ).measuredHeight
                )
            }
        }
        binding.tableDashboardRows.post(run)
    }

    private fun getDecFormat(iDouble: Double): String {
        return if (DefaultsViewModel.getDefault(cDEFAULT_ROUND_DASHBOARD) == "true") {
            val dformat = DecimalFormat("###0;(###0)")
            dformat.format(round(iDouble))
        } else {
            val dformat = DecimalFormat("###0.00;(###0.00)")
            dformat.format(iDouble)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun createViewRow(iRowType: String, iRowNo: Int, iCategoryID: Int, iCategory: String, iSubcategory: String, iDiscFlag: String, iBudgetAmount: Double, iActualAmount:Double) {
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
        tv1.gravity = if (iRowType == "Detail") Gravity.START else Gravity.END
        tv1.setPadding(15, 15, 0, 15)
        if (iRowType == "Header") {
            tv1.text = ""
        } else {
            when (iRowType) {
                "Detail" -> tv1.text = iSubcategory
                "Sub-total" -> {
                    tv1.text = "$iCategory Total"
                    tv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24, 0, 0, 0);                }
                else -> tv1.text = iCategory
            }
            if (tv1.text.length > 15) {
                tv1.text = tv1.text.substring(0,15) + "..."
            }
        }
        tv1.tag = "Expanded"
        val tv2 = TextView(requireContext())
        tv2.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        if (iRowType == "Sub-total") {
            tv2.gravity = Gravity.CENTER
            tv2.setPadding(0, 0, 0, 0)
        } else {
            tv2.gravity = Gravity.START
            tv2.setPadding(5, 15, 0, 15)
        }
        if (iRowType == "Header") {
            tv2.text = "D?"
            tv2.tooltipText = "Indicates whether this budget item is Discretionary (D), or not (ND)."
        } else {
            tv2.text = iDiscFlag
        }
        if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_DISC_DASHBOARD) == "false")
            tv2.visibility = View.GONE
        val tv3 = TextView(requireContext())
        if (iRowType == "Header") {
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
        if (iRowType == "Header") {
            tv3.text = "Budget"
        } else {
            if (iRowType == "Delta" && iBudgetAmount == 0.0)
                tv3.text = ""
            else
                tv3.text = getDecFormat(iBudgetAmount)
        }
        tv3.tag = iCategoryID
        val tv4 = TextView(requireContext())
        if (iRowType == "Header") {
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
        if (iRowType == "Header") {
            tv4.text = "Actual"
        } else {
            if (iRowType == "Delta" && iActualAmount == 0.0)
                tv4.text = ""
            else
                tv4.text = getDecFormat(iActualAmount)
        }
        tv4.tag = iCategoryID
        val tv5 = TextView(requireContext())
        if (iRowType == "Header") {
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
        if (iRowType == "Header") {
            tv5.text = "Delta"
        } else {
            if (iRowType != "Delta") {
                if (DefaultsViewModel.getDefault(cDEFAULT_DELTA_DASHBOARD) == "%") {
                    val percentFormat = java.text.DecimalFormat("# %")
                    when {
                        iActualAmount == 0.0 -> tv5.text = "0 %"
                        iBudgetAmount == 0.0 -> tv5.text = "INF %"
                        else -> {
                            tv5.text = percentFormat.format(iActualAmount / iBudgetAmount)
                        }
                    }
                } else {
                    val diff = BigDecimal(iBudgetAmount - iActualAmount).setScale(2, RoundingMode.HALF_EVEN)
                    tv5.text = getDecFormat(diff.toDouble())
                }
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
            "Sub-total" -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 20)
                tr.setPadding(5, 0, 0, 0)
            }
            "Grand total" -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, 50)
                tr.setPadding(5, 0, 5, 0)
            }
            else -> {
                trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
                tr.setPadding(5, 0, 0, 0)
            }
        }
        tr.layoutParams = trParams
        if (iRowType == "Detail") {
            tv5.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))
            tr.setBackgroundResource(R.drawable.row_left_border_no_color)
            if (Build.VERSION.SDK_INT >= 29) {
                val cat = DefaultsViewModel.getCategoryDetail(iCategory)
                if (cat.color != 0) {
                    tr.setBackgroundResource(R.drawable.row_left_border)
                    tr.background.colorFilter =
                        BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                }
//                tr.background.colorFilter = BlendModeColorFilter(Color.parseColor("#123456"), BlendMode.SRC_ATOP)
            }
        }
        else if (iRowType == "Header") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
        }
        else if (iRowType == "Sub-total") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))
            tr.setBackgroundResource(R.drawable.row_left_and_bottom_border_no_color)
            if (Build.VERSION.SDK_INT >= 29) {
                val cat = DefaultsViewModel.getCategoryDetail(iCategory)
                if (cat.color != 0) {
                    tr.setBackgroundResource(R.drawable.row_left_and_bottom_border)
                    tr.background.colorFilter =
                        BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                }
            }
        }
        else if (iRowType == "Grand total") {
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
                tr.setBackgroundResource(R.drawable.row_frame_no_color)
            }
            if (iActualAmount > iBudgetAmount)
                tv1.text = "Over Budget"
            else
                tv1.text = "Under Budget"
        } else if (iRowType == "Delta") {
            if (tv3.text != "") {
                tv1.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))
                tv1.text = "Under Budget"
                tv3.visibility = View.GONE
            } else if (tv4.text != "") {
                tv1.setTextColor(getBudgetColour(requireContext(), iActualAmount, iBudgetAmount, true))
                tv1.text = "Over Budget"
                tv4.visibility = View.GONE
            }
        }
        tr.addView(tv0)
        tr.addView(tv1)
        tr.addView(tv2)
        tr.addView(tv3)
        tr.addView(tv4)
        tr.addView(tv5)
        if (isInvisible(iCategory)) {
            tv1.tag = "Collapsed"
            if (iRowType == "Detail") {
                tr.visibility = View.GONE
            }  else if (iRowType == "Sub-total") {
                tv1.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
            }
        } else
            tr.visibility = View.VISIBLE

        if (iRowType != "Header" && iRowType != "Sub-total" && iRowType != "Grand total" && iRowType != "Delta") {
            tv3.setOnClickListener {
                val catID = it.tag.toString()
                Log.d("Alex", "row " + it.id + " was clicked, category is " + catID)
                // go to Budget
                val action =
                    DashboardFragmentDirections.actionDashboardFragmentToBudgetViewAllFragment()
                action.categoryID = catID
                findNavController().navigate(action)
            }

            tv4.setOnClickListener {
                val catID = it.tag.toString().toInt()
                val cat = CategoryViewModel.getCategory(catID)
                Log.d("Alex", "row " + it.id + " was clicked, category is " + catID)
                // go to ViewAll with the SubCategory as the search term
                MyApplication.transactionSearchText = cat?.categoryName + " " + cat?.subcategoryName
                if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "Month" ||
                    DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "Year")
                    MyApplication.transactionSearchText = MyApplication.transactionSearchText + " " + currentBudgetMonth.year.toString()
                MyApplication.transactionSearchText = MyApplication.transactionSearchText.replace("...","")
                if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "Month") {
                    if (currentBudgetMonth.month < 10)
                        MyApplication.transactionSearchText += "-0" + currentBudgetMonth.month.toString()
                    else
                        MyApplication.transactionSearchText += "-" + currentBudgetMonth.month.toString()
                }
                view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
            }
        } else if (iRowType == "Sub-total") {
            tr.setOnClickListener {
                Log.d("Alex", "header was clicked")
                val tableRow = it as TableRow
                val ttv1 = tableRow.getChildAt(1) as TextView
                val ttv2 = tableRow.getChildAt(2) as TextView
                Log.d("Alex", "header was clicked " + ttv2.text.toString())
                var tmpCat = ttv1.text.toString().replace(" Total","")
                tmpCat = tmpCat.replace("...","")
                tmpCat = tmpCat.trim()
                if (ttv1.tag.toString() == "Collapsed") {
                    ttv1.tag = "Expanded"
                    ttv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    refreshRows(tmpCat, View.VISIBLE)
                } else {
                    ttv1.tag = "Collapsed"
                    ttv1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    refreshRows(tmpCat, View.GONE)
                }
            }
        }
        if (iRowType == "Grand total")
            mTableLayout!!.addView(tr,0)
        else if (iRowType == "Delta")  // I've removed the Delta row, at least for now
//            mTableLayout!!.addView(tr, 2)
        else {
            if (iRowType == "Header")
                binding.tableHeaderRow.addView(tr)
            else
                mTableLayout!!.addView(tr)
        }
        if (iRowType != "Header") {
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
        } while (tableRow != null && !(tableRow.tag == "Sub-total" && cat == iCategory))
        if (tableRow == null) // no detail rows found
            return
        // found sub-total row, now work backwards
        lastDetailLine -= 1
        tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TableRow
        if (tableRow.tag != "Detail")  // ie no details for this category
            return
        firstDetailLine = lastDetailLine
        do {
            firstDetailLine -= 1
            tableRow = mTableLayout!!.getChildAt(firstDetailLine) as TableRow
        } while (tableRow != null && tableRow.tag == "Detail")
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
        Log.d("Alex", "onExpandClicked")
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

    @SuppressLint("SetTextI18n")
    private fun setActionBarTitle() {
        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_DASHBOARD) != "")
            currentFilterIndicator = " " + DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_DASHBOARD).substring(0,5)
        if (DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_DASHBOARD) != "")
            currentFilterIndicator = currentFilterIndicator + " " + SpenderViewModel.getSpenderName(DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_DASHBOARD).toInt())
        when {
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "All-Time" -> binding.dashboardTitle.text =
                "Dashboard (All-Time$currentFilterIndicator)"
            currentBudgetMonth.month == 0 -> binding.dashboardTitle.text = "Dashboard (" + currentBudgetMonth.year  + currentFilterIndicator + ")"
            else -> {
                binding.dashboardTitle.text = "Dashboard (" + MonthNames[currentBudgetMonth.month - 1] +
                        " " + currentBudgetMonth.year
                if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) == "YTD")
                    binding.dashboardTitle.text = binding.dashboardTitle.text.toString() + " YTD"
                binding.dashboardTitle.text = binding.dashboardTitle.text.toString() + currentFilterIndicator + ")"
            }
        }
    }

    private fun moveBackward() {
        if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) != "YTD" &&
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) != "All-Time") {
            if (currentBudgetMonth.month == 0)
                currentBudgetMonth.year--
            else
                currentBudgetMonth.decrementMonth()
            setActionBarTitle()
            Log.d("Alex", "In backward, loading $currentBudgetMonth")
            startLoadData(currentBudgetMonth)
        }
    }

    private fun moveForward() {
        if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) != "YTD" &&
                DefaultsViewModel.getDefault(cDEFAULT_VIEW_PERIOD_DASHBOARD) != "All-Time") {
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
        iBoughtForFlag: String = "",
        iViewPeriod: String = "Month"
    ): MutableList<DashboardData> {
        Log.d("Alex", "iviewperiod is $iViewPeriod")
        val data: MutableList<DashboardData> = mutableListOf()
        val startDate: String
        val endDate: String
        val boughtForFlag = when (iBoughtForFlag) {
            "0" -> 0
            "1" -> 1
            else -> -1
        }
        if (iViewPeriod == "All-Time") {
            startDate = "0000-00-00"
            endDate = "9999-99-99"
        } else if (iViewPeriod == "YTD") {
            startDate = iBudgetMonth.year.toString() + "-00-00"
            endDate = if (iBudgetMonth.month < 10) {
                iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-99"
            } else {
                iBudgetMonth.year.toString() + "-" + iBudgetMonth.month.toString() + "-99"
            }
        } else if (iViewPeriod == "Year") { //(iBudgetMonth.month == 0) {
            startDate = iBudgetMonth.year.toString() + "-00-00"
            endDate = iBudgetMonth.year.toString() + "-99-99"
            Log.d("Alex", "start date is $startDate and enddate is $endDate")
        } else {
            if (iBudgetMonth.month < 10) {
                startDate =
                    iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-00"
                endDate =
                    iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-99"
            } else {
                startDate =
                    iBudgetMonth.year.toString() + "-" + iBudgetMonth.month.toString() + "-00"
                endDate = iBudgetMonth.year.toString() + "-" + iBudgetMonth.month.toString() + "-99"
            }
        }

        for (i in 0 until ExpenditureViewModel.getCount()) {
            val expenditure = ExpenditureViewModel.getExpenditure(i)
            if (expenditure.date > startDate && expenditure.date < endDate) {
                val expDiscIndicator = CategoryViewModel.getCategory(expenditure.category)?.discType
                if (expenditure.type != "Transfer") {
                    if (iDiscFlag == "" || iDiscFlag == expDiscIndicator) {
                            if (boughtForFlag == -1 || expenditure.boughtfor == boughtForFlag || expenditure.boughtfor == 2) {
                                // this is a transaction to add to our subtotal
                                var multiplier = 1.0
                                if (boughtForFlag != -1) {
                                    multiplier = if (expenditure.boughtfor == 2 || expenditure.boughtfor == boughtForFlag) {
                                        if (boughtForFlag == 0)
                                            expenditure.bfname1split.toDouble() / 100
                                        else
                                            expenditure.getSplit2().toDouble() / 100
                                    } else
                                        0.0
                                }
                                val bdRow: DashboardData? =
                                    data.find { it.categoryID == expenditure.category }
                                if (bdRow == null) {
                                    val row = DashboardData()
                                    row.categoryID = expenditure.category
                                    row.category = CategoryViewModel.getCategory(expenditure.category)?.categoryName.toString()
                                    row.subcategory = CategoryViewModel.getCategory(expenditure.category)?.subcategoryName.toString()
                                    row.priority = DefaultsViewModel.getCategoryDetail(row.category).priority
                                    if (CategoryViewModel.getCategory(expenditure.category) == null) {
                                        Log.d("Alex", "Found null for exp ${expenditure.mykey}")
                                    }
                                    row.discIndicator =
                                        if (expDiscIndicator == cDiscTypeDiscretionary) "D" else "ND"
                                    row.actualAmount =
                                        expenditure.amount.toDouble() / 100 * multiplier
                                    data.add(row)
                                } else {
                                    bdRow.actualAmount += (expenditure.amount.toDouble() / 100 * multiplier)
                                }
                        }
                    }
                }
            }
        }
        // need to get budget categories for which there are budgets but no actuals; but, skip annual budgets
        val tBudgetCategories = BudgetViewModel.getBudgetCategories(iBudgetMonth, iDiscFlag)
        for (i in 0 until tBudgetCategories.size) {
            val dRow: DashboardData? = data.find {
                it.categoryID == tBudgetCategories[i]
            }
            if (dRow == null) {
                // add the row.  There's a budget for it, but no actuals
                val row = DashboardData()
                row.categoryID = tBudgetCategories[i]
                row.category = CategoryViewModel.getCategory(tBudgetCategories[i])?.categoryName.toString()
                row.subcategory = CategoryViewModel.getCategory(tBudgetCategories[i])?.subcategoryName.toString()
                row.discIndicator =
                    if (CategoryViewModel.getCategory(row.categoryID)?.discType
                        == cDiscTypeDiscretionary
                    ) "D" else "ND"
                row.actualAmount = 0.0
                row.priority = DefaultsViewModel.getCategoryDetail(row.category).priority
                data.add(row)
            }
        }
        // add budget amounts
        var whoToLookup = boughtForFlag
        if (whoToLookup == -1) {
            if (SpenderViewModel.singleUser())
                whoToLookup = 0
 //           else
 //               whoToLookup = "Joint"
        }
        Log.d("Alex", "who is $whoToLookup")
        data.forEach {
            if (iViewPeriod == "All-Time") {
                val dateNow = android.icu.util.Calendar.getInstance()
                val lastAnnualYear = if (dateNow.get(Calendar.MONTH) == 11) // ie Dec
                    dateNow.get(Calendar.YEAR)
                else
                    dateNow.get(Calendar.YEAR)-1
                // add annual budgets for previous years, and current year if it is Dec
                val earliestYear = ExpenditureViewModel.getEarliestYear()
                for (i in earliestYear until lastAnnualYear+1) {
                    it.budgetAmount += BudgetViewModel.getCalculatedBudgetAmount(
                        BudgetMonth(i, 0),
                        it.categoryID,
                        whoToLookup
                    )
                }
                // then add YTD for current year (if it's not Dec)
                if (dateNow.get(Calendar.MONTH) != 11) {
                    for (i in 1 until iBudgetMonth.month + 1) {
                        it.budgetAmount += BudgetViewModel.getCalculatedBudgetAmount(
                            BudgetMonth(
                                iBudgetMonth.year,
                                i
                            ),
                            it.categoryID,
                            whoToLookup
                        )
                    }
                }
            } else if (iViewPeriod == "YTD") {
                for (i in 1 until iBudgetMonth.month + 1) {
                    it.budgetAmount += BudgetViewModel.getCalculatedBudgetAmount(
                        BudgetMonth(
                            iBudgetMonth.year,
                            i
                        ),
                        it.categoryID,
                        whoToLookup)
                }
            } else {
                it.budgetAmount = BudgetViewModel.getCalculatedBudgetAmount(
                    iBudgetMonth,
                    it.categoryID,
                    whoToLookup
                )
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
        Log.d("Alex", "mGestureDetector is $mGestureDetector")
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
                Log.d("Alex", "scrolling horizontally $distanceX")
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
