@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

const val gMinimizeTaxAmount = 46226

enum class AssetType(val code: Int) {
    RRSP(1),
    LIRA_LIF(2),
    LIRA_Annuity(3),
    TFSA(4),
    SAVINGS(5),
    PROPERTY(6),
    ALL(7);
    companion object {
        fun getText(iValue: AssetType): String {
            return when (iValue) {
                RRSP -> "RRSP"
                LIRA_LIF -> "LIRA (LIF)"
                LIRA_Annuity -> "LIRA (Annuity)"
                TFSA -> "TFSA"
                SAVINGS -> "Savings"
                PROPERTY -> "Property"
                else -> "Unknown"
            }
        }
    }
}
enum class PensionType(val code: Int) {
    BASIC(1),
    PSPP(2),
    OTPP(3);
    companion object {
        fun getText(iValue: PensionType): String {
            return when (iValue) {
                BASIC -> "Basic"
                PSPP -> "PSPP"
                OTPP -> "OTPP"
            }
        }
    }
}

enum class MinimizeTaxEnum(val code: Int) {
    DO_NOT_MINIMIZE(1),
    ALWAYS_MINIMIZE(2),
    MINIMIZE_WHEN_POSSIBLE(3);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
}

data class RetirementData(
    var name: String,
    var userID: Int,
    var targetMonthlyIncome: Int,
    var retirementDate: String,
    var planToAge: Int,
    var cppAge: Int,
    var inflationRate: Double,
    var investmentGrowthRate: Double,
    var propertyGrowthRate: Double,
    var birthDate: String) {
        var assets: MutableList<Asset> = ArrayList()
        var pensions: MutableList<Pension> = ArrayList()
        var additionalItems: MutableList<AdditionalItem> = ArrayList()
        var salary: Salary = Salary(0, "", 0, 0.0)
        var cpp: CPP = CPP()
        var oas: OAS = OAS()

    companion object {
        fun create(iData: MutableList<DataSnapshot>) : RetirementData {
            val retData = RetirementData(
                "", 0, 0,
                "", 0, 0, 0.0,
                0.0, 0.0, ""
            )

            for (data in iData) {
                when (data.key.toString()) {
                    "userID" -> retData.userID = data.value.toString().toInt()
                    "birthDate" -> {
                        retData.birthDate = data.value.toString()
                    }
                    "cppAge" -> retData.cppAge = data.value.toString().toInt()
                    "inflationRate" -> retData.inflationRate =
                        data.value.toString().toDouble()
                    "investmentGrowthRate" -> retData.investmentGrowthRate =
                        data.value.toString().toDouble()
                    "propertyGrowthRate" -> retData.propertyGrowthRate =
                        data.value.toString().toDouble()
                    "name" -> retData.name = data.value.toString()
                    "planToAge" -> retData.planToAge = data.value.toString().toInt()
                    "retirementDate" -> retData.retirementDate =
                        data.value.toString()
                    "targetMonthlyIncome" -> retData.targetMonthlyIncome =
                        data.value.toString().toInt()
                    "cpp" -> {
                        var v60 = 0
                        var v65 = 0
                        var v70 = 0
                        for (cppRow in data.children.toMutableList()) {
                            when (cppRow.key.toString()) {
                                "annualValueAt60" -> v60 =
                                    cppRow.value.toString().toInt()
                                "annualValueAt65" -> v65 =
                                    cppRow.value.toString().toInt()
                                "annualValueAt70" -> v70 =
                                    cppRow.value.toString().toInt()
                            }
                            retData.cpp = CPP(v60, v65, v70)
                        }
                    }
                    "oas" -> {
                        var currentAnnualValue = 0
                        for (oasRow in data.children.toMutableList()) {
                            when (oasRow.key.toString()) {
                                "currentAnnualValue" -> currentAnnualValue =
                                    oasRow.value.toString().toInt()
                            }
                            retData.oas = OAS(currentAnnualValue)
                        }
                    }
                    "salary" -> {
                        var annualValueAfterTax = 0
                        var name = ""
                        var estimatedGrowthPct = 0.0
                        for (salRow in data.children.toMutableList()) {
                            when (salRow.key.toString()) {
                                "annualValueAfterTax" -> annualValueAfterTax =
                                    salRow.value.toString().toInt()
                                "name" -> name = salRow.value.toString()
                                "estimatedGrowthPct" -> estimatedGrowthPct =
                                    salRow.value.toString().toDouble()
                            }
                            retData.salary = Salary(
                                0,
                                name,
                                annualValueAfterTax,
                                estimatedGrowthPct
                            )
                        }
                    }
                    "additionalItems" -> {
                        for (addItem in data.children.toMutableList()) {
                            val addItemID = addItem.key.toString().toInt()
                            var name = ""
                            var amount = 0
                            var assetName = ""
                            var type = AdditionalType.DEPOSIT
                            var year = 0
                            for (det in addItem.children.toMutableList()) {
                                when (det.key.toString()) {
                                    "name" -> name = det.value.toString()
                                    "amount" -> amount = det.value.toString().toInt()
                                    "assetName" -> assetName = det.value.toString()
                                    "assetType", "type" -> {
                                        type =
                                            try {
                                                AdditionalType.valueOf(det.value.toString())
                                            } catch (e: IllegalArgumentException) {
                                                AdditionalType.DEPOSIT
                                            }
                                    }
                                    "year" -> year = det.value.toString().toInt()
                                }
                            }
                            val newItem = AdditionalItem(
                                addItemID,
                                type,
                                name,
                                year,
                                amount,
                                assetName
                            )
                            retData.additionalItems.add(
                                min(addItemID, retData.additionalItems.size),
                                newItem
                            )
                        }
                    }
                    "assets" -> {
                        for (asset in data.children.toMutableList()) {
                            val assetID = asset.key.toString().toInt()
                            var annualContribution = 0
                            var distributionOrder = 0
                            var useDefaultGrowthPct = true
                            var estimatedGrowthPct = 0.0
                            var useDefaultGrowthPctAsSavings = true
                            var estimatedGrowthPctAfterSale = 0.0
                            var willSellToFinanceRetirement = false
                            var taxSheltered = false
                            var minimizeTax = MinimizeTaxEnum.ALWAYS_MINIMIZE
                            var mortgageDetailsText = ""
                            var ownershipPercentage = 0.0
                            var name = ""
                            var type = AssetType.RRSP
                            var value = 0
                            var primaryResidence = true
                            var pensionStartDate = ""
                            var annualAmount = 0
                            for (det in asset.children.toMutableList()) {
                                when (det.key.toString()) {
                                    "annualContribution" -> annualContribution =
                                        det.value.toString().toInt()
                                    "distributionOrder" -> distributionOrder =
                                        det.value.toString().toInt()
                                    "useDefaultGrowthPct" -> useDefaultGrowthPct =
                                        det.value.toString().toBoolean()
                                    "estimatedGrowthPct" -> estimatedGrowthPct =
                                        det.value.toString().toDouble()
                                    "name" -> name = det.value.toString()
                                    "pensionStartDate" -> pensionStartDate = det.value.toString()
                                    "annualAmount" -> annualAmount = det.value.toString().toInt()
                                    "assetType", "type" -> {
                                        type =
                                            try {
                                                AssetType.valueOf(det.value.toString())
                                            } catch (e: IllegalArgumentException) {
                                                AssetType.RRSP
                                            }
                                    }
                                    "value" -> {
                                        value = det.value.toString().toInt()
                                    }
                                    "useDefaultGrowthPctAsSavings" -> useDefaultGrowthPctAsSavings =
                                        det.value.toString().toBoolean()
                                    "estimatedGrowthPctAsSavings" -> estimatedGrowthPctAfterSale =
                                        det.value.toString().toDouble()
                                    "willSellToFinanceRetirement" -> willSellToFinanceRetirement =
                                        (det.value.toString() == "true")
                                    "taxSheltered" -> taxSheltered =
                                        (det.value.toString() == "true")
                                    "minimizeTax" ->
                                        minimizeTax = if (det.value.toString().toIntOrNull() != null)
                                            MinimizeTaxEnum.getByValue(value)!!
                                        else {
                                            try {
                                                MinimizeTaxEnum.valueOf(det.value.toString())
                                            } catch (e: IllegalArgumentException) {
                                                MinimizeTaxEnum.ALWAYS_MINIMIZE
                                            }
                                        }
                                    "scheduledPaymentName" -> mortgageDetailsText =
                                        det.value.toString()
                                    "ownershipPct" -> ownershipPercentage =
                                        det.value.toString().toDouble()
                                    "primaryResidence" -> primaryResidence =
                                        (det.value.toString() == "true")
                                }
                            }
                            var newAsset: Asset? = null
                            when (type) {
                                AssetType.RRSP -> {
                                    newAsset = RRSP(
                                        assetID,
                                        name,
                                        value,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        annualContribution,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder,
                                        0,
                                        minimizeTax
                                    )
                                }
                                AssetType.TFSA -> {
                                    newAsset = TFSA(
                                        assetID,
                                        name,
                                        value,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        annualContribution,
                                        willSellToFinanceRetirement,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder
                                    )
                                }
                                AssetType.LIRA_LIF -> {
                                    newAsset = LIRALIF(
                                        assetID,
                                        name,
                                        value,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        annualContribution,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder,
                                        0,
                                        minimizeTax
                                    )
                                }
                                AssetType.LIRA_Annuity -> {
                                    newAsset = LIRAANNUITY(
                                        assetID,
                                        name,
                                        value,
                                        pensionStartDate,
                                        gCurrentDate.getYear(),
                                        annualAmount,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder
                                    )
                                }
                                AssetType.SAVINGS -> {
                                    newAsset = Savings(
                                        assetID,
                                        name,
                                        value,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        annualContribution,
                                        willSellToFinanceRetirement,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder,
                                        taxSheltered
                                    )
                                }
                                AssetType.PROPERTY -> {
                                    newAsset = Property(
                                        assetID,
                                        name,
                                        value,
                                        useDefaultGrowthPct,
                                        estimatedGrowthPct,
                                        willSellToFinanceRetirement,
                                        12 - gCurrentDate.getMonth(),
                                        distributionOrder,
                                        useDefaultGrowthPctAsSavings,
                                        estimatedGrowthPctAfterSale,
                                        mortgageDetailsText,
                                        ownershipPercentage,
                                        0,
                                        primaryResidence
                                    )
                                }
                                else -> {}
                            }
                            if (newAsset != null)
                                retData.assets.add(min(assetID, retData.assets.size), newAsset)
                        }
                        retData.assets.sortBy { it.distributionOrder }
                        retData.updateDistributionOrderAsRequired()
                    }
                    "pensions" -> {
                        for (pension in data.children.toMutableList()) {
                            val pensionID = pension.key.toString().toInt()
                            var name = ""
                            var pensionType = PensionType.BASIC
                            var value = 0
                            var pensionStartDate = ""
                            var workStartDate = ""
                            var best5 = 0
                            var pensionStartDelay = 0
                            for (det in pension.children.toMutableList()) {
                                when (det.key.toString()) {
                                    "name" -> name = det.value.toString()
                                    "pensionType" -> {
                                        pensionType =
                                            try {
                                                PensionType.valueOf(det.value.toString())
                                            } catch (e: IllegalArgumentException) {
                                                PensionType.BASIC
                                            }
                                    }
                                    "value" -> value = det.value.toString().toInt()
                                    "pensionStartDate" -> pensionStartDate =
                                        det.value.toString()
                                    "workStartDate" -> workStartDate = det.value.toString()
                                    "best5YearsSalary" -> best5 = det.value.toString().toInt()
                                    "pensionStartDelay" -> pensionStartDelay =
                                        det.value.toString().toInt()
                                }
                            }
                            val newPension = Pension(
                                pensionID,
                                name,
                                value,
                                pensionType,
                                workStartDate,
                                best5,
                                pensionStartDate,
                                pensionStartDelay
                            )
                            retData.pensions.add(
                                min(pensionID, retData.pensions.size),
                                newPension
                            )
                        }
                    }
                }
            }
            return retData
        }
    }

    fun updateDistributionOrderAsRequired() {
        for (i in 0 until assets.size) {
            if (assets[i].distributionOrder != i) {
                assets[i].distributionOrder = i
            }
        }
        assets.sortBy { it.distributionOrder }
    }
    fun setAssetsAndPensionsAndAdditionalItemsFromWorking() {
        if (gRetirementScenario != null) {
            for (i in 0 until gRetirementScenario!!.assets.count()) {
                val copiedAsset = gRetirementScenario!!.assets[i].copy()
                if (copiedAsset.assetType == AssetType.PROPERTY) {
                    if (copiedAsset.useDefaultGrowthPct)
                        copiedAsset.estimatedGrowthPct = propertyGrowthRate
                    if ((copiedAsset as Property).useDefaultGrowthPctAsSavings)
                        copiedAsset.estimatedGrowthPctAsSavings = investmentGrowthRate
                } else {
                    if (copiedAsset.useDefaultGrowthPct)
                        copiedAsset.estimatedGrowthPct = investmentGrowthRate
                }
                assets.add(copiedAsset)
            }
            updateDistributionOrderAsRequired()
            for (i in 0 until gRetirementScenario!!.pensions.count()) {
                gRetirementScenario!!.pensions[i].copy().let { pensions.add(it) }
            }
            for (i in 0 until gRetirementScenario!!.additionalItems.count()) {
                val temp = gRetirementScenario!!.additionalItems[i]
                additionalItems.add(temp.copy())
            }
        }
    }
    fun getAgeAtStartOfYear(iYear: Int) : Int {
        val birthYear = if (birthDate.substring(0,4).toIntOrNull() != null)
            birthDate.substring(0,4).toInt() else 0
        return if (birthYear != 0)
            iYear - birthYear - 1
        else
            99
    }
    fun getAdditionalGrowth(iAssetName: String, iYear: Int) : Int {
        var tot = 0
        additionalItems.forEach {
            if (it.year == iYear &&
                    it.assetName == iAssetName &&
                    it.type == AdditionalType.DEPOSIT) {
                tot += it.amount
            }
        }
        return tot
    }
    fun copy() : RetirementData {
        val rd = RetirementData(
            name, userID, targetMonthlyIncome, retirementDate, planToAge,
            cppAge, inflationRate, investmentGrowthRate, propertyGrowthRate, birthDate
        )
        rd.assets = assets.map {it.copy()}.toMutableList()
        rd.pensions = pensions.map {it.copy()}.toMutableList()
        rd.additionalItems = additionalItems.map {it.copy()}.toMutableList()
        rd.salary = Salary(salary.id, salary.name, salary.annualValueAfterTax, salary.estimatedGrowthPct)
        rd.cpp = CPP(cpp.annualValueAt60, cpp.annualValueAt65, cpp.annualValueAt70)
        rd.oas = OAS(oas.currentAnnualValue)
        return rd
    }
}

