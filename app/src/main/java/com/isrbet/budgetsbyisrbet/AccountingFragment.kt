package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.isrbet.budgetsbyisrbet.databinding.FragmentAccountingBinding
import java.text.DecimalFormat

const val cFIRST_NAME = 0
const val cSECOND_NAME = 1
const val cJOINT_NAME = 2

class AccountingFragment : Fragment() {
    private var _binding: FragmentAccountingBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountingBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_accounting, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillInContent()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }
    }
    @SuppressLint("SetTextI18n")
    fun fillInContent() {
        val totals = Array(3) {DoubleArray(4) {0.0} }
        val transferTotals = Array(3) {DoubleArray(4) {0.0} }
        val firstName = SpenderViewModel.getSpender(0, true)?.name.toString()
        val secondName = SpenderViewModel.getSpender(1, true)?.name.toString()
        Log.d("Alex", "first name is $firstName")
        Log.d("Alex", "second name is $secondName")
        binding.accountingHeaderFirstHeaderName.text = firstName
        binding.accountingHeaderTFheadername.text = secondName
        binding.accountingHeaderTSheadername.text = secondName
        binding.accountingSecondRowName.text = secondName
        binding.accountingTTosRowName.text = secondName
        binding.accountingJfrowName.text = "Jt-$firstName"
        binding.accountingJsrowName.text = "Jt-$secondName"
        binding.accountingTToj1RowName.text = "Jt-$firstName"
        binding.accountingTToj2RowName.text = "Jt-$secondName"

        ExpenditureViewModel.getExpenditures().forEach {
            if (it.type == "T") {
                when (it.paidby) {
                    firstName -> {
                        when (it.boughtfor) {
                            firstName -> transferTotals[cFIRST_NAME][cFIRST_NAME] += (it.amount/100.0)
                            secondName -> transferTotals[cFIRST_NAME][cSECOND_NAME] += (it.amount/100.0)
                            "Joint" -> {
                                transferTotals[cFIRST_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                transferTotals[cFIRST_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split / 100.0))
                            }
                        }
                    }
                    secondName -> {
                        when (it.boughtfor) {
                            firstName -> transferTotals[cSECOND_NAME][cFIRST_NAME] += (it.amount/100.0)
                            secondName -> transferTotals[cSECOND_NAME][cSECOND_NAME] += (it.amount/100.0)
                            "Joint" -> {
                                transferTotals[cSECOND_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                transferTotals[cSECOND_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split / 100.0))
                            }
                        }
                    }
                    "Joint" -> {
                        when (it.boughtfor) {
                            firstName -> transferTotals[cJOINT_NAME][cFIRST_NAME] += (it.amount/100.0)
                            secondName -> transferTotals[cJOINT_NAME][cSECOND_NAME] += (it.amount/100.0)
                            "Joint" -> {
                                transferTotals[cJOINT_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                transferTotals[cJOINT_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split/100.0))
                            }
                        }
                    }
                }
            } else {
                when (it.paidby) {
                    firstName -> {
                        when (it.boughtfor) {
                            firstName -> totals[cFIRST_NAME][cFIRST_NAME] += (it.amount/100.0)
                            secondName -> totals[cFIRST_NAME][cSECOND_NAME] += (it.amount/100.0)
                            "Joint" -> {
                                totals[cFIRST_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                totals[cFIRST_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split / 100.0))
                            }
                        }
                    }
                    secondName -> {
                        when (it.boughtfor) {
                            firstName -> totals[cSECOND_NAME][cFIRST_NAME] += (it.amount/100.0)
                            secondName -> totals[cSECOND_NAME][cSECOND_NAME] += (it.amount/100.0)
                            "Joint" -> {
                                totals[cSECOND_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                totals[cSECOND_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split / 100.0))
                            }
                        }
                    }
                    "Joint" -> {
                        when (it.boughtfor) {
                            firstName -> {
                                totals[cJOINT_NAME][cFIRST_NAME] += ((it.amount / 100.0) * (it.bfname1split / 100.0))
                            }
                            secondName -> totals[cJOINT_NAME][cSECOND_NAME] += ((it.amount/100.0) * (it.bfname2split/100.0))
                            "Joint" -> {
                                totals[cJOINT_NAME][cJOINT_NAME] += ((it.amount/100.0) * (it.bfname1split / 100.0))
                                totals[cJOINT_NAME][cJOINT_NAME+1] += ((it.amount/100.0) * (it.bfname2split/100.0))
                            }
                        }
                    }
                }
            }
        }
        val dec = DecimalFormat("#.00")
        binding.accountingFf.text = "$ " + dec.format(totals[cFIRST_NAME][cFIRST_NAME])
        binding.accountingSf.text = "$ " + dec.format(totals[cFIRST_NAME][cSECOND_NAME])
        binding.accountingJff.text = "$ " + dec.format(totals[cFIRST_NAME][cJOINT_NAME])
        binding.accountingJsf.text = "$ " + dec.format(totals[cFIRST_NAME][cJOINT_NAME+1])
        binding.accountingFs.text = "$ " + dec.format(totals[cSECOND_NAME][cFIRST_NAME])
        binding.accountingSs.text = "$ " + dec.format(totals[cSECOND_NAME][cSECOND_NAME])
        binding.accountingJfs.text = "$ " + dec.format(totals[cSECOND_NAME][cJOINT_NAME])
        binding.accountingJss.text = "$ " + dec.format(totals[cSECOND_NAME][cJOINT_NAME+1])
        binding.accountingFj.text = "$ " + dec.format(totals[cJOINT_NAME][cFIRST_NAME])
        binding.accountingSj.text = "$ " + dec.format(totals[cJOINT_NAME][cSECOND_NAME])
        binding.accountingJfj.text = "$ " + dec.format(totals[cJOINT_NAME][cJOINT_NAME])
        binding.accountingJsj.text = "$ " + dec.format(totals[cJOINT_NAME][cJOINT_NAME+1])

        binding.accountingTFf.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cFIRST_NAME])
        binding.accountingTFs.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cSECOND_NAME])
        binding.accountingTFj1.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cJOINT_NAME])
        binding.accountingTFj2.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cJOINT_NAME+1])
        binding.accountingTSf.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cFIRST_NAME])
        binding.accountingTSs.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cSECOND_NAME])
        binding.accountingTSj1.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cJOINT_NAME])
        binding.accountingTSj2.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cJOINT_NAME+1])
        binding.accountingTJf.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cFIRST_NAME])
        binding.accountingTJs.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cSECOND_NAME])
        binding.accountingTJj1.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cJOINT_NAME])
        binding.accountingTJj2.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cJOINT_NAME+1])

        val oneOwesTwo = ((-totals[cFIRST_NAME][cSECOND_NAME])
                + (totals[cSECOND_NAME][cFIRST_NAME])
                - (totals[cFIRST_NAME][cJOINT_NAME+1])
                + (totals[cSECOND_NAME][cJOINT_NAME])
                + ((totals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0)/100)
                - ((totals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1)/100)
                - (transferTotals[cFIRST_NAME][cSECOND_NAME])
                + (transferTotals[cSECOND_NAME][cFIRST_NAME])
                - (transferTotals[cFIRST_NAME][cJOINT_NAME+1])
                + (transferTotals[cSECOND_NAME][cJOINT_NAME])
                + ((transferTotals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0)/100)
                - ((transferTotals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1)/100))

