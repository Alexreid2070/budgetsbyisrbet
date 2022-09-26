package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class AssetAdapter (context: Context, data: RetirementData,
                    private val moveUp: (Asset) -> Unit = {},
                    private val moveDown: (Asset) -> Unit = {} ): BaseAdapter() {

    private var myData: MutableList<Asset> = arrayListOf()
    init {
        data.assets.forEach {
            myData.add(it)
        }
    }

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun refreshData(iData: RetirementData) {
        myData.clear()
        iData.assets.forEach {
            myData.add(it.copy())
        }
    }
    fun refreshData(newList: MutableList<Asset>) {
        myData.clear()
        newList.forEach {
            myData.add(it.copy())
        }
    }

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
    class AssetViewHolder(view: View) {
        var vhDescription: TextView = view.findViewById(R.id.assetDescription)
        var vhDescription2: TextView = view.findViewById(R.id.assetDescription2)
        var vhAssetType: TextView = view.findViewById(R.id.row_asset_type)
        var vhLabel: TextView = view.findViewById(R.id.row_label)
        var vhAmount: TextView = view.findViewById(R.id.row_amount)
        var vhGrowthRate: TextView = view.findViewById(R.id.row_growth_rate)
        var vhGrowthRateOnceSold: TextView = view.findViewById(R.id.row_growth_rate_once_sold)
        var vhAnnualContribution: TextView = view.findViewById(R.id.row_annual_contribution)
        var vhDistributionOrder: TextView = view.findViewById(R.id.row_distribution_order)
        var vhLinkToScheduledPayment: TextView = view.findViewById(R.id.row_link_to_scheduled_payment)
        var vhMoveUp: ImageView = view.findViewById(R.id.move_up)
        var vhMoveDown: ImageView = view.findViewById(R.id.move_down)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: AssetViewHolder
        val asset = getItem(pos) as Asset

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_asset, parent, false)
        viewHolder = AssetViewHolder(myConvertView)

        val desc1 = if (asset.type == AssetType.PROPERTY) {
            val prop = asset as Property
            String.format("%s %s %s @%s%% (id %s dist %s)", AssetType.getText(asset.type), asset.name,
                gDecWithCurrency((asset.value / (prop.ownershipPct / 100)).toInt()), prop.ownershipPct, prop.id, prop.distributionOrder)
        } else
            String.format("%s %s %s (id %s dist %s)", AssetType.getText(asset.type), asset.name, gDecWithCurrency(asset.value), asset.id, asset.distributionOrder)
        val desc2 = if (asset.type == AssetType.PROPERTY)
            String.format("Annual %s%% growth, (%s%% after sale), %s", asset.estimatedGrowthPct, (asset as Property).estimatedGrowthPctAsSavings, (asset as Property).scheduledPaymentName)
        else
            String.format("Annual %s%% growth, + %s", asset.estimatedGrowthPct, gDecWithCurrency(asset.annualContribution))
        viewHolder.vhDescription.text = desc1
        viewHolder.vhDescription2.text = desc2
        viewHolder.vhAssetType.text = asset.type.toString()
        viewHolder.vhLabel.text = asset.name
        viewHolder.vhAmount.text = asset.value.toString()
        viewHolder.vhGrowthRate.text = asset.estimatedGrowthPct.toString()
        if (asset.type == AssetType.PROPERTY) {
            viewHolder.vhGrowthRateOnceSold.text =
                (asset as Property).estimatedGrowthPctAsSavings.toString()
            viewHolder.vhLinkToScheduledPayment.text =
                (asset as Property).scheduledPaymentName
        }
        viewHolder.vhAnnualContribution.text = asset.annualContribution.toString()
        viewHolder.vhDistributionOrder.text = asset.distributionOrder.toString()

        viewHolder.vhMoveUp.setOnClickListener { moveUp(asset) }
        viewHolder.vhMoveDown.setOnClickListener { moveDown(asset) }

        return myConvertView
    }
}