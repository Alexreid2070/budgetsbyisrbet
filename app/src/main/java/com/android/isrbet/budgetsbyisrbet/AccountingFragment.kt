package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isrbet.budgetsbyisrbet.databinding.FragmentAccountingBinding
import java.text.DecimalFormat

const val cFIRSTNAME = 0
const val cSECONDNAME = 1
const val cJOINTNAME = 2

class AccountingFragment : Fragment() {
    private var _binding: FragmentAccountingBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountingBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_accounting, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillInContent()
    }

    fun fillInContent() {
        var totals = Array(3) {DoubleArray(4) {0.0} }
        var transfer_totals = Array(3) {DoubleArray(3) {0.0} }
        val firstName = SpenderViewModel.getSpender(0)?.name.toString()
        val secondName = SpenderViewModel.getSpender(1)?.name.toString()
        Log.d("Alex", "first name is " + firstName)
        Log.d("Alex", "second name is " + secondName)
        binding.accountingHeaderFheadername.setText(firstName)
        binding.accountingHeaderTFheadername.setText(firstName)
        binding.accountingFrowName.setText(firstName)
        binding.accountingTFrowName.setText(firstName)
        binding.accountingHeaderSheadername.setText(secondName)
        binding.accountingHeaderTSheadername.setText(secondName)
        binding.accountingSrowName.setText(secondName)
        binding.accountingTSrowName.setText(secondName)
        binding.accountingJfrowName.setText("Jt-"+firstName)
        binding.accountingJsrowName.setText("Jt-"+secondName)

        ExpenditureViewModel.getExpenditures().forEach {
            if (it.type == "T") {
                when (it.paidby) {
                    firstName -> {
                        when (it.boughtfor) {
                            firstName -> transfer_totals[cFIRSTNAME][cFIRSTNAME] =
                                transfer_totals[cFIRSTNAME][cFIRSTNAME] + (it.amount/100.0)
                            secondName -> transfer_totals[cFIRSTNAME][cSECONDNAME] =
                                transfer_totals[cFIRSTNAME][cSECONDNAME] + ((it.amount/100.0)*(it.bfname2split/10000.0))
                            "Joint" -> transfer_totals[cFIRSTNAME][cJOINTNAME] =
                                transfer_totals[cFIRSTNAME][cJOINTNAME] + ((it.amount/100.0)*(it.bfname2split/10000.0))
                        }
                    }
                    secondName -> {
                        when (it.boughtfor) {
                            firstName -> transfer_totals[cSECONDNAME][cFIRSTNAME] =
                                transfer_totals[cSECONDNAME][cFIRSTNAME] + ((it.amount/100.0)*(it.bfname1split/10000.0))
                            secondName -> transfer_totals[cSECONDNAME][cSECONDNAME] =
                                transfer_totals[cSECONDNAME][cSECONDNAME] + (it.amount/100.0)
                            "Joint" -> transfer_totals[cSECONDNAME][cJOINTNAME] =
                                transfer_totals[cSECONDNAME][cJOINTNAME] + ((it.amount/100.0)*(it.bfname1split/10000.0))
                        }
                    }
                    "Joint" -> {
                        when (it.boughtfor) {
                            firstName -> transfer_totals[cJOINTNAME][cFIRSTNAME] =
                                transfer_totals[cJOINTNAME][cFIRSTNAME] + ((it.amount/100.0)*(it.bfname2split/10000.0))
                            secondName -> transfer_totals[cJOINTNAME][cSECONDNAME] =
                                transfer_totals[cJOINTNAME][cSECONDNAME] + ((it.amount/100.0)*(it.bfname1split/10000.0))
                            "Joint" -> transfer_totals[cJOINTNAME][cJOINTNAME] =
                                transfer_totals[cJOINTNAME][cJOINTNAME] + (it.amount/100.0)
                        }
                    }
                }
            } else {
                when (it.paidby) {
                    firstName -> {
                        when (it.boughtfor) {
                            firstName -> totals[cFIRSTNAME][cFIRSTNAME] =
                                    totals[cFIRSTNAME][cFIRSTNAME] + (it.amount/100.0)
                            secondName -> totals[cFIRSTNAME][cSECONDNAME] =
                                totals[cFIRSTNAME][cSECONDNAME] + ((it.amount/100.0)*(it.bfname2split/10000.0))
                            "Joint" -> {
                                totals[cFIRSTNAME][cJOINTNAME] =
                                    totals[cFIRSTNAME][cJOINTNAME] + ((it.amount/100.0) * (it.bfname1split / 10000.0))
                                totals[cFIRSTNAME][cJOINTNAME+1] =
                                    totals[cFIRSTNAME][cJOINTNAME+1] + ((it.amount/100.0) * (it.bfname2split / 10000.0))
                            }
                        }
                    }
                    secondName -> {
                        when (it.boughtfor) {
                            firstName -> totals[cSECONDNAME][cFIRSTNAME] =
                                totals[cSECONDNAME][cFIRSTNAME] + + ((it.amount/100.0) * (it.bfname1split/10000.0))
                            secondName -> totals[cSECONDNAME][cSECONDNAME] =
                                totals[cSECONDNAME][cSECONDNAME] + (it.amount/100.0)
                            "Joint" -> {
                                totals[cSECONDNAME][cJOINTNAME] =
                                    totals[cSECONDNAME][cJOINTNAME] + ((it.amount/100.0) * (it.bfname1split / 10000.0))
                                totals[cSECONDNAME][cJOINTNAME+1] =
                                    totals[cSECONDNAME][cJOINTNAME+1] + ((it.amount/100.0) * (it.bfname2split / 10000.0))
                            }
                        }
                    }
                    "Joint" -> {
                        when (it.boughtfor) {
                            firstName -> totals[cJOINTNAME][cFIRSTNAME] =
                                totals[cJOINTNAME][cFIRSTNAME] + ((it.amount/100.0) * (it.bfname2split/10000.0))
                            secondName -> totals[cJOINTNAME][cSECONDNAME] =
                                totals[cJOINTNAME][cSECONDNAME] + ((it.amount/100.0) * (it.bfname1split/10000.0))
                            "Joint" -> {
                                totals[cJOINTNAME][cJOINTNAME] =
                                    totals[cJOINTNAME][cJOINTNAME] + ((it.amount/100.0) * SpenderViewModel.getSpenderSplit(0) / 100.0)
                                totals[cJOINTNAME][cJOINTNAME+1] =
                                    totals[cJOINTNAME][cJOINTNAME+1] + ((it.amount/100.0) * SpenderViewModel.getSpenderSplit(1) / 100.0)
                            }
                        }
                    }
                }
            }
        }
        val dec = DecimalFormat("#.00")
        binding.accountingFf.text = "$ " + dec.format(totals[cFIRSTNAME][cFIRSTNAME])
        binding.accountingSf.text = "$ " + dec.format(totals[cFIRSTNAME][cSECONDNAME])
        binding.accountingJff.text = "$ " + dec.format(totals[cFIRSTNAME][cJOINTNAME])
        binding.accountingJsf.text = "$ " + dec.format(totals[cFIRSTNAME][cJOINTNAME+1])
        binding.accountingFs.text = "$ " + dec.format(totals[cSECONDNAME][cFIRSTNAME])
        binding.accountingSs.text = "$ " + dec.format(totals[cSECONDNAME][cSECONDNAME])
        binding.accountingJfs.text = "$ " + dec.format(totals[cSECONDNAME][cJOINTNAME])
        binding.accountingJss.text = "$ " + dec.format(totals[cSECONDNAME][cJOINTNAME+1])
        binding.accountingFj.text = "$ " + dec.format(totals[cJOINTNAME][cFIRSTNAME])
        binding.accountingSj.text = "$ " + dec.format(totals[cJOINTNAME][cSECONDNAME])
        binding.accountingJfj.text = "$ " + dec.format(totals[cJOINTNAME][cJOINTNAME])
        binding.accountingJsj.text = "$ " + dec.format(totals[cJOINTNAME][cJOINTNAME+1])

        binding.accountingTFf.text = "$ " + dec.format(transfer_totals[cFIRSTNAME][cFIRSTNAME])
        binding.accountingTFs.text = "$ " + dec.format(transfer_totals[cFIRSTNAME][cSECONDNAME])
        binding.accountingTFj.text = "$ " + dec.format(transfer_totals[cFIRSTNAME][cJOINTNAME])
        binding.accountingTSf.text = "$ " + dec.format(transfer_totals[cSECONDNAME][cFIRSTNAME])
        binding.accountingTSs.text = "$ " + dec.format(transfer_totals[cSECONDNAME][cSECONDNAME])
        binding.accountingTSj.text = "$ " + dec.format(transfer_totals[cSECONDNAME][cJOINTNAME])
        binding.accountingTJf.text = "$ " + dec.format(transfer_totals[cJOINTNAME][cFIRSTNAME])
        binding.accountingTJs.text = "$ " + dec.format(transfer_totals[cJOINTNAME][cSECONDNAME])
        binding.accountingTJj.text = "$ " + dec.format(transfer_totals[cJOINTNAME][cJOINTNAME])

        var oneOwesTwo:Double = 0.0
        oneOwesTwo = ((-totals[cFIRSTNAME][cSECONDNAME])
                + (totals[cSECONDNAME][cFIRSTNAME])
                - (totals[cFIRSTNAME][cJOINTNAME+1])
                + (totals[cSECONDNAME][cJOINTNAME])
                + (totals[cJOINTNAME][cFIRSTNAME])
                - (totals[cJOINTNAME][cSECONDNAME])
                - (transfer_totals[cFIRSTNAME][cSECONDNAME])
                + (transfer_totals[cSECONDNAME][cFIRSTNAME])
                - (transfer_totals[cFIRSTNAME][cJOINTNAME])
                + (transfer_totals[cSECONDNAME][cJOINTNAME])
                + (transfer_totals[cJOINTNAME][cFIRSTNAME])
                - (transfer_totals[cJOINTNAME][cSECONDNAME]))

/*        oneOwesTwo = ((-totals[cFIRSTNAME][cSECONDNAME]/100.0)
                + (totals[cSECONDNAME][cFIRSTNAME]/100.0)
                - (totals[cFIRSTNAME][cJOINTNAME]/100 * SpenderViewModel.getSpenderSplit(1) /100)
                + (totals[cSECONDNAME][cJOINTNAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                + (totals[cJOINTNAME][cFIRSTNAME]/100 * SpenderViewModel.getSpenderSplit(1) / 100)
                - (totals[cJOINTNAME][cSECONDNAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                - (transfer_totals[cFIRSTNAME][cSECONDNAME]/100.0)
                + (transfer_totals[cSECONDNAME][cFIRSTNAME]/100.0)
                - (transfer_totals[cFIRSTNAME][cJOINTNAME]/100 * SpenderViewModel.getSpenderSplit(1) /100)
                + (transfer_totals[cSECONDNAME][cJOINTNAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                + (transfer_totals[cJOINTNAME][cFIRSTNAME]/100 * SpenderViewModel.getSpenderSplit(1) / 100)
                - (transfer_totals[cJOINTNAME][cSECONDNAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100))
*/
        Log.d("Alex", "oneowestwo is " + oneOwesTwo.toString())
        if (oneOwesTwo == 0.0)
            binding.accountingSummary.text = "Nobody owes anyone!"
        else if (oneOwesTwo > 0)
            binding.accountingSummary.text = firstName + " owes " + secondName + " $ " + dec.format(oneOwesTwo)
        else
            binding.accountingSummary.text = secondName + " owes " + firstName + " $ " + dec.format(oneOwesTwo*-1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}