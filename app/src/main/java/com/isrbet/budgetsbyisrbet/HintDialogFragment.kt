package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentHintDialogBinding

class HintDialogFragment : DialogFragment() {
    private var _binding: FragmentHintDialogBinding? = null
    private val binding get() = _binding!!
    private var currentHintID = -1
    private var currentHintText = ""

    companion object {
        private const val KEY_FRAGMENT = "0"
        private var myFragment = ""
        fun newInstance(
            fragment: String
        ): HintDialogFragment {
            val args = Bundle()

            args.putString(KEY_FRAGMENT, fragment)
            myFragment = fragment
            val hintFragment = HintDialogFragment()
            hintFragment.arguments = args
            return hintFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHintDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()

        val tHint = HintViewModel.getNextHint(myFragment)
        if (tHint == null) { // no hint to show
            dismiss()
        } else {
            currentHintID = tHint.id
            currentHintText = tHint.text
            binding.hintText.text = tHint.text
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        binding.doneButton.setOnClickListener {
            dismiss()
        }
        binding.prevButton.setOnClickListener {
            val tHint = HintViewModel.getPreviousHint(myFragment, currentHintID)
            if (tHint == null) { // no hint to show
                binding.hintText.text = String.format(
                    getString(R.string.there_are_no_previous_hints_to_show), currentHintText)
            } else {
                currentHintID = tHint.id
                currentHintText = tHint.text
                binding.hintText.text = tHint.text
            }
        }

        binding.nextButton.setOnClickListener {
            val tHint = HintViewModel.getNextHint(myFragment, currentHintID)
            if (tHint == null) { // no hint to show
                binding.hintText.text = String.format(
                    getString(R.string.there_are_no_more_hints_to_show), currentHintText)
            } else {
                currentHintID = tHint.id
                currentHintText = tHint.text
                binding.hintText.text = tHint.text
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}