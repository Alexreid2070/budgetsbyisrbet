package com.isrbet.budgetsbyisrbet

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.isrbet.budgetsbyisrbet.databinding.FragmentRetirementDefaultsBinding
import timber.log.Timber

class RetirementDefaultsFragment : Fragment() {
    private var _binding: FragmentRetirementDefaultsBinding? = null
    private val binding get() = _binding!!
    private val args: RetirementDefaultsFragmentArgs by navArgs()
    var loadRetirementInfoFromWorking = false
    var currentUserID = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRetirementDefaultsBinding.inflate(inflater, container, false)
        currentUserID = args.userID

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
            Timber.tag("Alex").d("Assets expand button clicked")
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
            val cdf = RetirementAssetDialogFragment.newInstance(currentUserID, "", RetirementScenarioType.DEFAULTS)
            cdf.setRetirementAssetDialogFragmentListener(object: RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val userDefault = RetirementViewModel.getUserDefault(currentUserID)
                    if (userDefault != null) {
                        val myAdapter = AssetAdapter(requireContext(),
                            userDefault.investmentGrowthRate,
                            userDefault.propertyGrowthRate,
                            RetirementScenarioType.DEFAULTS,
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
            val cdf = RetirementPensionDialogFragment.newInstance(currentUserID, "", RetirementScenarioType.DEFAULTS)
            cdf.setRetirementPensionDialogFragmentListener(object: RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    val myAdapter = PensionAdapter(requireContext(), RetirementScenarioType.DEFAULTS)
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
            val cdf = RetirementAdditionalDialogFragment.newInstance(-1, RetirementScenarioType.DEFAULTS)
            cdf.setRetirementAdditionalDialogFragmentListener(object: RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                override fun onNewDataSaved() {
                    loadRetirementInfoFromWorking = true
                    updateAdditionalItemsAdapters()
                }
            })
            cdf.show(parentFragmentManager, getString(R.string.add_additional_deposit_or_expense))
        }

        binding.buttonCancel.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.buttonUpdateDefaults.setOnClickListener {
            hideKeyboard(requireContext(), requireView())
            collapseAll()
            onSaveButtonClicked()
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.rf.expandAllButton.tag = R.drawable.ic_baseline_keyboard_double_arrow_down_24
        binding.rf.switchUseBudget.setOnClickListener {
            Timber.tag("Alex").d("switchusebudget.onclick ${binding.rf.switchUseBudget.isChecked}")
            if (binding.rf.switchUseBudget.isChecked) {
                binding.rf.targetMonthlyIncome.visibility = View.GONE
                binding.rf.targetMonthlyIncome.setText("")
            } else {
                binding.rf.targetMonthlyIncome.visibility = View.VISIBLE
            }
        }

        inflater.inflate(R.layout.fragment_retirement, container, false)
        return binding.root
    }
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        setupDefaults()
        binding.rf.assetsExpandButton.text = String.format(getString(R.string.assets), 0)
        binding.rf.pensionsExpandButton.text = String.format(getString(R.string.pensions), 0)
        binding.rf.additionalExpandButton.text = String.format(getString(R.string.additional_deposits_or_expenditures), 0)

        binding.rf.currencySymbol2.text = String.format("${getLocalCurrencySymbol()} ")
        binding.rf.currencySymbol3.text = String.format("${getLocalCurrencySymbol()} ")
        binding.rf.currencySymbol4.text = String.format("${getLocalCurrencySymbol()} ")
        gRetirementDetailsList = arrayListOf()
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
        binding.whoRadioGroup.setOnCheckedChangeListener { _, optionId ->
            currentUserID = when (optionId) {
                R.id.name2RadioButton -> 1
                else -> 0
            }
            Timber.tag("Alex").d("in setOnChecked $currentUserID")
            loadScreen()
        }
        binding.name1RadioButton.text = SpenderViewModel.getSpenderName(0)
        if (SpenderViewModel.getNumberOfUsers() > 1) {
            binding.name2RadioButton.visibility = View.VISIBLE
            binding.name2RadioButton.text = SpenderViewModel.getSpenderName(1)
        } else {
            binding.name2RadioButton.visibility = View.GONE
        }
        if (currentUserID == 0)
            binding.name1RadioButton.isChecked = true
        else
            binding.name2RadioButton.isChecked = true
    }

    private fun updateAdditionalItemsAdapters() {
        val myExpAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.EXPENSE, RetirementScenarioType.DEFAULTS)
        binding.rf.additionalExpendituresListView.adapter = myExpAdapter
        setListViewHeightBasedOnChildren(binding.rf.additionalExpendituresListView)
        val myDepAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.DEPOSIT, RetirementScenarioType.DEFAULTS)
        binding.rf.additionalDepositsListView.adapter = myDepAdapter
        setListViewHeightBasedOnChildren(binding.rf.additionalDepositsListView)
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
        if (!checkThatAllFieldsAreOK())
            return
        val rtData = createDataFromScreenValues()

