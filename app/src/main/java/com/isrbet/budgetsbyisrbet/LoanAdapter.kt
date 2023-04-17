package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

data class LoanPayment(val paymentDate: MyDate, val paymentAmount: Double,
                       val calculatedPaymentAmount: Double, val interestAmount: Double,
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

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: LoanViewHolder
        val bData = getItem(pos) as LoanPayment

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_loan, parent, false)
        viewHolder = LoanViewHolder(myConvertView)

        viewHolder.vhDate.text = bData.paymentDate.toString()
        viewHolder.vhAmount.text = gDecWithCurrency(bData.paymentAmount)
        viewHolder.vhInterestAmount.text = gDecWithCurrency(bData.interestAmount)
        viewHolder.vhPrincipalAmount.text = gDecWithCurrency(bData.principalAmount)
        viewHolder.vhRemainingPrincipal.text = gDecWithCurrency(bData.loanPrincipalRemaining)
        return myConvertView
    }
}