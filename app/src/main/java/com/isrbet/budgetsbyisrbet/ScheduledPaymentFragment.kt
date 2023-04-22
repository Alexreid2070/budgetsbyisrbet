package com.isrbet.budgetsbyisrbet

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentScheduledPaymentBinding
import timber.log.Timber


class ScheduledPaymentFragment : Fragment() {

    private var _binding: FragmentScheduledPaymentBinding? = null
    private val binding get() = _binding!!
    private var gestureDetector: GestureDetectorCompat? = null
    private var secondSwipeUp = 0
    private var secondSwipeDown = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spListObserver = Observer<MutableList<ScheduledPayment>> {
            val rows = ScheduledPaymentViewModel.getCopyOfScheduledPayments()
            val adapter = ScheduledPaymentAdapter(requireContext(), rows)
            binding.scheduledPaymentListView.adapter = adapter
            buildCalendarView()
        }
        ScheduledPaymentViewModel.observeList(this, spListObserver)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduledPaymentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val rows = ScheduledPaymentViewModel.getCopyOfScheduledPayments()
        val adapter = ScheduledPaymentAdapter(requireContext(), rows)
        binding.scheduledPaymentListView.adapter = adapter

        gestureDetector = GestureDetectorCompat(requireActivity(), object:
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (event2.y < event1.y) { // swiped up.  If at bottom already...
                    if (!binding.scheduledPaymentListView.canScrollVertically(1)) { // ie can't scroll up anymore
                        secondSwipeUp++
                        if (secondSwipeUp == 1)
                            hideCalendar()
                    }
                } else
                    secondSwipeUp = 0
                if (event2.y > event1.y) { // swiped down.  If at top already...
                    if (!binding.scheduledPaymentListView.canScrollVertically(-1)) { // ie can't scroll down anymore
                        secondSwipeDown++
                        if (secondSwipeDown == 1)
                            showCalendar()
                    }
                } else
                    secondSwipeDown = 0
                return true
            }
        })
        binding.scheduledPaymentListView.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1 != null) {
                    try {
                        // this call is in a "try" because  sometimes it crashes.  So we want to ignore that gesture
                        gestureDetector?.onTouchEvent(p1)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                }
                return false
            }
        })

        binding.tableCalendarRows.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeTop() {
                super.onSwipeTop()
                binding.tableCalendarRows.visibility = View.GONE
                binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
            }
        })

        binding.expandButton.setOnClickListener {
            if (binding.tableCalendarRows.visibility == View.VISIBLE) {
                hideCalendar()
            } else {
                showCalendar()
            }
        }
        binding.scheduledPaymentListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = binding.scheduledPaymentListView.getItemAtPosition(position) as ScheduledPayment
                val rtdf = ScheduledPaymentEditDialogFragment.newInstance(itemValue.mykey)
                rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
            }
        if (rows.size == 0) {
            binding.noInformationText.visibility = View.VISIBLE
            binding.noInformationText.text = getString(R.string.you_have_not_yet_entered_any_scheduled_payments)
        } else {
            binding.noInformationText.visibility = View.GONE
        }
        binding.addFab.setOnClickListener {
            addScheduledPayment()
        }
        var firstVisibleItemInList = -1
        var lastVisibleItemInList = -1

        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
        HintViewModel.showHint(parentFragmentManager, cHINT_SCHEDULED_PAYMENT)

