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

    companion object {
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_SUBCATEGORY = "KEY_SUBCATEGORY"
        private const val KEY_DISCTYPE = "KEY_DISCTYPE"
        private var oldCategory: String = ""
        private var oldSubcategory: String = ""
        private var oldDisctype: String = ""
        fun newInstance(
            category: String,
            subcategory: String,
            disctype: String
        ): CategoryEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_SUBCATEGORY, subcategory)
            args.putString(KEY_DISCTYPE, disctype)
            val fragment = CategoryEditDialogFragment()
            fragment.arguments = args
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

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.editCategoryNewNameSpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.editCategoryNewNameSpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.editCategoryNewNameSpinner.requestFocus()
        binding.editCategoryNewNameSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY) {
                    binding.categoryDialogLinearLayout3.visibility = View.VISIBLE
//                    binding.editCategoryNewName.requestFocus()
                    focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                } else {
                    binding.categoryDialogLinearLayout3.visibility = View.GONE
                }
            }
        }

        val dtSpinner:Spinner = binding.editCategoryNewDisctypeSpinner
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
            binding.newNameHeader.text = "Add Category / SubCategory"
        } else { // ie this is an edit
            binding.oldCategoryName.text = oldCategory
            binding.oldSubcategoryName.text = oldSubcategory
            binding.oldDisctype.text = oldDisctype
            binding.editSubcategoryNewName.setText(oldSubcategory)
            dtSpinner.setSelection(arrayAdapter.getPosition(oldDisctype))
            binding.categoryDialogLinearLayoutBudget.visibility = View.GONE
        }
        dtSpinner.setBackgroundColor(Color.parseColor(hexColor))
        dtSpinner.setPopupBackgroundResource(R.drawable.spinner)
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
        CategoryViewModel.getCategoryNames().forEach {
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
            Log.d("Alex", "on save " + oldCategory + " " + binding.editCategoryNewNameSpinner.selectedItem.toString())

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
                binding.editSubcategoryNewName.error = "Subcategory name has invalid characters."
                focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                return@setOnClickListener
            }
            if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY &&
                    binding.editCategoryNewName.text.toString() == "") {
                binding.editCategoryNewName.error = "Enter new category name."
                focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryNewName)
                return@setOnClickListener
            }
            if (binding.editSubcategoryNewName.text.toString() == "") {
                binding.editSubcategoryNewName.error = "Enter new sub-category name."
                focusAndOpenSoftKeyboard(requireContext(), binding.editSubcategoryNewName)
                return@setOnClickListener
            }
            if (oldCategory == "" && binding.editCategoryBudgetAmount.text.toString() == "") {
                binding.editCategoryBudgetAmount.error = "Enter new budget amount."
                focusAndOpenSoftKeyboard(requireContext(), binding.editCategoryBudgetAmount)
                return@setOnClickListener
            }
            val chosenCategory = if (binding.editCategoryNewNameSpinner.selectedItem.toString() == cNEW_CATEGORY)
                binding.editCategoryNewName.text.toString().trim()
            else
                binding.editCategoryNewNameSpinner.selectedItem.toString()
            val dtSpinner:Spinner = binding.editCategoryNewDisctypeSpinner
            if (oldCategory == chosenCategory &&
                    oldSubcategory == binding.editSubcategoryNewName.text.toString() &&
                    oldDisctype != dtSpinner.selectedItem.toString()) {
                // disc type changed so update it
                CategoryViewModel.updateCategory(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                CategoryViewModel.setDiscType(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
            } else if (oldCategory == "") { // ie this is an add
                CategoryViewModel.addCategoryAndSubcategory(chosenCategory, binding.editSubcategoryNewName.text.toString().trim(), binding.editCategoryNewDisctypeSpinner.selectedItem.toString())
                val dateNow = Calendar.getInstance()
                val bmNow = BudgetMonth(dateNow.get(Calendar.YEAR), dateNow.get(Calendar.MONTH)+1)
                val newWho = if (SpenderViewModel.getActiveCount() > 1) "Joint" else SpenderViewModel.getSpenderName(0)
                val tempDouble : Double = binding.editCategoryBudgetAmount.text.toString().toDouble()

                BudgetViewModel.updateBudget(Category(chosenCategory, binding.editSubcategoryNewName.text.toString().trim()),
                    bmNow.toString(), newWho, tempDouble, cBUDGET_RECURRING)
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                setupCategorySpinner(chosenCategory)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
            } else if (oldCategory != chosenCategory ||
                    oldSubcategory != binding.editSubcategoryNewName.text.toString()) {
                CategoryViewModel.deleteCategoryAndSubcategory(oldCategory, oldSubcategory)
                CategoryViewModel.updateCategory(chosenCategory, binding.editSubcategoryNewName.text.toString().trim(), dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                setupCategorySpinner(chosenCategory)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                dismiss()
            }
        }

        binding.categoryDialogButtonDelete.setOnClickListener {
            fun yesClicked() {
                CategoryViewModel.deleteCategoryAndSubcategory(binding.oldCategoryName.text.toString(), binding.oldSubcategoryName.text.toString())
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
                .setMessage("Are you sure that you want to delete this Cat/Subcat (" +
                        binding.oldCategoryName.text.toString() + "-" +
                        binding.oldSubcategoryName.text.toString() + ")?")
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.categoryDialogButtonCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setCategoryEditDialogFragmentListener(listener: CategoryEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}