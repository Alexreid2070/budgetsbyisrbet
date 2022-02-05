package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentTrackerBinding
import java.text.DecimalFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TrackerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrackerFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
        if (ExpenditureViewModel.getCount() > 1 && CategoryViewModel.getCount() > 1)
            loadGraph()
        if (parentFragment is HomeFragment){
            Log.d("debug", "you are in Home")
            // remove top padding
            binding.constraintLayout.setPadding(binding.constraintLayout.paddingLeft, 0, binding.constraintLayout.paddingRight, binding.constraintLayout.paddingBottom)
        } else  {
            Log.d("debug", "you are in Host (ie on Tracker page)")
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

    fun loadGraph() {
        binding.chartTitle.visibility = View.VISIBLE
        binding.barChart.visibility = View.VISIBLE
        binding.chartSummaryText.visibility = View.VISIBLE
        initializeBarChart()
        createBarChart(getGraphData())
    }

    fun hideGraph() {
        binding.chartTitle.visibility = View.GONE
        binding.barChart.visibility = View.GONE
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

        binding.barChart.onChartGestureListener = object : OnChartGestureListener  {
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
                Log.d("Alex", "Tapped!")
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
    private fun getGraphData() : ArrayList<DataObject> {
        val tList = ArrayList<DataObject>()
        val dateNow = android.icu.util.Calendar.getInstance()
        val daysInMonth = getDaysInMonth(dateNow)
        val totalDiscBudget = BudgetViewModel.getTotalDiscretionaryBudgetForMonth(dateNow)
        val totalDiscBudgetToDate =
            totalDiscBudget * dateNow.get(Calendar.DATE) / daysInMonth
        val totalDiscActualsToDate =
            ExpenditureViewModel.getTotalDiscretionaryActualsToDate(dateNow)
        Log.d("Alex", "amounts are $totalDiscBudget and $totalDiscBudgetToDate and $totalDiscActualsToDate")
        tList.add(DataObject("Budget this month", totalDiscBudget, ContextCompat.getColor(requireContext(), R.color.dark_gray)))
        tList.add(DataObject("Budget to date", totalDiscBudgetToDate, ContextCompat.getColor(requireContext(), R.color.medium_gray)))
        val showRed = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()

        if (totalDiscActualsToDate > (totalDiscBudgetToDate * (1+showRed/100)))
            tList.add(DataObject("Actuals", totalDiscActualsToDate, ContextCompat.getColor(requireContext(), R.color.red)))
        else if (totalDiscActualsToDate > totalDiscBudgetToDate)
            tList.add(DataObject("Actuals", totalDiscActualsToDate, ContextCompat.getColor(requireContext(), R.color.yellow)))
        else
            tList.add(DataObject("Actuals", totalDiscActualsToDate, ContextCompat.getColor(requireContext(), R.color.green)))
        if (totalDiscActualsToDate > totalDiscBudget)
            binding.chartSummaryText.text = "You are over your discretionary budget this month."
        else {
            val remainingBudget = totalDiscBudget - totalDiscActualsToDate
            val daysRemaining = daysInMonth - dateNow.get(Calendar.DATE) + 1
            val dollarFormat = DecimalFormat("$###.00")

            binding.chartSummaryText.text =
                "Keeping discretionary expenses below " + dollarFormat.format(remainingBudget/daysRemaining) + " per day will keep you within budget this month."
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
//            binding.barChart.invalidate()
            binding.barChart.data.notifyDataChanged()
            binding.barChart.notifyDataSetChanged()
//        }
        val dateNow = android.icu.util.Calendar.getInstance()
        binding.chartTitle.text = "Discretionary Expense Tracker - " +
                MonthNames[dateNow.get(Calendar.MONTH)] + " " +
                dateNow.get(Calendar.YEAR)
//                (if ((dateNow.get(Calendar.MONTH)+1) < 10 ) "0" else "") +
//                        (dateNow.get(Calendar.MONTH)+1)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TrackerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TrackerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class DataObject(var label: String, var value: Double, var color: Int)

class MyYAxisValueFormatter : ValueFormatter() {
    private val mFormat: DecimalFormat = DecimalFormat("$ ###,###,##0")
    override fun getFormattedValue(value: Float): String {
        //write your logic here
        //access the YAxis object to get more information
        return mFormat.format(value.toDouble())
    }

}

