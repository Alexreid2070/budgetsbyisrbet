package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.isrbet.budgetsbyisrbet.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        binding.helpVersionName.text = BuildConfig.VERSION_NAME
        binding.helpVersionCode.text = BuildConfig.VERSION_CODE.toString()
        setHasOptionsMenu(true)
        inflater.inflate(R.layout.fragment_help, container, false)
        return binding.root
    }
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}