//        buildCalendarView() not needed since the observer triggers to start
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showCalendar() {
        binding.tableCalendarRows.visibility = View.VISIBLE
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
    }
    private fun hideCalendar() {
        binding.tableCalendarRows.visibility = View.GONE
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
    }

    private fun addScheduledPayment() {
        val rtdf = ScheduledPaymentEditDialogFragment.newInstance("")
        rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
    }

    private fun buildCalendarView() {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        binding.tableCalendarRows.removeAllViews()
        val weekLabelRow = getNewTableRow()
        for (d in 0 until 7) {
            val tv = weekLabelRow.getChildAt(d) as TextView
            tv.setTypeface(null, Typeface.BOLD)
            tv.text = days[d].substring(0,3)
        }
        binding.tableCalendarRows.addView(weekLabelRow)

        val tDate = MyDate(gCurrentDate).increment(cPeriodDay, -(gCurrentDate.theDate.dayOfWeek.value))
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.colorPrimary, Color.BLACK), cOpacity)
        for (w in 0 until 5) { // ie 5 week view
            val headerRow = getNewTableRow(true, false)
            binding.tableCalendarRows.addView(headerRow) // day #s

            binding.tableCalendarRows.addView(getNewTableRow(false))
            val firstRowForSPs = binding.tableCalendarRows.childCount - 1

            for (d in 0 until 7) {
                val tv = headerRow.getChildAt(d) as TextView
                if (tDate == gCurrentDate)
                    tv.setBackgroundColor(Color.parseColor(hexColor))
                tv.text = if ((w == 0 && d == 0) || tDate.getDay() == 1)
                    underlined(tDate.getMMMDD())
                else
                    underlined(tDate.getDay().toString())
                val sps = ScheduledPaymentViewModel.getScheduledPaymentsOnDate(tDate.toString())
                var nextRowToUse = firstRowForSPs
                if (sps.isNotEmpty()) {
                    for (c in 0 until sps.count()) {
                        if (nextRowToUse <= binding.tableCalendarRows.childCount -1) {
                            val tRow = binding.tableCalendarRows.getChildAt(nextRowToUse) as TableRow
                            val ttv = tRow.getChildAt(d) as TextView
                            ttv.text = "${sps[c].vendor}"
                            ttv.tag = sps[c].mykey
                            ttv.setOnClickListener { tv -> // value of item that is clicked
                                val rtdf = ScheduledPaymentEditDialogFragment.newInstance(tv.tag.toString())
                                rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
                            }
                        } else {
                            // need to remove bottom border from row above
                            val tr = getNewTableRow(false)
                            binding.tableCalendarRows.addView(tr)
                            val ttv = tr.getChildAt(d) as TextView
                            ttv.text = "${sps[c].vendor}"
                            ttv.tag = sps[c].mykey
                            ttv.setOnClickListener { tv -> // value of item that is clicked
                                val rtdf = ScheduledPaymentEditDialogFragment.newInstance(tv.tag.toString())
                                rtdf.show(parentFragmentManager, getString(R.string.edit_scheduled_payment))
                            }
                        }
                        nextRowToUse += 1
                    }
                }
                tDate.increment(cPeriodDay, 1)
            }
        }
        addBottomFrameToTable()
    }
    private fun getNewTextView(iWantTop: Boolean = true, iWantBottom: Boolean = true) : TextView {
        val ntv = TextView(requireContext())
        ntv.layoutParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.MATCH_PARENT,
            1F)
        ntv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F) // 14F is default
        if (iWantTop && iWantBottom) {
            ntv.setBackgroundResource(R.drawable.row_thin_frame)
            ntv.setPadding(5, 5, 5, 5)
        } else if (iWantTop && !iWantBottom) {
                ntv.setBackgroundResource(R.drawable.row_thin_frame_no_bottom)
                ntv.setPadding(5, 5, 5, 5)
        } else {
            ntv.setBackgroundResource(R.drawable.row_thin_frame_sides_only)
            ntv.setPadding(5, 5, 5, 5)
        }
        return ntv
    }
    private fun getNewTableRow(iWantTop: Boolean = true, iWantBottom: Boolean = true) : TableRow {
        var tr = TableRow(requireContext())
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT)
        for (d in 0 until 7) {
            val tv = getNewTextView(iWantTop, iWantBottom)
            tr.addView(tv)
        }
        return tr
    }
    private fun addBottomFrameToTable() {
        val tr = binding.tableCalendarRows.getChildAt(binding.tableCalendarRows.childCount-1) as TableRow
        for (c in 0 until 7) {
            val tv = tr.getChildAt(c) as TextView
            tv.setBackgroundResource(R.drawable.row_thin_frame_no_top)
        }
    }
}