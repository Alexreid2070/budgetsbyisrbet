package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTrackerLineChartBinding
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt


class TrackerLineChartFragment : Fragment() {
    private var _binding: FragmentTrackerLineChartBinding? = null
    private val binding get() = _binding!!
    private val args: TrackerLineChartFragmentArgs by navArgs()
    private var currentCategoryID = 0
    private var incomingCategoryUsed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentTrackerLineChartBinding.inflate(inflater, container, false)

        currentCategoryID = args.categoryID
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!TransactionViewModel.isLoaded())
            return
        when (DefaultsViewModel.getDefaultFilterWhoTracker()) {
            0 -> binding.name1RadioButton.isChecked = true
            1 -> binding.name2RadioButton.isChecked = true
            else -> binding.whoAllRadioButton.isChecked = true
        }
        if (SpenderViewModel.singleUser()) {
            binding.whoLayout.visibility = View.GONE
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)

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
        binding.filterWhoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.name1RadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 0)
                    loadLineChart("name1")
                }
                R.id.name2RadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 1)
                    loadLineChart("name2")
                }
                R.id.whoAllRadioButton -> {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_FILTER_WHO_TRACKER, 2)
                    loadLineChart("nameAll")
                }
            }
        }
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.categorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.categorySpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.subcategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.subcategorySpinner.setPopupBackgroundResource(R.drawable.spinner)

        binding.categorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (incomingCategoryUsed || currentCategoryID == 0) {
                    addSubCategories(binding.categorySpinner.selectedItem.toString(), "")
                } else {
                    val cat = CategoryViewModel.getCategory(currentCategoryID)
                    if (cat != null) {
                        addSubCategories(binding.categorySpinner.selectedItem.toString(), cat.subcategoryName)
                    }
                    incomingCategoryUsed = true
                }
            }
        }

        binding.subcategorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadLineChart("subcategorySpinner onItemSelected")
            }
        }
        binding.buttonBackward.setOnClickListener {
            changeCategory(-1)
        }
        binding.buttonForward.setOnClickListener {
            changeCategory(1)
        }

        val cat = CategoryViewModel.getCategory(currentCategoryID)
        if (cat == null) {
            addCategories("")
//            addSubCategories(binding.categorySpinner.selectedItem.toString(), "")
        } else {
            addCategories(cat.categoryName)
  //          addSubCategories(cat.categoryName, cat.subcategoryName)
        }
        HintViewModel.showHint(parentFragmentManager, cHINT_TRACKER)
//        if (TransactionViewModel.getCount() > 0 && CategoryViewModel.getCount() > 0) {
  //          loadLineChart()
    //    }
    }
    private fun addCategories(iCategory: String) {
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoriesForSpinner())
        binding.categorySpinner.adapter = arrayAdapter
        if (iCategory == "")
            binding.categorySpinner.setSelection(0)
        else
            binding.categorySpinner.setSelection(arrayAdapter.getPosition(iCategory))
        arrayAdapter.notifyDataSetChanged()
//        addSubCategories(iCategory, iSubCategory)
    }
    private fun addSubCategories(iCategory: String, iSubCategory: String) {
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            CategoryViewModel.getSubcategoriesForSpinner(iCategory, iSubCategory, true))
        binding.subcategorySpinner.adapter = arrayAdapter
        if (iSubCategory == "")
