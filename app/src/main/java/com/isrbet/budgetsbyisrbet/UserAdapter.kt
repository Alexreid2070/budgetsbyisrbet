package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

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
        var vhPrimary: TextView = view.findViewById(R.id.userPrimary)
        var vhSecondary: TextView = view.findViewById(R.id.userSecondary)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: UserViewHolder
        val rtData = getItem(pos) as AppUser

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_user, parent, false)
        viewHolder = UserViewHolder(myConvertView)

        viewHolder.vhEmail.text = rtData.email
        viewHolder.vhUID.text = rtData.uid
        if (rtData.primary == "" && rtData.secondary == "") {
            viewHolder.vhPrimary.visibility = View.GONE
            viewHolder.vhSecondary.visibility = View.GONE
        } else {
            viewHolder.vhPrimary.visibility = View.VISIBLE
            viewHolder.vhSecondary.visibility = View.VISIBLE
            if (rtData.primary == "") { // this user is the primary
                viewHolder.vhPrimary.text =
                    MyApplication.getString(R.string.secondary) + ": "
                viewHolder.vhSecondary.text = rtData.secondary
            } else {  // this user is the seconary
                viewHolder.vhPrimary.text =
                    MyApplication.getString(R.string.primary) + ": "
                viewHolder.vhSecondary.text = AppUserViewModel.getPrimaryEmail(rtData.primary)
            }
        }
        return myConvertView
    }
}