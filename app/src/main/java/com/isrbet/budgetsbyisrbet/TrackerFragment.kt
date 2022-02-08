package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentTrackerBinding
import java.text.DecimalFormat
import java.util.*


class TrackerFragment : Fragment() {
    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        _binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        TranslationViewModel.showMe()
        if (ExpenditureViewModel.getCount() > 1 && CategoryViewModel.getCount() > 1)
            loadBarChart()
        if (parentFragment is HomeFragment) { // ie on home page
            // remove top padding
            binding.constraintLayout.setPadding(
                binding.constraintLayout.paddingLeft,
                0,
                binding.constraintLayout.paddingRight,
                binding.constraintLayout.paddingBottom
            )
        } else { // ie on Tracker page
            binding.buttonLayout.visibility = View.VISIBLE
            binding.buttonBar.setOnClickListener {
                hidePieChart()
                loadBarChart()
            }
            binding.buttonPie.setOnClickListener {
                hideBarChart()
                loadPieChart()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (parentFragment !is HomeFragment) { // ie only do this if displaying as own fragment
            super.onPrepareOptionsMenu(menu)
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = menu.getItem(i).itemId == R.id.DashboardFragment
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.DashboardFragment) {
            view?.findNavController()?.navigate(R.id.DashboardFragment)
            true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    fun loadBarChart() {
        binding.chartTitle.visibility = View.VISIBLE
        binding.barChart.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        initializeBarChart()
        createBarChart(getBarChartData())
    }

    fun hideBarChart() {
        binding.chartTitle.visibility = View.GONE
        binding.barChart.visibility = View.GONE
        binding.chartSummaryText.visibility = View.GONE
    }

    private fun loadPieChart() {
        binding.chartTitle.visibility = View.VISIBLE
        binding.actualPieChart.visibility = View.VISIBLE
        binding.budgetPieChart.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        initializePieChart(binding.actualPieChart)
        initializePieChart(binding.budgetPieChart)
        createPieChart(binding.actualPieChart, getActualPieChartData(), "Actuals")
        createPieChart(binding.budgetPieChart, getBudgetPieChartData(), "Budget")
    }

    private fun hidePieChart() {
        binding.chartTitle.visibility = View.GONE
        binding.actualPieChart.visibility = View.GONE
        binding.budgetPieChart.visibility = View.GONE
        binding.chartSummaryText.visibility = View.GONE
    }

    private fun initializeBarChart() {
        binding.barChart.description.isEnabled = false
        // if more than 60 entries are displayed in the chart, no values will be drawn
        binding.barChart.setMaxVisibleValueCount(3)
        binding.barChart.xAxis.setDrawGridLines(false)
        // scaling can now only be done on x- and y-axis separately
        binding.barChart.setPinchZoom(false)
        binding.barChart.setDrawBarShadow(false)
        binding.barChart.setDrawGridBackground(false)
        val xAxis: XAxis = binding.barChart.xAxis
        xAxis.setDrawGridLines(false)
        binding.barChart.axisLeft.setDrawGridLines(false)
        binding.barChart.axisRight.setDrawGridLines(false)
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.axisLeft.isEnabled = true
        binding.barChart.xAxis.setDrawGridLines(false)
        // add a nice and smooth animation
        binding.barChart.animateY(1500)
        binding.barChart.legend.isEnabled = false
        binding.barChart.setTouchEnabled(true)
        binding.barChart.isDoubleTapToZoomEnabled = false
        binding.barChart.xAxis.isEnabled = true
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barChart.axisLeft.axisMinimum = 0F
        binding.barChart.axisLeft.valueFormatter = (MyYAxisValueFormatter())

        binding.barChart.setDrawValueAboveBar(true)

        binding.barChart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
            }

            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
            }

            override fun onChartLongPressed(me: MotionEvent?) {
            }

            override fun onChartDoubleTapped(me: MotionEvent?) {
            }

            override fun onChartSingleTapped(me: MotionEvent?) {
                if (parentFragment is HomeFragment) {
                    view?.findNavController()?.navigate(R.id.TrackerFragment)
                }
            }

            override fun onChartFling(
                me1: MotionEvent?,
                me2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {
            }

            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
            }

            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
            }

        }
        binding.barChart.invalidate()
    }

    @SuppressLint("SetTextI18n")
    private fun getBarChartData(): ArrayList<DataObject> {
        val tList = ArrayList<DataObject>()
        val dateNow = android.icu.util.Calendar.getInstance()
        val daysInMonth = getDaysInMonth(dateNow)
        val totalDiscBudget = BudgetViewModel.getTotalBudgetForMonth(BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1), cDiscTypeDiscretionary)
        val totalDiscBudgetToDate =
            totalDiscBudget * dateNow.get(Calendar.DATE) / daysInMonth
        val totalDiscActualsToDate =
            ExpenditureViewModel.getTotalDiscretionaryActualsToDate(dateNow)
        Log.d(
            "Alex",
            "amounts are $totalDiscBudget and $totalDiscBudgetToDate and $totalDiscActualsToDate"
        )
        tList.add(
            DataObject(
                "Budget this month",
                totalDiscBudget,
                ContextCompat.getColor(requireContext(), R.color.dark_gray)
            )
        )
        tList.add(
            DataObject(
                "Budget to date",
                totalDiscBudgetToDate,
                ContextCompat.getColor(requireContext(), R.color.medium_gray)
            )
        )
        val showRed = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()

        if (totalDiscActualsToDate > (totalDiscBudgetToDate * (1 + showRed / 100)))
            tList.add(
                DataObject(
                    "Actuals",
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.red)
                )
            )
        else if (totalDiscActualsToDate > totalDiscBudgetToDate)
            tList.add(
                DataObject(
                    "Actuals",
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                )
            )
        else
            tList.add(
                DataObject(
                    "Actuals",
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            )
        if (totalDiscActualsToDate > totalDiscBudget)
            binding.chartSummaryText.text = "You are over your discretionary budget this month."
        else {
            val remainingBudget = totalDiscBudget - totalDiscActualsToDate
            val daysRemaining = daysInMonth - dateNow.get(Calendar.DATE) + 1
            val dollarFormat = DecimalFormat("$###.00")

            binding.chartSummaryText.text =
                "Keeping discretionary expenses below " + dollarFormat.format(remainingBudget / daysRemaining) + " per day will keep you within budget this month."
        }
        return tList
    }

    @SuppressLint("SetTextI18n")
    private fun createBarChart(iData: ArrayList<DataObject>) {
        val dataSets: ArrayList<IBarDataSet> = ArrayList()
        for (i in 0 until iData.size) { // for until loop excludes the "until" number
            val values: ArrayList<BarEntry> = ArrayList()
            val dataObject: DataObject = iData[i]
            values.add(BarEntry(i.toFloat(), dataObject.value.toFloat(), dataObject.color))
            val ds = BarDataSet(values, "Data Set")
            ds.color = dataObject.color
            ds.setDrawValues(true)
            dataSets.add(ds)
        }
//        val set1: BarDataSet
//        if (binding.barChart.data != null &&
//            binding.barChart.data.dataSetCount > 0) {
        // code comes here after the first time, ie hits the else below the first time the chart is drawn
//            set1 = binding.barChart.data.getDataSetByIndex(0) as BarDataSet
//            set1.values = values
//            binding.barChart.data.notifyDataChanged()
//            binding.barChart.notifyDataSetChanged()
//        } else {
//            set1 = BarDataSet(values, "Data Set")
//            set1.setColors(SessionManagement.MATERIAL_COLORS)
//            set1.setDrawValues(true)
//            dataSets.add(set1)
        val data = BarData(dataSets)
        binding.barChart.data = data
        binding.barChart.setVisibleXRange(1.0F, iData.size.toFloat())
        binding.barChart.setFitBars(true)
        val xAxis: XAxis = binding.barChart.xAxis
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        val labelList = ArrayList<String>()
        for (j in 0 until iData.size)
            labelList.add(iData[j].label)
        xAxis.valueFormatter =
            IndexAxisValueFormatter(labelList) //setting String values in X axis
        for (set in binding.barChart.data
            .dataSets) set.setDrawValues(!set.isDrawValuesEnabled)
        binding.barChart.data.notifyDataChanged()
        binding.barChart.notifyDataSetChanged()
        val dateNow = android.icu.util.Calendar.getInstance()
        binding.chartTitle.text = "Discretionary Expense Tracker - " +
                MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                dateNow.get(Calendar.YEAR)
    }

    @SuppressLint("SetTextI18n")
    private fun getActualPieChartData(): PieDataSet {
        val dateNow = android.icu.util.Calendar.getInstance()
        val catActuals = ExpenditureViewModel.getCategoryActuals(BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1))
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567"))

        //input data and fit data into pie chart entry
        for (actual in catActuals) {
            pieEntries.add(PieEntry(actual.value.toFloat(), actual.label))
        }
        //collecting the entries with label name
        val label = ""
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        pieDataSet.valueTextColor = Color.BLACK
        return pieDataSet
    }

    @SuppressLint("SetTextI18n")
    private fun getBudgetPieChartData(): PieDataSet {
        val dateNow = android.icu.util.Calendar.getInstance()
        val catBudgets = BudgetViewModel.getCategoryBudgets(BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1))
        val label = "Category"
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567"))

        //input data and fit data into pie chart entry
        for (budget in catBudgets) {
            pieEntries.add(PieEntry(budget.value.toFloat(), budget.label))
        }
        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        pieDataSet.valueTextColor = Color.BLACK
        return pieDataSet
    }
    private fun initializePieChart(pieChart: PieChart) {
        //using percentage as values instead of amount
        pieChart.setUsePercentValues(true)
        //remove the description label on the lower left corner, default true if not set
        pieChart.getDescription().setEnabled(true)
        pieChart.description.textSize = 16F
        //enabling the user to rotate the chart, default true
        pieChart.setRotationEnabled(false)
        //adding friction when rotating the pie chart
        pieChart.setDragDecelerationFrictionCoef(0.9f)
        //setting the first entry start from right hand side, default starting from top
        pieChart.setRotationAngle(0F)
        //highlight the entry when it is tapped, default true if not set
        pieChart.setHighlightPerTapEnabled(true)
        //adding animation so the entries pop up from 0 degree
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        //setting the color of the hole in the middle, default white
        pieChart.setHoleColor(Color.parseColor("#FFFFFF"))
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    }

    @SuppressLint("SetTextI18n")
    private fun createPieChart(pieChart: PieChart, pieDataSet: PieDataSet, iDescription: String) {
        //setting text size of the value
        pieDataSet.valueTextSize = 12f
        //grouping the data set from entry to chart
        //showing the value of the entries, default true if not set
        val pieData = PieData(pieDataSet)
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieChart.setData(pieData)
        pieChart.description.text = ""
//        pieChart.centerText = iDescription
//        pieChart.setCenterTextSize(14F)
        val s = SpannableString(iDescription)
        s.setSpan(RelativeSizeSpan(2f), 0, s.length, 0)
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, 0)
        pieChart.centerText = s
        pieChart.invalidate()

        val dateNow = android.icu.util.Calendar.getInstance()
        binding.chartTitle.text = "Allocation (%) of Budget and Actuals - " +
                MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                dateNow.get(Calendar.YEAR)
        binding.chartSummaryText.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

class MyYAxisValueFormatter : ValueFormatter() {
    private val mFormat: DecimalFormat = DecimalFormat("$ ###,###,##0")
    override fun getFormattedValue(value: Float): String {
        //write your logic here
        //access the YAxis object to get more information
        return mFormat.format(value.toDouble())
    }

}