class AdditionalItem(var id: Int, var type: AdditionalType, var name: String, var year: Int,
                     var amount: Int, var assetName: String = "") {
    fun copy() : AdditionalItem {
        return AdditionalItem(id, type, name, year, amount, assetName)
    }
}

abstract class Asset(val id: Int, var assetType: AssetType, val name: String, private var value: Int,
                     var useDefaultGrowthPct: Boolean,
                     var estimatedGrowthPct: Double, var annualContribution: Int,
                     var willSellToFinanceRetirement: Boolean,
                     val monthsOfGrowthThisYear: Int = 12, var distributionOrder: Int) {
    var growthThisYear = 0
    var additionalGrowthThisYear = 0
    var withdrawalAmount = 0
    init {
        this.computeGrowth()
    }
    fun getValue() : Int {
        return value
    }
    fun setValue(iValue: Int) {
        value = iValue
    }
    fun getAvailableValue() : Int {
        return value + additionalGrowthThisYear
    }
    open fun getEndingBalance() : Int {
        return  value - withdrawalAmount + growthThisYear + additionalGrowthThisYear
    }
    open fun computeGrowth() {
        growthThisYear = if (value != 0 && withdrawalAmount < value)
            round((value  - withdrawalAmount) *
                (getGrowthPct() / 100 * monthsOfGrowthThisYear / 12) + annualContribution ).toInt()
        else
            0
    }
    open fun getTaxableIncome(iYear: Int): Int {
        return 0
    }
    open fun withdrawalIsTaxable(): Boolean {
        return false
    }
    open fun growthIsTaxable(): Boolean {
        return false
    }
    open fun getGrowthPct() : Double {
        return estimatedGrowthPct
    }
    abstract fun copy() : Asset
    abstract fun getNextYear(iAmIRetired: Boolean) : Asset
    abstract fun withdraw(iAmount: Int, iYear: Int)
    abstract fun getGrossIncome(): Int
    open fun withdraw(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int, iRetirementData: RetirementData?) {
        if (willSellToFinanceRetirement)
            withdraw(iAmount, iYear)
    }
    open fun withdrawExtra(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int, iRetirementData: RetirementData?) {
        // do nothing; this is when an asset is minimizing tax and is being asked a second time for contributions...
    }
}

open class RRSP (
    id: Int,
    name: String,
    value: Int,
    useDefaultGrowthPct: Boolean,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int,
    private var ageAtStartOfYear: Int,
    var minimizeTax: MinimizeTaxEnum) :
        Asset(id, AssetType.RRSP, name, value, useDefaultGrowthPct, estimatedGrowthPct,
            annualContribution, true, monthsOfGrowthThisYear,
            distributionOrder) {
    init {
        withdrawalAmount = round(getMinimumRRIFWithdrawalPercentage(ageAtStartOfYear) * value).toInt()
        computeGrowth()
    }

    override fun copy(): Asset {
        val rrsp = RRSP(id, name, getValue(), useDefaultGrowthPct, estimatedGrowthPct, annualContribution,
            monthsOfGrowthThisYear, distributionOrder, ageAtStartOfYear, minimizeTax)
        rrsp.additionalGrowthThisYear = additionalGrowthThisYear
        return rrsp
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return RRSP(
            id,
            name,
            getEndingBalance(),
            useDefaultGrowthPct,
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            12,
            distributionOrder,
            ageAtStartOfYear+1,
            minimizeTax
        )
    }

    override fun withdraw(iAmount: Int, iYear: Int) {
        return withdraw(iAmount, 0, iYear, null)
    }
    override fun withdraw(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int,
        iRetirementData: RetirementData?) {
        val minimumWithdrawalAmount = withdrawalAmount  // it was set in the constructor
        val currentMinimalTaxAmount = if (minimizeTax == MinimizeTaxEnum.DO_NOT_MINIMIZE) {
            999999
        } else {
            val inflationRate = iRetirementData?.inflationRate ?: 0.0
            round(gMinimizeTaxAmount *
                    (1 + inflationRate/100.0).pow(iYear - gCurrentDate.getYear())).toInt()
        }
        var minWithdrawal1 = min(getAvailableValue(), iAmount)
        minWithdrawal1 = min(minWithdrawal1, currentMinimalTaxAmount - iTaxableIncomeAlreadyMade) // at this point minWithdrawal1 is the minimum of...
        // the current balance of the account, the amount requested, and the minimum tax bracket

        withdrawalAmount = max(minWithdrawal1, minimumWithdrawalAmount)
        computeGrowth()
    }

    override fun getGrossIncome(): Int {
        return withdrawalAmount
    }

    override fun withdrawExtra(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int, iRetirementData: RetirementData?) {
        if (minimizeTax != MinimizeTaxEnum.MINIMIZE_WHEN_POSSIBLE)
            return

        withdrawalAmount += min(getAvailableValue()-withdrawalAmount, iAmount)
        computeGrowth()
    }

    override fun withdrawalIsTaxable(): Boolean {
        return true
    }

    override fun getTaxableIncome(iYear: Int): Int {
        return withdrawalAmount
    }
}

