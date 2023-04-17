package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.l4digital.fastscroll.FastScroller
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

class TransactionRecyclerAdapter(
    private val context: Context, private var list: MutableList<Transaction>,
    filters: PreviousFilters,
    iSortOrder: TransactionSortOrder = TransactionSortOrder.DATE_ASCENDING,
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
    private var currentSortOrder = iSortOrder

    init {
        categoryFilter = filters.prevCategoryFilter
        subcategoryFilter = filters.prevSubcategoryFilter
        discretionaryFilter = filters.prevDiscretionaryFilter
        paidbyFilter = filters.prevPaidbyFilter
        boughtforFilter = filters.prevBoughtForFilter
        typeFilter = filters.prevTypeFilter
        filterTheList(MyApplication.transactionSearchText)
        currentTotal = getTotal()
        sortBy(iSortOrder)
    }

    fun reset(iList: MutableList<Transaction>) {
        list = iList
        filterTheList("")
    }
    fun sortBy(iSortOrder: TransactionSortOrder) {
        currentSortOrder = iSortOrder
        when (currentSortOrder) {
            TransactionSortOrder.DATE_ASCENDING -> filteredList.sortBy { it.date.toString() }
            TransactionSortOrder.DATE_DESCENDING -> filteredList.sortByDescending { it.date.toString() }
            TransactionSortOrder.AMOUNT_ASCENDING -> filteredList.sortBy { it.amount }
            TransactionSortOrder.AMOUNT_DESCENDING -> filteredList.sortByDescending { it.amount }
            TransactionSortOrder.CATEGORY_ASCENDING -> filteredList.sortBy { CategoryViewModel.getFullCategoryName(it.category) }
            TransactionSortOrder.CATEGORY_DESCENDING -> filteredList.sortByDescending { CategoryViewModel.getFullCategoryName(it.category) }
            TransactionSortOrder.WHO_ASCENDING -> filteredList.sortBy { SpenderViewModel.getSpenderName(it.paidby)+SpenderViewModel.getSpenderName(it.boughtfor) }
            TransactionSortOrder.WHO_DESCENDING -> filteredList.sortByDescending { SpenderViewModel.getSpenderName(it.paidby)+SpenderViewModel.getSpenderName(it.boughtfor) }
            TransactionSortOrder.NOTE_ASCENDING -> filteredList.sortBy { it.note.lowercase() }
            TransactionSortOrder.NOTE_DESCENDING -> filteredList.sortByDescending { it.note.lowercase() }
            TransactionSortOrder.TYPE_ASCENDING -> filteredList.sortBy { it.type }
            TransactionSortOrder.TYPE_DESCENDING -> filteredList.sortByDescending { it.type }
        }
        setGroupList()
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

        if (currentSortOrder == TransactionSortOrder.DATE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.DATE_DESCENDING) {
            holder.vtfdate.text = data.date.toString()
            holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
            if (data.paidby == data.boughtfor)
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfnote.text = data.note
            holder.vtftype.text = data.type
            holder.vtfamount.text = gDecWithCurrency(data.amount)
        }
        if (currentSortOrder == TransactionSortOrder.CATEGORY_ASCENDING ||
            currentSortOrder == TransactionSortOrder.CATEGORY_DESCENDING) {
            holder.vtfdate.text = CategoryViewModel.getFullCategoryName(data.category)
            holder.vtfcategory.text = data.date.toString()
            if (data.paidby == data.boughtfor)
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfnote.text = data.note
            holder.vtftype.text = data.type
            holder.vtfamount.text = gDecWithCurrency(data.amount)
        }
        if (currentSortOrder == TransactionSortOrder.WHO_ASCENDING ||
            currentSortOrder == TransactionSortOrder.WHO_DESCENDING) {
            if (data.paidby == data.boughtfor)
                holder.vtfdate.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfdate.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfwho.text = data.date.toString()
            holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
            holder.vtfnote.text = data.note
            holder.vtftype.text = data.type
            holder.vtfamount.text = gDecWithCurrency(data.amount)
        }
        if (currentSortOrder == TransactionSortOrder.NOTE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.NOTE_DESCENDING) {
            holder.vtfdate.text = data.note
            holder.vtfnote.text = data.date.toString()
            if (data.paidby == data.boughtfor)
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
            holder.vtftype.text = data.type
            holder.vtfamount.text = gDecWithCurrency(data.amount)
        }
        if (currentSortOrder == TransactionSortOrder.TYPE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.TYPE_DESCENDING) {
            holder.vtfdate.text = data.type
            holder.vtftype.text = data.date.toString()
            if (data.paidby == data.boughtfor)
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
            holder.vtfnote.text = data.note
            holder.vtfamount.text = gDecWithCurrency(data.amount)
        }
        if (currentSortOrder == TransactionSortOrder.AMOUNT_ASCENDING ||
            currentSortOrder == TransactionSortOrder.AMOUNT_DESCENDING) {
            holder.vtfdate.text = gDecWithCurrency(data.amount)
            holder.vtfamount.text = data.date.toString()
            holder.vtftype.text = data.date.toString()
            if (data.paidby == data.boughtfor)
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby)
            else
                holder.vtfwho.text = SpenderViewModel.getSpenderName(data.paidby).substring(0,2) +
                        ":" + SpenderViewModel.getSpenderName(data.boughtfor).substring(0,2)
            holder.vtfcategory.text = CategoryViewModel.getFullCategoryName(data.category)
            holder.vtfnote.text = data.note
        }
        if (position < groupList.size) {
            if (groupList[position] == -100) { // ie first transaction for this year
                holder.vtfdate.isVisible = true
                holder.vtfdate.paint.isUnderlineText = true
                holder.vtfdate.setTypeface(null, Typeface.BOLD)
                val hexColor = getColorInHex(MaterialColors.getColor(context, R.attr.colorSecondary, Color.BLACK), cOpacity)
                holder.vtfdate.setBackgroundColor(Color.parseColor(hexColor))
            } else if (groupList[position] == -10) { // ie first transaction for this month
                holder.vtfdate.isVisible = true
                holder.vtfdate.paint.isUnderlineText = true
                holder.vtfdate.setTypeface(null, Typeface.BOLD)
            } else holder.vtfdate.isVisible = groupList[position] == 0
        }
        val percentage1 = data.amount * data.bfname1split / 100
        val rounded = BigDecimal(percentage1).setScale(2, RoundingMode.HALF_UP)
        holder.vtfpercentage1.text = gDecWithCurrency(rounded.toDouble())
        val percentage2 = data.amount - rounded.toDouble()
        holder.vtfpercentage2.text = gDecWithCurrency(percentage2)
        if (position < runningTotalList.size)
            holder.vtfrunningtotal.text = gDecWithCurrency((runningTotalList[position] * 100).toInt() / 100.0)
        holder.vtfCategoryID.text = data.category.toString()
        if (CategoryViewModel.getCategory(data.category)?.discType == cDiscTypeDiscretionary)
            holder.vtfdisc.text = MyApplication.getString(R.string.disc_short)
        else
            holder.vtfdisc.text = MyApplication.getString(R.string.non_disc_short)
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
                tgroupList.add(c, -100)
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.DATE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.DATE_DESCENDING) {
                if (filteredList[i].date.toString() == filteredList[i - 1].date.toString()) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    if (filteredList[i].date.getYear() != filteredList[i - 1].date.getYear())
                        tgroupList.add(c, -100) // -100 symbolizes change in year
                    else if (filteredList[i].date.getMonth() != filteredList[i - 1].date.getMonth()) {
                        tgroupList.add(c, -10) // -10 symbolizes change in month
                    } else
                        tgroupList.add(c, j)
                }
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.AMOUNT_ASCENDING ||
                currentSortOrder == TransactionSortOrder.AMOUNT_DESCENDING) {
                if (filteredList[i].amount == filteredList[i - 1].amount) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    tgroupList.add(c, -100) // -10 symbolizes change
                }
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.CATEGORY_ASCENDING ||
                currentSortOrder == TransactionSortOrder.CATEGORY_DESCENDING) {
                if (filteredList[i].category == filteredList[i - 1].category) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    tgroupList.add(c, -100) // -10 symbolizes change
                }
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.WHO_ASCENDING ||
                currentSortOrder == TransactionSortOrder.WHO_DESCENDING) {
                if (filteredList[i].paidby == filteredList[i - 1].paidby &&
                        filteredList[i].boughtfor == filteredList[i-1].boughtfor) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    tgroupList.add(c, -100) // -10 symbolizes change
                }
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.NOTE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.NOTE_DESCENDING) {
                if (filteredList[i].note == filteredList[i - 1].note) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    tgroupList.add(c, -100) // -10 symbolizes change
                }
                c++
                j++
            } else if (currentSortOrder == TransactionSortOrder.TYPE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.TYPE_DESCENDING) {
                if (filteredList[i].type == filteredList[i - 1].type) {
                    tgroupList.add(c, j)
                } else {
                    j = 0
                    tgroupList.add(c, -100) // -10 symbolizes change
                }
                c++
                j++
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
        if (currentSortOrder == TransactionSortOrder.DATE_ASCENDING ||
                currentSortOrder == TransactionSortOrder.DATE_DESCENDING) {
            when (jump) {
                cPREV_YEAR -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val targetYear = if (filteredList[currentTopPosition].date.getYear()
                        == filteredList[currentTopPosition - 1].date.getYear())
                    // we're not at beginning of current year, so aim for that
                        filteredList[currentTopPosition].date.getYear()
                    else
                    //we're already at beginning of current year, so aim for previous year
                        filteredList[currentTopPosition - 1].date.getYear()
                    while (newPosition >= 0 && filteredList[newPosition].date.getYear() >= targetYear
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val targetYearMonth: String = if (filteredList[currentTopPosition].date.getYYYYMM()
                        == filteredList[currentTopPosition - 1].date.getYYYYMM()
                    )
                    // we're not at beginning of current month, so aim for that
                        filteredList[currentTopPosition].date.getYYYYMM()
                    else
                    //we're already at beginning of current month, so aim for previous month
                        filteredList[currentTopPosition - 1].date.getYYYYMM()
                    while (newPosition >= 0 && filteredList[newPosition].date.getYYYYMM()
                        >= targetYearMonth
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH -> {
                    newPosition = currentTopPosition + 1
                    val currentYearMonth: String = filteredList[currentTopPosition].date.getYYYYMM()
                    while (newPosition < filteredList.size && filteredList[newPosition].date.getYYYYMM() == currentYearMonth
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
                cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentYear = filteredList[currentTopPosition].date.getYear()
                    while (newPosition < filteredList.size && filteredList[newPosition].date.getYear() == currentYear
                    ) {
                        newPosition++
                    }
                    if (newPosition >= filteredList.size)
                        newPosition = filteredList.size - 1
                    return newPosition
                }
            }
        } else if (currentSortOrder == TransactionSortOrder.CATEGORY_ASCENDING ||
            currentSortOrder == TransactionSortOrder.CATEGORY_DESCENDING) {
            when (jump) {
                cPREV_YEAR, cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val target = if (filteredList[currentTopPosition].category
                        == filteredList[currentTopPosition - 1].category)
                    // we're not at beginning of current target, so aim for that
                        filteredList[currentTopPosition].category
                    else
                    //we're already at beginning of current target, so aim for previous year
                        filteredList[currentTopPosition - 1].category
                    while (newPosition >= 0 && filteredList[newPosition].category >= target
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH, cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentTarget = filteredList[currentTopPosition].category
                    while (newPosition < filteredList.size && filteredList[newPosition].category == currentTarget
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
            }
        } else if (currentSortOrder == TransactionSortOrder.AMOUNT_ASCENDING ||
            currentSortOrder == TransactionSortOrder.AMOUNT_DESCENDING) {
            when (jump) {
                cPREV_YEAR, cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val target = if (filteredList[currentTopPosition].amount
                        == filteredList[currentTopPosition - 1].amount)
                    // we're not at beginning of current target, so aim for that
                        filteredList[currentTopPosition].amount
                    else
                    //we're already at beginning of current target, so aim for previous year
                        filteredList[currentTopPosition - 1].amount
                    while (newPosition >= 0 && filteredList[newPosition].amount >= target
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH, cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentTarget = filteredList[currentTopPosition].amount
                    while (newPosition < filteredList.size && filteredList[newPosition].amount == currentTarget
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
            }
        } else if (currentSortOrder == TransactionSortOrder.TYPE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.TYPE_DESCENDING) {
            when (jump) {
                cPREV_YEAR, cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val target = if (filteredList[currentTopPosition].type
                        == filteredList[currentTopPosition - 1].type)
                    // we're not at beginning of current target, so aim for that
                        filteredList[currentTopPosition].type
                    else
                    //we're already at beginning of current target, so aim for previous year
                        filteredList[currentTopPosition - 1].type
                    while (newPosition >= 0 && filteredList[newPosition].type >= target
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH, cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentTarget = filteredList[currentTopPosition].type
                    while (newPosition < filteredList.size && filteredList[newPosition].type == currentTarget
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
            }
        } else if (currentSortOrder == TransactionSortOrder.NOTE_ASCENDING ||
            currentSortOrder == TransactionSortOrder.NOTE_DESCENDING) {
            when (jump) {
                cPREV_YEAR, cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val target = if (filteredList[currentTopPosition].note
                        == filteredList[currentTopPosition - 1].note)
                    // we're not at beginning of current target, so aim for that
                        filteredList[currentTopPosition].note
                    else
                    //we're already at beginning of current target, so aim for previous year
                        filteredList[currentTopPosition - 1].note
                    while (newPosition >= 0 && filteredList[newPosition].note >= target
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH, cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentTarget = filteredList[currentTopPosition].note
                    while (newPosition < filteredList.size && filteredList[newPosition].note == currentTarget
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
            }
        } else if (currentSortOrder == TransactionSortOrder.WHO_ASCENDING ||
            currentSortOrder == TransactionSortOrder.WHO_DESCENDING) {
            when (jump) {
                cPREV_YEAR, cPREV_MONTH -> {
                    if (currentTopPosition == 0) return 0
                    newPosition = currentTopPosition - 1
                    val target = if ((filteredList[currentTopPosition].paidby.toString() + filteredList[currentTopPosition].boughtfor.toString())
                        == (filteredList[currentTopPosition-1].paidby.toString() + filteredList[currentTopPosition-1].boughtfor.toString()))
                    // we're not at beginning of current target, so aim for that
                        (filteredList[currentTopPosition].paidby.toString() + filteredList[currentTopPosition].boughtfor.toString())
                    else
                    //we're already at beginning of current target, so aim for previous year
                        (filteredList[currentTopPosition-1].paidby.toString() + filteredList[currentTopPosition-1].boughtfor.toString())
                    while (newPosition >= 0 && (filteredList[newPosition].paidby.toString() + filteredList[newPosition].boughtfor.toString()) >= target
                    ) {
                        newPosition--
                    }
                    newPosition++
                    return newPosition
                }
                cNEXT_MONTH, cNEXT_YEAR -> {
                    newPosition = currentTopPosition + 1
                    val currentTarget = (filteredList[currentTopPosition].paidby.toString() + filteredList[currentTopPosition].boughtfor.toString())
                    while (newPosition < filteredList.size && (filteredList[newPosition].paidby.toString() + filteredList[newPosition].boughtfor.toString()) == currentTarget
                    ) {
                        newPosition++
                    }
                    return newPosition
                }
            }
        }
        return 0
    }

    override fun getSectionText(position: Int): CharSequence {
        return if (position >= 0)
            when (currentSortOrder) {
                TransactionSortOrder.DATE_ASCENDING, TransactionSortOrder.DATE_DESCENDING ->
                    filteredList[position].date.toString()
                TransactionSortOrder.CATEGORY_ASCENDING, TransactionSortOrder.CATEGORY_DESCENDING ->
                    CategoryViewModel.getFullCategoryName(filteredList[position].category)
                TransactionSortOrder.WHO_ASCENDING, TransactionSortOrder.WHO_DESCENDING -> {
                    if (filteredList[position].paidby == filteredList[position].boughtfor)
                        SpenderViewModel.getSpenderName(filteredList[position].paidby)
                    else
                        SpenderViewModel.getSpenderName(filteredList[position].paidby) + ":" +
                            SpenderViewModel.getSpenderName(filteredList[position].boughtfor)
                }
                TransactionSortOrder.NOTE_ASCENDING, TransactionSortOrder.NOTE_DESCENDING ->
                    filteredList[position].note
                TransactionSortOrder.TYPE_ASCENDING, TransactionSortOrder.TYPE_DESCENDING ->
                    filteredList[position].type
                TransactionSortOrder.AMOUNT_ASCENDING, TransactionSortOrder.AMOUNT_DESCENDING ->
                    gDecWithCurrency(filteredList[position].amount)
            }
        else
            ""
    }
}