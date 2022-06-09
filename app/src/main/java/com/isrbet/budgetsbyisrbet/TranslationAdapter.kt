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

    // class for holding the cached view
    class TViewHolder(view: View) {
        var vhBefore: TextView = view.findViewById(R.id.row_before)
        var vhAfter: TextView = view.findViewById(R.id.row_after)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: TViewHolder
        val cData = getItem(pos) as Translation

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_translation, parent, false)
        viewHolder = TViewHolder(myConvertView)

        viewHolder.vhBefore.text = cData.before
        viewHolder.vhAfter.text = cData.after
        return myConvertView
    }
}