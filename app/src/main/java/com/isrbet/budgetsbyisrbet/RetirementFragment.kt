package com.isrbet.budgetsbyisrbet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class RetirementFragment : Fragment(), CoroutineScope {
    private var _binding: FragmentRetirementBinding? = null
    private val binding get() = _binding!!
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
    var loadRetirementInfoFromWorking = false
    var previousSpinnerSelection = -1
    var currentUserID = SpenderViewModel.myIndex()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag("Alex").d("Retirement onCreate")
        super.onCreate(savedInstanceState)

        val retObserver = Observer<MutableList<RetirementData>> {
            Timber.tag("Alex").d("Retirement Observer triggered")
            val userDefault = RetirementViewModel.getUserDefault(currentUserID)
            loadScreen(userDefault)
        }
        RetirementViewModel.observeList(this, retObserver)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.tag("Alex").d("Retirement onCreateView")
        _binding = FragmentRetirementBinding.inflate(inflater, container, false)

        binding.rf.expandAllButton.setOnClickListener {
            onAllClicked()
        }
        binding.rf.basicsExpandButton.setOnClickListener {
            onBasicsExpandClicked()
        }
        binding.rf.salaryExpandButton.setOnClickListener {
            onSalaryExpandClicked()
        }
        binding.rf.assetsExpandButton.setOnClickListener {
            onAssetsExpandClicked()
        }
        binding.rf.pensionsExpandButton.setOnClickListener {
            onPensionsExpandClicked()
        }
        binding.rf.additionalExpandButton.setOnClickListener {
            onAdditionalItemsExpandClicked()
        }
        binding.rf.cppOasExpandButton.setOnClickListener {
            onCPPOASExpandClicked()
        }
        binding.rf.assetsAddButton.setOnClickListener {
            val cdf = RetirementAssetDialogFragment.newInstance(currentUserID, "", true)
            cdf.setRetirementAssetDialogFragmentListener(object: RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val userDefault = RetirementViewModel.getUserDefault(currentUserID)
                    if (userDefault != null) {
                        val myAdapter = AssetAdapter(requireContext(),
                            userDefault.investmentGrowthRate,
                            userDefault.propertyGrowthRate,
                            { item ->
                                moveAsset(item.distributionOrder, -1)
                            },
                            { item ->
                                moveAsset(item.distributionOrder, 1)
                            })
                        binding.rf.assetsListView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(binding.rf.assetsListView)
                        binding.rf.assetsExpandButton.text =
                            String.format(getString(R.string.assets), myAdapter.count)
                        myAdapter.notifyDataSetChanged()
                    }
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_asset))
        }

        binding.rf.pensionsAddButton.setOnClickListener {
            val cdf = RetirementPensionDialogFragment.newInstance(currentUserID, "", true)
            cdf.setRetirementPensionDialogFragmentListener(object: RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val myAdapter = PensionAdapter(requireContext())
                    binding.rf.pensionsListView.adapter = myAdapter
                    setListViewHeightBasedOnChildren(binding.rf.pensionsListView)
                    binding.rf.pensionsExpandButton.text =
                        String.format(getString(R.string.pensions), myAdapter.count)
                    myAdapter.notifyDataSetChanged()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_pension))
        }

        binding.rf.additionalAddButton.setOnClickListener {
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
                        val userDefault = RetirementViewModel.getUserDefault(position) // ?: return
                        currentUserID = position
                        Timber.tag("Alex").d("Just set A binding.userid to $currentUserID in scenarioOnItemSelected")
                        loadScreen(userDefault)
                        binding.buttonDeleteScenario.visibility = View.GONE
                    } else {
                        val scenario = RetirementViewModel.getScenario(position - SpenderViewModel.getNumberOfUsers())
                        currentUserID = scenario.userID
                        Timber.tag("Alex").d("Just set B binding.userid to $currentUserID in scenarioOnItemSelected")
                        loadScreen(scenario)
                        binding.buttonDeleteScenario.visibility = View.VISIBLE
                    }
                } else { // returned to the screen from See Details
                    Timber.tag("Alex").d("Did I just return?  Loading screen")
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
            }
        }

        binding.buttonCalculate.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onCalculateButtonClicked()
        }

        binding.scenarioSaveButton.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked()
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
            val action =
                RetirementFragmentDirections.actionRetirementFragmentToRetirementDefaultsFragment()
            action.userID = currentUserID
            Timber.tag("Alex").d("UserID is $currentUserID")
            findNavController().navigate(action)
        }
        binding.rf.expandAllButton.tag = R.drawable.ic_baseline_keyboard_double_arrow_down_24

        inflater.inflate(R.layout.fragment_retirement, container, false)
        return binding.root
    }
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        Timber.tag("Alex").d("Retirement onViewCreated")
        super.onViewCreated(itemView, savedInstanceState)
        setupDefaults()
        binding.rf.assetsExpandButton.text = String.format(getString(R.string.assets), 0)
        binding.rf.pensionsExpandButton.text = String.format(getString(R.string.pensions), 0)
        binding.rf.additionalExpandButton.text = String.format(getString(R.string.additional_deposits_or_expenditures), 0)

        binding.rf.currencySymbol2.text = String.format("${getLocalCurrencySymbol()} ")
        binding.rf.currencySymbol3.text = String.format("${getLocalCurrencySymbol()} ")
        binding.rf.currencySymbol4.text = String.format("${getLocalCurrencySymbol()} ")
        gRetirementDetailsList = arrayListOf()