class LIRALIF (
    id: Int,
    name: String,
    value: Int,
    useDefaultGrowthPct: Boolean,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int,
    private var ageAtStartOfYear: Int,
    minimizeTax: MinimizeTaxEnum) :
    RRSP(id, name, value, useDefaultGrowthPct, estimatedGrowthPct,
        annualContribution, monthsOfGrowthThisYear, distributionOrder,
        ageAtStartOfYear, minimizeTax) {
    init {
        withdrawalAmount = round(getMinimumRRIFWithdrawalPercentage(ageAtStartOfYear) * value).toInt()
        assetType = AssetType.LIRA_LIF
        computeGrowth()
    }

    override fun copy(): Asset {
        val liraLif = LIRALIF(id, name, getValue(), useDefaultGrowthPct, estimatedGrowthPct, annualContribution,
            monthsOfGrowthThisYear, distributionOrder, ageAtStartOfYear, minimizeTax)
        liraLif.additionalGrowthThisYear = additionalGrowthThisYear
        return liraLif
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return LIRALIF(
            id,
            name,
            getEndingBalance(),
            useDefaultGrowthPct,
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            12,
            distributionOrder,
            ageAtStartOfYear+1,
            minimizeTax
        )
    }
    override fun withdrawalIsTaxable(): Boolean {
        return true
    }

    override fun getTaxableIncome(iYear: Int): Int {
        return withdrawalAmount
    }
}

class LIRAANNUITY (
    id: Int,
    name: String,
    value: Int,
    var pensionStartDate: String,
    var currentYear: Int,
    var annualAmount: Int,
    useDefaultGrowthPct: Boolean,
    estimatedGrowthPct: Double,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int) :
    Asset(id, AssetType.LIRA_Annuity, name, value, useDefaultGrowthPct, estimatedGrowthPct,
        0, true, monthsOfGrowthThisYear, distributionOrder) {
    init {
        withdrawalAmount = if (pensionStartDate.substring(0,4).toInt() == 0)
            0
        else if (currentYear < pensionStartDate.substring(0,4).toInt())
            0
        else if (currentYear == pensionStartDate.substring(0,4).toInt())
            annualAmount * round((12 - pensionStartDate.substring(5,7).toInt() + 1)/12.0).toInt()
        else
            annualAmount
        assetType = AssetType.LIRA_Annuity
        computeGrowth()
    }

    override fun copy(): Asset {
        return LIRAANNUITY(
            id, name, getValue(), pensionStartDate, currentYear,
            annualAmount, useDefaultGrowthPct, estimatedGrowthPct,
            monthsOfGrowthThisYear, distributionOrder
        )
    }
    override fun computeGrowth() {
        growthThisYear = round(getValue() *
                    (getGrowthPct() / 100 * monthsOfGrowthThisYear / 12) ).toInt()
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return LIRAANNUITY(
            id,
            name,
            getEndingBalance(),
            pensionStartDate,
            currentYear+1,
            annualAmount,
            useDefaultGrowthPct,
            estimatedGrowthPct,
            12,
            distributionOrder
        )
    }

    override fun getEndingBalance() : Int {
        return  getValue() + growthThisYear
    }
    override fun withdraw(iAmount: Int, iYear: Int) {
        return // can't withdraw more from an annuity
    }
    override fun getGrossIncome(): Int {
        return withdrawalAmount
    }
    override fun withdrawalIsTaxable(): Boolean {
        return true
    }

    override fun getTaxableIncome(iYear: Int): Int {
        return withdrawalAmount
    }
}

class TFSA(
    id: Int,
    name: String,
    value: Int,
    useDefaultGrowthPct: Boolean,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    willSellToFinanceRetirement: Boolean,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int) :
        Asset(id, AssetType.TFSA, name, value, useDefaultGrowthPct, estimatedGrowthPct,
            annualContribution, willSellToFinanceRetirement, monthsOfGrowthThisYear,
            distributionOrder) {

    override fun withdraw(iAmount: Int, iYear: Int) {
        if (willSellToFinanceRetirement) {
            withdrawalAmount = min(iAmount, getAvailableValue())
            computeGrowth()
        }
    }

    override fun getGrossIncome(): Int {
        return withdrawalAmount
    }

    override fun copy(): Asset {
        val tfsa = TFSA(id, name, getValue(), useDefaultGrowthPct, estimatedGrowthPct,
            annualContribution, willSellToFinanceRetirement, monthsOfGrowthThisYear,
            distributionOrder)
        tfsa.additionalGrowthThisYear = additionalGrowthThisYear
        return tfsa
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return TFSA(
            id,
            name,
            getEndingBalance(),
            useDefaultGrowthPct,
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            willSellToFinanceRetirement,
            12,
            distributionOrder
        )
    }
}

class Savings(
    id: Int,
    name: String,
    value: Int,
    useDefaultGrowthPct: Boolean,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    willSellToFinanceRetirement: Boolean,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int,
    var taxSheltered: Boolean) :
        Asset(id, AssetType.SAVINGS, name, value, useDefaultGrowthPct, estimatedGrowthPct,
            annualContribution, willSellToFinanceRetirement, monthsOfGrowthThisYear,
            distributionOrder) {

    override fun withdraw(iAmount: Int, iYear: Int) {
        if (willSellToFinanceRetirement) {
            withdrawalAmount = min(iAmount, getAvailableValue())
            computeGrowth()
        }
    }

    override fun getGrossIncome(): Int {
        return withdrawalAmount //+
//            if (taxSheltered) 0 else growthThisYear
    }

    override fun copy(): Asset {
        val savings = Savings(id, name, getValue(), useDefaultGrowthPct, estimatedGrowthPct,
            annualContribution, willSellToFinanceRetirement, monthsOfGrowthThisYear,
            distributionOrder, taxSheltered)
        savings.additionalGrowthThisYear = additionalGrowthThisYear
        return savings
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return Savings(
            id,
            name,
            getEndingBalance(),
            useDefaultGrowthPct,
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            willSellToFinanceRetirement,
            12,
            distributionOrder,
            taxSheltered
        )
    }
    override fun growthIsTaxable(): Boolean {
        return !taxSheltered
    }

    override fun getTaxableIncome(iYear: Int): Int {
        return if (taxSheltered)
            0
        else {
            growthThisYear
        }
    }
}

class Property(
    id: Int,
    name: String,
    value: Int,
    useDefaultGrowthPct: Boolean,
    private var estimatedGrowthPctAsProperty: Double,
    willSellToFinanceRetirement: Boolean,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int,
    var useDefaultGrowthPctAsSavings: Boolean,
    var estimatedGrowthPctAsSavings: Double,
    var scheduledPaymentName: String,
    var ownershipPct: Double,
    var soldInYear: Int,
    var primaryResidence: Boolean) :
        Asset(id, AssetType.PROPERTY, name, value, useDefaultGrowthPct,
            estimatedGrowthPctAsProperty, 0, willSellToFinanceRetirement,
            monthsOfGrowthThisYear, distributionOrder) {
    init {
        computeGrowth() // needs to be done here, because the base asset call is too soon
    }

    override fun computeGrowth() { // needs to be overridden to get the correct growth pct
        growthThisYear = if (getValue() != 0 && withdrawalAmount < getValue())
            round((getValue()  - withdrawalAmount) *
                    (getGrowthPct() / 100 * monthsOfGrowthThisYear / 12) + annualContribution + additionalGrowthThisYear).toInt()
        else
            0
    }
    override fun copy(): Asset {
        val prop = Property(id, name, getValue(), useDefaultGrowthPct,
            estimatedGrowthPctAsProperty, willSellToFinanceRetirement,
            monthsOfGrowthThisYear, distributionOrder,
            useDefaultGrowthPctAsSavings, estimatedGrowthPctAsSavings,
            scheduledPaymentName, ownershipPct, soldInYear, primaryResidence)
        prop.additionalGrowthThisYear = additionalGrowthThisYear
        return prop
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return Property(
            id,
            name,
            getEndingBalance(),
            useDefaultGrowthPct,
            estimatedGrowthPctAsProperty,
            willSellToFinanceRetirement,
            12,
            distributionOrder,
            useDefaultGrowthPctAsSavings,
            estimatedGrowthPctAsSavings,
            scheduledPaymentName,
            ownershipPct,
            soldInYear,
            primaryResidence
        )
    }

    override fun getGrowthPct(): Double {
        return if (soldInYear == 0) {
            estimatedGrowthPctAsProperty
        } else
            estimatedGrowthPctAsSavings
    }

    override fun withdraw(iAmount: Int, iYear: Int) {
        if (willSellToFinanceRetirement) {
            if (soldInYear == 0) {
                soldInYear = iYear
                val outstandingLoanAmount = getOutstandingLoanAmount(MyDate(iYear, 1, 1))
                if (outstandingLoanAmount > 0) {
                    setValue(getValue() - outstandingLoanAmount)
                }
                withdrawalAmount = if (primaryResidence) {
                    min(iAmount, getAvailableValue())
                } else {
                    min(iAmount, getAvailableValue()) + (getValue() * .25).toInt()
                }
            } else
                withdrawalAmount = min(iAmount, getAvailableValue())
            estimatedGrowthPct = getGrowthPct()
            computeGrowth()
        }
    }

    override fun getGrossIncome(): Int {
        return withdrawalAmount
    }
    override fun getTaxableIncome(iYear: Int): Int {
        return if (soldInYear == iYear &&
            !primaryResidence)
            (getValue() * .25).toInt()  // assumes 25% tax on capital gains
        else
            0
    }
    private fun getOutstandingLoanAmount(iDate: MyDate): Int {
        val sp = ScheduledPaymentViewModel.getScheduledPayment(scheduledPaymentName)
        return sp?.getOutstandingLoanAmount(iDate, ownershipPct) ?: 0
    }
}

