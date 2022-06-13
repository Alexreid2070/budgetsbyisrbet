package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.DecimalFormat

class RecurringTransactionAdapter (context: Context, data: MutableList<RecurringTransaction>): BaseAdapter() {

    private var myData: MutableList<RecurringTransaction> = arrayListOf()
    init {
        myData = data
    }

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return myData.size
    }

    override fun getItem(pos: Int): Any {
        return myData[pos]
    }

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    // class for holding the cached view
    class RTViewHolder(view: View) {
        var vhDescription: TextView = view.findViewById(R.id.rtDescription)
        var vhDescription2: TextView = view.findViewById(R.id.rtDescription2)
        var vhName: TextView = view.findViewById(R.id.rt_row_name)
        var vhAmount: TextView = view.findViewById(R.id.rt_row_amount)
        var vhNextDate: TextView = view.findViewById(R.id.rt_row_next_date)
        var vhPeriod: TextView = view.findViewById(R.id.rt_row_period)
        var vhRegularity: TextView = view.findViewById(R.id.rt_row_regularity)
        var vhCategory: TextView = view.findViewById(R.id.rt_row_category)
        var vhSubcategory: TextView = view.findViewById(R.id.rt_row_subcategory)
        var vhPaidby: TextView = view.findViewById(R.id.rt_row_paidby)
        var vhBoughtfor: TextView = view.findViewById(R.id.rt_row_boughtfor)
        var vhSplit1: TextView = view.findViewById(R.id.rt_row_split1)
        var vhSplit2: TextView = view.findViewById(R.id.rt_row_split2)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: RTViewHolder
        val rtData = getItem(pos) as RecurringTransaction

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_recurring_transaction, parent, false)
        viewHolder = RTViewHolder(myConvertView)

        viewHolder.vhName.text = rtData.name
        val formattedAmount = (rtData.amount/100).toDouble() + (rtData.amount % 100).toDouble()/100
        viewHolder.vhAmount.text = gDecWithCurrency(formattedAmount)
        viewHolder.vhNextDate.text = rtData.nextdate
        viewHolder.vhPeriod.text = rtData.period
        viewHolder.vhRegularity.text = rtData.regularity.toString()
        viewHolder.vhCategory.text = CategoryViewModel.getCategory(rtData.category)?.categoryName
        viewHolder.vhSubcategory.text = CategoryViewModel.getCategory(rtData.category)?.subcategoryName
        viewHolder.vhSplit1.text = rtData.split1.toString()
        viewHolder.vhSplit2.text = rtData.getSplit2().toString()
        if (SpenderViewModel.multipleUsers()) {
            viewHolder.vhPaidby.text = SpenderViewModel.getSpenderName(rtData.paidby)
            viewHolder.vhBoughtfor.text = SpenderViewModel.getSpenderName(rtData.boughtfor)
        }
        viewHolder.vhDescription.text = rtData.name + " payment of " + gDecWithCurrency(formattedAmount) + " occurs "
        if (rtData.regularity == 1)
            viewHolder.vhDescription.text = viewHolder.vhDescription.text.toString() + rtData.period.lowercase() + "ly"
        else
            viewHolder.vhDescription.text = viewHolder.vhDescription.text.toString() + "every " + rtData.regularity.toString() + " " + rtData.period.lowercase() + "s"

        viewHolder.vhDescription2.text = "Next payment due " + rtData.nextdate + ", " +
                CategoryViewModel.getCategory(rtData.category)?.fullCategoryName() + ", "
        if (rtData.paidby == rtData.boughtfor)
            viewHolder.vhDescription2.text = viewHolder.vhDescription2.text.toString() +
                    SpenderViewModel.getSpenderName(rtData.paidby)
        else
            viewHolder.vhDescription2.text = viewHolder.vhDescription2.text.toString() + " paid by " +
                    SpenderViewModel.getSpenderName(rtData.paidby) +
                    " for " + SpenderViewModel.getSpenderName(rtData.boughtfor)
        if (rtData.boughtfor == 2) {
            viewHolder.vhDescription2.text = viewHolder.vhDescription2.text.toString() + " " + rtData.split1 + ":" + rtData.getSplit2()
        }

        return myConvertView
    }
}