@file:Suppress("HardCodedStringLiteral")

package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

const val gMinimizeTaxAmount = 45142

enum class AssetType(val code: Int) {
    RRSP(1),
    TFSA(2),
    SAVINGS(3),
    PROPERTY(5),
    ALL(6);
    companion object {
        fun getText(iValue: AssetType): String {
            return when (iValue) {
                RRSP -> "RRSP"
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

data class RetirementData(
    var name: String,
    var userID: Int,
    var targetMonthlyIncome: Int,
    var retirementDate: String,
    var planToAge: Int,
    var cppAge: Int,
    var minimizeTax: Boolean,
    var inflationRate: Double,
    var birthDate: String) {
        var assets: MutableList<Asset> = ArrayList()
        var pensions: MutableList<Pension> = ArrayList()
        var salary: Salary = Salary(0, "", 0, 0.0)
        var cpp: CPP = CPP()
        var oas: OAS = OAS()

    fun updateDistributionOrderAsRequired() {
        for (i in 0 until assets.size) {
            if (assets[i].distributionOrder != i) {
                assets[i].distributionOrder = i
           //     TODO(update in FIREBASE)
            }
        }
        assets.sortBy { it.distributionOrder }
    }
    fun updateDistributionOrder(id: Int, iNewDistributionOrder: Int) {
        val asset = assets.find { it.id == id }
        if (asset != null) {
            asset.distributionOrder = iNewDistributionOrder
            assets.sortBy { it.distributionOrder }
        }
    }
    fun setAssetsAndPensionsAndCPPandOAS(iRetirementScenario: RetirementData) {
        iRetirementScenario.assets.forEach {
            assets.add(it.copy())
        }
        iRetirementScenario.pensions.forEach {
            pensions.add(it.copy())
        }
        cpp = CPP(iRetirementScenario.cpp)
        oas = OAS(iRetirementScenario.oas)
    }
    fun deleteAsset(iAssetID: Int) {
        val asset = assets.find {it.id == iAssetID}
        if (asset != null) {
            assets.remove(asset)
        }
        updateDistributionOrderAsRequired()
    }
    fun addAsset(iAsset: Asset, iAssetID: Int = -1) {
        if (iAssetID == -1)
            assets.add(iAsset)
        else
            assets.add(iAssetID, iAsset)
        updateDistributionOrderAsRequired()
        assets.sortBy { it.distributionOrder }
    }
    fun updateAsset(iOldAssetID: Int, iAsset: Asset) {
        deleteAsset(iOldAssetID)
        addAsset(iAsset, iAsset.distributionOrder)
    }
    fun deletePension(iPensionID: Int) {
        val pension = pensions.find {it.id == iPensionID}
        if (pension != null) {
            pensions.remove(pension)
        }
        updateDistributionOrderAsRequired()
    }
    fun addPension(iPension: Pension, iPensionID: Int = -1) {
        if (iPensionID == -1)
            pensions.add(iPension)
        else
            pensions.add(iPensionID, iPension)
    }
    fun updatePension(iOldPensionID: Int, iPension: Pension) {
        deletePension(iOldPensionID)
        addPension(iPension)
    }
    fun getAgeAtStartOfYear(iYear: Int) : Int {
        val birthYear = birthDate.substring(0,4)
        return if (isNumber(birthYear))
            iYear - birthYear.toInt() - 1
        else
            99
    }
}

abstract class Asset(val id: Int, val type: AssetType, val name: String, var value: Int,
                     var estimatedGrowthPct: Double, var annualContribution: Int,
                     val monthsOfGrowthThisYear: Int = 12, var distributionOrder: Int) {
    var growthThisYear = 0
    var withdrawalAmount = 0
    init {
        this.computeGrowth()
    }
    open fun getEndingBalance() : Int {
        return  value - withdrawalAmount + growthThisYear
    }
    open fun computeGrowth() {
        growthThisYear = if (value != 0 && withdrawalAmount < value)
            round((value  - withdrawalAmount) *
                    (estimatedGrowthPct / 100 * monthsOfGrowthThisYear / 12) + annualContribution).toInt()
        else
            0
    }
    open fun getTaxableIncome(): Int {
        return 0
    }
    open fun generatesTaxableIncome(): Boolean {
        return false
    }
    abstract fun copy() : Asset
    abstract fun getNextYear(iAmIRetired: Boolean) : Asset
    abstract fun withdraw(iAmount: Int, iYear: Int)
    open fun withdraw(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int, iRetirementData: RetirementData?) {
        withdraw(iAmount, iYear)
    }
}

class RRSP (
    id: Int,
    name: String,
    value: Int,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int) :
        Asset(id, AssetType.RRSP, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear, distributionOrder) {

    override fun generatesTaxableIncome(): Boolean {
        return true
    }

    override fun copy(): Asset {
        return RRSP(id, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear,
            distributionOrder)
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return RRSP(
            id,
            name,
            getEndingBalance(),
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            12,
            distributionOrder
        )
    }

    override fun withdraw(iAmount: Int, iYear: Int) {
        return withdraw(iAmount, 0, iYear, null)
    }
    override fun withdraw(iAmount: Int, iTaxableIncomeAlreadyMade: Int, iYear: Int,
        iRetirementData: RetirementData?) {
        val inflationRate = iRetirementData?.inflationRate ?: 0.0
        val minimizeTax = iRetirementData?.minimizeTax ?: true
        val minimumWithdrawalAmount = if (iRetirementData == null)
            0
        else {
            round(getMinimumRRIFWithdrawalPercentage(iRetirementData.getAgeAtStartOfYear(iYear)) * value).toInt()
        }
        val minWithdrawal = max(iAmount, minimumWithdrawalAmount)

        withdrawalAmount = min(value, minWithdrawal)
//        Log.d("Alex", "Min withdrawal amount is $minimumWithdrawalAmount $minWithdrawal $withdrawalAmount")
        val cal = android.icu.util.Calendar.getInstance()
        val currentMinimalTaxAmount = round(gMinimizeTaxAmount *
                (1 + inflationRate/100.0).pow(iYear - cal.get(Calendar.YEAR))).toInt()
        if (minimizeTax) {
            if (withdrawalAmount > currentMinimalTaxAmount - iTaxableIncomeAlreadyMade)
                withdrawalAmount = max(currentMinimalTaxAmount - iTaxableIncomeAlreadyMade, minimumWithdrawalAmount)
        }
        computeGrowth()
    }
    override fun getTaxableIncome(): Int {
        return withdrawalAmount
    }
}

class TFSA(
    id: Int,
    name: String,
    value: Int,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int) :
        Asset(id, AssetType.TFSA, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear, distributionOrder) {

    override fun withdraw(iAmount: Int, iYear: Int) {
        withdrawalAmount = min(iAmount, value)
        computeGrowth()
    }

    override fun copy(): Asset {
        return TFSA(id, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear, distributionOrder)
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return TFSA(
            id,
            name,
            getEndingBalance(),
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            12,
            distributionOrder
        )
    }
}

class Savings(
    id: Int,
    name: String,
    value: Int,
    estimatedGrowthPct: Double,
    annualContribution: Int,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int) :
        Asset(id, AssetType.SAVINGS, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear, distributionOrder) {

    override fun withdraw(iAmount: Int, iYear: Int) {
        withdrawalAmount = min(iAmount, value)
        computeGrowth()
    }

    override fun copy(): Asset {
        return Savings(id, name, value, estimatedGrowthPct, annualContribution, monthsOfGrowthThisYear, distributionOrder)
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return Savings(
            id,
            name,
            getEndingBalance(),
            estimatedGrowthPct,
            if (iAmIRetired) 0 else annualContribution,
            12,
            distributionOrder
        )
    }
}

class Property(
    id: Int,
    name: String,
    value: Int,
    private var estimatedGrowthPctAsProperty: Double,
    monthsOfGrowthThisYear: Int,
    distributionOrder: Int,
    var estimatedGrowthPctAsSavings: Double,
    var willSellToFinanceRetirement: Boolean,
    var scheduledPaymentName: String,
    var ownershipPct: Double,
    var soldInYear: Int) :
        Asset(id, AssetType.PROPERTY, name, round(value*ownershipPct/100).toInt(), estimatedGrowthPctAsProperty,
            0, monthsOfGrowthThisYear, distributionOrder) {

    override fun copy(): Asset {
        return Property(id, name, (value/(ownershipPct/100)).toInt(), estimatedGrowthPctAsProperty, monthsOfGrowthThisYear,
        distributionOrder, estimatedGrowthPctAsSavings, willSellToFinanceRetirement, scheduledPaymentName,
        ownershipPct, soldInYear)
    }
    override fun getNextYear(iAmIRetired: Boolean): Asset {
        return Property(
            id,
            name,
            getEndingBalance(),
            estimatedGrowthPct,
            12,
            distributionOrder,
            estimatedGrowthPctAsSavings,
            willSellToFinanceRetirement,
            scheduledPaymentName,
            100.0,
            soldInYear
        )
    }

    override fun withdraw(iAmount: Int, iYear: Int) {
        if (willSellToFinanceRetirement) {
            if (soldInYear == 0) {
                soldInYear = iYear
            }
            withdrawalAmount = min(iAmount, value)
            estimatedGrowthPct = estimatedGrowthPctAsSavings
            computeGrowth()
        }
    }
}

class Pension(
    val id: Int,
    val name: String,
    var value: Int,
    val pensionType: PensionType,
    var workStartDate: String,
    val best5YearsSalary: Int,
    val pensionStartDate: String) {

    fun copy(): Pension {
        return Pension(id, name, value, pensionType, workStartDate, best5YearsSalary, pensionStartDate)
    }
}

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

    init {
        id = iPension.id
        name = iPension.name
        value = iPension.value
        pensionType = iPension.pensionType
        workStartDate = iPension.workStartDate
        best5YearsSalary = iPension.best5YearsSalary
        pensionStartDate = iPension.pensionStartDate
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
                if (iYear >= retirementLD.year) {
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
//      pensionReduction = Math.min((0.025 * (85 - brent85FactorAtRetirement)), 0.05 * (65 - DateDiffInDays(brentBirthDate, brentRetirementDate) / 365));
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
                        val pensionReduction = min((0.025 * (85.0 - factor85AtRetirement)),
                        0.05 * (65.0 - (retirementDouble - birthDouble)))

                        computedPension -= (computedPension * pensionReduction)
                    }
                    if (iYear == retirementLD.year) {
                        computedPension *= ((12 - birthLD.monthValue) / 12.0)
                    }
                    return round(computedPension).toInt()
                } else {
                    return 0
                }
            }
            PensionType.PSPP -> {
                return 0
// Federal employee pension
// Need:
//  CPP max insurable earnings in some year, and that year
//  inflation rate
//  expected highest 5-year average salary
//  start work date
//  retirement date
// Avg salary up to average YMPE (avg CPP max insurable earnings for each year of work) * 1.4% * years of full-time service +
//      Avg salary over average YMPE * 2% * years of full-time service
// it is adjusted for inflation

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
            pensionStartDate
        ), retirementDate, inflationRate, cppAge, birthDate)
    }
}