        RetirementViewModel.updateRetirementDefault(rtData, false)
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        MyApplication.displayToast(getString(R.string.default_updated))
    }

    private fun createDataFromScreenValues() : RetirementData {
        val ageRadioGroup = binding.rf.cppStartAgeRadioGroup
        val ageRadioGroupChecked = ageRadioGroup.checkedRadioButtonId
        val ageRadioButton = requireActivity().findViewById(ageRadioGroupChecked) as RadioButton

        val scenarioName = getString(R.string.defaultt)
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

        rtData.setAssetsAndPensionsAndAdditionalItemsFromWorking(gRetirementDefaults)

        gRetirementDefaults = rtData
        return rtData
    }

    private fun setupDefaults() {
        binding.buttonCancel.visibility = View.VISIBLE
        binding.buttonUpdateDefaults.visibility = View.VISIBLE
        binding.rf.birthDate.isEnabled = true
        binding.rf.cpp60Amount.isEnabled = true
        binding.rf.cpp65Amount.isEnabled = true
        binding.rf.cpp70Amount.isEnabled = true
        binding.rf.oasCurrentAnnualAmount.isEnabled = true
    }

    private fun loadScreen() {
        val userDefault = RetirementViewModel.getUserDefault(currentUserID)
        if (userDefault != null) {
            for (i in 0 until binding.rf.cppStartAgeRadioGroup.childCount) {
                val o = binding.rf.cppStartAgeRadioGroup.getChildAt(i)
                if (o is RadioButton) {
                    if (o.text == userDefault.cppAge.toString()) {
                        o.isChecked = true
                    }
                }
            }
            binding.rf.retirementDate.setText(userDefault.retirementDate)
            binding.rf.switchUseBudget.isChecked = userDefault.useBudgetAsTargetIncome
            if (binding.rf.switchUseBudget.isChecked) {
                binding.rf.targetMonthlyIncome.visibility = View.GONE
                binding.rf.targetMonthlyIncome.setText("")
            } else {
                binding.rf.targetMonthlyIncome.visibility = View.VISIBLE
                binding.rf.targetMonthlyIncome.setText(gDec(userDefault.targetMonthlyIncome))
            }
            binding.rf.planToAge.setText(userDefault.planToAge.toString())
            binding.rf.salaryAmount.setText(userDefault.salary.annualValueAfterTax.toString())
            binding.rf.salaryAnnualIncrease.setText(userDefault.salary.estimatedGrowthPct.toString())
            binding.rf.birthDate.setText(userDefault.birthDate)
            binding.rf.inflationRate.setText(userDefault.inflationRate.toString())
            binding.rf.investmentGrowthRate.setText(userDefault.investmentGrowthRate.toString())
            binding.rf.propertyGrowthRate.setText(userDefault.propertyGrowthRate.toString())
            binding.rf.cpp60Amount.setText(userDefault.cpp.annualValueAt60.toString())
            binding.rf.cpp65Amount.setText(userDefault.cpp.annualValueAt65.toString())
            binding.rf.cpp70Amount.setText(userDefault.cpp.annualValueAt70.toString())
            binding.rf.oasCurrentAnnualAmount.setText(userDefault.oas.currentAnnualValue.toString())
            gRetirementDefaults = userDefault.copy()
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
            binding.rf.assetsListView.adapter = null
            setListViewHeightBasedOnChildren(binding.rf.assetsListView)
            binding.rf.pensionsListView.adapter = null
            setListViewHeightBasedOnChildren(binding.rf.pensionsListView)
            binding.rf.additionalDepositsListView.adapter = null
            setListViewHeightBasedOnChildren(binding.rf.additionalDepositsListView)
            binding.rf.additionalExpendituresListView.adapter = null
            setListViewHeightBasedOnChildren(binding.rf.additionalExpendituresListView)
        }
    }
    private fun setupAdapters(iUserID: Int, iInvestmentGrowthRate: Double,
                              iPropertyGrowthRate: Double) {
        val adapter = AssetAdapter(requireContext(),
            iInvestmentGrowthRate,
            iPropertyGrowthRate,
            RetirementScenarioType.DEFAULTS,
            { item ->
                moveAsset(item.distributionOrder, -1)
            },
            { item ->
                moveAsset(item.distributionOrder, 1)
            }
        )
        binding.rf.assetsExpandButton.text =
            String.format(getString(R.string.assets), adapter.count)
        binding.rf.assetsListView.adapter = adapter
        Timber.tag("Alex").d("in setupadapters ${binding.rf.assetsListView.adapter.count}")
        setListViewHeightBasedOnChildren(binding.rf.assetsListView)
        binding.rf.assetsListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = binding.rf.assetsListView.getItemAtPosition(position) as Asset
                val cdf = RetirementAssetDialogFragment.newInstance(
                    iUserID,
                    itemValue.name,
                    RetirementScenarioType.DEFAULTS
                )
                cdf.setRetirementAssetDialogFragmentListener(object :
                    RetirementAssetDialogFragment.RetirementAssetDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        val myAdapter = AssetAdapter(requireContext(),
                            iInvestmentGrowthRate,
                            iPropertyGrowthRate,
                            RetirementScenarioType.DEFAULTS,
                            { item ->
                                moveAsset(item.distributionOrder, -1) },
                            { item ->
                                moveAsset(item.distributionOrder, 1) })
                        binding.rf.assetsListView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(binding.rf.assetsListView)
                        binding.rf.assetsExpandButton.text =
                            String.format(getString(R.string.assets), myAdapter.count)
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_asset))
            }

        binding.rf.pensionsExpandButton.text =
            String.format(getString(R.string.pensions), gRetirementDefaults?.getPensionListCount())
        val padapter = PensionAdapter(requireContext(), RetirementScenarioType.DEFAULTS)
        binding.rf.pensionsListView.adapter = padapter
        setListViewHeightBasedOnChildren(binding.rf.pensionsListView)
        binding.rf.pensionsListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = binding.rf.pensionsListView.getItemAtPosition(position) as Pension
                val cdf = RetirementPensionDialogFragment.newInstance(
                    iUserID,
                    itemValue.name,
                    RetirementScenarioType.DEFAULTS
                )
                cdf.setRetirementPensionDialogFragmentListener(object :
                    RetirementPensionDialogFragment.RetirementPensionDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        val myAdapter = PensionAdapter(requireContext(), RetirementScenarioType.DEFAULTS)
                        binding.rf.pensionsListView.adapter = myAdapter
                        setListViewHeightBasedOnChildren(binding.rf.pensionsListView)
                        binding.rf.pensionsExpandButton.text =
                            String.format(getString(R.string.pensions), gRetirementDefaults?.getPensionListCount())
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_pension))
            }

        binding.rf.additionalExpandButton.text =
            String.format(getString(R.string.additional_deposits_or_expenditures), gRetirementDefaults?.getAdditionalListCount())
        binding.rf.additionalDepositsLabel.text =
            String.format(getString(R.string.additional_deposits),
                gRetirementDefaults?.getAdditionalListCount(AdditionalType.DEPOSIT))
        binding.rf.additionalExpensesLabel.text =
            String.format(getString(R.string.additional_expenditures),
                gRetirementDefaults?.getAdditionalListCount(AdditionalType.EXPENSE))
        val expAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.EXPENSE, RetirementScenarioType.DEFAULTS)
        binding.rf.additionalExpendituresListView.adapter = expAdapter
        setListViewHeightBasedOnChildren(binding.rf.additionalExpendituresListView)
        val depAdapter = AdditionalItemsAdapter(requireContext(), AdditionalType.DEPOSIT, RetirementScenarioType.DEFAULTS)
        binding.rf.additionalDepositsListView.adapter = depAdapter
        setListViewHeightBasedOnChildren(binding.rf.additionalDepositsListView)
        binding.rf.additionalExpendituresListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val item: AdditionalItem = binding.rf.additionalExpendituresListView.adapter.getItem(position) as AdditionalItem
                val cdf = RetirementAdditionalDialogFragment.newInstance(
                    item.id,
                    RetirementScenarioType.DEFAULTS
                )
                cdf.setRetirementAdditionalDialogFragmentListener(object :
                    RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        updateAdditionalItemsAdapters()
                        if (gRetirementDefaults?.getAdditionalListCount() == 0) {
                            onAdditionalItemsExpandClicked("Closed")
                        }
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_expense))
            }
        binding.rf.additionalDepositsListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val item: AdditionalItem = binding.rf.additionalDepositsListView.adapter.getItem(position) as AdditionalItem
                val cdf = RetirementAdditionalDialogFragment.newInstance(
                    item.id,
                    RetirementScenarioType.DEFAULTS
                )
                cdf.setRetirementAdditionalDialogFragmentListener(object :
                    RetirementAdditionalDialogFragment.RetirementAdditionalDialogFragmentListener {
                    override fun onNewDataSaved() {
                        loadRetirementInfoFromWorking = true
                        updateAdditionalItemsAdapters()
                        if (gRetirementDefaults?.getAdditionalListCount() == 0) {
                            onAdditionalItemsExpandClicked("Closed")
                        }
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_deposit))
            }
    }
    private fun moveAsset(iDistributionOrder: Int, iDirection: Int) {
        val adapter = binding.rf.assetsListView.adapter as AssetAdapter

        gRetirementDefaults?.changeDefaultDistributionOrder(
            iDistributionOrder,
            iDirection
        )

        adapter.refreshData()
        setListViewHeightBasedOnChildren(binding.rf.assetsListView)
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
        val assetCount = binding.rf.assetsListView.adapter?.count ?: 0
        Timber.tag("Alex").d("count is $assetCount")
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
        val pensionCount = binding.rf.pensionsListView.adapter?.count ?: 0
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
        val totCount = (binding.rf.additionalDepositsListView.adapter?.count ?: 0) +
                (binding.rf.additionalExpendituresListView.adapter?.count ?: 0)
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
}