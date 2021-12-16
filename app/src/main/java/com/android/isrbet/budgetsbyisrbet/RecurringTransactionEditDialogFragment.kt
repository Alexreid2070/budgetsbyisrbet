import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.*
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionEditDialogBinding
import kotlinx.android.synthetic.main.fragment_recurring_transaction_edit_dialog.view.*
import java.text.DecimalFormat
import kotlin.math.round

class RecurringTransactionEditDialogFragment() : DialogFragment() {
    interface RecurringTransactionEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: RecurringTransactionEditDialogFragmentListener? = null

    companion object {
        private const val KEY_NAME = "KEY_NAME"
        private const val KEY_AMOUNT = "KEY_AMOUNT"
        private const val KEY_PERIOD = "KEY_PERIOD"
        private const val KEY_NEXTDATE = "KEY_NEXT_DATE"
        private const val KEY_REGULARITY = "KEY_REGULARITY"
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_SUBCATEGORY = "KEY_SUBCATEGORY"
        private const val KEY_WHO = "KEY_WHO"
        private var oldName: String = ""
        private var oldPeriod: String = ""
        private var oldAmount: Int = 0
        private var oldDate: String = ""
        private var oldRegularity: Int = 0
        private var oldCategory: String = ""
        private var oldSubcatgory: String = ""
        private var oldWho: String = ""
        fun newInstance(
            name: String,
            amount: Int,
            period: String,
            nextdate: String,
            regularity: Int,
            category: String,
            subcategory: String,
            who: String
        ): RecurringTransactionEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, name)
            args.putString(KEY_AMOUNT, amount.toString())
            args.putString(KEY_PERIOD, period)
            args.putString(KEY_NEXTDATE, nextdate)
            args.putString(KEY_REGULARITY, regularity.toString())
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_SUBCATEGORY, subcategory)
            args.putString(KEY_WHO, who)
            val fragment = RecurringTransactionEditDialogFragment()
            fragment.arguments = args
            oldName = name
            oldAmount = amount
            oldPeriod = period
            oldRegularity = regularity
            oldDate = nextdate
            oldCategory = category
            oldSubcatgory = subcategory
            oldWho = who
            return fragment
        }
    }

    private var _binding: FragmentRecurringTransactionEditDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecurringTransactionEditDialogBinding.inflate(inflater, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_recurring_transaction_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.edit_rt_old_name.text = oldName
        view.edit_rt_new_name.setText(oldName)

        val dec = DecimalFormat("#.00")
        val formattedAmount = (oldAmount/100).toDouble() + (oldAmount % 100).toDouble()/100
        view.edit_rt_old_amount.text = dec.format(formattedAmount)

        view.edit_rt_new_amount.setText(dec.format(formattedAmount))
        view.edit_rt_old_period.text = oldPeriod
        view.edit_rt_old_regularity.text = oldRegularity.toString()
        view.edit_rt_new_regularity.setText(oldRegularity.toString())
        view.edit_rt_old_next_date.text = oldDate
        view.edit_rt_new_next_date.setText(oldDate)
        view.edit_rt_old_category.text = oldCategory
        view.edit_rt_old_subcategory.text = oldSubcatgory
        view.edit_rt_old_who.text = oldWho
        setupClickListeners(view)
        view.edit_rt_new_name.requestFocus()

        val pSpinner:Spinner = view.edit_rt_new_period_spinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PeriodValues
        )
        pSpinner.adapter = arrayAdapter
        pSpinner.setSelection(arrayAdapter.getPosition(oldPeriod))

        var categorySpinner:Spinner = view.edit_rt_new_category
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryNames()
        )
        categorySpinner.adapter = catArrayAdapter
        if (oldCategory == "")
            categorySpinner.setSelection(0)
        else
            categorySpinner.setSelection(catArrayAdapter.getPosition(oldCategory))

