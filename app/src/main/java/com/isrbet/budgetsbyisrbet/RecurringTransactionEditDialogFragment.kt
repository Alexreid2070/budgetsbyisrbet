import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.*
import com.isrbet.budgetsbyisrbet.databinding.FragmentRecurringTransactionEditDialogBinding
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class RecurringTransactionEditDialogFragment() : DialogFragment() {
    interface RecurringTransactionEditDialogFragmentListener {

        fun onNewDataSaved()
    }
    private var listener: RecurringTransactionEditDialogFragmentListener? = null
    var cal = android.icu.util.Calendar.getInstance()

    companion object {
        private const val KEY_NAME = "KEY_NAME"
        private const val KEY_AMOUNT = "KEY_AMOUNT"
        private const val KEY_PERIOD = "KEY_PERIOD"
        private const val KEY_NEXTDATE = "KEY_NEXT_DATE"
        private const val KEY_REGULARITY = "KEY_REGULARITY"
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_SUBCATEGORY = "KEY_SUBCATEGORY"
        private const val KEY_PAIDBY = "KEY_PAIDBY"
        private const val KEY_BOUGHTFOR = "KEY_BOUGHTFOR"
        private var oldName: String = ""
        private var oldPeriod: String = ""
        private var oldAmount: Int = 0
        private var oldDate: String = ""
        private var oldRegularity: Int = 0
        private var oldCategory: String = ""
        private var oldSubcatgory: String = ""
        private var oldPaidBy: String = ""
        private var oldBoughtFor: String = ""
        fun newInstance(
            name: String,
            amount: Int,
            period: String,
            nextdate: String,
            regularity: Int,
            category: String,
            subcategory: String,
            paidby: String,
            boughtfor: String
        ): RecurringTransactionEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, name)
            args.putString(KEY_AMOUNT, amount.toString())
            args.putString(KEY_PERIOD, period)
            args.putString(KEY_NEXTDATE, nextdate)
            args.putString(KEY_REGULARITY, regularity.toString())
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_SUBCATEGORY, subcategory)
            args.putString(KEY_PAIDBY, paidby)
            args.putString(KEY_BOUGHTFOR, boughtfor)
            val fragment = RecurringTransactionEditDialogFragment()
            fragment.arguments = args
            oldName = name
            oldAmount = amount
            oldPeriod = period
            oldRegularity = regularity
            oldDate = nextdate
            oldCategory = category
            oldSubcatgory = subcategory
            oldPaidBy = paidby
            oldBoughtFor = boughtfor
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
        binding.editRtOldName.text = oldName
        binding.editRtNewName.setText(oldName)

        val dec = DecimalFormat("#.00")
        val formattedAmount = (oldAmount/100).toDouble() + (oldAmount % 100).toDouble()/100
        binding.editRtOldAmount.text = dec.format(formattedAmount)
        binding.editRtNewAmount.setText(dec.format(formattedAmount))
        binding.editRtOldPeriod.text = oldPeriod
        binding.editRtOldRegularity.text = oldRegularity.toString()
        binding.editRtNewRegularity.setText(oldRegularity.toString())
        binding.editRtOldNextDate.text = oldDate
        binding.editRtOldCategory.text = oldCategory
        binding.editRtOldSubcategory.text = oldSubcatgory
        binding.editRtOldPaidBy.text = oldPaidBy
        binding.editRtOldBoughtFor.text = oldBoughtFor
        setupClickListeners(view)
        binding.editRtNewName.requestFocus()

        val pSpinner:Spinner = binding.editRtNewPeriodSpinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PeriodValues
        )
        pSpinner.adapter = arrayAdapter
        pSpinner.setSelection(arrayAdapter.getPosition(oldPeriod))

        var categorySpinner:Spinner = binding.editRtNewCategory
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

        var paidBySpinner:Spinner = binding.editRtNewPaidBy
        val paidByArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getSpenders()
        )
        paidBySpinner.adapter = paidByArrayAdapter
        paidBySpinner.setSelection(paidByArrayAdapter.getPosition(oldPaidBy))

        var boughtForSpinner:Spinner = binding.editRtNewBoughtFor
        val boughtForArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SpenderViewModel.getSpenders()
        )
        boughtForSpinner.adapter = boughtForArrayAdapter
        boughtForSpinner.setSelection(boughtForArrayAdapter.getPosition(oldBoughtFor))

        if (oldName == "") { // ie this is an add, not an edit
            binding.editRtOldAmount.visibility = View.GONE
            binding.editRtOldPeriod.visibility = View.GONE
            binding.editRtOldRegularity.visibility = View.GONE
            binding.editRtOldNextDate.visibility = View.GONE
            binding.rtCurrentValueHeader.visibility = View.GONE
            binding.editRtOldPaidBy.visibility = View.GONE
            binding.editRtOldBoughtFor.visibility = View.GONE
        } else {
            val tOldDate = LocalDate.parse(oldDate, DateTimeFormatter.ISO_DATE)
            cal.set(Calendar.YEAR, tOldDate.year)
            cal.set(Calendar.MONTH, tOldDate.monthValue-1)
            cal.set(Calendar.DAY_OF_MONTH, tOldDate.dayOfMonth)
        }
        binding.editRtNewNextDate.setText(giveMeMyDateFormat(cal))

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editRtNewNextDate.setText(giveMeMyDateFormat(cal))
            }

        binding.editRtNewNextDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.editRtNewPeriodSpinner.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.editRtNewPeriodSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewCategory.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.editRtNewCategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewSubcategory.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.editRtNewSubcategory.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewPaidBy.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.editRtNewPaidBy.setPopupBackgroundResource(R.drawable.spinner)
        binding.editRtNewBoughtFor.setBackgroundColor(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        binding.editRtNewBoughtFor.setPopupBackgroundResource(R.drawable.spinner)

/*        binding.editRtNewName.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewAmount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewRegularity.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewNextDate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewPeriodSpinner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewCategory.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewSubcategory.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewPaidby.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
        binding.editRtNewBoughtfor.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.robin_egg_blue))
*/
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
        binding.rtDialogButtonSave.setOnClickListener {
            onSaveButtonClicked()
        }

        binding.rtDialogButtonDelete.setOnClickListener {
            fun yesClicked() {
                RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(binding.editRtOldName.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to delete this recurring transaction(" + binding.editRtOldName.text.toString() + ")?")
                .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
                .show()
        }

        binding.rtDialogButtonCancel.setOnClickListener() {
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
            if (oldDate != binding.editRtNewNextDate.text.toString()) {
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
            if (oldPaidBy != binding.editRtNewPaidBy.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "paidby", binding.editRtNewPaidBy.selectedItem.toString())
                somethingChanged = true
            }
            if (oldBoughtFor != binding.editRtNewBoughtFor.toString()) {
                RecurringTransactionViewModel.updateRecurringTransactionStringField(oldName, "boughtfor", binding.editRtNewBoughtFor.selectedItem.toString())
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
                    binding.editRtNewPaidBy.selectedItem.toString(),
                    binding.editRtNewBoughtFor.selectedItem.toString()
                )
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        } else if (oldName == "") { // ie this is an add
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(), amountInt, rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(), binding.editRtNewNextDate.text.toString(), binding.editRtNewCategory.selectedItem.toString(), binding.editRtNewSubcategory.selectedItem.toString(), binding.editRtNewPaidBy.selectedItem.toString(), binding.editRtNewBoughtFor.selectedItem.toString())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        } else if (oldName != binding.editRtNewName.text.toString()) {
            val rt = RecurringTransaction(binding.editRtNewName.text.toString().trim(), amountInt, rtSpinner.selectedItem.toString(), binding.editRtNewRegularity.text.toString().toInt(), binding.editRtNewNextDate.text.toString(), binding.editRtNewCategory.selectedItem.toString(), binding.editRtNewSubcategory.selectedItem.toString(), binding.editRtNewPaidBy.selectedItem.toString(), binding.editRtNewBoughtFor.selectedItem.toString())
            RecurringTransactionViewModel.addRecurringTransaction(rt)
            RecurringTransactionViewModel.deleteRecurringTransactionFromFirebase(oldName)
            if (listener != null) {
                listener?.onNewDataSaved()
            }
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }
    }

    fun setDialogFragmentListener(listener: RecurringTransactionEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}