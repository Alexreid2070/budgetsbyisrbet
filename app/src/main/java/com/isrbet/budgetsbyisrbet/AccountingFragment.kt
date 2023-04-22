package com.isrbet.budgetsbyisrbet

import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.GridLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentAccountingBinding

const val cFIRST_NAME = 0
const val cSECOND_NAME = 1
const val cJOINT_NAME = 2

class AccountingFragment : Fragment() {
    private var _binding: FragmentAccountingBinding? = null
    private val binding get() = _binding!!

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val inflater = TransitionInflater.from(requireContext())
//        enterTransition = inflater.inflateTransition(R.transition.slide_right)
//        returnTransition = null
//        exitTransition = inflater.inflateTransition(R.transition.slide_left)
//    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountingBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_accounting, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillInContent()
        binding.transferAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransferFragment)
        }
        binding.accountingSummary.setOnClickListener {
            val action =
                AccountingFragmentDirections.actionAccountingFragmentToTransactionViewAllFragment()
            action.filterMode = cACCOUNTING_FILTER
            findNavController().navigate(action)
            MyApplication.displayToast(getString(R.string.these_are_the_transactions))
        }
        binding.summarySection.setOnClickListener {
            val action =
                AccountingFragmentDirections.actionAccountingFragmentToTransactionViewAllFragment()
            action.filterMode = cACCOUNTING_FILTER
            findNavController().navigate(action)
            MyApplication.displayToast(getString(R.string.these_are_the_transactions))
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.transfer_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
        HintViewModel.showHint(parentFragmentManager, cHINT_ACCOUNTING)
    }

    private fun fillInContent() {
        val totals = Array(3) {DoubleArray(4) {0.0} }
        val transferTotals = Array(3) {DoubleArray(4) {0.0} }
        val firstName = SpenderViewModel.getSpender(0)?.name.toString()
        val secondName = SpenderViewModel.getSpender(1)?.name.toString()
        binding.accountingHeaderFirstHeaderName.text = firstName
        binding.accountingHeaderSecondHeaderName.text = secondName
        binding.accountingHeaderTFheadername.text = firstName
        binding.accountingHeaderTSheadername.text = secondName
        binding.accountingFirstRowName.text = firstName
        binding.accountingSecondRowName.text = secondName
        binding.accountingTTofRowName.text = firstName
        binding.accountingTTosRowName.text = secondName
        binding.accountingJfrowName.text = String.format(getString(R.string.JTdash), firstName)
        binding.accountingJsrowName.text = String.format(getString(R.string.JTdash), secondName)
        binding.accountingTToj1RowName.text = String.format(getString(R.string.JTdash), firstName)
        binding.accountingTToj2RowName.text = String.format(getString(R.string.JTdash), secondName)

        for (i in 0 until TransactionViewModel.getCount()) {
            val exp = TransactionViewModel.getTransaction(i)
            if (exp.type == cTRANSACTION_TYPE_TRANSFER) {
                when (exp.paidby) {
                    0 -> {
                        when (exp.boughtfor) {
                            0 -> transferTotals[cFIRST_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> transferTotals[cFIRST_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                transferTotals[cFIRST_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                transferTotals[cFIRST_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                    1 -> {
                        when (exp.boughtfor) {
                            0 -> transferTotals[cSECOND_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> transferTotals[cSECOND_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                transferTotals[cSECOND_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                transferTotals[cSECOND_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                    2 -> {
                        when (exp.boughtfor) {
                            0 -> transferTotals[cJOINT_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> transferTotals[cJOINT_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                transferTotals[cJOINT_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                transferTotals[cJOINT_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                }
            } else {
                when (exp.paidby) {
                    0 -> {
                        when (exp.boughtfor) {
                            0 -> totals[cFIRST_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> totals[cFIRST_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                totals[cFIRST_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                totals[cFIRST_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                    1 -> {
                        when (exp.boughtfor) {
                            0 -> totals[cSECOND_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> totals[cSECOND_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                totals[cSECOND_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                totals[cSECOND_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                    2 -> {
                        when (exp.boughtfor) {
                            0 -> totals[cJOINT_NAME][cFIRST_NAME] += exp.getAmountByUser(0, false)
                            1 -> totals[cJOINT_NAME][cSECOND_NAME] += exp.getAmountByUser(1, false)
                            2 -> {
                                totals[cJOINT_NAME][cJOINT_NAME] += exp.getAmountByUser(0, false)
                                totals[cJOINT_NAME][cJOINT_NAME+1] += exp.getAmountByUser(1, false)
                            }
                        }
                    }
                }
            }
        }
        binding.accountingFf.text = gDecWithCurrency(totals[cFIRST_NAME][cFIRST_NAME])
        binding.accountingFf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSf.text = gDecWithCurrency(totals[cFIRST_NAME][cSECOND_NAME])
        if (totals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.accountingSf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJff.text = gDecWithCurrency(totals[cFIRST_NAME][cJOINT_NAME])
        binding.accountingJff.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJsf.text = gDecWithCurrency(totals[cFIRST_NAME][cJOINT_NAME+1])
        if (totals[cFIRST_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingJsf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingFs.text = gDecWithCurrency(totals[cSECOND_NAME][cFIRST_NAME])
        if (totals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.accountingFs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSs.text = gDecWithCurrency(totals[cSECOND_NAME][cSECOND_NAME])
        binding.accountingSs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJfs.text = gDecWithCurrency(totals[cSECOND_NAME][cJOINT_NAME])
        if (totals[cSECOND_NAME][cJOINT_NAME] == 0.0)
            binding.accountingJfs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJss.text = gDecWithCurrency(totals[cSECOND_NAME][cJOINT_NAME+1])
        binding.accountingJss.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingFj.text = gDecWithCurrency(totals[cJOINT_NAME][cFIRST_NAME])
        if (totals[cJOINT_NAME][cFIRST_NAME] == 0.0)
            binding.accountingFj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSj.text = gDecWithCurrency(totals[cJOINT_NAME][cSECOND_NAME])
        if (totals[cJOINT_NAME][cSECOND_NAME] == 0.0)
            binding.accountingSj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJfj.text = gDecWithCurrency(totals[cJOINT_NAME][cJOINT_NAME])

        val jointIsAsExpected = totals[cJOINT_NAME][cJOINT_NAME] == ((totals[cJOINT_NAME][cJOINT_NAME] +
                totals[cJOINT_NAME][cJOINT_NAME+1]) * SpenderViewModel.getSpenderSplit(0))
        if (totals[cJOINT_NAME][cJOINT_NAME] == 0.0 || jointIsAsExpected)
            binding.accountingJfj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJsj.text = gDecWithCurrency(totals[cJOINT_NAME][cJOINT_NAME+1])
        if (totals[cJOINT_NAME][cJOINT_NAME+1] == 0.0 || jointIsAsExpected)
            binding.accountingJsj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        binding.accountingTFf.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cFIRST_NAME])
        if (transferTotals[cFIRST_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTFf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFs.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cSECOND_NAME])
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTFs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFj1.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cJOINT_NAME])
        if (transferTotals[cFIRST_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTFj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFj2.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cJOINT_NAME+1])
        if (transferTotals[cFIRST_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTFj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSf.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cFIRST_NAME])
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTSf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSs.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cSECOND_NAME])
        if (transferTotals[cSECOND_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTSs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSj1.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cJOINT_NAME])
        if (transferTotals[cSECOND_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTSj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSj2.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cJOINT_NAME+1])
        if (transferTotals[cSECOND_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTSj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJf.text = gDecWithCurrency(transferTotals[cJOINT_NAME][cFIRST_NAME])
        if (transferTotals[cJOINT_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTJf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJs.text = gDecWithCurrency(transferTotals[cJOINT_NAME][cSECOND_NAME])
        if (transferTotals[cJOINT_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTJs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJj1.text = gDecWithCurrency(transferTotals[cJOINT_NAME][cJOINT_NAME])
        if (transferTotals[cJOINT_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTJj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJj2.text = gDecWithCurrency(transferTotals[cJOINT_NAME][cJOINT_NAME+1])
        if (transferTotals[cJOINT_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTJj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        val oneOwesTwo = ((-totals[cFIRST_NAME][cSECOND_NAME])
                + (totals[cSECOND_NAME][cFIRST_NAME])
                - (totals[cFIRST_NAME][cJOINT_NAME+1])
                + (totals[cSECOND_NAME][cJOINT_NAME])
                + ((totals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0))
                - ((totals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1))
                + ((totals[cJOINT_NAME][cJOINT_NAME]) * SpenderViewModel.getSpenderSplit(0))
                - ((totals[cJOINT_NAME][cJOINT_NAME+1]) * SpenderViewModel.getSpenderSplit(1))
                - (transferTotals[cFIRST_NAME][cSECOND_NAME])
                + (transferTotals[cSECOND_NAME][cFIRST_NAME])
                - (transferTotals[cFIRST_NAME][cJOINT_NAME+1])
                + (transferTotals[cSECOND_NAME][cJOINT_NAME])
                + ((transferTotals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0))
                - ((transferTotals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1)))

        val gridLayout = binding.gridLayout
        var cellIndex = 0
        gridLayout.alignmentMode = GridLayout.ALIGN_BOUNDS
        gridLayout.columnCount = 2
        gridLayout.rowCount = 14
        var subtotal = 0.0

        if (totals[cFIRST_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1),
                totals[cFIRST_NAME][cSECOND_NAME], cellIndex)
            cellIndex += 2
            subtotal += totals[cFIRST_NAME][cSECOND_NAME]
        }
        if (totals[cFIRST_NAME][cJOINT_NAME+1] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                totals[cFIRST_NAME][cJOINT_NAME+1], cellIndex)
            cellIndex += 2
            subtotal += totals[cFIRST_NAME][cJOINT_NAME+1]
        }
        if (totals[cJOINT_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                SpenderViewModel.getSpenderName(1),
                totals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1), cellIndex)
            cellIndex += 2
            subtotal += totals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)
        }
        if (totals[cJOINT_NAME][cJOINT_NAME+1] != 0.0 && !jointIsAsExpected) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                getString(R.string.joint) + " (" + SpenderViewModel.getSpenderName(1) + ")",
                totals[cJOINT_NAME][cJOINT_NAME+1] * SpenderViewModel.getSpenderSplit(1), cellIndex)
            cellIndex += 2
            subtotal += totals[cJOINT_NAME][cJOINT_NAME+1] * SpenderViewModel.getSpenderSplit(1)
        }
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1),
                transferTotals[cFIRST_NAME][cSECOND_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal += transferTotals[cFIRST_NAME][cSECOND_NAME]
        }
        if (transferTotals[cFIRST_NAME][cJOINT_NAME+1] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                transferTotals[cFIRST_NAME][cJOINT_NAME+1], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal += transferTotals[cFIRST_NAME][cJOINT_NAME+1]
        }
        if (transferTotals[cJOINT_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                SpenderViewModel.getSpenderName(1),
                transferTotals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1),
                cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal += transferTotals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
            SpenderViewModel.getSpenderName(1),
            subtotal, cellIndex, getString(R.string.sub_total))
        cellIndex += 2

        var subtotal2 = 0.0
        if (totals[cSECOND_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                SpenderViewModel.getSpenderName(0),
                totals[cSECOND_NAME][cFIRST_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cSECOND_NAME][cFIRST_NAME]
        }
        if (totals[cSECOND_NAME][cJOINT_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                totals[cSECOND_NAME][cJOINT_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cSECOND_NAME][cJOINT_NAME]
        }
        if (totals[cJOINT_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                SpenderViewModel.getSpenderName(0),
                totals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0), cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)
        }
        if (totals[cJOINT_NAME][cJOINT_NAME] != 0.0 && !jointIsAsExpected) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                getString(R.string.joint) + " ("+SpenderViewModel.getSpenderName(0)+")",
                totals[cJOINT_NAME][cJOINT_NAME] * SpenderViewModel.getSpenderSplit(0), cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT_NAME][cJOINT_NAME] * SpenderViewModel.getSpenderSplit(0)
        }
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                SpenderViewModel.getSpenderName(0),
                transferTotals[cSECOND_NAME][cFIRST_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cSECOND_NAME][cFIRST_NAME]
        }
        if (transferTotals[cSECOND_NAME][cJOINT_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                transferTotals[cSECOND_NAME][cJOINT_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cSECOND_NAME][cJOINT_NAME]
        }
        if (transferTotals[cJOINT_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                SpenderViewModel.getSpenderName(0),
                transferTotals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0),
                cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
            SpenderViewModel.getSpenderName(0),
            subtotal2, cellIndex, getString(R.string.sub_total))
        cellIndex += 2

        when {
            oneOwesTwo == 0.0 -> binding.accountingSummary.text = getString(R.string.nobody_owes_anybody)
            oneOwesTwo > 0 -> {
                binding.accountingSummary.text = String.format(getString(R.string.owes), firstName,
                    secondName, gDecWithCurrency(oneOwesTwo))
                binding.accountingSummary2.text = String.format(getString(R.string.owes2),
                    gDecWithCurrency(subtotal2), gDecWithCurrency(subtotal))
            }
            else -> {
                binding.accountingSummary.text = String.format(getString(R.string.owes),
                    secondName, firstName,
                    gDecWithCurrency(oneOwesTwo*-1))
                binding.accountingSummary2.text = String.format(getString(R.string.owes2),
                    gDecWithCurrency(subtotal),
                    gDecWithCurrency(subtotal2))
            }
        }

    }

    private fun buildGrid(iGridLayout: GridLayout, iName1: String, iName2: String, iAmount: Double, iCellIndex: Int, iTransfer: String = "") {
        val paramsT: GridLayout.LayoutParams = GridLayout.LayoutParams()
        paramsT.rowSpec = GridLayout.spec(iCellIndex / 2, GridLayout.CENTER)
        paramsT.columnSpec = GridLayout.spec(iCellIndex % 2, GridLayout.RIGHT)
        val paramsA: GridLayout.LayoutParams = GridLayout.LayoutParams()
        paramsA.rowSpec = GridLayout.spec(iCellIndex / 2, GridLayout.CENTER)
        paramsA.columnSpec = GridLayout.spec(iCellIndex % 2 + 1, GridLayout.RIGHT)
        val titleText = TextView(context)
        val amountText = TextView(context)
        when (iTransfer) {
            cTRANSACTION_TYPE_TRANSFER -> titleText.text = String.format(getString(R.string.transferred_to), iName1, iName2)
            getString(R.string.sub_total) -> {
                paramsT.topMargin = 10
                paramsT.bottomMargin = 40
                titleText.setTypeface(null, Typeface.BOLD)
                amountText.setTypeface(null, Typeface.BOLD)
                titleText.text = String.format(getString(R.string.total_funds_used_for), iName1, iName2)
            }
            else -> titleText.text = String.format(getString(R.string.paid_for), iName1, iName2)
        }
        titleText.layoutParams = paramsT
        iGridLayout.addView(titleText,0)
        if (iTransfer == getString(R.string.sub_total))
            amountText.text = gDecWithCurrency(iAmount)
        else
            amountText.text = gDecWithCurrency(iAmount)
        amountText.layoutParams = paramsA
        iGridLayout.addView(amountText,1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}