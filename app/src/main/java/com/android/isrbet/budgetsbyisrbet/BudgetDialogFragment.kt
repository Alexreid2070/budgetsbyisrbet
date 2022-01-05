import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.isrbet.budgetsbyisrbet.*
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetEditDialogBinding
import com.isrbet.budgetsbyisrbet.databinding.FragmentDashboardBinding
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
        private var oldYear: Int = 0
        private var oldMonth: Int = 0
        private var oldWho: String = ""
        private var oldAmount: Double = 0.0

        fun newInstance(
            category: String,
            year: Int, month: Int, who: String, amount: Double
        ): BudgetDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_YEAR_VALUE, year.toString())
            args.putString(KEY_MONTH_VALUE, month.toString())
            args.putString(KEY_WHO_VALUE, who)
            args.putString(KEY_AMOUNT_VALUE, amount.toString())
            val fragment = BudgetDialogFragment()
            fragment.arguments = args
            oldAmount = amount
            oldYear = year
            oldMonth = month
            oldWho = who
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBudgetEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
        setupClickListeners(view)

        var ctr: Int
        ctr = 200
        for (i in 0..SpenderViewModel.getCount()-1) {
            var spender = SpenderViewModel.getSpender(i)
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.setText(spender?.name)
            newRadioButton.id = ctr++
            binding.budgetDialogNewWhoRadioGroup.addView(newRadioButton)
            if (spender?.name == oldWho) {
                binding.budgetDialogNewWhoRadioGroup.check(newRadioButton.id)
            }
        }

        for (i in 0 until binding.budgetDialogNewWhoRadioGroup.getChildCount()) {
            (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupView(view: View) {
        binding.budgetDialogCategory.setText(arguments?.getString(KEY_CATEGORY))
        binding.budgetDialogYear.setText(arguments?.getString(KEY_YEAR_VALUE))
        val month = arguments?.getString(KEY_MONTH_VALUE)
        if (month == "0")
            binding.budgetDialogMonth.setText("")
        else
            binding.budgetDialogMonth.setText(month)
        val dec = DecimalFormat("#.00")
        var amtDouble: Double = 0.0
        val amt = arguments?.getString(KEY_AMOUNT_VALUE)
        if (amt != null)
            amtDouble = amt.toDouble()
        else
            amtDouble = 0.0
        binding.budgetDialogNewAmount.setText(dec.format(amtDouble))
        val who = arguments?.getString(KEY_WHO_VALUE)
        Log.d("Alex", "who is " + who)

        val whoRadioGroup = binding.budgetDialogNewWhoRadioGroup
        for (i in 0 until whoRadioGroup.childCount) {
            val o = whoRadioGroup.getChildAt(i)
            if (o is RadioButton) {
                Log.d("Alex", "o.text is " + o.text)
                if (o.text == who) {
                    o.isChecked = true
                }
            }
        }
    }

    private fun setupClickListeners(view: View) {
        binding.budgetDialogButton1.setOnClickListener {
            if (currentMode == "View") {
                binding.budgetDialogButton1.text = "Save"
                binding.budgetDialogButton2.text = "Cancel"
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.getChildCount()) {
                    (binding.budgetDialogNewWhoRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                }
                binding.budgetDialogNewAmount.isEnabled = true
                currentMode = "Edit"
            } else { // it's edit

                Log.d("Alex", "need to save new values")
                var tmpDouble1: Double = 0.0
                tmpDouble1 = binding.budgetDialogNewAmount.text.toString().toDouble()
                tmpDouble1 = round(tmpDouble1 * 100) / 100
                var amountInt: Int = (tmpDouble1 * 100.0).toInt()

                val selectedId = binding.budgetDialogNewWhoRadioGroup.checkedRadioButtonId
                var newWho: String = ""
                for (i in 0 until binding.budgetDialogNewWhoRadioGroup.childCount) {
                    val o = binding.budgetDialogNewWhoRadioGroup.getChildAt(i)
                    if (o is RadioButton)
                        if (o.isChecked)
                            newWho = o.text.toString()
                }
                Log.d("Alex", "oldwho is " + oldWho + " and newwho is " + newWho)
                if (oldWho == newWho) {
                    BudgetViewModel.updateBudget(
                        binding.budgetDialogCategory.text.toString(),
                        BudgetMonth(
                            oldYear, oldMonth
                        ).toString(),
                        newWho,
                        amountInt)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
                    mp.start()
                    dismiss()
                } else {
                    // check to see if there is already an entry for the new who.
                    var budgetListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.getValue() == null) { // nothing exists at this node so we can add it
                                Log.d("Alex", "NOthing exists at this node so we can add it ")
                                var prevMonth = BudgetMonth(
                                    binding.budgetDialogYear.text.toString().toInt(),
                                    binding.budgetDialogMonth.text.toString().toInt()
                                )
                                prevMonth.decrementMonth()
                                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                                    binding.budgetDialogCategory.text.toString(),
                                    prevMonth,
                                    newWho,
                                    true
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
                                    if (listener != null)
                                        listener?.onNewDataSaved()
                                    Log.d("Alex",
                                        "tmpDouble1 is same as previous month so just deleted the entry '" + tmpDouble1 + "'"
                                    )
                                } else { // new amount is different from previous month, so need to record it
                                    BudgetViewModel.updateBudget(
                                        binding.budgetDialogCategory.text.toString(),
                                        BudgetMonth(
                                            oldYear, oldMonth
                                        ).toString(),
                                        newWho,
                                        amountInt
                                    )
                                    if (listener != null)
                                        listener?.onNewDataSaved()
                                    Log.d("Alex", "tmpDouble1 is '" + tmpDouble1 + "'")
                                }
                                // delete oldWho
                                BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), BudgetMonth(
                                    oldYear, oldMonth).toString(), oldWho)
                                if (listener != null)
                                    listener?.onNewDataSaved()
                                dismiss()
                                val mp: MediaPlayer =
                                    MediaPlayer.create(context, R.raw.impact_jaw_breaker)
                                mp.start()
                            } else { // something exists so error
                                showErrorMessage(
                                    getParentFragmentManager(),
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
                    val dbRef =
                        MyApplication.databaseref.child("Users/" + MyApplication.userUID + "/NewBudget")
                            .child(binding.budgetDialogCategory.text.toString())
                            .child(
                                BudgetMonth(
                                    binding.budgetDialogYear.text.toString().toInt(),
                                    binding.budgetDialogMonth.text.toString().toInt()
                                ).toString()
                            )
                            .child(newWho)
                    dbRef.addListenerForSingleValueEvent(budgetListener);
                }
            }
        }
        binding.budgetDialogButton2.setOnClickListener() {
            if (currentMode == "View") { // ie user chose Delete
                fun yesClicked() {
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), BudgetMonth(
                        binding.budgetDialogYear.text.toString().toInt(),
                        binding.budgetDialogMonth.text.toString().toInt()
                    ).toString(), oldWho)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    Toast.makeText(activity, "Budget deleted", Toast.LENGTH_SHORT).show()
                    dismiss()
                    val mp: MediaPlayer = MediaPlayer.create(context, R.raw.short_springy_gun)
                    mp.start()
                }
                fun noClicked() {
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Are you sure?")
                    .setMessage("Are you sure that you want to delete this budget item?")
                    .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
                    .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
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