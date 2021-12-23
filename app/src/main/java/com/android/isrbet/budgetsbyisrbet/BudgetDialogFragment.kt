import android.media.MediaPlayer
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
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetEditDialogBinding
import com.isrbet.budgetsbyisrbet.databinding.FragmentDashboardBinding
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
    private var _binding: FragmentBudgetEditDialogBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentBudgetEditDialogBinding.inflate(inflater, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_budget_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
        setupClickListeners(view)
        binding.budgetDialogM1Value.requestFocus()
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
        binding.budgetDialogM1Heading.setText(arguments?.getString(KEY_M1_HEADING))
        binding.budgetDialogM2Heading.setText(arguments?.getString(KEY_M2_HEADING))
        binding.budgetDialogM3Heading.setText(arguments?.getString(KEY_M3_HEADING))
        binding.budgetDialogM4Heading.setText(arguments?.getString(KEY_M4_HEADING))
        binding.budgetDialogM1Value.setText(arguments?.getString(KEY_M1_VALUE))
        binding.budgetDialogM2Value.setText(arguments?.getString(KEY_M2_VALUE))
        binding.budgetDialogM3Value.setText(arguments?.getString(KEY_M3_VALUE))
        binding.budgetDialogM4Value.setText(arguments?.getString(KEY_M4_VALUE))
    }

    private fun setupClickListeners(view: View) {
        binding.budgetDialogButtonSave.setOnClickListener {
            Log.d("Alex", "need to save new values")
            var tmpDouble1: Double = 0.0
            if (binding.budgetDialogM1Value.text.toString() != "")
                tmpDouble1 = binding.budgetDialogM1Value.text.toString().toDouble()
            tmpDouble1 = round(tmpDouble1 * 100) / 100

            if (oldM1 != tmpDouble1) {
                var prevMonth = BudgetMonth(binding.budgetDialogM1Heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    binding.budgetDialogCategory.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble1 '" + tmpDouble1.toString() + "'")
                if (tmpDouble1 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM1Heading.text.toString())
                    Log.d("Alex", "tmpDouble1 is same as previous month so just deleted the entry '" + tmpDouble1 + "'")
                    budgetDialogViewModel.mym1.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM1Heading.text.toString(), tmpDouble1*100)
                    Log.d("Alex", "tmpDouble1 is '" + tmpDouble1 + "'")
                    budgetDialogViewModel.mym1.value = tmpDouble1*100
                }
            }
            var tmpDouble2: Double = 0.0
            if (binding.budgetDialogM2Value.text.toString() != "")
                tmpDouble2 = binding.budgetDialogM2Value.text.toString().toDouble()
            tmpDouble2 = round(tmpDouble2 * 100) / 100
            if (oldM2 != tmpDouble2) {
                var prevMonth = BudgetMonth(binding.budgetDialogM2Heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    binding.budgetDialogCategory.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble2 '" + tmpDouble2.toString() + "'")
                if (tmpDouble2 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM2Heading.text.toString())
                    Log.d("Alex", "tmpDouble2 is same as previous month so just deleted the entry '" + tmpDouble2 + "'")
                    budgetDialogViewModel.mym2.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM2Heading.text.toString(), tmpDouble2*100)
                    budgetDialogViewModel.mym2.value = tmpDouble2*100
                }
            }
            var tmpDouble3: Double = 0.0
            if (binding.budgetDialogM3Value.text.toString() != "")
                tmpDouble3 = binding.budgetDialogM3Value.text.toString().toDouble()
            tmpDouble3 = round(tmpDouble3 * 100) / 100
            if (oldM3 != tmpDouble3) {
                var prevMonth = BudgetMonth(binding.budgetDialogM3Heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    binding.budgetDialogCategory.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble3 '" + tmpDouble3.toString() + "'")
                if (tmpDouble3 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM3Heading.text.toString())
                    Log.d("Alex", "tmpDouble3 is same as previous month so just deleted the entry '" + tmpDouble3 + "'")
                    budgetDialogViewModel.mym3.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM3Heading.text.toString(), tmpDouble3*100)
                    budgetDialogViewModel.mym3.value = tmpDouble3*100
                }
            }
            var tmpDouble4: Double = 0.0
            if (binding.budgetDialogM4Value.text.toString() != "")
                tmpDouble4 = binding.budgetDialogM4Value.text.toString().toDouble()
            tmpDouble4 = round(tmpDouble4 * 100) / 100
            if (oldM4 != tmpDouble4) {
                var prevMonth = BudgetMonth(binding.budgetDialogM4Heading.text.toString())
                prevMonth.decrementMonth()
                var tmpPrevAmt = BudgetViewModel.getBudgetAmount(
                    binding.budgetDialogCategory.text.toString(),prevMonth)
                Log.d("Alex", "tmpPrevAmt '" + tmpPrevAmt.amount.toString() + "' tmpDouble4 '" + tmpDouble4.toString() + "'")
                if (tmpDouble4 == tmpPrevAmt.amount) {
                    // ie new amount is same as previous month, so we can just delete this month's change
                    BudgetViewModel.deleteBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM4Heading.text.toString())
                    Log.d("Alex", "tmpDouble4 is same as previous month so just deleted the entry '" + tmpDouble4 + "'")
                    budgetDialogViewModel.mym4.value = tmpPrevAmt.amount
                } else { // new amount is different from previous month, so need to record it
                    BudgetViewModel.updateBudget(binding.budgetDialogCategory.text.toString(), binding.budgetDialogM4Heading.text.toString(), tmpDouble4*100)
                    budgetDialogViewModel.mym4.value = tmpDouble4*100
                }
            }
            dismiss()
            val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
            mp.start()
        }
        binding.budgetDialogButtonCancel.setOnClickListener() {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}