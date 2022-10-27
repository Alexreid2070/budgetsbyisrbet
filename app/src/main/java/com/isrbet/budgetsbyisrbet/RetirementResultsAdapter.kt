package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class RetirementResultsAdapter (context: Context, data: RetirementCalculationRow): BaseAdapter() {
    private var myData: MutableList<Asset> = arrayListOf()
    init {
        data.assetIncomes.forEach {
            myData.add(it)
        }
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
    class ResultViewHolder(view: View) {
        var vhLabel: TextView = view.findViewById(R.id.row_label)
        var vhAmount: TextView = view.findViewById(R.id.row_amount)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ResultViewHolder
        val asset = getItem(pos) as Asset

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_retirement_result, parent, false)
        viewHolder = ResultViewHolder(myConvertView)

        viewHolder.vhLabel.text = String.format("${AssetType.getText(asset.assetType)}: ${asset.name}")
        if (asset.assetType == AssetType.PROPERTY) {
            if ((asset as Property).soldInYear != 0)
                viewHolder.vhLabel.text = String.format(MyApplication.getString(R.string.sold_in),
                    AssetType.getText(asset.assetType), asset.name, asset.soldInYear)
        }
        viewHolder.vhAmount.text = gDecWithCurrency(asset.getEndingBalance())
        return myConvertView
    }
}