//            binding.subcategorySpinner.performClick()
            binding.subcategorySpinner.setSelection(0)
        else
            binding.subcategorySpinner.setSelection(arrayAdapter.getPosition(iSubCategory))
        arrayAdapter.notifyDataSetChanged()
    }

    private fun changeCategory(iDirection: Int) {
        val nextCat = CategoryViewModel.getNextCategory(currentCategoryID, iDirection)
        if (nextCat != null) {
            currentCategoryID = nextCat.id
            incomingCategoryUsed = false
            if (binding.categorySpinner.selectedItem.toString() == nextCat.categoryName) {
                val currentSubcategoryPosition = binding.subcategorySpinner.selectedItemPosition
                binding.subcategorySpinner.setSelection(currentSubcategoryPosition+iDirection)
            } else {
                val adCnt = binding.categorySpinner.adapter.count
                if (iDirection == 1) {
                    if (binding.categorySpinner.selectedItemPosition == adCnt-1)
                        binding.categorySpinner.setSelection(0)
                    else
                        binding.categorySpinner.setSelection(binding.categorySpinner.selectedItemPosition + 1)
                } else {
                    if (binding.categorySpinner.selectedItemPosition == 0)
                        binding.categorySpinner.setSelection(adCnt-1)
                    else
                        binding.categorySpinner.setSelection(binding.categorySpinner.selectedItemPosition - 1)
                }
            }
        }
    }

    private fun loadLineChart(iTag: String) {
        binding.chartTitle.visibility = View.VISIBLE
        binding.lineChart.visibility = View.VISIBLE
        initializeLineChart()
        val whoFilter = DefaultsViewModel.getDefaultFilterWhoTracker()
        currentCategoryID = CategoryViewModel.getID(binding.categorySpinner.selectedItem.toString(),
            binding.subcategorySpinner.selectedItem.toString())
        val lineChartData = getLineChartData(currentCategoryID, whoFilter)
        var avg = 0.0
        for (i in 0 until lineChartData.size) {
            avg += lineChartData[i]
        }
        avg /= lineChartData.size
        val avgData: ArrayList<Double> = arrayListOf()
        for (i in 0 until lineChartData.size) {
            avgData.add(avg)
        }
        createLineChart(
            lineChartData,
            avgData,
            BudgetViewModel.getCategoryBudgetsForEveryMonth(currentCategoryID, whoFilter),
            TransactionViewModel.getDateRange())
    }
    private fun initializeLineChart() {
        binding.lineChart.description.isEnabled = false
        binding.lineChart.setMaxVisibleValueCount(3)
        binding.lineChart.xAxis.setDrawGridLines(false)
        binding.lineChart.setPinchZoom(true)
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.setDrawGridBackground(false)
        val xAxis: XAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.labelRotationAngle = 270f
        xAxis.isEnabled = true
        xAxis.textSize = 10f
        xAxis.textColor = MaterialColors.getColor(requireContext(), R.attr.textOnBackground, Color.BLACK)

        binding.lineChart.axisLeft.setDrawGridLines(true)
        binding.lineChart.axisLeft.isEnabled = true
        binding.lineChart.axisLeft.axisMinimum = 0F
        binding.lineChart.axisLeft.valueFormatter = (MyYAxisValueFormatter())
        binding.lineChart.axisLeft.textColor = MaterialColors.getColor(requireContext(), R.attr.textOnBackground, Color.BLACK)

        binding.lineChart.axisRight.isEnabled = false
        binding.lineChart.animateY(1000)
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.isDoubleTapToZoomEnabled = false

        binding.lineChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
            override fun onNothingSelected() {
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                Timber.tag("Alex").d("e is $e")
                val action =
                    TrackerTabsFragmentDirections.actionTrackerTabsFragmentToTransactionViewAllFragment()
                action.categoryID = currentCategoryID
                findNavController().navigate(action)
            }
        })
        binding.lineChart.legend.isEnabled = true
        binding.lineChart.legend.textSize = 15f
        binding.lineChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.lineChart.legend.textColor = MaterialColors.getColor(requireContext(), R.attr.textOnBackground, Color.BLACK)
        binding.lineChart.invalidate()
    }

    private fun getLineChartData(iCategoryID: Int, iWho: Int): ArrayList<Double> {
        Timber.tag("Alex").d("Getting data for who $iWho")
        return TransactionViewModel.getCategoryActualsForEveryMonth(iCategoryID, iWho)
    }

    private fun createLineChart(iActuals: ArrayList<Double>, iAverage: ArrayList<Double>,
                                iBudgets: ArrayList<Double>, iDates: ArrayList<MyDate>) {
//        Timber.tag("Alex").d("Sizes are ${iActuals.size} ${iAverage.size} ${iBudgets.size} ${iDates.size}")
  //      Timber.tag("Alex").d("Dates: $iDates")
    //    Timber.tag("Alex").d("Budgets: $iBudgets")
      //  Timber.tag("Alex").d("Actuals: $iActuals")

        val values: ArrayList<Entry> = arrayListOf()
        for (i in 0 until iActuals.size) {
            values.add(Entry(i.toFloat(), iActuals[i].toFloat()))
        }
        val ds = LineDataSet(values, getString(R.string.actual))
        ds.setDrawCircles(true)
        ds.enableDashedLine(10F, 0F, 0F)
        ds.enableDashedHighlightLine(10F, 0F, 0F)
        ds.color = MaterialColors.getColor(requireContext(), R.attr.colorPrimary, Color.BLACK)
        ds.setCircleColor(MaterialColors.getColor(requireContext(), R.attr.colorPrimary, Color.BLACK))
        ds.lineWidth = 2f
        ds.circleRadius = 3f
        ds.setDrawCircleHole(true)
        ds.valueTextSize = 10f
        ds.setDrawFilled(false)
        ds.setDrawValues(true)
        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(ds)

        val bValues: ArrayList<Entry> = arrayListOf()
        for (i in 0 until iBudgets.size) {
            bValues.add(Entry(i.toFloat(), iBudgets[i].toFloat()))
        }
        val bDS = LineDataSet(bValues, getString(R.string.budget))
        bDS.color = MaterialColors.getColor(requireContext(), R.attr.budgetLineGraphColor, Color.BLACK)
        bDS.fillColor = MaterialColors.getColor(requireContext(), R.attr.budgetLineGraphColor, Color.BLACK)
        bDS.setDrawCircles(false)
        bDS.setDrawFilled(true)
        dataSets.add(bDS)

        val avgValues: ArrayList<Entry> = arrayListOf()
        for (i in 0 until iAverage.size) {
            avgValues.add(Entry(i.toFloat(), iAverage[i].toFloat()))
        }
        val avgDS = LineDataSet(avgValues, "${getString(R.string.average)} (\$ ${gDecWithCurrency(iAverage[0].roundToInt())})")
        avgDS.setDrawFilled(false)
        avgDS.setDrawCircles(false)
        val color2 = MaterialColors.getColor(requireContext(), R.attr.colorSecondary, Color.BLACK)
        avgDS.color = color2
        avgDS.enableDashedLine(10f, 10f, 0f)
        avgDS.lineWidth = 5f
        dataSets.add(avgDS)

        val data = LineData(dataSets)
        binding.lineChart.data = data
        val xAxis: XAxis = binding.lineChart.xAxis
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        binding.lineChart.data.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        setChartTitle(iAverage[0])
        binding.lineChart.xAxis.valueFormatter = ClaimsXAxisValueFormatter(iDates)
        binding.lineChart.axisLeft.valueFormatter = ClaimsYAxisValueFormatter()
    }

    private fun setChartTitle(iAverage: Double) {
        binding.chartTitle.text = CategoryViewModel.getFullCategoryName(currentCategoryID)
        var currentFilterIndicator = ""
        if (DefaultsViewModel.getDefaultFilterWhoTracker() != 2) {
            currentFilterIndicator =
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

    class ClaimsXAxisValueFormatter(private var datesList: ArrayList<MyDate>) :
        ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase): String {
            val position = value.roundToInt()
            return if (position < datesList.size)
                datesList[position].getMMMYY()
            else ""
        }
    }
    class ClaimsYAxisValueFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase): String {
            return value.roundToInt().toString()
        }
    }
}