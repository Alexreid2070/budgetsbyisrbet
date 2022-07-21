package com.isrbet.budgetsbyisrbet

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.method.DigitsKeyListener
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
        private const val KEY_TRANSACTION_ID = "0"
        private var oldID: String = ""
        fun newInstance(
            inID: String
        ): TransactionCreditDialogFragment {
            val args = Bundle()

            args.putString(KEY_TRANSACTION_ID, inID)
            val fragment = TransactionCreditDialogFragment()
            fragment.arguments = args
            oldID = inID
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionCreditDialogBinding.inflate(inflater, container, false)
        binding.creditAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cal = android.icu.util.Calendar.getInstance()
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        val thisTransaction = TransactionViewModel.getTransaction(oldID) ?: return

        binding.creditAmount.setText(gDecM(-1 * thisTransaction.amount))
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

    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            val thisTransaction = TransactionViewModel.getTransaction(oldID) ?: return@setOnClickListener

            if (binding.creditAmount.text.toString() == "") {
                binding.creditAmount.error=getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.creditAmount)
                return@setOnClickListener
            }
            val amount = gNumberFormat.parse(binding.creditAmount.text.toString()).toDouble()
            if (amount > 0.0) {
                binding.creditAmount.error=getString(R.string.amountNotNegative)
                focusAndOpenSoftKeyboard(requireContext(), binding.creditAmount)
                return@setOnClickListener
            }

            val transactionOut = TransactionOut(
                binding.creditDate.text.toString(),
                round(amount*100).toInt(),
                thisTransaction.category,
                thisTransaction.note,
                binding.creditNote.text.toString().trim(),
                thisTransaction.paidby,
                thisTransaction.boughtfor,
                thisTransaction.bfname1split,
                cTRANSACTION_TYPE_CREDIT
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