data class Salary(
    var id: Int,
    var name: String,
    var annualValueAfterTax: Int,
    var estimatedGrowthPct: Double) {

    fun getSalary(iRetirementDate: String, iForYear: Int) : Int {
        val retirementYear = iRetirementDate.subSequence(0,4).toString().toInt()

        if (iForYear > retirementYear)
            return 0

        val multiplier = if (iForYear == retirementYear) {
            iRetirementDate.substring(5,7).toInt() / 12.0
        } else
            1.0
        return round(annualValueAfterTax * multiplier).toInt()
    }
}

data class CPP(
    var annualValueAt60: Int = 0,
    var annualValueAt65: Int = 0,
    var annualValueAt70: Int = 0,
    var inflationRate: Double = 0.0) {

    constructor(iCPP: CPP)
            : this(iCPP.annualValueAt60, iCPP.annualValueAt65,
            iCPP.annualValueAt70, iCPP.inflationRate)

    // this section is called for the very first row, so data needs to be loaded from scenario / current balances
    // I am not adjusting the supplied CPP amount for inflation.  The website isn't clear on this...
    fun getCPPIncome(tookAtAge: Int, iBirthDate: String, forYear: Int): Int {
        val birthYear = iBirthDate.substring(0,4).toInt()
        val birthMonth = iBirthDate.substring(5,7).toInt()
        if (forYear < birthYear + tookAtAge)
            return 0
        else {
            var originalAmount = 0
            var yearsToCompound = 0
            when (tookAtAge) {
                60 -> {
                    originalAmount = annualValueAt60
                    yearsToCompound = forYear - (birthYear + 60)
                }
                65 -> {
                    originalAmount = annualValueAt65
                    yearsToCompound = forYear - (birthYear + 65)
                }
                70 -> {
                    originalAmount = annualValueAt70
                    yearsToCompound = forYear - (birthYear + 70)
                }
            }
            val multiplier = if (forYear == birthYear + tookAtAge)
                (12 - birthMonth + 1) / 12.0
            else
                1.0
            return round(originalAmount * (1 + inflationRate/100.0).pow(yearsToCompound) * multiplier).toInt()
        }
    }
}
data class OAS(
    var currentAnnualValue: Int = 0,
    var inflationRate: Double = 0.0) {

    constructor(iOAS: OAS)
            : this(iOAS.currentAnnualValue, iOAS.inflationRate)

    // I am adjusting the supplied OAS amount for inflation.  The amount on the website is adjusted for inflation quarterly
    fun getOASIncome(iBirthDate: String, forYear: Int): Int {
        val birthYear = iBirthDate.substring(0,4).toInt()
        val birthMonth = iBirthDate.substring(5,7).toInt()
        if (forYear < birthYear + 67)
            return 0

        val firstYearMultiplier = if (forYear == birthYear + 67)
            (12 - birthMonth + 1) / 12.0
        else
            1.0
        val cal = android.icu.util.Calendar.getInstance()
        return round((currentAnnualValue) * (1 + inflationRate/100.0).pow(forYear - cal.get(Calendar.YEAR)) * firstYearMultiplier).toInt()
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

        fun getListOfRetirementScenarios(): MutableList<String> {
            val tList: MutableList<String> = ArrayList()
            singleInstance.scenarios.forEach {
                tList.add(it.name)
            }
            return tList
        }

        fun getUserDefault(id: Int): RetirementData? {
            return singleInstance.userDefaults.find { it.userID == id }
        }

        fun changeDefaultDistributionOrder(iUser: Int, iCurrentDistributionOrder: Int, iDirection: Int) {
            val userDefs = singleInstance.userDefaults.find { it.userID == iUser }
                ?: //               Log.d("Alex", "can't find user")
                return

            if (iCurrentDistributionOrder == 0 && iDirection == -1) {
 //               Log.d("Alex", "${userDefs.assets[0].name} is already at start of list")
                return
            }
            if (iCurrentDistributionOrder >= userDefs.assets.count() - 1 && iDirection == 1) {
//                Log.d("Alex", "${userDefs.assets[userDefs.assets.count()-1].name} is already at end of list")
                return
            }

            val temp = userDefs.assets[iCurrentDistributionOrder + iDirection]
            userDefs.assets[iCurrentDistributionOrder + iDirection] = userDefs.assets[iCurrentDistributionOrder]
            userDefs.assets[iCurrentDistributionOrder] = temp
            userDefs.updateDistributionOrderAsRequired()
        }

        fun getScenario(iName: String): RetirementData? {
            return singleInstance.scenarios.find { it.name == iName }
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        fun getEarliestRetirementYear(iRetirementScenario: RetirementData) : Int {
            val cal = android.icu.util.Calendar.getInstance()
            val retirementDateSaver = iRetirementScenario.retirementDate
            var yearToTryToRetire = min(cal.get(Calendar.YEAR) - 1, iRetirementScenario.retirementDate.substring(0,4).toInt())
            val retUser = getUserDefault(iRetirementScenario.userID)
            val endYear = if (retUser == null)
                yearToTryToRetire
            else
                retUser.birthDate.substring(0,4).toInt() + iRetirementScenario.planToAge
            while (yearToTryToRetire <= endYear) {
                iRetirementScenario.retirementDate = "$yearToTryToRetire-12-31"
                val calcRows = getCalculationRows(iRetirementScenario)
                val lastRow = calcRows[calcRows.size - 1]
                if (lastRow.netIncomeGreaterThanTargetIncome()) {
                    iRetirementScenario.retirementDate = retirementDateSaver
                    return yearToTryToRetire
                }
                yearToTryToRetire += 1
            }
            iRetirementScenario.retirementDate = retirementDateSaver
            return endYear
        }

        fun getMaximumMonthlyIncome(iRetirementScenario: RetirementData) : Int {
            val targetMonthlyIncomeSaver = iRetirementScenario.targetMonthlyIncome
            var gap = 1000
            var lastSuccessfulAmount = 0
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
                if (it.getTotalNetIncome() < it.targetAnnualIncome)
                    return false
            }
            return true
        }

        fun getCalculationRows(iRetirementScenario: RetirementData, iLogOutput: Boolean = false)
        : MutableList<RetirementCalculationRow> {
            val tList: MutableList<RetirementCalculationRow> = ArrayList()
            val cal = android.icu.util.Calendar.getInstance()
            var calcRow = RetirementCalculationRow(iRetirementScenario, cal.get(Calendar.YEAR),
                iRetirementScenario.inflationRate)
            calcRow.ensureIncomeIsAdequate()
            if (iLogOutput) {
                Log.d("Alex", "Year TgIncom MaxTaxA Taxable GrossIn     Tax NetInco  Salary     CPP     OAS RRSPWit RRSPBal TFSAWit TFSABal SaviWit SaviBal TotSavi PropWit PropBal NetWort")
                calcRow.logRow()
            }
            val retUser = getUserDefault(iRetirementScenario.userID) ?: return tList
            val endYear = retUser.birthDate.substring(0,4).toInt() + iRetirementScenario.planToAge
            while (calcRow.year < endYear) {
                tList.add(calcRow)
                calcRow = calcRow.createNextRow(iRetirementScenario)
                calcRow.ensureIncomeIsAdequate()
                if (iLogOutput)
                    calcRow.logRow()
            }
            tList.add(calcRow)
            return tList
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
//                dataSnapshot.children.forEach() // ie for each scenario
                {
                    val retData = RetirementData("", 0, 0, "", 0, 0,true, 0.0, "")

                    for (data in retirementChild.children.toMutableList()) {
                        when (data.key.toString()) {
                            "userID" -> retData.userID = data.value.toString().toInt()
                            "birthDate" -> retData.birthDate = data.value.toString()
                            "cppAge" -> retData.cppAge = data.value.toString().toInt()
                            "inflationRate" -> retData.inflationRate = data.value.toString().toDouble()
                            "minimizeTax" -> retData.minimizeTax = (data.value.toString() == "true")
                            "name" -> retData.name = data.value.toString()
                            "planToAge" -> retData.planToAge = data.value.toString().toInt()
                            "retirementDate" -> retData.retirementDate = data.value.toString()
                            "targetMonthlyIncome" -> retData.targetMonthlyIncome = data.value.toString().toInt()
                            "cpp" -> {
                                var v60 = 0
                                var v65 = 0
                                var v70 = 0
                                var estimatedGrowthPct = 0.0
                                for (cppRow in data.children.toMutableList()) {
                                    when (cppRow.key.toString()) {
                                        "annualValueAt60" -> v60 =
                                            cppRow.value.toString().toInt()
                                        "annualValueAt65" -> v65 =
                                            cppRow.value.toString().toInt()
                                        "annualValueAt70" -> v70 =
                                            cppRow.value.toString().toInt()
                                        "estimatedGrowthPct" -> estimatedGrowthPct =
                                            cppRow.value.toString().toDouble()
                                    }
                                    retData.cpp = CPP(v60, v65, v70, estimatedGrowthPct)
                                }
                            }
                            "oas" -> {
                                var currentAnnualValue = 0
                                var inflationRate = 0.0
                                for (oasRow in data.children.toMutableList()) {
                                    when (oasRow.key.toString()) {
                                        "annualValueAt67" -> currentAnnualValue =
                                            oasRow.value.toString().toInt()
                                        "inflationRate" -> inflationRate =
                                            oasRow.value.toString().toDouble()
                                    }
                                    retData.oas = OAS(currentAnnualValue, inflationRate)
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
                                    retData.salary = Salary(0, name, annualValueAfterTax, estimatedGrowthPct)
                                }
                            }
                            "assets" -> {
                                for (asset in data.children.toMutableList()) {
                                    var assetID = -1
                                    var distributionOrder = 0
                                    for (det in asset.children.toMutableList()) {
                                        when (det.key.toString()) {
                                            "distributionOrder" -> distributionOrder = det.value.toString().toInt()
                                            "id" -> assetID = det.value.toString().toInt()
                                        }
                                    }
                                    val userDefault = getUserDefault(SpenderViewModel.myIndex())
                                    val newAsset: Asset? = userDefault?.assets?.find {it.id == assetID}
                                    if (newAsset != null) {
                                        val assetToAdd = newAsset.copy()
                                        assetToAdd.distributionOrder = distributionOrder
                                        retData.assets.add(assetToAdd)
                                    }
                                }
                                retData.assets.sortBy { it.distributionOrder }
                            }
                        }
                    }
                    updateRetirementScenario(retData, true)
                }
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

data class RetirementCalculationRow(val userID: Int, val year: Int, val inflationRate: Double) {
    var targetAnnualIncome: Int = 0
    var cppIncome: Int = 0
    var oasIncome: Int = 0
    private var salaryIncomes: MutableList<Income> = ArrayList()
    var assetIncomes: MutableList<Asset> = ArrayList()
    var pensionIncomes: MutableList<PensionIncome> = ArrayList()
    var parentScenario: RetirementData? = null

    constructor(iRetirementScenario: RetirementData, forYear: Int, inflationRate: Double)
            : this(iRetirementScenario.userID, forYear, inflationRate) {
        parentScenario = iRetirementScenario
        // this section is called for the very first row, so data needs to be loaded from scenario / current balances
        targetAnnualIncome = iRetirementScenario.targetMonthlyIncome * 12
//        val retUser = RetirementViewModel.getUserDefaults(userID)
//        if (retUser != null) {
            cppIncome = iRetirementScenario.cpp.getCPPIncome(iRetirementScenario.cppAge,
                iRetirementScenario.birthDate, year)
            oasIncome = iRetirementScenario.oas.getOASIncome(iRetirementScenario.birthDate, year)
//        }
        salaryIncomes.add(Income(iRetirementScenario.salary.id, iRetirementScenario.salary.getSalary(iRetirementScenario.retirementDate, year)))
//        iRetirementScenario.pensions.forEach {
//            pensionIncomes.add(Income(it.id, it.annualValue))
//        }
        val cal = android.icu.util.Calendar.getInstance()
        iRetirementScenario.assets.forEach {
            when (it.type) {
                AssetType.RRSP -> {
                    val rrsp = RRSP(it.id,
                        it.name,
                        it.value,
                        it.estimatedGrowthPct,
                        it.annualContribution,
                        12 - cal.get(Calendar.MONTH) - 1,
                        it.distributionOrder)
                    assetIncomes.add(rrsp)
                }
                AssetType.TFSA -> {
                    val tfsa = TFSA(it.id,
                        it.name,
                        it.value,
                        it.estimatedGrowthPct,
                        it.annualContribution,
                        12 - cal.get(Calendar.MONTH) - 1,
                        it.distributionOrder)
                    assetIncomes.add(tfsa)
                }
                AssetType.SAVINGS -> {
                    val sav = Savings(it.id,
                        it.name,
                        it.value,
                        it.estimatedGrowthPct,
                        it.annualContribution,
                        12 - cal.get(Calendar.MONTH) - 1,
                        it.distributionOrder)
                    assetIncomes.add(sav)
                }
                AssetType.PROPERTY -> {
                    val prop = Property(it.id,
                        it.name,
                        it.value,
                        it.estimatedGrowthPct,
                        12 - cal.get(Calendar.MONTH) - 1,
                        it.distributionOrder,
                        (it as Property).estimatedGrowthPctAsSavings,
                        it.willSellToFinanceRetirement,
                        it.scheduledPaymentName,
                        100.0,
                        0)
                    assetIncomes.add(prop)
                }
                else -> {
                    Log.d("Alex", "What is this??")
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
                it.pensionStartDate)
            pensionIncomes.add(PensionIncome(pension, iRetirementScenario.retirementDate,
                iRetirementScenario.inflationRate, iRetirementScenario.cppAge,
                iRetirementScenario.birthDate))
        }
    }

    fun logRow() {
        val cal = android.icu.util.Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val multiplier = (1 + inflationRate/100.0).pow(year - currentYear)
        val outputString =  String.format("%4d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d",
            year,
            targetAnnualIncome,
            round(gMinimizeTaxAmount*multiplier).toInt(),
            getTaxableIncome(year),
            getTotalGrossIncome(year),
            getTotalTax(),
            getTotalNetIncome(),
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
        Log.d("Alex", outputString)
//        Log.d("Alex", "$userID $year tgInc ${gDec(targetAnnualIncome)} cpp ${gDec(cppIncome)} oas ${gDec(oasIncome)} sal $salaryIncomes pen $pensionIncomes")
//        assetIncomes.forEach {
//            Log.d("Alex", "${it.name} value ${gDec(it.value)} gpct ${gDec(it.estimatedGrowthPct)} growth ${gDec(it.growthThisYear)} withdrawal ${gDec(it.withdrawalAmount)} end ${gDec(it.getEndingBalance())}")
//        }
//        Log.d("Alex", "GrossInc ${gDec(getTotalGrossIncome())} TaxableInc ${gDec(getTaxableIncome())} Tax ${gDec(getTotalTax())} TotalNetInc ${gDec(getTotalNetIncome())}")
    }

    fun getMaximumTaxableIncome() : Int {
        val cal = android.icu.util.Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val multiplier = (1 + inflationRate/100.0).pow(year - currentYear)
        return round(gMinimizeTaxAmount*multiplier).toInt()
    }

    fun createNextRow(iRetirementScenario: RetirementData)
    : RetirementCalculationRow {
//        val retUser = RetirementViewModel.getUserDefaults(userID) ?: return null
        val nextRow = RetirementCalculationRow(userID, year+1, iRetirementScenario.inflationRate)

        nextRow.parentScenario = iRetirementScenario
        nextRow.targetAnnualIncome = round(targetAnnualIncome * (1 + inflationRate/100.0)).toInt()

        nextRow.cppIncome = iRetirementScenario.cpp.getCPPIncome(iRetirementScenario.cppAge,
            iRetirementScenario.birthDate, nextRow.year)
        nextRow.oasIncome = iRetirementScenario.oas.getOASIncome(iRetirementScenario.birthDate, nextRow.year)

        nextRow.salaryIncomes.add(Income(iRetirementScenario.salary.id, iRetirementScenario.salary.getSalary(iRetirementScenario.retirementDate, nextRow.year)))
//        iRetirementScenario.pensions.forEach {
//            nextRow.pensionIncomes.add(Income(it.id, it.getPensionIncome(nextRow.year)))
//        }

        val retirementYear = iRetirementScenario.retirementDate.substring(0,4).toInt()
        assetIncomes.forEach {
            nextRow.assetIncomes.add(it.getNextYear(nextRow.year > retirementYear))
        }
        pensionIncomes.forEach {
            nextRow.pensionIncomes.add(it.getNextYear())
        }
        return nextRow
    }

    fun getNetWorth(iType: AssetType = AssetType.ALL) : Int {
        var tmp = 0
        assetIncomes.forEach {
            if (iType == AssetType.ALL || it.type == iType)
                tmp += it.getEndingBalance()
        }
        return tmp
    }

    fun getPropertySoldYear() : Int {
        assetIncomes.forEach {
            if (it.type == AssetType.PROPERTY) {
                val prop: Property = it as Property
                return prop.soldInYear
            }
        }
        return 0
    }
    fun getTotalGrossIncome(iYear: Int) : Int {
        var tTotal = cppIncome + oasIncome
        salaryIncomes.forEach {
            tTotal += it.amount
        }
//        pensionIncomes.forEach {
//            tTotal += it.amount
//        }
        assetIncomes.forEach {
            tTotal += (it.withdrawalAmount)
        }
        pensionIncomes.forEach {
            tTotal += (it.getPensionIncome(iYear))
        }
        return tTotal
    }

    fun getTotalTax(): Int {
        val cal = android.icu.util.Calendar.getInstance()
        val yearsInFuture = year - cal.get(Calendar.YEAR)
        return getFederalTax(getTaxableIncome(year), yearsInFuture) +
                getOntarioTax(getTaxableIncome(year), yearsInFuture)
    }

    fun netIncomeGreaterThanTargetIncome(): Boolean {
        return getTotalNetIncome() >= targetAnnualIncome
    }

    fun getTotalNetIncome() : Int {
        val cal = android.icu.util.Calendar.getInstance()
        val yearsInFuture = year - cal.get(Calendar.YEAR)
        val tax = getFederalTax(getTaxableIncome(year), yearsInFuture) +
            getOntarioTax(getTaxableIncome(year), yearsInFuture)
        return getTotalGrossIncome(year) - tax
    }

    fun getTaxableIncome(iYear: Int) : Int {
        var tTotal = cppIncome + oasIncome
        assetIncomes.forEach {
            tTotal += it.getTaxableIncome()
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

    private fun getFederalTax(iTaxableIncome: Int, iYearsInTheFuture: Int): Int {
        var tTax = 0
        val inflationMultiplier = (1 + inflationRate/100.0).pow(iYearsInTheFuture)

        var remainingIncome = iTaxableIncome
        var threshold = round(216511 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .33).toInt()
            remainingIncome = threshold
        }
        threshold = round(151978 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .29).toInt()
            remainingIncome = threshold
        }
        threshold = round(98040 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .26).toInt()
            remainingIncome = threshold
        }
        threshold = round(49020 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .2005).toInt()
            remainingIncome = threshold
        }
        threshold = round(14398 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .15).toInt()
        }
        return tTax
    }

    private fun getOntarioTax(iTaxableIncome: Int, iYearsInTheFuture: Int): Int {
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
        threshold = round(90287 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .1116).toInt()
            remainingIncome = threshold
        }
        threshold = round(45142 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .0915).toInt()
            remainingIncome = threshold
        }
        threshold = round(11141 * inflationMultiplier).toInt()
        if (remainingIncome > threshold) {
            tTax += round((remainingIncome - threshold) * .0505).toInt()
        }
        return tTax
    }

    private fun howMuchGrossDoINeed(iTotalTaxableNetIncomeNeeded: Int, iCurrentGrossIncome: Int, iTryThisAmount: Int, iYearsInTheFuture: Int) : Int {
        val tax = getFederalTax(iTryThisAmount+iCurrentGrossIncome, iYearsInTheFuture) +
                getOntarioTax(iTryThisAmount+iCurrentGrossIncome, iYearsInTheFuture)
        return if (abs(iTotalTaxableNetIncomeNeeded + tax - iTryThisAmount) < 1.0) {
            iTryThisAmount+1 // add a bit for rounding purposes
        } else {
            howMuchGrossDoINeed(iTotalTaxableNetIncomeNeeded, iCurrentGrossIncome,
                iTotalTaxableNetIncomeNeeded + tax, iYearsInTheFuture)
        }

    }

    fun ensureIncomeIsAdequate() {
        if (getTotalNetIncome() >= targetAnnualIncome)
            return

//        Log.d("Alex", "Needs more income ${targetAnnualIncome - getTotalNetIncome()} tot ${getTotalNetIncome()} but tg is $targetAnnualIncome")
        for (i in 0 until assetIncomes.size) {
            assetIncomes.forEach {
                if (it.distributionOrder == i && it.value > 0.0 &&
                    getTotalNetIncome() < targetAnnualIncome) {
                    val netWithdrawalAmountNeeded = min(it.value, targetAnnualIncome - getTotalNetIncome())
                    if (it.generatesTaxableIncome()) { // e.g. RRSP
                        val cal = android.icu.util.Calendar.getInstance()
                        it.withdraw(
                            howMuchGrossDoINeed(netWithdrawalAmountNeeded, getTaxableIncome(year),
                                netWithdrawalAmountNeeded, year - cal.get(Calendar.YEAR)),
                            getTaxableIncome(year), // tell the RRSP class how much taxable income already exists this year
                            year,
                            parentScenario
                        )
                    } else
                        it.withdraw(netWithdrawalAmountNeeded, year)
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
            if (it.type == AssetType.RRSP)
                minRRIFWithdrawal += round((it as RRSP).value * minimumWithdrawalPct).toInt()
        }
        return minRRIFWithdrawal
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
