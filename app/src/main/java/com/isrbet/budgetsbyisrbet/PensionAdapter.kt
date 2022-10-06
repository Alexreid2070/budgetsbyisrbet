package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class PensionAdapter (context: Context): BaseAdapter() {

    private var myData: MutableList<Pension> = arrayListOf()
    init {
        myData.clear()
        for (i in 0 until RetirementViewModel.getWorkingPensionListCount()) {
            RetirementViewModel.getWorkingPension(i).let { myData.add(it) }
        }
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
    class PensionViewHolder(view: View) {
        var vhDescription: TextView = view.findViewById(R.id.pensionDescription)
        var vhDescription2: TextView = view.findViewById(R.id.pensionDescription2)
        var vhPensionType: TextView = view.findViewById(R.id.row_pension_type)
        var vhName: TextView = view.findViewById(R.id.row_pension_name)
        var vhValue: TextView = view.findViewById(R.id.row_pension_value)
        var vhWorkStartDate: TextView = view.findViewById(R.id.row_pension_work_start_date)
        var vhPensionStartDate: TextView = view.findViewById(R.id.row_pension_start_date)
        var vhBest5: TextView = view.findViewById(R.id.row_pension_best5)
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: PensionViewHolder
        val pension = getItem(pos) as Pension

        val myConvertView: View = convertView ?: inflater.inflate(R.layout.row_pension, parent, false)
        viewHolder = PensionViewHolder(myConvertView)

        val desc1 = pension.name
        val desc2 = when (pension.pensionType) {
            PensionType.BASIC ->
                String.format("%s starts on %s", gDecWithCurrency(pension.value), pension.pensionStartDate)
            PensionType.OTPP ->
                if (pension.pensionStartDelay > 0)
                    String.format("Work start %s, Salary %s, Delay %s", pension.workStartDate,
                        pension.best5YearsSalary, pension.pensionStartDelay)
                else
                    String.format("Work start %s, Salary %s", pension.workStartDate, gDecWithCurrency(pension.best5YearsSalary))
            PensionType.PSPP ->
                String.format("Work start %s, Salary %s", pension.workStartDate, gDecWithCurrency(pension.best5YearsSalary))
        }
        viewHolder.vhDescription.text = desc1
        viewHolder.vhDescription2.text = desc2
        viewHolder.vhPensionType.text = pension.pensionType.toString()
        viewHolder.vhName.text = pension.name
        viewHolder.vhValue.text = pension.value.toString()
        viewHolder.vhWorkStartDate.text = pension.workStartDate
        viewHolder.vhPensionStartDate.text = pension.pensionStartDate
        viewHolder.vhBest5.text = pension.best5YearsSalary.toString()

        return myConvertView
    }
}