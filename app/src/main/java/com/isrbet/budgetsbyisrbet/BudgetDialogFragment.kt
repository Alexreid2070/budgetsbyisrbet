import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.*
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetEditDialogBinding
import java.text.DecimalFormat
import kotlin.math.round

class BudgetDialogFragment : DialogFragment() {
    interface BudgetEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: BudgetEditDialogFragmentListener? = null

    private var _binding: FragmentBudgetEditDialogBinding? = null
    private val binding get() = _binding!!
    private var currentMode = "View"

    companion object {
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_YEAR_VALUE = "KEY_YEAR_VALUE"
        private const val KEY_MONTH_VALUE = "KEY_MONTH_VALUE"
        private const val KEY_WHO_VALUE = "KEY_WHO_VALUE"
        private const val KEY_AMOUNT_VALUE = "KEY_AMOUNT_VALUE"
        private const val KEY_OCCURENCE_VALUE = "KEY_OCCURENCE_VALUE"
        private var oldYear: Int = 0
        private var oldMonth: Int = 0
        private var oldWho: String = ""
        private var oldAmount: Double = 0.0
        private var oldOccurence: Int = -1

        fun newInstance(
            category: String,
            year: Int, month: Int, who: String, amount: Double, occurence: Int
        ): BudgetDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_YEAR_VALUE, year.toString())
            args.putString(KEY_MONTH_VALUE, month.toString())
            args.putString(KEY_WHO_VALUE, who)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupClickListeners()

