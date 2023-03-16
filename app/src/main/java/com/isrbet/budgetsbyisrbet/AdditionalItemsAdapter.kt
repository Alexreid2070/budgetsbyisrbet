package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class AdditionalItemsAdapter (context: Context, iItemTypes: AdditionalType): BaseAdapter() {
    private val myType = iItemTypes
    private var myAdditionalItems: MutableList<AdditionalItem> = arrayListOf()
    init {
        refreshData()
    }

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun refreshData() {
        myAdditionalItems.clear()
        for (i in 0 until RetirementViewModel.getWorkingAdditionalListCount()) {
            val additionalItem = RetirementViewModel.getWorkingAdditionalItem(i)
            if (additionalItem != null) {
                if (additionalItem.type == myType)
                    myAdditionalItems.add(additionalItem.copy())
            }
        }
    }
    override fun getCount(): Int {
        return myAdditionalItems.size
    }

    override fun getItem(pos: Int): Any {
        return myAdditionalItems[pos]
    }

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    // class for holding the cached view
    class ItemViewHolder(view: View) {
        var vhDescription: TextView = view.findViewById(R.id.itemDescription)
        var vhType: TextView = view.findViewById(R.id.row_type)
        var vhYear: TextView = view.findViewById(R.id.row_year)
        var vhAmount: TextView = view.findViewById(R.id.row_amount)
        var vhAssetName: TextView = view.findViewById(R.id.row_asset_name)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ItemViewHolder
        val item = getItem(pos) as AdditionalItem

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_additional_item, parent, false)
        viewHolder = ItemViewHolder(myConvertView)

        val desc1 = if (item.type == AdditionalType.DEPOSIT) {
            String.format("%s: %s in %s to %s",
                item.name,
                gDecWithCurrency(item.amount),
                item.year,
                item.assetName)
        } else {
            String.format("%s: %s in %s",
                item.name,
                gDecWithCurrency(item.amount),
                item.year)
        }
        viewHolder.vhDescription.text = desc1
        viewHolder.vhYear.text = item.year.toString()
        viewHolder.vhType.text = item.type.toString()
        viewHolder.vhAmount.text = item.amount.toString()
        viewHolder.vhAssetName.text = item.assetName

        return myConvertView
    }
}