class Pension(
    val id: Int,
    val name: String,
    var value: Int,
    val pensionType: PensionType,
    var workStartDate: String,
    val best5YearsSalary: Int,
    val pensionStartDate: String,
    val pensionStartDelay: Int) {

    fun copy(): Pension {
        return Pension(id, name, value, pensionType, workStartDate, best5YearsSalary, pensionStartDate, pensionStartDelay)
    }
}

@Suppress("UNUSED_PARAMETER")
class PensionIncome(iPension: Pension, private var retirementDate: String,
                    private var inflationRate: Double, private var cppAge: Int,
                    private var birthDate: String) {
    var id: Int = 0
    var name: String = ""
    var value: Int = 0
    private var pensionType: PensionType = PensionType.BASIC
    private var workStartDate: String = ""
    private var best5YearsSalary: Int = 0
    private var pensionStartDate: String = ""
    private var pensionStartDelay: Int = 0
    private val cppMaxInsurableEarnings: MutableList<Int> = ArrayList()

    init {
        id = iPension.id
        name = iPension.name
        value = iPension.value
        pensionType = iPension.pensionType
        workStartDate = iPension.workStartDate
        best5YearsSalary = iPension.best5YearsSalary
        pensionStartDelay = iPension.pensionStartDelay
        if (pensionType == PensionType.OTPP) {
            var pensionStartLD = LocalDate.parse(retirementDate)
            pensionStartLD = pensionStartLD.plusYears(iPension.pensionStartDelay.toLong())
            pensionStartDate = pensionStartLD.toString()
        } else
            pensionStartDate = iPension.pensionStartDate
        if (pensionType == PensionType.PSPP) {
            cppMaxInsurableEarnings.add(28900) // 1990
            cppMaxInsurableEarnings.add(30500) // 1991
            cppMaxInsurableEarnings.add(32200) // 1992
            cppMaxInsurableEarnings.add(33400) // 1993
            cppMaxInsurableEarnings.add(34400) // 1994
            cppMaxInsurableEarnings.add(34900) // 1995
            cppMaxInsurableEarnings.add(35400) // 1996
            cppMaxInsurableEarnings.add(35800) // 1997
            cppMaxInsurableEarnings.add(36900) // 1998
            cppMaxInsurableEarnings.add(37400) // 1999
            cppMaxInsurableEarnings.add(37600) // 2000
            cppMaxInsurableEarnings.add(38300) // 2001
            cppMaxInsurableEarnings.add(39100) // 2002
            cppMaxInsurableEarnings.add(39900) // 2003
            cppMaxInsurableEarnings.add(40500) // 2004
            cppMaxInsurableEarnings.add(41100) // 2005
            cppMaxInsurableEarnings.add(42100) // 2006
            cppMaxInsurableEarnings.add(43700) // 2007
            cppMaxInsurableEarnings.add(44900) // 2008
            cppMaxInsurableEarnings.add(46300) // 2009
            cppMaxInsurableEarnings.add(47200) // 2010
            cppMaxInsurableEarnings.add(48300) // 2011
            cppMaxInsurableEarnings.add(50100) // 2012
            cppMaxInsurableEarnings.add(51100) // 2013
            cppMaxInsurableEarnings.add(52500) // 2014
            cppMaxInsurableEarnings.add(53600) // 2015
            cppMaxInsurableEarnings.add(54900) // 2016
            cppMaxInsurableEarnings.add(55300) // 2017
            cppMaxInsurableEarnings.add(55900) // 2018
            cppMaxInsurableEarnings.add(57400) // 2019
            cppMaxInsurableEarnings.add(58700) // 2020
            cppMaxInsurableEarnings.add(61600) // 2021
            cppMaxInsurableEarnings.add(64900) // 2022
            var prevAmount = 64900
            for (i in 0 until 50) {
                prevAmount = (prevAmount * (1 + inflationRate/100.0)).toInt()
                cppMaxInsurableEarnings.add(prevAmount) // 2023 + i
            }
        }
    }

    fun getPensionIncome(iYear: Int) : Int {
        when (pensionType) {
            PensionType.BASIC -> {
                return if (pensionStartDate.length < 10)
                    0
                else if (iYear < pensionStartDate.substring(0,4).toInt())
                    0
                else if (iYear == pensionStartDate.substring(0,4).toInt())
                    value * (12 - pensionStartDate.substring(5,7).toInt() + 1)/12
                else
                    round(value  * (1 + inflationRate/100).pow(iYear - pensionStartDate.substring(0,4).toInt())).toInt()
            }
            PensionType.OTPP -> {
//     startingPension = 0.02 * DateDiffInDays(brentStartTeachingDate, brentRetirementDate) / 365 * best5YearsSalaryAverage
//     pension then goes up 2% (inflation) per year
                val workStartLD = LocalDate.parse(workStartDate)
                val retirementLD = LocalDate.parse(retirementDate)
                val pensionStartLD = LocalDate.parse(pensionStartDate)
                if (iYear >= pensionStartLD.year) {
                    val yearsWorked = ChronoUnit.DAYS.between(workStartLD, retirementLD) / 365.0
                    val startingPension = .02 * yearsWorked * best5YearsSalary
                    val multiplier = (1 + inflationRate / 100.0).pow(iYear - retirementLD.year)
                    var computedPension = startingPension * multiplier
                    val birthLD = LocalDate.parse(birthDate)

//     Once Brent starts his CPP, CPP clawback reduces the pension by:
//          0.0045 * numberOfYearsWorked * best5YearsSalary
                    if (iYear > birthLD.year + cppAge) {
                        computedPension -= 0.0045 * yearsWorked * best5YearsSalary
                    }
//      if Brent retires before his 85 factor Nov 30 2028, OTPP is reduced by multiplying his computed pension by -pensionReduction
//      pensionReduction = min of A or B.
//      A = Math.min((0.025 * (85 - brent85FactorAtRetirement)), 0.05 * (65 - DateDiffInDays(brentBirthDate, brentRetirementDate) / 365));
//      B = Math.min(1 - (0.95 ^ (85 - brent85AtPensionStart)), 1 - (0.95 ^ (65 - DateDiffInDays(brentBirthDate, brentStartOTPPDate) / 365)))
//     brent85FactorAtRetirement = DateDiffInDays(brentBirthDate, brentRetirementDate) / 365 + DateDiffInDays(brentStartTeachingDate, brentRetirementDate) / 365,
//          ie age at retirement + number of years taught at retirement
                    val workStartDouble:Double = workStartLD.year + (workStartLD.dayOfYear/365.0)
                    val birthDouble:Double = birthLD.year + (birthLD.dayOfYear/365.0)
                    val factor85Double = workStartDouble + ((85 - (workStartDouble - birthDouble)) / 2)
                    val factor85Year = truncate(factor85Double).toInt()
                    val factor85DayOfYear = round((factor85Double - factor85Year) * 365).toInt()
                    val factor85LD = LocalDate.ofYearDay(factor85Year, factor85DayOfYear)
                    if (retirementLD < factor85LD) {
                        val retirementDouble:Double = retirementLD.year + (retirementLD.dayOfYear/365.0)
//                        val factor85AtRetirement = (retirementDouble - birthDouble) + (retirementDouble - workStartDouble)
                        val factor85AtRetirement = ChronoUnit.DAYS.between(birthLD,retirementLD)/365.0 +
                                ChronoUnit.DAYS.between(workStartLD, retirementLD)/365.0
                        val pensionReduction1 = min((0.025 * (85.0 - factor85AtRetirement)),
                        0.05 * (65.0 - (retirementDouble - birthDouble)))
                        val factor85AtPensionStart = ChronoUnit.DAYS.between(birthLD,pensionStartLD)/365.0 +
                                ChronoUnit.DAYS.between(workStartLD, retirementLD)/365.0
                        val ageAtPensionStart = ChronoUnit.DAYS.between(birthLD,pensionStartLD)/365.0
                        val pensionReduction2 = min(1 - (0.95.pow(85 - factor85AtPensionStart)),
                            1 - (0.95.pow(65 - ageAtPensionStart)))

                        computedPension -= (computedPension * min(pensionReduction1, pensionReduction2))
                    }
                    if (iYear == pensionStartLD.year) {
                        computedPension *= ((12 - pensionStartLD.monthValue) / 12.0)
                    }
                    return round(computedPension).toInt()
                } else {
                    return 0
                }
            }
            PensionType.PSPP -> {
                val birthLD = LocalDate.parse(birthDate)
                val birthDouble:Double = birthLD.year + (birthLD.dayOfYear/365.0)
                val workStartLD = LocalDate.parse(workStartDate)
                val workStartDouble:Double = workStartLD.year + (workStartLD.dayOfYear/365.0)
                val retirementLD = LocalDate.parse(retirementDate)
                val retirementDouble = retirementLD.year + (retirementLD.dayOfYear/365.0)
                if (workStartLD.year < 1990)
                    return 0  // kludge, I could really compute the max cpp earnings for prior but I'm being lazy
                var averageCPPMaxInsurableEarnings = 0.0
                for (i in workStartLD.year until retirementLD.year+1) { // note that it stops when it hits "workEndYear+1", ie doesn't do the +1 year
                    averageCPPMaxInsurableEarnings += cppMaxInsurableEarnings[i-1990]
                }
                val yearsOfService = retirementDouble - workStartDouble
                averageCPPMaxInsurableEarnings /= (retirementLD.year - workStartLD.year + 1)
                val firstHalf = min(averageCPPMaxInsurableEarnings, best5YearsSalary.toDouble()) * .014 * yearsOfService
                var secondHalf = 0.0
                if (best5YearsSalary > averageCPPMaxInsurableEarnings) {
                    secondHalf = (best5YearsSalary - averageCPPMaxInsurableEarnings) * .02 * yearsOfService
                }
// Avg salary up to average YMPE (avg CPP max insurable earnings for each year of work) * 1.4% * years of full-time service +
//      Avg salary over average YMPE * 2% * years of full-time service
// it is adjusted for inflation
                var computedPension = (firstHalf + secondHalf) * (1 + inflationRate / 100.0).pow(iYear - retirementLD.year)
                if (iYear == retirementLD.year) {
                    computedPension *= ((12 - birthLD.monthValue) / 12.0)
                }
// If early retirement (ie retiree is not yet 65 or has not yet reached his 85 factor), then an early retirement penalty applies
// penalty is 3% * min(the number of years it would take to reach age 65, or the number of years until you reach 85 factor)
                val age65LD = LocalDate.ofYearDay(birthLD.year + 65, birthLD.dayOfYear)
                val age65Double:Double = age65LD.year + (age65LD.dayOfYear/365.0)
                val yearsToAge65Double:Double = age65Double - retirementDouble
                val factor85Double = workStartDouble + ((85 - (workStartDouble - birthDouble)) / 2)
                val factor85Year = truncate(factor85Double).toInt()
                val factor85DayOfYear = round((factor85Double - factor85Year) * 365).toInt()
                val factor85LD = LocalDate.ofYearDay(factor85Year, factor85DayOfYear)
                val yearsTo85FactorDouble:Double = factor85Double - retirementDouble
                if (retirementLD < factor85LD || retirementLD < age65LD) {
                    val minYears = min(yearsToAge65Double, yearsTo85FactorDouble)
                    computedPension -= (minYears * .03 * computedPension)
                }
                return round(computedPension).toInt()
            }
        }
    }

    fun getNextYear(i: Int = 0): PensionIncome {  // if this fun is here with no params, then crash.  If I delete it, everything's fine.
        // If I add a parm, it's fine.  WTF?
        return PensionIncome(Pension(
            id,
            name,
            value,
            pensionType,
            workStartDate,
            best5YearsSalary,
            pensionStartDate,
            pensionStartDelay
        ), retirementDate, inflationRate, cppAge, birthDate)
    }
}

