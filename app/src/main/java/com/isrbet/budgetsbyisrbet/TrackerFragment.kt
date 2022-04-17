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
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
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
    private var currentBudgetMonth: BudgetMonth = BudgetMonth(0,0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dateNow = android.icu.util.Calendar.getInstance()
        currentBudgetMonth = BudgetMonth(dateNow.get(android.icu.util.Calendar.YEAR), dateNow.get(
            android.icu.util.Calendar.MONTH) + 1)

        Log.d("Alex", "Default is " + DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER))
        when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER)) {
            cDiscTypeDiscretionary -> {
                Log.d("Alex", "checking Disc")
                binding.discRadioButton.isChecked = true
            }
            cDiscTypeNondiscretionary -> {
                Log.d("Alex", "cheking non-Disc")
                binding.nonDiscRadioButton.isChecked = true
            }
            else -> {
                Log.d("Alex", "Checking all")
                binding.allDiscRadioButton.isChecked = true
            }
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_TRACKER)) {
            "0" -> binding.name1RadioButton.isChecked = true
            "1" -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_SHOW_TOTALS_TRACKER)) {
            "#" -> binding.showDollarRadioButton.isChecked = true
            "%" -> binding.showPercentageRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER)) {
            "Month" -> binding.buttonViewMonth.isChecked = true
            "Year" -> binding.buttonViewYear.isChecked = false
            "YTD" -> binding.buttonViewYtd.isChecked = true
            else -> binding.buttonViewAllTime.isChecked = false
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)

        hidePieChart()
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
            binding.chartButtonLayout.visibility = View.VISIBLE
            binding.buttonBar.setOnClickListener {
                hidePieChart()
                loadBarChart()
            }
            binding.buttonPie.setOnClickListener {
                hideBarChart()
                loadPieChart()
            }
            binding.numericTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val radioButton = requireActivity().findViewById(checkedId) as RadioButton
                setPieGraphNumericStyle(binding.actualPieChart, radioButton.text.toString())
                setPieGraphNumericStyle(binding.budgetPieChart, radioButton.text.toString())
                binding.actualPieChart.invalidate()
                binding.budgetPieChart.invalidate()
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
            binding.resetFilterButton.setOnClickListener {
                Log.d("Alex", "clicked resetFilter")
                DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeAll)
                binding.allDiscRadioButton.isChecked = true
                DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_TRACKER, "2")
                binding.whoAllRadioButton.isChecked = true
                onExpandClicked(binding.expandFilter, binding.filterButtonLinearLayout)
            }
            binding.buttonBackward.setOnClickListener {
                moveOneMonthBackward()
            }
            binding.buttonForward.setOnClickListener {
                moveOneMonthForward()
            }
            binding.buttonViewMonth.setOnClickListener {
                DefaultsViewModel.updateDefault(cDEFAULT_VIEW_BY_TRACKER, "Month")
                currentBudgetMonth = BudgetMonth(dateNow.get(android.icu.util.Calendar.YEAR), dateNow.get(
                    android.icu.util.Calendar.MONTH) + 1)
                startLoadData()
            }
            binding.buttonViewYear.setOnClickListener {
                Toast.makeText(activity, "This view is not yet implemented", Toast.LENGTH_SHORT).show()
                DefaultsViewModel.updateDefault(cDEFAULT_VIEW_BY_TRACKER, "Year")
                currentBudgetMonth.month = 0
                startLoadData()
            }
            binding.buttonViewYtd.setOnClickListener {
                Toast.makeText(activity, "This view is not yet implemented", Toast.LENGTH_SHORT).show()
                DefaultsViewModel.updateDefault(cDEFAULT_VIEW_BY_TRACKER, "YTD")
                currentBudgetMonth = BudgetMonth(dateNow.get(android.icu.util.Calendar.YEAR), dateNow.get(
                    android.icu.util.Calendar.MONTH) + 1)
                startLoadData()
            }
            binding.buttonViewAllTime.setOnClickListener {
                Toast.makeText(activity, "This view is not yet implemented", Toast.LENGTH_SHORT).show()
                DefaultsViewModel.updateDefault(cDEFAULT_VIEW_BY_TRACKER, "All-Time")
                currentBudgetMonth = BudgetMonth(dateNow.get(android.icu.util.Calendar.YEAR), dateNow.get(
                    android.icu.util.Calendar.MONTH) + 1)
                startLoadData()
            }
            binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, optionId ->
                when (optionId) {
                    R.id.discRadioButton -> {
                        Log.d("Alex", "Clicked Disc in DiscRadioGroup")
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeDiscretionary)
    //                    setChartTitle()
                        startLoadData()
                        // do something when radio button 1 is selected
                    }
                    R.id.nonDiscRadioButton -> {
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeNondiscretionary)
    //                    setChartTitle()
                        startLoadData()
                    }
                    R.id.allDiscRadioButton -> {
                        Log.d("Alex", "Clicked All in DiscRadioGroup")
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeAll)
     //                   setChartTitle()
                        startLoadData()
                    }
                }
            }
            binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
                when (optionId) {
                    R.id.name1RadioButton -> {
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_TRACKER, "0")
    //                    setChartTitle()
                        startLoadData()
                        // do something when radio button 1 is selected
                    }
                    R.id.name2RadioButton -> {
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_TRACKER, "1")
    //                    setChartTitle()
                        startLoadData()
                    }
                    R.id.whoAllRadioButton -> {
                        DefaultsViewModel.updateDefault(cDEFAULT_FILTER_WHO_TRACKER, "2")
    //                    setChartTitle()
                        startLoadData()
                    }
                }
            }
        HintViewModel.showHint(requireContext(), binding.resetFilterButton, "Tracker")
        }
    }
    private fun moveOneMonthBackward() {
        if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) != "YTD" &&
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) != "All-Time") {
            if (currentBudgetMonth.month == 0)
                currentBudgetMonth.year--
            else
                currentBudgetMonth.decrementMonth()
            Log.d("Alex", "In backward, loading $currentBudgetMonth")
            startLoadData()
        }
    }
    private fun moveOneMonthForward() {
        if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) != "YTD" &&
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) != "All-Time") {
            if (currentBudgetMonth.month == 0)
                currentBudgetMonth.year++
            else
                currentBudgetMonth.addMonth()
            startLoadData()
        }
    }
    private fun startLoadData() {
        if (binding.barChart.visibility == View.VISIBLE)
            loadBarChart()
        else
            loadPieChart()
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
    fun setPieGraphNumericStyle(pieChart: PieChart, iStyle: String) {
        val dateNow = android.icu.util.Calendar.getInstance()
        when (iStyle) {
            "%" -> {
                pieChart.setUsePercentValues(true)
                pieChart.setUsePercentValues(true)
                pieChart.data.setValueFormatter(PercentFormatter(pieChart))
                pieChart.data.setValueFormatter(PercentFormatter(pieChart))
                setChartTitle()
                binding.chartTitle.text = "Allocation (%) of Budget and Actuals - " +
                        MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                        dateNow.get(Calendar.YEAR)
            }
            "#" -> {
                pieChart.setUsePercentValues(false)
                pieChart.setUsePercentValues(false)
                pieChart.data.setValueFormatter(MyYAxisValueFormatter())
                pieChart.data.setValueFormatter(MyYAxisValueFormatter())
                setChartTitle()
                binding.chartTitle.text = "Allocation ($) of Budget and Actuals - " +
                        MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                        dateNow.get(Calendar.YEAR)
            }
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
        binding.numericTypeRadioGroup.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        binding.expandOptions.visibility = View.VISIBLE
        initializePieChart(binding.actualPieChart)
        initializePieChart(binding.budgetPieChart)
        createPieChart(binding.actualPieChart, getActualPieChartData(), "Actuals")
        createPieChart(binding.budgetPieChart, getBudgetPieChartData(), "Budget")
    }

    private fun hidePieChart() {
        binding.chartTitle.visibility = View.GONE
        binding.actualPieChart.visibility = View.GONE
        binding.budgetPieChart.visibility = View.GONE
        binding.expandOptions.visibility = View.GONE
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
        binding.barChart.animateY(1000)
        binding.barChart.legend.isEnabled = false
        binding.barChart.setTouchEnabled(true)
        binding.barChart.isDoubleTapToZoomEnabled = false
        binding.barChart.xAxis.isEnabled = true
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
        binding.barChart.xAxis.xOffset = 250F
        binding.barChart.axisLeft.axisMinimum = 0F
        binding.barChart.xAxis.textSize = 13f
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
        val discFilter = DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER)
        val whoFilter = DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_TRACKER).toInt()
        val totalDiscBudget = BudgetViewModel.getTotalCalculatedBudgetForMonth(currentBudgetMonth, discFilter, whoFilter)
        val dateNow = android.icu.util.Calendar.getInstance()
        val totalDiscBudgetToDate: Double
        val daysInMonth: Int
        if (currentBudgetMonth.year == dateNow.get(Calendar.YEAR) &&
                currentBudgetMonth.month == dateNow.get(Calendar.MONTH)+1) {
            daysInMonth = getDaysInMonth(dateNow)
            totalDiscBudgetToDate =
                totalDiscBudget * dateNow.get(Calendar.DATE) / daysInMonth
        } else {
            totalDiscBudgetToDate = totalDiscBudget
            daysInMonth = getDaysInMonth(currentBudgetMonth)
        }
        val totalDiscActualsToDate =
            ExpenditureViewModel.getTotalActualsToDate(currentBudgetMonth,
            discFilter, whoFilter)

        tList.add(
            DataObject(
                "Budget this month $ " + gDec.format(totalDiscBudget),
                totalDiscBudget,
                ContextCompat.getColor(requireContext(), R.color.dark_gray)
            )
        )
        tList.add(
            DataObject(
                "Budget to date $ " + gDec.format(totalDiscBudgetToDate),
                totalDiscBudgetToDate,
                ContextCompat.getColor(requireContext(), R.color.medium_gray)
            )
        )
        val showRed = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()

        when {
            totalDiscActualsToDate > (totalDiscBudgetToDate * (1 + showRed / 100)) -> tList.add(
                DataObject(
                    "Actuals $ " + gDec.format(totalDiscActualsToDate),
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.red)
                )
            )
            totalDiscActualsToDate > totalDiscBudgetToDate -> tList.add(
                DataObject(
                    "Actuals $ " + gDec.format(totalDiscActualsToDate),
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                )
            )
            else -> tList.add(
                DataObject(
                    "Actuals $ " + gDec.format(totalDiscActualsToDate),
                    totalDiscActualsToDate,
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            )
        }
        val lab = when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER)) {
            cDiscTypeDiscretionary -> "discretionary"
            cDiscTypeNondiscretionary -> "non-discretionary"
            else -> ""
        }
        val lab2 = when (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER)) {
            cDiscTypeDiscretionary -> "your discretionary"
            cDiscTypeNondiscretionary -> "your non-discretionary"
            else -> "your overall"
        }
        if (totalDiscActualsToDate > totalDiscBudget)
            binding.chartSummaryText.text = "You are over your $lab budget this month."
        else {
            val remainingBudget = totalDiscBudget - totalDiscActualsToDate
            val daysRemaining = daysInMonth - dateNow.get(Calendar.DATE) + 1
            val dollarFormat = DecimalFormat("$###.00")


            binding.chartSummaryText.text =
                "Keeping $lab expenses below " + dollarFormat.format(remainingBudget / daysRemaining) + " per day will keep you within $lab2 budget this month."
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
//        val dateNow = android.icu.util.Calendar.getInstance()
        setChartTitle()
/*        binding.chartTitle.text = "Discretionary Expense Tracker - " +
                MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                dateNow.get(Calendar.YEAR)*/
    }

    @SuppressLint("SetTextI18n")
    private fun setChartTitle() {
        val prefix = if (binding.barChart.visibility == View.VISIBLE) {
            "Expense Tracker"
        } else {
            if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_TOTALS_TRACKER) == "%")
                "Allocation (%) of Budget and Actuals - "
            else
                "Allocation ($) of Budget and Actuals - "
        }
        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER) != cDiscTypeAll)
            currentFilterIndicator = " " + DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER).substring(0,5)
        if (DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_TRACKER) != "" && DefaultsViewModel.getDefault(
                cDEFAULT_FILTER_WHO_TRACKER) != "2")
            currentFilterIndicator = currentFilterIndicator + " " + SpenderViewModel.getSpenderName(DefaultsViewModel.getDefault(cDEFAULT_FILTER_WHO_TRACKER).toInt())
        when {
            DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) == "All-Time" -> binding.chartTitle.text =
                "$prefix (All-Time$currentFilterIndicator)"
            currentBudgetMonth.month == 0 -> binding.chartTitle.text = prefix + " (" + currentBudgetMonth.year  + currentFilterIndicator + ")"
            else -> {
                binding.chartTitle.text = prefix + " (" + MonthNames[currentBudgetMonth.month - 1] +
                        " " + currentBudgetMonth.year
                if (DefaultsViewModel.getDefault(cDEFAULT_VIEW_BY_TRACKER) == "YTD")
                    binding.chartTitle.text = binding.chartTitle.text.toString() + " YTD"
                binding.chartTitle.text = binding.chartTitle.text.toString() + currentFilterIndicator + ")"
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun getActualPieChartData(): PieDataSet {
        val dateNow = android.icu.util.Calendar.getInstance()
        val catActuals = ExpenditureViewModel.getCategoryActuals(BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1))
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
/*        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567")) */

        //input data and fit data into pie chart entry
        for (actual in catActuals) {
            pieEntries.add(PieEntry(actual.value.toFloat(), actual.label))
            colors.add(DefaultsViewModel.getCategoryDetail(actual.label).color)
        }
        //collecting the entries with label name
        val label = ""
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.valueTextColor = Color.BLACK
        return pieDataSet
    }

    @SuppressLint("SetTextI18n")
    private fun getBudgetPieChartData(): PieDataSet {
        val dateNow = android.icu.util.Calendar.getInstance()
        val discFilter = DefaultsViewModel.getDefault(cDEFAULT_FILTER_DISC_TRACKER)
        val whoFilter = if (binding.name1RadioButton.isChecked) 0 else if (binding.name2RadioButton.isChecked) 1 else 2
        val catBudgets = BudgetViewModel.getCategoryBudgets(BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1), whoFilter)
        val label = "" // "Category"
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
/*        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567")) */

        //input data and fit data into pie chart entry
        for (budget in catBudgets) {
            pieEntries.add(PieEntry(budget.value.toFloat(), budget.label))
            colors.add(DefaultsViewModel.getCategoryDetail(budget.label).color)
        }
        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.valueTextColor = Color.BLACK
        return pieDataSet
    }
    private fun initializePieChart(pieChart: PieChart) {
        //using percentage as values instead of amount
        pieChart.setUsePercentValues(true)
        //remove the description label on the lower left corner, default true if not set
        pieChart.description.isEnabled = true
        pieChart.description.textSize = 16F
        //enabling the user to rotate the chart, default true
        pieChart.isRotationEnabled = false
        //adding friction when rotating the pie chart
        pieChart.dragDecelerationFrictionCoef = 0.9f
        //setting the first entry start from right hand side, default starting from top
        pieChart.rotationAngle = 0F
        //highlight the entry when it is tapped, default true if not set
        pieChart.isHighlightPerTapEnabled = true
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
        pieChart.data = pieData
        pieChart.description.text = ""
//        pieChart.centerText = iDescription
//        pieChart.setCenterTextSize(14F)
        val s = SpannableString(iDescription)
        s.setSpan(RelativeSizeSpan(2f), 0, s.length, 0)
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, 0)
        pieChart.centerText = s

        binding.chartSummaryText.text = ""
        val selectedId = binding.numericTypeRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        setPieGraphNumericStyle(pieChart, radioButton.text.toString())
        pieChart.invalidate()
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

