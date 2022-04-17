package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

data class LoanPayment(val paymentDate: Calendar, val paymentAmount: Double, val interestAmount: Double,
val principalAmount: Double, val loanPrincipalRemaining: Double)

class LoanAdapter (context: Context, data: MutableList<LoanPayment>): BaseAdapter() {
    private var myData: MutableList<LoanPayment> = mutableListOf()
    init {
        myData = data
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
    class LoanViewHolder(view: View) {
        var vhDate: TextView = view.findViewById(R.id.row_payment_date)
        var vhAmount: TextView = view.findViewById(R.id.row_amount)
        var vhInterestAmount: TextView = view.findViewById(R.id.row_interest_amount)
        var vhPrincipalAmount: TextView = view.findViewById(R.id.row_principal_amount)
        var vhRemainingPrincipal: TextView = view.findViewById(R.id.row_principal_remaining_amount)
    }
    @SuppressLint("SetTextI18n")
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: LoanViewHolder
        val bData = getItem(pos) as LoanPayment

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_loan, parent, false)
        viewHolder = LoanViewHolder(myConvertView)

        viewHolder.vhDate.text = giveMeMyDateFormat(bData.paymentDate)
        viewHolder.vhAmount.text = gDec.format(bData.paymentAmount)
        viewHolder.vhInterestAmount.text = gDec.format(bData.interestAmount)
        viewHolder.vhPrincipalAmount.text = gDec.format(bData.principalAmount)
        viewHolder.vhRemainingPrincipal.text = gDec.format(bData.loanPrincipalRemaining)
        return myConvertView
    }
}