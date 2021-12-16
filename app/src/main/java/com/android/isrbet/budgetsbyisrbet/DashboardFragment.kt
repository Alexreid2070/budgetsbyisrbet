package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.icu.util.Calendar
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentDashboardBinding
import android.widget.*
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import android.widget.TableLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var mTableLayout: TableLayout? = null
    private var currentBudgetMonth: BudgetMonth = BudgetMonth(0,0)
    private var currentRecFilter: String = ""
    private var currentDiscFilter: String = ""
    private var currentWhoFilter: String = ""
    private var currentPeriodView: String = "Month"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun startLoadData(iBudgetMonth: BudgetMonth, iRecFlag: String = "", iDiscFlag: String = "", iWhoFlag: String = "") {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0
        val dashboardRows = DashboardRows()
        val data: MutableList<DashboardData> = dashboardRows.getRows(iBudgetMonth, iRecFlag, iDiscFlag, iWhoFlag)

        val decimalFormat = DecimalFormat("0.00")
        val percentFormat = DecimalFormat("0.00%")
        val rows = data.size
        var textSpacer: TextView? = null
        mTableLayout!!.removeAllViews()
        var lastCategory: String = ""
        var lastCategoryBudgetTotal: Double = 0.0
        var lastCategoryActualTotal: Double = 0.0
        var grandBudgetTotal: Double = 0.0
        var grandActualTotal: Double = 0.0

        // -1 means heading row
        var i = -1
        // do header row
        createViewRow("Header", i++, "", "", 0.0, 0.0)

        while (i < rows) {
            var row: DashboardData? = null
            if (i > -1) row = data[i] else {
                textSpacer = TextView(requireContext())
                textSpacer.text = ""
            }

            if (row != null && lastCategory != "" && row.category != lastCategory) {
                // sub-total row
                createViewRow("Sub-total", i, lastCategory + " Total", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
                grandBudgetTotal += lastCategoryBudgetTotal
                grandActualTotal += lastCategoryActualTotal
                lastCategoryBudgetTotal = 0.0
                lastCategoryActualTotal = 0.0
            }

            if (row != null) {
                lastCategory = row.category
                createViewRow("Detail", i, row.category + "-" + row.subcategory, row.discIndicator, row.budgetAmount, row.actualAmount)
                lastCategoryBudgetTotal += row.budgetAmount
                lastCategoryActualTotal += row.actualAmount
            }
            i++
        }

        createViewRow("Sub-total", i++, lastCategory + " Total", "", lastCategoryBudgetTotal, lastCategoryActualTotal)
        grandBudgetTotal += lastCategoryBudgetTotal
        grandActualTotal += lastCategoryActualTotal
        createViewRow("Grand total", i++, "Grand Total", "", grandBudgetTotal, grandActualTotal)
        // delta row
        if (grandActualTotal > grandBudgetTotal) {
            createViewRow("Delta", i++, "Delta", "", 0.0, grandActualTotal-grandBudgetTotal)
        }
        else {
            createViewRow("Delta", i++, "Delta", "", grandBudgetTotal-grandActualTotal,0.0)
        }
    }

    private fun createViewRow(iRowType: String, iRowNo: Int, iCategory: String, iDiscFlag: String, iBudgetAmount: Double, iActualAmount:Double) {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0
        val decimalFormat = DecimalFormat("0.00")
        val percentFormat = DecimalFormat("0.00%")
        var pct: Double = 0.0

        val tv1 = TextView(requireContext())
        tv1.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        if (iRowType == "Detail")
            tv1.gravity = Gravity.LEFT
        else
            tv1.gravity = Gravity.RIGHT
        tv1.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv1.text = ""
            tv1.setBackgroundColor(Color.parseColor("#f7f7f7"))
        } else {
            tv1.setBackgroundColor(Color.parseColor("#ffffff"))
            val dash = iCategory.indexOf("-")
            if (dash > -1)
                tv1.setText(iCategory.substring(dash+1,iCategory.length))
            else
                tv1.setText(iCategory)
        }
        val tv2 = TextView(requireContext())
        tv2.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        tv2.gravity = Gravity.LEFT
        tv2.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv2.text = "Disc?"
            tv2.setBackgroundColor(Color.parseColor("#f7f7f7"))
        } else {
            tv2.setBackgroundColor(Color.parseColor("#ffffff"))
            tv2.setText(iDiscFlag)
        }
        val tv3 = TextView(requireContext())
        if (iRowType == "Header") {
            tv3.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv3.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
        }
        tv3.gravity = Gravity.RIGHT
        tv3.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv3.text = "Budget"
            tv3.setBackgroundColor(Color.parseColor("#f7f7f7"))
        } else {
            tv3.setBackgroundColor(Color.parseColor("#ffffff"))
            tv3.setTextColor(Color.parseColor("#000000"))
            if (iRowType == "Delta" && iBudgetAmount == 0.0)
                tv3.setText("")
            else
                tv3.setText(decimalFormat.format(iBudgetAmount))
        }
        val tv4 = TextView(requireContext())
        if (iRowType == "Header") {
            tv4.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv4.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
        }
        tv4.gravity = Gravity.RIGHT
        tv4.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv4.text = "Actual"
            tv4.setBackgroundColor(Color.parseColor("#f7f7f7"))
        } else {
            tv4.setBackgroundColor(Color.parseColor("#ffffff"))
            tv4.setTextColor(Color.parseColor("#000000"))
            if (iRowType == "Delta" && iActualAmount == 0.0)
                tv4.setText("")
            else
                tv4.setText(decimalFormat.format(iActualAmount))
        }
        val tv5 = TextView(requireContext())
        if (iRowType == "Header") {
            tv5.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        } else {
            tv5.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
        }
        tv5.gravity = Gravity.RIGHT
        tv5.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv5.text = "%"
            tv5.setBackgroundColor(Color.parseColor("#f7f7f7"))
        } else {
            tv5.setBackgroundColor(Color.parseColor("#ffffff"))
            tv5.setTextColor(Color.parseColor("#000000"))
            pct =
                if (iActualAmount == 0.0 || iBudgetAmount == 0.0) 0.0
                else iActualAmount / iBudgetAmount
            if (iRowType != "Delta")
               tv5.setText(percentFormat.format(pct))
        }
        // add table row
        val tr = TableRow(requireContext())
        tr.id = iRowNo + 1
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        tr.setPadding(0, 0, 0, 0)
        tr.layoutParams = trParams
        if (iRowType == "Detail") {
            val color = getBudgetColour(iActualAmount, iBudgetAmount)
            tv5.setBackgroundColor(color)
        }
        else if (iRowType == "Header") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
            tv1.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv2.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv3.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv4.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv5.setBackgroundColor(Color.parseColor("#f8f8f8"))
        }
        else if (iRowType == "Sub-total") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv1.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv2.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv3.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv4.setBackgroundColor(Color.parseColor("#f8f8f8"))
            tv5.setBackgroundColor(Color.parseColor("#f8f8f8"))
        }
        else if (iRowType == "Grand total") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F); // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F); // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F); // 14F is default
            tv1.setBackgroundColor(Color.parseColor("#C0C0C0"))
            tv2.setBackgroundColor(Color.parseColor("#C0C0C0"))
            tv3.setBackgroundColor(Color.parseColor("#C0C0C0"))
            tv4.setBackgroundColor(Color.parseColor("#C0C0C0"))
            tv5.setBackgroundColor(Color.parseColor("#C0C0C0"))
        } else if (iRowType == "Delta") {
            if (tv3.text != "") {
                tv1.text = "Under Budget"
                tv3.setBackgroundColor(Color.GREEN)
            } else if (tv4.text != "") {
                tv1.text = "Over Budget"
                tv4.setBackgroundColor(Color.RED)
            }
        }
        tr.addView(tv1)
        tr.addView(tv2)
        tr.addView(tv3)
        tr.addView(tv4)
        tr.addView(tv5)
        if (iRowType != "Header") {
            tr.setOnClickListener {
                Log.d("Alex", "row " + it.id + " was clicked")
                // go to ViewAll with the SubCategory as the search term
                val tableRow = it as TableRow
                val textView = tableRow.getChildAt(0) as TextView
                MyApplication.transactionSearchText = textView.text.toString() + " " + currentBudgetMonth.year.toString()
                if (currentBudgetMonth.month != 0)
                    MyApplication.transactionSearchText += "-" + currentBudgetMonth.month.toString()
                view?.findNavController()?.navigate(R.id.ViewTransactionsFragment)
            }
        }
        mTableLayout!!.addView(tr, trParams)
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
            val tvSep = TextView(requireContext())
            val tvSepLay: TableRow.LayoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        getView()?.findViewById<Button>(R.id.button_backward)?.setOnClickListener { view: View ->
            moveOneMonthBackward()
        }
        getView()?.findViewById<Button>(R.id.button_forward)?.setOnClickListener { view: View ->
            moveOneMonthForward()
        }
        getView()?.findViewById<Button>(R.id.button_by_month)?.setOnClickListener { view: View ->
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
        }
        getView()?.findViewById<Button>(R.id.button_by_year)?.setOnClickListener { view: View ->
            currentBudgetMonth.month = 0
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
        }
        mTableLayout = requireActivity().findViewById(R.id.table_dashboard_rows) as TableLayout
        mTableLayout!!.setStretchAllColumns(true);

        val dateNow = Calendar.getInstance()
        currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
        setActionBarTitle()
        startLoadData(currentBudgetMonth)

        val vButtonLinearLayout =         getView()?.findViewById<LinearLayout>(R.id.button_linear_layout2)
        vButtonLinearLayout?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                moveOneMonthForward()
                Log.d("Alex", "swiped left")
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                moveOneMonthBackward()
                Log.d("Alex", "swiped right")
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.FilterRecurring) {
                menu.getItem(i).setVisible(true)
                if (currentRecFilter == "Recurring")
                    menu.getItem(i).setChecked(true)
                else
                    menu.getItem(i).setChecked(false)
            } else if (menu.getItem(i).getItemId() === R.id.FilterDiscretionary) {
                menu.getItem(i).setVisible(true)
                if (currentDiscFilter == "Discretionary")
                    menu.getItem(i).setChecked(true)
                else
                    menu.getItem(i).setChecked(false)
            } else if (menu.getItem(i).getItemId() === R.id.FilterNonDiscretionary) {
                menu.getItem(i).setVisible(true)
                if (currentDiscFilter == "Non-Discretionary") {
                    menu.getItem(i).setChecked(true)
                } else {
                    menu.getItem(i).setChecked(false)
                }
            } else if (menu.getItem(i).getItemId() === R.id.FilterAlex) {
                menu.getItem(i).setVisible(true)
                if (currentWhoFilter == "Alex")
                    menu.getItem(i).setChecked(true)
                else
                    menu.getItem(i).setChecked(false)
            } else if (menu.getItem(i).getItemId() === R.id.FilterBrent) {
                menu.getItem(i).setVisible(true)
                if (currentWhoFilter == "Brent") {
                    menu.getItem(i).setChecked(true)
                } else {
                    menu.getItem(i).setChecked(false)
                }
            } else if (menu.getItem(i).getItemId() === R.id.FilterTitle) {
                menu.getItem(i).setVisible(true)
            } else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.FilterRecurring) {
            if (currentRecFilter == "Recurring") {
                item.setChecked(false)
                currentRecFilter = ""
            } else {
                item.setChecked(true)
                currentRecFilter = "Recurring"
            }
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
            return true
        } else if (item.itemId === R.id.FilterDiscretionary) {
            if (currentDiscFilter == "Discretionary") {
                item.setChecked(false)
                currentDiscFilter = ""
            } else {
                item.setChecked(true)
                currentDiscFilter = "Discretionary"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
            return true
        } else if (item.itemId === R.id.FilterNonDiscretionary) {
            if (currentDiscFilter == "Non-Discretionary") {
                item.setChecked(false)
                currentDiscFilter = ""
            } else {
                item.setChecked(true)
                currentDiscFilter = "Non-Discretionary"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
            return true
        } else if (item.itemId === R.id.FilterAlex) {
            if (currentWhoFilter == "Alex") {
                item.setChecked(false)
                currentWhoFilter = ""
            } else {
                item.setChecked(true)
                currentWhoFilter = "Alex"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
            return true
        } else if (item.itemId === R.id.FilterBrent) {
            if (currentWhoFilter == "Brent") {
                item.setChecked(false)
                currentWhoFilter = ""
            } else {
                item.setChecked(true)
                currentWhoFilter = "Brent"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun setActionBarTitle() {
        var currentFilterIndicator: String = ""
        if (currentRecFilter != "" || currentDiscFilter != "" || currentWhoFilter != "")
            currentFilterIndicator = " FILTERED! "

        if (currentBudgetMonth.month == 0)
            (activity as AppCompatActivity).supportActionBar?.title =
                "Dashboard (" + currentBudgetMonth.year  + currentFilterIndicator + ")"
        else
            (activity as AppCompatActivity).supportActionBar?.title =
                "Dashboard (" + MonthNames[currentBudgetMonth.month-1] + " " + currentBudgetMonth.year  + currentFilterIndicator + ")"
    }

    fun moveOneMonthBackward() {
        if (currentBudgetMonth.month == 0)
            currentBudgetMonth.year--
        else
            currentBudgetMonth.decrementMonth()
        setActionBarTitle()
        Log.d("Alex", "In backward, loading " + currentBudgetMonth.toString())
        startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)

    }

    fun moveOneMonthForward() {
        if (currentBudgetMonth.month == 0)
            currentBudgetMonth.year++
        else
            currentBudgetMonth.addMonth()
        setActionBarTitle()
        startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentWhoFilter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class DashboardData {
    var category = ""
    var subcategory = ""
    var discIndicator = ""
    var budgetAmount: Double = 0.0
    var actualAmount: Double = 0.0
}

class DashboardRows {
    fun getRows(iBudgetMonth: BudgetMonth, iRecFlag: String = "", iDiscFlag: String = "", iWhoFlag: String = ""): MutableList<DashboardData> {
        val data: MutableList<DashboardData> = mutableListOf<DashboardData>()
        var expenditures = ExpenditureViewModel.getExpenditures()
        var startDate: String
        var endDate: String
        if (iBudgetMonth.month == 0) {
            startDate = iBudgetMonth.year.toString() + "-00-00"
            endDate = iBudgetMonth.year.toString() + "-99-99"
        } else {
            if (iBudgetMonth.month < 10) {
                startDate = iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-00"
                endDate = iBudgetMonth.year.toString() + "-0" + iBudgetMonth.month.toString() + "-99"
            } else {
                startDate = iBudgetMonth.year.toString() + "-" + iBudgetMonth.month.toString() + "-00"
                endDate = iBudgetMonth.year.toString() + "-" + iBudgetMonth.month.toString() + "-99"
            }
        }
        Log.d("Alex", "start date is " + startDate + " and end date is " + endDate + " iDiscFlag is '" + iDiscFlag + "' and iWhoFlag is " + iWhoFlag)

        loop@ for (expenditure in expenditures) {
            if (expenditure.date > startDate && expenditure.date < endDate) {
                val expDiscIndicator = CategoryViewModel.getDiscretionaryIndicator(
                    expenditure.category,
                    expenditure.subcategory
                )
                if (iRecFlag == "" || (iRecFlag == "Recurring" && expenditure.type == "R")) {
                    if (iDiscFlag == "" || iDiscFlag == expDiscIndicator) {
                        if (iWhoFlag == "" || expenditure.who == iWhoFlag || expenditure.who == "Joint") {
                            // this is a transaction to add to our subtotal
                            var multiplier = 1.0
                            if (iWhoFlag != "") {
                                if (expenditure.who == "Joint") {
                                    val spender = SpenderViewModel.getSpender(iWhoFlag)
                                    if (spender != null)
                                        multiplier = spender.split.toDouble() / 100
                                } else if (iWhoFlag != expenditure.who) {
                                    multiplier = 0.0
                                }
                            }
                            val bdRow: DashboardData? =
                                data.find { it.category == expenditure.category && it.subcategory == expenditure.subcategory }
                            if (bdRow == null) {
                                val row = DashboardData()
                                row.category = expenditure.category
                                row.subcategory = expenditure.subcategory
                                if (expDiscIndicator == "Discretionary")
                                    row.discIndicator = "D"
                                else
                                    row.discIndicator = "ND"
                                row.actualAmount = expenditure.amount.toDouble() / 100 * multiplier
                                data.add(row)
                            } else {
                                bdRow.actualAmount += (expenditure.amount.toDouble() / 100 * multiplier)
                            }
                        }
                    }
                }
            }
        }
        // need to get budget categories for which there were no actuals
        var tBudgetCategories = BudgetViewModel.getBudgetCategories(iBudgetMonth, iDiscFlag)
        for (i in 0..tBudgetCategories.size - 1) {
            val dash = tBudgetCategories[i].indexOf("-")
            val dRow: DashboardData? = data.find {
                it.category == tBudgetCategories[i].substring(0, dash) &&
                        it.subcategory == tBudgetCategories[i].substring(
                    dash + 1,
                    tBudgetCategories[i].length
                )
            }
            if (dRow == null) {
                // add the row.  There's a budget for it, but no actuals
                val row = DashboardData()
                row.category = tBudgetCategories[i].substring(0, dash)
                row.subcategory =
                    tBudgetCategories[i].substring(dash + 1, tBudgetCategories[i].length)
                if (CategoryViewModel.getDiscretionaryIndicator(
                        row.category,
                        row.subcategory
                    ) == "Discretionary"
                )
                    row.discIndicator = "D"
                else
                    row.discIndicator = "ND"
                row.actualAmount = 0.0
                data.add(row)
            }
        }
        // add budget amounts
        data.forEach {
            it.budgetAmount =
                BudgetViewModel.getBudgetAmount(it.category + "-" + it.subcategory, iBudgetMonth).amount
        }

        data.sortWith(compareBy({ it.category }, { it.subcategory }))
        return data
    }
}
