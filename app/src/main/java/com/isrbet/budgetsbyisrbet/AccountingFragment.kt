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
const val cJOINT1_NAME = 2
const val cJOINT2_NAME = 3

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
        val totals = Array(4) {DoubleArray(4) }
        val transferTotals = Array(4) {DoubleArray(4) }
        val firstName = SpenderViewModel.getSpender(0)?.name.toString()
        val secondName = SpenderViewModel.getSpender(1)?.name.toString()

        binding.detailsHeaderNameF.text = firstName
        binding.detailsHeaderNameS.text = secondName
        binding.detailsHeaderNameJ1.text = String.format(getString(R.string.JTdash), firstName)
        binding.detailsHeaderNameJ2.text = String.format(getString(R.string.JTdash), secondName)
        binding.detailsRowNameF.text = firstName
        binding.detailsRowNameS.text = secondName
        binding.detailsRowNameJ1.text = String.format(getString(R.string.JTdash), firstName)
        binding.detailsRowNameJ2.text = String.format(getString(R.string.JTdash), secondName)

        binding.transferHeaderNameF.text = firstName
        binding.transferHeaderNameS.text = secondName
        binding.transferRowNameF.text = firstName
        binding.transferRowNameS.text = secondName
        binding.transferRowNameJ1.text = String.format(getString(R.string.JTdash), firstName)
        binding.transferRowNameJ2.text = String.format(getString(R.string.JTdash), secondName)

        for (i in 0 until TransactionViewModel.getCount()) {
            val exp = TransactionViewModel.getTransaction(i)
            if (exp.type == cTRANSACTION_TYPE_TRANSFER) {
                when (exp.boughtfor) {
                    0 -> transferTotals[cFIRST_NAME][exp.paidby] += exp.getAmountByUser(0, false)
                    1 -> transferTotals[cSECOND_NAME][exp.paidby] += exp.getAmountByUser(1, false)
                    2 -> {
                        transferTotals[cJOINT1_NAME][exp.paidby] += exp.getAmountByUser(0, false)
                        transferTotals[cJOINT2_NAME][exp.paidby] += exp.getAmountByUser(1, false)
                    }
                }
            } else {
                when (exp.boughtfor) {
                    0,1 -> {
                        if (exp.paidby == 2) {
                            totals[exp.boughtfor][exp.paidby] +=
                                (exp.getAmountByUser(exp.boughtfor, false) * SpenderViewModel.getSpenderSplit(0))
                            totals[exp.boughtfor][exp.paidby+1] +=
                                (exp.getAmountByUser(exp.boughtfor, false) * SpenderViewModel.getSpenderSplit(1))
                        } else {
                            totals[exp.boughtfor][exp.paidby] += exp.getAmountByUser(exp.boughtfor, false)
                        }
                    }
//                    1 -> totals[cSECOND_NAME][exp.paidby] += exp.getAmountByUser(1, false)
                    2 -> {
                        if (exp.paidby == 2) {
                            totals[cJOINT1_NAME][exp.paidby] += exp.getAmountByUser(0, false) * SpenderViewModel.getSpenderSplit(0)
                            totals[cJOINT2_NAME][exp.paidby] += exp.getAmountByUser(1, false) * SpenderViewModel.getSpenderSplit(0)
                            totals[cJOINT1_NAME][exp.paidby+1] += exp.getAmountByUser(0, false) * SpenderViewModel.getSpenderSplit(1)
                            totals[cJOINT2_NAME][exp.paidby+1] += exp.getAmountByUser(1, false) * SpenderViewModel.getSpenderSplit(1)
                        } else {
                            totals[cJOINT1_NAME][exp.paidby] += exp.getAmountByUser(0, false)
                            totals[cJOINT2_NAME][exp.paidby] += exp.getAmountByUser(1, false)
                        }
                    }
                }
            }
        }
        binding.detailsFF.text = gDecWithCurrency(totals[cFIRST_NAME][cFIRST_NAME])
        binding.detailsFF.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsSF.text = gDecWithCurrency(totals[cSECOND_NAME][cFIRST_NAME])
        if (totals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.detailsSF.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ1F.text = gDecWithCurrency(totals[cJOINT1_NAME][cFIRST_NAME])
        binding.detailsJ1F.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ2F.text = gDecWithCurrency(totals[cJOINT2_NAME][cFIRST_NAME])
        if (totals[cJOINT2_NAME][cFIRST_NAME] == 0.0)
            binding.detailsJ2F.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsFS.text = gDecWithCurrency(totals[cFIRST_NAME][cSECOND_NAME])
        if (totals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.detailsFS.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsSS.text = gDecWithCurrency(totals[cSECOND_NAME][cSECOND_NAME])
        binding.detailsSS.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ1S.text = gDecWithCurrency(totals[cJOINT1_NAME][cSECOND_NAME])
        if (totals[cJOINT1_NAME][cSECOND_NAME] == 0.0)
            binding.detailsJ1S.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ2S.text = gDecWithCurrency(totals[cJOINT2_NAME][cSECOND_NAME])
        binding.detailsJ2S.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsFJ1.text = gDecWithCurrency(totals[cFIRST_NAME][cJOINT1_NAME])
        if (totals[cFIRST_NAME][cJOINT1_NAME] == 0.0)
            binding.detailsFJ1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsSJ1.text = gDecWithCurrency(totals[cSECOND_NAME][cJOINT1_NAME])
        if (totals[cSECOND_NAME][cJOINT1_NAME] == 0.0)
            binding.detailsSJ1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ1J1.text = gDecWithCurrency(totals[cJOINT1_NAME][cJOINT1_NAME])
        binding.detailsJ1J1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ2J1.text = gDecWithCurrency(totals[cJOINT2_NAME][cJOINT1_NAME])
        if (totals[cJOINT2_NAME][cJOINT1_NAME] == 0.0)
            binding.detailsSJ1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsFJ2.text = gDecWithCurrency(totals[cFIRST_NAME][cJOINT2_NAME])
        if (totals[cFIRST_NAME][cJOINT2_NAME] == 0.0)
            binding.detailsSJ1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsSJ2.text = gDecWithCurrency(totals[cSECOND_NAME][cJOINT2_NAME])
        binding.detailsSJ2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ1J2.text = gDecWithCurrency(totals[cJOINT1_NAME][cJOINT2_NAME])
        if (totals[cJOINT1_NAME][cJOINT2_NAME] == 0.0)
            binding.detailsSJ1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ2J2.text = gDecWithCurrency(totals[cJOINT2_NAME][cJOINT2_NAME])
        binding.detailsJ2J2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        val jointIsAsExpected = (totals[cJOINT1_NAME][cJOINT1_NAME] + totals[cJOINT1_NAME][cJOINT2_NAME]) ==
                ((totals[cJOINT1_NAME][cJOINT1_NAME] + totals[cJOINT1_NAME][cJOINT2_NAME] +
                    totals[cJOINT2_NAME][cJOINT1_NAME] + totals[cJOINT2_NAME][cJOINT2_NAME]) * SpenderViewModel.getSpenderSplit(0))
        if (totals[cJOINT1_NAME][cJOINT1_NAME] == 0.0 || jointIsAsExpected)
            binding.detailsJ1J1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        if (totals[cJOINT2_NAME][cJOINT2_NAME] == 0.0 || jointIsAsExpected)
            binding.detailsJ2J2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ2J1.text = gDecWithCurrency(totals[cJOINT2_NAME][cJOINT1_NAME])
        if (totals[cJOINT2_NAME][cJOINT1_NAME] == 0.0 || jointIsAsExpected)
            binding.detailsJ2J1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.detailsJ1J2.text = gDecWithCurrency(totals[cJOINT1_NAME][cJOINT2_NAME])
        if (totals[cJOINT1_NAME][cJOINT2_NAME] == 0.0 || jointIsAsExpected)
            binding.detailsJ1J2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        binding.transferFF.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cFIRST_NAME])
        if (transferTotals[cFIRST_NAME][cFIRST_NAME] == 0.0)
            binding.transferFF.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferFS.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cSECOND_NAME])
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.transferFS.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferFJ.text = gDecWithCurrency(transferTotals[cFIRST_NAME][cJOINT1_NAME])
        if (transferTotals[cFIRST_NAME][cJOINT1_NAME] == 0.0)
            binding.transferFJ.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferSF.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cFIRST_NAME])
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.transferSF.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferSS.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cSECOND_NAME])
        if (transferTotals[cSECOND_NAME][cSECOND_NAME] == 0.0)
            binding.transferSS.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferSJ.text = gDecWithCurrency(transferTotals[cSECOND_NAME][cJOINT1_NAME])
        if (transferTotals[cSECOND_NAME][cJOINT1_NAME] == 0.0)
            binding.transferSJ.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ1F.text = gDecWithCurrency(transferTotals[cJOINT1_NAME][cFIRST_NAME])
        if (transferTotals[cJOINT1_NAME][cFIRST_NAME] == 0.0)
            binding.transferJ1F.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ1S.text = gDecWithCurrency(transferTotals[cJOINT1_NAME][cSECOND_NAME])
        if (transferTotals[cJOINT1_NAME][cSECOND_NAME] == 0.0)
            binding.transferJ1S.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ1J.text = gDecWithCurrency(transferTotals[cJOINT1_NAME][cJOINT1_NAME])
        if (transferTotals[cJOINT1_NAME][cJOINT1_NAME] == 0.0)
            binding.transferJ1J.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ2F.text = gDecWithCurrency(transferTotals[cJOINT2_NAME][cFIRST_NAME])
        if (transferTotals[cJOINT2_NAME][cFIRST_NAME] == 0.0)
            binding.transferJ2F.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ2S.text = gDecWithCurrency(transferTotals[cJOINT2_NAME][cSECOND_NAME])
        if (transferTotals[cJOINT2_NAME][cSECOND_NAME] == 0.0)
            binding.transferJ2S.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.transferJ2J.text = gDecWithCurrency(transferTotals[cJOINT2_NAME][cJOINT1_NAME])
        if (transferTotals[cJOINT2_NAME][cJOINT1_NAME] == 0.0)
            binding.transferJ2J.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        val oneOwesTwo = ((-totals[cFIRST_NAME][cSECOND_NAME])
                - (totals[cJOINT1_NAME][cSECOND_NAME])
                - (totals[cFIRST_NAME][cJOINT2_NAME])
                - (totals[cJOINT1_NAME][cJOINT2_NAME])
                + (totals[cSECOND_NAME][cFIRST_NAME])
                + (totals[cSECOND_NAME][cJOINT1_NAME])
                + (totals[cJOINT2_NAME][cFIRST_NAME])
                + (totals[cJOINT2_NAME][cJOINT1_NAME])
                //                + ((totals[cJOINT_NAME][cJOINT_NAME]) * SpenderViewModel.getSpenderSplit(1))
                - (transferTotals[cFIRST_NAME][cSECOND_NAME])
                + (transferTotals[cSECOND_NAME][cFIRST_NAME])
                - (transferTotals[cFIRST_NAME][cJOINT2_NAME])
                + (transferTotals[cSECOND_NAME][cJOINT1_NAME])
                + ((transferTotals[cJOINT1_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(1))
                - ((transferTotals[cJOINT1_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(0)))

        val gridLayout = binding.gridLayout
        var cellIndex = 0
        gridLayout.alignmentMode = GridLayout.ALIGN_BOUNDS
        gridLayout.columnCount = 2
        gridLayout.rowCount = 14
        var subtotal1 = 0.0

        if (totals[cSECOND_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1),
                totals[cSECOND_NAME][cFIRST_NAME], cellIndex)
            cellIndex += 2
            subtotal1 += totals[cSECOND_NAME][cFIRST_NAME]
        }
        if (totals[cJOINT2_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(0),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                totals[cJOINT2_NAME][cFIRST_NAME], cellIndex)
            cellIndex += 2
            subtotal1 += totals[cJOINT2_NAME][cFIRST_NAME]
        }
        if (totals[cSECOND_NAME][cJOINT1_NAME] != 0.0) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                SpenderViewModel.getSpenderName(1),
                totals[cSECOND_NAME][cJOINT1_NAME], cellIndex)
            cellIndex += 2
            subtotal1 += totals[cSECOND_NAME][cJOINT1_NAME]
        }
        if (totals[cJOINT2_NAME][cJOINT1_NAME] != 0.0 && !jointIsAsExpected) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                totals[cJOINT2_NAME][cJOINT1_NAME], cellIndex)
            cellIndex += 2
            subtotal1 += totals[cJOINT2_NAME][cJOINT1_NAME]
        }
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1),
                transferTotals[cSECOND_NAME][cFIRST_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal1 += transferTotals[cSECOND_NAME][cFIRST_NAME]
        }
        if (transferTotals[cSECOND_NAME][cJOINT1_NAME] != 0.0) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                SpenderViewModel.getSpenderName(1),
                transferTotals[cSECOND_NAME][cJOINT1_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal1 += transferTotals[cSECOND_NAME][cJOINT1_NAME]
        }
        if (transferTotals[cJOINT1_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(0),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                transferTotals[cJOINT1_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0),
                cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal1 += transferTotals[cJOINT1_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
            SpenderViewModel.getSpenderName(1),
            subtotal1, cellIndex, getString(R.string.sub_total))
        cellIndex += 2

        var subtotal2 = 0.0
        if (totals[cFIRST_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(1),
                SpenderViewModel.getSpenderName(0),
                totals[cFIRST_NAME][cSECOND_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cFIRST_NAME][cSECOND_NAME]
        }
        if (totals[cJOINT1_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(1),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                totals[cJOINT1_NAME][cSECOND_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT1_NAME][cSECOND_NAME]
        }
        if (totals[cFIRST_NAME][cJOINT2_NAME] != 0.0) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                SpenderViewModel.getSpenderName(0),
                totals[cFIRST_NAME][cJOINT2_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cFIRST_NAME][cJOINT2_NAME]
        }
        if (totals[cJOINT1_NAME][cJOINT2_NAME] != 0.0 && !jointIsAsExpected) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                totals[cJOINT1_NAME][cJOINT2_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT1_NAME][cJOINT2_NAME]
        }
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(1),
                SpenderViewModel.getSpenderName(0),
                transferTotals[cFIRST_NAME][cSECOND_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cFIRST_NAME][cSECOND_NAME]
        }
        if (transferTotals[cFIRST_NAME][cJOINT2_NAME] != 0.0) {
            buildGrid(gridLayout,
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(1)),
                SpenderViewModel.getSpenderName(0),
                transferTotals[cFIRST_NAME][cJOINT2_NAME], cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cFIRST_NAME][cJOINT2_NAME]
        }
        if (transferTotals[cJOINT1_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout,
                SpenderViewModel.getSpenderName(1),
                String.format(getString(R.string.s_portion), SpenderViewModel.getSpenderName(0)),
                transferTotals[cJOINT1_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1),
                cellIndex, cTRANSACTION_TYPE_TRANSFER)
            cellIndex += 2
            subtotal2 += transferTotals[cJOINT1_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
            SpenderViewModel.getSpenderName(0),
            subtotal2, cellIndex, getString(R.string.sub_total))
        cellIndex += 2

        when {
            oneOwesTwo == 0.0 -> binding.accountingSummary.text = getString(R.string.nobody_owes_anybody)
            oneOwesTwo < 0 -> {
                binding.accountingSummary.text = String.format(getString(R.string.owes), firstName,
                    secondName, gDecWithCurrency(oneOwesTwo*-1))
                binding.accountingSummary2.text = String.format(getString(R.string.owes2),
                    gDecWithCurrency(subtotal2), gDecWithCurrency(subtotal1))
            }
            else -> {
                binding.accountingSummary.text = String.format(getString(R.string.owes),
                    secondName, firstName,
                    gDecWithCurrency(oneOwesTwo))
                binding.accountingSummary2.text = String.format(getString(R.string.owes2),
                    gDecWithCurrency(subtotal1),
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
        titleText.textSize = 12f
        val amountText = TextView(context)
        amountText.textSize = 13f
        when (iTransfer) {
            cTRANSACTION_TYPE_TRANSFER -> titleText.text = String.format(getString(R.string.transferred_to), iName1, iName2)
            cTRANSACTION_TYPE_INSURANCE_REIMBURSEMENT -> titleText.text = String.format(getString(R.string.x_reimbursed_for_y), iName1, iName2)
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