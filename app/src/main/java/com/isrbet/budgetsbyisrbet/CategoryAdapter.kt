package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors

class CategoryAdapter (context: Context, data: MutableList<Category>): BaseAdapter() {
    private var groupList: MutableList<Int> = mutableListOf()
    private var myData: MutableList<Category> = arrayListOf()
    private val myContext = context
    init {
        myData = data
        setGroupList()
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
        var vhSubcategory: TextView = view.findViewById(R.id.row_subcategory)
        var vhDiscType: TextView = view.findViewById(R.id.row_disctype)
        var vhState: TextView = view.findViewById(R.id.row_state)
        var vhPrivacy: ImageView = view.findViewById(R.id.row_private)
        var vhDetail: LinearLayout = view.findViewById(R.id.row_detail)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: CategoryViewHolder
        val cData = getItem(pos) as Category

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_category, parent, false)
        viewHolder = CategoryViewHolder(myConvertView)

        if (groupList[pos] == 0) { // ie first row of this category
            viewHolder.vhCategory.isVisible = true
            viewHolder.vhCategory.text = cData.categoryName
        } else {
            viewHolder.vhCategory.isVisible = false
        }
        if (SpenderViewModel.twoDistinctUsers())
            viewHolder.vhPrivacy.visibility = View.VISIBLE
        else
            viewHolder.vhPrivacy.visibility = View.GONE

        viewHolder.vhID.text = cData.id.toString()
        viewHolder.vhSubcategory.text = cData.subcategoryName
        viewHolder.vhDiscType.text =
            if (cData.discType == cDiscTypeDiscretionary)
                MyApplication.getString(R.string.discretionary)
            else
                MyApplication.getString(R.string.non_discretionary)
        viewHolder.vhState.text = if (cData.inUse) MyApplication.getString(R.string.on)
            else MyApplication.getString(R.string.off)
        if (SpenderViewModel.twoDistinctUsers()) {
            viewHolder.vhPrivacy.visibility = if (cData.private != 2) View.VISIBLE else View.INVISIBLE
        } else
            viewHolder.vhPrivacy.visibility = View.INVISIBLE
        if (cData.inUse) {
            val col = MaterialColors.getColor(
                myContext,
                R.attr.textOnBackground,
                Color.BLACK
            )
            viewHolder.vhSubcategory.setTextColor(col)
            viewHolder.vhDiscType.setTextColor(col)
        } else {
            viewHolder.vhSubcategory.setTextColor(ContextCompat.getColor(myContext, R.color.red))
            viewHolder.vhDiscType.setTextColor(ContextCompat.getColor(myContext, R.color.red))
        }
        val cat = DefaultsViewModel.getCategoryDetail(cData.categoryName)
        if (cat.color != 0) {
            viewHolder.vhCategory.setBackgroundColor(cat.color)
            if (Build.VERSION.SDK_INT >= 29) {
                viewHolder.vhDetail.setBackgroundResource(R.drawable.row_left_border)
                viewHolder.vhDetail.background.colorFilter =
                    BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
            } else {
                viewHolder.vhDetail.setBackgroundColor(cat.color)
                viewHolder.vhDetail.background.alpha = 44
            }
            viewHolder.vhDetail.setPadding(30, 5, 5, 5)

            val trParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            trParams.setMargins(15, 0, 10, 0)
            viewHolder.vhCategory.layoutParams = trParams
            viewHolder.vhDetail.layoutParams = trParams
        }

        return myConvertView
    }
    private fun setGroupList() {
        val tgroupList: MutableList<Int> = mutableListOf()
        tgroupList.clear()
        var c = 0 // row counter
        var j = 0 // number of transaction within specific date

        for (i in 0 until myData.size) {
            if (tgroupList.size == 0) {
                tgroupList.add(c, j)
                c++
                j++
            } else {
                if (myData[i].categoryName == myData[i - 1].categoryName) {
                    tgroupList.add(c, j)
                    c++
                    j++
                } else {
                    j = 0
                    tgroupList.add(c, j)
                    c++
                    j++
                }
            }
        }
        groupList = tgroupList
    }
}