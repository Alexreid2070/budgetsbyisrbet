package com.isrbet.budgetsbyisrbet

import android.app.DatePickerDialog
import android.icu.text.NumberFormat
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.isrbet.budgetsbyisrbet.databinding.FragmentLoanBinding
import timber.log.Timber
import java.util.*
import kotlin.math.pow
import kotlin.math.round

class LoanFragment : Fragment() {
    private var _binding: FragmentLoanBinding? = null
    private val binding get() = _binding!!
    private val args: LoanFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoanBinding.inflate(inflater, container, false)
        binding.loanAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.amortizationPeriod.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.interestRate.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        binding.acceleratedPaymentAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        inflater.inflate(R.layout.fragment_loan, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.currencySymbol1.text = String.format("${getLocalCurrencySymbol()} ")
        binding.currencySymbol2.text = String.format("${getLocalCurrencySymbol()} ")
        if (args.loanID != "") {
            setupLoanSpinner(args.loanID)
            val sp = ScheduledPaymentViewModel.getScheduledPayment(args.loanID)
            if (sp != null) {
                loadRows(
                    sp.loanFirstPaymentDate,
                    sp.loanAmortization,
                    sp.loanPaymentRegularity,
                    sp.loanInterestRate / 100.0,
                    sp.loanAmount,
                    if (sp.actualPayment > 0.0) sp.actualPayment else sp.amount)
            }
        } else
            setupLoanSpinner()
        binding.loanStartDate.setText(gCurrentDate.toString())
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.loanStartDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.loanStartDate.setOnClickListener {
            var localDate = MyDate()
            if (binding.loanStartDate.text.toString() != "") {
                localDate = MyDate(binding.loanStartDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                localDate.getYear(),
                localDate.getMonth()-1,
                localDate.getDay()
            ).show()
        }

        binding.LoanSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                if (position > 0) {
                    val sp = ScheduledPaymentViewModel.getScheduledPayment(selection as String)
                    if (sp != null) {
                        binding.loanStartDate.setText(sp.loanFirstPaymentDate.toString())
                        binding.loanAmount.setText(gDec(sp.loanAmount))
                        binding.amortizationPeriod.setText(gDec(sp.loanAmortization))
                        binding.interestRate.setText(gDec(sp.loanInterestRate))
                        binding.acceleratedPaymentAmount.setText(gDec(if (sp.actualPayment > 0.0) sp.actualPayment else sp.amount))
                        when (sp.loanPaymentRegularity) {
                            LoanPaymentRegularity.WEEKLY -> binding.buttonWeekly.isChecked = true
                            LoanPaymentRegularity.BIWEEKLY -> binding.buttonBiweekly.isChecked = true
                            LoanPaymentRegularity.MONTHLY -> binding.buttonMonthly.isChecked = true
                        }
                    }
                }
            }
        }
        binding.buttonCalculateLoan.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            onCalculateButtonClicked()
        }
        binding.buttonReset.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            reset()
        }
        HintViewModel.showHint(parentFragmentManager, cHINT_LOAN)
    }

    private fun reset() {
        binding.LoanSpinner.setSelection(0)
        binding.loanStartDate.setText(gCurrentDate.toString())
        binding.loanAmount.setText("")
        binding.amortizationPeriod.setText("")
        binding.interestRate.setText("")
        binding.calculatedPaymentAmount.text = ""
        binding.calculatedPaymentText.visibility = View.INVISIBLE
        binding.acceleratedPaymentAmount.setText("")
        val myList: MutableList<LoanPayment> = ArrayList()
        val adapter = LoanAdapter(requireContext(), myList)
        binding.loanListView.adapter = adapter
    }

    private fun setupLoanSpinner(iSelection: String = "") {
        val listOfActiveLoans = ScheduledPaymentViewModel.getActiveLoanSPs()
        if (listOfActiveLoans.size > 0) {
            listOfActiveLoans.add(0,getString(R.string.choose_existing_loan))
            binding.loanSpinnerLayout.visibility = View.VISIBLE
            val loanSpinner = binding.LoanSpinner
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOfActiveLoans)
            loanSpinner.adapter = arrayAdapter
            if (iSelection != "") {
                loanSpinner.setSelection(arrayAdapter.getPosition(iSelection))
            }
            arrayAdapter.notifyDataSetChanged()
        } else {
            binding.loanSpinnerLayout.visibility = View.GONE
        }
    }

    private fun onCalculateButtonClicked() {
        // need to reject if all the fields aren't entered
        if (binding.loanAmount.text.toString() == "") {
            binding.loanAmount.error=getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.loanAmount)
            return
        }
        if (binding.amortizationPeriod.text.toString() == "") {
            binding.amortizationPeriod.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.amortizationPeriod)
            return
        }
        if (binding.interestRate.text.toString() == "") {
            binding.interestRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.interestRate)
            return
        }
        val selectedId = binding.paymentFrequencyGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        val freq = when (radioButton.text) {
            getString(R.string.weekly) -> LoanPaymentRegularity.WEEKLY
            getString(R.string.biweekly) -> LoanPaymentRegularity.BIWEEKLY
            else -> LoanPaymentRegularity.MONTHLY
        }

        val lNumberFormat: NumberFormat = NumberFormat.getInstance()
        val loanAmountDouble = lNumberFormat.parse(binding.loanAmount.text.toString()).toDouble()
        val accAmountDouble = binding.acceleratedPaymentAmount.text.toString().replace(',', '.').toDoubleOrNull()
        loadRows(MyDate(binding.loanStartDate.text.toString()),
            lNumberFormat.parse(binding.amortizationPeriod.text.toString()).toDouble(),
            freq,
            lNumberFormat.parse(binding.interestRate.text.toString()).toDouble()/100.0,
            loanAmountDouble,
            accAmountDouble)
    }


    private fun loadRows(iFirstPaymentDate: MyDate, iAmortizationYears: Double,
                            iPaymentRegularity: LoanPaymentRegularity,
                            iInterestRate: Double, iPrincipal: Double,
                            iAccAmount: Double?) {
        val myList = getPaymentList(iFirstPaymentDate, iAmortizationYears, iPaymentRegularity,
            iInterestRate, iPrincipal, iAccAmount)
        if (myList.size > 0) {
            binding.calculatedPaymentAmount.text =
                gDecWithCurrency(myList[0].calculatedPaymentAmount)
            binding.calculatedPaymentText.visibility = View.VISIBLE
        }
        val adapter = LoanAdapter(requireContext(), myList)
        val listView: ListView = requireActivity().findViewById(R.id.loan_list_view)
        listView.adapter = adapter
        goToCorrectRow()
    }

    private fun goToCorrectRow() {
        val listView: ListView = requireActivity().findViewById(R.id.loan_list_view)
        var row = 0
        for (i in 0 until listView.adapter.count) {
            val loanPayment: LoanPayment = listView.adapter.getItem(i) as LoanPayment
            if (loanPayment.paymentDate >= gCurrentDate) {
                row = if (i == 0) 0 else i-1
                break
            }
        }
        listView.setSelection(row)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun getPaymentList(iFirstPaymentDate: MyDate, iAmortizationYears: Double,
                   iPaymentRegularity: LoanPaymentRegularity,
                   iInterestRate: Double, iPrincipal: Double,
                   iAccAmount: Double?): MutableList<LoanPayment> {
    val myList: MutableList<LoanPayment> = ArrayList()
    val wDate = MyDate(iFirstPaymentDate)

    val iPaymentsPerYear = when (iPaymentRegularity) {
        LoanPaymentRegularity.WEEKLY -> 52
        LoanPaymentRegularity.BIWEEKLY -> 26
        LoanPaymentRegularity.MONTHLY -> 12
    }

    val calcPayment =  if (iInterestRate == 0.0)
        iPrincipal / iAmortizationYears / iPaymentsPerYear
    else
        iPrincipal * (iInterestRate / iPaymentsPerYear) * (1 + iInterestRate / iPaymentsPerYear).pow(iPaymentsPerYear * iAmortizationYears) /
                ((1 + iInterestRate / iPaymentsPerYear).pow(iPaymentsPerYear*iAmortizationYears) - 1)

    var owingAtEndOfPeriod = iPrincipal
    val extraPayment = if (iAccAmount == null) 0.0 else iAccAmount - calcPayment
    for (i in 1..(round(iAmortizationYears * iPaymentsPerYear).toInt())) {
        val interestOwingForThisPeriod = owingAtEndOfPeriod * iInterestRate / iPaymentsPerYear
        owingAtEndOfPeriod = if (iInterestRate == 0.0)
            owingAtEndOfPeriod - calcPayment
        else
            (1 + iInterestRate/iPaymentsPerYear).pow(i)*iPrincipal - (((1+iInterestRate/iPaymentsPerYear).pow(i) - 1)/(iInterestRate/iPaymentsPerYear)*calcPayment) - (extraPayment*i)
        if (iAccAmount != null && iAccAmount > owingAtEndOfPeriod) { // ie last payment
            myList.add(
                LoanPayment(
                    wDate,
                    owingAtEndOfPeriod,
                    calcPayment,
                    interestOwingForThisPeriod,
                    owingAtEndOfPeriod - interestOwingForThisPeriod,
                    0.0
                )
            )
            break
        } else {
            val tLoanPayment = LoanPayment(
                MyDate(wDate),
                iAccAmount ?: calcPayment,
                calcPayment,
                interestOwingForThisPeriod,
                if (iAccAmount != null) iAccAmount - interestOwingForThisPeriod
                else calcPayment - interestOwingForThisPeriod,
                owingAtEndOfPeriod
            )

            myList.add(tLoanPayment)
        }
        when (iPaymentRegularity) {
            LoanPaymentRegularity.WEEKLY -> wDate.increment(cPeriodWeek, 1)
            LoanPaymentRegularity.BIWEEKLY -> wDate.increment(cPeriodWeek, 2)
            LoanPaymentRegularity.MONTHLY -> wDate.increment(cPeriodMonth, 1)
        }
    }
    return myList
}