/*        oneOwesTwo = ((-totals[cFIRST_NAME][cSECOND_NAME]/100.0)
                + (totals[cSECOND_NAME][cFIRST_NAME]/100.0)
                - (totals[cFIRST_NAME][cJOINT_NAME]/100 * SpenderViewModel.getSpenderSplit(1) /100)
                + (totals[cSECOND_NAME][cJOINT_NAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                + (totals[cJOINT_NAME][cFIRST_NAME]/100 * SpenderViewModel.getSpenderSplit(1) / 100)
                - (totals[cJOINT_NAME][cSECOND_NAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                - (transfer_totals[cFIRST_NAME][cSECOND_NAME]/100.0)
                + (transfer_totals[cSECOND_NAME][cFIRST_NAME]/100.0)
                - (transfer_totals[cFIRST_NAME][cJOINT_NAME]/100 * SpenderViewModel.getSpenderSplit(1) /100)
                + (transfer_totals[cSECOND_NAME][cJOINT_NAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100)
                + (transfer_totals[cJOINT_NAME][cFIRST_NAME]/100 * SpenderViewModel.getSpenderSplit(1) / 100)
                - (transfer_totals[cJOINT_NAME][cSECOND_NAME]/100 * SpenderViewModel.getSpenderSplit(0) / 100))
*/
        Log.d("Alex", "one owes two is $oneOwesTwo")
        when {
            oneOwesTwo == 0.0 -> binding.accountingSummary.text = "Nobody owes anyone!"
            oneOwesTwo > 0 -> binding.accountingSummary.text = firstName + " owes " + secondName + " $ " + dec.format(oneOwesTwo)
            else -> binding.accountingSummary.text = secondName + " owes " + firstName + " $ " + dec.format(oneOwesTwo*-1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}