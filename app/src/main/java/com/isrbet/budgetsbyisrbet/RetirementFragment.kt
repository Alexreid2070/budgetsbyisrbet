package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class RetirementFragment : Fragment(), CoroutineScope {
    private var _binding: FragmentRetirementBinding? = null
    private val binding get() = _binding!!
    private var cal = android.icu.util.Calendar.getInstance()
    private var lRetirementDetailsList: MutableList<RetirementCalculationRow> = arrayListOf()
    private var myEarliestRetirementYear = ""
    private var myPropertySoldYear = ""
    private var myMaximumMonthlyBudget = 0
    private var myEndingRRSPBalance = 0
    private var myEndingTFSABalance = 0
    private var myEndingSavingsBalance = 0
    private var myEndingPropertyBalance = 0
    private var myEndingNetWorth = 0
    private var job: Job = Job()
    private var inDefaultMode = false // is set to true if user is editing defaults

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (inDefaultMode)
                setupDefaultMode(false)
            else
                findNavController().popBackStack()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRetirementBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_retirement, container, false)
        return binding.root
    }
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.currencySymbol1.text = String.format("${getLocalCurrencySymbol()} ")
        gRetirementDetailsList = arrayListOf()
        if (myEarliestRetirementYear == "") {
            setSummaryFields(View.GONE)
        }
        binding.birthDate.setText(giveMeMyDateFormat(cal))
        val birthDateSetListener = // this is fired when user clicks OK
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val lcal = android.icu.util.Calendar.getInstance()
                lcal.set(Calendar.YEAR, year)
                lcal.set(Calendar.MONTH, monthOfYear)
                lcal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.birthDate.setText(giveMeMyDateFormat(lcal))
            }
        binding.birthDate.setOnClickListener {
            val lcal = android.icu.util.Calendar.getInstance()
            if (binding.birthDate.text.toString() != "") {
                lcal.set(Calendar.YEAR, binding.birthDate.text.toString().substring(0,4).toInt())
                lcal.set(Calendar.MONTH, binding.birthDate.text.toString().substring(5,7).toInt()-1)
                lcal.set(Calendar.DAY_OF_MONTH, binding.birthDate.text.toString().substring(8,10).toInt())
            }
            DatePickerDialog( // this is fired when user clicks into date field
                requireContext(), birthDateSetListener,
                lcal.get(Calendar.YEAR),
                lcal.get(Calendar.MONTH),
                lcal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.retirementDate.setText(giveMeMyDateFormat(cal))
        val retDateSetListener = // this is fired when user clicks OK
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val lcal = android.icu.util.Calendar.getInstance()
                lcal.set(Calendar.YEAR, year)
                lcal.set(Calendar.MONTH, monthOfYear)
                lcal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.retirementDate.setText(giveMeMyDateFormat(lcal))
            }
        binding.retirementDate.setOnClickListener {
            val lcal = android.icu.util.Calendar.getInstance()
            if (binding.retirementDate.text.toString() != "") {
                lcal.set(Calendar.YEAR, binding.retirementDate.text.toString().substring(0,4).toInt())
                lcal.set(Calendar.MONTH, binding.retirementDate.text.toString().substring(5,7).toInt()-1)
                lcal.set(Calendar.DAY_OF_MONTH, binding.retirementDate.text.toString().substring(8,10).toInt())
            }
            DatePickerDialog( // this is fired when user clicks into date field
                requireContext(), retDateSetListener,
                lcal.get(Calendar.YEAR),
                lcal.get(Calendar.MONTH),
                lcal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.userIDRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            loadDefaults(RetirementViewModel.getUserDefault(SpenderViewModel.getSpenderIndex(radioButton.text.toString())))
            setSummaryFields(View.GONE)
            binding.scenarioNameInput.error = null
            binding.scenarioNameEntireLayout.visibility = View.GONE
        }

        binding.basicsExpandButton.setOnClickListener {
            onBasicsExpandClicked()
        }
        binding.salaryExpandButton.setOnClickListener {
            onSalaryExpandClicked()
        }
        binding.assetsExpandButton.setOnClickListener {
            onAssetsExpandClicked()
        }
        binding.pensionsExpandButton.setOnClickListener {
            onPensionsExpandClicked()
        }
        binding.cppOasExpandButton.setOnClickListener {
            onCPPOASExpandClicked()
        }
        binding.assetsAddButton.setOnClickListener {
            val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            val cdf = RetirementAssetDialogFragment.newInstance(SpenderViewModel.getSpenderIndex(radioButton.text.toString()), -1)
            cdf.setRetirementAssetDialogFragmentListener(object: RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                override fun onNewDataSaved() {
                    val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
                    val myAdapter =
                        RetirementViewModel.getUserDefault(SpenderViewModel.getSpenderIndex(radioButton.text.toString()))
                            ?.let { AssetAdapter(requireContext(), it,
                                { item ->
                                    moveAsset(item.distributionOrder, -1) },
                                { item ->
                                    moveAsset(item.distributionOrder, 1) }) }
                    listView.adapter = myAdapter
                    setListViewHeightBasedOnChildren(listView)
                    binding.assetsExpandButton.text =
                        String.format(getString(R.string.assets), myAdapter?.count)
                    myAdapter?.notifyDataSetChanged()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_asset))
        }

        binding.pensionsAddButton.setOnClickListener {
            val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            val cdf = RetirementPensionDialogFragment.newInstance(SpenderViewModel.getSpenderIndex(radioButton.text.toString()), -1)
            cdf.setRetirementPensionDialogFragmentListener(object: RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                override fun onNewDataSaved() {
                    val listView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
                    val myAdapter = RetirementViewModel.getUserDefault(SpenderViewModel.getSpenderIndex(radioButton.text.toString()))
                        ?.let { it1 -> PensionAdapter(requireContext(), it1) }
                    listView.adapter = myAdapter
                    setListViewHeightBasedOnChildren(listView)
                    binding.pensionsExpandButton.text =
                        String.format(getString(R.string.pensions), myAdapter?.count)
                    myAdapter?.notifyDataSetChanged()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_pension))
        }
        loadUserButtons()
        val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
        val radioButton = requireActivity().findViewById(selectedId) as RadioButton
        loadDefaults(RetirementViewModel.getUserDefault(SpenderViewModel.getSpenderIndex(radioButton.text.toString())))
        setupScenarioSpinner()

        binding.scenarioSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setSummaryFields(View.GONE)
                binding.scenarioNameInput.error = null
                binding.scenarioNameEntireLayout.visibility = View.GONE
                val selection = parent?.getItemAtPosition(position)
                if (position == 0) {
                    val userRadioGroup = binding.userIDRadioGroup
                    val userRadioGroupChecked = userRadioGroup.checkedRadioButtonId
                    val uRadioButton = requireActivity().findViewById(userRadioGroupChecked) as RadioButton
                    val spenderIndex = SpenderViewModel.getSpenderIndex(uRadioButton.text.toString())
                    val userDefault = RetirementViewModel.getUserDefault(spenderIndex) ?: return
                    loadDefaults(userDefault)
                    binding.buttonUpdateScenario.visibility = View.GONE
                    binding.buttonDeleteScenario.visibility = View.GONE
//                    binding.buttonLayout.weightSum = 2.0F
                } else {
                    val scenario = RetirementViewModel.getScenario(selection as String)
                    if (scenario != null) {
                        val uRadioGroup = binding.userIDRadioGroup
                        for (i in 0 until uRadioGroup.childCount) {
                            val o = uRadioGroup.getChildAt(i)
                            if (o is RadioButton) {
                                if (o.text == SpenderViewModel.getSpenderName(scenario.userID)) {
                                    o.isChecked = true
                                }
                            }
                        }
                        loadDefaults(scenario)

                        binding.buttonUpdateScenario.visibility = View.VISIBLE
                        binding.buttonDeleteScenario.visibility = View.VISIBLE
//                        binding.buttonLayout.weightSum = 4.0F
                    }
                }
            }
        }

        binding.buttonCancel.setOnClickListener {
            if (binding.scenarioNameEntireLayout.visibility == View.VISIBLE) { // we're saving a new scenario
                binding.scenarioNameInput.error = null
                binding.scenarioNameEntireLayout.visibility = View.GONE
                binding.buttonUpdateScenario.visibility = View.VISIBLE
                binding.buttonCalculate.visibility = View.VISIBLE
                binding.buttonCancel.visibility = View.GONE
                if (binding.scenarioSpinner.selectedItem.toString() == binding.scenarioSpinner.adapter.getItem(0).toString()) {
                    binding.buttonUpdateScenario.visibility = View.GONE
                }
            } else // we're in default edit mode
                setupDefaultMode(false)
        }

        binding.buttonCalculate.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onCalculateButtonClicked()
        }

        binding.buttonUpdateScenario.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked(inDefaultMode, false)
        }
        binding.buttonDeleteScenario.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onDeleteButtonClicked()
        }
        binding.buttonSaveScenario.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked(inDefaultMode, true)
        }
        binding.buttonSeeDetails.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSeeDetailsButtonClicked()
        }
        binding.retirementDefaultsButton.setOnClickListener {
            setupDefaultMode(true)
        }
        binding.earliestRetirementYear.text = myEarliestRetirementYear
        binding.propertySoldYear.text = myPropertySoldYear
        binding.maximumMonthlyBudget.text = gDecWithCurrency(myMaximumMonthlyBudget)
        binding.endingRrspBalance.text = gDecWithCurrency(myEndingRRSPBalance)
        binding.endingTfsaBalance.text = gDecWithCurrency(myEndingTFSABalance)
        binding.endingSavingsBalance.text = gDecWithCurrency(myEndingSavingsBalance)
        binding.endingPropertyBalance.text = gDecWithCurrency(myEndingPropertyBalance)
        binding.endingNetWorth.text = gDecWithCurrency(myEndingNetWorth)
    }
    private fun setupScenarioSpinner(iSelection: String = "") {
        val listOfRetirementScenarios = RetirementViewModel.getListOfRetirementScenarios()
        if (listOfRetirementScenarios.size > 0) {
            listOfRetirementScenarios.add(0,getString(R.string.use_default))
            binding.scenarioSpinnerLabel.visibility = View.VISIBLE
            binding.scenarioSpinnerRelativeLayout.visibility = View.VISIBLE
            val scenarioSpinner = binding.scenarioSpinner
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOfRetirementScenarios)
            scenarioSpinner.adapter = arrayAdapter
            if (iSelection != "") {
                scenarioSpinner.setSelection(arrayAdapter.getPosition(iSelection))
            }
            arrayAdapter.notifyDataSetChanged()
        } else {
            binding.scenarioSpinnerLabel.visibility = View.GONE
            binding.scenarioSpinnerRelativeLayout.visibility = View.GONE
            binding.buttonUpdateScenario.visibility = View.GONE
            binding.buttonDeleteScenario.visibility = View.GONE
//            binding.buttonLayout.weightSum = 2.0F
        }
    }

    private fun setSummaryFields(iView: Int) {
        binding.buttonSeeDetails.visibility = iView
        binding.earliestRetirementYearLayout.visibility = iView
        binding.propertySoldLayout.visibility = iView
        binding.maximumMonthlyBudgetLayout.visibility = iView
        binding.endingRrspBalanceLayout.visibility = iView
        binding.endingTfsaBalanceLayout.visibility = iView
        binding.endingSavingsBalanceLayout.visibility = iView
        binding.endingPropertyBalanceLayout.visibility = iView
        binding.endingNetWorthLayout.visibility = iView

    }
    private fun onCalculateButtonClicked() {
        MyApplication.displayToast(getString(R.string.calculating))

        setSummaryFields(View.GONE)
        binding.scenarioNameInput.error = null
        binding.scenarioNameEntireLayout.visibility = View.GONE
        myEarliestRetirementYear = ""
        binding.earliestRetirementYear.text = ""
        binding.propertySoldYear.text = ""
        binding.maximumMonthlyBudget.text = ""
        binding.endingRrspBalance.text = ""
        binding.endingTfsaBalance.text = ""
        binding.endingSavingsBalance.text = ""
        binding.endingPropertyBalance.text = ""
        binding.endingNetWorth.text = ""
        val userRadioGroup = binding.userIDRadioGroup
        val userRadioGroupChecked = userRadioGroup.checkedRadioButtonId
        val uRadioButton = requireActivity().findViewById(userRadioGroupChecked) as RadioButton

        val ageRadioGroup = binding.cppStartAgeRadioGroup
        val ageRadioGroupChecked = ageRadioGroup.checkedRadioButtonId
        val ageRadioButton = requireActivity().findViewById(ageRadioGroupChecked) as RadioButton

        val spenderIndex = SpenderViewModel.getSpenderIndex(uRadioButton.text.toString())
        val userDefault = RetirementViewModel.getUserDefault(spenderIndex) ?: return

        val rtData = RetirementData(
            "newName",
            spenderIndex,
            binding.targetMonthlyIncome.text.toString().toInt(),
            binding.retirementDate.text.toString(),
            binding.planToAge.text.toString().toInt(),
            ageRadioButton.text.toString().toInt(),
            (binding.minimizeRRSPSwitch.isChecked),
            binding.inflationRate.text.toString().toDouble(),
            userDefault.birthDate
        )
        rtData.salary = Salary(
            0,
            getString(R.string.salary),
            binding.salaryAmount.text.toString().toInt(),
            binding.salaryAnnualIncrease.text.toString().toDouble()
        )
        rtData.setAssetsAndPensionsAndCPPandOAS(userDefault)

        val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        val assetCount = listView.adapter?.count ?: 0
        for (i in 0 until assetCount) {
            val asset = listView.adapter.getItem(i) as Asset
            rtData.updateDistributionOrder(asset.id, i)
        }

        launch {
            val result =  getCalculationRows(rtData, false)
            gotTheCalculationRows(result) // onResult is called on the main thread
        }

        launch {
            val result =  getEarliestRetirementYear(rtData)
            gotTheEarliestRetirementYear(result) // onResult is called on the main thread
        }
        launch {
            val result =  getMaximumMonthlyIncome(rtData)
            gotTheMaximumMonthlyIncome(result) // onResult is called on the main thread
        }
    }

    private fun getCalculationRows(iRetirementScenario: RetirementData, iLogResult: Boolean)
            : MutableList<RetirementCalculationRow> {
        return RetirementViewModel.getCalculationRows(iRetirementScenario, iLogResult)
    }

    private fun gotTheCalculationRows(result: MutableList<RetirementCalculationRow> ) {
        lRetirementDetailsList = result
        val lastRow = lRetirementDetailsList[lRetirementDetailsList.size - 1]
        myEndingNetWorth = lastRow.getNetWorth(AssetType.ALL)
        binding.endingNetWorth.text = gDecWithCurrency(myEndingNetWorth)
        myEndingPropertyBalance = lastRow.getNetWorth(AssetType.PROPERTY)
        binding.endingPropertyBalance.text = gDecWithCurrency(myEndingPropertyBalance)
        myEndingSavingsBalance = lastRow.getNetWorth(AssetType.SAVINGS)
        binding.endingSavingsBalance.text = gDecWithCurrency(myEndingSavingsBalance)
        myEndingTFSABalance = lastRow.getNetWorth(AssetType.TFSA)
        binding.endingTfsaBalance.text = gDecWithCurrency(myEndingTFSABalance)
        myEndingRRSPBalance = lastRow.getNetWorth(AssetType.RRSP)
        binding.endingRrspBalance.text = gDecWithCurrency(myEndingRRSPBalance)
        myEndingPropertyBalance = lastRow.getNetWorth(AssetType.PROPERTY)
        binding.endingPropertyBalance.text = gDecWithCurrency(myEndingPropertyBalance)
        binding.propertySoldYear.text = lastRow.getPropertySoldYear().toString()
        myPropertySoldYear = binding.propertySoldYear.text.toString()
        setSummaryFields(View.VISIBLE)
    }

    private fun getEarliestRetirementYear(iRetirementScenario: RetirementData) : Int {
        return RetirementViewModel.getEarliestRetirementYear(iRetirementScenario)
    }

    private fun gotTheEarliestRetirementYear(result: Int) {
        binding.earliestRetirementYear.text = result.toString()
        myEarliestRetirementYear = result.toString()
        binding.earliestRetirementYearLayout.visibility = View.VISIBLE
    }

    private fun getMaximumMonthlyIncome(iRetirementScenario: RetirementData) : Int {
        return RetirementViewModel.getMaximumMonthlyIncome(iRetirementScenario)
    }

    private fun gotTheMaximumMonthlyIncome(result: Int) {
        binding.maximumMonthlyBudget.text = gDecWithCurrency(result)
        myMaximumMonthlyBudget = result
        binding.maximumMonthlyBudgetLayout.visibility = View.VISIBLE
    }

    private fun onDeleteButtonClicked() {
        fun yesClicked() {
            val scenarioName = binding.scenarioSpinner.selectedItem.toString()
            RetirementViewModel.deleteRetirementScenario(scenarioName)
            setupScenarioSpinner()
            MyApplication.playSound(context, R.raw.short_springy_gun)
            MyApplication.displayToast(getString(R.string.scenario_deleted))
            setSummaryFields(View.GONE)
            binding.scenarioNameInput.error = null
            binding.scenarioNameEntireLayout.visibility = View.GONE
        }
        fun noClicked() {
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.are_you_sure))
            .setMessage(String.format(getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                binding.scenarioSpinner.selectedItem.toString()))
            .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    private fun onSaveButtonClicked(inDefaultMode: Boolean, iNewScenario: Boolean) {
        setSummaryFields(View.GONE)
        val scenarioName: String
        if (binding.birthDate.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.birthDate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.birthDate)
            return
        }
        if (binding.inflationRate.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.inflationRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.inflationRate)
            return
        }
        if (binding.targetMonthlyIncome.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.targetMonthlyIncome.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.targetMonthlyIncome)
            return
        }
        if (binding.planToAge.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.planToAge.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.planToAge)
            return
        }
        if (binding.retirementDate.text.toString() == "") {
            binding.salaryLayout.visibility = View.VISIBLE
            binding.retirementDate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.retirementDate)
            return
        }
        if (binding.cpp60Amount.text.toString() == "") {
            binding.cppOasLayout.visibility = View.VISIBLE
            binding.cpp60Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.cpp60Amount)
            return
        }
        if (binding.cpp65Amount.text.toString() == "") {
            binding.cppOasLayout.visibility = View.VISIBLE
            binding.cpp65Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.cpp65Amount)
            return
        }
        if (binding.cpp70Amount.text.toString() == "") {
            binding.cppOasLayout.visibility = View.VISIBLE
            binding.cpp70Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.cpp70Amount)
            return
        }
        if (binding.oasCurrentAnnualAmount.text.toString() == "") {
            binding.cppOasLayout.visibility = View.VISIBLE
            binding.oasCurrentAnnualAmount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.oasCurrentAnnualAmount)
            return
        }
        if (inDefaultMode) {
            scenarioName = getString(R.string.defaultt)
        } else if (iNewScenario) {
            if (binding.scenarioNameInput.text.toString() == "") {
                binding.scenarioNameEntireLayout.visibility = View.VISIBLE
                binding.scenarioNameInput.error = getString(R.string.value_cannot_be_blank)
                binding.buttonCalculate.visibility = View.GONE
                binding.buttonUpdateScenario.visibility = View.GONE
                binding.buttonCancel.visibility = View.VISIBLE
                focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                return
            }
            scenarioName = binding.scenarioNameInput.text.toString()
            val scenario = RetirementViewModel.getScenario(scenarioName)
            if (scenario != null) {
                // scenario already exists with this name, can't save over it
                binding.scenarioNameInput.error = getString(R.string.name_already_exists)
                focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                return
            }
        } else { // is an update
            scenarioName = binding.scenarioSpinner.selectedItem.toString()
            binding.scenarioNameInput.error = null
            binding.scenarioNameEntireLayout.visibility = View.GONE
        }

        val uRadioGroup = binding.userIDRadioGroup
        val uRadioGroupChecked = uRadioGroup.checkedRadioButtonId
        val uRadioButton = requireActivity().findViewById(uRadioGroupChecked) as RadioButton

        val ageRadioGroup = binding.cppStartAgeRadioGroup
        val ageRadioGroupChecked = ageRadioGroup.checkedRadioButtonId
        val ageRadioButton = requireActivity().findViewById(ageRadioGroupChecked) as RadioButton
        val spenderIndex = SpenderViewModel.getSpenderIndex(uRadioButton.text.toString())

        if (inDefaultMode) {
            val rtData = RetirementData(
                scenarioName,
                SpenderViewModel.getSpenderIndex(uRadioButton.text.toString()),
                binding.targetMonthlyIncome.text.toString().toInt(),
                binding.retirementDate.text.toString(),
                binding.planToAge.text.toString().toInt(),
                ageRadioButton.text.toString().toInt(),
                (binding.minimizeRRSPSwitch.isChecked),
                binding.inflationRate.text.toString().toDouble(),
                binding.birthDate.text.toString()
            )
            rtData.salary = Salary(
                0,
                getString(R.string.salary),
                binding.salaryAmount.text.toString().toInt(),
                binding.salaryAnnualIncrease.text.toString().toDouble()
            )
            rtData.cpp = CPP(
                binding.cpp60Amount.text.toString().toInt(),
                binding.cpp65Amount.text.toString().toInt(),
                binding.cpp70Amount.text.toString().toInt(),
                binding.inflationRate.text.toString().toDouble()
            )
            rtData.oas = OAS(
                binding.oasCurrentAnnualAmount.text.toString().toInt(),
                binding.inflationRate.text.toString().toDouble()
            )
            val alistView: ListView = requireActivity().findViewById(R.id.assets_list_view)
            val assetCount = alistView.adapter?.count ?: 0
            rtData.assets.clear()
            for (i in 0 until assetCount) {
                val asset = alistView.adapter.getItem(i) as Asset
                rtData.assets.add(asset)
            }
            rtData.updateDistributionOrderAsRequired()
            val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
            val pensionCount = plistView.adapter?.count ?: 0
            rtData.pensions.clear()
            for (i in 0 until pensionCount) {
                val pension = plistView.adapter.getItem(i) as Pension
                rtData.pensions.add(pension)
            }
            RetirementViewModel.updateRetirementDefault(rtData, false)
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            MyApplication.displayToast(getString(R.string.default_updated))
            setupDefaultMode(false)
        } else {
            val userDefault = RetirementViewModel.getUserDefault(spenderIndex) ?: return

            val rtData = RetirementData(
                scenarioName,
                SpenderViewModel.getSpenderIndex(uRadioButton.text.toString()),
                binding.targetMonthlyIncome.text.toString().toInt(),
                binding.retirementDate.text.toString(),
                binding.planToAge.text.toString().toInt(),
                ageRadioButton.text.toString().toInt(),
                (binding.minimizeRRSPSwitch.isChecked),
                binding.inflationRate.text.toString().toDouble(),
                userDefault.birthDate
            )
            rtData.salary = Salary(
                0,
                getString(R.string.salary),
                binding.salaryAmount.text.toString().toInt(),
                binding.salaryAnnualIncrease.text.toString().toDouble()
            )
            rtData.setAssetsAndPensionsAndCPPandOAS(userDefault)
            val alistView: ListView = requireActivity().findViewById(R.id.assets_list_view)
//            rtData.assets.clear()
            val assetCount = alistView.adapter?.count ?: 0
            for (i in 0 until assetCount) {
                val asset = alistView.adapter.getItem(i) as Asset
                rtData.updateDistributionOrder(asset.id, i)
            }
            RetirementViewModel.updateRetirementScenario(rtData, false)
            MyApplication.playSound(context, R.raw.impact_jaw_breaker)

            setupScenarioSpinner(scenarioName)
            binding.scenarioNameEntireLayout.visibility = View.GONE
            if (iNewScenario)
                MyApplication.displayToast(getString(R.string.new_scenario_saved))
            else
                MyApplication.displayToast(getString(R.string.scenario_updated))
            binding.scenarioNameInput.setText("")
        }
    }
    private fun onSeeDetailsButtonClicked() {
        gRetirementDetailsList = lRetirementDetailsList
        findNavController().navigate(R.id.RetirementDetailsFragment)
    }
    private fun setupDefaultMode(iMoveToDefaultMode: Boolean) {
        inDefaultMode = iMoveToDefaultMode
        if (iMoveToDefaultMode) {
            binding.pageTitle.text = getString(R.string.retirement_defaults)
            binding.retirementDefaultsButton.visibility = View.GONE
            binding.scenarioSpinnerLabel.visibility = View.GONE
            binding.scenarioSpinnerRelativeLayout.visibility = View.GONE
            binding.buttonCancel.visibility = View.VISIBLE
            binding.buttonUpdateScenario.visibility = View.VISIBLE
//            binding.buttonUpdateScenario.text = getString(R.string.save)
            binding.buttonDeleteScenario.visibility = View.GONE
            binding.buttonSaveScenario.visibility = View.GONE
            binding.buttonCalculate.visibility = View.GONE
//            binding.buttonLayout.weightSum = 2.0F
            binding.assetsAddButton.visibility = View.VISIBLE
            binding.pensionsAddButton.visibility = View.VISIBLE
            val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            loadDefaults(RetirementViewModel.getUserDefault(SpenderViewModel.getSpenderIndex(radioButton.text.toString())))
        } else {
            binding.pageTitle.text = getString(R.string.calculate_retirement)
            binding.retirementDefaultsButton.visibility = View.VISIBLE
            val listOfRetirementScenarios = RetirementViewModel.getListOfRetirementScenarios()
            if (listOfRetirementScenarios.size > 0) {
                binding.scenarioSpinnerLabel.visibility = View.VISIBLE
                binding.scenarioSpinnerRelativeLayout.visibility = View.VISIBLE
            }
            binding.buttonSaveScenario.visibility = View.VISIBLE
            binding.buttonCalculate.visibility = View.VISIBLE
            binding.assetsAddButton.visibility = View.GONE
            binding.pensionsAddButton.visibility = View.GONE
            binding.buttonCancel.visibility = View.GONE
//            binding.buttonLayout.weightSum = 2.0F
            binding.buttonUpdateScenario.visibility = View.GONE
//            binding.buttonUpdateScenario.text = getString(R.string.save)
            binding.buttonDeleteScenario.visibility = View.GONE
        }
        binding.birthDate.isEnabled = iMoveToDefaultMode
        binding.cpp60Amount.isEnabled = iMoveToDefaultMode
        binding.cpp65Amount.isEnabled = iMoveToDefaultMode
        binding.cpp70Amount.isEnabled = iMoveToDefaultMode
        binding.oasCurrentAnnualAmount.isEnabled = iMoveToDefaultMode
        setSummaryFields(View.GONE)
        binding.scenarioNameInput.error = null
        binding.scenarioNameEntireLayout.visibility = View.GONE
        binding.scenarioSpinner.setSelection(0)
    }

    private fun loadDefaults(iRetirementData: RetirementData?) {
        if (iRetirementData != null) {
            val ageRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.cppStartAgeRadioGroup)
            for (i in 0 until ageRadioGroup.childCount) {
                val o = ageRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == iRetirementData.cppAge.toString()) {
                        o.isChecked = true
                    }
                }
            }
            val uRadioGroup = binding.userIDRadioGroup
            for (i in 0 until uRadioGroup.childCount) {
                val o = uRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == SpenderViewModel.getSpenderName(iRetirementData.userID)) {
                        o.isChecked = true
                    }
                }
            }
            binding.retirementDate.setText(iRetirementData.retirementDate)
            binding.targetMonthlyIncome.setText(gDec(iRetirementData.targetMonthlyIncome))
            binding.planToAge.setText(iRetirementData.planToAge.toString())
            binding.minimizeRRSPSwitch.isChecked = iRetirementData.minimizeTax
            binding.salaryAmount.setText(iRetirementData.salary.annualValueAfterTax.toString())
            binding.salaryAnnualIncrease.setText(iRetirementData.salary.estimatedGrowthPct.toString())
            binding.birthDate.setText(iRetirementData.birthDate)
            binding.inflationRate.setText(iRetirementData.inflationRate.toString())
            binding.cpp60Amount.setText(iRetirementData.cpp.annualValueAt60.toString())
            binding.cpp65Amount.setText(iRetirementData.cpp.annualValueAt65.toString())
            binding.cpp70Amount.setText(iRetirementData.cpp.annualValueAt70.toString())
            binding.oasCurrentAnnualAmount.setText(iRetirementData.oas.currentAnnualValue.toString())
            val assetsToLoad = if (inDefaultMode)
                iRetirementData
            else {
                // this block updates the scenario asset list to the current defaults, then reorders it to match the scenario ordering
                val assetList: MutableList<Asset> = ArrayList()
                val defaultAssetList = RetirementViewModel.getUserDefault(iRetirementData.userID)
                defaultAssetList?.assets?.forEach {
                    val copiedAsset = it.copy()
                    assetList.add(copiedAsset)
                    val defaultAssetID = it.id

                    val scAsset: Asset? = iRetirementData.assets.find {it.id == defaultAssetID}
                    if (scAsset != null) {
                        copiedAsset.distributionOrder = scAsset.distributionOrder
                    }
                }
                assetList.sortBy { it.distributionOrder }
                iRetirementData.assets = assetList
                iRetirementData
            }
            binding.assetsExpandButton.text =
                String.format(getString(R.string.assets), assetsToLoad.assets.size)
            val adapter = AssetAdapter(requireContext(), assetsToLoad,
                { item ->
                    moveAsset(item.distributionOrder, -1)
                },
                { item ->
                    moveAsset(item.distributionOrder, 1)
                }
            )
            val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
            listView.adapter = adapter
            setListViewHeightBasedOnChildren(listView)
            listView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                    if (inDefaultMode) {
                        val itemValue = listView.getItemAtPosition(position) as Asset
                        val cdf = RetirementAssetDialogFragment.newInstance(
                            iRetirementData.userID,
                            itemValue.id
                        )
                        cdf.setRetirementAssetDialogFragmentListener(object :
                            RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                            override fun onNewDataSaved() {
                                val myAdapter =
                                    RetirementViewModel.getUserDefault(iRetirementData.userID)
                                        ?.let { AssetAdapter(requireContext(), it,
                                            { item ->
                                                moveAsset(item.distributionOrder, -1) },
                                            { item ->
                                                moveAsset(item.distributionOrder, 1) }) }
                                listView.adapter = myAdapter
                                setListViewHeightBasedOnChildren(listView)
                                myAdapter?.notifyDataSetChanged()
                            }
                        })
                        cdf.show(parentFragmentManager, getString(R.string.edit_asset))
                    }
                }

            val pensionsToLoad = if (inDefaultMode)
                iRetirementData
            else
                RetirementViewModel.getUserDefault(iRetirementData.userID)
            if (pensionsToLoad != null) {
                binding.pensionsExpandButton.text =
                    String.format(getString(R.string.pensions), pensionsToLoad.pensions.size)
                val padapter = PensionAdapter(requireContext(), pensionsToLoad)
                val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
                plistView.adapter = padapter
                setListViewHeightBasedOnChildren(plistView)
                plistView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                        if (inDefaultMode) {
                            val itemValue = plistView.getItemAtPosition(position) as Pension
                            val cdf = RetirementPensionDialogFragment.newInstance(
                                iRetirementData.userID,
                                itemValue.id
                            )
                            cdf.setRetirementPensionDialogFragmentListener(object :
                                RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                                override fun onNewDataSaved() {
                                    val myAdapter =
                                        RetirementViewModel.getUserDefault(iRetirementData.userID)
                                            ?.let {
                                                PensionAdapter(
                                                    requireContext(),
                                                    it
                                                )
                                            }
                                    plistView.adapter = myAdapter
                                    setListViewHeightBasedOnChildren(plistView)
                                    myAdapter?.notifyDataSetChanged()
                                }
                            })
                            cdf.show(parentFragmentManager, getString(R.string.edit_pension))
                        }
                    }
            }
        } else {
            binding.retirementDate.setText("")
            binding.targetMonthlyIncome.setText("")
            binding.planToAge.setText("")
            binding.minimizeRRSPSwitch.isChecked = true
            binding.salaryAmount.setText("")
            binding.salaryAnnualIncrease.setText("")
            binding.birthDate.setText("")
            binding.inflationRate.setText("")
            binding.cpp60Amount.setText("")
            binding.cpp65Amount.setText("")
            binding.cpp70Amount.setText("")
            binding.oasCurrentAnnualAmount.setText("")
            val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
            listView.adapter = null
            setListViewHeightBasedOnChildren(listView)
            val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
            plistView.adapter = null
            setListViewHeightBasedOnChildren(plistView)
        }
    }
    private fun moveAsset(iDistributionOrder: Int, iDirection: Int) {
        val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        val adapter = listView.adapter as AssetAdapter

        if (inDefaultMode) {
            val selectedId = binding.userIDRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            val userID = SpenderViewModel.getSpenderIndex(radioButton.text.toString())

            RetirementViewModel.changeDefaultDistributionOrder(
                userID,
                iDistributionOrder,
                iDirection
            )

            val newData = RetirementViewModel.getUserDefault(userID)
            if (newData != null)
                adapter.refreshData(newData)
            setListViewHeightBasedOnChildren(listView)
            adapter.notifyDataSetChanged()
        } else {
            if (iDistributionOrder == 0 && iDirection == -1) {
                return
            }
            if (iDistributionOrder >= adapter.count - 1 && iDirection == 1) {
                return
            }
            val lAssets: MutableList<Asset> = ArrayList()
            for (i in 0 until adapter.count) {
                val asset = adapter.getItem(i) as Asset
                lAssets.add(asset.copy())
            }
            val temp = lAssets[iDistributionOrder + iDirection]
            lAssets[iDistributionOrder + iDirection] = lAssets[iDistributionOrder]
            lAssets[iDistributionOrder] = temp
            for (i in 0 until lAssets.count())
                lAssets[i].distributionOrder = i
            adapter.refreshData(lAssets)
            setListViewHeightBasedOnChildren(listView)
            adapter.notifyDataSetChanged()
        }
    }

    private fun setListViewHeightBasedOnChildren(listView: ListView) {
        val listAdapter = listView.adapter
        var totalHeight = 0

        val cnt = listAdapter?.count ?: 0

        for (i in 0 until cnt) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = (totalHeight
                + listView.dividerHeight * (cnt - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    private fun onBasicsExpandClicked(iForceClosed: Boolean = false) {
        if (binding.basicsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.basicsLayout.visibility = View.GONE
            binding.basicsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.basicsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onSalaryExpandClicked(iForceClosed: Boolean = false) {
        if (binding.salaryLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.salaryLayout.visibility = View.GONE
            binding.salaryExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.salaryLayout.visibility = View.VISIBLE
            binding.salaryExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onAssetsExpandClicked(iForceClosed: Boolean = false) {
        if (binding.assetsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.assetsLayout.visibility = View.GONE
            binding.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.assetsLayout.visibility = View.VISIBLE
            binding.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onPensionsExpandClicked(iForceClosed: Boolean = false) {
        if (binding.pensionsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.pensionsLayout.visibility = View.GONE
            binding.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.pensionsLayout.visibility = View.VISIBLE
            binding.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onCPPOASExpandClicked(iForceClosed: Boolean = false) {
        if (binding.cppOasLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.cppOasLayout.visibility = View.GONE
            binding.cppOasExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else { // ie retract the section
            binding.cppOasLayout.visibility = View.VISIBLE
            binding.cppOasExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }

    private fun collapseAll() {
        onBasicsExpandClicked(true)
        onSalaryExpandClicked(true)
        onAssetsExpandClicked(true)
        onPensionsExpandClicked(true)
        onCPPOASExpandClicked(true)
    }

    private fun loadUserButtons() {
        if (SpenderViewModel.getActiveCount() == 1) {
            binding.userIdLayout.visibility = View.GONE
        } else
            binding.userIdLayout.visibility = View.VISIBLE
        var ctr = 100
        val userRadioGroup = binding.userIDRadioGroup
        userRadioGroup.removeAllViews()

        for (i in 0 until SpenderViewModel.getActiveCount()) {
            val spender = SpenderViewModel.getSpender(i)
            if (spender?.name != getString(R.string.joint)) {
                val newRadioButton = RadioButton(requireContext())
                newRadioButton.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                newRadioButton.buttonTintList =
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            requireContext(),
                            R.attr.editTextBackground,
                            Color.BLACK
                        )
                    )
                newRadioButton.text = spender?.name
                newRadioButton.id = ctr++
                userRadioGroup.addView(newRadioButton)
                if (spender?.name == SpenderViewModel.getCurrentSpender()?.name) {
                    userRadioGroup.check(newRadioButton.id)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}