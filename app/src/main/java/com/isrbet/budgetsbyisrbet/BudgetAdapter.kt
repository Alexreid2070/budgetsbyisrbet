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

data class BudgetInputRow(var dateApplicable: String, var amount: String, var who: Int, var occurence: String, var isAnnual: String, var dateStarted: String)

class BudgetAdapter (context: Context, data: MutableList<BudgetInputRow>): BaseAdapter() {

    private var myData: MutableList<BudgetInputRow> = mutableListOf()
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
    class BudgetViewHolder(view: View) {
        var vhDateApplicable: TextView = view.findViewById(R.id.row_budget_month_applicable)
        var vhAmount: TextView = view.findViewById(R.id.row_budget_amount)
        var vhWho: TextView = view.findViewById(R.id.row_budget_who)
        var vhOccurence: TextView = view.findViewById(R.id.row_budget_occurence)
        var vhAnnual: TextView = view.findViewById(R.id.row_budget_annual)
        var vhDateStarted: TextView = view.findViewById(R.id.row_budget_month_started)
    }
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: BudgetViewHolder
        val bData = getItem(pos) as BudgetInputRow

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_budget, parent, false)
        viewHolder = BudgetViewHolder(myConvertView)

        val dec = DecimalFormat("#.00")
        viewHolder.vhDateApplicable.text = bData.dateApplicable
        Log.d("Alex", "isannual is ${bData.isAnnual}")
        viewHolder.vhAmount.text = dec.format(bData.amount.toDouble())
        viewHolder.vhWho.text = SpenderViewModel.getSpenderName(bData.who)
        viewHolder.vhOccurence.text = bData.occurence
        if (bData.occurence == "0")
            viewHolder.vhOccurence.text = "Recurring"
        else
            viewHolder.vhOccurence.text = "Once"
//            viewHolder.vhOccurence.visibility = View.INVISIBLE
        viewHolder.vhAnnual.text = bData.isAnnual
        val bmDateStarted = BudgetMonth(bData.dateStarted)
        val bmDateApplicable = BudgetMonth(bData.dateApplicable)

        when {
            bData.dateStarted == "9999-12" -> viewHolder.vhDateStarted.text = ""
            bmDateStarted.isAnnualBudget() -> viewHolder.vhDateStarted.text = bmDateStarted.year.toString()
            else -> viewHolder.vhDateStarted.text = bData.dateStarted
        }

        if (bData.dateApplicable == bData.dateStarted ||
            (bmDateApplicable.month == 1 && bmDateStarted.isAnnualBudget())) {
            viewHolder.vhDateApplicable.setTypeface(viewHolder.vhDateApplicable.typeface, Typeface.BOLD)
            viewHolder.vhAmount.setTypeface(viewHolder.vhAmount.typeface, Typeface.BOLD)
            viewHolder.vhWho.setTypeface(viewHolder.vhWho.typeface, Typeface.BOLD)
            viewHolder.vhOccurence.setTypeface(viewHolder.vhOccurence.typeface, Typeface.BOLD)
            viewHolder.vhAnnual.setTypeface(viewHolder.vhAnnual.typeface, Typeface.BOLD)
            viewHolder.vhDateStarted.setTypeface(viewHolder.vhDateStarted.typeface, Typeface.BOLD)
        }
        if (SpenderViewModel.singleUser()) {
            viewHolder.vhWho.visibility = View.GONE
        }

        return myConvertView
    }
}