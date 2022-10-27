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
        private const val KEY_PENSION_NAME = ""
        private const val KEY_DEFAULT_MODE_ID = "3"
        private var userID: Int = 0
        private var pensionName: String = ""
        private var inDefaultMode: Boolean = false
        fun newInstance(
            userIDIn: Int,
            pensionNameIn: String,
            defaultModeIn: Boolean
        ): RetirementPensionDialogFragment {
            val args = Bundle()

            args.putString(KEY_USER_ID, userIDIn.toString())
            args.putString(KEY_PENSION_NAME, pensionNameIn)
            args.putString(KEY_DEFAULT_MODE_ID, defaultModeIn.toString())
            val fragment = RetirementPensionDialogFragment()
            fragment.arguments = args
            userID = userIDIn
            pensionName = pensionNameIn
            inDefaultMode = defaultModeIn
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
        if (pensionName == "")
            binding.deleteButton.visibility = View.GONE
        val pension = RetirementViewModel.getWorkingPension(pensionName)
        if (pension != null) {
            binding.title.text = String.format("${PensionType.getText(pension.pensionType)}: ${pension.name}")
            setupPensionTypeSpinner(PensionType.getText(pension.pensionType))
            binding.pensionName.setText(pension.name)
            binding.pensionValue.setText(pension.value.toString())
            binding.workStartDate.setText(pension.workStartDate)
            binding.pensionStartDate.setText(pension.pensionStartDate)
            binding.best5YearsSalary.setText(pension.best5YearsSalary.toString())
            binding.pensionStartDelay.setText(pension.pensionStartDelay.toString())
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
            if (pension.pensionType == PensionType.OTPP)
                binding.pensionStartDelayLayout.visibility = View.VISIBLE
            else
                binding.pensionStartDelayLayout.visibility = View.GONE
            binding.pensionStartDate.setText(pension.pensionStartDate)
            binding.workStartDate.setText(pension.workStartDate)
        }
//            val cal = android.icu.util.Calendar.getInstance()
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
//            binding.workStartDate.setText(giveMeMyDateFormat(cal))
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
                if (selection == PensionType.getText(PensionType.OTPP))
                    binding.pensionStartDelayLayout.visibility = View.VISIBLE
                else
                    binding.pensionStartDelayLayout.visibility = View.GONE
                binding.title.text = String.format("${selection.toString()}: ${binding.pensionName.text.toString()}")
            }
        }
        if (false) { // (!inDefaultMode) {
            binding.pensionTypeSpinner.isEnabled = false
            binding.pensionName.isEnabled = false
            binding.pensionValue.isEnabled = false
            binding.pensionStartDate.isEnabled = false
            binding.workStartDate.isEnabled = false
            binding.best5YearsSalary.isEnabled = false
            binding.pensionStartDelay.isEnabled = false
            binding.saveButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
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
            if (RetirementViewModel.getWorkingPension(binding.pensionName.text.toString()) == null) {
                binding.pensionName.error = getString(R.string.name_already_exists)
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
            if (pensionType == PensionType.OTPP) {
                if (binding.pensionStartDelay.text.toString() == "") {
                    binding.pensionStartDelay.error = getString(R.string.value_cannot_be_blank)
                    focusAndOpenSoftKeyboard(requireContext(), binding.pensionStartDelay)
                    return@setOnClickListener
                }
            }
            val pension = when (pensionType) {
                PensionType.BASIC -> Pension(
                    RetirementViewModel.getWorkingPensionListCount(),
                    binding.pensionName.text.toString(),
                    binding.pensionValue.text.toString().toInt(),
                    pensionType,
                    binding.pensionStartDate.text.toString(),
                    0,
                    binding.pensionStartDate.text.toString(),
                0)
                PensionType.OTPP -> Pension(
                    RetirementViewModel.getWorkingPensionListCount(),
                    binding.pensionName.text.toString(),
                    0,
                    pensionType,
                    binding.workStartDate.text.toString(),
                    if (binding.best5YearsSalary.text.toString().toIntOrNull() != null)
                        binding.best5YearsSalary.text.toString().toInt()
                    else
                        0,
                    binding.workStartDate.text.toString(),
                    binding.pensionStartDelay.text.toString().toInt())
                PensionType.PSPP -> Pension(
                    RetirementViewModel.getWorkingPensionListCount(),
                    binding.pensionName.text.toString(),
                    0,
                    pensionType,
                    binding.workStartDate.text.toString(),
                    if (binding.best5YearsSalary.text.toString().toIntOrNull() != null)
                        binding.best5YearsSalary.text.toString().toInt()
                    else
                        0,
                    binding.workStartDate.text.toString(),
                0)
                }
                if (pensionName == "") { // adding new
                    RetirementViewModel.addPensionToWorkingList(pension)
                } else { // editing existing
                    RetirementViewModel.updatePensionInWorkingList(pensionName, pension)
                }
                if (listener != null)
                    listener?.onNewDataSaved()
                dismiss()
            }
        binding.deleteButton.setOnClickListener {
            fun yesClicked() {
                RetirementViewModel.deletePensionFromWorkingList(pensionName)
                if (listener != null) {
                    listener?.onNewDataSaved()
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

        binding.pensionHelpButton.setOnClickListener {
            fun yesClicked() {
            }

            val pensionType =
                try {
                    PensionType.valueOf(binding.pensionTypeSpinner.selectedItem.toString())
                } catch (e: IllegalArgumentException) {
                    PensionType.BASIC
                }
            @Suppress("HardCodedStringLiteral") val message = when (pensionType) {
                PensionType.BASIC -> "This pension type simply pays 1/12 of the annual amount per month, " +
                        "starting on the date specified.  It increases annually by the inflation rate."
                PensionType.PSPP -> "This Canada Public Service Pension Plan pension uses this formula: \n\n" +
                        "Compute the average YMPE (average CPP maximum insurable earnings for each year of work). \n\n" +
                        "Compute the first pension half:  min (average YMPE, best 5 years salary) * 1.4% * years of full-time service.\n\n" +
                        "Compute the second pension half: if best 5 Years Salary > average YMPE, " +
                        "   compute (best 5 Years Salary - average YMPE) * 2% * years of full-time service.\n\n" +
                        "Next, add first and second pension halves, and adjust for inflation. \n\n" +
                        "If this is an early retirement (i.e. retiree is not yet 65 or has not yet reached his 85 factor), an early retirement penalty applies.\n\n" +
                        "Penalty is 3% * min(the number of years it would take to reach age 65, or the number of years until you reach 85 factor).\n\n" +
                        "Final pension amount is reduced by this penalty."
                PensionType.OTPP -> "This Ontario Teachers Pension Plan pension uses the formula below.  Note that " +
                        "the retiree can defer the start of his pension.\n\n" +
                        "Compute: startingPension = 2% * years of full-time service * best 5 Years salary.\n\n" +
                        "Adjust pension for inflation.\n\n" +
                        "Once retiree starts his CPP, CPP clawback reduces the pension by .45% * years of full-time service * best 5 years salary.\n\n" +
                        "If retirement is before the 85 factor, an early retirement penalty applies.\n" +
                        "Penalty is minimum of these 2 possible penalties:\n" +
                        "  possible penalty 1: min((2.5% * (85 - 85FactorAtRetirement)), 5% * (65 - age at retirement)\n" +
                        "  possible penalty 2: min(1 - (0.95 ^ (85 - 85FactorAtPensionStart)), 1 - (0.95 ^ (65 - age at pension start) / 365)))\n\n" +
                        "Final pension amount is reduced by this penalty."
            }

            AlertDialog.Builder(requireContext())
                .setTitle(String.format(getString(R.string.pension_formula), PensionType.getText(pensionType)))
                .setMessage(message)
                .setNeutralButton(android.R.string.ok) { _, _ -> yesClicked() }
                .show()
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