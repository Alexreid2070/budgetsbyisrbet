package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.GridLayout
import android.widget.GridLayout.Spec
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentAccountingBinding
import java.text.DecimalFormat


const val cFIRST_NAME = 0
const val cSECOND_NAME = 1
const val cJOINT_NAME = 2

class AccountingFragment : Fragment() {
    private var _binding: FragmentAccountingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val inflater = TransitionInflater.from(requireContext())
//        enterTransition = inflater.inflateTransition(R.transition.slide_right)
//        returnTransition = null
//        exitTransition = inflater.inflateTransition(R.transition.slide_left)
    }
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
        binding.transferAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransferFragment)
        }
        binding.summaryLinearLayout.setOnClickListener {
            val action =
                AccountingFragmentDirections.actionAccountingFragmentToTransactionViewAllFragment()
            action.accountingFlag = "Accounting"
            findNavController().navigate(action)
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.transfer_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
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
        binding.accountingHeaderFirstHeaderName.text = firstName
        binding.accountingHeaderSecondHeaderName.text = secondName
        binding.accountingHeaderTFheadername.text = firstName
        binding.accountingHeaderTSheadername.text = secondName
        binding.accountingFirstRowName.text = firstName
        binding.accountingSecondRowName.text = secondName
        binding.accountingTTofRowName.text = firstName
        binding.accountingTTosRowName.text = secondName
        binding.accountingJfrowName.text = "Jt-$firstName"
        binding.accountingJsrowName.text = "Jt-$secondName"
        binding.accountingTToj1RowName.text = "Jt-$firstName"
        binding.accountingTToj2RowName.text = "Jt-$secondName"

        for (i in 0 until ExpenditureViewModel.getCount()) {
            val exp = ExpenditureViewModel.getExpenditure(i)
            if (exp.type == "Transfer") {
                when (exp.paidby) {
                    firstName -> {
                        when (exp.boughtfor) {
                            firstName -> transferTotals[cFIRST_NAME][cFIRST_NAME] += (exp.amount/100.0)
                            secondName -> transferTotals[cFIRST_NAME][cSECOND_NAME] += (exp.amount/100.0)
                            "Joint" -> {
                                transferTotals[cFIRST_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                transferTotals[cFIRST_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split / 100.0))
                            }
                        }
                    }
                    secondName -> {
                        when (exp.boughtfor) {
                            firstName -> transferTotals[cSECOND_NAME][cFIRST_NAME] += (exp.amount/100.0)
                            secondName -> transferTotals[cSECOND_NAME][cSECOND_NAME] += (exp.amount/100.0)
                            "Joint" -> {
                                transferTotals[cSECOND_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                transferTotals[cSECOND_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split / 100.0))
                            }
                        }
                    }
                    "Joint" -> {
                        when (exp.boughtfor) {
                            firstName -> transferTotals[cJOINT_NAME][cFIRST_NAME] += (exp.amount/100.0)
                            secondName -> transferTotals[cJOINT_NAME][cSECOND_NAME] += (exp.amount/100.0)
                            "Joint" -> {
                                transferTotals[cJOINT_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                transferTotals[cJOINT_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split/100.0))
                            }
                        }
                    }
                }
            } else {
                when (exp.paidby) {
                    firstName -> {
                        when (exp.boughtfor) {
                            firstName -> totals[cFIRST_NAME][cFIRST_NAME] += (exp.amount/100.0)
                            secondName -> totals[cFIRST_NAME][cSECOND_NAME] += (exp.amount/100.0)
                            "Joint" -> {
                                totals[cFIRST_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                totals[cFIRST_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split / 100.0))
                            }
                        }
                    }
                    secondName -> {
                        when (exp.boughtfor) {
                            firstName -> totals[cSECOND_NAME][cFIRST_NAME] += (exp.amount/100.0)
                            secondName -> totals[cSECOND_NAME][cSECOND_NAME] += (exp.amount/100.0)
                            "Joint" -> {
                                totals[cSECOND_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                totals[cSECOND_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split / 100.0))
                            }
                        }
                    }
                    "Joint" -> {
                        when (exp.boughtfor) {
                            firstName -> {
                                totals[cJOINT_NAME][cFIRST_NAME] += ((exp.amount / 100.0) * (exp.bfname1split / 100.0))
                            }
                            secondName -> totals[cJOINT_NAME][cSECOND_NAME] += ((exp.amount/100.0) * (exp.bfname2split/100.0))
                            "Joint" -> {
                                totals[cJOINT_NAME][cJOINT_NAME] += ((exp.amount/100.0) * (exp.bfname1split / 100.0))
                                totals[cJOINT_NAME][cJOINT_NAME+1] += ((exp.amount/100.0) * (exp.bfname2split/100.0))
                            }
                        }
                    }
                }
            }
        }
        val dec = DecimalFormat("#.00")
        binding.accountingFf.text = "$ " + dec.format(totals[cFIRST_NAME][cFIRST_NAME])
        if (totals[cFIRST_NAME][cFIRST_NAME] == 0.0)
            binding.accountingFf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSf.text = "$ " + dec.format(totals[cFIRST_NAME][cSECOND_NAME])
        if (totals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.accountingSf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJff.text = "$ " + dec.format(totals[cFIRST_NAME][cJOINT_NAME])
        if (totals[cFIRST_NAME][cJOINT_NAME] == 0.0)
            binding.accountingJff.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJsf.text = "$ " + dec.format(totals[cFIRST_NAME][cJOINT_NAME+1])
        if (totals[cFIRST_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingJsf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingFs.text = "$ " + dec.format(totals[cSECOND_NAME][cFIRST_NAME])
        if (totals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.accountingFs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSs.text = "$ " + dec.format(totals[cSECOND_NAME][cSECOND_NAME])
        if (totals[cSECOND_NAME][cSECOND_NAME] == 0.0)
            binding.accountingSs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJfs.text = "$ " + dec.format(totals[cSECOND_NAME][cJOINT_NAME])
        if (totals[cSECOND_NAME][cJOINT_NAME] == 0.0)
            binding.accountingJfs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJss.text = "$ " + dec.format(totals[cSECOND_NAME][cJOINT_NAME+1])
        if (totals[cSECOND_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingJss.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingFj.text = "$ " + dec.format(totals[cJOINT_NAME][cFIRST_NAME])
        if (totals[cJOINT_NAME][cFIRST_NAME] == 0.0)
            binding.accountingFj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingSj.text = "$ " + dec.format(totals[cJOINT_NAME][cSECOND_NAME])
        if (totals[cJOINT_NAME][cSECOND_NAME] == 0.0)
            binding.accountingSj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJfj.text = "$ " + dec.format(totals[cJOINT_NAME][cJOINT_NAME])
        if (totals[cJOINT_NAME][cJOINT_NAME] == 0.0)
            binding.accountingJfj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingJsj.text = "$ " + dec.format(totals[cJOINT_NAME][cJOINT_NAME+1])
        if (totals[cJOINT_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingJsj.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        binding.accountingTFf.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cFIRST_NAME])
        if (transferTotals[cFIRST_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTFf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFs.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cSECOND_NAME])
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTFs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFj1.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cJOINT_NAME])
        if (transferTotals[cFIRST_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTFj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTFj2.text = "$ " + dec.format(transferTotals[cFIRST_NAME][cJOINT_NAME+1])
        if (transferTotals[cFIRST_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTFj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSf.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cFIRST_NAME])
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTSf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSs.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cSECOND_NAME])
        if (transferTotals[cSECOND_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTSs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSj1.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cJOINT_NAME])
        if (transferTotals[cSECOND_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTSj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTSj2.text = "$ " + dec.format(transferTotals[cSECOND_NAME][cJOINT_NAME+1])
        if (transferTotals[cSECOND_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTSj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJf.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cFIRST_NAME])
        if (transferTotals[cJOINT_NAME][cFIRST_NAME] == 0.0)
            binding.accountingTJf.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJs.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cSECOND_NAME])
        if (transferTotals[cJOINT_NAME][cSECOND_NAME] == 0.0)
            binding.accountingTJs.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJj1.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cJOINT_NAME])
        if (transferTotals[cJOINT_NAME][cJOINT_NAME] == 0.0)
            binding.accountingTJj1.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
        binding.accountingTJj2.text = "$ " + dec.format(transferTotals[cJOINT_NAME][cJOINT_NAME+1])
        if (transferTotals[cJOINT_NAME][cJOINT_NAME+1] == 0.0)
            binding.accountingTJj2.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))

        val oneOwesTwo = ((-totals[cFIRST_NAME][cSECOND_NAME])
                + (totals[cSECOND_NAME][cFIRST_NAME])
                - (totals[cFIRST_NAME][cJOINT_NAME+1])
                + (totals[cSECOND_NAME][cJOINT_NAME])
                + ((totals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0)/100)
                - ((totals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1)/100)
                + ((totals[cJOINT_NAME][cJOINT_NAME]) * SpenderViewModel.getSpenderSplit(0)/100)
                - ((totals[cJOINT_NAME][cJOINT_NAME+1]) * SpenderViewModel.getSpenderSplit(1)/100)
                - (transferTotals[cFIRST_NAME][cSECOND_NAME])
                + (transferTotals[cSECOND_NAME][cFIRST_NAME])
                - (transferTotals[cFIRST_NAME][cJOINT_NAME+1])
                + (transferTotals[cSECOND_NAME][cJOINT_NAME])
                + ((transferTotals[cJOINT_NAME][cFIRST_NAME]) * SpenderViewModel.getSpenderSplit(0)/100)
                - ((transferTotals[cJOINT_NAME][cSECOND_NAME]) * SpenderViewModel.getSpenderSplit(1)/100))

        val gridLayout = binding.gridLayout
        var cellIndex = 0
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS)
        gridLayout.setColumnCount(2)
        gridLayout.setRowCount(14)
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
                "Jt-"+SpenderViewModel.getSpenderName(1),
                totals[cFIRST_NAME][cJOINT_NAME+1], cellIndex)
            cellIndex += 2
            subtotal += totals[cFIRST_NAME][cJOINT_NAME+1]
        }
        if (totals[cJOINT_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(0)+"'s portion)",
                SpenderViewModel.getSpenderName(1),
                totals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)/100, cellIndex)
            cellIndex += 2
            subtotal += totals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)/100
        }
        if (totals[cJOINT_NAME][cJOINT_NAME+1] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(0)+"'s portion)",
                "Jt-"+SpenderViewModel.getSpenderName(1),
                totals[cJOINT_NAME][cJOINT_NAME+1] * SpenderViewModel.getSpenderSplit(1)/100, cellIndex)
            cellIndex += 2
            subtotal += totals[cJOINT_NAME][cJOINT_NAME+1] * SpenderViewModel.getSpenderSplit(1)/100
        }
        if (transferTotals[cFIRST_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                SpenderViewModel.getSpenderName(1),
                transferTotals[cFIRST_NAME][cSECOND_NAME], cellIndex, "Transfer")
            cellIndex += 2
            subtotal += transferTotals[cFIRST_NAME][cSECOND_NAME]
        }
        if (transferTotals[cFIRST_NAME][cJOINT_NAME+1] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
                "Jt-"+SpenderViewModel.getSpenderName(1),
                transferTotals[cFIRST_NAME][cJOINT_NAME+1], cellIndex, "Transfer")
            cellIndex += 2
            subtotal += transferTotals[cFIRST_NAME][cJOINT_NAME+1]
        }
        if (transferTotals[cJOINT_NAME][cSECOND_NAME] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(0)+"'s portion)",
                SpenderViewModel.getSpenderName(1),
                transferTotals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)/100,
                cellIndex, "Transfer")
            cellIndex += 2
            subtotal += transferTotals[cJOINT_NAME][cSECOND_NAME] * SpenderViewModel.getSpenderSplit(1)/100
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(0),
            SpenderViewModel.getSpenderName(1),
            subtotal, cellIndex, "Sub-Total")
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
                "Jt-"+SpenderViewModel.getSpenderName(0),
                totals[cSECOND_NAME][cJOINT_NAME], cellIndex)
            cellIndex += 2
            subtotal2 += totals[cSECOND_NAME][cJOINT_NAME]
        }
        if (totals[cJOINT_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(1)+"'s portion)",
                SpenderViewModel.getSpenderName(0),
                totals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)/100, cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)/100
        }
        if (totals[cJOINT_NAME][cJOINT_NAME] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(1)+"'s portion)",
                "Jt-"+SpenderViewModel.getSpenderName(0),
                totals[cJOINT_NAME][cJOINT_NAME] * SpenderViewModel.getSpenderSplit(0)/100, cellIndex)
            cellIndex += 2
            subtotal2 += totals[cJOINT_NAME][cJOINT_NAME] * SpenderViewModel.getSpenderSplit(0)/100
        }
        if (transferTotals[cSECOND_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                SpenderViewModel.getSpenderName(0),
                transferTotals[cSECOND_NAME][cFIRST_NAME], cellIndex, "Transfer")
            cellIndex += 2
            subtotal2 += transferTotals[cSECOND_NAME][cFIRST_NAME]
        }
        if (transferTotals[cSECOND_NAME][cJOINT_NAME] != 0.0) {
            buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
                "Jt-"+SpenderViewModel.getSpenderName(0),
                transferTotals[cSECOND_NAME][cJOINT_NAME], cellIndex, "Transfer")
            cellIndex += 2
            subtotal2 += transferTotals[cSECOND_NAME][cJOINT_NAME]
        }
        if (transferTotals[cJOINT_NAME][cFIRST_NAME] != 0.0) {
            buildGrid(gridLayout, "Joint ("+SpenderViewModel.getSpenderName(1)+"'s portion)",
                SpenderViewModel.getSpenderName(0),
                transferTotals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)/100,
                cellIndex, "Transfer")
            cellIndex += 2
            subtotal2 += transferTotals[cJOINT_NAME][cFIRST_NAME] * SpenderViewModel.getSpenderSplit(0)/100
        }
        buildGrid(gridLayout, SpenderViewModel.getSpenderName(1),
            SpenderViewModel.getSpenderName(0),
            subtotal2, cellIndex, "Sub-Total")
        cellIndex += 2

        when {
            oneOwesTwo == 0.0 -> binding.accountingSummary.text = "Nobody owes anyone!"
            oneOwesTwo > 0 -> {
                binding.accountingSummary.text = firstName + " owes " + secondName + " $ " + dec.format(oneOwesTwo)
                binding.accountingSummary2.text = " (" + dec.format(subtotal2) + " - " + dec.format(subtotal) + ")"
            }
            else -> {
                binding.accountingSummary.text = secondName + " owes " + firstName + " $ " + dec.format(oneOwesTwo*-1)
                binding.accountingSummary2.text = " (" + dec.format(subtotal) + " - " + dec.format(subtotal2) + ")"
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
        val dec = DecimalFormat("#.00")

        val titleText = TextView(context)
        val amountText = TextView(context)
        if (iTransfer == "Transfer")
            titleText.text = iName1 + " transferred to " + iName2 + " "
        else if (iTransfer == "Sub-Total") {
            paramsT.topMargin = 10
            paramsT.bottomMargin = 40
            titleText.setTypeface(null, Typeface.BOLD)
            amountText.setTypeface(null, Typeface.BOLD)
            titleText.text = "Total " + iName1 + "'s funds used for " + iName2 + "  "
        } else
            titleText.text = iName1 + " paid for " + iName2 + "  "
        titleText.layoutParams = paramsT
        iGridLayout.addView(titleText,0)
        if (iTransfer == "Sub-Total")
            amountText.text = "$ " + dec.format(iAmount)
        else
            amountText.text = dec.format(iAmount)
        amountText.layoutParams = paramsA
        iGridLayout.addView(amountText,1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}