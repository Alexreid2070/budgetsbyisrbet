package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetEditDialogBinding
import timber.log.Timber
import java.util.*

class BudgetEditDialogFragment : DialogFragment() {
    interface BudgetEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: BudgetEditDialogFragmentListener? = null

    private var _binding: FragmentBudgetEditDialogBinding? = null
    private val binding get() = _binding!!
    private var currentMode = cMODE_VIEW
//    private var monthInt = -1

    @Suppress("HardCodedStringLiteral")
    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"
        private const val KEY_DATE_VALUE = "KEY_DATE_VALUE"
        private const val KEY_PERIOD_VALUE = "KEY_PERIOD_VALUE"
        private const val KEY_REGULARITY_VALUE = "KEY_REGULARITY_VALUE"
        private const val KEY_WHO_VALUE = "KEY_WHO_VALUE"
        private const val KEY_AMOUNT_VALUE = "KEY_AMOUNT_VALUE"
        private const val KEY_OCCURENCE_VALUE = "KEY_OCCURENCE_VALUE"
        private const val KEY_KEY_VALUE = "KEY_KEY_VALUE"
        private var oldDate: MyDate = MyDate()
        private var oldPeriod: String = ""
        private var oldRegularity: Int = 1
        private var oldWho: Int = -1
        private var oldAmount: Double = 0.0
        private var oldOccurence: Int = -1
        private var oldKey: String = ""

