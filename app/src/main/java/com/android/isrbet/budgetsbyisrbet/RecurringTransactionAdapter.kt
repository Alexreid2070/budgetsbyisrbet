package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.DecimalFormat

class RecurringTransactionAdapter (context: Context, data: MutableList<RecurringTransaction>): BaseAdapter() {

    private var myData: MutableList<RecurringTransaction> = arrayListOf<RecurringTransaction>()
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

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.row_recurring_transaction, parent, false)

        val tvName = rowView.findViewById(R.id.rt_row_name) as TextView
        val tvAmount = rowView.findViewById(R.id.rt_row_amount) as TextView
        val tvNextDate = rowView.findViewById(R.id.rt_row_next_date) as TextView
        val tvPeriod = rowView.findViewById(R.id.rt_row_period) as TextView
        val tvRegularity = rowView.findViewById(R.id.rt_row_regularity) as TextView
        val tvCategory = rowView.findViewById(R.id.rt_row_category) as TextView
        val tvSubcategory = rowView.findViewById(R.id.rt_row_subcategory) as TextView
        val tvWho = rowView.findViewById(R.id.rt_row_who) as TextView

        val rtData = getItem(pos) as RecurringTransaction
        tvName.text = rtData.name
        val dec = DecimalFormat("#.00")
        val formattedAmount = (rtData.amount/100).toDouble() + (rtData.amount % 100).toDouble()/100
        tvAmount.text = dec.format(formattedAmount)
        tvNextDate.text = rtData.nextdate
        tvPeriod.text = rtData.period
        tvRegularity.text = rtData.regularity.toString()
        tvCategory.text = rtData.category.toString()
        tvSubcategory.text = rtData.subcategory.toString()
        tvWho.text = rtData.who.toString()
        return rowView
    }
}