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
import com.google.android.material.color.MaterialColors
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var mTableLayout: TableLayout? = null
    private var currentBudgetMonth: BudgetMonth = BudgetMonth(0,0)
    private var currentRecFilter: String = ""
    private var currentDiscFilter: String = ""
    private var currentPaidByFilter: String = ""
    private var currentBoughtForFilter: String = ""
    private var collapsedCategories: MutableList<String> = ArrayList()

    private fun startLoadData(iBudgetMonth: BudgetMonth, iRecFlag: String = "", iDiscFlag: String = "", iPaidByFlag: String = "", iBoughtForFlag: String = "") {
        val dashboardRows = DashboardRows()
        val data: MutableList<DashboardData> = dashboardRows.getRows(iBudgetMonth, iRecFlag, iDiscFlag, iPaidByFlag, iBoughtForFlag)

        val rows = data.size
        var textSpacer: TextView?
        mTableLayout!!.removeAllViews()
        var lastCategory = ""
        var lastCategoryBudgetTotal = 0.0
        var lastCategoryActualTotal = 0.0
        var grandBudgetTotal = 0.0
        var grandActualTotal = 0.0

        // -1 means heading row
        var i = -1
        // do header row
        createViewRow("Header", i++, "", "", "", 0.0, 0.0)

        while (i < rows) {
            var row: DashboardData? = null
            if (i > -1) row = data[i] else {
                textSpacer = TextView(requireContext())
                textSpacer.text = ""
            }

            if (row != null && lastCategory != "" && row.category != lastCategory) {
                // sub-total row
                createViewRow("Sub-total", i, lastCategory, "", "-", lastCategoryBudgetTotal, lastCategoryActualTotal)
                grandBudgetTotal += lastCategoryBudgetTotal
                grandActualTotal += lastCategoryActualTotal
                lastCategoryBudgetTotal = 0.0
                lastCategoryActualTotal = 0.0
            }

            if (row != null) {
                lastCategory = row.category
                createViewRow("Detail", i, row.category, row.subcategory, row.discIndicator, row.budgetAmount, row.actualAmount)
                lastCategoryBudgetTotal += row.budgetAmount
                lastCategoryActualTotal += row.actualAmount
            }
            i++
        }

        createViewRow("Sub-total", i++, lastCategory, "", "-", lastCategoryBudgetTotal, lastCategoryActualTotal)
        grandBudgetTotal += lastCategoryBudgetTotal
        grandActualTotal += lastCategoryActualTotal
        createViewRow("Grand total", i++, "Grand Total", "", "", grandBudgetTotal, grandActualTotal)
        // delta row
        if (grandActualTotal > grandBudgetTotal) {
            createViewRow("Delta", i, "Delta", "", "", 0.0, grandActualTotal-grandBudgetTotal)
        }
        else {
            createViewRow("Delta", i, "Delta", "", "", grandBudgetTotal-grandActualTotal,0.0)
        }
    }

    private fun createViewRow(iRowType: String, iRowNo: Int, iCategory: String, iSubcategory: String, iDiscFlag: String, iBudgetAmount: Double, iActualAmount:Double) {
        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0
        val decimalFormat = DecimalFormat("0.00")
        val percentFormat = DecimalFormat("0.00%")
        val pct: Double

        val tv1 = TextView(requireContext())
        tv1.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        tv1.gravity = if (iRowType == "Detail") Gravity.START else Gravity.END
        tv1.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv1.text = ""
            tv1.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnSecondary, Color.BLACK))
        } else {
            tv1.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK))
            when (iRowType) {
                "Detail" -> tv1.text = iSubcategory
                "Sub-total" -> tv1.text = "$iCategory Total"
                else -> tv1.text = iCategory
            }
            if (tv1.text.length > 15) {
                tv1.text = tv1.text.substring(0,15) + "..."
            }
        }
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
            tv2.text = "Disc"
            tv2.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnSecondary, Color.BLACK))
            tv2.tooltipText = "Indicates whether this budget item is Discretionary (D), or not (ND)."
        } else {
            tv2.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK))
            tv2.text = iDiscFlag
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
        tv3.gravity = Gravity.END
        tv3.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv3.text = "Budget"
            tv3.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnSecondary, Color.BLACK))
        } else {
            tv3.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK))
            if (iRowType == "Delta" && iBudgetAmount == 0.0)
                tv3.text = ""
            else
                tv3.text = decimalFormat.format(iBudgetAmount)
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
        tv4.gravity = Gravity.END
        tv4.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv4.text = "Actual"
            tv4.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnSecondary, Color.BLACK))
        } else {
            tv4.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK))
            if (iRowType == "Delta" && iActualAmount == 0.0)
                tv4.text = ""
            else
                tv4.text = decimalFormat.format(iActualAmount)
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
        tv5.gravity = Gravity.END
        tv5.setPadding(5, 15, 0, 15)
        if (iRowType == "Header") {
            tv5.text = "%"
            tv5.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnSecondary, Color.BLACK))
        } else {
            tv5.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.background, Color.BLACK))
            pct =
                if (iActualAmount == 0.0 || iBudgetAmount == 0.0) 0.0
                else iActualAmount / iBudgetAmount
            if (iRowType != "Delta")
               tv5.text = percentFormat.format(pct)
        }
        // add table row
        val tr = TableRow(requireContext())
        tr.id = iRowNo + 1
        tr.tag = iRowType
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        tr.setPadding(0, 0, 0, 0)
        tr.layoutParams = trParams
        if (iRowType == "Detail") {
            val color = getBudgetColour(requireContext(), iActualAmount, iBudgetAmount)
            tv5.setBackgroundColor(color)
        }
        else if (iRowType == "Header") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv2.setTypeface(null, Typeface.BOLD)
            tv3.setTypeface(null, Typeface.BOLD)
            tv4.setTypeface(null, Typeface.BOLD)
            tv5.setTypeface(null, Typeface.BOLD)
            tv1.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv2.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv3.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv4.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv5.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
        }
        else if (iRowType == "Sub-total") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv1.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.colorPrimary, Color.BLACK), "1F")
            tv2.setBackgroundColor(Color.parseColor(hexColor))
            tv3.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv4.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
            tv5.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.BLACK))
        }
        else if (iRowType == "Grand total") {
            tv1.setTypeface(null, Typeface.BOLD)
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F) // 14F is default
            tv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F) // 14F is default
            tv1.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryVariant, Color.BLACK))
            tv2.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryVariant, Color.BLACK))
            tv3.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryVariant, Color.BLACK))
            tv4.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryVariant, Color.BLACK))
            tv5.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryVariant, Color.BLACK))
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
        if (isInvisible(iCategory)) {
            if (iRowType == "Detail") {
                tr.visibility = View.GONE
            }  else if (iRowType == "Sub-total") {
                tv2.text = "+"
            }
        } else
            tr.visibility = View.VISIBLE

        if (iRowType != "Header" && iRowType != "Sub-total" && iRowType != "Grand total" && iRowType != "Delta") {
            tr.setOnClickListener {
                Log.d("Alex", "row " + it.id + " was clicked")
                // go to ViewAll with the SubCategory as the search term
                val tableRow = it as TableRow
                val textView = tableRow.getChildAt(0) as TextView
                MyApplication.transactionSearchText = textView.text.toString() + " " + currentBudgetMonth.year.toString()
                MyApplication.transactionSearchText = MyApplication.transactionSearchText.replace("...","")
                if (currentBudgetMonth.month != 0) {
                    if (currentBudgetMonth.month < 10)
                        MyApplication.transactionSearchText += "-0" + currentBudgetMonth.month.toString()
                    else
                        MyApplication.transactionSearchText += "-" + currentBudgetMonth.month.toString()
                }
                view?.findNavController()?.navigate(R.id.ViewTransactionsFragment)
            }
        } else if (iRowType == "Sub-total") {
            tr.setOnClickListener {
                Log.d("Alex", "header was clicked")
                val tableRow = it as TableRow
                val tv1 = tableRow.getChildAt(0) as TextView
                val tv2 = tableRow.getChildAt(1) as TextView
                Log.d("Alex", "header was clicked " + tv2.text.toString())
                var tmpCat = tv1.text.toString().replace(" Total","")
                tmpCat = tmpCat.replace("...","")
                tmpCat = tmpCat.trim()
                if (tv2.text.toString() == "+") {
                    Log.d("Alex", "Expand")
                    tv2.text = "-"
                    refreshRows(tmpCat, View.VISIBLE)
                } else {
                        Log.d("Alex", "Collapse")
                    tv2.text = "+"
                    refreshRows(tmpCat, View.GONE)
                }
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
        }
    }

    private fun refreshRows(iCategory: String, iVisibility: Int) {
        var firstDetailLine: Int
        var lastDetailLine = 0
        mTableLayout = requireActivity().findViewById(R.id.table_dashboard_rows) as TableLayout
        var tableRow: TableRow?
        do {
            lastDetailLine += 1
            tableRow = mTableLayout!!.getChildAt(lastDetailLine) as TableRow
            val cat = tableRow.getChildAt(0) as TextView
            Log.d("Alex", "tag is " + tableRow.tag + " cat is '" + cat.text.toString() + "' iCategory is " + iCategory + " len " + iCategory.length)
            val lenToCompare = if (cat.text.toString().length < iCategory.length) cat.text.toString().length else iCategory.length
        } while (tableRow != null && !(tableRow.tag == "Sub-total" && cat.text.toString().substring(0,lenToCompare) == iCategory))
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
        view?.findViewById<Button>(R.id.button_backward)?.setOnClickListener {
            moveOneMonthBackward()
        }
        view?.findViewById<Button>(R.id.button_forward)?.setOnClickListener {
            moveOneMonthForward()
        }
        view?.findViewById<Button>(R.id.button_by_month)?.setOnClickListener {
            val dateNow = Calendar.getInstance()
            currentBudgetMonth = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
        }
        view?.findViewById<Button>(R.id.button_by_year)?.setOnClickListener {
            currentBudgetMonth.month = 0
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
        }
        mTableLayout = requireActivity().findViewById(R.id.table_dashboard_rows) as TableLayout
        binding.tableDashboardRows.isStretchAllColumns = true

        if (currentBudgetMonth.year == 0) {
            val dateNow = Calendar.getInstance()
            currentBudgetMonth =
                BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
        }
        setActionBarTitle()
//        startLoadData(currentBudgetMonth)
        startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == R.id.FilterRecurring) {
                menu.getItem(i).isVisible = true
                menu.getItem(i).isChecked = currentRecFilter == "Recurring"
            } else if (menu.getItem(i).itemId == R.id.FilterDiscretionary) {
                menu.getItem(i).isVisible = true
                menu.getItem(i).isChecked = currentDiscFilter == "Discretionary"
            } else if (menu.getItem(i).itemId == R.id.FilterNonDiscretionary) {
                menu.getItem(i).isVisible = true
                menu.getItem(i).isChecked = currentDiscFilter == "Non-Discretionary"
            } else if (menu.getItem(i).itemId == R.id.FilterPaidByName1 ||
                menu.getItem(i).itemId == R.id.FilterPaidByName2 ||
                menu.getItem(i).itemId == R.id.FilterPaidByTitle ||
                menu.getItem(i).itemId == R.id.FilterBoughtForTitle ||
                menu.getItem(i).itemId == R.id.FilterBoughtForName1 ||
                menu.getItem(i).itemId == R.id.FilterBoughtForName2) {
                if (!SpenderViewModel.singleUser()) {
                    if (menu.getItem(i).itemId == R.id.FilterPaidByTitle ||
                        menu.getItem(i).itemId == R.id.FilterBoughtForTitle) {
                        menu.getItem(i).isVisible = true
                    } else if (menu.getItem(i).itemId == R.id.FilterPaidByName1) {
                        menu.getItem(i).isVisible = true
                        menu.getItem(i).title = SpenderViewModel.getSpender(0, true)?.name
                        menu.getItem(i).isChecked =
                            currentPaidByFilter == SpenderViewModel.getSpender(0, true)?.name
                    } else if (menu.getItem(i).itemId == R.id.FilterPaidByName2) {
                        menu.getItem(i).isVisible = true
                        menu.getItem(i).title = SpenderViewModel.getSpender(1, true)?.name
                        menu.getItem(i).isChecked =
                            currentPaidByFilter == SpenderViewModel.getSpender(1, true)?.name
                    } else if (menu.getItem(i).itemId == R.id.FilterBoughtForName1) {
                        menu.getItem(i).isVisible = true
                        menu.getItem(i).title = SpenderViewModel.getSpender(0, true)?.name
                        menu.getItem(i).isChecked =
                            currentBoughtForFilter == SpenderViewModel.getSpender(0, true)?.name
                    } else if (menu.getItem(i).itemId == R.id.FilterBoughtForName2) {
                        menu.getItem(i).isVisible = true
                        menu.getItem(i).title = SpenderViewModel.getSpender(1, true)?.name
                        menu.getItem(i).isChecked =
                            currentBoughtForFilter == SpenderViewModel.getSpender(1, true)?.name
                    }
                }
            } else if (menu.getItem(i).itemId == R.id.FilterTitle) {
                menu.getItem(i).isVisible = true
            } else
                menu.getItem(i).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.FilterRecurring) {
            if (currentRecFilter == "Recurring") {
                item.isChecked = false
                currentRecFilter = ""
            } else {
                item.isChecked = true
                currentRecFilter = "Recurring"
            }
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterDiscretionary) {
            if (currentDiscFilter == "Discretionary") {
                item.isChecked = false
                currentDiscFilter = ""
            } else {
                item.isChecked = true
                currentDiscFilter = "Discretionary"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterNonDiscretionary) {
            if (currentDiscFilter == "Non-Discretionary") {
                item.isChecked = false
                currentDiscFilter = ""
            } else {
                item.isChecked = true
                currentDiscFilter = "Non-Discretionary"
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterPaidByName1) {
            if (currentPaidByFilter == SpenderViewModel.getSpender(0, true)?.name) {
                item.isChecked = false
                currentPaidByFilter = ""
            } else {
                item.isChecked = true
                currentPaidByFilter = SpenderViewModel.getSpender(0, true)?.name.toString()
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterPaidByName2) {
            if (currentPaidByFilter == SpenderViewModel.getSpender(1, true)?.name) {
                item.isChecked = false
                currentPaidByFilter = ""
            } else {
                item.isChecked = true
                currentPaidByFilter = SpenderViewModel.getSpender(1, true)?.name.toString()
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterBoughtForName1) {
            if (currentBoughtForFilter == SpenderViewModel.getSpender(0, true)?.name) {
                item.isChecked = false
                currentBoughtForFilter = ""
            } else {
                item.isChecked = true
                currentBoughtForFilter = SpenderViewModel.getSpender(0, true)?.name.toString()
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else if (item.itemId == R.id.FilterBoughtForName2) {
            if (currentBoughtForFilter == SpenderViewModel.getSpender(1, true)?.name) {
                item.isChecked = false
                currentBoughtForFilter = ""
            } else {
                item.isChecked = true
                currentBoughtForFilter = SpenderViewModel.getSpender(1, true)?.name.toString()
            }
            activity?.invalidateOptionsMenu()
            setActionBarTitle()
            startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun setActionBarTitle() {
        var currentFilterIndicator = ""
        if (currentRecFilter != "" || currentDiscFilter != "" || currentPaidByFilter != "" || currentBoughtForFilter != "")
            currentFilterIndicator = " FILTERED! "

        if (currentBudgetMonth.month == 0)
            (activity as AppCompatActivity).supportActionBar?.title =
                "Dashboard (" + currentBudgetMonth.year  + currentFilterIndicator + ")"
        else
            (activity as AppCompatActivity).supportActionBar?.title =
                "Dashboard (" + MonthNames[currentBudgetMonth.month-1] + " " + currentBudgetMonth.year  + currentFilterIndicator + ")"
    }

    private fun moveOneMonthBackward() {
        if (currentBudgetMonth.month == 0)
            currentBudgetMonth.year--
        else
            currentBudgetMonth.decrementMonth()
        setActionBarTitle()
        Log.d("Alex", "In backward, loading $currentBudgetMonth")
        startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)

    }

    private fun moveOneMonthForward() {
        if (currentBudgetMonth.month == 0)
            currentBudgetMonth.year++
        else
            currentBudgetMonth.addMonth()
        setActionBarTitle()
        startLoadData(currentBudgetMonth, currentRecFilter, currentDiscFilter, currentPaidByFilter, currentBoughtForFilter)
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
    var category = ""
    var subcategory = ""
    var discIndicator = ""
    var budgetAmount: Double = 0.0
    var actualAmount: Double = 0.0
}

class DashboardRows {
    fun getRows(iBudgetMonth: BudgetMonth, iRecFlag: String = "", iDiscFlag: String = "", iPaidByFlag: String = "", iBoughtForFlag: String = ""): MutableList<DashboardData> {
        val data: MutableList<DashboardData> = mutableListOf()
        val expenditures = ExpenditureViewModel.getExpenditures()
        val startDate: String
        val endDate: String
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
        Log.d("Alex",
            "start date is $startDate and end date is $endDate iDiscFlag is '$iDiscFlag' and iPaidByFlag is $iPaidByFlag' and iBoughtForFlag is $iBoughtForFlag"
        )

        loop@ for (expenditure in expenditures) {
            if (expenditure.date > startDate && expenditure.date < endDate) {
                val expDiscIndicator = CategoryViewModel.getDiscretionaryIndicator(
                    expenditure.category,
                    expenditure.subcategory
                )
                if (expenditure.type != "T") {
                    if (iRecFlag == "" || (iRecFlag == "Recurring" && expenditure.type == "R")) {
                        if (iDiscFlag == "" || iDiscFlag == expDiscIndicator) {
                            if (iPaidByFlag == "" || expenditure.paidby == iPaidByFlag || expenditure.paidby == "Joint") {
                                if (iBoughtForFlag == "" || expenditure.boughtfor == iBoughtForFlag || expenditure.boughtfor == "Joint") {
                                    // this is a transaction to add to our subtotal
                                    var multiplier = 1.0
                                    if (iPaidByFlag != "") {
                                        if (expenditure.paidby == "Joint" || expenditure.paidby == iPaidByFlag) {
                                            if (SpenderViewModel.getSpenderName(0) == iPaidByFlag)
                                                multiplier = expenditure.bfname1split.toDouble()/100
                                            else
                                                multiplier = expenditure.bfname2split.toDouble()/100
                                        } else
                                            multiplier = 0.0
                                    } else if (iBoughtForFlag != "") {
                                        if (expenditure.boughtfor == "Joint" || expenditure.boughtfor == iBoughtForFlag) {
                                            if (SpenderViewModel.getSpenderName(0) == iBoughtForFlag)
                                                multiplier = expenditure.bfname1split.toDouble()/100
                                            else
                                                multiplier = expenditure.bfname2split.toDouble()/100
                                        } else
                                            multiplier = 0.0
                                    }
                                    val bdRow: DashboardData? =
                                        data.find { it.category == expenditure.category && it.subcategory == expenditure.subcategory }
                                    if (bdRow == null) {
                                        val row = DashboardData()
                                        row.category = expenditure.category
                                        row.subcategory = expenditure.subcategory
                                        row.discIndicator = if (expDiscIndicator == "Discretionary") "D" else "ND"
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
            }
        }
        // need to get budget categories for which there are budgets but no actuals; but, skip annual budgets
        val  tBudgetCategories = BudgetViewModel.getBudgetCategories(iBudgetMonth, iDiscFlag)
        for (i in 0 until tBudgetCategories.size) {
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
                row.discIndicator = if (CategoryViewModel.getDiscretionaryIndicator(row.category, row.subcategory)
                    == "Discretionary") "D" else "ND"
                row.actualAmount = 0.0
                data.add(row)
            }
        }
        // add budget amounts
        var whoToLookup = iBoughtForFlag
        if (whoToLookup == "") {
            if (SpenderViewModel.singleUser())
                whoToLookup = SpenderViewModel.getSpenderName(0)
            else
                whoToLookup = "Joint"
        }
        data.forEach {
            val budgetForPeriod = BudgetViewModel.getBudgetAmount(
                it.category + "-" + it.subcategory,
                iBudgetMonth,
                whoToLookup,
                false
            )

            if (iBudgetMonth.month == 0 || budgetForPeriod.dateStarted.month != 0) { // ie an annual view, or not an annual budget
                it.budgetAmount = budgetForPeriod.amount
            } else {
                val totalAnnualBudget = BudgetViewModel.getBudgetAmount(it.category + "-" + it.subcategory, iBudgetMonth, whoToLookup, false).amount // get total annual budget
                var totalAnnualActualsForEarlierMonths = 0.0
                if (iBudgetMonth.month != 1) {
                    if (whoToLookup == "Joint") {
                        for (i in 0..2) {
                            totalAnnualActualsForEarlierMonths +=
                                ExpenditureViewModel.getActualsForPeriod(
                                    it.category, it.subcategory,
                                    BudgetMonth(iBudgetMonth.year, 1),
                                    BudgetMonth(iBudgetMonth.year, iBudgetMonth.month - 1),
                                    SpenderViewModel.getSpenderName(i)
                                )
                        }
                    } else {
                        totalAnnualActualsForEarlierMonths =
                            ExpenditureViewModel.getActualsForPeriod(
                                it.category, it.subcategory,
                                BudgetMonth(iBudgetMonth.year, 1),
                                BudgetMonth(iBudgetMonth.year, iBudgetMonth.month - 1),
                                whoToLookup
                            )
                    }
                }
                val budgetRemaining = if (totalAnnualBudget - totalAnnualActualsForEarlierMonths > 0.0) totalAnnualBudget - totalAnnualActualsForEarlierMonths else 0.0
                it.budgetAmount = if (it.actualAmount < budgetRemaining) it.actualAmount else budgetRemaining
            }
        }

        data.sortWith(compareBy({ it.category }, { it.subcategory }))
        return data
    }
}
