package com.isrbet.budgetsbyisrbet

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.isrbet.budgetsbyisrbet.databinding.FragmentLoanBinding
import java.util.*
import kotlin.math.pow
import kotlin.math.round

class LoanFragment : Fragment() {
    private var _binding: FragmentLoanBinding? = null
    private val binding get() = _binding!!
    private val args: LoanFragmentArgs by navArgs()
    private var cal = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoanBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_loan, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        Log.d("Alex", "Came to Loan fragment with arg ${args.loanID}")
        if (args.loanID != "") {
            setupLoanSpinner(args.loanID)
            val rt = RecurringTransactionViewModel.getRecurringTransaction(args.loanID)
            if (rt != null) {
                cal.set(rt.loanFirstPaymentDate.substring(0,4).toInt(), rt.loanFirstPaymentDate.substring(5,7).toInt()-1, rt.loanFirstPaymentDate.substring(8,10).toInt())
                loadRows(
                    cal,
                    rt.loanAmortization / 100.0,
                    rt.loanPaymentRegularity,
                    rt.loanInterestRate / 10000.0,
                    rt.loanAmount / 100.0,
                rt.loanAcceleratedPaymentAmount / 100.0)
            }
        } else
            setupLoanSpinner()
        binding.loanStartDate.setText(giveMeMyDateFormat(cal))
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(java.util.Calendar.YEAR, year)
                cal.set(java.util.Calendar.MONTH, monthOfYear)
                cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.loanStartDate.setText(giveMeMyDateFormat(cal))
            }

        binding.loanStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.LoanSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                if (position > 0) {
                    val rt = RecurringTransactionViewModel.getRecurringTransaction(selection as String)
                    if (rt != null) {
                        binding.loanStartDate.setText(rt.loanFirstPaymentDate)
                        cal.set(rt.loanFirstPaymentDate.substring(0,4).toInt(), rt.loanFirstPaymentDate.substring(5,7).toInt()-1, rt.loanFirstPaymentDate.substring(8,10).toInt())
                        binding.loanAmount.setText((rt.loanAmount / 100.0).toString())
                        binding.amortizationPeriod.setText((rt.loanAmortization / 100.0).toString())
                        binding.interestRate.setText((rt.loanInterestRate / 100.0).toString())
                        binding.acceleratedPaymentAmount.setText((rt.loanAcceleratedPaymentAmount / 100.0).toString())
                        when (rt.loanPaymentRegularity) {
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
        HintViewModel.showHint(requireContext(), binding.loanSpinnerLabel, "Loan")
    }

    private fun reset() {
        binding.LoanSpinner.setSelection(0)
        val today = Calendar.getInstance()
        binding.loanStartDate.setText(giveMeMyDateFormat(today))
        binding.loanAmount.setText("")
        binding.amortizationPeriod.setText("")
        binding.interestRate.setText("")
        binding.calculatedPaymentAmount.text = ""
        binding.acceleratedPaymentAmount.setText("")
        val myList: MutableList<LoanPayment> = ArrayList()
        val adapter = LoanAdapter(requireContext(), myList)
        val listView: ListView = requireActivity().findViewById(R.id.loan_list_view)
        listView.adapter = adapter
    }

    private fun setupLoanSpinner(iSelection: String = "") {
        Log.d("Alex", "Setting up loan spinner with $iSelection")
        val listOfActiveLoans = RecurringTransactionViewModel.getActiveLoanRTs()
        if (listOfActiveLoans.size > 0) {
            listOfActiveLoans.add(0,"<Choose existing loan>")
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
            binding.loanAmount.error=getString(R.string.missingAmountError)
            focusAndOpenSoftKeyboard(requireContext(), binding.loanAmount)
            return
        }
        if (binding.amortizationPeriod.text.toString() == "") {
            binding.amortizationPeriod.error="Amortization period (in years) is missing."
            focusAndOpenSoftKeyboard(requireContext(), binding.amortizationPeriod)
            return
        }
        if (binding.interestRate.text.toString() == "") {
            binding.interestRate.error="Interest rate is missing."
            focusAndOpenSoftKeyboard(requireContext(), binding.interestRate)
            return
        }
        val selectedId = binding.paymentFrequencyGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        val freq = when (radioButton.text) {
            "Weekly" -> LoanPaymentRegularity.WEEKLY
            "Biweekly" -> LoanPaymentRegularity.BIWEEKLY
            else -> LoanPaymentRegularity.MONTHLY
        }

        val dt = binding.loanStartDate.text.toString()
        cal.set(dt.substring(0,4).toInt(), dt.substring(5,7).toInt()-1, dt.substring(8,10).toInt())

        val accPayment =
            if (binding.acceleratedPaymentAmount.text.toString() == "") 0.0 else binding.acceleratedPaymentAmount.text.toString().toDouble()
        loadRows(cal,
            binding.amortizationPeriod.text.toString().toDouble(),
            freq,
            binding.interestRate.text.toString().toDouble()/100.0,
            binding.loanAmount.text.toString().toDouble(),
            accPayment)
    }

    private fun loadRows(iFirstPaymentDate: Calendar, iAmortizationYears: Double,
                            iPaymentRegularity: LoanPaymentRegularity,
                            iInterestRate: Double, iPrincipal: Double,
                            iAcceleratedPayment: Double) {
        Log.d("Alex", "$iPrincipal $iAmortizationYears $iInterestRate $iPaymentRegularity")
        val myList: MutableList<LoanPayment> = ArrayList()
        val iPaymentsPerYear = when (iPaymentRegularity) {
            LoanPaymentRegularity.WEEKLY -> 52
            LoanPaymentRegularity.BIWEEKLY -> 26
            LoanPaymentRegularity.MONTHLY -> 12
        }

        val calcPayment =  iPrincipal * (iInterestRate / iPaymentsPerYear) * (1 + iInterestRate / iPaymentsPerYear).pow(iPaymentsPerYear * iAmortizationYears) /
                ((1 + iInterestRate / iPaymentsPerYear).pow(iPaymentsPerYear*iAmortizationYears) - 1)
        binding.calculatedPaymentAmount.text = gDec.format(calcPayment)
        Log.d("Alex", "payment is $calcPayment")
        var owingAtEndOfPeriod = iPrincipal
        val extraPayment = if (iAcceleratedPayment == 0.0) 0.0 else iAcceleratedPayment - calcPayment
        Log.d("Alex", "Extra payment is $extraPayment")
        for (i in 1..(round(iAmortizationYears * iPaymentsPerYear).toInt())) {
            val interestOwingForThisPeriod = owingAtEndOfPeriod * iInterestRate / iPaymentsPerYear
            owingAtEndOfPeriod = (1 + iInterestRate/iPaymentsPerYear).pow(i)*iPrincipal - (((1+iInterestRate/iPaymentsPerYear).pow(i) - 1)/(iInterestRate/iPaymentsPerYear)*calcPayment) - (extraPayment*i)
            if (iAcceleratedPayment > owingAtEndOfPeriod) { // ie last payment
                myList.add(
                    LoanPayment(
                        iFirstPaymentDate.clone() as Calendar,
                        owingAtEndOfPeriod,
                        interestOwingForThisPeriod,
                        owingAtEndOfPeriod - interestOwingForThisPeriod,
                        0.0
                    )
                )
                break
            } else
                myList.add(LoanPayment(iFirstPaymentDate.clone() as Calendar,
                    if (iAcceleratedPayment != 0.0) iAcceleratedPayment else calcPayment,
                    interestOwingForThisPeriod,
                    if (iAcceleratedPayment != 0.0) iAcceleratedPayment-interestOwingForThisPeriod
                    else calcPayment-interestOwingForThisPeriod,
                    owingAtEndOfPeriod))
            when (iPaymentRegularity) {
                LoanPaymentRegularity.WEEKLY -> iFirstPaymentDate.add(Calendar.DATE, 7)
                LoanPaymentRegularity.BIWEEKLY -> iFirstPaymentDate.add(Calendar.DATE, 14)
                LoanPaymentRegularity.MONTHLY -> iFirstPaymentDate.add(Calendar.MONTH, 1)
            }
        }
        val adapter = LoanAdapter(requireContext(), myList)
        val listView: ListView = requireActivity().findViewById(R.id.loan_list_view)
        listView.adapter = adapter
        goToCorrectRow()
    }

    private fun goToCorrectRow() {
        val listView: ListView = requireActivity().findViewById(R.id.loan_list_view)
        var row = 0
        val dateNow = Calendar.getInstance()
        for (i in 0 until listView.adapter.count) {
            val loanPayment: LoanPayment = listView.adapter.getItem(i) as LoanPayment
            if (loanPayment.paymentDate >= dateNow) {
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