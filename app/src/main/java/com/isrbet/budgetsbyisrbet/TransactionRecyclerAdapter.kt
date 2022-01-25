package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
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
    private var groupList: MutableList<Int> = mutableListOf<Int>()

    init {
        Log.d("Alex", "transactionSearchTExt init is " + MyApplication.transactionSearchText)
        Log.d("Alex", "calling filterTheList from init")
        filterTheList(MyApplication.transactionSearchText)
    }
    override fun getItemCount(): Int {
        return filteredList.size
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                Log.d("Alex", "calling filterTheList from performFiltering")
                filterTheList(charSearch)
                val filterResults = FilterResults()
                filterResults.values = filteredList
                Log.d("Alex", "performFiltering '" + constraint + "' size is " + filteredList.size)
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as MutableList<Expenditure>
                Log.d("Alex", "filteredList changed 1, new size " + filteredList.size)
                Log.d("Alex", "transactionSearchTExt1 is " + MyApplication.transactionSearchText)
                setGroupList()
                notifyDataSetChanged()
            }
        }
    }

    fun filterTheList(iConstraint: String) {
        if (iConstraint.isEmpty()) {
            filteredList = list
            Log.d("Alex", "filteredList changed 2, new size " + filteredList.size)
            Log.d("Alex", "transactionSearchTExt2 is " + MyApplication.transactionSearchText)
        } else {
            val resultList: MutableList<Expenditure> = mutableListOf<Expenditure>()
            val splitSearchTerms: List<String> = iConstraint.split(" ")
            for (row in list) {
                var found = true
                for (r in splitSearchTerms) {
                    if (! row.contains(r))
                        found = false
                }

                if (found) {
                    resultList.add(row)
                }
            }
            filteredList = resultList
            Log.d("Alex", "filteredList changed 3, new size " + filteredList.size)
            Log.d("Alex", "transactionSearchTExt3 is " + MyApplication.transactionSearchText)
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = filteredList[position]

        holder.vtf_date.text = data.date
        if (groupList[position] == 0) { // ie first transaction on this date
            holder.vtf_date.isVisible = true
            holder.vtf_date.paint.isUnderlineText = true
        } else {
            holder.vtf_date.isVisible = false
        }
        val dec = DecimalFormat("#.00")
        val formattedAmount = (data.amount / 100).toDouble() + (data.amount % 100).toDouble() / 100
        holder.vtf_amount.text = dec.format(formattedAmount)
        holder.vtf_category.text = data.category
        holder.vtf_subcategory.text = data.subcategory
        if (data.paidby == data.boughtfor)
            holder.vtf_who.text = data.paidby
        else
            holder.vtf_who.text =
                data.paidby.subSequence(0, 2).toString() + ":" + data.boughtfor.subSequence(0, 2)
                    .toString()
        holder.vtf_note.text = data.note
        holder.vtf_type.text = data.type
        holder.itemView.setOnClickListener { listener(data) }
        if (SpenderViewModel.singleUser()) {
            holder.vtf_who.visibility = View.GONE
        }
    }

    fun setGroupList() {
        groupList.clear()
        var c = 0 // row counter
        var j = 0 // number of transaction within specific date

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

    fun getCount(): Int {
        return filteredList.size
    }

    fun getPositionOf(currentTopPosition: Int, jump: String): Int {
        var newPosition: Int
        when (jump) {
            "-year" -> {
                if (currentTopPosition == 0) return 0
                newPosition = currentTopPosition - 1
                val targetYear: String
                targetYear = if (filteredList[currentTopPosition].date.substring(
                        0,
                        4
                    ) == filteredList[currentTopPosition - 1].date.substring(0, 4)
                )
                // we're not at beginning of current year, so aim for that
                    filteredList[currentTopPosition].date.substring(0, 4)
                else
                //we're already at beginning of current year, so aim for previous year
                    filteredList[currentTopPosition - 1].date.substring(0, 4)
                while (newPosition >= 0 && filteredList[newPosition].date.substring(
                        0,
                        4
                    ) >= targetYear
                ) {
                    newPosition--
                }
                newPosition++
                return newPosition
            }
            "-month" -> {
                if (currentTopPosition == 0) return 0
                newPosition = currentTopPosition - 1
                val targetYearMonth: String
                targetYearMonth = if (filteredList[currentTopPosition].date.substring(
                        0,
                        7
                    ) == filteredList[currentTopPosition - 1].date.substring(0, 7)
                )
                // we're not at beginning of current month, so aim for that
                    filteredList[currentTopPosition].date.substring(0, 7)
                else
                //we're already at beginning of current month, so aim for previous month
                    filteredList[currentTopPosition - 1].date.substring(0, 7)
                while (newPosition >= 0 && filteredList[newPosition].date.substring(0, 7)
                         >= targetYearMonth
                ) {
                    newPosition--
                }
                newPosition++
                return newPosition
            }
            "today" -> {
                val currentDate: String
                val cal = android.icu.util.Calendar.getInstance()
                currentDate = giveMeMyDateFormat(cal)
                newPosition = 0
                while (newPosition < filteredList.size && filteredList[newPosition].date < currentDate) {
                    newPosition++
                }
                return (newPosition -1)
            }
            "+month" -> {
                Log.d("Alex", "currentTopPosition = $currentTopPosition")
                val currentYearMonth: String
                newPosition = currentTopPosition + 1
                currentYearMonth = filteredList[currentTopPosition].date.substring(0, 7)
                while (newPosition < filteredList.size && filteredList[newPosition].date.substring(
                        0,
                        7
                    ) == currentYearMonth
                ) {
                    newPosition++
                }
                Log.d("Alex", "newPosition is $newPosition")
                return newPosition
            }
            "+year" -> {
                val currentYear: String
                newPosition = currentTopPosition + 1
                currentYear = filteredList[currentTopPosition].date.substring(0, 4)
                while (newPosition < filteredList.size && filteredList[newPosition].date.substring(
                        0,
                        4
                    ) == currentYear
                ) {
                    newPosition++
                }
                Log.d("Alex", "+year newPosition is $newPosition")
                if (newPosition >= filteredList.size)
                    newPosition = filteredList.size - 1
                return newPosition
            }
        }
        return 0
    }
}