package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Typeface
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DecimalFormat


data class BudgetInputRow(var categoryID: Int, var dateApplicable: String, var amount: String,
                          var who: Int, var occurence: String, var isAnnual: String,
                          var dateStarted: String, var label: String) {
    var category = CategoryViewModel.getCategory(categoryID)?.categoryName
    var subcategory = CategoryViewModel.getCategory(categoryID)?.subcategoryName
    var categoryPriority = category?.let { DefaultsViewModel.getCategoryDetail(it).priority }
}

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
        var vhLabel: TextView = view.findViewById(R.id.row_label)
        var vhAmount: TextView = view.findViewById(R.id.row_budget_amount)
        var vhWho: TextView = view.findViewById(R.id.row_budget_who)
        var vhOccurence: TextView = view.findViewById(R.id.row_budget_occurence)
        var vhAnnual: TextView = view.findViewById(R.id.row_budget_annual)
    }
    @SuppressLint("SetTextI18n")
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: BudgetViewHolder
        val bData = getItem(pos) as BudgetInputRow

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_budget, parent, false)
        viewHolder = BudgetViewHolder(myConvertView)

        val dec = DecimalFormat("#.00")

        viewHolder.vhLabel.text =
            if (bData.label == cBudgetDateView)
                CategoryViewModel.getFullCategoryName(bData.categoryID)
            else
                bData.dateStarted

        viewHolder.vhWho.text = SpenderViewModel.getSpenderName(bData.who)
        viewHolder.vhOccurence.text = bData.occurence
        if (bData.occurence == "0")
            viewHolder.vhOccurence.text = "Recurring"
        else
            viewHolder.vhOccurence.text = "Once"
//            viewHolder.vhOccurence.visibility = View.INVISIBLE
        if (bData.isAnnual == "") {
            viewHolder.vhAmount.text = dec.format(bData.amount.toDouble())
            viewHolder.vhAnnual.text = ""
        } else {
            viewHolder.vhAmount.text = dec.format(bData.amount.toDouble()/12)
            viewHolder.vhAnnual.text = dec.format(bData.amount.toDouble())
            if (bData.label == cBudgetCategoryView)
                viewHolder.vhLabel.text = bData.dateApplicable.substring(0,4)
        }
        if (bData.dateApplicable == bData.dateStarted ) {
            viewHolder.vhLabel.setTypeface(viewHolder.vhLabel.typeface, Typeface.BOLD)
/*            viewHolder.vhAmount.setTypeface(viewHolder.vhAmount.typeface, Typeface.BOLD)
            viewHolder.vhWho.setTypeface(viewHolder.vhWho.typeface, Typeface.BOLD)
            viewHolder.vhOccurence.setTypeface(viewHolder.vhOccurence.typeface, Typeface.BOLD)
            viewHolder.vhAnnual.setTypeface(viewHolder.vhAnnual.typeface, Typeface.BOLD)
            viewHolder.vhDateStarted.setTypeface(viewHolder.vhDateStarted.typeface, Typeface.BOLD) */
        }
        if (SpenderViewModel.singleUser()) {
            viewHolder.vhWho.visibility = View.GONE
        }

        if (bData.label == cBudgetDateView) {
            val param = viewHolder.vhLabel.layoutParams as LinearLayout.LayoutParams
            param.weight = 3f
            viewHolder.vhLabel.layoutParams = param
            viewHolder.vhOccurence.visibility = View.GONE
            val cat = DefaultsViewModel.getCategoryDetail(bData.category.toString())
            if (cat.color != 0) {
                viewHolder.vhLabel.setBackgroundColor(cat.color)
                viewHolder.vhAmount.setBackgroundColor(cat.color)
                viewHolder.vhAnnual.setBackgroundColor(cat.color)
                viewHolder.vhWho.setBackgroundColor(cat.color)
            }
        }

        return myConvertView
    }
}