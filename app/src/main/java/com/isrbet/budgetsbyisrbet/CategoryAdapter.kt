package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CategoryAdapter (context: Context, data: MutableList<Category>): BaseAdapter() {

    private var myData: MutableList<Category> = arrayListOf<Category>()
    private val myContext = context
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
        val rowView = inflater.inflate(R.layout.row_category, parent, false)

        val categoryView = rowView.findViewById(R.id.row_category) as TextView
        val subcategoryView = rowView.findViewById(R.id.row_subcategory) as TextView
        val discTypeView = rowView.findViewById(R.id.row_disctype) as TextView

        val cData = getItem(pos) as Category
        categoryView.text = cData.categoryName
        subcategoryView.text = cData.subcategoryName
        discTypeView.text = cData.discType
        if (cData.discType == cDiscTypeOff) {
            categoryView.setTextColor(ContextCompat.getColor(myContext, R.color.red))
            subcategoryView.setTextColor(ContextCompat.getColor(myContext, R.color.red))
            discTypeView.setTextColor(ContextCompat.getColor(myContext, R.color.red))
        }
        return rowView
    }
}