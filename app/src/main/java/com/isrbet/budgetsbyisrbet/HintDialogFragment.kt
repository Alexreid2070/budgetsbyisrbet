package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
        private const val KEY_FRAGMENT = "KEY_FRAGMENT"
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

    @SuppressLint("SetTextI18n")
    private fun setupClickListeners() {
        binding.doneButton.setOnClickListener {
            dismiss()
        }
        binding.prevButton.setOnClickListener {
            Log.d("Alex", "on hint $currentHintID")
            val tHint = HintViewModel.getPreviousHint(myFragment, currentHintID)
            if (tHint == null) { // no hint to show
                binding.hintText.text = "$currentHintText\n\nThere are no previous hints to show."
            } else {
                currentHintID = tHint.id
                currentHintText = tHint.text
                binding.hintText.text = tHint.text
            }
        }

        binding.nextButton.setOnClickListener {
            val tHint = HintViewModel.getNextHint(myFragment, currentHintID)
            if (tHint == null) { // no hint to show
                binding.hintText.text = "$currentHintText\n\nThere are no next hints to show."
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