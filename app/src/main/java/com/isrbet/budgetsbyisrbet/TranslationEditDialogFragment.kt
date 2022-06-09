package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentTranslationEditDialogBinding

class TranslationEditDialogFragment : DialogFragment() {
    interface TranslationEditDialogFragmentListener {
        fun onNewDataSaved()
    }
    private var listener: TranslationEditDialogFragmentListener? = null
    private var _binding: FragmentTranslationEditDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_BEFORE = "KEY_BEFORE"
        private const val KEY_AFTER = "KEY_AFTER"
        private const val KEY_KEY = "KEY_KEY"
        private val myTranslation = Translation("","","")
        fun newInstance(
            before: String,
            after: String,
            key: String
        ): TranslationEditDialogFragment {
            val args = Bundle()
            args.putString(KEY_BEFORE, before)
            args.putString(KEY_AFTER, after)
            args.putString(KEY_KEY, key)
            val fragment = TranslationEditDialogFragment()
            fragment.arguments = args
            myTranslation.before = before
            myTranslation.after = after
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        Log.d("Alex", "in dialog, translation is $myTranslation")

        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.afterField.setBackgroundColor(Color.parseColor(hexColor))
        binding.afterField.requestFocus()

        binding.beforeField.text = myTranslation.before
        binding.afterField.setText(myTranslation.after)
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
            TranslationViewModel.updateTranslation(myTranslation.key, binding.afterField.text.toString())
            if (listener != null)
                listener?.onNewDataSaved()
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            dismiss()
        }

        binding.buttonDelete.setOnClickListener {
            fun yesClicked() {
                TranslationViewModel.deleteTranslation(myTranslation.key)
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
                .setMessage("Are you sure that you want to delete this translation?")
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setTranslationEditDialogFragmentListener(listener: TranslationEditDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}