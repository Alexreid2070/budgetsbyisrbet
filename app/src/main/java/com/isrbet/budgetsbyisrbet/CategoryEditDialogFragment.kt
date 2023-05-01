package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryEditDialogBinding
import timber.log.Timber
import java.util.ArrayList

class CategoryEditDialogFragment : DialogFragment() {
    private var _binding: FragmentCategoryEditDialogBinding? = null
    private val binding get() = _binding!!
    private var budgetCtr = 0
    private var spCtr = 0
    private var expenseCtr = 0
    private var currentMode = cMODE_VIEW

    companion object {
        private const val KEY_CATEGORY_ID = "1"
        private var oldCategoryID: Int = 0
        fun newInstance(
            categoryID: Int
        ): CategoryEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY_ID, categoryID.toString())
            val fragment = CategoryEditDialogFragment()
            fragment.arguments = args
            oldCategoryID = categoryID
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()

        val hexColor = getColorInHex(
            MaterialColors.getColor(
                requireContext(),
                R.attr.editTextBackground,
                Color.BLACK
            ), cOpacity
        )
        if (SpenderViewModel.twoDistinctUsers())
            binding.privacyLayout.visibility = View.VISIBLE
        else
            binding.privacyLayout.visibility = View.GONE
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
                if (binding.editCategoryNewNameSpinner.selectedItem.toString() ==
                    getString(R.string.add_new_category_name)) {
                    binding.categoryDialogLinearLayout3.visibility = View.VISIBLE
//                    binding.editCategoryNewName.requestFocus()
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                } else {
                    binding.categoryDialogLinearLayout3.visibility = View.GONE
                }
            }
        }

        val dtSpinner: Spinner = binding.editCategoryNewDisctypeSpinner
        val discTypeValues = listOf(getString(R.string.discretionary), getString(R.string.non_discretionary))
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            discTypeValues
        )
        dtSpinner.adapter = arrayAdapter
        val oldCat = CategoryViewModel.getCategory(oldCategoryID)
        if (oldCategoryID == 0) { // ie this is an add, not an edit
            setupCategorySpinner(getString(R.string.lcfirst))
            binding.privacySwitch.isChecked = false
            binding.stateSwitch.isChecked = true

            binding.oldCategoryLayout.visibility = View.GONE
            binding.newNameHeader.text = getString(R.string.add_category)
            binding.buttonDelete.visibility = View.GONE
            binding.buttonDeleteView.visibility = View.GONE
            binding.buttonSave.visibility = View.VISIBLE
            binding.buttonEdit.visibility = View.GONE
            currentMode = cMODE_ADD
            if (SpenderViewModel.twoDistinctUsers())
                binding.privacySwitch.visibility = View.VISIBLE
            else
                binding.privacySwitch.visibility = View.GONE
        } else { // ie this is a view
            binding.buttonEdit.visibility = View.VISIBLE
            binding.buttonSave.visibility = View.GONE
            setupCategorySpinner(oldCat?.categoryName ?: getString(R.string.lcfirst))
            binding.privacySwitch.isChecked = oldCat?.private != 2
            binding.stateSwitch.isChecked = oldCat?.inUse == true

            binding.categoryId.text = oldCategoryID.toString()
            binding.oldCategoryName.text = oldCat?.categoryName
            binding.oldSubcategoryName.text = oldCat?.subcategoryName
            binding.oldDisctype.text =
                if (oldCat?.discType == cDiscTypeDiscretionary)
                    getString(R.string.discretionary)
                else
                    getString(R.string.non_discretionary)
            binding.oldPrivacy.text = if (oldCat?.private == 2) getString(R.string.not_private) else getString(R.string.ucprivate)
            binding.oldState.text = if (oldCat?.inUse == true) getString(R.string.yes) else getString(R.string.ucnot_in_use)
            if (oldCat?.inUse != true) {
                binding.oldState.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red))
            }
            binding.editSubcategoryNewName.setText(oldCat?.subcategoryName)
            if (oldCat?.discType == cDiscTypeDiscretionary)
                dtSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.discretionary)))
            else
                dtSpinner.setSelection(arrayAdapter.getPosition(getString(R.string.non_discretionary)))
            binding.switchEnterBudget.visibility = View.GONE
            budgetCtr = BudgetViewModel.budgetExistsUsingCategory(oldCategoryID)
            expenseCtr = TransactionViewModel.transactionExistsUsingCategory(oldCategoryID)
            spCtr = ScheduledPaymentViewModel.scheduledPaymentExistsUsingCategory(
                oldCategoryID
            )
            if (budgetCtr + spCtr + expenseCtr == 0)
                binding.messageCounter.text = getString(R.string.this_category_is_not_used)
            if (expenseCtr == 0)
                binding.messageTransaction.visibility = View.GONE
            else
                binding.messageTransaction.text = String.format("$expenseCtr ${getString(R.string.transaction_psp)}")
            if (budgetCtr == 0)
                binding.messageBudget.visibility = View.GONE
            else
                binding.messageBudget.text = if (budgetCtr == 0) "" else "$budgetCtr " + getString(R.string.budget_psp)
            if (spCtr == 0)
                binding.messageScheduledPayment.visibility = View.GONE
            else
                binding.messageScheduledPayment.text =
                    if (spCtr == 0) "" else "$spCtr " + getString(R.string.scheduled_payment_template_psp)
            if (oldCategoryID != DefaultsViewModel.getDefaultCategory())
                binding.messageDefaultCategory.visibility = View.GONE
            if (currentMode == cMODE_VIEW) {
                binding.editCategoryOldNameHeader.text = CategoryViewModel.getFullCategoryName(binding.categoryId.text.toString().toInt())
                binding.categoryDialogNewHeaderLinearLayout.visibility = View.GONE
                binding.categoryDialogLinearLayout2.visibility = View.GONE
                binding.categoryDialogLinearLayout3.visibility = View.GONE
                binding.categoryDialogLinearLayout4.visibility = View.GONE
                binding.categoryDialogLinearLayout5.visibility = View.GONE
                binding.privacySwitch.visibility = View.GONE
                binding.stateSwitch.visibility = View.GONE
                binding.categoryLayout.visibility = View.GONE
                binding.subcategoryLayout.visibility = View.GONE
            }
        }
        dtSpinner.setBackgroundColor(Color.parseColor(hexColor))
        dtSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.categoryDialogOldHeaderLinearLayout.setBackgroundColor(DefaultsViewModel.getCategoryDetail(oldCat?.categoryName.toString()).color)
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
        categoryList.add(getString(R.string.add_new_category_name))
        CategoryViewModel.getCategoryNames(true).forEach {
            categoryList.add(it)
        }
        val catArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categoryList
        )
        binding.editCategoryNewNameSpinner.adapter = catArrayAdapter
        if (iSelection == getString(R.string.lcfirst))
            binding.editCategoryNewNameSpinner.setSelection(1)
        else
            binding.editCategoryNewNameSpinner.setSelection(catArrayAdapter.getPosition(iSelection))
        catArrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.buttonEdit.setOnClickListener {
            binding.buttonEdit.visibility = View.GONE
            binding.buttonSave.visibility = View.VISIBLE
            binding.categoryDialogOldHeaderLinearLayout.visibility = View.VISIBLE
            binding.categoryDialogNewHeaderLinearLayout.visibility = View.VISIBLE
            binding.categoryDialogLinearLayout2.visibility = View.VISIBLE
            binding.categoryDialogLinearLayout3.visibility = View.VISIBLE
            binding.categoryDialogLinearLayout4.visibility = View.VISIBLE
            binding.categoryDialogLinearLayout5.visibility = View.VISIBLE
            if (SpenderViewModel.twoDistinctUsers())
                binding.privacySwitch.visibility = View.VISIBLE
            else
                binding.privacySwitch.visibility = View.GONE
            binding.stateSwitch.visibility = View.VISIBLE
            binding.buttonDelete.visibility = View.GONE
            binding.buttonDeleteView.visibility = View.GONE

            currentMode = cMODE_EDIT
        }
        binding.buttonSave.setOnClickListener {
            val oldCat = CategoryViewModel.getCategory(oldCategoryID)
            if (binding.editCategoryNewName.text.toString().contains("-")) {
                binding.editCategoryNewName.error = getString(R.string.field_has_invalid_character)
                focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                return@setOnClickListener
            }
            if (!textIsSafeForKey(binding.editCategoryNewName.text.toString())) {
                binding.editCategoryNewName.error = getString(R.string.field_has_invalid_character)
                focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                return@setOnClickListener
            }
            if (!textIsSafeForKey(binding.editSubcategoryNewName.text.toString())) {
                binding.editSubcategoryNewName.error = getString(R.string.field_has_invalid_character)
                focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                return@setOnClickListener
            }
            if (binding.editCategoryNewNameSpinner.selectedItem.toString() ==
                getString(R.string.add_new_category_name) &&
                binding.editCategoryNewName.text.toString() == ""
            ) {
                binding.editCategoryNewName.error = getString(R.string.enter_new_category_name)
                focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                return@setOnClickListener
            }
            if (binding.editSubcategoryNewName.text.toString() == "") {
                binding.editSubcategoryNewName.error = getString(R.string.enter_new_subcategory_name)
                focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                return@setOnClickListener
            }
            val chosenCategory =
                if (binding.editCategoryNewNameSpinner.selectedItem.toString() ==
                    getString(R.string.add_new_category_name))
                    binding.editCategoryNewName.text.toString().trim()
                else
                    binding.editCategoryNewNameSpinner.selectedItem.toString()
            val dtSpinner: Spinner = binding.editCategoryNewDisctypeSpinner
            val chosenDiscType = if (dtSpinner.selectedItem.toString() == getString(R.string.discretionary))
                cDiscTypeDiscretionary
            else
                cDiscTypeNondiscretionary

            if (oldCategoryID == 0) { // ie this is an add
                if (CategoryViewModel.getID(
                        chosenCategory,
                        binding.editSubcategoryNewName.text.toString().trim()
                    ) != 0
                ) {
                    binding.editSubcategoryNewName.error = getString(R.string.name_already_exists)
                    focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                    return@setOnClickListener
                }
                val cat = CategoryViewModel.updateCategory(
                    0,
                    chosenCategory,
                    binding.editSubcategoryNewName.text.toString().trim(),
                    chosenDiscType,
                    if (binding.privacySwitch.isChecked) MyApplication.userIndex else 2,
                    binding.stateSwitch.isChecked
                )
                setupCategorySpinner(chosenCategory)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
                if (binding.switchEnterBudget.isChecked) {
                    val action =
                        SettingsTabsFragmentDirections.actionSettingsTabFragmentToBudgetFragment()
                    action.categoryID = cat.id.toString()
                    Timber.tag("Alex").d("Calling budget fragment with id ${action.categoryID}")
                    findNavController().navigate(action)
                }
            } else if (oldCat?.categoryName == chosenCategory &&
                oldCat.subcategoryName == binding.editSubcategoryNewName.text.toString() &&
                (oldCat.discType != chosenDiscType ||
                        (oldCat.private != 2) != binding.privacySwitch.isChecked) ||
                oldCat?.inUse != binding.stateSwitch.isChecked
            ) {
                // disc type or privacy changed so update it/them
                CategoryViewModel.updateCategory(
                    binding.categoryId.text.toString().toInt(),
                    oldCat?.categoryName.toString(),
                    oldCat?.subcategoryName.toString(),
                    chosenDiscType,
                    if (binding.privacySwitch.isChecked) MyApplication.userIndex else 2,
                    binding.stateSwitch.isChecked
                )
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
            } else if (oldCat.categoryName != chosenCategory ||
                oldCat.subcategoryName != binding.editSubcategoryNewName.text.toString()
            ) {
                CategoryViewModel.updateCategory(
                    binding.categoryId.text.toString().toInt(),
                    chosenCategory,
                    binding.editSubcategoryNewName.text.toString().trim(),
                    chosenDiscType,
                    if (binding.privacySwitch.isChecked) MyApplication.userIndex else 2,
                    binding.stateSwitch.isChecked
                )
                setupCategorySpinner(chosenCategory)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
            } else {
                Toast.makeText(activity, getString(R.string.no_changes_made), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        binding.buttonDelete.setOnClickListener {
            if (spCtr > 0) {
                tellUserCantDelete(getString(R.string.scheduled_payments))
                return@setOnClickListener
            }
            if (budgetCtr > 0) {
                tellUserCantDelete(getString(R.string.budgets))
                return@setOnClickListener
            }
            if (expenseCtr > 0) {
                tellUserCantDelete(getString(R.string.transactions))
                return@setOnClickListener
            }

            fun yesClicked() {
                CategoryViewModel.deleteCategoryAndSubcategory(
                    binding.categoryId.text.toString().toInt()
                )
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }

            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                            binding.oldCategoryName.text.toString() + "-" +
                            binding.oldSubcategoryName.text.toString())
                )
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.messageBudget.setOnClickListener {
            val action = SettingsTabsFragmentDirections.actionSettingsTabsFragmentToBudgetViewAllFragment()
            action.categoryID = oldCategoryID.toString()
            dismiss()
            findNavController().navigate(action)
        }
        binding.messageTransaction.setOnClickListener {
            val action =
                SettingsTabsFragmentDirections.actionSettingsTabFragmentToTransactionViewAllFragment()
            action.categoryID = oldCategoryID.toString()
            dismiss()
            findNavController().navigate(action)
        }
        binding.messageScheduledPayment.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.ScheduledPaymentFragment)
        }
    }

    private fun tellUserCantDelete(iWhere: String) {
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cant_delete) + " ${binding.oldCategoryName.text}-${binding.oldSubcategoryName.text}")
            .setMessage(String.format(getString(R.string.cannot_be_deleted_as_it_is_used_in), "${binding.oldCategoryName.text}-${binding.oldSubcategoryName.text} ", iWhere))
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}