package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView

class TranslationAdapter (context: Context, data: MutableList<Translation>,
    iSortOrder: TranslationSortOrder = TranslationSortOrder.BEFORE_ASCENDING): BaseAdapter(), Filterable {

    private var myData: MutableList<Translation> = arrayListOf()
    var filteredList: MutableList<Translation> = mutableListOf()

    init {
        myData = data
        filteredList = data
        sortBy(iSortOrder)
    }

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return filteredList.size
    }

    override fun getItem(pos: Int): Any {
        return filteredList[pos]
    }

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    fun sortBy(iSortOrder: TranslationSortOrder) {
        when (iSortOrder) {
            TranslationSortOrder.BEFORE_ASCENDING -> filteredList.sortBy { it.before.lowercase() }
            TranslationSortOrder.BEFORE_DESCENDING -> filteredList.sortByDescending { it.before.lowercase() }
            TranslationSortOrder.AFTER_ASCENDING -> filteredList.sortBy { it.after.lowercase() }
            TranslationSortOrder.AFTER_DESCENDING -> filteredList.sortByDescending { it.after.lowercase() }
            TranslationSortOrder.CATEGORY_ASCENDING -> filteredList.sortBy { CategoryViewModel.getFullCategoryName(it.category) }
            else -> filteredList.sortByDescending { it.category }
        }
    }

    // class for holding the cached view
    class TViewHolder(view: View) {
        var vhBefore: TextView = view.findViewById(R.id.row_before)
        var vhAfter: TextView = view.findViewById(R.id.row_after)
        var vhCategory: TextView = view.findViewById(R.id.row_category)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: TViewHolder
        val cData = getItem(pos) as Translation

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_translation, parent, false)
        viewHolder = TViewHolder(myConvertView)

        viewHolder.vhBefore.text = cData.before
        viewHolder.vhAfter.text = cData.after
        viewHolder.vhCategory.text = CategoryViewModel.getFullCategoryName(cData.category)
        return myConvertView
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                filterTheList(charSearch)
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as MutableList<Translation>
                notifyDataSetChanged()
            }
        }
    }
    fun filterTheList(iConstraint: String) {
        if (iConstraint.isEmpty()) {
            filteredList = myData
        } else {
            val resultList: MutableList<Translation> = mutableListOf()
            val splitSearchTerms: List<String> = iConstraint.split(" ")
            for (row in myData) {
                var found = true
                for (r in splitSearchTerms) {
                    found = found && row.contains(r)
                }
                if (found) {
                    resultList.add(row)
                }
            }
            filteredList = resultList
        }
    }
}