        fun newInstance(
            key: String, categoryID: Int, date: String, period: String, regularity: Int,
            who: Int, amount: Double, occurence: Int
        ): BudgetEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY_ID, categoryID.toString())
            args.putString(KEY_DATE_VALUE, date)
            args.putString(KEY_PERIOD_VALUE, period)
            args.putString(KEY_REGULARITY_VALUE, regularity.toString())
            args.putString(KEY_WHO_VALUE, who.toString())
            args.putString(KEY_AMOUNT_VALUE, amount.toString())
            args.putString(KEY_OCCURENCE_VALUE, occurence.toString())
            args.putString(KEY_KEY_VALUE, key)
            val fragment = BudgetEditDialogFragment()
            fragment.arguments = args

            oldAmount = amount
            oldDate = MyDate(date)
            oldPeriod = period
            oldRegularity = regularity
            oldWho = who
            oldOccurence = occurence
            oldKey = key
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
        binding.currencySymbol.text = String.format("${getLocalCurrencySymbol()} ")
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
            binding.periodSpinnerRelativeLayout.visibility = View.GONE
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

        binding.periodField.text = oldPeriod
        val pSpinner: Spinner = binding.periodSpinner
        val periodValues = listOf(
            getString(R.string.week),
            getString(R.string.month),
            getString(R.string.year))
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodValues
        )
        pSpinner.adapter = arrayAdapter
        pSpinner.setSelection(arrayAdapter.getPosition(oldPeriod))
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.periodSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.periodSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.regularity.setText(oldRegularity.toString())
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
        binding.budgetDialogCategorySubcategory.text = String.format("${cat?.categoryName}-${cat?.subcategoryName}")
        binding.startDate.setText(oldDate.toString())
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.startDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }

        binding.startDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.startDate.text.toString() != "") {
                lcal = MyDate(binding.startDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), dateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }
        binding.periodSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (binding.periodSpinner.selectedItem.toString()) {
                    getString(R.string.month) -> {
                        val startOfPeriod = MyDate(binding.startDate.text.toString())
                        if (startOfPeriod.getDay() != 1) {
                            val lDate = MyDate(startOfPeriod.getYear(), startOfPeriod.getMonth(), 1)
                            binding.startDate.setText(lDate.toString())
                        }
                    }
                    getString(R.string.year) -> {
                        val startOfPeriod = MyDate(binding.startDate.text.toString())
                        if (startOfPeriod.getDay() != 1 || startOfPeriod.getMonth() != 1) {
                            val lDate = MyDate(startOfPeriod.getYear(), 1, 1)
                            binding.startDate.setText(lDate.toString())
                        }
                    }
                }
            }
        }

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
                val param = binding.budgetDialogWhoHeading.layoutParams as LinearLayout.LayoutParams
                param.weight = 0f
                binding.budgetDialogNewAmount.isEnabled = true
                currentMode = cMODE_EDIT
                binding.startDate.isEnabled = true
                binding.regularity.isEnabled = true
                binding.amountLayout.visibility = View.VISIBLE
                binding.amountField.visibility = View.GONE
                binding.budgetDialogNewWhoRadioGroup.visibility = View.VISIBLE
                binding.periodSpinnerRelativeLayout.visibility = View.VISIBLE
                binding.whoField.visibility = View.GONE
                binding.periodField.visibility = View.GONE
                binding.budgetDialogOccurenceRadioGroup.visibility = View.VISIBLE
                binding.occurenceField.visibility = View.GONE
            } else { // we're in edit mode, so need to save
                val lNumberFormat: NumberFormat = NumberFormat.getInstance()
                val amountDouble = lNumberFormat.parse(binding.budgetDialogNewAmount.text.toString()).toDouble()
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
                val errorMsg = BudgetViewModel.checkNewBudget(
                    oldKey,
                    binding.budgetDialogCategoryID.text.toString().toInt(),
                    MyDate(binding.startDate.text.toString()),
                    newWho,
                    binding.budgetDialogNewAmount.text.toString(),
                    binding.periodSpinner.selectedItem.toString(),
                    binding.regularity.text.toString())
                if (errorMsg == "") {
                    BudgetViewModel.updateBudget(
                        oldKey,
                        binding.budgetDialogCategoryID.text.toString().toInt(),
                        MyDate(binding.startDate.text.toString()),
                        newWho,
                        amountDouble,
                        binding.periodSpinner.selectedItem.toString(),
                        binding.regularity.text.toString().toInt(),
                        newOccurenceID
                    )
                    if (listener != null)
                        listener?.onNewDataSaved()
                    dismiss()
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                } else {
                    showErrorMessage(parentFragmentManager, errorMsg)
                    focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
//                    return
                }
/*                if (oldWho == newWho) {
                    BudgetViewModel.updateBudget(
                        oldKey,
                        binding.budgetDialogCategoryID.text.toString().toInt(),
                        oldDate,
                        newWho,
                        amountDouble,
                        binding.periodSpinner.selectedItem.toString(),
                        binding.regularity.text.toString().toInt(),
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
                                    val  tempBudget = BudgetViewModel.getBudget(binding.budgetDialogCategoryID.text.toString().toInt())
                                    if (tempBudget != null) {
                                        if (!tempBudget.overlapsWithExistingBudget(
                                                MyDate(binding.startDate.text.toString()),
                                                binding.periodSpinner.selectedItem.toString(),
                                                binding.regularity.text.toString().toInt(),
                                                newWho)) {
                                            BudgetViewModel.updateBudget(
                                                oldKey,
                                                binding.budgetDialogCategoryID.text.toString().toInt(),
                                                MyDate(binding.startDate.text.toString()),
                                                newWho,
                                                amountDouble,
                                                binding.periodSpinner.selectedItem.toString(),
                                                binding.regularity.text.toString().toInt(),
                                                newOccurenceID
                                            )
                                            // delete oldWho
                                            BudgetViewModel.deleteBudget(
                                                binding.budgetDialogCategoryID.text.toString().toInt(),
                                                oldKey
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
//                                }
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
                            MyApplication.databaseRef.child(budgetListenerLocation)
                                .removeEventListener(budgetListener)
                        }

                        override fun onCancelled(dataSnapshot: DatabaseError) {
                            MyApplication.displayToast(getString(R.string.user_authorization_failed) + " 103.")
                        }
                    }
                    budgetListenerLocation = "Users/"+MyApplication.userUID+"/Budget/" +
                        binding.budgetDialogCategoryID.text.toString() + "/" +
                        MyDate(binding.startDate.text.toString()).toString() + "/" +
                        newWho.toString()
                    MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Budget")
                        .child(binding.budgetDialogCategoryID.text.toString())
                        .child(MyDate(binding.startDate.text.toString()).toString())
                        .child(newWho.toString())
                        .addValueEventListener(budgetListener)
                } */
            }
        }
        binding.budgetDialogButtonDelete.setOnClickListener {
            if (currentMode == cMODE_VIEW) { // ie user chose Delete
                fun yesClicked() {
                    BudgetViewModel.deleteBudget(
                        binding.budgetDialogCategoryID.text.toString().toInt(),
                        oldKey
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