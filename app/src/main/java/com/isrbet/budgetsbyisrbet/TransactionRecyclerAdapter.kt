package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.l4digital.fastscroll.FastScroller
import java.math.BigDecimal
import java.math.RoundingMode

class TransactionRecyclerAdapter(
    private val context: Context, private val list: MutableList<Transaction>,
    filters: PreviousFilters,
    private val listener: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionRecyclerAdapter.ViewHolder>(),
    Filterable, FastScroller.SectionIndexer {

    var filteredList: MutableList<Transaction> = mutableListOf()
    private var groupList: MutableList<Int> = mutableListOf()
    private var runningTotalList: MutableList<Double> = mutableListOf()
    var currentTotal = 0.0
    private var categoryIDFilter = 0
    private var categoryFilter = ""
    private var subcategoryFilter = ""
    private var discretionaryFilter = ""
    private var paidbyFilter = -1
    private var boughtforFilter = -1
    private var typeFilter = ""
    private var accountingFilter = false

    init {
        categoryFilter = filters.prevCategoryFilter
        subcategoryFilter = filters.prevSubcategoryFilter
        discretionaryFilter = filters.prevDiscretionaryFilter
        paidbyFilter = filters.prevPaidbyFilter
        boughtforFilter = filters.prevBoughtForFilter
        typeFilter = filters.prevTypeFilter
        filterTheList(MyApplication.transactionSearchText)
        currentTotal = getTotal()
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
                currentTotal = getTotal()
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as MutableList<Transaction>
                notifyDataSetChanged()
            }
        }
    }

    fun getTotal(): Double {
        var tTotal = 0.0
        filteredList.forEach {
            if (it.type != cTRANSACTION_TYPE_TRANSFER)
                tTotal += it.amount
        }
        return tTotal
    }

    // this version is used by search
    fun filterTheList(iConstraint: String) {
        if (iConstraint.isEmpty() && categoryIDFilter == 0 &&
            categoryFilter == "" && subcategoryFilter == "" &&
            discretionaryFilter == "" && paidbyFilter == -1 &&
            boughtforFilter == -1 && typeFilter == "" && !accountingFilter
        ) {
            filteredList = list
        } else {
            val resultList: MutableList<Transaction> = mutableListOf()
            val splitSearchTerms: List<String> = iConstraint.split(" ")
            var subcatDiscIndicator = ""
            for (row in list) {
                var found = true
                for (r in splitSearchTerms) {
                    found = found && row.contains(r)
                }
                if (found) { // ie no sense looking further if search term didn't match
                    if (discretionaryFilter != "") {
                        subcatDiscIndicator = CategoryViewModel.getCategory(row.category)?.discType.toString()
                    }
                    found = if ((categoryIDFilter == 0 || row.category == categoryIDFilter) &&
                        (categoryFilter == "" ||
                        CategoryViewModel.getCategory(row.category)?.categoryName == categoryFilter) &&
                        (subcategoryFilter == "" ||
                        CategoryViewModel.getCategory(row.category)?.subcategoryName == subcategoryFilter) &&
                        (paidbyFilter == -1 || row.paidby == paidbyFilter) &&
                        (boughtforFilter == -1 || row.boughtfor == boughtforFilter) &&
                        (typeFilter == "" || row.type == typeFilter || (row.type == "" && typeFilter ==
                                MyApplication.getString(R.string.expense))) &&
                        (discretionaryFilter == "" || discretionaryFilter == subcatDiscIndicator)
                    ) {
                        if (accountingFilter) {
                            if (row.paidby == row.boughtfor && row.paidby != 2)
                                false
                            else if (row.paidby == 2 && row.boughtfor == 2 &&
                                row.bfname1split == (SpenderViewModel.getSpenderSplit(0)*100).toInt()
                            )
                                false
                            else
                                found
                        } else
                            found
                    } else
                        false
                }
                if (found) {
                    resultList.add(row)
                }
            }
            filteredList = resultList
        }
        setGroupList()
        currentTotal = getTotal()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vtfdate: TextView = view.findViewById(R.id.vtf_date)
        val vtfamount: TextView = view.findViewById(R.id.vtf_amount)
        val vtfpercentage1: TextView = view.findViewById(R.id.vtf_percentage1)
        val vtfpercentage2: TextView = view.findViewById(R.id.vtf_percentage2)
        val vtfCategoryID: TextView = view.findViewById(R.id.vtf_category_id)
        val vtfcategory: TextView = view.findViewById(R.id.vtf_category)
        val vtfwho: TextView = view.findViewById(R.id.vtf_who)
        val vtfnote: TextView = view.findViewById(R.id.vtf_note)
        val vtfdisc: TextView = view.findViewById(R.id.vtf_disc)
        val vtftype: TextView = view.findViewById(R.id.vtf_type)
        val vtfrunningtotal: TextView = view.findViewById(R.id.vtf_running_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.row_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (filteredList.size == 0 || groupList.size == 0)
            return

        val data = filteredList[position]

        holder.vtfdate.text = data.date
        if (groupList[position] == 0) { // ie first transaction on this date
            holder.vtfdate.isVisible = true
            holder.vtfdate.paint.isUnderlineText = true
        } else {
            holder.vtfdate.isVisible = false
        }
        holder.vtfamount.text = gDecWithCurrency(data.amount)
        val percentage1 = data.amount * data.bfname1split / 100
        val rounded = BigDecimal(percentage1).setScale(2, RoundingMode.HALF_UP)
        holder.vtfpercentage1.text = gDecWithCurrency(rounded.toDouble())
        val percentage2 = data.amount - rounded.toDouble()
        holder.vtfpercentage2.text = gDecWithCurrency(percentage2)
        holder.vtfrunningtotal.text = gDecWithCurrency((runningTotalList[position] * 100).toInt() / 100.0)
        holder.vtfCategoryID.text = data.category.toString()
        holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
        if (data.paidby == data.boughtfor)
            holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
        else
            holder.vtfwho.text = String.format("%s:%s",
                SpenderViewModel.getSpenderName(data.paidby).subSequence(0, 2).toString(),
                SpenderViewModel.getSpenderName(data.boughtfor).subSequence(0, 2).toString())
        holder.vtfnote.text = data.note
        if (CategoryViewModel.getCategory(data.category)?.discType == cDiscTypeDiscretionary)
            holder.vtfdisc.text = MyApplication.getString(R.string.disc_short)
        else
            holder.vtfdisc.text = MyApplication.getString(R.string.non_disc_short)
        if (data.type.isNotEmpty())
            holder.vtftype.text = data.type.substring(0, 1)
        else
            holder.vtftype.text = ""
        if (holder.vtftype.text == "R") holder.vtftype.text = MyApplication.getString(R.string.scheduled_payment_short)  // now called Scheduled payment rather than Recurring transaction
        holder.itemView.setOnClickListener { listener(data) }
        if (SpenderViewModel.singleUser()) {
            holder.vtfwho.visibility = View.GONE
        }
        if (accountingFilter || !DefaultsViewModel.getDefaultShowCategoryInViewAll()) {
            holder.vtfcategory.visibility = View.GONE
        }
        if (!accountingFilter && !DefaultsViewModel.getDefaultShowIndividualAmountsInViewAll()) {
            holder.vtfpercentage1.visibility = View.GONE
            holder.vtfpercentage2.visibility = View.GONE
        }
        if (!accountingFilter && !DefaultsViewModel.getDefaultShowTypeInViewAll())
            holder.vtftype.visibility = View.GONE
        if (!accountingFilter && !DefaultsViewModel.getDefaultShowWhoInViewAll())
            holder.vtfwho.visibility = View.GONE
        if (accountingFilter || !DefaultsViewModel.getDefaultShowNoteInViewAll())
            holder.vtfnote.visibility = View.GONE
        if (accountingFilter || !DefaultsViewModel.getDefaultShowDiscInViewAll())
            holder.vtfdisc.visibility = View.GONE
        if (!accountingFilter && !DefaultsViewModel.getDefaultShowRunningTotalInViewAll())
            holder.vtfrunningtotal.visibility = View.GONE
    }

    fun setCategoryIDFilter(iFilter: Int) {
        categoryIDFilter = iFilter
    }
    fun setCategoryFilter(iFilter: String) {
        categoryFilter = iFilter
    }

    fun setSubcategoryFilter(iFilter: String) {
        subcategoryFilter = iFilter
    }

    fun setDiscretionaryFilter(iFilter: String) {
        discretionaryFilter = iFilter
    }

    fun setPaidByFilter(iFilter: Int) {
        paidbyFilter = iFilter
    }

    fun setBoughtForFilter(iFilter: Int) {
        boughtforFilter = iFilter
    }

    fun setTypeFilter(iFilter: String) {
        typeFilter = iFilter
    }

    fun setAccountingFilter(iFilter: Boolean) {
        accountingFilter = iFilter
    }

    private fun setGroupList() {
        val tgroupList: MutableList<Int> = mutableListOf()
        tgroupList.clear()
        val trunningTotalList: MutableList<Double> = mutableListOf()
        trunningTotalList.clear()
        var c = 0 // row counter
        var j = 0 // number of transaction within specific date
        var previousRunningTotal = 0.0

        for (i in 0 until filteredList.size) {
            // calculating what name 1 owes name 2
            if (filteredList[i].paidby != filteredList[i].boughtfor ||
                filteredList[i].paidby == 2
            ) {
                val name1PortionOfExpense =
//                    round(filteredList[i].amount * filteredList[i].bfname1split) / 100.0
                    filteredList[i].amount * filteredList[i].bfname1split / 100.0
                val name1PortionOfFundsUsed =
                    when (filteredList[i].paidby) {
                        0 -> filteredList[i].amount
                        1 -> 0.0
                        else // must be Joint
                        -> filteredList[i].amount * SpenderViewModel.getSpenderSplit(0)
                    }
                trunningTotalList.add(previousRunningTotal + name1PortionOfExpense - name1PortionOfFundsUsed)
                previousRunningTotal += (name1PortionOfExpense - name1PortionOfFundsUsed)
//                previousRunningTotal = round(previousRunningTotal * 100) / 100
            } else {
                trunningTotalList.add(previousRunningTotal)
            }
            if (tgroupList.size == 0) {
                tgroupList.add(c, j)
                c++
                j++
            } else {
                if (filteredList[i].date == filteredList[i - 1].date) {
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
        runningTotalList = trunningTotalList
    }

    fun getCount(): Int {
        return filteredList.size
    }

    fun getPositionOf(currentTopPosition: Int, jump: Int): Int {
        var newPosition: Int
        when (jump) {
            cPREV_YEAR -> {
                if (currentTopPosition == 0) return 0
                newPosition = currentTopPosition - 1
                val targetYear: String = if (filteredList[currentTopPosition].date.substring(
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
            cPREV_MONTH -> {
                if (currentTopPosition == 0) return 0
                newPosition = currentTopPosition - 1
                val targetYearMonth: String = if (filteredList[currentTopPosition].date.substring(
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
            cTODAY -> {
                val currentDate = giveMeMyDateFormat(gCurrentDate)
                newPosition = 0
                while (newPosition < filteredList.size && filteredList[newPosition].date < currentDate) {
                    newPosition++
                }
                return (newPosition - 1)
            }
            cNEXT_MONTH -> {
                newPosition = currentTopPosition + 1
                val currentYearMonth: String = filteredList[currentTopPosition].date.substring(0, 7)
                while (newPosition < filteredList.size && filteredList[newPosition].date.substring(
                        0,
                        7
                    ) == currentYearMonth
                ) {
                    newPosition++
                }
                return newPosition
            }
            cNEXT_YEAR -> {
                newPosition = currentTopPosition + 1
                val currentYear: String = filteredList[currentTopPosition].date.substring(0, 4)
                while (newPosition < filteredList.size && filteredList[newPosition].date.substring(
                        0,
                        4
                    ) == currentYear
                ) {
                    newPosition++
                }
                if (newPosition >= filteredList.size)
                    newPosition = filteredList.size - 1
                return newPosition
            }
        }
        return 0
    }

    override fun getSectionText(position: Int): CharSequence {
        return if (position >= 0)
            filteredList[position].date
        else
            ""
    }
}