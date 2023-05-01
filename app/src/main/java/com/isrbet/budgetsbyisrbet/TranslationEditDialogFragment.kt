package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTranslationEditDialogBinding

class TranslationEditDialogFragment : DialogFragment() {
//    interface TranslationEditDialogFragmentListener {
  //      fun onNewDataSaved()
    //}
    //private var listener: TranslationEditDialogFragmentListener? = null
    private var _binding: FragmentTranslationEditDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_BEFORE = "0"
        private const val KEY_AFTER = "1"
        private const val KEY_CATEGORY = "2"
        private const val KEY_KEY = "3"
        private val myTranslation = Translation("","",0, "")
        fun newInstance(
            before: String,
            after: String,
            category: Int,
            key: String
        ): TranslationEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_BEFORE, before)
            args.putString(KEY_AFTER, after)
            args.putString(KEY_CATEGORY, category.toString())
            args.putString(KEY_KEY, key)
            val fragment = TranslationEditDialogFragment()
            fragment.arguments = args
            myTranslation.before = before
            myTranslation.after = after
            myTranslation.category = category
            myTranslation.key = key
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.afterField.setBackgroundColor(Color.parseColor(hexColor))
        binding.afterField.requestFocus()

        binding.beforeField.text = myTranslation.before
        binding.afterField.setText(myTranslation.after)
        binding.categoryField.text = CategoryViewModel.getFullCategoryName(myTranslation.category)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            TranslationViewModel.updateTranslation(myTranslation.key,
                binding.beforeField.text.toString(),
                binding.afterField.text.toString(),
                myTranslation.category)
//            if (listener != null)
  //              listener?.onNewDataSaved()
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }

        binding.buttonDelete.setOnClickListener {
            fun yesClicked() {
                TranslationViewModel.deleteTranslation(myTranslation.key)
    //            if (listener != null) {
      //              listener?.onNewDataSaved()
        //        }
                MyApplication.playSound(context, R.raw.short_springy_gun)
                dismiss()
            }
            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(getString(R.string.are_you_sure_that_you_want_to_delete_this_item), myTranslation.before))
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

//    fun setTranslationEditDialogFragmentListener(listener: TranslationEditDialogFragmentListener) {
  //      this.listener = listener
    //}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}