data class Salary(
    var id: Int,
    var name: String,
    var annualValueAfterTax: Int,
    var estimatedGrowthPct: Double) {

    fun getSalary(iRetirementDate: String, iForYear: Int) : Int {
        if (iForYear > iRetirementDate.substring(0,4).toInt())
            return 0

        val multiplier = if (iForYear == iRetirementDate.substring(0,4).toInt()) {
            iRetirementDate.substring(5,7).toInt() / 12.0
        } else
            1.0
        val inflationMultiplier = (1 + estimatedGrowthPct/100.0).pow(iForYear - gCurrentDate.getYear())
        return round(annualValueAfterTax * multiplier * inflationMultiplier).toInt()
    }
}

data class CPP(
    var annualValueAt60: Int = 0,
    var annualValueAt65: Int = 0,
    var annualValueAt70: Int = 0) {

    // this section is called for the very first row, so data needs to be loaded from scenario / current balances
    fun getCPPIncome(tookAtAge: Int, iBirthDate: String, inflationRate: Double, forYear: Int): Int {
        if (forYear < iBirthDate.substring(0,4).toInt() + tookAtAge)
            return 0
        else {
            var originalAmount = 0
            var yearsToCompound = 0
            when (tookAtAge) {
                60 -> {
                    originalAmount = annualValueAt60
                    yearsToCompound = forYear - (iBirthDate.substring(0,4).toInt() + 60)
                }
                65 -> {
                    originalAmount = annualValueAt65
                    yearsToCompound = forYear - (iBirthDate.substring(0,4).toInt() + 65)
                }
                70 -> {
                    originalAmount = annualValueAt70
                    yearsToCompound = forYear - (iBirthDate.substring(0,4).toInt() + 70)
                }
            }
            val multiplier = if (forYear == iBirthDate.substring(0,4).toInt() + tookAtAge)
                (12 - iBirthDate.substring(5,7).toInt() + 1) / 12.0
            else
                1.0
            return round(originalAmount * (1 + inflationRate/100.0).pow(yearsToCompound) * multiplier).toInt()
        }
    }
}
data class OAS(
    var currentAnnualValue: Int = 0) {

    // I am adjusting the supplied OAS amount for inflation.  The amount on the website is adjusted for inflation quarterly
    fun getOASIncome(iBirthDate: String, inflationRate: Double, forYear: Int): Int {
        if (forYear < iBirthDate.substring(0,4).toInt() + 67)
            return 0

        val firstYearMultiplier = if (forYear == iBirthDate.substring(0,4).toInt() + 67)
            (12 - iBirthDate.substring(5,7).toInt() + 1) / 12.0
        else
            1.0
        return round((currentAnnualValue) * (1 + inflationRate/100.0).pow(forYear - gCurrentDate.getYear()) * firstYearMultiplier).toInt()
    }
}

class RetirementViewModel : ViewModel() {
    private var dataListener: ValueEventListener? = null
    private var dataUpdatedCallback: DataUpdatedCallback? = null
    private var loaded: Boolean = false
    private val userDefaults: MutableList<RetirementData> = ArrayList()
    private val scenarios: MutableList<RetirementData> = ArrayList()

    companion object {
        lateinit var singleInstance: RetirementViewModel // used to track static single instance of self

        fun clearDefaults() {
            singleInstance.userDefaults.clear()
        }
        fun clear() {
            if (singleInstance.dataListener != null) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Retirement")
                    .child(SpenderViewModel.myIndex().toString())
                    .removeEventListener(singleInstance.dataListener!!)
                singleInstance.dataListener = null
            }
            singleInstance.userDefaults.clear()
            singleInstance.scenarios.clear()
            gRetirementScenario = null
//            singleInstance.workingAssetList.clear()
  //          singleInstance.workingPensionList.clear()
    //        singleInstance.workingAdditionalList.clear()
            singleInstance.loaded = false
        }

        fun updateRetirementDefault(iRetirementData: RetirementData, iLocalOnly: Boolean) : RetirementData{
            val scenario = getUserDefault(iRetirementData.userID)
            if (scenario != null) {
                singleInstance.userDefaults.remove(scenario)
            }
            singleInstance.userDefaults.add(iRetirementData)
            if (!iLocalOnly) {
//                iRetirementData.pensions.clear()
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Defaults")
                    .child(SpenderViewModel.myIndex().toString())
                    .child("Retirement")
                    .child(iRetirementData.userID.toString())
                    .setValue(iRetirementData)
            }
            return iRetirementData
        }

        fun updateRetirementScenario(iRetirementData: RetirementData, iLocalOnly: Boolean) : RetirementData{
            val scenario = getScenario(iRetirementData.name)
            if (scenario != null) {
                singleInstance.scenarios.remove(scenario)
            }
            singleInstance.scenarios.add(iRetirementData)

            if (!iLocalOnly) {
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Retirement")
                    .child(SpenderViewModel.myIndex().toString())
                    .child(iRetirementData.name)
                    .setValue(iRetirementData)
            }
            return iRetirementData
        }

        fun deleteRetirementScenario(iScenarioName: String) {
            val scenario = getScenario(iScenarioName)
            if (scenario != null) {
                singleInstance.scenarios.remove(scenario)
                MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Retirement")
                    .child(SpenderViewModel.myIndex().toString())
                    .child(iScenarioName)
                    .removeValue()
            }
        }
        fun getListOfRetirementScenarios(): MutableList<String> {
            val tList: MutableList<String> = ArrayList()
            singleInstance.scenarios.forEach {
                tList.add(addUserToScenarioName(it.name, it.userID))
            }
            tList.sortBy { it }
            return tList
        }

        fun addUserToScenarioName(iScenarioName: String, iUserID: Int) : String {
            return iScenarioName + " (" + SpenderViewModel.getSpenderName(iUserID) + ")"
        }

        fun getUserDefault(id: Int): RetirementData? {
            return singleInstance.userDefaults.find { it.userID == id }
        }

