package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.DecimalFormat

data class BudgetInputRow(var dateApplicable: String, var amount: String, var who: String, var occurence: String, var isAnnual: String, var dateStarted: String) {

}

class BudgetAdapter (context: Context, data: MutableList<BudgetInputRow>): BaseAdapter() {

    private var myData: MutableList<BudgetInputRow> = mutableListOf<BudgetInputRow>()
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
        val rowView = inflater.inflate(R.layout.row_budget, parent, false)

        val dateApplicableView = rowView.findViewById(R.id.row_budget_month_applicable) as TextView
        val amountView = rowView.findViewById(R.id.row_budget_amount) as TextView
        val whoView = rowView.findViewById(R.id.row_budget_who) as TextView
        val occurenceView = rowView.findViewById(R.id.row_budget_occurence) as TextView
        val annualView = rowView.findViewById(R.id.row_budget_annual) as TextView
        val dateStartedView = rowView.findViewById(R.id.row_budget_month_started) as TextView

        val bData = getItem(pos) as BudgetInputRow

        val dec = DecimalFormat("#.00")
        dateApplicableView.text = bData.dateApplicable
        amountView.text = dec.format(bData.amount.toDouble())
        whoView.text = bData.who
        occurenceView.text = bData.occurence
        if (bData.occurence != "1")
            occurenceView.visibility = View.INVISIBLE
        annualView.text = bData.isAnnual
        var bmDateStarted = BudgetMonth(bData.dateStarted)
        var bmDateApplicable = BudgetMonth(bData.dateApplicable)

        if (bData.dateStarted == "9999-12")
            dateStartedView.text = ""
        else if (bmDateStarted.month == 0)
            dateStartedView.text = bmDateStarted.year.toString()
        else
            dateStartedView.text = bData.dateStarted

        if (bData.dateApplicable == bData.dateStarted ||
            (bmDateApplicable.month == 1 && bmDateStarted.month == 0)) {
            dateApplicableView.setTypeface(dateApplicableView.typeface, Typeface.BOLD)
            amountView.setTypeface(amountView.typeface, Typeface.BOLD)
            whoView.setTypeface(whoView.typeface, Typeface.BOLD)
            occurenceView.setTypeface(occurenceView.typeface, Typeface.BOLD)
            annualView.setTypeface(annualView.typeface, Typeface.BOLD)
            dateStartedView.setTypeface(dateStartedView.typeface, Typeface.BOLD)
        }
        if (SpenderViewModel.getCount() == 1) {
            whoView.visibility = View.GONE
        }

        return rowView
    }
}