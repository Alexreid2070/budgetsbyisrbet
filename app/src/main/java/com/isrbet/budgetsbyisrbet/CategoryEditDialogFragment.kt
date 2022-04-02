package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryEditDialogBinding
import java.util.ArrayList

const val cNEW_CATEGORY = "<add new category name>"

class CategoryEditDialogFragment : DialogFragment() {
    interface CategoryEditDialogFragmentListener {
        fun onNewDataSaved()
    }

    private var listener: CategoryEditDialogFragmentListener? = null
    private var _binding: FragmentCategoryEditDialogBinding? = null
    private val binding get() = _binding!!
    private var budgetCtr = 0
    private var rtCtr = 0
    private var expenseCtr = 0
    private var currentMode = "View"

    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_SUBCATEGORY = "KEY_SUBCATEGORY"
        private const val KEY_DISCTYPE = "KEY_DISCTYPE"
        private var oldCategoryID: Int = 0
        private var oldCategory: String = ""
        private var oldSubcategory: String = ""
        private var oldDisctype: String = ""
        fun newInstance(
            categoryID: String,
            category: String,
            subcategory: String,
            disctype: String
        ): CategoryEditDialogFragment {
            val args = Bundle()

            args.putString(KEY_CATEGORY_ID, categoryID)
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_SUBCATEGORY, subcategory)
            args.putString(KEY_DISCTYPE, disctype)
            val fragment = CategoryEditDialogFragment()
            fragment.arguments = args
            oldCategoryID = categoryID.toInt()
            oldCategory = category
            oldSubcategory = subcategory
            oldDisctype = disctype
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryEditDialogBinding.inflate(inflater, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_category_edit_dialog, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("Alex", "OnViewCreated oldCategory is '$oldCategory'")
        setupClickListeners()

        setupCategorySpinner(if (oldCategory == "") "first" else oldCategory)

        val hexColor = getColorInHex(
            MaterialColors.getColor(
                requireContext(),
                R.attr.editTextBackground,
                Color.BLACK
            ), "1F"
        )
        binding.editCategoryNewNameSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.editCategoryNewNameSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.editSubcategoryNewName.requestFocus()
        binding.editCategoryNewNameSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY) {
                    binding.categoryDialogLinearLayout3.visibility = View.VISIBLE
//                    binding.editCategoryNewName.requestFocus()
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                } else {
                    binding.categoryDialogLinearLayout3.visibility = View.GONE
                }
            }
        }

        val dtSpinner: Spinner = binding.editCategoryNewDisctypeSpinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            DiscTypeValues
        )
        dtSpinner.adapter = arrayAdapter
        if (oldCategory == "") { // ie this is an add, not an edit
            Log.d("Alex", "in blank")
            binding.oldCategoryLayout.visibility = View.GONE
            binding.categoryDialogButtonDelete.visibility = View.GONE
            binding.newNameHeader.text = "Add New Category"
            binding.categoryDialogButtonSave.text = "Save"
            currentMode = "Add"
        } else { // ie this is an edit
            binding.categoryId.text = oldCategoryID.toString()
            binding.oldCategoryName.text = oldCategory
            binding.oldSubcategoryName.text = oldSubcategory
            binding.oldDisctype.text = oldDisctype
            binding.editSubcategoryNewName.setText(oldSubcategory)
            dtSpinner.setSelection(arrayAdapter.getPosition(oldDisctype))
            binding.categoryDialogLinearLayoutBudget.visibility = View.GONE
            budgetCtr = BudgetViewModel.budgetExistsUsingCategory(oldCategoryID)
            expenseCtr = ExpenditureViewModel.expenditureExistsUsingCategory(oldCategoryID)
            rtCtr = RecurringTransactionViewModel.recurringTransactionExistsUsingCategory(
                oldCategoryID
            )
            if (budgetCtr + rtCtr + expenseCtr == 0)
                binding.messageCounter.text = "This category is not used."
            if (expenseCtr == 0)
                binding.messageExpenditure.visibility = View.GONE
            else
                binding.messageExpenditure.text = "$expenseCtr transaction(s)"
            if (budgetCtr == 0)
                binding.messageBudgetLayout.visibility = View.GONE
            else
                binding.messageBudget.text = if (budgetCtr == 0) "" else "$budgetCtr budget(s)"
            if (rtCtr == 0)
                binding.messageRtLayout.visibility = View.GONE
            else
                binding.messageRecurringTransaction.text =
                    if (rtCtr == 0) "" else "$rtCtr recurring transaction template(s)"
            if (oldCategoryID != DefaultsViewModel.getDefault(cDEFAULT_CATEGORY_ID).toInt())
                binding.messageDefaultCategoryLayout.visibility = View.GONE
            if (currentMode == "View") {
                binding.editCategoryOldNameHeader.text = CategoryViewModel.getFullCategoryName(binding.categoryId.text.toString().toInt())
                binding.categoryDialogNewHeaderLinearLayout.visibility = View.GONE
                binding.categoryDialogLinearLayout2.visibility = View.GONE
                binding.categoryDialogLinearLayout3.visibility = View.GONE
                binding.categoryDialogLinearLayout4.visibility = View.GONE
                binding.categoryDialogLinearLayout5.visibility = View.GONE
            }
        }
        dtSpinner.setBackgroundColor(Color.parseColor(hexColor))
        dtSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.categoryDialogOldHeaderLinearLayout.setBackgroundColor(DefaultsViewModel.getCategoryDetail(oldCategory).color)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupCategorySpinner(iSelection: String) {
        val categoryList: MutableList<String> = ArrayList()
        categoryList.add(cNEW_CATEGORY)
        CategoryViewModel.getCategoryNames(true).forEach {
            categoryList.add(it)
        }
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categoryList
        )
        binding.editCategoryNewNameSpinner.adapter = catArrayAdapter
        if (iSelection == "first")
            binding.editCategoryNewNameSpinner.setSelection(1)
        else
            binding.editCategoryNewNameSpinner.setSelection(catArrayAdapter.getPosition(iSelection))
        catArrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.categoryDialogButtonSave.setOnClickListener {
            if (currentMode == "View") { // change to edit
                binding.editCategoryOldNameHeader.text = "Current Details:"
                binding.categoryDialogButtonSave.text = "Save"
                binding.categoryDialogOldHeaderLinearLayout.visibility = View.VISIBLE
                binding.categoryDialogNewHeaderLinearLayout.visibility = View.VISIBLE
                binding.categoryDialogLinearLayout2.visibility = View.VISIBLE
                binding.categoryDialogLinearLayout3.visibility = View.VISIBLE
                binding.categoryDialogLinearLayout4.visibility = View.VISIBLE
                binding.categoryDialogLinearLayout5.visibility = View.VISIBLE
                binding.categoryDialogButtonDelete.visibility = View.GONE
                currentMode = "Edit"
            } else { // already in Edit mode, so Save...
                if (binding.editCategoryNewName.text.toString().contains("-")) {
                    binding.editCategoryNewName.error = "Category name has invalid character '-'."
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                    return@setOnClickListener
                }
                if (!textIsSafeForKey(binding.editCategoryNewName.text.toString())) {
                    binding.editCategoryNewName.error = "Category name has invalid characters."
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                    return@setOnClickListener
                }
                if (!textIsSafeForKey(binding.editSubcategoryNewName.text.toString())) {
                    binding.editSubcategoryNewName.error =
                        "Subcategory name has invalid characters."
                    focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                    return@setOnClickListener
                }
                if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY &&
                    binding.editCategoryNewName.text.toString() == ""
                ) {
                    binding.editCategoryNewName.error = "Enter new category name."
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                    return@setOnClickListener
                }
                if (binding.editSubcategoryNewName.text.toString() == "") {
                    binding.editSubcategoryNewName.error = "Enter new sub-category name."
                    focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                    return@setOnClickListener
                }
                val chosenCategory =
                    if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY)
                        binding.editCategoryNewName.text.toString().trim()
                    else
                        binding.editCategoryNewNameSpinner.selectedItem.toString()
                val dtSpinner: Spinner = binding.editCategoryNewDisctypeSpinner
                if (oldCategory == chosenCategory &&
                    oldSubcategory == binding.editSubcategoryNewName.text.toString() &&
                    oldDisctype != dtSpinner.selectedItem.toString()
                ) {
                    // disc type changed so update it
                    Log.d("Alex", "categoryID is ${oldCategoryID}")
                    CategoryViewModel.updateCategory(
                        binding.categoryId.text.toString().toInt(),
                        oldCategory,
                        oldSubcategory,
                        dtSpinner.selectedItem.toString()
                    )
                    if (listener != null)
                        listener?.onNewDataSaved()
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    dismiss()
                } else if (oldCategory == "") { // ie this is an add
                    if (CategoryViewModel.getID(
                            chosenCategory,
                            binding.editSubcategoryNewName.text.toString().trim()
                        ) != 0
                    ) {
                        binding.editSubcategoryNewName.error =
                            "Category / Subcategory of this name already exists."
                        focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                        return@setOnClickListener
                    }
                    val cat = CategoryViewModel.updateCategory(
                        0,
                        chosenCategory,
                        binding.editSubcategoryNewName.text.toString().trim(),
                        binding.editCategoryNewDisctypeSpinner.selectedItem.toString()
                    )
                    if (listener != null) {
                        listener?.onNewDataSaved()
                    }
/*                    val dateNow = Calendar.getInstance()
                    val bmNow =
                        BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH) + 1)
                    val newWho = if (SpenderViewModel.getActiveCount() > 1) 2 else 0
                    val tempDouble: Double =
                        binding.editCategoryBudgetAmount.text.toString().toDouble()

                    BudgetViewModel.updateBudget(
                        cat.id,
                        bmNow.toString(), newWho, tempDouble, cBUDGET_RECURRING
                    )*/
                    setupCategorySpinner(chosenCategory)
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    dismiss()
                    if (binding.switchEnterBudget.isChecked) {
                        val action =
                            CategoryFragmentDirections.actionCategoryFragmentToBudgetFragment()
                        action.categoryID = cat.id.toString()
                        findNavController().navigate(action)
                    }
                } else if (oldCategory != chosenCategory ||
                    oldSubcategory != binding.editSubcategoryNewName.text.toString()
                ) {
                    CategoryViewModel.updateCategory(
                        binding.categoryId.text.toString().toInt(),
                        chosenCategory,
                        binding.editSubcategoryNewName.text.toString().trim(),
                        dtSpinner.selectedItem.toString()
                    )
                    if (listener != null)
                        listener?.onNewDataSaved()
                    setupCategorySpinner(chosenCategory)
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    dismiss()
                }
            }
        }
        binding.categoryDialogButtonDelete.setOnClickListener {
            if (rtCtr > 0) {
                tellUserCantDelete("Recurring Transactions")
                return@setOnClickListener
            }
            if (budgetCtr > 0) {
                tellUserCantDelete("Budgets")
                return@setOnClickListener
            }
            if (expenseCtr > 0) {
                tellUserCantDelete("Expenditures")
                return@setOnClickListener
            }

            fun yesClicked() {
                CategoryViewModel.deleteCategoryAndSubcategory(
                    binding.categoryId.text.toString().toInt()
                )
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
                .setMessage(
                    "Are you sure that you want to delete " +
                            binding.oldCategoryName.text.toString() + "-" +
                            binding.oldSubcategoryName.text.toString() + "?"
                )
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.categoryDialogButtonCancel.setOnClickListener {
            dismiss()
        }
        binding.messageBudget.setOnClickListener {
            val action = CategoryFragmentDirections.actionCategoryFragmentToBudgetViewAllFragment()
            action.categoryID = oldCategoryID.toString()
            dismiss()
            findNavController().navigate(action)
        }
        binding.messageExpenditure.setOnClickListener {
            val action =
                CategoryFragmentDirections.actionCategoryFragmentToTransactionViewAllFragment()
            action.categoryID = oldCategoryID.toString()
            dismiss()
            findNavController().navigate(action)
        }
        binding.messageRecurringTransaction.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.RecurringTransactionFragment)
        }
    }

    private fun tellUserCantDelete(iWhere: String) {
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Can't delete ${binding.oldCategoryName.text}-${binding.oldSubcategoryName.text}")
            .setMessage("${binding.oldCategoryName.text}-${binding.oldSubcategoryName.text} cannot be deleted as it is used in $iWhere.")
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    fun setCategoryEditDialogFragmentListener(listener: CategoryEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}