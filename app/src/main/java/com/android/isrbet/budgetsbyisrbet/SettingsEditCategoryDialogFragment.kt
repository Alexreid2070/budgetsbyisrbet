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
import kotlinx.android.synthetic.main.fragment_category_edit_dialog.view.*

class SettingsEditCategoryDialogFragment() : DialogFragment() {
    interface SettingsEditCategoryDialogFragmentListener {
        fun onNewDataSaved()
    }
    private var listener: SettingsEditCategoryDialogFragmentListener? = null

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
        return inflater.inflate(R.layout.fragment_category_edit_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.edit_category_old_name.text = oldCategory
        view.edit_category_new_name.setText(oldCategory)
        view.edit_subcategory_old_name.text = oldSubcategory
        view.edit_subcategory_new_name.setText(oldSubcategory)
        view.edit_category_old_disctype.text = oldDisctype
        setupClickListeners(view)
        view.edit_category_new_name.requestFocus()

        val dtSpinner:Spinner = view.edit_category_new_disctype_spinner
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            DiscTypeValues
        )
        dtSpinner.adapter = arrayAdapter
        dtSpinner.setSelection(arrayAdapter.getPosition(oldDisctype))

        if (oldCategory == "") { // ie this is an add, not an edit
            view.edit_category_old_name_header.visibility = View.GONE
            view.edit_category_old_name.visibility = View.GONE
            view.edit_subcategory_old_name.visibility = View.GONE
            view.edit_category_old_disctype.visibility = View.GONE
            view.category_dialog_button_delete.visibility = View.GONE
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
        val dtSpinner:Spinner = view.edit_category_new_disctype_spinner
        view.category_dialog_button_save.setOnClickListener {
            if (oldCategory == view.edit_category_new_name.text.toString() &&
                    oldSubcategory == view.edit_subcategory_new_name.text.toString() &&
                    oldDisctype != dtSpinner.selectedItem.toString()) {
                // disc type changed so update it
                CategoryViewModel.updateCategory(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                CategoryViewModel.setDiscType(oldCategory, oldSubcategory, dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            } else if (oldCategory == "") { // ie this is an add
                CategoryViewModel.addCategoryAndSubcategory(view.edit_category_new_name.text.toString().trim(), view.edit_subcategory_new_name.text.toString().trim(), view.edit_category_new_disctype_spinner.selectedItem.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            } else if (oldCategory != view.edit_category_new_name.text.toString() ||
                    oldSubcategory != view.edit_subcategory_new_name.text.toString()) {
                CategoryViewModel.deleteCategory(oldCategory, oldSubcategory)
                CategoryViewModel.updateCategory(view.edit_category_new_name.text.toString().trim(), view.edit_subcategory_new_name.text.toString().trim(), dtSpinner.selectedItem.toString())
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            }
        }

        view.category_dialog_button_delete.setOnClickListener {
            fun yesClicked() {
                CategoryViewModel.deleteCategoryAndSubcategory(view.edit_category_old_name.text.toString(), view.edit_subcategory_old_name.text.toString())
                if (listener != null) {
                    listener?.onNewDataSaved()
                }
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to delete this Cat/Subcat (" + view.edit_category_old_name.text.toString() + "-" + view.edit_subcategory_old_name.text.toString() + ")?")
                .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
                .show()
        }

        view.category_dialog_button_cancel.setOnClickListener() {
            dismiss()
        }
    }

    fun setSettingsEditCategoryDialogFragmentListener(listener: SettingsEditCategoryDialogFragmentListener) {
        this.listener = listener
    }
}