        fun getScenario(ind: Int): RetirementData {
            return singleInstance.scenarios[ind]
        }
        private fun getScenario(iName: String): RetirementData? {
            return singleInstance.scenarios.find { it.name == iName }
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getEarliestRetirementYear(iRetirementScenario: RetirementData) : Int {
            val retirementDateSaver = iRetirementScenario.retirementDate
            var yearToTryToRetire = min(gCurrentDate.getYear() - 1, iRetirementScenario.retirementDate.substring(0,4).toInt())
            val retUser = getUserDefault(iRetirementScenario.userID)
            val endYear = if (retUser == null)
                yearToTryToRetire
            else
                retUser.birthDate.substring(0,4).toInt() + iRetirementScenario.planToAge
            while (yearToTryToRetire <= endYear) {
                iRetirementScenario.retirementDate = "$yearToTryToRetire-12-31"
                val calcRows = getCalculationRows(iRetirementScenario)
                var enoughIncome = true
                calcRows.forEach {
                    if (!it.availableIncomeGreaterThanTargetIncome()) {
                        enoughIncome = false
                        return@forEach
                    }
                }
//                val lastRow = calcRows[calcRows.size - 1]
//                if (lastRow.availableIncomeGreaterThanTargetIncome()) {
                if (enoughIncome && yearToTryToRetire != endYear) {
                    iRetirementScenario.retirementDate = retirementDateSaver
                    return yearToTryToRetire
                }
                yearToTryToRetire += 1
            }
            iRetirementScenario.retirementDate = retirementDateSaver
            return 0
        }

        fun getMaximumMonthlyIncome(iRetirementScenario: RetirementData) : Int {
            val targetMonthlyIncomeSaver = iRetirementScenario.targetMonthlyIncome
            var gap = 1000
            var lastSuccessfulAmount = -gap
            val tolerance = 5
            var success = true

            while (gap > tolerance) {
                if (success) {
                    if (gap <= tolerance) {
                        break
                    } else {
                        lastSuccessfulAmount += gap
                        iRetirementScenario.targetMonthlyIncome = lastSuccessfulAmount + gap
                        val calcRows = getCalculationRows(iRetirementScenario)
                        success = allRowsMetTarget(calcRows)
                    }
                } else {
                    gap /= 2
                    iRetirementScenario.targetMonthlyIncome = lastSuccessfulAmount + gap
                    val calcRows = getCalculationRows(iRetirementScenario)
                    success = allRowsMetTarget(calcRows)
                }
            }
            iRetirementScenario.targetMonthlyIncome = targetMonthlyIncomeSaver
            return lastSuccessfulAmount
        }

        private fun allRowsMetTarget(iList: MutableList<RetirementCalculationRow>): Boolean {
            iList.forEach {
                if (it.getTotalAvailableIncome() < it.getTotalTargetIncome()) {
                    return false
                }
            }
            return true
        }

        fun getCalculationRows(iRetirementScenario: RetirementData, iLogOutput: Boolean = false)
        : MutableList<RetirementCalculationRow> {
            val tList: MutableList<RetirementCalculationRow> = ArrayList()
            var calcRow = RetirementCalculationRow(iRetirementScenario, gCurrentDate.getYear(),
                iRetirementScenario.inflationRate, iRetirementScenario.investmentGrowthRate,
                iRetirementScenario.propertyGrowthRate)
            calcRow.ensureIncomeIsAdequate()
            calcRow.ensureIncomeIsAdequate2()
            if (iLogOutput) {
                Timber.tag("Alex").d("Year TgIncom MaxTaxA Taxable GrossIn     Tax NetInco  Salary     CPP     OAS RRSPWit RRSPBal TFSAWit TFSABal SaviWit SaviBal TotSavi PropWit PropBal NetWort")
                calcRow.logRow()
            }
            val retUser = getUserDefault(iRetirementScenario.userID) ?: return tList
            val endYear = retUser.birthDate.substring(0,4).toInt() + iRetirementScenario.planToAge
            while (calcRow.year < endYear) {
                tList.add(calcRow)
                calcRow = calcRow.createNextRow(iRetirementScenario)
                calcRow.ensureIncomeIsAdequate()
                calcRow.ensureIncomeIsAdequate2()
                if (iLogOutput)
                    calcRow.logRow()
            }
            tList.add(calcRow)
            return tList
        }
/*        fun populateWorkingAssetList(iList: MutableList<Asset>) {
            singleInstance.workingAssetList.clear()
            iList.forEach {
                addAssetToWorkingList(it.copy())
            }
        } */

        fun getWorkingAssetListCount(iAssetType: AssetType = AssetType.ALL) : Int {
            return if (gRetirementScenario == null) {
                0
            } else {
                if (iAssetType == AssetType.ALL)
                    gRetirementScenario!!.assets.size
                else {
                    var cnt = 0
                    gRetirementScenario!!.assets.forEach {
                        if (it.assetType == iAssetType)
                            cnt += 1
                    }
                    cnt
                }
            }
        }
        fun getWorkingAsset(iAssetName: String) : Asset? {
            return gRetirementScenario?.assets?.find {it.name == iAssetName}
        }
        fun getWorkingAsset(iIndex: Int) : Asset? {
            return gRetirementScenario?.assets?.get(iIndex)
        }
        fun deleteAssetFromWorkingList(iAssetName: String) {
            val asset = gRetirementScenario?.assets?.find {it.name == iAssetName}
            if (asset != null) {
                gRetirementScenario?.assets?.remove(asset)
            }
            updateDistributionOrderAsRequired()
        }
        fun addAssetToWorkingList(iAsset: Asset) {
            gRetirementScenario?.assets?.add(iAsset)
            updateDistributionOrderAsRequired()
        }
        fun updateAssetInWorkingList(iOldAssetName: String, iAsset: Asset) {
            if (gRetirementScenario != null) {
                val ind = gRetirementScenario!!.assets.indexOfFirst { it.name == iOldAssetName }
                if (ind == -1)
                    Timber.tag("Alex").d("WHY can't I find asset $iOldAssetName????")
                else
                    gRetirementScenario!!.assets[ind] = iAsset
                updateDistributionOrderAsRequired()
            }
        }
        private fun updateDistributionOrderAsRequired() {
            if (gRetirementScenario != null) {
                for (i in 0 until gRetirementScenario!!.assets.size) {
                    gRetirementScenario!!.assets[i].distributionOrder = i
                }
                gRetirementScenario!!.assets.sortBy { it.distributionOrder }
            }
        }
        fun changeDefaultDistributionOrder(iCurrentDistributionOrder: Int, iDirection: Int) {
            if (iCurrentDistributionOrder == 0 && iDirection == -1) {
                return
            }
            if (iCurrentDistributionOrder >= getWorkingAssetListCount() - 1 && iDirection == 1) {
                return
            }
            if (gRetirementScenario != null) {
                val temp = gRetirementScenario!!.assets[iCurrentDistributionOrder + iDirection]
                gRetirementScenario!!.assets[iCurrentDistributionOrder + iDirection] =
                    gRetirementScenario!!.assets[iCurrentDistributionOrder]
                gRetirementScenario!!.assets[iCurrentDistributionOrder] = temp
                updateDistributionOrderAsRequired()
            }
        }
/*        fun populateWorkingPensionList(iList: MutableList<Pension>) {
            singleInstance.workingPensionList.clear()
            iList.forEach {
                addPensionToWorkingList(it.copy())
            }
        } */
        fun getWorkingPensionListCount() : Int {
            return if (gRetirementScenario == null)
                0
            else
                gRetirementScenario!!.pensions.size
        }
        fun getWorkingPension(iPensionName: String) : Pension? {
            return gRetirementScenario?.pensions?.find {it.name == iPensionName}
        }
        fun getWorkingPension(iIndex: Int) : Pension? {
            return gRetirementScenario?.pensions?.get(iIndex)
        }
        fun deletePensionFromWorkingList(iPensionName: String) {
            val pension = gRetirementScenario?.pensions?.find {it.name == iPensionName}
            if (pension != null) {
                gRetirementScenario!!.pensions.remove(pension)
            }
        }
        fun addPensionToWorkingList(iPension: Pension) {
            gRetirementScenario?.pensions?.add(iPension)
        }
        fun updatePensionInWorkingList(iOldPensionName: String, iPension: Pension) {
            if (gRetirementScenario != null) {
                val ind =
                    gRetirementScenario!!.pensions.indexOfFirst { it.name == iOldPensionName }
                if (ind == -1)
                    Timber.tag("Alex").d("WHY can't I find pension $iOldPensionName????")
                else
                    gRetirementScenario!!.pensions[ind] = iPension
            }
        }
/*        fun populateWorkingAdditionalList(iList: MutableList<AdditionalItem>) {
            singleInstance.workingAdditionalList.clear()
            iList.forEach {
                addAdditionalItemToWorkingList(it.copy())
            }
        } */
        fun getWorkingAdditionalListCount(iType: AdditionalType? = null) : Int {
            return if (gRetirementScenario == null) {
                0
            } else {
                if (iType == null)
                    gRetirementScenario!!.additionalItems.size
                else {
                    var count = 0
                    gRetirementScenario!!.additionalItems.forEach {
                        if (it.type == iType)
                            count++
                    }
                    count
                }
            }
        }
        fun getWorkingAdditionalItem(iIndex: Int) : AdditionalItem? {
            return if (iIndex >= 0 &&
                gRetirementScenario != null &&
                iIndex < gRetirementScenario!!.additionalItems.size)
                gRetirementScenario!!.additionalItems[iIndex]
            else
                null
        }
        fun deleteAdditionalItemFromWorkingList(iIndex: Int) {
            if (gRetirementScenario != null) {
                if (iIndex < gRetirementScenario!!.additionalItems.size)
                    gRetirementScenario!!.additionalItems.removeAt(iIndex)
                var i = 0
                gRetirementScenario!!.additionalItems.forEach {
                    it.id = i
                    i += 1
                }
            }
        }
        fun addAdditionalItemToWorkingList(iItem: AdditionalItem) {
            if (gRetirementScenario != null) {
                gRetirementScenario!!.additionalItems.add(iItem)
                var i = 0
                gRetirementScenario!!.additionalItems.forEach {
                    it.id = i
                    i += 1
                }
            }
        }
        fun updateAdditionalItemInWorkingList(iIndex: Int, iAdditionalItem: AdditionalItem) {
            if (gRetirementScenario != null) {
                if (iIndex < gRetirementScenario!!.additionalItems.size)
                    gRetirementScenario!!.additionalItems[iIndex] = iAdditionalItem
            }
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (dataListener != null) {
            MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Retirement")
                .child(SpenderViewModel.myIndex().toString())
                .removeEventListener(dataListener!!)
            dataListener = null
        }
    }

    fun setCallback(iCallback: DataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }
    fun clearCallback() {
        dataUpdatedCallback = null
    }
    fun loadRetirementUsers() {
        singleInstance.dataListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                singleInstance.scenarios.clear()
                for (retirementChild in dataSnapshot.children)
                {
                    val retData = RetirementData.create(retirementChild.children.toMutableList())
                    updateRetirementScenario(retData, true)
                }
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                MyApplication.displayToast("User authorization failed 157.")
            }
        }
        MyApplication.database.getReference("Users/" + MyApplication.userUID + "/Retirement")
            .child(SpenderViewModel.myIndex().toString())
            .addValueEventListener(singleInstance.dataListener as ValueEventListener)
    }
}

data class Income(val id: Int, val amount: Int)

