package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ChatAdapter (context: Context, data: MutableList<Chat>): BaseAdapter() {

    private var myData: MutableList<Chat> = arrayListOf()
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
    class ViewHolder(view: View) {
        var vhDate: TextView = view.findViewById(R.id.chat_date)
        var vhTime: TextView = view.findViewById(R.id.chat_time)
        var vhUserName: TextView = view.findViewById(R.id.chat_user_name)
        var vhText: TextView = view.findViewById(R.id.chat_text)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder
        val rtData = getItem(pos) as Chat

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_chat, parent, false)
        viewHolder = ViewHolder(myConvertView)

        viewHolder.vhDate.text = rtData.date
        viewHolder.vhTime.text = rtData.time
        viewHolder.vhUserName.text = rtData.username
        viewHolder.vhText.text = rtData.text

        return myConvertView
    }
}