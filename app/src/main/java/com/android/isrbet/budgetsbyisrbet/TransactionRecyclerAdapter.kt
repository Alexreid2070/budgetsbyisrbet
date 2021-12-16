package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class TransactionRecyclerAdapter(private val context: Context, private val list: MutableList<Expenditure>,
                                 private val listener: (Expenditure) -> Unit = {}) : RecyclerView.Adapter<TransactionRecyclerAdapter.ViewHolder>(),
                        Filterable {

    var filteredList: MutableList<Expenditure> = mutableListOf<Expenditure>()
    var groupList: MutableList<Int> = mutableListOf<Int>()

    init {
        filterTheList(MyApplication.transactionSearchText)
    }
    override fun getItemCount(): Int {
        return filteredList.size
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

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as MutableList<Expenditure>
                setGroupList()
                notifyDataSetChanged()
            }
        }
    }

    fun filterTheList(iConstraint: String) {
        if (iConstraint.isEmpty()) {
            filteredList = list
        } else {
            val resultList: MutableList<Expenditure> = mutableListOf<Expenditure>()
            val splitSearchTerms: List<String> = iConstraint.split(" ")
            var i: Int = 0
            for (row in list) {
                var found: Boolean = true
                for (r in splitSearchTerms) {
                    if (! row.contains(r))
                        found = false
                }

                if (found) {
                    resultList.add(row)
                }
            }
            filteredList = resultList
        }
        setGroupList()
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val vtf_date: TextView = view.findViewById(R.id.vtf_date)
        val vtf_amount: TextView = view.findViewById(R.id.vtf_amount)
        val vtf_category: TextView = view.findViewById(R.id.vtf_category)
        val vtf_subcategory: TextView = view.findViewById(R.id.vtf_subcategory)
        val vtf_who: TextView = view.findViewById(R.id.vtf_who)
        val vtf_note: TextView = view.findViewById(R.id.vtf_note)
        val vtf_type: TextView = view.findViewById(R.id.vtf_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_transaction_view_all,parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = filteredList[position]

        holder.vtf_date.text = data.date
        if (groupList.get(position) == 0) { // ie first transaction on this date
            holder.vtf_date.isVisible = true
            holder.vtf_date.paint.isUnderlineText = true
        } else {
            holder.vtf_date.isVisible = false
        }
        val dec = DecimalFormat("#.00")
        val formattedAmount = (data.amount/100).toDouble() + (data.amount % 100).toDouble()/100
        holder.vtf_amount.text = dec.format(formattedAmount)
        holder.vtf_category.text = data.category
        holder.vtf_subcategory.text = data.subcategory
        holder.vtf_who.text = data.who
        holder.vtf_note.text = data.note
        holder.vtf_type.text = data.type
        holder.itemView.setOnClickListener {listener(data)}
    }

    fun setGroupList() {
        groupList.clear()
        var c: Int = 0 // row counter
        var j: Int = 0 // number of transaction within specific date

        for (i in 0..filteredList.size-1) {
            if (groupList.size == 0) {
                groupList.add(c,j)
                c++
                j++
            } else {
                if (filteredList[i].date == filteredList[i-1].date) {
                    groupList.add(c,j)
                    c++
                    j++
                } else {
                    j=0
                    groupList.add(c,j)
                    c++
                    j++
                }
            }
        }
    }
}