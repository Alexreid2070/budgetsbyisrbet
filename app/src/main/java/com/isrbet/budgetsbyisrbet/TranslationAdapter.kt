package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class TranslationAdapter (context: Context, data: MutableList<Translation>): BaseAdapter() {

    private var myData: MutableList<Translation> = arrayListOf()
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
        val rowView = inflater.inflate(R.layout.row_translation, parent, false)

        val beforeView = rowView.findViewById(R.id.row_before) as TextView
        val afterView = rowView.findViewById(R.id.row_after) as TextView

        val cData = getItem(pos) as Translation
        beforeView.text = cData.before
        afterView.text = cData.after
        return rowView
    }
}