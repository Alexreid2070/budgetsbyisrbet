import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.*
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryEditDialogBinding

class SettingsEditCategoryDialogFragment() : DialogFragment() {
    interface SettingsEditCategoryDialogFragmentListener {
        fun onNewDataSaved()
    }
    private var listener: SettingsEditCategoryDialogFragmentListener? = null
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
        ): SettingsEditCategoryDialogFragment {
            val args = Bundle()
            args.putString(KEY_CATEGORY, category)
            args.putString(KEY_SUBCATEGORY, subcategory)
            args.putString(KEY_DISCTYPE, disctype)
            val fragment = SettingsEditCategoryDialogFragment()
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
    ): View? {
        _binding = FragmentCategoryEditDialogBinding.inflate(inflater, container, false)
        return inflater.inflate(R.layout.fragment_category_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editCategoryOldName.text = oldCategory
        binding.editCategoryNewName.setText(oldCategory)
        binding.editSubcategoryOldName.text = oldSubcategory
        binding.editSubcategoryNewName.setText(oldSubcategory)
        binding.editCategoryOldDisctype.text = oldDisctype
        setupClickListeners(view)
        binding.editCategoryNewName.requestFocus()

        val dtSpinner:Spinner = binding.editCategoryNewDisctypeSpinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            DiscTypeValues
        )
        dtSpinner.adapter = arrayAdapter
        dtSpinner.setSelection(arrayAdapter.getPosition(oldDisctype))

        if (oldCategory == "") { // ie this is an add, not an edit
            binding.editCategoryOldNameHeader.visibility = View.GONE
            binding.editCategoryOldName.visibility = View.GONE
            binding.editSubcategoryOldName.visibility = View.GONE
            binding.editCategoryOldDisctype.visibility = View.GONE
            binding.categoryDialogButtonDelete.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners(view: View) {
        // val dtSpinner = requireActivity().findViewById(R.id.category_dialog_disctype_spinner) as Spinner
        val dtSpinner:Spinner = binding.editCategoryNewDisctypeSpinner
        binding.categoryDialogButtonSave.setOnClickListener {
            if (oldCategory == binding.editCategoryNewName.text.toString() &&
                    oldSubcategory == binding.editSubcategoryNewName.text.toString() &&
                    oldDisctype != dtSpinner.selectedItem.toString()) {
                // disc type changed so update it
                CategoryViewModel.updateCategory(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                CategoryViewModel.setDiscType(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            } else if (oldCategory == "") { // ie this is an add
                CategoryViewModel.addCategoryAndSubcategory(binding.editCategoryNewName.text.toString().trim(), binding.editSubcategoryNewName.text.toString().trim(), binding.editCategoryNewDisctypeSpinner.selectedItem.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            } else if (oldCategory != binding.editCategoryNewName.text.toString() ||
                    oldSubcategory != binding.editSubcategoryNewName.text.toString()) {
                CategoryViewModel.deleteCategory(oldCategory, oldSubcategory)
                CategoryViewModel.updateCategory(binding.editCategoryNewName.text.toString().trim(), binding.editSubcategoryNewName.text.toString().trim(), dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            }
        }

        binding.categoryDialogButtonDelete.setOnClickListener {
            fun yesClicked() {
                CategoryViewModel.deleteCategoryAndSubcategory(binding.editCategoryOldName.text.toString(), binding.editSubcategoryOldName.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to delete this Cat/Subcat (" + binding.editCategoryOldName.text.toString() + "-" + binding.editSubcategoryOldName.text.toString() + ")?")
                .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
                .show()
        }

        binding.categoryDialogButtonCancel.setOnClickListener() {
            dismiss()
        }
    }

    fun setSettingsEditCategoryDialogFragmentListener(listener: SettingsEditCategoryDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}