package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionCreditDialogBinding
import java.util.*
import kotlin.math.round

class TransactionCreditDialogFragment : DialogFragment() {
    private var _binding: FragmentTransactionCreditDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_AMOUNT = "KEY_AMOUNT"
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"
        private const val KEY_WHERE = "KEY_WHERE"
        private const val KEY_PAID_BY = "KEY_PAID_BY"
        private const val KEY_BOUGHT_FOR = "KEY_BOUGHT_FOR"
        private const val KEY_SPLIT = "KEY_SPLIT"
        private var oldAmount: Double = 0.0
        private var oldCategoryID: Int = 0
        private var oldWhere: String = ""
        private var oldPaidBy: Int = 0
        private var oldBoughtFor: Int = 0
        private var oldSplit: Int = 0
        fun newInstance(
            inAmount: Int,
            inCategoryID: Int,
            inWhere: String,
            inPaidBy: Int,
            inBoughtor: Int,
            inSplit: Int
        ): TransactionCreditDialogFragment {
            val args = Bundle()

            args.putString(KEY_AMOUNT, inAmount.toString())
            args.putString(KEY_CATEGORY_ID, inCategoryID.toString())
            args.putString(KEY_WHERE, inWhere)
            args.putString(KEY_PAID_BY, inPaidBy.toString())
            args.putString(KEY_BOUGHT_FOR, inBoughtor.toString())
            args.putString(KEY_SPLIT, inSplit.toString())
            val fragment = TransactionCreditDialogFragment()
            fragment.arguments = args
            oldAmount = inAmount / 100.0
            oldCategoryID = inCategoryID
            oldWhere = inWhere
            oldPaidBy = inPaidBy
            oldBoughtFor = inBoughtor
            oldSplit = inSplit
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionCreditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cal = android.icu.util.Calendar.getInstance()
        super.onViewCreated(view, savedInstanceState)
        Log.d("Alex", "In amount $oldAmount cat $oldCategoryID where $oldWhere pb $oldPaidBy bf $oldBoughtFor split $oldSplit")

        setupClickListeners()

        binding.creditAmount.setText("-" + gDec.format(oldAmount))
        binding.creditDate.setText(giveMeMyDateFormat(cal))
        binding.creditNote.setText("CREDIT")
        binding.currencySymbol.text = getLocalCurrencySymbol() + " "
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.creditDate.setText(giveMeMyDateFormat(cal))
            }

        binding.creditDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            if (binding.creditAmount.text.toString() == "") {
                binding.creditAmount.error=getString(R.string.missingAmountError)
                focusAndOpenSoftKeyboard(requireContext(), binding.creditAmount)
                return@setOnClickListener
            }
            val amountInt: Int
            val tempDouble : Double = round(getDoubleValue(binding.creditAmount.text.toString())*100)
            amountInt = tempDouble.toInt()
            val transactionOut = TransactionOut(
                binding.creditDate.text.toString(),
                amountInt,
                oldCategoryID,
                oldWhere,
                binding.creditNote.text.toString().trim(),
                oldPaidBy,
                oldBoughtFor,
                oldSplit,
                "Credit"
            )
            TransactionViewModel.addTransaction(transactionOut)
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}