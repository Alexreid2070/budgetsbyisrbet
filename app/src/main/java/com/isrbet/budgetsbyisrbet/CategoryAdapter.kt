package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

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
        var vhDetail: LinearLayout = view.findViewById(R.id.row_detail)
    }

    @SuppressLint("SetTextI18n")
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

        viewHolder.vhID.text = cData.id.toString()
        viewHolder.vhSubcategory.text = cData.subcategoryName
        viewHolder.vhDiscType.text = cData.discType
        if (cData.discType == cDiscTypeOff) {
            viewHolder.vhSubcategory.setTextColor(ContextCompat.getColor(myContext, R.color.red))
            viewHolder.vhDiscType.setTextColor(ContextCompat.getColor(myContext, R.color.red))
        } else {
            viewHolder.vhSubcategory.setTextColor(ContextCompat.getColor(myContext, R.color.black))
            viewHolder.vhDiscType.setTextColor(ContextCompat.getColor(myContext, R.color.black))
        }
        val cat = DefaultsViewModel.getCategoryDetail(cData.categoryName)
        if (cat.color != 0) {
            viewHolder.vhCategory.setBackgroundColor(cat.color)
            viewHolder.vhDetail.setBackgroundResource(R.drawable.row_left_border_no_color)
            if (Build.VERSION.SDK_INT >= 29) {
                viewHolder.vhDetail.setBackgroundResource(R.drawable.row_left_border)
                viewHolder.vhDetail.background.colorFilter =
                    BlendModeColorFilter(cat.color, BlendMode.SRC_ATOP)
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