/*        var subCategorySpinner:Spinner = view.edit_rt_new_subcategory
        val subCatArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            CategoryViewModel.getSubcategoriesForSpinner(oldCategory)
        )
        subCategorySpinner.adapter = subCatArrayAdapter
        subCategorySpinner.setSelection(subCatArrayAdapter.getPosition(oldSubcatgory)) */

        var whoSpinner:Spinner = view.edit_rt_new_who
        val whoArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getSpenders()
        )
        whoSpinner.adapter = whoArrayAdapter
        whoSpinner.setSelection(whoArrayAdapter.getPosition(oldWho))

        if (oldName == "") { // ie this is an add, not an edit
            binding.editRtOldAmount.visibility = View.GONE
            binding.editRtOldPeriod.visibility = View.GONE
            binding.editRtOldRegularity.visibility = View.GONE
            binding.editRtOldNextDate.visibility = View.GONE
            binding.rtCurrentValueHeader.visibility = View.GONE
        }
        binding.editRtNewName.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewRegularity.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewNextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewPeriodSpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewCategory.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewSubcategory.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewWho.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))

        binding.editRtNewCategory.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                addSubCategories(binding.editRtNewCategory.selectedItem.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addSubCategories(iCategory: String) {
        var subCategorySpinner = binding.editRtNewSubcategory
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, CategoryViewModel.getSubcategoriesForSpinner(iCategory))
        subCategorySpinner.adapter = arrayAdapter
        if (oldSubcatgory == "")
            subCategorySpinner.setSelection(0)
        else
            subCategorySpinner.setSelection(arrayAdapter.getPosition(oldSubcatgory))

        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged()
        }
    }

    private fun setupClickListeners(view: View) {
        view.rt_dialog_button_save.setOnClickListener {
            onSaveButtonClicked()
        }

        view.rt_dialog_button_delete.setOnClickListener {
            fun yesClicked() {
                RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(view.edit_rt_old_name.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to delete this recurring transaction(" + view.edit_rt_old_name.text.toString() + ")?")
                .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
                .show()
        }

        view.rt_dialog_button_cancel.setOnClickListener() {
            dismiss()
        }
    }

    fun onSaveButtonClicked() {
        val rtSpinner:Spinner = binding.editRtNewPeriodSpinner
        var amountInt: Int
        var somethingChanged = false
        amountInt = round(binding.editRtNewAmount.text.toString().toDouble()*100).toInt()

        if (!textIsSafe(binding.editRtNewName.text.toString())) {
            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            focusAndOpenSoftKeyboard(requireContext(), binding.editRtNewName)
            return
        }

        if (oldName == binding.editRtNewName.text.toString()) {
            if (oldAmount != amountInt) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "amount", amountInt)
                somethingChanged = true
            }
            if (oldPeriod != rtSpinner.selectedItem.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "period", rtSpinner.selectedItem.toString())
                somethingChanged = true
            }
            if (oldDate != binding.editRtNewNextDate.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "nextdate", binding.editRtNewNextDate.text.toString())
                somethingChanged = true
            }
            if (oldCategory != binding.editRtNewCategory.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "category", binding.editRtNewCategory.selectedItem.toString())
                somethingChanged = true
            }
            if (oldSubcatgory != binding.editRtNewSubcategory.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "subcategory", binding.editRtNewSubcategory.selectedItem.toString())
                somethingChanged = true
            }
            if (oldWho != binding.editRtNewWho.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "who", binding.editRtNewWho.selectedItem.toString())
                somethingChanged = true
            }
            if (oldRegularity != binding.editRtNewRegularity.text.toString().toInt()) {
                RecurringTransactionViewModel.updateRecurringTransactionIntField(oldName, "regularity", binding.editRtNewRegularity.text.toString().toInt())
                somethingChanged = true
            }
            if (somethingChanged) {
                RecurringTransactionViewModel.updateRecurringTransaction(
                    oldName,
                    amountInt,
                    rtSpinner.selectedItem.toString(),
                    binding.editRtNewNextDate.text.toString(),
                    binding.editRtNewRegularity.text.toString().toInt(),
                    binding.editRtNewCategory.selectedItem.toString(),
                    binding.editRtNewSubcategory.selectedItem.toString(),
                    binding.editRtNewWho.selectedItem.toString()
                )
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()

            }
        } else if (oldName == "") { // ie this is an add
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(), amountInt, rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(), binding.editRtNewNextDate.text.toString(), binding.editRtNewCategory.selectedItem.toString(), binding.editRtNewSubcategory.selectedItem.toString(), binding.editRtNewWho.selectedItem.toString())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            dismiss()
        } else if (oldName != binding.editRtNewName.text.toString()) {
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(), amountInt, rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(), binding.editRtNewNextDate.text.toString(), binding.editRtNewCategory.selectedItem.toString(), binding.editRtNewSubcategory.selectedItem.toString(), binding.editRtNewWho.selectedItem.toString())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(oldName)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            dismiss()
        }
    }

    fun setDialogFragmentListener(listener: RecurringTransactionEditDialogFragmentListener) {
        this.listener = listener
    }
}