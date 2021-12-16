package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.DecimalFormat

data class BudgetInputRow(var category: String, var m1: String, var m2: String, var m3: String, var m4: String,
                          var m1date: String, var m2date: String, var m3date: String, var m4date: String) {

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

        val categoryView = rowView.findViewById(R.id.budget_category) as TextView
        val m1View = rowView.findViewById(R.id.budget_month1) as TextView
        val m2View = rowView.findViewById(R.id.budget_month2) as TextView
        val m3View = rowView.findViewById(R.id.budget_month3) as TextView
        val m4View = rowView.findViewById(R.id.budget_month4) as TextView
        val m1DateView = rowView.findViewById(R.id.budget_set_date1) as TextView
        val m2DateView = rowView.findViewById(R.id.budget_set_date2) as TextView
        val m3DateView = rowView.findViewById(R.id.budget_set_date3) as TextView
        val m4DateView = rowView.findViewById(R.id.budget_set_date4) as TextView

        val bData = getItem(pos) as BudgetInputRow
        categoryView.text = bData.category

        val dec = DecimalFormat("#.00")
        m1View.text = dec.format(bData.m1.toDouble())
        m2View.text = dec.format(bData.m2.toDouble())
        m3View.text = dec.format(bData.m3.toDouble())
        m4View.text = dec.format(bData.m4.toDouble())
        m1DateView.text = bData.m1date
        m2DateView.text = bData.m2date
        m3DateView.text = bData.m3date
        m4DateView.text = bData.m4date
        return rowView
    }
}