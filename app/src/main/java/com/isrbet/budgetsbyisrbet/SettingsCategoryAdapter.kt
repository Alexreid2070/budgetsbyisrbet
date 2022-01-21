package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class SettingsCategoryAdapter (context: Context, data: MutableList<Category>): BaseAdapter() {

    private var myData: MutableList<Category> = arrayListOf<Category>()
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
        val rowView = inflater.inflate(R.layout.row_settings_category, parent, false)

        val categoryView = rowView.findViewById(R.id.settings_row_category) as TextView
        val subcategoryView = rowView.findViewById(R.id.settings_row_subcategory) as TextView
        val discTypeView = rowView.findViewById(R.id.settings_row_category_disctype) as TextView

        val cData = getItem(pos) as Category
        categoryView.text = cData.categoryName
        subcategoryView.text = cData.subcategoryName
        discTypeView.text = cData.discType
        return rowView
    }
}