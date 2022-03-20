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

    @SuppressLint("SetTextI18n")
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.row_recurring_transaction, parent, false)

        val tvDescription = rowView.findViewById(R.id.rtDescription) as TextView
        val tvDescription2 = rowView.findViewById(R.id.rtDescription2) as TextView
        val tvName = rowView.findViewById(R.id.rt_row_name) as TextView
        val tvAmount = rowView.findViewById(R.id.rt_row_amount) as TextView
        val tvNextDate = rowView.findViewById(R.id.rt_row_next_date) as TextView
        val tvPeriod = rowView.findViewById(R.id.rt_row_period) as TextView
        val tvRegularity = rowView.findViewById(R.id.rt_row_regularity) as TextView
        val tvCategory = rowView.findViewById(R.id.rt_row_category) as TextView
        val tvSubcategory = rowView.findViewById(R.id.rt_row_subcategory) as TextView
        val tvPaidby = rowView.findViewById(R.id.rt_row_paidby) as TextView
        val tvBoughtfor = rowView.findViewById(R.id.rt_row_boughtfor) as TextView
        val tvSplit1 = rowView.findViewById(R.id.rt_row_split1) as TextView
        val tvSplit2 = rowView.findViewById(R.id.rt_row_split2) as TextView

        val rtData = getItem(pos) as RecurringTransaction
        tvName.text = rtData.name
        val dec = DecimalFormat("#.00")
        val formattedAmount = (rtData.amount/100).toDouble() + (rtData.amount % 100).toDouble()/100
        tvAmount.text = dec.format(formattedAmount)
        tvNextDate.text = rtData.nextdate
        tvPeriod.text = rtData.period
        tvRegularity.text = rtData.regularity.toString()
        tvCategory.text = rtData.category
        tvSubcategory.text = rtData.subcategory
        tvSplit1.text = rtData.split1.toString()
        tvSplit2.text = rtData.split2.toString()
        if (!SpenderViewModel.singleUser()) {
            tvPaidby.text = SpenderViewModel.getSpenderName(rtData.paidby)
            tvBoughtfor.text = SpenderViewModel.getSpenderName(rtData.boughtfor)
        }
        tvDescription.text = rtData.name + " payment of $" + dec.format(formattedAmount) + " occurs "
        if (rtData.regularity == 1)
            tvDescription.text = tvDescription.text.toString() + rtData.period.lowercase() + "ly"
        else
            tvDescription.text = tvDescription.text.toString() + "every " + rtData.regularity.toString() + " " + rtData.period.lowercase() + "s"

        tvDescription2.text = "(Next payment due " + rtData.nextdate + ", " + rtData.category + "-" + rtData.subcategory + ", "
        if (rtData.paidby == rtData.boughtfor)
            tvDescription2.text = tvDescription2.text.toString() + rtData.paidby
        else
            tvDescription2.text = tvDescription2.text.toString() + " paid by " + rtData.paidby + " for " + rtData.boughtfor
        if (rtData.boughtfor == 2) {
            tvDescription2.text = tvDescription2.text.toString() + " " + rtData.split1 + ":" + rtData.split2
        }
        tvDescription2.text = tvDescription2.text.toString() + ")"

        return rowView
    }
}