        var ctr = 200
        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i, true)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.text = spender?.name
            newRadioButton.id = ctr++
            binding.budgetDialogNewWhoRadioGroup.addView(newRadioButton)
            if (spender?.name == oldWho) {
                binding.budgetDialogNewWhoRadioGroup.check(newRadioButton.id)
            }
        }
        ctr = 300
        var newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = cBUDGET_JUST_THIS_MONTH
        newRadioButton.id = ctr++
        binding.budgetDialogOccurenceRadioGroup.addView(newRadioButton)
        if (oldOccurence == 1)
            binding.budgetDialogOccurenceRadioGroup.check(newRadioButton.id)

        newRadioButton = RadioButton(requireContext())
        newRadioButton.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newRadioButton.text = cBUDGET_RECURRING
        newRadioButton.id = ctr
        binding.budgetDialogOccurenceRadioGroup.addView(newRadioButton)
        if (oldOccurence == 0)
            binding.budgetDialogOccurenceRadioGroup.check(newRadioButton.id)

        for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
            (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
        }
        for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
            (binding.budgetDialogOccurenceRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
        }
        if (SpenderViewModel.singleUser()) {
            binding.budgetDialogWhoHeading.visibility = View.GONE
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
        binding.budgetDialogCategory.text = arguments?.getString(KEY_CATEGORY)
        binding.budgetDialogYear.text = arguments?.getString(KEY_YEAR_VALUE)
        val month = arguments?.getString(KEY_MONTH_VALUE)
        if (month == "0")
            binding.budgetDialogMonth.text = ""
        else
            binding.budgetDialogMonth.text = month
        val dec = DecimalFormat("#.00")
        val amtDouble: Double
        val amt = arguments?.getString(KEY_AMOUNT_VALUE)
        amtDouble = amt?.toDouble() ?: 0.0
        binding.budgetDialogNewAmount.setText(dec.format(amtDouble))
        val who = arguments?.getString(KEY_WHO_VALUE)
        Log.d("Alex", "who is $who")
        val occurence = arguments?.getString(KEY_OCCURENCE_VALUE)
        Log.d("Alex", "occurence is $occurence")

        val whoRadioGroup = binding.budgetDialogNewWhoRadioGroup
        for (i in 0 until whoRadioGroup.childCount) {
            val o = whoRadioGroup.getChildAt(i)
            if (o is RadioButton) {
                Log.d("Alex", "who.text is " + o.text)
                if (o.text == who) {
                    o.isChecked = true
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.budgetDialogButton1.setOnClickListener {
            if (currentMode == "View") {
                binding.budgetDialogButton1.text = "Save"
                binding.budgetDialogButton2.text = "Cancel"
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
                    (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                }
                for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
                    (binding.budgetDialogOccurenceRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                }
                binding.budgetDialogNewAmount.isEnabled = true
                currentMode = "Edit"
            } else { // it's edit
                // Make sure there are values set for all fields
                if (binding.budgetDialogNewAmount.text.toString() == "") {
//                    showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
                    binding.budgetDialogNewAmount.error=getString(R.string.missingAmountError)
                    focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
                    return@setOnClickListener
                }
                if (binding.budgetDialogNewAmount.text.toString().toDouble() == 0.0) {
//                    showErrorMessage(getParentFragmentManager(), getString(R.string.missingAmountError))
                    binding.budgetDialogNewAmount.error=getString(R.string.missingAmountError)
                    focusAndOpenSoftKeyboard(requireContext(), binding.budgetDialogNewAmount)
                    return@setOnClickListener
                }

                Log.d("Alex", "need to save new values")
                var tmpDouble1: Double = binding.budgetDialogNewAmount.text.toString().toDouble()
                tmpDouble1 = round(tmpDouble1 * 100) / 100
                val amountInt: Int = (tmpDouble1 * 100.0).toInt()

                var newWho = ""
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
                    val o = binding.budgetDialogNewWhoRadioGroup.getChildAt(i)
                    if (o is RadioButton)
                        if (o.isChecked)
                            newWho = o.text.toString()
                }
                Log.d("Alex", "oldwho is $oldWho and newwho is $newWho")
                var newOccurence = ""
                for (i in 0 until binding.budgetDialogOccurenceRadioGroup.childCount) {
                    val o = binding.budgetDialogOccurenceRadioGroup.getChildAt(i)
                    if (o is RadioButton)
                        if (o.isChecked)
                            newOccurence = o.text.toString()
                }
                Log.d("Alex", "oldOccurence is $oldOccurence and new is $newOccurence")
                if (oldWho == newWho) {
                    BudgetViewModel.updateBudget(
                        binding.budgetDialogCategory.text.toString(),
                        BudgetMonth(
                            oldYear, oldMonth
                        ).toString(),
                        newWho,
                        amountInt,
                        newOccurence)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    dismiss()
                } else {
                    // check to see if there is already an entry for the new who.
                    val budgetListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.value == null) { // nothing exists at this node so we can add it
                                Log.d("Alex", "Nothing exists at this node so we can add it ")
                                val monthToUse: Int = if (binding.budgetDialogMonth.text.toString() == "")
                                    0
                                else
                                    binding.budgetDialogMonth.text.toString().toInt()
                                    val prevMonth = BudgetMonth(
                                        binding.budgetDialogYear.text.toString().toInt(),
                                        monthToUse
                                    )
                                    prevMonth.decrementMonth()
                                    val tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                                        binding.budgetDialogCategory.text.toString(),
                                        prevMonth,
                                        newWho
                                    )
                                Log.d("Alex", "tmpDouble1 is " + tmpDouble1.toString() + " and tmpPrev is " + tmpPrevAmt.amount.toString())
                                if (tmpDouble1 == tmpPrevAmt.amount) {
                                    // ie new amount is same as previous month, so we can just delete this month's change
                                    BudgetViewModel.deleteBudget(
                                        binding.budgetDialogCategory.text.toString(),
                                        BudgetMonth(
                                            oldYear, oldMonth
                                        ).toString(),
                                        newWho
                                    )
                                    // delete oldWho
                                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), BudgetMonth(
                                        oldYear, oldMonth).toString(), oldWho)
                                    if (listener != null)
                                        listener?.onNewDataSaved()
                                    dismiss()
                                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                                    Log.d("Alex",
                                        "tmpDouble1 is same as previous month so just deleted the entry '$tmpDouble1'"
                                    )
                                } else { // new amount is different from previous month, so need to record it
                                    val  tempBudget = BudgetViewModel.getBudget(binding.budgetDialogCategory.text.toString())
                                    if (tempBudget != null) {
                                        if (!tempBudget.overlapsWithExistingBudget(
                                                BudgetMonth(binding.budgetDialogYear.text.toString().toInt(), monthToUse).toString(),
                                                newWho)) {
                                            BudgetViewModel.updateBudget(
                                                binding.budgetDialogCategory.text.toString(),
                                                BudgetMonth(
                                                    oldYear, oldMonth
                                                ).toString(),
                                                newWho,
                                                amountInt,
                                                newOccurence
                                            )
                                            // delete oldWho
                                            BudgetViewModel.deleteBudget(
                                                binding.budgetDialogCategory.text.toString(),
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
                        }

                        override fun onCancelled(dataSnapshot: DatabaseError) {
                        }
                    }
                    val monthToUse: Int = if (binding.budgetDialogMonth.text.toString() == "")
                        0
                    else
                        binding.budgetDialogMonth.text.toString().toInt()
                    val dbRef =
                        MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/NewBudget")
                            .child(binding.budgetDialogCategory.text.toString())
                            .child(
                                BudgetMonth(
                                    binding.budgetDialogYear.text.toString().toInt(),
                                    monthToUse
                                ).toString()
                            )
                            .child(newWho)
                    dbRef.addListenerForSingleValueEvent(budgetListener)
                }
            }
        }
        binding.budgetDialogButton2.setOnClickListener {
            if (currentMode == "View") { // ie user chose Delete
                fun yesClicked() {
                    val monthToUse: Int = if (binding.budgetDialogMonth.text.toString() == "")
                        0
                    else
                        binding.budgetDialogMonth.text.toString().toInt()
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), BudgetMonth(
                        binding.budgetDialogYear.text.toString().toInt(),
                        monthToUse
                    ).toString(), oldWho)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    Toast.makeText(activity, "Budget deleted", Toast.LENGTH_SHORT).show()
                    dismiss()
                    MyApplication.playSound(context, R.raw.short_springy_gun)
                }
                fun noClicked() {
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Are you sure?")
                    .setMessage("Are you sure that you want to delete this budget item?")
                    .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                    .show()

            } else { // ie user chose Cancel
                dismiss()
            }
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