data class RetirementCalculationRow(val userID: Int, val year: Int, val inflationRate: Double,
    val investmentGrowthRate: Double, val propertyGrowthRate: Double) {
    private var targetAnnualIncome: Int = 0
    var cppIncome: Int = 0
    var oasIncome: Int = 0
    private var salaryIncomes: MutableList<Income> = ArrayList()
    var assetIncomes: MutableList<Asset> = ArrayList()
    var pensionIncomes: MutableList<PensionIncome> = ArrayList()
//    var additionalItems: MutableList<AdditionalItem> = ArrayList()
    private var parentScenario: RetirementData? = null

    constructor(iRetirementScenario: RetirementData, forYear: Int, inflationRate: Double,
        investmentGrowthRate: Double, propertyGrowthRate: Double)
            : this(iRetirementScenario.userID, forYear, inflationRate,
                investmentGrowthRate, propertyGrowthRate) {
        parentScenario = iRetirementScenario
        // this section is called for the very first row, so data needs to be loaded from scenario / current balances
        targetAnnualIncome = iRetirementScenario.targetMonthlyIncome * 12
        cppIncome = iRetirementScenario.cpp.getCPPIncome(iRetirementScenario.cppAge,
            iRetirementScenario.birthDate, iRetirementScenario.inflationRate, year)
        oasIncome = iRetirementScenario.oas.getOASIncome(iRetirementScenario.birthDate,
            iRetirementScenario.inflationRate, year)
        salaryIncomes.add(Income(iRetirementScenario.salary.id, iRetirementScenario.salary.getSalary(iRetirementScenario.retirementDate, year)))
        var amRetired = false
        if (parentScenario != null) {
            if (forYear > parentScenario!!.retirementDate.substring(0,4).toInt())
                amRetired = true
        }
        iRetirementScenario.assets.forEach {
            val additionalGrowth = parentScenario!!.getAdditionalGrowth(it.name, year)
            when (it.assetType) {
                AssetType.RRSP -> {
                    val rrsp = RRSP(it.id,
                        it.name,
                        it.getValue(),
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) investmentGrowthRate else it.estimatedGrowthPct,
                        if (amRetired) 0 else it.annualContribution,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder,
                        getAgeAtStartOfYear(forYear),
                        (it as RRSP).minimizeTax)
                    rrsp.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(rrsp)
                }
                AssetType.TFSA -> {
                    val tfsa = TFSA(it.id,
                        it.name,
                        it.getValue(),
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) investmentGrowthRate else it.estimatedGrowthPct,
                        if (amRetired) 0 else it.annualContribution,
                        it.willSellToFinanceRetirement,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder)
                    tfsa.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(tfsa)
                }
                AssetType.LIRA_LIF -> {
                    val lira = LIRALIF(it.id,
                        it.name,
                        it.getValue(),
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) investmentGrowthRate else it.estimatedGrowthPct,
                        if (amRetired) 0 else it.annualContribution,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder,
                        getAgeAtStartOfYear(forYear),
                        (it as LIRALIF).minimizeTax)
                    lira.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(lira)
                }
                AssetType.LIRA_Annuity -> {
                    val lira = LIRAANNUITY(it.id,
                        it.name,
                        it.getValue(),
                        (it as LIRAANNUITY).pensionStartDate,
                        it.currentYear,
                        it.annualAmount,
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) investmentGrowthRate else it.estimatedGrowthPct,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder)
                    lira.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(lira)
                }
                AssetType.SAVINGS -> {
                    val sav = Savings(it.id,
                        it.name,
                        it.getValue(),
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) investmentGrowthRate else it.estimatedGrowthPct,
                        if (amRetired) 0 else it.annualContribution,
                        it.willSellToFinanceRetirement,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder,
                        (it as Savings).taxSheltered)
                    sav.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(sav)
                }
                AssetType.PROPERTY -> {
                    val prop = Property(it.id,
                        it.name,
                        it.getValue(),
                        it.useDefaultGrowthPct,
                        if (it.useDefaultGrowthPct) propertyGrowthRate else it.estimatedGrowthPct,
                        it.willSellToFinanceRetirement,
                        12 - gCurrentDate.getMonth(),
                        it.distributionOrder,
                        (it as Property).useDefaultGrowthPctAsSavings,
                        if (it.useDefaultGrowthPctAsSavings) investmentGrowthRate else it.estimatedGrowthPctAsSavings,
                        it.scheduledPaymentName,
                        it.ownershipPct,
                        0,
                        it.primaryResidence)
                    prop.additionalGrowthThisYear = additionalGrowth
                    assetIncomes.add(prop)
                }
                else -> {
                    Timber.tag("Alex").d("What is this??")
                }
            }
        }
        iRetirementScenario.pensions.forEach {
            val pension = Pension(it.id,
                it.name,
                it.value,
                it.pensionType,
                it.workStartDate,
                it.best5YearsSalary,
                it.pensionStartDate,
                it.pensionStartDelay)
            pensionIncomes.add(PensionIncome(pension, iRetirementScenario.retirementDate,
                iRetirementScenario.inflationRate, iRetirementScenario.cppAge,
                iRetirementScenario.birthDate))
        }
