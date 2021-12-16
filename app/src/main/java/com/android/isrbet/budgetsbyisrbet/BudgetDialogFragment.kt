import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.isrbet.budgetsbyisrbet.BudgetMonth
import com.isrbet.budgetsbyisrbet.BudgetViewModel
import com.isrbet.budgetsbyisrbet.R
import kotlinx.android.synthetic.main.fragment_budget_edit_dialog.view.*
import kotlin.math.round

class BudgetDialogViewModel : ViewModel() {
    val mym1: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>()
    }
    val mym2: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>()
    }
    val mym3: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>()
    }
    val mym4: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>()
    }
}

class BudgetDialogFragment : DialogFragment() {
    private val budgetDialogViewModel: BudgetDialogViewModel by activityViewModels()

    companion object {
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_M1_HEADING = "KEY_M1_HEADING"
        private const val KEY_M2_HEADING = "KEY_M2_HEADING"
        private const val KEY_M3_HEADING = "KEY_M3_HEADING"
        private const val KEY_M4_HEADING = "KEY_M4_HEADING"
        private const val KEY_M1_VALUE = "KEY_M1_VALUE"
        private const val KEY_M2_VALUE = "KEY_M2_VALUE"
        private const val KEY_M3_VALUE = "KEY_M3_VALUE"
        private const val KEY_M4_VALUE = "KEY_M4_VALUE"
        private var oldM1: Double = 0.0
        private var oldM2: Double = 0.0
        private var oldM3: Double = 0.0
        private var oldM4: Double = 0.0

        fun newInstance(
            category: String,
            m1_heading: String, m1_value: String,
            m2_heading: String, m2_value: String,
            m3_heading: String, m3_value: String,
            m4_heading: String, m4_value: String
        ): BudgetDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_M1_HEADING, m1_heading)
            args.putString(KEY_M1_VALUE, m1_value)
            args.putString(KEY_M2_HEADING, m2_heading)
            args.putString(KEY_M2_VALUE, m2_value)
            args.putString(KEY_M3_HEADING, m3_heading)
            args.putString(KEY_M3_VALUE, m3_value)
            args.putString(KEY_M4_HEADING, m4_heading)
            args.putString(KEY_M4_VALUE, m4_value)
            val fragment = BudgetDialogFragment()
            fragment.arguments = args
            oldM1 = m1_value.toDouble()
            oldM2 = m2_value.toDouble()
            oldM3 = m3_value.toDouble()
            oldM4 = m4_value.toDouble()
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
        setupClickListeners(view)
        view.budget_dialog_m1_value.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupView(view: View) {
        view.budget_dialog_category.text = arguments?.getString(KEY_CATEGORY)
        view.budget_dialog_m1_heading.text = arguments?.getString(KEY_M1_HEADING)
        view.budget_dialog_m2_heading.text = arguments?.getString(KEY_M2_HEADING)
        view.budget_dialog_m3_heading.text = arguments?.getString(KEY_M3_HEADING)
        view.budget_dialog_m4_heading.text = arguments?.getString(KEY_M4_HEADING)
        view.budget_dialog_m1_value.setText(arguments?.getString(KEY_M1_VALUE))
        view.budget_dialog_m2_value.setText(arguments?.getString(KEY_M2_VALUE))
        view.budget_dialog_m3_value.setText(arguments?.getString(KEY_M3_VALUE))
        view.budget_dialog_m4_value.setText(arguments?.getString(KEY_M4_VALUE))
    }

    private fun setupClickListeners(view: View) {
        view.budget_dialog_button_save.setOnClickListener {
            Log.d("Alex", "need to save new values")
            var tmpDouble1: Double = 0.0
            if (view.budget_dialog_m1_value.text.toString() != "")
                tmpDouble1 = view.budget_dialog_m1_value.text.toString().toDouble()
            tmpDouble1 = round(tmpDouble1 * 100) / 100

            if (oldM1 != tmpDouble1) {
                var prevMonth = BudgetMonth(view.budget_dialog_m1_heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    view.budget_dialog_category.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble1 '" + tmpDouble1.toString() + "'")
                if (tmpDouble1 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m1_heading.text.toString())
                    Log.d("Alex", "tmpDouble1 is same as previous month so just deleted the entry '" + tmpDouble1 + "'")
                    budgetDialogViewModel.mym1.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m1_heading.text.toString(), tmpDouble1*100)
                    Log.d("Alex", "tmpDouble1 is '" + tmpDouble1 + "'")
                    budgetDialogViewModel.mym1.value = tmpDouble1*100
                }
            }
            var tmpDouble2: Double = 0.0
            if (view.budget_dialog_m2_value.text.toString() != "")
                tmpDouble2 = view.budget_dialog_m2_value.text.toString().toDouble()
            tmpDouble2 = round(tmpDouble2 * 100) / 100
            if (oldM2 != tmpDouble2) {
                var prevMonth = BudgetMonth(view.budget_dialog_m2_heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    view.budget_dialog_category.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble2 '" + tmpDouble2.toString() + "'")
                if (tmpDouble2 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m2_heading.text.toString())
                    Log.d("Alex", "tmpDouble2 is same as previous month so just deleted the entry '" + tmpDouble2 + "'")
                    budgetDialogViewModel.mym2.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m2_heading.text.toString(), tmpDouble2*100)
                    budgetDialogViewModel.mym2.value = tmpDouble2*100
                }
            }
            var tmpDouble3: Double = 0.0
            if (view.budget_dialog_m3_value.text.toString() != "")
                tmpDouble3 = view.budget_dialog_m3_value.text.toString().toDouble()
            tmpDouble3 = round(tmpDouble3 * 100) / 100
            if (oldM3 != tmpDouble3) {
                var prevMonth = BudgetMonth(view.budget_dialog_m3_heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    view.budget_dialog_category.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble3 '" + tmpDouble3.toString() + "'")
                if (tmpDouble3 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m3_heading.text.toString())
                    Log.d("Alex", "tmpDouble3 is same as previous month so just deleted the entry '" + tmpDouble3 + "'")
                    budgetDialogViewModel.mym3.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m3_heading.text.toString(), tmpDouble3*100)
                    budgetDialogViewModel.mym3.value = tmpDouble3*100
                }
            }
            var tmpDouble4: Double = 0.0
            if (view.budget_dialog_m4_value.text.toString() != "")
                tmpDouble4 = view.budget_dialog_m4_value.text.toString().toDouble()
            tmpDouble4 = round(tmpDouble4 * 100) / 100
            if (oldM4 != tmpDouble4) {
                var prevMonth = BudgetMonth(view.budget_dialog_m4_heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    view.budget_dialog_category.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble4 '" + tmpDouble4.toString() + "'")
                if (tmpDouble4 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m4_heading.text.toString())
                    Log.d("Alex", "tmpDouble4 is same as previous month so just deleted the entry '" + tmpDouble4 + "'")
                    budgetDialogViewModel.mym4.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(view.budget_dialog_category.text.toString(), view.budget_dialog_m4_heading.text.toString(), tmpDouble4*100)
                    budgetDialogViewModel.mym4.value = tmpDouble4*100
                }
            }
            dismiss()
        }
        view.budget_dialog_button_cancel.setOnClickListener() {
            dismiss()
        }
    }
}