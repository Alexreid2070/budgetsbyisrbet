package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.RadioButton
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTrackerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class TrackerFragment : Fragment(), CoroutineScope {
    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!
    private var currentBudgetMonth: MyDate = MyDate(1900, 1, 1)
    private var hackBudgetTotal = 0.0
    private var hackActualTotal = 0.0
    private var currentCategory = ""
    private var subcategoryColours: MutableList<SubCategoryColour> = ArrayList()
    private var job: Job = Job()
    private val args: TrackerFragmentArgs by navArgs()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (binding.barChart.visibility == View.GONE && // it's a pie chart
                currentCategory != "") { // ie it's a pie chart AND user has drilled down
                currentCategory = ""
                loadPieChart(currentCategory)
            } else
                findNavController().popBackStack()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun initCurrentBudgetMonth() {
        currentBudgetMonth = MyDate(
            gCurrentDate.getYear(), gCurrentDate.getMonth(), 1)
        if (DefaultsViewModel.getDefaultViewByTracker() == cPeriodYear) {
            currentBudgetMonth.representsYear = true
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (parentFragment is HomeFragment) { // ie on home page
            // remove top padding
            binding.constraintLayout.setPadding(
                binding.constraintLayout.paddingLeft,
                0,
                binding.constraintLayout.paddingRight,
                binding.constraintLayout.paddingBottom
            )
            binding.buttonLayout.visibility = View.GONE
        }
        if (!TransactionViewModel.isLoaded())
            return
        initCurrentBudgetMonth()
        if (currentBudgetMonth.representsYear) {
            currentBudgetMonth = MyDate(
                gCurrentDate.getYear(), gCurrentDate.getMonth(), 1
            )
        }
        when (DefaultsViewModel.getDefaultFilterDiscTracker()) {
            cDiscTypeDiscretionary -> {
                binding.discRadioButton.isChecked = true
            }
            cDiscTypeNondiscretionary -> {
                binding.nonDiscRadioButton.isChecked = true
            }
            else -> {
                binding.allDiscRadioButton.isChecked = true
            }
        }
        when (DefaultsViewModel.getDefaultFilterWhoTracker()) {
            0 -> binding.name1RadioButton.isChecked = true
            1 -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        if (SpenderViewModel.singleUser()) {
            binding.whoLayout.visibility = View.GONE
        }
        when (DefaultsViewModel.getDefaultShowTotalsTracker()) {
            "#" -> binding.showDollarRadioButton.isChecked = true
            "%" -> binding.showPercentageRadioButton.isChecked = true
        }
        when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodMonth -> binding.buttonViewMonth.isChecked = true
            cPeriodYear -> {
                binding.buttonViewYear.isChecked = true
                currentBudgetMonth.representsYear = true
            }
            cPeriodYTD -> binding.buttonViewYtd.isChecked = true
            else -> binding.buttonViewAllTime.isChecked = true
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)

        if (parentFragment is HomeFragment) { // ie on home page
            binding.buttonLayout.visibility = View.GONE
        } else { // ie on Tracker page
            binding.buttonLayout.visibility = View.VISIBLE
            binding.buttonLayout.visibility = View.VISIBLE
            binding.buttonSettings.setOnClickListener {
                binding.optionsLinearLayout.visibility = View.VISIBLE
                binding.navButtonLinearLayout.visibility = View.GONE
            }
            binding.optionsLinearLayout.setOnTouchListener(object :
                OnSwipeTouchListener(requireContext()) {
                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    binding.optionsLinearLayout.visibility = View.GONE
                    binding.navButtonLinearLayout.visibility = View.VISIBLE
                }
            })
            binding.numericTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val radioButton = requireActivity().findViewById(checkedId) as RadioButton
                setPieGraphNumericStyle(binding.actualPieChart, radioButton.text.toString())
                setPieGraphNumericStyle(binding.budgetPieChart, radioButton.text.toString())
                binding.actualPieChart.invalidate()
                binding.budgetPieChart.invalidate()
            }
            binding.buttonBackward.setOnClickListener {
                moveOneMonthBackward()
            }
            binding.buttonForward.setOnClickListener {
                moveOneMonthForward()
            }
            binding.buttonViewMonth.setOnClickListener {
                DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_BY_TRACKER, cPeriodMonth)
                currentBudgetMonth = MyDate(
                    gCurrentDate.getYear(), gCurrentDate.getMonth(), 1)
                startLoadData()
            }
            binding.buttonViewYear.setOnClickListener {
                DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_BY_TRACKER, cPeriodYear)
                currentBudgetMonth.representsYear = true
                startLoadData()
            }
            binding.buttonViewYtd.setOnClickListener {
                DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_BY_TRACKER, cPeriodYTD)
                currentBudgetMonth = MyDate(
                    gCurrentDate.getYear(), gCurrentDate.getMonth(), 1)
                startLoadData()
            }
            binding.buttonViewAllTime.setOnClickListener {
                DefaultsViewModel.updateDefaultString(cDEFAULT_VIEW_BY_TRACKER, cPeriodAllTime)
                currentBudgetMonth = MyDate(
                    gCurrentDate.getYear(), gCurrentDate.getMonth(), 1)
                startLoadData()
            }
            binding.filterDiscRadioGroup.setOnCheckedChangeListener { _, optionId ->
                when (optionId) {
                    R.id.discRadioButton -> {
                        DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeDiscretionary)
                        startLoadData()
                    }
                    R.id.nonDiscRadioButton -> {
                        DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeNondiscretionary)
                        startLoadData()
                    }
                    R.id.allDiscRadioButton -> {
                        DefaultsViewModel.updateDefaultString(cDEFAULT_FILTER_DISC_TRACKER, cDiscTypeAll)
                        startLoadData()
                    }
                }
            }
            binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
                when (optionId) {
                    R.id.name1RadioButton -> {
                        DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 0)
                        startLoadData()
                    }
                    R.id.name2RadioButton -> {
                        DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 1)
                        startLoadData()
                    }
                    R.id.whoAllRadioButton -> {
                        DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 2)
                        startLoadData()
                    }
                }
            }
            HintViewModel.showHint(parentFragmentManager, cHINT_TRACKER)
            if (TransactionViewModel.getCount() > 0 && CategoryViewModel.getCount() > 0) {
                if (args.type == "Pie") {
                    hideBarChart()
                    loadPieChart(currentCategory)
                } else {
                    hidePieChart()
                    launch {
                        loadBarChart()
                    }
                }
            }
        }
    }
    private fun moveOneMonthBackward() {
        if (DefaultsViewModel.getDefaultViewByTracker() != cPeriodYTD &&
            DefaultsViewModel.getDefaultViewByTracker() != cPeriodAllTime) {
            if (currentBudgetMonth.representsYear)
                currentBudgetMonth.increment(cPeriodYear, -1)
            else
                currentBudgetMonth.increment(cPeriodMonth, -1)
            startLoadData()
        }
    }
    private fun moveOneMonthForward() {
        if (DefaultsViewModel.getDefaultViewByTracker() != cPeriodYTD &&
            DefaultsViewModel.getDefaultViewByTracker() != cPeriodAllTime) {
            if (currentBudgetMonth.representsYear)
                currentBudgetMonth.increment(cPeriodYear, 1)
            else
                currentBudgetMonth.increment(cPeriodMonth, 1)
            startLoadData()
        }
    }
    private fun startLoadData() {
        Timber.tag("Alex").d("startLoadData")
        if (binding.barChart.visibility == View.VISIBLE) {
            loadBarChart()
        } else
            loadPieChart(currentCategory)
    }
    private fun setPieGraphNumericStyle(pieChart: PieChart, iStyle: String) {
        when (iStyle) {
            "%" -> {
                pieChart.setUsePercentValues(true)
                pieChart.data.setValueFormatter(PercentFormatter(pieChart))
                setChartTitle()
            }
            "#" -> {
                pieChart.setUsePercentValues(false)
                pieChart.data.setValueFormatter(MyYAxisValueFormatter())
                setChartTitle()
            }
        }
    }

    fun loadBarChart() {
        binding.chartTitle.visibility = View.VISIBLE
        binding.barChart.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        initializeBarChart()
        val bcd: ArrayList<DataObject> = getBarChartData()
        createBarChart(bcd)
        if (hackActualTotal == 0.0 && hackBudgetTotal == 0.0) {
            hideBarChart()
            binding.chartSummaryText.visibility = View.VISIBLE
            binding.chartSummaryText.text = getString(R.string.no_tracker_data)
        }
    }

    fun hideBarChart() {
        binding.chartTitle.visibility = View.GONE
        binding.chartSubTitle.visibility = View.GONE
        binding.barChart.visibility = View.GONE
        binding.chartSummaryText.visibility = View.GONE
    }

    private fun loadPieChart(iSpecificCategory: String = "") {
        binding.chartTitle.visibility = View.VISIBLE
        binding.actualPieChart.visibility = View.VISIBLE
        binding.budgetPieChart.visibility = View.VISIBLE
        binding.numericTypeRadioGroup.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        binding.showDeltaLayout.visibility = View.VISIBLE
        initializePieChart(getString(R.string.actual), binding.actualPieChart)
        initializePieChart(getString(R.string.budget), binding.budgetPieChart)
        // budget pie must be created first so that totals comparisons work
        createPieChart(binding.budgetPieChart, getBudgetPieChartData(iSpecificCategory), getString(R.string.budget))
        createPieChart(binding.actualPieChart, getActualPieChartData(iSpecificCategory), getString(R.string.actual))
        if (iSpecificCategory != "") {
            binding.chartSubTitle.visibility = View.VISIBLE
            if (binding.chartSubTitle.text == "")
                binding.chartSubTitle.text = iSpecificCategory
            else
                binding.chartSubTitle.text = String.format(getString(R.string.two_items),
                    iSpecificCategory, binding.chartSubTitle.text)
        }
    }

    private fun hidePieChart() {
        binding.chartTitle.visibility = View.GONE
        binding.actualPieChart.visibility = View.GONE
        binding.budgetPieChart.visibility = View.GONE
        binding.showDeltaLayout.visibility = View.GONE
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
        binding.barChart.axisLeft.isEnabled = false
        binding.barChart.xAxis.isEnabled = true
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
        binding.barChart.xAxis.xOffset = 250F
        binding.barChart.axisLeft.axisMinimum = 0F
        binding.barChart.xAxis.textSize = 13f
        binding.barChart.axisLeft.valueFormatter = (MyYAxisValueFormatter())
        binding.barChart.setDrawValueAboveBar(true)

        binding.barChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
            override fun onNothingSelected() {
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (parentFragment is HomeFragment) {
                    view?.findNavController()?.navigate(R.id.TrackerTabsFragment)
                    return
                }
                when (e?.x) {
                    0F -> {
                        // Budget this month
                        val action =
                            TrackerTabsFragmentDirections.actionTrackerTabsFragmentToSettingsTabsFragment()
                        action.targetTab = 2
                        action.year = currentBudgetMonth.getYear().toString()
                        if (!currentBudgetMonth.representsYear)
                            action.month = currentBudgetMonth.getMonth().toString()
                        findNavController().navigate(action)
                    }
                    1F -> {
                        // Budget to date
                        val action =
                            TrackerTabsFragmentDirections.actionTrackerTabsFragmentToSettingsTabsFragment()
                        action.targetTab = 2
                        action.year = currentBudgetMonth.getYear().toString()
                        if (!currentBudgetMonth.representsYear)
                            action.month = currentBudgetMonth.getMonth().toString()
                        else {
                            action.month = (gCurrentDate.getMonth()).toString()
                        }
                        findNavController().navigate(action)
                    }
                    2F -> {
                        // Actuals
                        MyApplication.transactionSearchText = currentBudgetMonth.getYear().toString()
                        if (!currentBudgetMonth.representsYear) {
                            MyApplication.transactionSearchText = "${MyApplication.transactionSearchText}-%02d".format(currentBudgetMonth.getMonth())
                        }
                        view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
                    }
                }
            }
        })

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
                    view?.findNavController()?.navigate(R.id.TrackerTabsFragment)
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

    private fun getBarChartData(): ArrayList<DataObject> {
        val tList = ArrayList<DataObject>()

        val discFilter = DefaultsViewModel.getDefaultFilterDiscTracker()
        val whoFilter = DefaultsViewModel.getDefaultFilterWhoTracker()

        val viewPeriod = when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodAllTime -> DateRangeEnum.ALLTIME
            cPeriodYTD -> DateRangeEnum.YTD
            cPeriodYear -> DateRangeEnum.YEAR
            else -> DateRangeEnum.MONTH
        }