//        iRetirementScenario.additionalItems.forEach {
  //          additionalItems.add(it.copy())
    //    }
    }

    fun getAgeAtStartOfYear(iYear: Int) : Int {
        return parentScenario?.getAgeAtStartOfYear(iYear) ?: 0
    }

    fun logRow() {
        val currentYear = gCurrentDate.getYear()
        val multiplier = (1 + inflationRate/100.0).pow(year - currentYear)
        val outputString =  String.format("%4d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d",
            year,
            getTotalTargetIncome(),
            round(gMinimizeTaxAmount*multiplier).toInt(),
            getTaxableIncome(year),
            getTotalGrossIncome(year),
            getTotalTax(),
            getTotalAvailableIncome(),
            salaryIncomes[0].amount,
            cppIncome,
            oasIncome,
            assetIncomes[0].withdrawalAmount,
            assetIncomes[0].getEndingBalance(),
            assetIncomes[1].withdrawalAmount,
            assetIncomes[1].getEndingBalance(),
            assetIncomes[2].withdrawalAmount,
            assetIncomes[2].getEndingBalance(),
            assetIncomes[1].getEndingBalance()+assetIncomes[2].getEndingBalance(),
            assetIncomes[3].withdrawalAmount,
            assetIncomes[3].getEndingBalance(),
            getNetWorth())
//        Log.d("Alex", "$userID $year tgInc ${gDec(targetAnnualIncome)} cpp ${gDec(cppIncome)} oas ${gDec(oasIncome)} sal $salaryIncomes pen $pensionIncomes")
//        assetIncomes.forEach {
//            Log.d("Alex", "${it.name} value ${gDec(it.value)} gpct ${gDec(it.estimatedGrowthPct)} growth ${gDec(it.growthThisYear)} withdrawal ${gDec(it.withdrawalAmount)} end ${gDec(it.getEndingBalance())}")
//        }
//        Log.d("Alex", "GrossInc ${gDec(getTotalGrossIncome())} TaxableInc ${gDec(getTaxableIncome())} Tax ${gDec(getTotalTax())} TotalNetInc ${gDec(getTotalNetIncome())}")
    }

    fun getMaximumTaxableIncome() : Int {
        val currentYear = gCurrentDate.getYear()
        val multiplier = (1 + inflationRate/100.0).pow(year - currentYear)
        return round(gMinimizeTaxAmount*multiplier).toInt()
    }

    fun getTotalTargetIncome() : Int {
        var totalAdditionalItemRequired = 0
        parentScenario?.additionalItems?.forEach {
            if (it.year == year && it.type == AdditionalType.EXPENSE)
                totalAdditionalItemRequired += it.amount
        }
        return targetAnnualIncome + totalAdditionalItemRequired
    }

    fun createNextRow(iRetirementScenario: RetirementData)
    : RetirementCalculationRow {
//        val retUser = RetirementViewModel.getUserDefaults(userID) ?: return null
        val nextRow = RetirementCalculationRow(userID, year+1, iRetirementScenario.inflationRate,
        iRetirementScenario.investmentGrowthRate, iRetirementScenario.propertyGrowthRate)

        nextRow.parentScenario = iRetirementScenario
        nextRow.targetAnnualIncome = round(targetAnnualIncome * (1 + inflationRate/100.0)).toInt()

        nextRow.cppIncome = iRetirementScenario.cpp.getCPPIncome(iRetirementScenario.cppAge,
            iRetirementScenario.birthDate, iRetirementScenario.inflationRate, nextRow.year)
        nextRow.oasIncome = iRetirementScenario.oas.getOASIncome(iRetirementScenario.birthDate,
            iRetirementScenario.inflationRate, nextRow.year)

        nextRow.salaryIncomes.add(Income(iRetirementScenario.salary.id, iRetirementScenario.salary.getSalary(iRetirementScenario.retirementDate, nextRow.year)))

        val retirementYear = iRetirementScenario.retirementDate.substring(0,4).toInt()
        assetIncomes.forEach {
            val additionalGrowth = parentScenario?.getAdditionalGrowth(it.name, year+1)
            val asset = it.getNextYear(nextRow.year > retirementYear)
            if (additionalGrowth != null) {
                asset.additionalGrowthThisYear = additionalGrowth
            }
            nextRow.assetIncomes.add(asset)
        }
        pensionIncomes.forEach {
            nextRow.pensionIncomes.add(it.getNextYear())
        }
        return nextRow
    }

    fun getNetWorth(iType: AssetType = AssetType.ALL) : Int {
        var tmp = 0
        assetIncomes.forEach {
            if (iType == AssetType.ALL || it.assetType == iType)
                tmp += it.getEndingBalance()
        }
        return tmp
    }

    fun getTotalGrossIncome(iYear: Int) : Int {
        var tTotal = cppIncome + oasIncome
        salaryIncomes.forEach {
            tTotal += it.amount
        }
        assetIncomes.forEach {
            tTotal += it.getGrossIncome()
        }
        pensionIncomes.forEach {
            tTotal += (it.getPensionIncome(iYear))
        }
        return tTotal
    }

    fun getTotalTax(): Int {
        val yearsInFuture = year - gCurrentDate.getYear()
        val birthYear = if (parentScenario == null)
            9999
        else
            parentScenario!!.birthDate.substring(0,4).toInt()
        return getFederalTax(getTaxableIncome(year), yearsInFuture, birthYear) +
                getOntarioTax(getTaxableIncome(year), yearsInFuture, birthYear)
    }

    fun availableIncomeGreaterThanTargetIncome(): Boolean {
        return getTotalAvailableIncome() >= getTotalTargetIncome()
    }

    fun getTotalAvailableIncome() : Int {
        val yearsInFuture = year - gCurrentDate.getYear()
        val taxableIncome = getTaxableIncome(year)
        val birthYear = if (parentScenario == null)
            9999
        else
            parentScenario!!.birthDate.substring(0,4).toInt()
        val tax = getFederalTax(taxableIncome, yearsInFuture, birthYear) +
            getOntarioTax(taxableIncome, yearsInFuture, birthYear)

        var tTotal = cppIncome + oasIncome
        salaryIncomes.forEach {
            tTotal += it.amount
        }
        assetIncomes.forEach {
            tTotal += it.withdrawalAmount
        }
        pensionIncomes.forEach {
            tTotal += (it.getPensionIncome(year))
        }

        return tTotal - tax
    }

    fun getTaxableIncome(iYear: Int) : Int {
        var tTotal = cppIncome + oasIncome
        assetIncomes.forEach {
            tTotal += it.getTaxableIncome(iYear)
        }
        pensionIncomes.forEach {
            tTotal += it.getPensionIncome(iYear)
        }
        return tTotal
    }

    fun getTotalSalary() : Int {
        var sal = 0
        salaryIncomes.forEach {
            sal += it.amount
        }
        return sal
    }

    private fun getFederalTax(iTaxableIncome: Int, iYearsInTheFuture: Int, iBirthYear: Int): Int {
        var tTax = 0
        val inflationMultiplier = (1 + inflationRate/100.0).pow(iYearsInTheFuture)

        var remainingIncome = iTaxableIncome
        var threshold = round(221708 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .33).toInt()
            remainingIncome = threshold
        }
        threshold = round(155625 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .29).toInt()
            remainingIncome = threshold
        }
        threshold = round(100392 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .26).toInt()
            remainingIncome = threshold
        }
        threshold = round(50197 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .2005).toInt()
            remainingIncome = threshold
        }
        threshold = round(14398 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .15).toInt()
        }

        // Federal senior age tax credit
        val age = gCurrentDate.getYear() - iBirthYear + iYearsInTheFuture
        val minThreshold = 39826 * inflationMultiplier // as of 2022
        val maxThreshold = 92479 * inflationMultiplier // as of 2022
        val seniorAgeTaxCredit = 7898 * inflationMultiplier // as of 2022
        if (age >= 65) {
            if (iTaxableIncome <= minThreshold) {
                tTax -= (seniorAgeTaxCredit * .15).toInt()
            } else if (iTaxableIncome < maxThreshold) {
                tTax -= ((seniorAgeTaxCredit - (iTaxableIncome - minThreshold) * .15) * .15).toInt()
            }
        }
        return if (tTax < 0) 0 else tTax
    }

    private fun getOntarioTax(iTaxableIncome: Int, iYearsInTheFuture: Int, iBirthYear: Int): Int {
        var tTax = 0
        val inflationMultiplier = (1 + inflationRate/100.0).pow(iYearsInTheFuture)

        var remainingIncome = iTaxableIncome
        var threshold = round(220000 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .1316).toInt()
            remainingIncome = threshold
        }
        threshold = round(150000 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .1216).toInt()
            remainingIncome = threshold
        }
        threshold = round(92454 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .1116).toInt()
            remainingIncome = threshold
        }
        threshold = round(46226 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .0915).toInt()
            remainingIncome = threshold
        }
        threshold = round(16230 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .0505).toInt()
        }

        // Ontario senior age tax credit
        val age = gCurrentDate.getYear() - iBirthYear + iYearsInTheFuture
        val minThreshold = 40495 * inflationMultiplier // as of 2022
        val maxThreshold = 76762 * inflationMultiplier // as of 2022
        val seniorAgeTaxCredit = 5440 * inflationMultiplier // as of 2022
        if (age >= 65) {
            if (iTaxableIncome <= minThreshold) {
                tTax -= (seniorAgeTaxCredit * .0505).toInt()
            } else if (iTaxableIncome < maxThreshold) {
                tTax -= ((seniorAgeTaxCredit - (iTaxableIncome - minThreshold) * .15) * .0505).toInt()
            }
        }

        return if (tTax < 0) 0 else tTax
    }

    private fun howMuchGrossDoINeed(iTotalTaxableNetIncomeNeeded: Int, iCurrentTotalTaxableIncome: Int,
                                    iCurrentTotalTax: Int, iTryThisAmount: Int, iYearsInTheFuture: Int) : Int {
        val birthYear = if (parentScenario == null)
            9999
        else
            parentScenario!!.birthDate.substring(0,4).toInt()
        val incrementalTax = getFederalTax(iTryThisAmount+iCurrentTotalTaxableIncome, iYearsInTheFuture, birthYear) +
                getOntarioTax(iTryThisAmount+iCurrentTotalTaxableIncome, iYearsInTheFuture, birthYear) -
                iCurrentTotalTax
        return if (abs(iTotalTaxableNetIncomeNeeded + iCurrentTotalTax + incrementalTax - iTryThisAmount) < 1.0) {
            iTryThisAmount+1 // add a bit for rounding purposes
        } else {
            howMuchGrossDoINeed(iTotalTaxableNetIncomeNeeded, iCurrentTotalTaxableIncome, iCurrentTotalTax,
                iTotalTaxableNetIncomeNeeded + iCurrentTotalTax + incrementalTax, iYearsInTheFuture)
        }

    }

    fun ensureIncomeIsAdequate() {
        if (getTotalAvailableIncome() >= getTotalTargetIncome())
            return
//        Log.d("Alex", "$year gross ${getTotalGrossIncome(year)} totalNetIncome ${getTotalAvailableIncome()} taxable ${getTaxableIncome(year)}")
        for (i in 0 until assetIncomes.size) {
            assetIncomes.forEach {
                val totalNetIncome = getTotalAvailableIncome()
                if (it.distributionOrder == i && it.getAvailableValue() > 0.0 &&
                    totalNetIncome < getTotalTargetIncome()) {
                    val netWithdrawalAmountNeeded = min(it.getAvailableValue(), getTotalTargetIncome() - totalNetIncome)
                    if (it.withdrawalIsTaxable()) { // e.g. RRSP
                        it.withdraw(
                            howMuchGrossDoINeed(netWithdrawalAmountNeeded, getTaxableIncome(year),
                                getTotalTax(),
                                netWithdrawalAmountNeeded, year - gCurrentDate.getYear()),
                            getTaxableIncome(year), // tell the RRSP class how much taxable income already exists this year
                            year,
                            parentScenario
                            )
                    } else {
                        it.withdraw(netWithdrawalAmountNeeded, year)
                    }
                }
            }
        }
    }
    fun ensureIncomeIsAdequate2() {
        if (getTotalAvailableIncome() >= getTotalTargetIncome())
            return
        for (i in 0 until assetIncomes.size) {
            assetIncomes.forEach {
                if (it.distributionOrder == i && it.getAvailableValue() > 0.0 &&
                    getTotalAvailableIncome() < targetAnnualIncome) {
                    val netWithdrawalAmountNeeded = min(it.getAvailableValue(), getTotalTargetIncome() - getTotalAvailableIncome())
                    if (it.withdrawalIsTaxable()) { // e.g. RRSP
                        it.withdrawExtra(
                            howMuchGrossDoINeed(netWithdrawalAmountNeeded, getTaxableIncome(year),
                                getTotalTax(),
                                netWithdrawalAmountNeeded, year - gCurrentDate.getYear()),
                            getTaxableIncome(year), // tell the RRSP class how much taxable income already exists this year
                            year,
                            parentScenario
                        )
                    }
                }
            }
        }
    }
    fun getMinimumRRIFWithdrawal(iAgeAtStartOfYear: Int) : Int {
        val minimumWithdrawalPct = if (parentScenario == null)
            0.0
        else {
            getMinimumRRIFWithdrawalPercentage(iAgeAtStartOfYear)
        }

        var minRRIFWithdrawal = 0
        assetIncomes.forEach {
            if (it.assetType == AssetType.RRSP)
                minRRIFWithdrawal += round((it as RRSP).getValue() * minimumWithdrawalPct).toInt()
        }
        return minRRIFWithdrawal
    }
    fun getMinimumRRIFWithdrawalPercentage() : Double {
        return getMinimumRRIFWithdrawalPercentage(getAgeAtStartOfYear(year))
    }
}

fun getMinimumRRIFWithdrawalPercentage(ageAtStartOfYear: Int): Double {
    // minimum withdrawal from RRIF is this percentage below * value at start of year
    // You must convert your RRSP to an RRIF by December 31 of the year you turn 71
    // You must begin withdrawing money from your RRIF the year after your 71st birthday. (for me 2036 5.28%)
    if (ageAtStartOfYear < 71)
        return 0.0
    return when (ageAtStartOfYear) {
        70 -> .0500
        71 -> .0528
        72 -> .0540
        73 -> .0553
        74 -> .0567
        75 -> .0582
        76 -> .0598
        77 -> .0617
        78 -> .0636
        79 -> .0658
        80 -> .0682
        81 -> .0708
        82 -> .0738
        83 -> .0771
        84 -> .0808
        85 -> .0851
        86 -> .0899
        87 -> .0955
        88 -> .1021
        89 -> .1099
        90 -> .1192
        91 -> .1306
        92 -> .1449
        93 -> .1634
        94 -> .1879
        else -> .20
    }
}
