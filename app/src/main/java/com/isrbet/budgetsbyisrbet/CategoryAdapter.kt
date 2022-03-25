package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CategoryAdapter (context: Context, data: MutableList<Category>): BaseAdapter() {

    private var myData: MutableList<Category> = arrayListOf()
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

    // class for holding the cached view
    class CategoryViewHolder(view: View) {
        var vhID: TextView = view.findViewById(R.id.row_category_id)
        var vhCategory: TextView = view.findViewById(R.id.row_category)
        var vhDiscType: TextView = view.findViewById(R.id.row_disctype)
    }

    @SuppressLint("SetTextI18n")
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: CategoryViewHolder
        val cData = getItem(pos) as Category

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_category, parent, false)
        viewHolder = CategoryViewHolder(myConvertView)

        viewHolder.vhID.text = cData.id.toString()
        viewHolder.vhCategory.text = "${cData.categoryName}-${cData.subcategoryName}"
        viewHolder.vhDiscType.text = cData.discType
        if (cData.discType == cDiscTypeOff) {
            viewHolder.vhCategory.setTextColor(ContextCompat.getColor(myContext, R.color.red))
            viewHolder.vhDiscType.setTextColor(ContextCompat.getColor(myContext, R.color.red))
        }
        return myConvertView
    }
}