//        val totalBudget = BudgetViewModel.getTotalCalculatedBudgetForMonth(currentBudgetMonth, discFilter, whoFilter)
        var totalBudget = 0.0
        val catBudgets = BudgetViewModel.getCategoryBudgets(
            viewPeriod,
            currentBudgetMonth,
            discFilter,
            whoFilter,
            ""
        )
        for (budget in catBudgets) {
            totalBudget += budget.value.toFloat()
        }

        var totalBudgetToDate = 0.0
        var daysInMonth = 1

        if (viewPeriod == DateRangeEnum.MONTH) {
            if (currentBudgetMonth.getYear() == gCurrentDate.getYear() &&
                currentBudgetMonth.getMonth() == gCurrentDate.getMonth()) {
                daysInMonth = getDaysInMonth(gCurrentDate)
                totalBudgetToDate =
                    totalBudget * gCurrentDate.getDay() / daysInMonth
            } else if (currentBudgetMonth.getYear() > gCurrentDate.getYear() ||
                    (currentBudgetMonth.getYear() == gCurrentDate.getYear() &&
                            currentBudgetMonth.getMonth() > gCurrentDate.getMonth())) {
                // looking at a future month
                totalBudgetToDate = 0.0
            } else { // looking at a past month
                totalBudgetToDate = totalBudget
            }
        } else if (viewPeriod == DateRangeEnum.YTD) {
            val currentCatBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, currentBudgetMonth, discFilter, whoFilter, "")
            var totalCurrentBudget = 0.0
            for (budget in currentCatBudgets) {
                totalCurrentBudget += budget.value.toFloat()
            }

            daysInMonth = getDaysInMonth(gCurrentDate)
            val thisMonthBudget =
                totalCurrentBudget * gCurrentDate.getDay() / daysInMonth
            totalBudgetToDate = totalBudget - totalCurrentBudget + thisMonthBudget
        } else if (viewPeriod == DateRangeEnum.YEAR) {
            val thisMonth = MyDate(currentBudgetMonth.getYear(), gCurrentDate.getMonth(), 1)
            val ytdBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.YTD, thisMonth, discFilter, whoFilter, "")
            var totalYTDBudget = 0.0
            for (budget in ytdBudgets) {
                totalYTDBudget += budget.value.toFloat()
            }

            val currentCatBudgets = BudgetViewModel.getCategoryBudgets(DateRangeEnum.MONTH, thisMonth, discFilter, whoFilter, "")
            var totalCurrentBudget = 0.0
            for (budget in currentCatBudgets) {
                totalCurrentBudget += budget.value.toFloat()
            }

            daysInMonth = getDaysInMonth(gCurrentDate)
            val thisMonthBudget =
                totalCurrentBudget * gCurrentDate.getDay() / daysInMonth
            totalBudgetToDate = totalYTDBudget - totalCurrentBudget + thisMonthBudget
        } else { // all-time, don't show this row

        }

        val startDate = when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodAllTime -> MyDate(TransactionViewModel.getEarliestYear(), 1, 1)
            cPeriodYTD, cPeriodYear -> MyDate(currentBudgetMonth.getYear(), 1, 1)
            else -> currentBudgetMonth.getFirstOfMonth()
        }

        val endDate = when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodAllTime -> MyDate(TransactionViewModel.getLatestYear(), 12, 31)
            cPeriodYear -> MyDate(currentBudgetMonth.getYear(), 12, 31)
            else -> currentBudgetMonth.getLastDayOfMonth()
        }

        val totalActualsToDate: Double = TransactionViewModel.getTotalActualsForRange(
                MyDate(startDate), MyDate(endDate),
                discFilter, whoFilter
            )
        hackActualTotal = totalActualsToDate
        tList.add(
            DataObject(
                0, getString(R.string.budget_this_period) + " " + gDecWithCurrency(totalBudget),
                totalBudget, "",
                ContextCompat.getColor(requireContext(), R.color.dark_gray)
            )
        )
        if (viewPeriod != DateRangeEnum.ALLTIME) {
            tList.add(
                DataObject(
                    0, getString(R.string.budget_to_date) + " (" + gCurrentDate.getDay() + "/" + daysInMonth + ") "+ gDecWithCurrency(totalBudgetToDate),
                    totalBudgetToDate, "",
                    ContextCompat.getColor(requireContext(), R.color.medium_gray)
                )
            )
        }
        val showRed = DefaultsViewModel.getDefaultShowRed().toFloat()

        when {
            totalActualsToDate > (totalBudgetToDate * (1 + showRed / 100)) -> tList.add(
                DataObject(
                    0, getString(R.string.actual) + " " + gDecWithCurrency(totalActualsToDate),
                    totalActualsToDate, "",
                    ContextCompat.getColor(requireContext(), R.color.red)
                )
            )
            totalActualsToDate > totalBudgetToDate -> tList.add(
                DataObject(
                    0, getString(R.string.actual) + " " + gDecWithCurrency(totalActualsToDate),
                    totalActualsToDate, "",
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                )
            )
            else -> tList.add(
                DataObject(
                    0, getString(R.string.actual) + " " + gDecWithCurrency(totalActualsToDate),
                    totalActualsToDate, "",
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            )
        }
        val lab = when (DefaultsViewModel.getDefaultFilterDiscTracker()) {
            cDiscTypeDiscretionary -> getString(R.string.discretionary).lowercase()
            cDiscTypeNondiscretionary -> getString(R.string.non_discretionary).lowercase()
            else -> ""
        }
        val lab2 = when (DefaultsViewModel.getDefaultFilterDiscTracker()) {
            cDiscTypeDiscretionary -> getString(R.string.discretionary).lowercase()
            cDiscTypeNondiscretionary -> getString(R.string.non_discretionary).lowercase()
            else -> getString(R.string.overall).lowercase()
        }
        if (totalActualsToDate > totalBudget)
            binding.chartSummaryText.text = String.format(getString(R.string.you_are_over_your_x_budget_this_period), lab)
//          binding.chartSummaryText.text = lab
        else {
            val remainingBudget = totalBudget - totalActualsToDate
            val daysRemaining = daysInMonth - gCurrentDate.getDay() + 1

            binding.chartSummaryText.text = String.format(getString(R.string.keeping_x_expenses_below),
                lab, gDecWithCurrency(remainingBudget / daysRemaining), lab2)
        }
        return tList
    }

    private fun createBarChart(iData: ArrayList<DataObject>) {
        val dataSets: ArrayList<IBarDataSet> = ArrayList()
        for (i in 0 until iData.size) { // for until loop excludes the "until" number
            val values: ArrayList<BarEntry> = ArrayList()
            val dataObject: DataObject = iData[i]
            values.add(BarEntry(i.toFloat(), dataObject.value.toFloat(), dataObject.color))
            val ds = BarDataSet(values, getString(R.string.data))
            ds.color = dataObject.color
            ds.setDrawValues(true)
            dataSets.add(ds)
        }
        val data = BarData(dataSets)
        binding.barChart.data = data
        binding.barChart.setVisibleXRange(1.0F, iData.size.toFloat())
        binding.barChart.setFitBars(true)
        binding.barChart.xAxis.textColor = MaterialColors.getColor(
            requireContext(),
            R.attr.textOnBackground,
            Color.BLACK
        )
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
        setChartTitle()
    }

    private fun setChartTitle() {
        binding.chartTitle.text = when (DefaultsViewModel.getDefaultViewByTracker()) {
                cPeriodAllTime -> getString(R.string.all_time)
                cPeriodYTD -> getString(R.string.ytd)
                cPeriodYear -> "${currentBudgetMonth.getYear()}"
                else -> gMonthName(currentBudgetMonth.getMonth()) + " " + currentBudgetMonth.getYear()
            }
        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefaultFilterDiscTracker() != cDiscTypeAll) {
            currentFilterIndicator = if (DefaultsViewModel.getDefaultFilterDiscTracker() == cDiscTypeDiscretionary)
                getString(R.string.discretionary)
            else
                getString(R.string.non_discretionary)
        }
        if (DefaultsViewModel.getDefaultFilterWhoTracker() != 2) {
            currentFilterIndicator = currentFilterIndicator + " " +
                SpenderViewModel.getSpenderName(DefaultsViewModel.getDefaultFilterWhoTracker())
        }
        if (currentFilterIndicator == "") {
            binding.chartSubTitle.visibility = View.GONE
            binding.chartSubTitle.text = ""
        } else {
            binding.chartSubTitle.visibility = View.VISIBLE
            binding.chartSubTitle.text = String.format(getString(R.string.bracketed), currentFilterIndicator)
        }
    }

    private fun getActualPieChartData(iSpecificCategory: String): PieDataSet {
        val discFilter = DefaultsViewModel.getDefaultFilterDiscTracker()
        val whoFilter = DefaultsViewModel.getDefaultFilterWhoTracker()

        val viewPeriod = when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodMonth -> DateRangeEnum.MONTH
            cPeriodYear -> DateRangeEnum.YEAR
            cPeriodYTD -> DateRangeEnum.YTD
            else -> DateRangeEnum.ALLTIME
        }

        val catActuals = TransactionViewModel.getCategoryActuals(currentBudgetMonth, viewPeriod, discFilter, whoFilter)
        Timber.tag("Alex").d("cat totals size ${catActuals.size}")
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()

        //input data and fit data into pie chart entry
        hackActualTotal = 0.0
        var lastCategory = getString(R.string.unknown)
        var lastColor = 0
        var groupActual = 0.0
        for (i in 0 until catActuals.size) {
            if (iSpecificCategory == "" || catActuals[i].label == iSpecificCategory) {
                val toCompare = if (iSpecificCategory == "") catActuals[i].label else CategoryViewModel.getCategory(catActuals[i].id)?.subcategoryName
                if ( lastCategory != toCompare && groupActual > 0.0) {
                    pieEntries.add(PieEntry(groupActual.toFloat(), lastCategory))
                    colors.add(lastColor)
                    hackActualTotal += groupActual.toFloat()
                    groupActual = 0.0
                }
                groupActual += catActuals[i].value
                if (iSpecificCategory == "") {
                    lastCategory = catActuals[i].label
                    lastColor = catActuals[i].color
                } else {
                    lastCategory = CategoryViewModel.getCategory(catActuals[i].id)?.subcategoryName.toString()
                    lastColor = getSubcategoryColour(CategoryViewModel.getCategory(catActuals[i].id)?.subcategoryName.toString())
                }
            }
        }
        if (catActuals.size > 0 && groupActual > 0.0) {
            pieEntries.add(PieEntry(groupActual.toFloat(), lastCategory))
            colors.add(lastColor)
/*            if (iSpecificCategory == "")
                colors.add(catActuals[catActuals.size-1].color)
            else {
                CategoryViewModel.getCategory(catActuals[catActuals.size-1].id)
                    ?.let { getSubcategoryColour(it.subcategoryName) }?.let { colors.add(it) }
            } */
            hackActualTotal += groupActual.toFloat()
        }
        //collecting the entries with label name
        val label = ""
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        val col = MaterialColors.getColor(
            requireContext(),
            R.attr.textOnBackground,
            Color.BLACK
        )
        pieDataSet.valueTextColor = col
        pieDataSet.valueLineColor =  col
        return pieDataSet
    }

    private fun getBudgetPieChartData(iSpecificCategory: String): PieDataSet {
        val discFilter = DefaultsViewModel.getDefaultFilterDiscTracker()
        val whoFilter = DefaultsViewModel.getDefaultFilterWhoTracker()
        val viewPeriod = when (DefaultsViewModel.getDefaultViewByTracker()) {
            cPeriodAllTime -> DateRangeEnum.ALLTIME
            cPeriodYTD -> DateRangeEnum.YTD
            cPeriodYear -> DateRangeEnum.YEAR
            else -> DateRangeEnum.MONTH
        }
        val catBudgets = BudgetViewModel.getCategoryBudgets(viewPeriod, currentBudgetMonth, discFilter, whoFilter, iSpecificCategory)
        val label = "" // "Category"
        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        //input data and fit data into pie chart entry
        hackBudgetTotal = 0.0
        for (budget in catBudgets) {
            hackBudgetTotal += budget.value.toFloat()
            pieEntries.add(PieEntry(budget.value.toFloat(), budget.label))
            if (iSpecificCategory == "")
                colors.add(DefaultsViewModel.getCategoryDetail(budget.label).color)
            else {
                colors.add(getSubcategoryColour(budget.label))
            }
        }
        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, label)
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        val col = MaterialColors.getColor(
            requireContext(),
            R.attr.textOnBackground,
            Color.BLACK
        )
        pieDataSet.valueTextColor = col
        pieDataSet.valueLineColor =  col
        return pieDataSet
    }
    private fun initializePieChart(iLabel: String, pieChart: PieChart) {
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
        //setting the color of the hole in the middle
        pieChart.setHoleColor(MaterialColors.getColor(
            requireContext(),
            R.attr.background,
            Color.BLACK))
        pieChart.setEntryLabelColor(MaterialColors.getColor(
            requireContext(),
            R.attr.textOnBackground,
            Color.BLACK))
        pieChart.legend.isEnabled = false
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

        if (iLabel == getString(R.string.actual)) {
            binding.actualPieChart.setOnChartValueSelectedListener(object :
                OnChartValueSelectedListener {
                override fun onNothingSelected() {
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val catName = h?.x?.toInt()
                        ?.let { pieChart.data.dataSet.getEntryForIndex(it).label }

                    if (currentCategory == "") { // ie clicking into category
                        currentCategory = catName.toString()
                        loadPieChart(currentCategory)
                    } else { // ie clicking into subcategory
                        MyApplication.transactionSearchText = "$currentCategory $catName ${currentBudgetMonth.getYear()}"
                        if (!currentBudgetMonth.representsYear) {
                            MyApplication.transactionSearchText = "${MyApplication.transactionSearchText}-%02d".format(currentBudgetMonth.getMonth())
                        }
                        view?.findNavController()?.navigate(R.id.TransactionViewAllFragment)
                    }
                }
            })
        } else {
            binding.budgetPieChart.setOnChartValueSelectedListener(object :
                OnChartValueSelectedListener {
                override fun onNothingSelected() {
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val catName = h?.x?.toInt()
                        ?.let { pieChart.data.dataSet.getEntryForIndex(it).label }

                    if (currentCategory == "") { // ie clicking into category
                        currentCategory = catName.toString()
                        loadPieChart(currentCategory)
                    } else { // ie clicking into subcategory
                        val catID = catName?.let { CategoryViewModel.getID(currentCategory, it) }
                        val action =
                            TrackerTabsFragmentDirections.actionTrackerTabsFragmentToSettingsTabsFragment()
                        action.targetTab = 2
                        action.categoryID = catID.toString()
                        findNavController().navigate(action)
                    }
                }
            })
        }
        pieChart.highlightValues(null) // makes sure no pie slice is showing as selected
    }

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
        val dm = activity?.resources?.displayMetrics
        val labelFontSize = 40
        val amountLabelFontSize = if (dm?.widthPixels!! <= 600)
            18
        else
            35
        val textColor = MaterialColors.getColor(
            requireContext(),
            R.attr.textOnBackground,
            Color.BLACK
        )
        val label = "$iDescription\n".setFontSizeForPath(0, labelFontSize, textColor)
        val amountLabel = if (iDescription == getString(R.string.budget))
            gDecWithCurrency(hackBudgetTotal).setFontSizeForPath(0, amountLabelFontSize, textColor)
        else {
            val col = if (hackActualTotal > hackBudgetTotal)
                ContextCompat.getColor(requireContext(), R.color.red)
            else
                ContextCompat.getColor(requireContext(), R.color.green)
            gDecWithCurrency(hackActualTotal).setFontSizeForPath(
                0,
                amountLabelFontSize,
                col
            )
        }
        if (dm.widthPixels <= 600) {
            label.setSpan(RelativeSizeSpan(1f), 0, label.length, 0)
            amountLabel.setSpan(RelativeSizeSpan(1f), 0, amountLabel.length, 0)
        } else {
            label.setSpan(RelativeSizeSpan(1.3f), 0, label.length, 0)
            amountLabel.setSpan(RelativeSizeSpan(1.3f), 0, amountLabel.length, 0)
        }
        label.setSpan(StyleSpan(Typeface.BOLD), 0, label.length, 0)
        amountLabel.setSpan(StyleSpan(Typeface.BOLD), 0, amountLabel.length, 0)
        val overallLabel = TextUtils.concat(label, amountLabel)
        pieChart.centerText = overallLabel

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

    private fun getSubcategoryColour(iLabel: String) : Int {
        subcategoryColours.forEach {
            if (it.label == iLabel) {
                return it.colour
            }
        }
        val newColour = Color.argb(255, Random().nextInt(256), Random().nextInt(256), Random().nextInt(256))
        subcategoryColours.add(SubCategoryColour(iLabel, newColour))
        return newColour
    }
}

class MyYAxisValueFormatter : ValueFormatter() {
    private val mFormat: DecimalFormat = DecimalFormat("$ ###,###,##0.00")
    override fun getFormattedValue(value: Float): String {
        //write your logic here
        //access the YAxis object to get more information
        return mFormat.format(value.toDouble())
    }

}

data class SubCategoryColour (val label: String, val colour: Int)