//        gRetirementScenario = null
        if (myEarliestRetirementYear == 0) {
            setSummaryFields(View.GONE)
        }
        binding.rf.birthDate.setText(gCurrentDate.toString())
        val birthDateSetListener = // this is fired when user clicks OK
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.rf.birthDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }
        binding.rf.birthDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.rf.birthDate.text.toString() != "") {
                lcal = MyDate(binding.rf.birthDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), birthDateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }
        binding.rf.retirementDate.setText(gCurrentDate.toString())
        val retDateSetListener = // this is fired when user clicks OK
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                binding.rf.retirementDate.setText(MyDate(year, monthOfYear+1, dayOfMonth).toString())
            }
        binding.rf.retirementDate.setOnClickListener {
            var lcal = MyDate()
            if (binding.rf.retirementDate.text.toString() != "") {
                lcal = MyDate(binding.rf.retirementDate.text.toString())
            }
            DatePickerDialog(
                requireContext(), retDateSetListener,
                lcal.getYear(),
                lcal.getMonth()-1,
                lcal.getDay()
            ).show()
        }
        binding.rf.switchUseBudget.setOnClickListener {
            if (binding.rf.switchUseBudget.isChecked) {
                binding.rf.targetMonthlyIncome.visibility = View.GONE
                binding.rf.targetMonthlyIncome.setText("")
            } else {
                binding.rf.targetMonthlyIncome.visibility = View.VISIBLE
            }
        }
        setupScenarioSpinner(RetirementViewModel.addUserToScenarioName(getString(R.string.defaultt),
            currentUserID))
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
        binding.rf.additionalExpandButton.text =
            String.format(getString(R.string.additional_deposits_or_expenditures), totalItems)
        binding.rf.additionalDepositsLabel.text =
            String.format(getString(R.string.additional_deposits), myDepAdapter.count)
        binding.rf.additionalExpensesLabel.text =
            String.format(getString(R.string.additional_expenditures), myExpAdapter.count)
        myExpAdapter.notifyDataSetChanged()
        myDepAdapter.notifyDataSetChanged()

    }

    private fun setupScenarioSpinner(iSelection: String = "", iOnlyDefaults: Boolean = false) {
        var listOfRetirementScenarios: MutableList<String> = arrayListOf()
        if (iOnlyDefaults) {
            for (i in 0 until SpenderViewModel.getNumberOfUsers()) {
                listOfRetirementScenarios.add(i,
                    String.format(getString(R.string.user_default),
                        SpenderViewModel.getSpenderName(i)))
            }
        } else {
            listOfRetirementScenarios = RetirementViewModel.getListOfRetirementScenarios()
            for (i in 0 until SpenderViewModel.getNumberOfUsers()) {
                listOfRetirementScenarios.add(i,
                    String.format(getString(R.string.user_default),
                        SpenderViewModel.getSpenderName(i)))
            }
        }

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
            binding.resultsListView.adapter = null
            binding.lifetimeTaxes.text = ""
            binding.lifetimeSurplus.text = ""
            binding.endingNetWorth.text = ""
        }
    }
    private fun onCalculateButtonClicked() {
        if (!checkThatAllFieldsAreOK())
            return

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
            rtData.useBudgetAsTargetIncome = false
            val result = getMaximumMonthlyIncome(rtData)
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
            val desiredBudget = if (binding.rf.targetMonthlyIncome.text.toString() == "")
                0
            else
                binding.rf.targetMonthlyIncome.text.toString().toInt()
            val desiredRetirementYear = if (binding.rf.retirementDate.text.toString() == "")
                0
            else
                binding.rf.retirementDate.text.toString().substring(0,4).toInt()

            if (myMaximumMonthlyBudget >= desiredBudget && myEarliestRetirementYear <= desiredRetirementYear) {
                myCalculationResponse = String.format(getString(R.string.excellent))
                myCalculationResponse2 = String.format(getString(R.string.excellent2), gDecWithCurrency(myMaximumMonthlyBudget), gDecWithCurrency(myMaximumMonthlyBudget*12))
                binding.calculationResponse.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green))
                binding.calculationResponse2.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green))
                binding.konfettiView.bringToFront()
                binding.konfettiView.build()
                    .addColors(Color.RED, Color.CYAN, Color.GREEN, Color.YELLOW)
                    .setDirection(0.0, 359.0)
                    .setSpeed(1f, 6f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(1000L)
                    .addShapes(Shape.Square, Shape.Circle)
                    .addSizes(Size(12))
                    .setPosition(-50f, binding.konfettiView.width+150f, -50f, binding.konfettiView.height+150f)
                    .streamFor(300, 2000L)
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
        } else {
            myCalculationResponse = String.format(getString(R.string.you_cannot))
            binding.calculationResponse.text = myCalculationResponse
            myCalculationResponse2 = ""
            binding.calculationResponse2.text = myCalculationResponse2
            binding.calculationResponse.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.red))
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

    private fun checkThatAllFieldsAreOK(): Boolean {
        if (binding.rf.birthDate.text.toString() == "") {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.birthDate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.birthDate)
            return false
        }
        if (binding.rf.inflationRate.text.toString() == "") {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.inflationRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.inflationRate)
            return false
        }
        if (binding.rf.investmentGrowthRate.text.toString() == "") {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.investmentGrowthRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.investmentGrowthRate)
            return false
        }
        if (binding.rf.propertyGrowthRate.text.toString() == "") {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.propertyGrowthRate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.propertyGrowthRate)
            return false
        }
        if (binding.rf.targetMonthlyIncome.text.toString() == "" &&
                !binding.rf.switchUseBudget.isChecked) {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.targetMonthlyIncome.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.targetMonthlyIncome)
            return false
        }
        if (binding.rf.planToAge.text.toString() == "") {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.planToAge.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.planToAge)
            return false
        }
        if (binding.rf.retirementDate.text.toString() == "") {
            binding.rf.salaryLayout.visibility = View.VISIBLE
            binding.rf.retirementDate.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.retirementDate)
            return false
        }
        if (binding.rf.cpp60Amount.text.toString() == "") {
            binding.rf.cppOasLayout.visibility = View.VISIBLE
            binding.rf.cpp60Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.cpp60Amount)
            return false
        }
        if (binding.rf.cpp65Amount.text.toString() == "") {
            binding.rf.cppOasLayout.visibility = View.VISIBLE
            binding.rf.cpp65Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.cpp65Amount)
            return false
        }
        if (binding.rf.cpp70Amount.text.toString() == "") {
            binding.rf.cppOasLayout.visibility = View.VISIBLE
            binding.rf.cpp70Amount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.cpp70Amount)
            return false
        }
        if (binding.rf.oasCurrentAnnualAmount.text.toString() == "") {
            binding.rf.cppOasLayout.visibility = View.VISIBLE
            binding.rf.oasCurrentAnnualAmount.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.rf.oasCurrentAnnualAmount)
            return false
        }
        return true
    }
    private fun onSaveButtonClicked() {
        setSummaryFields(View.GONE)
        if (!checkThatAllFieldsAreOK())
            return
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

        val rtData = createDataFromScreenValues()

        RetirementViewModel.updateRetirementScenario(rtData, false)
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        if (binding.scenarioSpinner.selectedItem.toString() != rtData.name)
            setupScenarioSpinner(RetirementViewModel.addUserToScenarioName(rtData.name, rtData.userID))
        binding.scenarioNameEntireLayout.visibility = View.GONE
        binding.buttonCancel.visibility = View.GONE
        binding.buttonCalculate.visibility = View.VISIBLE
        binding.scenarioNameInput.setText("")
        MyApplication.displayToast(getString(R.string.scenario_updated))
        loadRetirementInfoFromWorking = false
    }

    private fun createDataFromScreenValues() : RetirementData {
        val ageRadioGroup = binding.rf.cppStartAgeRadioGroup
        val ageRadioGroupChecked = ageRadioGroup.checkedRadioButtonId
        val ageRadioButton = requireActivity().findViewById(ageRadioGroupChecked) as RadioButton

        val scenarioName = binding.scenarioNameInput.text.toString()
        val targetIncome = if (binding.rf.switchUseBudget.isChecked) 0
        else binding.rf.targetMonthlyIncome.text.toString().toInt()
        val rtData = RetirementData(
            scenarioName,
            currentUserID,
            targetIncome,
            binding.rf.switchUseBudget.isChecked,
            binding.rf.retirementDate.text.toString(),
            binding.rf.planToAge.text.toString().toInt(),
            ageRadioButton.text.toString().toInt(),
            binding.rf.inflationRate.text.toString().toDouble(),
            binding.rf.investmentGrowthRate.text.toString().toDouble(),
            binding.rf.propertyGrowthRate.text.toString().toDouble(),
            binding.rf.birthDate.text.toString()
        )
        if (binding.rf.salaryAmount.text.toString().toIntOrNull() != null) {
            rtData.salary = Salary(
                0,
                getString(R.string.salary),
                binding.rf.salaryAmount.text.toString().toInt(),
                binding.rf.salaryAnnualIncrease.text.toString().toDouble()
            )
        }
        rtData.cpp = CPP(
            binding.rf.cpp60Amount.text.toString().toInt(),
            binding.rf.cpp65Amount.text.toString().toInt(),
            binding.rf.cpp70Amount.text.toString().toInt()
        )
        rtData.oas = OAS(
            binding.rf.oasCurrentAnnualAmount.text.toString().toInt()
        )

        rtData.setAssetsAndPensionsAndAdditionalItemsFromWorking()

        gRetirementScenario = rtData
        return rtData
    }

    private fun onSeeDetailsButtonClicked() {
        gRetirementDetailsList = lRetirementDetailsList
//        gRetirementScenario = createDataFromScreenValues()
        val action =
            RetirementFragmentDirections.actionRetirementFragmentToRetirementDetailsFragment()
        action.scenarioName = binding.scenarioNameInput.text.toString()
        action.userID = currentUserID
        findNavController().navigate(action)
    }
    private fun setupDefaults() {
        binding.pageTitle.text = getString(R.string.calculate_retirement)
        binding.rf.switchUseBudget.visibility = View.VISIBLE
        binding.retirementDefaultsButton.visibility = View.VISIBLE
        binding.buttonCalculate.visibility = View.VISIBLE
        binding.buttonCancel.visibility = View.GONE
        binding.buttonDeleteScenario.visibility = View.GONE
        binding.scenarioSaveButton.visibility = View.VISIBLE
        binding.rf.birthDate.isEnabled = false
        binding.rf.cpp60Amount.isEnabled = false
        binding.rf.cpp65Amount.isEnabled = false
        binding.rf.cpp70Amount.isEnabled = false
        binding.rf.oasCurrentAnnualAmount.isEnabled = false
        setSummaryFields(View.GONE)
        binding.scenarioNameInput.error = null
        binding.scenarioNameEntireLayout.visibility = View.GONE
        binding.scenarioSpinner.setSelection(SpenderViewModel.myIndex())
    }

    private fun loadScreen(iRetirementData: RetirementData?, iJustResetAdaptersFromWorking: Boolean = false) {
        if (iJustResetAdaptersFromWorking) {
            val inv = binding.rf.investmentGrowthRate.text.toString().replace(',', '.').toDoubleOrNull()
            val prop = binding.rf.propertyGrowthRate.text.toString().replace(',', '.').toDoubleOrNull()
            if (inv != null && prop != null) {
                setupAdapters(
                    currentUserID,
                    binding.rf.investmentGrowthRate.text.toString().toDouble(),
                    binding.rf.propertyGrowthRate.text.toString().toDouble()
                )
            }
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
            currentUserID = iRetirementData.userID
            Timber.tag("Alex").d("Just set D binding.userid to $currentUserID in loadScreen")
            binding.rf.retirementDate.setText(iRetirementData.retirementDate)
            binding.rf.switchUseBudget.isChecked = iRetirementData.useBudgetAsTargetIncome
            if (iRetirementData.useBudgetAsTargetIncome) {
                binding.rf.targetMonthlyIncome.visibility = View.GONE
                binding.rf.targetMonthlyIncome.setText("")
            } else {
                binding.rf.targetMonthlyIncome.visibility = View.VISIBLE
                binding.rf.targetMonthlyIncome.setText(gDec(iRetirementData.targetMonthlyIncome))
            }
            binding.rf.planToAge.setText(iRetirementData.planToAge.toString())
            binding.rf.salaryAmount.setText(iRetirementData.salary.annualValueAfterTax.toString())
            binding.rf.salaryAnnualIncrease.setText(iRetirementData.salary.estimatedGrowthPct.toString())
            binding.rf.birthDate.setText(iRetirementData.birthDate)
            binding.rf.inflationRate.setText(iRetirementData.inflationRate.toString())
            binding.rf.investmentGrowthRate.setText(iRetirementData.investmentGrowthRate.toString())
            binding.rf.propertyGrowthRate.setText(iRetirementData.propertyGrowthRate.toString())
            binding.rf.cpp60Amount.setText(iRetirementData.cpp.annualValueAt60.toString())
            binding.rf.cpp65Amount.setText(iRetirementData.cpp.annualValueAt65.toString())
            binding.rf.cpp70Amount.setText(iRetirementData.cpp.annualValueAt70.toString())
            binding.rf.oasCurrentAnnualAmount.setText(iRetirementData.oas.currentAnnualValue.toString())
            gRetirementScenario = iRetirementData.copy()
            setupAdapters(currentUserID,
                binding.rf.investmentGrowthRate.text.toString().toDouble(),
                binding.rf.propertyGrowthRate.text.toString().toDouble())
        } else {
            binding.rf.retirementDate.setText("")
            binding.rf.targetMonthlyIncome.setText("")
            binding.rf.planToAge.setText("")
            binding.rf.salaryAmount.setText("")
            binding.rf.salaryAnnualIncrease.setText("")
            binding.rf.birthDate.setText("")
            binding.rf.inflationRate.setText("")
            binding.rf.investmentGrowthRate.setText("")
            binding.rf.propertyGrowthRate.setText("")
            binding.rf.cpp60Amount.setText("")
            binding.rf.cpp65Amount.setText("")
            binding.rf.cpp70Amount.setText("")
            binding.rf.oasCurrentAnnualAmount.setText("")
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
        binding.rf.assetsExpandButton.text =
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
                    false
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
                        binding.rf.assetsExpandButton.text =
                            String.format(getString(R.string.assets), myAdapter.count)
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_asset))
            }

        binding.rf.pensionsExpandButton.text =
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
                    false
                )
                cdf.setRetirementPensionDialogFragmentListener(object :
                    RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        val myAdapter = PensionAdapter(requireContext())
                        plistView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(plistView)
                        binding.rf.pensionsExpandButton.text =
                            String.format(getString(R.string.pensions), RetirementViewModel.getWorkingPensionListCount())
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_pension))
            }

        binding.rf.additionalExpandButton.text =
            String.format(getString(R.string.additional_deposits_or_expenditures), RetirementViewModel.getWorkingAdditionalListCount())
        binding.rf.additionalDepositsLabel.text =
            String.format(getString(R.string.additional_deposits),
                RetirementViewModel.getWorkingAdditionalListCount(AdditionalType.DEPOSIT))
        binding.rf.additionalExpensesLabel.text =
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
                            onAdditionalItemsExpandClicked("Closed")
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
                            onAdditionalItemsExpandClicked("Closed")
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

    private fun onBasicsExpandClicked(iForceOpenOrClosed: String = "") {
        if ((binding.rf.basicsLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.basicsLayout.visibility = View.GONE
            binding.rf.basicsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.rf.basicsLayout.visibility = View.VISIBLE
            binding.rf.basicsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onSalaryExpandClicked(iForceOpenOrClosed: String = "") {
        if ((binding.rf.salaryLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.salaryLayout.visibility = View.GONE
            binding.rf.salaryExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else {
            binding.rf.salaryLayout.visibility = View.VISIBLE
            binding.rf.salaryExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onAssetsExpandClicked(iForceOpenOrClosed: String = "") {
        val plistView: ListView = requireActivity().findViewById(R.id.assets_list_view)
        val assetCount = plistView.adapter?.count ?: 0
        if ((binding.rf.assetsLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.assetsLayout.visibility = View.GONE
            binding.rf.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (assetCount > 0) {
            binding.rf.assetsLayout.visibility = View.VISIBLE
            binding.rf.assetsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onPensionsExpandClicked(iForceOpenOrClosed: String = "") {
        val plistView: ListView = requireActivity().findViewById(R.id.pensions_list_view)
        val pensionCount = plistView.adapter?.count ?: 0
        if ((binding.rf.pensionsLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.pensionsLayout.visibility = View.GONE
            binding.rf.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (pensionCount > 0) {
            binding.rf.pensionsLayout.visibility = View.VISIBLE
            binding.rf.pensionsExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onAdditionalItemsExpandClicked(iForceOpenOrClosed: String = "") {
        val depListView: ListView = requireActivity().findViewById(R.id.additional_deposits_list_view)
        val expListView: ListView = requireActivity().findViewById(R.id.additional_expenditures_list_view)
        val totCount = (depListView.adapter?.count ?: 0) + (expListView.adapter?.count ?: 0)
        if ((binding.rf.additionalDepositsLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.additionalDepositsLayout.visibility = View.GONE
            binding.rf.additionalExpendituresLayout.visibility = View.GONE
            binding.rf.additionalExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else if (totCount > 0) {
            binding.rf.additionalDepositsLayout.visibility = View.VISIBLE
            binding.rf.additionalExpendituresLayout.visibility = View.VISIBLE
            binding.rf.additionalExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }
    private fun onCPPOASExpandClicked(iForceOpenOrClosed: String = "") {
        if ((binding.rf.cppOasLayout.visibility == View.VISIBLE && iForceOpenOrClosed != "Open") ||
            iForceOpenOrClosed == "Closed") {
            binding.rf.cppOasLayout.visibility = View.GONE
            binding.rf.cppOasExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_more_24, 0)
        } else { // ie retract the section
            binding.rf.cppOasLayout.visibility = View.VISIBLE
            binding.rf.cppOasExpandButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_baseline_expand_less_24, 0)
        }
    }

    private fun onAllClicked() {
        if (binding.rf.expandAllButton.tag == R.drawable.ic_baseline_keyboard_double_arrow_down_24) {
            // need to expand all
            binding.rf.expandAllButton.tag = R.drawable.ic_baseline_keyboard_double_arrow_up_24
            binding.rf.expandAllButton.setImageDrawable(context?.let
            { ContextCompat.getDrawable(it,R.drawable.ic_baseline_keyboard_double_arrow_up_24) } )
            onExpandAllClicked()
        } else { // need to collapse all
            binding.rf.expandAllButton.tag = R.drawable.ic_baseline_keyboard_double_arrow_down_24
            binding.rf.expandAllButton.setImageDrawable(context?.let
            { ContextCompat.getDrawable(it,R.drawable.ic_baseline_keyboard_double_arrow_down_24) } )
            collapseAll()
        }
    }

    private fun collapseAll() {
        onBasicsExpandClicked("Closed")
        onSalaryExpandClicked("Closed")
        onAssetsExpandClicked("Closed")
        onPensionsExpandClicked("Closed")
        onAdditionalItemsExpandClicked("Closed")
        onCPPOASExpandClicked("Closed")
    }

    private fun onExpandAllClicked() {
        onBasicsExpandClicked("Open")
        onSalaryExpandClicked("Open")
        onAssetsExpandClicked("Open")
        onPensionsExpandClicked("Open")
        onAdditionalItemsExpandClicked("Open")
        onCPPOASExpandClicked("Open")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}