package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetEditDialogBinding

class BudgetDialogFragment : DialogFragment() {
    interface BudgetEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: BudgetEditDialogFragmentListener? = null

    private var _binding: FragmentBudgetEditDialogBinding? = null
    private val binding get() = _binding!!
    private var currentMode = cMODE_VIEW
    private var monthInt = -1

    @Suppress("HardCodedStringLiteral")
    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"
        private const val KEY_YEAR_VALUE = "KEY_YEAR_VALUE"
        private const val KEY_MONTH_VALUE = "KEY_MONTH_VALUE"
        private const val KEY_WHO_VALUE = "KEY_WHO_VALUE"
        private const val KEY_AMOUNT_VALUE = "KEY_AMOUNT_VALUE"
        private const val KEY_OCCURENCE_VALUE = "KEY_OCCURENCE_VALUE"
        private var oldYear: Int = 0
        private var oldMonth: Int = 0
        private var oldWho: Int = -1
        private var oldAmount: Double = 0.0
        private var oldOccurence: Int = -1

        fun newInstance(
            categoryID: Int,
            year: Int, month: Int, who: Int, amount: Double, occurence: Int
        ): BudgetDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY_ID, categoryID.toString())
            args.putString(KEY_YEAR_VALUE, year.toString())
            args.putString(KEY_MONTH_VALUE, month.toString())
            args.putString(KEY_WHO_VALUE, who.toString())
            args.putString(KEY_AMOUNT_VALUE, amount.toString())
            args.putString(KEY_OCCURENCE_VALUE, occurence.toString())
            val fragment = BudgetDialogFragment()
            fragment.arguments = args

            oldAmount = amount
            oldYear = year
            oldMonth = month
            oldWho = who
            oldOccurence = occurence
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetEditDialogBinding.inflate(inflater, container, false)
        binding.budgetDialogNewAmount.keyListener = DigitsKeyListener.getInstance("-0123456789$gDecimalSeparator")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currencySymbol.text = getLocalCurrencySymbol() + " "
        setupView()
        setupClickListeners()

        var ctr = 200
        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            binding.budgetDialogNewWhoRadioGroup.addView(newRadioButton)
            if (oldWho == i) {
                binding.budgetDialogNewWhoRadioGroup.check(newRadioButton.id)
            }
        }
        binding.whoField.text = SpenderViewModel.getSpenderName(oldWho)
        if (currentMode == cMODE_VIEW) {
            binding.budgetDialogNewWhoRadioGroup.visibility = View.GONE
        }
        ctr = 300
        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.once)
        newRadioButton.id = ctr++
        binding.budgetDialogOccurenceRadioGroup.addView(newRadioButton)
        if (oldOccurence == cBUDGET_JUST_THIS_MONTH)
            binding.budgetDialogOccurenceRadioGroup.check(newRadioButton.id)
        binding.occurenceField.text = if (oldOccurence == 0) getString(R.string.recurring) else getString(R.string.once)
        binding.periodField.text = if (oldMonth == 0) getString(R.string.annual) else getString(R.string.monthly)
        if (currentMode == cMODE_VIEW) {
            binding.budgetDialogOccurenceRadioGroup.visibility = View.GONE
        }

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = getString(R.string.recurring)
        newRadioButton.id = ctr
        binding.budgetDialogOccurenceRadioGroup.addView(newRadioButton)
        if (oldOccurence == cBUDGET_RECURRING)
            binding.budgetDialogOccurenceRadioGroup.check(newRadioButton.id)

        for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
            (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
        }
        for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
            (binding.budgetDialogOccurenceRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
        }
        if (SpenderViewModel.singleUser()) {
            binding.whoLayout.visibility = View.GONE
            binding.budgetDialogNewWhoRadioGroup.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupView() {
        val categoryID = arguments?.getString(KEY_CATEGORY_ID)
        val cat = CategoryViewModel.getCategory(categoryID.toString().toInt())
        binding.budgetDialogCategoryID.text = cat?.id.toString()
        binding.budgetDialogCategorySubcategory.text = "${cat?.categoryName}-${cat?.subcategoryName}"
        binding.budgetDialogYear.text = arguments?.getString(KEY_YEAR_VALUE)
        val month = arguments?.getString(KEY_MONTH_VALUE)
        monthInt = if (month == "") -1 else month?.toInt()?.minus(1)!!
        if (monthInt == -1)
            binding.budgetDialogMonth.text = ""
        else
            binding.budgetDialogMonth.text = gMonthName(monthInt+1)
        val amtDouble: Double
        val amt = arguments?.getString(KEY_AMOUNT_VALUE)
        amtDouble = amt?.toDouble() ?: 0.0
        binding.budgetDialogNewAmount.setText(gDec(amtDouble))
        binding.amountField.text = gDec(amtDouble)
        if (currentMode == cMODE_VIEW) {
            binding.amountLayout.visibility = View.GONE
            val catName = cat?.categoryName.toString()
            binding.categoryLayout.setBackgroundColor(
                DefaultsViewModel.getCategoryDetail(catName).color)
        }

        val who = arguments?.getString(KEY_WHO_VALUE)
//        val occurence = arguments?.getString(KEY_OCCURENCE_VALUE)

        val whoRadioGroup = binding.budgetDialogNewWhoRadioGroup
        for (i in 0 until whoRadioGroup.childCount) {
            val o = whoRadioGroup.getChildAt(i)
            if (o is RadioButton) {
                if (o.text == who) {
                    o.isChecked = true
                }
            }
        }
    }

    private fun setupClickListeners() {
        var budgetListenerLocation = ""
        lateinit var budgetListener: ValueEventListener
        binding.budgetDialogButtonEdit.setOnClickListener {
            if (currentMode == cMODE_VIEW) {
                binding.budgetDialogButtonEdit.text = getString(R.string.save)
                binding.budgetDialogButtonEdit.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(requireActivity(),R.drawable.ic_baseline_save_24), null, null)
                binding.budgetDialogButtonDelete.visibility = View.GONE
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
                    (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                }
                for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
                    (binding.budgetDialogOccurenceRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                }
                binding.budgetDialogNewAmount.isEnabled = true
                currentMode = cMODE_EDIT
                binding.amountLayout.visibility = View.VISIBLE
                binding.amountField.visibility = View.GONE
                binding.budgetDialogNewWhoRadioGroup.visibility = View.VISIBLE
                binding.whoField.visibility = View.GONE
                binding.budgetDialogOccurenceRadioGroup.visibility = View.VISIBLE
                binding.occurenceField.visibility = View.GONE
            } else { // we're in edit mode, so need to save
                // Make sure there are values set for all fields
                if (binding.budgetDialogNewAmount.text.toString() == "") {
//                    showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
                    binding.budgetDialogNewAmount.error=getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
                    return@setOnClickListener
                }
                val amountDouble = gNumberFormat.parse(binding.budgetDialogNewAmount.text.toString()).toDouble()
                if (amountDouble == 0.0) {
                    binding.budgetDialogNewAmount.error=getString(R.string.value_cannot_be_zero)
                    focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
                    return@setOnClickListener
                }

                var newWho = -1
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
                    val o = binding.budgetDialogNewWhoRadioGroup.getChildAt(i)
                    if (o is RadioButton)
                        if (o.isChecked)
                            newWho = SpenderViewModel.getSpenderIndex(o.text.toString())
                }
                var newOccurence = ""
                for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
                    val o = binding.budgetDialogOccurenceRadioGroup.getChildAt(i)
                    if (o is RadioButton)
                        if (o.isChecked)
                            newOccurence = o.text.toString()
                }
                val newOccurenceID = if (newOccurence == getString(R.string.once))
                    cBUDGET_JUST_THIS_MONTH
                else
                    cBUDGET_RECURRING
                @Suppress("HardCodedStringLiteral")
                if (oldWho == newWho) {
                    BudgetViewModel.updateBudget(
                        binding.budgetDialogCategoryID.text.toString().toInt(),
                        BudgetMonth(
                            oldYear, oldMonth
                        ).toString(),
                        newWho,
                        amountDouble,
                        newOccurenceID)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    dismiss()
                } else {
                    // check to see if there is already an entry for the new who.
                    budgetListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.value == null) { // nothing exists at this node so we can add it
                                val monthToUse: Int = if (monthInt == -1)
                                    0
                                else
                                    monthInt + 1
                                val prevMonth = BudgetMonth(
                                    binding.budgetDialogYear.text.toString().toInt(),
                                    monthToUse
                                )
                                prevMonth.decrementMonth()
                                val tmpPrevAmt = BudgetViewModel.getOriginalBudgetAmount(
                                    binding.budgetDialogCategoryID.text.toString().toInt(),
                                    prevMonth,
                                    newWho
                                )
                                if (amountDouble == tmpPrevAmt.amount) {
                                    // ie new amount is same as previous month, so we can just delete this month's change
                                    BudgetViewModel.deleteBudget(
                                        binding.budgetDialogCategoryID.text.toString().toInt(),
                                        BudgetMonth(
                                            oldYear, oldMonth
                                        ).toString(),
                                        newWho
                                    )
                                    // delete oldWho
                                    BudgetViewModel.deleteBudget(
                                        binding.budgetDialogCategoryID.text.toString().toInt(),
                                        BudgetMonth(
                                        oldYear, oldMonth
                                        ).toString(), oldWho
                                    )
                                    if (listener != null)
                                        listener?.onNewDataSaved()
                                    dismiss()
                                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                                } else { // new amount is different from previous month, so need to record it
                                    val  tempBudget = BudgetViewModel.getBudget(binding.budgetDialogCategoryID.text.toString().toInt())
                                    if (tempBudget != null) {
                                        if (!tempBudget.overlapsWithExistingBudget(
                                                BudgetMonth(binding.budgetDialogYear.text.toString().toInt(), monthToUse).toString(),
                                                newWho)) {
                                            BudgetViewModel.updateBudget(
                                                binding.budgetDialogCategoryID.text.toString().toInt(),
                                                BudgetMonth(
                                                    oldYear, oldMonth
                                                ).toString(),
                                                newWho,
                                                amountDouble,
                                                newOccurenceID
                                            )
                                            // delete oldWho
                                            BudgetViewModel.deleteBudget(
                                                binding.budgetDialogCategoryID.text.toString().toInt(),
                                                BudgetMonth(
                                                    oldYear, oldMonth
                                                ).toString(),
                                                oldWho
                                            )
                                            if (listener != null)
                                                listener?.onNewDataSaved()
                                            dismiss()
                                            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                                        } else {
                                            showErrorMessage(parentFragmentManager, getString(R.string.budgetOverlap))
                                            focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
                                            return
                                        }
                                    }
                                }
                            } else { // something exists so error
                                showErrorMessage(
                                    parentFragmentManager,
                                    getString(R.string.budgetExistsAtThatNode)
                                )
                                focusAndOpenSoftKeyboard(
                                    requireContext(),
                                    binding.budgetDialogNewWhoRadioGroup
                                )
                                return
                            }

                            // remove the listener, as we only wanted the value once
                            MyApplication.databaseref.child(budgetListenerLocation)
                                .removeEventListener(budgetListener)
                        }

                        override fun onCancelled(dataSnapshot: DatabaseError) {
                            MyApplication.displayToast(getString(R.string.user_authorization_failed) + " 103.")
                        }
                    }
                    val monthToUse: Int = if (monthInt == -1)
                        0
                    else
                        monthInt + 1
                    budgetListenerLocation = "Users/"+MyApplication.userUID+"/Budget/" +
                        binding.budgetDialogCategoryID.text.toString() + "/" +
                        BudgetMonth(binding.budgetDialogYear.text.toString().toInt(),monthToUse).toString() + "/" +
                        newWho.toString()
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                        .child(binding.budgetDialogCategoryID.text.toString())
                        .child(BudgetMonth(
                                binding.budgetDialogYear.text.toString().toInt(),
                                monthToUse
                            ).toString())
                        .child(newWho.toString())
                        .addValueEventListener(budgetListener)
                }
            }
        }
        binding.budgetDialogButtonDelete.setOnClickListener {
            if (currentMode == cMODE_VIEW) { // ie user chose Delete
                fun yesClicked() {
                    val monthToUse: Int = if (binding.budgetDialogMonth.text.toString() == "")
                        0
                    else {
                        val m = arguments?.getString(KEY_MONTH_VALUE)
                        m?.toInt() ?: 0
                    }
                    BudgetViewModel.deleteBudget(
                        binding.budgetDialogCategoryID.text.toString().toInt(),
                        BudgetMonth(
                        binding.budgetDialogYear.text.toString().toInt(),
                        monthToUse
                    ).toString(), oldWho
                    )
                    if (listener != null)
                        listener?.onNewDataSaved()
                    Toast.makeText(activity, getString(R.string.budget_deleted), Toast.LENGTH_SHORT).show()
                    dismiss()
                    MyApplication.playSound(context, R.raw.short_springy_gun)
                }
                fun noClicked() {
                }

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.are_you_sure))
                    .setMessage(getString(R.string.are_you_sure_budget))
                    .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                    .show()

            } else { // ie user chose Cancel
                dismiss()
            }
        }
        binding.budgetDialogButtonCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setDialogFragmentListener(listener: BudgetEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}