package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementPensionDialogBinding
import java.util.*

class RetirementPensionDialogFragment : DialogFragment() {
    interface RetirementPensionDialogFragmentListener {
        fun onNewDataSaved()
    }

    private var listener: RetirementPensionDialogFragmentListener? = null
    private var _binding: FragmentRetirementPensionDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_USER_ID = "1"
        private const val KEY_PENSION_ID = "2"
        private var userID: Int = 0
        private var pensionID: Int = 0
        fun newInstance(
            userIDIn: Int,
            pensionIDIn: Int
        ): RetirementPensionDialogFragment {
            val args = Bundle()

            args.putString(KEY_USER_ID, userIDIn.toString())
            args.putString(KEY_PENSION_ID, pensionIDIn.toString())
            val fragment = RetirementPensionDialogFragment()
            fragment.arguments = args
            userID = userIDIn
            pensionID = pensionIDIn
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRetirementPensionDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPensionTypeSpinner()
        setupClickListeners()
        if (pensionID == -1)
            binding.deleteButton.visibility = View.GONE
        val userDefault = RetirementViewModel.getUserDefault(userID)
        if (userDefault != null) {
            val pension = userDefault.pensions.find {it.id == pensionID}
            if (pension != null) {
                binding.title.text = PensionType.getText(pension.pensionType) + ": " + pension.name
                setupPensionTypeSpinner(PensionType.getText(pension.pensionType))
                binding.pensionName.setText(pension.name)
                binding.pensionValue.setText(pension.value.toString())
                binding.workStartDate.setText(pension.workStartDate)
                binding.pensionStartDate.setText(pension.pensionStartDate)
                binding.best5YearsSalary.setText(pension.best5YearsSalary.toString())
                if (pension.pensionType == PensionType.BASIC) {
                    binding.pensionStartDateLayout.visibility = View.VISIBLE
                    binding.workStartDateLayout.visibility = View.GONE
                    binding.best5YearsSalaryLayout.visibility = View.GONE
                    binding.pensionValueLayout.visibility = View.GONE
                } else {
                    binding.pensionStartDateLayout.visibility = View.GONE
                    binding.workStartDateLayout.visibility = View.VISIBLE
                    binding.best5YearsSalaryLayout.visibility = View.VISIBLE
                    binding.pensionValueLayout.visibility = View.VISIBLE
                }
            }
            val cal = android.icu.util.Calendar.getInstance()
            binding.pensionStartDate.setText(giveMeMyDateFormat(cal))
            val pensionStartDateSetListener = // this is fired when user clicks OK
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    val lcal = android.icu.util.Calendar.getInstance()
                    lcal.set(Calendar.YEAR, year)
                    lcal.set(Calendar.MONTH, monthOfYear)
                    lcal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.pensionStartDate.setText(giveMeMyDateFormat(lcal))
                }
            binding.pensionStartDate.setOnClickListener {
                val lcal = android.icu.util.Calendar.getInstance()
                if (binding.pensionStartDate.text.toString() != "") {
                    lcal.set(Calendar.YEAR, binding.pensionStartDate.text.toString().substring(0,4).toInt())
                    lcal.set(Calendar.MONTH, binding.pensionStartDate.text.toString().substring(5,7).toInt()-1)
                    lcal.set(Calendar.DAY_OF_MONTH, binding.pensionStartDate.text.toString().substring(8,10).toInt())
                }
                DatePickerDialog( // this is fired when user clicks into date field
                    requireContext(), pensionStartDateSetListener,
                    lcal.get(Calendar.YEAR),
                    lcal.get(Calendar.MONTH),
                    lcal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            binding.workStartDate.setText(giveMeMyDateFormat(cal))
            val workStartDateSetListener = // this is fired when user clicks OK
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    val lcal = android.icu.util.Calendar.getInstance()
                    lcal.set(Calendar.YEAR, year)
                    lcal.set(Calendar.MONTH, monthOfYear)
                    lcal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.workStartDate.setText(giveMeMyDateFormat(lcal))
                }
            binding.workStartDate.setOnClickListener {
                val lcal = android.icu.util.Calendar.getInstance()
                if (binding.workStartDate.text.toString() != "") {
                    lcal.set(Calendar.YEAR, binding.workStartDate.text.toString().substring(0,4).toInt())
                    lcal.set(Calendar.MONTH, binding.workStartDate.text.toString().substring(5,7).toInt()-1)
                    lcal.set(Calendar.DAY_OF_MONTH, binding.workStartDate.text.toString().substring(8,10).toInt())
                }
                DatePickerDialog( // this is fired when user clicks into date field
                    requireContext(), workStartDateSetListener,
                    lcal.get(Calendar.YEAR),
                    lcal.get(Calendar.MONTH),
                    lcal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        binding.pensionTypeSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                if (selection == PensionType.getText(PensionType.BASIC)) {
                    binding.workStartDateLayout.visibility = View.GONE
                    binding.best5YearsSalaryLayout.visibility = View.GONE
                    binding.pensionStartDateLayout.visibility = View.VISIBLE
                    binding.pensionValueLayout.visibility = View.VISIBLE
                } else {
                    binding.workStartDateLayout.visibility = View.VISIBLE
                    binding.best5YearsSalaryLayout.visibility = View.VISIBLE
                    binding.pensionStartDateLayout.visibility = View.GONE
                    binding.pensionValueLayout.visibility = View.GONE
                }
                binding.title.text = selection.toString() + ": " + binding.pensionName.text.toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupPensionTypeSpinner(iSelection: String = "") {
        val pensionTypeList: MutableList<String> = ArrayList()
        pensionTypeList.add(PensionType.getText(PensionType.BASIC))
        pensionTypeList.add(PensionType.getText(PensionType.PSPP))
        pensionTypeList.add(PensionType.getText(PensionType.OTPP))
        val pensionArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            pensionTypeList
        )
        binding.pensionTypeSpinner.adapter = pensionArrayAdapter
        if (iSelection == "")
            binding.pensionTypeSpinner.setSelection(0)
        else
            binding.pensionTypeSpinner.setSelection(pensionArrayAdapter.getPosition(iSelection))
        pensionArrayAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            if (binding.pensionName.text.toString() == "") {
                binding.pensionName.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.pensionName)
                return@setOnClickListener
            }
            val pensionType =
                try {
                    PensionType.valueOf(binding.pensionTypeSpinner.selectedItem.toString())
                } catch (e: IllegalArgumentException) {
                    PensionType.BASIC
                }
            if (pensionType == PensionType.PSPP || pensionType == PensionType.OTPP) {
                if (binding.workStartDate.text.toString() == "") {
                    binding.workStartDate.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.workStartDate)
                    return@setOnClickListener
                }
                if (binding.best5YearsSalary.text.toString() == "") {
                    binding.best5YearsSalary.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.best5YearsSalary)
                    return@setOnClickListener
                }
            } else {
                if (binding.pensionStartDate.text.toString() == "") {
                    binding.pensionStartDate.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.pensionStartDate)
                    return@setOnClickListener
                }
            }
            val userDefault = RetirementViewModel.getUserDefault(userID)
            if (userDefault != null) {
                val pension = when (pensionType) {
                    PensionType.BASIC -> Pension(
                        userDefault.pensions.size,
                        binding.pensionName.text.toString(),
                        binding.pensionValue.text.toString().toInt(),
                        pensionType,
                        binding.pensionStartDate.text.toString(),
                        0,
                        binding.pensionStartDate.text.toString())
                    PensionType.OTPP -> Pension(
                        userDefault.pensions.size,
                        binding.pensionName.text.toString(),
                        0,
                        pensionType,
                        binding.workStartDate.text.toString(),
                        if (isNumber(binding.best5YearsSalary.text.toString())) binding.best5YearsSalary.text.toString().toInt() else 0,
                        binding.workStartDate.text.toString())
                    PensionType.PSPP -> Pension(
                        userDefault.pensions.size,
                        binding.pensionName.text.toString(),
                        0,
                        pensionType,
                        binding.workStartDate.text.toString(),
                        if (isNumber(binding.best5YearsSalary.text.toString())) binding.best5YearsSalary.text.toString().toInt() else 0,
                        binding.workStartDate.text.toString())
                }
                if (pensionID == -1) { // adding new
                    userDefault.addPension(pension)
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    dismiss()
                } else { // editing existing
                    userDefault.updatePension(pensionID, pension)
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                    if (listener != null)
                        listener?.onNewDataSaved()
                    dismiss()
                }
            }
        }
        binding.deleteButton.setOnClickListener {
            fun yesClicked() {
                val userDefault = RetirementViewModel.getUserDefault(userID)
                if (userDefault != null) {
                    userDefault.deletePension(pensionID)
                    if (listener != null) {
                        listener?.onNewDataSaved()
                    }
                    MyApplication.playSound(context, R.raw.short_springy_gun)
                }
                dismiss()
            }

            fun noClicked() {
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(
                    getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                            binding.pensionName.text.toString())
                )
                .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
                .show()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setRetirementPensionDialogFragmentListener(listener: RetirementPensionDialogFragmentListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}