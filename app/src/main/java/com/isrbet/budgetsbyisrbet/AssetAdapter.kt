package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

class AssetAdapter (context: Context,
                    private val defaultInvestmentGrowthRate: Double,
                    private val defaultPropertyGrowthRate: Double,
                    iRetirementType: RetirementScenarioType,
                    private val moveUp: (Asset) -> Unit = {},
                    private val moveDown: (Asset) -> Unit = {} ): BaseAdapter() {
    private val myContext = context
    private var myData: MutableList<Asset> = arrayListOf()
    private var myScenarioType = iRetirementType
    init {
        refreshData()
    }

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun refreshData() {
        val myScenario = if (myScenarioType == RetirementScenarioType.SCENARIO)
            gRetirementWorking
        else
            gRetirementDefaults
        myData.clear()
        if (myScenario != null) {
            for (i in 0 until myScenario.assets.size) {
                myScenario.assets[i].let { myData.add(it) }
            }
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

        val desc1 = if (asset.assetType == AssetType.PROPERTY) {
            val prop = asset as Property
            String.format("%s %s %s @%s%%", AssetType.getText(asset.assetType), asset.name,
                gDecWithCurrency((asset.getValue() / (prop.ownershipPct / 100)).toInt()), prop.ownershipPct)
        } else
            String.format("%s %s %s", AssetType.getText(asset.assetType), asset.name, gDecWithCurrency(asset.getValue()))
        val desc2 = if (asset.assetType == AssetType.PROPERTY) {
            val propGrowth = if (asset.useDefaultGrowthPct) defaultPropertyGrowthRate else asset.estimatedGrowthPct
            val invGrowth = if ((asset as Property).useDefaultGrowthPctAsSavings)
                defaultInvestmentGrowthRate
            else
                asset.estimatedGrowthPctAsSavings
            String.format("Annual %s%% growth, (%s%% after sale), %s", propGrowth, invGrowth, asset.scheduledPaymentName)
        } else {
            val growth = if (asset.useDefaultGrowthPct) defaultInvestmentGrowthRate else asset.estimatedGrowthPct
            val taxed = if (asset.growthIsTaxable())
                "(taxed)"
            else
                ""
            String.format("Annual %s%% growth %s, + %s", growth, taxed, gDecWithCurrency(asset.annualContribution))
        }
        viewHolder.vhDescription.text = desc1
        viewHolder.vhDescription2.text = desc2
        if (!asset.willSellToFinanceRetirement) {
            viewHolder.vhDescription.setTextColor(
                ContextCompat.getColor(myContext, R.color.red))
            viewHolder.vhDescription2.setTextColor(ContextCompat.getColor(myContext, R.color.red))
        }
        viewHolder.vhAssetType.text = asset.assetType.toString()
        viewHolder.vhLabel.text = asset.name
        viewHolder.vhAmount.text = asset.getValue().toString()
        viewHolder.vhGrowthRate.text = asset.estimatedGrowthPct.toString()
        if (asset.assetType == AssetType.PROPERTY) {
            viewHolder.vhGrowthRateOnceSold.text =
                (asset as Property).estimatedGrowthPctAsSavings.toString()
            viewHolder.vhLinkToScheduledPayment.text =
                asset.scheduledPaymentName
        }
        viewHolder.vhAnnualContribution.text = asset.annualContribution.toString()
        viewHolder.vhDistributionOrder.text = asset.distributionOrder.toString()

        viewHolder.vhMoveUp.setOnClickListener { moveUp(asset) }
        viewHolder.vhMoveDown.setOnClickListener { moveDown(asset) }

        return myConvertView
    }
}