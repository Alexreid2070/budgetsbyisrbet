package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

class RetirementFragment : Fragment(), CoroutineScope {
    private var _binding: FragmentRetirementBinding? = null
    private val binding get() = _binding!!
    private var cal = android.icu.util.Calendar.getInstance()
    private var lRetirementDetailsList: MutableList<RetirementCalculationRow> = arrayListOf()
    private var myCalculationResponse = ""
    private var myCalculationResponse2 = ""
    private var myEarliestRetirementYear = 0
    private var myMaximumMonthlyBudget = 0
    private var myLifetimeTaxes = 0
    private var myLifetimeSurplus = 0
    private var myEndingNetWorth = 0
    private var myLastRow: RetirementCalculationRow? = null
    private var job: Job = Job()
    private var inDefaultMode = false // is set to true if user is editing defaults
    var loadRetirementInfoFromWorking = false
    var previousSpinnerSelection = -1

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
        binding.additionalExpandButton.setOnClickListener {
            onAdditionalItemsExpandClicked()
        }
        binding.cppOasExpandButton.setOnClickListener {
            onCPPOASExpandClicked()
        }
        binding.assetsAddButton.setOnClickListener {
            val cdf = RetirementAssetDialogFragment.newInstance(binding.userID.text.toString().toInt(), "", true)
            cdf.setRetirementAssetDialogFragmentListener(object: RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val userDefault = RetirementViewModel.getUserDefault(binding.userID.text.toString().toInt())
                    if (userDefault != null) {
                        val listView: ListView =
                            requireActivity().findViewById(R.id.assets_list_view)
                        val myAdapter = AssetAdapter(requireContext(),
                            userDefault.investmentGrowthRate,
                            userDefault.propertyGrowthRate,
                            { item ->
                                moveAsset(item.distributionOrder, -1)
                            },
                            { item ->
                                moveAsset(item.distributionOrder, 1)
                            })
                        listView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(listView)
                        binding.assetsExpandButton.text =
                            String.format(getString(R.string.assets), myAdapter.count)
                        myAdapter.notifyDataSetChanged()
                    }
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_asset))
        }

        binding.pensionsAddButton.setOnClickListener {
            val cdf = RetirementPensionDialogFragment.newInstance(binding.userID.text.toString().toInt(), "", true)
            cdf.setRetirementPensionDialogFragmentListener(object: RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val listView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
                    val myAdapter = PensionAdapter(requireContext())
                    listView.adapter = myAdapter
                    setListViewHeightBasedOnChildren(listView)
                    binding.pensionsExpandButton.text =
                        String.format(getString(R.string.pensions), myAdapter.count)
                    myAdapter.notifyDataSetChanged()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_pension))
        }

        binding.additionalAddButton.setOnClickListener {
            val cdf = RetirementAdditionalDialogFragment.newInstance(-1)
            cdf.setRetirementAdditionalDialogFragmentListener(object: RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    updateAdditionalItemsAdapters()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_additional_deposit_or_expense))
        }

        binding.scenarioSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selection = parent?.getItemAtPosition(position)
                binding.scenarioNameInput.setText(getStrippedScenarioName(selection.toString()))
                binding.scenarioNameInput.error = null
                binding.scenarioNameEntireLayout.visibility = View.GONE
                if (previousSpinnerSelection != position) { // a new scenario has been chosen
                    loadRetirementInfoFromWorking = false
                    previousSpinnerSelection = position
                    setSummaryFields(View.GONE)
                    if (position < SpenderViewModel.getNumberOfUsers()) {
                        val userDefault = RetirementViewModel.getUserDefault(position) ?: return
                        binding.userID.text = position.toString()
                        loadScreen(userDefault)
                        if (!inDefaultMode)
                            binding.buttonUpdateDefaults.visibility = View.GONE
                        binding.buttonDeleteScenario.visibility = View.GONE
                    } else {
                        val scenario = RetirementViewModel.getScenario(position - SpenderViewModel.getNumberOfUsers())
                        binding.userID.text = scenario.userID.toString()
                        loadScreen(scenario)
                        binding.buttonDeleteScenario.visibility = View.VISIBLE
                    }
                } else { // returned to the screen from See Details
                    loadScreen(null, true)
                }
            }
        }

        binding.buttonCancel.setOnClickListener {
            if (binding.scenarioNameEntireLayout.visibility == View.VISIBLE) { // we're saving a new scenario
                binding.scenarioNameInput.error = null
                binding.scenarioNameEntireLayout.visibility = View.GONE
                binding.buttonCalculate.visibility = View.VISIBLE
                binding.buttonCancel.visibility = View.GONE
            } else // we're in default edit mode
                setupDefaultMode(false)
        }

        binding.buttonCalculate.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onCalculateButtonClicked()
        }

        binding.scenarioSaveButton.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked(inDefaultMode)
        }
        binding.buttonUpdateDefaults.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked(inDefaultMode)
        }
        binding.buttonDeleteScenario.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onDeleteButtonClicked()
        }
        binding.buttonSeeDetails.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSeeDetailsButtonClicked()
        }
        binding.retirementDefaultsButton.setOnClickListener {
            setupDefaultMode(true)
            setupScenarioSpinner(RetirementViewModel.addUserToScenarioName(getString(R.string.defaultt),
                binding.userID.text.toString().toInt()))
        }

        inflater.inflate(R.layout.fragment_retirement, container, false)
        return binding.root
    }
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        setupDefaultMode(false)
        binding.currencySymbol1.text = String.format("${getLocalCurrencySymbol()} ")
        binding.currencySymbol2.text = String.format("${getLocalCurrencySymbol()} ")
        binding.currencySymbol3.text = String.format("${getLocalCurrencySymbol()} ")
        binding.currencySymbol4.text = String.format("${getLocalCurrencySymbol()} ")
        gRetirementDetailsList = arrayListOf()
        if (myEarliestRetirementYear == 0) {
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
        binding.userID.text = SpenderViewModel.myIndex().toString()
        setupScenarioSpinner(binding.scenarioNameInput.text.toString())
        binding.lifetimeTaxes.text = gDecWithCurrency(myLifetimeTaxes)
        binding.lifetimeSurplus.text = gDecWithCurrency(myLifetimeSurplus)
        binding.endingNetWorth.text = gDecWithCurrency(myEndingNetWorth)
        fillInSummaryFields()
        prepareCalculationResponse()
    }
    private fun getStrippedScenarioName(iLongName: String) : String {
        val paran = iLongName.indexOf(" (")
        return if (paran == -1)
            iLongName
        else
            iLongName.substring(0,paran)
    }

    private fun updateAdditionalItemsAdapters() {
        val expListView: ListView = requireActivity().findViewById(R.id.additional_expenditures_list_view)
        val myExpAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.EXPENSE)
        expListView.adapter = myExpAdapter
        setListViewHeightBasedOnChildren(expListView)
        val depListView: ListView = requireActivity().findViewById(R.id.additional_deposits_list_view)
        val myDepAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.DEPOSIT)
        depListView.adapter = myDepAdapter
        setListViewHeightBasedOnChildren(depListView)
        val totalItems = myExpAdapter.count + myDepAdapter.count
        binding.additionalExpandButton.text =
            String.format(getString(R.string.additional_deposits_or_expenditures), totalItems)
        binding.additionalDepositsLabel.text =
            String.format(getString(R.string.additional_deposits), myDepAdapter.count)
        binding.additionalExpensesLabel.text =
            String.format(getString(R.string.additional_expenditures), myExpAdapter.count)
        myExpAdapter.notifyDataSetChanged()
        myDepAdapter.notifyDataSetChanged()

    }

    private fun setupScenarioSpinner(iSelection: String = "", iOnlyDefaults: Boolean = false) {
        val listOfRetirementScenarios = RetirementViewModel.getListOfRetirementScenarios()
        for (i in 0 until SpenderViewModel.getNumberOfUsers()) {
            listOfRetirementScenarios.add(i,
                String.format(getString(R.string.user_default),
                        SpenderViewModel.getSpenderName(i)))
        }

        if (!iOnlyDefaults) {
            if (listOfRetirementScenarios.size > 0) {
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
                binding.buttonDeleteScenario.visibility = View.GONE
            }
        }
    }

    private fun setSummaryFields(iView: Int) {
        binding.calculationLayout.visibility = iView
        binding.buttonSeeDetails.visibility = iView
        binding.lifetimeTaxesLayout.visibility = iView
        binding.lifetimeSurplusLayout.visibility = iView
        binding.endingNetWorthLayout.visibility = iView

        if (iView == View.GONE) {
            myCalculationResponse = ""
            myCalculationResponse2 = ""
            myEarliestRetirementYear = 0
            myMaximumMonthlyBudget = 0
            myLastRow = null
            binding.calculationResponse.text = ""
            val listView: ListView = requireActivity().findViewById(R.id.results_list_view)
            listView.adapter = null
            binding.lifetimeTaxes.text = ""
            binding.lifetimeSurplus.text = ""
            binding.endingNetWorth.text = ""
        }
    }
    private fun onCalculateButtonClicked() {
        MyApplication.displayToast(getString(R.string.calculating))

        setSummaryFields(View.GONE)
        binding.scenarioNameInput.error = null
        binding.scenarioNameEntireLayout.visibility = View.GONE

        val rtData = createDataFromScreenValues()

        launch {
            val result =  getCalculationRows(rtData, false)
            gotTheCalculationRows(result, rtData.inflationRate) // onResult is called on the main thread
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

    private fun gotTheCalculationRows(result: MutableList<RetirementCalculationRow>,
        inflationRate: Double) {
        lRetirementDetailsList = result
        myLastRow = lRetirementDetailsList[lRetirementDetailsList.size - 1]
        var lifetimeTaxes = 0
        var lifetimeSurplus = 0.0
        result.forEach {
            val thisYearsTaxes = it.getTotalTax()
            lifetimeTaxes += thisYearsTaxes
            val thisYearsSurplus = it.getTotalAvailableIncome() - it.getTotalTargetIncome()
            lifetimeSurplus = (lifetimeSurplus * (1 + inflationRate/100.0)) + thisYearsSurplus
        }
        myLifetimeTaxes = lifetimeTaxes
        myLifetimeSurplus = lifetimeSurplus.toInt()
        fillInSummaryFields()
    }
    private fun fillInSummaryFields() {
        if (myLastRow != null) {
            myEndingNetWorth = myLastRow!!.getNetWorth(AssetType.ALL)
            binding.lifetimeTaxes.text = gDecWithCurrency(myLifetimeTaxes)
            binding.lifetimeSurplus.text = gDecWithCurrency(myLifetimeSurplus)
            binding.endingNetWorth.text = gDecWithCurrency(myEndingNetWorth)
            setSummaryFields(View.VISIBLE)

            val adapter = RetirementResultsAdapter(requireContext(), myLastRow!!)
            val listView: ListView = requireActivity().findViewById(R.id.results_list_view)
            listView.adapter = adapter
            setListViewHeightBasedOnChildren(listView)
        }
    }

    private fun getEarliestRetirementYear(iRetirementScenario: RetirementData) : Int {
        return RetirementViewModel.getEarliestRetirementYear(iRetirementScenario)
    }

    private fun gotTheEarliestRetirementYear(result: Int) {
//        binding.earliestRetirementYear.text = result.toString()
        myEarliestRetirementYear = result
//        binding.earliestRetirementYearLayout.visibility = View.VISIBLE
        prepareCalculationResponse()
    }

    private fun getMaximumMonthlyIncome(iRetirementScenario: RetirementData) : Int {
        return RetirementViewModel.getMaximumMonthlyIncome(iRetirementScenario)
    }

    private fun gotTheMaximumMonthlyIncome(result: Int) {
//        binding.maximumMonthlyBudget.text = gDecWithCurrency(result)
        myMaximumMonthlyBudget = result
//        binding.maximumMonthlyBudgetLayout.visibility = View.VISIBLE
        prepareCalculationResponse()
    }

    private fun prepareCalculationResponse() {
        if (myMaximumMonthlyBudget != 0 && myEarliestRetirementYear != 0) {
            val desiredBudget = if (binding.targetMonthlyIncome.text.toString() == "")
                0
            else
                binding.targetMonthlyIncome.text.toString().toInt()
            val desiredRetirementYear = if (binding.retirementDate.text.toString() == "")
                0
            else
                binding.retirementDate.text.toString().substring(0,4).toInt()

            if (myMaximumMonthlyBudget >= desiredBudget && myEarliestRetirementYear <= desiredRetirementYear) {
                myCalculationResponse = String.format(getString(R.string.excellent))
                myCalculationResponse2 = String.format(getString(R.string.excellent2), gDecWithCurrency(myMaximumMonthlyBudget))
                binding.calculationResponse.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green))
                binding.calculationResponse2.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green))
            } else {
                myCalculationResponse = String.format(getString(R.string.you_cannot))
                myCalculationResponse2 = String.format(getString(R.string.you_cannot2),
                    myEarliestRetirementYear, gDecWithCurrency(myMaximumMonthlyBudget))
                binding.calculationResponse.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red))
                binding.calculationResponse2.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red))
            }
            binding.calculationResponse.text = myCalculationResponse
            binding.calculationResponse2.text = myCalculationResponse2
        }
    }

    private fun onDeleteButtonClicked() {
        fun yesClicked() {
            val scenarioName = getStrippedScenarioName(binding.scenarioSpinner.selectedItem.toString())
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

        val scenarioName = getStrippedScenarioName(binding.scenarioSpinner.selectedItem.toString())
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.are_you_sure))
            .setMessage(String.format(getString(R.string.are_you_sure_that_you_want_to_delete_this_item),
                scenarioName))
            .setPositiveButton(android.R.string.ok) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> noClicked() }
            .show()
    }

    private fun onSaveButtonClicked(inDefaultMode: Boolean) {
        setSummaryFields(View.GONE)
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
        if (binding.investmentGrowthRate.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.investmentGrowthRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.investmentGrowthRate)
            return
        }
        if (binding.propertyGrowthRate.text.toString() == "") {
            binding.basicsLayout.visibility = View.VISIBLE
            binding.propertyGrowthRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.propertyGrowthRate)
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
        if (!inDefaultMode) {
            if (binding.scenarioNameEntireLayout.visibility == View.GONE ) {
                binding.scenarioNameEntireLayout.visibility = View.VISIBLE
                binding.buttonCancel.visibility = View.VISIBLE
                binding.buttonCalculate.visibility = View.GONE
                if (binding.scenarioNameInput.text.toString() == getString(R.string.defaultt))
                    binding.scenarioNameInput.setText("")  // don't allow user to save a scenario named "Default"
                focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                return
            } else {
                if (binding.scenarioNameInput.text.toString().trim() == "") {
                    binding.scenarioNameInput.error = getString(R.string.value_cannot_be_blank)
                    binding.buttonCalculate.visibility = View.GONE
//                binding.buttonUpdateScenario.visibility = View.GONE
                    binding.buttonCancel.visibility = View.VISIBLE
                    focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                    return
                }
                if (binding.scenarioNameInput.text.toString().trim() == getString(R.string.defaultt)) {
                    binding.scenarioNameInput.error = getString(R.string.invalid_name)
                    binding.buttonCalculate.visibility = View.GONE
//                binding.buttonUpdateScenario.visibility = View.GONE
                    binding.buttonCancel.visibility = View.VISIBLE
                    focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                    return
                }
                if (binding.scenarioNameInput.text.toString().indexOf("(") != -1) {
                    binding.scenarioNameInput.error = getString(R.string.invalid_name)
                    binding.buttonCalculate.visibility = View.GONE
//                binding.buttonUpdateScenario.visibility = View.GONE
                    binding.buttonCancel.visibility = View.VISIBLE
                    focusAndOpenSoftKeyboard(requireContext(), binding.scenarioNameInput)
                    return
                }
            }
        }

        val rtData = createDataFromScreenValues()

        if (inDefaultMode)
            RetirementViewModel.updateRetirementDefault(rtData, false)
        else
            RetirementViewModel.updateRetirementScenario(rtData, false)
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        if (inDefaultMode) {
            MyApplication.displayToast(getString(R.string.default_updated))
            setupDefaultMode(false)
            setupScenarioSpinner(RetirementViewModel.addUserToScenarioName(getString(R.string.defaultt), rtData.userID))
        } else {
            if (binding.scenarioSpinner.selectedItem.toString() != rtData.name)
                setupScenarioSpinner(RetirementViewModel.addUserToScenarioName(rtData.name, rtData.userID))
            binding.scenarioNameEntireLayout.visibility = View.GONE
            binding.buttonCancel.visibility = View.GONE
            binding.buttonCalculate.visibility = View.VISIBLE
            binding.scenarioNameInput.setText("")
            MyApplication.displayToast(getString(R.string.scenario_updated))
        }
        loadRetirementInfoFromWorking = false
    }

    private fun createDataFromScreenValues() : RetirementData {
        val ageRadioGroup = binding.cppStartAgeRadioGroup
        val ageRadioGroupChecked = ageRadioGroup.checkedRadioButtonId
        val ageRadioButton = requireActivity().findViewById(ageRadioGroupChecked) as RadioButton

        val scenarioName = if (inDefaultMode)
            getString(R.string.defaultt)
        else
            binding.scenarioNameInput.text.toString()
        val rtData = RetirementData(
            scenarioName,
            binding.userID.text.toString().toInt(),
            binding.targetMonthlyIncome.text.toString().toInt(),
            binding.retirementDate.text.toString(),
            binding.planToAge.text.toString().toInt(),
            ageRadioButton.text.toString().toInt(),
            binding.inflationRate.text.toString().toDouble(),
            binding.investmentGrowthRate.text.toString().toDouble(),
            binding.propertyGrowthRate.text.toString().toDouble(),
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
            binding.cpp70Amount.text.toString().toInt()
        )
        rtData.oas = OAS(
            binding.oasCurrentAnnualAmount.text.toString().toInt()
        )
        rtData.setAssetsAndPensionsAndAdditionalItemsFromWorking()

        return rtData
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
//            binding.scenarioSpinnerRelativeLayout.visibility = View.GONE
            binding.buttonCancel.visibility = View.VISIBLE
            binding.buttonUpdateDefaults.visibility = View.VISIBLE
            binding.buttonDeleteScenario.visibility = View.GONE
            binding.buttonCalculate.visibility = View.GONE
            binding.scenarioSaveButton.visibility = View.GONE
        } else {
            binding.pageTitle.text = getString(R.string.calculate_retirement)
            binding.retirementDefaultsButton.visibility = View.VISIBLE
            binding.buttonCalculate.visibility = View.VISIBLE
            binding.buttonCancel.visibility = View.GONE
            binding.buttonUpdateDefaults.visibility = View.GONE
            binding.buttonDeleteScenario.visibility = View.GONE
            binding.scenarioSaveButton.visibility = View.VISIBLE
        }
        binding.birthDate.isEnabled = iMoveToDefaultMode
        binding.cpp60Amount.isEnabled = iMoveToDefaultMode
        binding.cpp65Amount.isEnabled = iMoveToDefaultMode
        binding.cpp70Amount.isEnabled = iMoveToDefaultMode
        binding.oasCurrentAnnualAmount.isEnabled = iMoveToDefaultMode
        setSummaryFields(View.GONE)
        binding.scenarioNameInput.error = null
        binding.scenarioNameEntireLayout.visibility = View.GONE
        binding.scenarioSpinner.setSelection(SpenderViewModel.myIndex())
    }

    private fun loadScreen(iRetirementData: RetirementData?, iJustResetAdaptersFromWorking: Boolean = false) {
        if (iJustResetAdaptersFromWorking) {
            setupAdapters(binding.userID.text.toString().toInt(),
                binding.investmentGrowthRate.text.toString().toDouble(),
                binding.propertyGrowthRate.text.toString().toDouble())

        } else if (iRetirementData != null) {
            val ageRadioGroup = requireActivity().findViewById<RadioGroup>(R.id.cppStartAgeRadioGroup)
            for (i in 0 until ageRadioGroup.childCount) {
                val o = ageRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == iRetirementData.cppAge.toString()) {
                        o.isChecked = true
                    }
                }
            }
            binding.userID.text = iRetirementData.userID.toString()
            binding.retirementDate.setText(iRetirementData.retirementDate)
            binding.targetMonthlyIncome.setText(gDec(iRetirementData.targetMonthlyIncome))
            binding.planToAge.setText(iRetirementData.planToAge.toString())
            binding.salaryAmount.setText(iRetirementData.salary.annualValueAfterTax.toString())
            binding.salaryAnnualIncrease.setText(iRetirementData.salary.estimatedGrowthPct.toString())
            binding.birthDate.setText(iRetirementData.birthDate)
            binding.inflationRate.setText(iRetirementData.inflationRate.toString())
            binding.investmentGrowthRate.setText(iRetirementData.investmentGrowthRate.toString())
            binding.propertyGrowthRate.setText(iRetirementData.propertyGrowthRate.toString())
            binding.cpp60Amount.setText(iRetirementData.cpp.annualValueAt60.toString())
            binding.cpp65Amount.setText(iRetirementData.cpp.annualValueAt65.toString())
            binding.cpp70Amount.setText(iRetirementData.cpp.annualValueAt70.toString())
            binding.oasCurrentAnnualAmount.setText(iRetirementData.oas.currentAnnualValue.toString())
            if (!loadRetirementInfoFromWorking) {
                RetirementViewModel.populateWorkingAssetList(iRetirementData.assets)
            }
            if (!loadRetirementInfoFromWorking) {
                RetirementViewModel.populateWorkingPensionList(iRetirementData.pensions)
            }
            if (!loadRetirementInfoFromWorking) {
                RetirementViewModel.populateWorkingAdditionalList(iRetirementData.additionalItems)
            }
            setupAdapters(binding.userID.text.toString().toInt(),
                binding.investmentGrowthRate.text.toString().toDouble(),
                binding.propertyGrowthRate.text.toString().toDouble())
        } else {
            binding.retirementDate.setText("")
            binding.targetMonthlyIncome.setText("")
            binding.planToAge.setText("")
            binding.salaryAmount.setText("")
            binding.salaryAnnualIncrease.setText("")
            binding.birthDate.setText("")
            binding.inflationRate.setText("")
            binding.investmentGrowthRate.setText("")
            binding.propertyGrowthRate.setText("")
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
            val dlistView: ListView = requireActivity().findViewById(R.id.additional_deposits_list_view)
            dlistView.adapter = null
            setListViewHeightBasedOnChildren(dlistView)
            val elistView: ListView = requireActivity().findViewById(R.id.additional_expenditures_list_view)
            elistView.adapter = null
            setListViewHeightBasedOnChildren(elistView)
        }
    }
    private fun setupAdapters(iUserID: Int, iInvestmentGrowthRate: Double,
        iPropertyGrowthRate: Double) {
        val adapter = AssetAdapter(requireContext(),
            iInvestmentGrowthRate,
            iPropertyGrowthRate,
            { item ->
                moveAsset(item.distributionOrder, -1)
            },
            { item ->
                moveAsset(item.distributionOrder, 1)
            }
        )
        binding.assetsExpandButton.text =
            String.format(getString(R.string.assets), adapter.count)
        val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        listView.adapter = adapter
        setListViewHeightBasedOnChildren(listView)
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Asset
                val cdf = RetirementAssetDialogFragment.newInstance(
                    iUserID,
                    itemValue.name,
                    inDefaultMode
                )
                cdf.setRetirementAssetDialogFragmentListener(object :
                    RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        val myAdapter = AssetAdapter(requireContext(),
                            iInvestmentGrowthRate,
                            iPropertyGrowthRate,
                            { item ->
                                moveAsset(item.distributionOrder, -1) },
                            { item ->
                                moveAsset(item.distributionOrder, 1) })
                        listView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(listView)
                        binding.assetsExpandButton.text =
                            String.format(getString(R.string.assets), myAdapter.count)
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_asset))
            }

        binding.pensionsExpandButton.text =
            String.format(getString(R.string.pensions), RetirementViewModel.getWorkingPensionListCount())
        val padapter = PensionAdapter(requireContext())
        val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
        plistView.adapter = padapter
        setListViewHeightBasedOnChildren(plistView)
        plistView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = plistView.getItemAtPosition(position) as Pension
                val cdf = RetirementPensionDialogFragment.newInstance(
                    iUserID,
                    itemValue.name,
                    inDefaultMode
                )
                cdf.setRetirementPensionDialogFragmentListener(object :
                    RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        val myAdapter = PensionAdapter(requireContext())
                        plistView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(plistView)
                        binding.pensionsExpandButton.text =
                            String.format(getString(R.string.pensions), RetirementViewModel.getWorkingPensionListCount())
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_pension))
            }

        binding.additionalExpandButton.text =
            String.format(getString(R.string.additional_deposits_or_expenditures), RetirementViewModel.getWorkingAdditionalListCount())
        binding.additionalDepositsLabel.text =
            String.format(getString(R.string.additional_deposits),
                RetirementViewModel.getWorkingAdditionalListCount(AdditionalType.DEPOSIT))
        binding.additionalExpensesLabel.text =
            String.format(getString(R.string.additional_expenditures),
                RetirementViewModel.getWorkingAdditionalListCount(AdditionalType.EXPENSE))
        val expAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.EXPENSE)
        val expListView: ListView = requireActivity().findViewById(R.id.additional_expenditures_list_view)
        expListView.adapter = expAdapter
        setListViewHeightBasedOnChildren(expListView)
        val depAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.DEPOSIT)
        val depListView: ListView = requireActivity().findViewById(R.id.additional_deposits_list_view)
        depListView.adapter = depAdapter
        setListViewHeightBasedOnChildren(depListView)
        expListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val item: AdditionalItem = expListView.adapter.getItem(position) as AdditionalItem
                val cdf = RetirementAdditionalDialogFragment.newInstance(
                    item.id
                )
                cdf.setRetirementAdditionalDialogFragmentListener(object :
                    RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        updateAdditionalItemsAdapters()
                        if (RetirementViewModel.getWorkingAdditionalListCount() == 0) {
                            onAdditionalItemsExpandClicked(true)
                        }
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_expense))
            }
        depListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val item: AdditionalItem = depListView.adapter.getItem(position) as AdditionalItem
                val cdf = RetirementAdditionalDialogFragment.newInstance(
                    item.id
                )
                cdf.setRetirementAdditionalDialogFragmentListener(object :
                    RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        updateAdditionalItemsAdapters()
                        if (RetirementViewModel.getWorkingAdditionalListCount() == 0) {
                            onAdditionalItemsExpandClicked(true)
                        }
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_deposit))
            }
    }
    private fun moveAsset(iDistributionOrder: Int, iDirection: Int) {
        val listView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        val adapter = listView.adapter as AssetAdapter

        RetirementViewModel.changeDefaultDistributionOrder(
            iDistributionOrder,
            iDirection
        )

        adapter.refreshData()
        setListViewHeightBasedOnChildren(listView)
        adapter.notifyDataSetChanged()
        loadRetirementInfoFromWorking = true
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
        val plistView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        val assetCount = plistView.adapter?.count ?: 0
        if (binding.assetsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.assetsLayout.visibility = View.GONE
            binding.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (assetCount > 0){
            binding.assetsLayout.visibility = View.VISIBLE
            binding.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onPensionsExpandClicked(iForceClosed: Boolean = false) {
        val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
        val pensionCount = plistView.adapter?.count ?: 0
        if (binding.pensionsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.pensionsLayout.visibility = View.GONE
            binding.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (pensionCount > 0){
            binding.pensionsLayout.visibility = View.VISIBLE
            binding.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onAdditionalItemsExpandClicked(iForceClosed: Boolean = false) {
        val depListView: ListView = requireActivity().findViewById(R.id.additional_deposits_list_view)
        val expListView: ListView = requireActivity().findViewById(R.id.additional_expenditures_list_view)
        val totCount = (depListView.adapter?.count ?: 0) + (expListView.adapter?.count ?: 0)
        if (binding.additionalDepositsLayout.visibility == View.VISIBLE || iForceClosed) {
            binding.additionalDepositsLayout.visibility = View.GONE
            binding.additionalExpendituresLayout.visibility = View.GONE
            binding.additionalExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (totCount > 0) {
            binding.additionalDepositsLayout.visibility = View.VISIBLE
            binding.additionalExpendituresLayout.visibility = View.VISIBLE
            binding.additionalExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
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
        onAdditionalItemsExpandClicked(true)
        onCPPOASExpandClicked(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}