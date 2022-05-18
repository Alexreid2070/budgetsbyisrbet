package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.firebase.ui.auth.data.model.User
import java.text.DecimalFormat

class UserAdapter (context: Context, data: MutableList<AppUser>): BaseAdapter() {

    private var myData: MutableList<AppUser> = arrayListOf()
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
    class UserViewHolder(view: View) {
        var vhEmail: TextView = view.findViewById(R.id.userEmail)
        var vhUID: TextView = view.findViewById(R.id.userUID)
    }

    @SuppressLint("SetTextI18n")
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: UserViewHolder
        val rtData = getItem(pos) as AppUser

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_user, parent, false)
        viewHolder = UserViewHolder(myConvertView)

        viewHolder.vhEmail.text = rtData.email
        viewHolder.vhUID.text = rtData.uid

        return myConvertView
    }
}