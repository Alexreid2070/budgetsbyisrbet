package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.view.isVisible

data class BudgetInputRow(var key: String, var categoryID: Int,
                          var dateApplicable: String, var amount: String,
                          var who: Int, var occurence: String,
                          var dateStarted: String, var period: String,
                          var regularity: Int, var label: String) {
    var category = CategoryViewModel.getCategory(categoryID)?.categoryName
    var subcategory = CategoryViewModel.getCategory(categoryID)?.subcategoryName
    var categoryPriority = category?.let { DefaultsViewModel.getCategoryDetail(it).priority }
}

class BudgetAdapter (context: Context, data: MutableList<BudgetInputRow>): BaseAdapter() {
    private var groupList: MutableList<Int> = mutableListOf()
    private var myData: MutableList<BudgetInputRow> = mutableListOf()
    init {
        myData = data
        setGroupList()
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
        var vhCategory: TextView = view.findViewById(R.id.row_category)
        var vhLabel: TextView = view.findViewById(R.id.row_label)
        var vhAmount: TextView = view.findViewById(R.id.row_budget_amount)
        var vhWho: TextView = view.findViewById(R.id.row_budget_who)
        var vhOccurence: TextView = view.findViewById(R.id.row_budget_occurence)
        var vhPeriodIndicator: TextView = view.findViewById(R.id.row_budget_period_indicator)
        var vhDetail: LinearLayout = view.findViewById(R.id.row_detail)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: BudgetViewHolder
        val bData = getItem(pos) as BudgetInputRow

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_budget, parent, false)
        viewHolder = BudgetViewHolder(myConvertView)

        if (bData.label == cBudgetDateView) {
            if (groupList[pos] == 0) { // ie first row of this category
                viewHolder.vhCategory.isVisible = true
                viewHolder.vhCategory.text = bData.category
            } else {
                viewHolder.vhCategory.isVisible = false
            }
            viewHolder.vhLabel.text =
                CategoryViewModel.getCategory(bData.categoryID)?.subcategoryName ?: ""
        } else {
            viewHolder.vhLabel.text = bData.dateStarted
            viewHolder.vhCategory.isVisible = false
        }
        viewHolder.vhWho.text = SpenderViewModel.getSpenderName(bData.who)
        viewHolder.vhOccurence.text = bData.occurence
        if (bData.occurence == "0")
            viewHolder.vhOccurence.text =  MyApplication.getString(R.string.recurring)
        else
            viewHolder.vhOccurence.text = MyApplication.getString(R.string.once)
        viewHolder.vhAmount.text = gDecWithCurrency(bData.amount.toDouble())
        viewHolder.vhPeriodIndicator.text =
            if (bData.regularity == 1)
                bData.period.substring(0,1)
            else
                bData.regularity.toString() + bData.period.substring(0,1)
/*        if (bData.isAnnual == "") {
            viewHolder.vhPeriodIndicator.text = ""
        } else {
            viewHolder.vhAnnualIndicator.text = MyApplication.getString(R.string.annual_short)
            if (bData.label == cBudgetCategoryView)
                viewHolder.vhLabel.text = bData.dateApplicable.substring(0,4)
        } */
/*        if (bData.dateApplicable == bData.dateStarted ) {
            viewHolder.vhLabel.setTypeface(viewHolder.vhLabel.typeface, Typeface.BOLD)
        } */
        if (SpenderViewModel.singleUser()) {
            viewHolder.vhWho.visibility = View.GONE
        }

        if (bData.label == cBudgetDateView) {
            val param = viewHolder.vhLabel.layoutParams as LinearLayout.LayoutParams
            param.weight = 2.5f
            viewHolder.vhLabel.layoutParams = param
            viewHolder.vhOccurence.visibility = View.GONE
            val cat = DefaultsViewModel.getCategoryDetail(bData.category.toString())
            if (cat.color != 0) {
                viewHolder.vhCategory.setBackgroundColor(cat.color)
                if (Build.VERSION.SDK_INT >= 29) {
                    viewHolder.vhDetail.setBackgroundResource(R.drawable.row_left_border)
                    viewHolder.vhDetail.background.colorFilter =
                        BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
                } else {
                    viewHolder.vhDetail.setBackgroundColor(cat.color)
                    viewHolder.vhDetail.background.alpha = 44
                }
                viewHolder.vhDetail.setPadding(30, 5, 5, 5)

                val trParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                trParams.setMargins(15, 0, 10, 0)
                viewHolder.vhCategory.layoutParams = trParams
                viewHolder.vhDetail.layoutParams = trParams
            }
        }

        return myConvertView
    }
    private fun setGroupList() {
        val tGroupList: MutableList<Int> = mutableListOf()
        tGroupList.clear()
        var c = 0 // row counter
        var j = 0 // number of transaction within specific date

        for (i in 0 until myData.size) {
            if (tGroupList.size == 0) {
                tGroupList.add(c, j)
                c++
                j++
            } else {
                if (myData[i].category == myData[i - 1].category) {
                    tGroupList.add(c, j)
                    c++
                    j++
                } else {
                    j = 0
                    tGroupList.add(c, j)
                    c++
                    j++
                }
            }
        }
        groupList = tGroupList
    }
}