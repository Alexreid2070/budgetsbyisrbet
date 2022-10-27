package com.isrbet.budgetsbyisrbet

import com.google.api.services.sheets.v4.model.*

const val cRow_SCENARIO_NAME = 1
const val cRow_USER_NAME = 2
const val cRow_BIRTH_DATE = 3
const val cRow_INFLATION_RATE = 4
const val cRow_INVESTMENT_GROWTH = 5
const val cRow_PROPERTY_GROWTH = 6
const val cRow_TARGET_MONTHLY_INCOME = 7
const val cRow_PLAN_TO_AGE = 8
const val cRow_SALARY_AMOUNT = 9
const val cRow_SALARY_GROWTH = 10
const val cRow_RETIREMENT_DATE = 11
const val cRow_CPP_AMOUNT = 12
const val cRow_CPP_RETIRE_AGE = 13
const val cRow_OAS_AMOUNT = 14
const val cRow_Blank = 15
const val cRow_HEADINGS = 16
const val cFIRST_DATA_ROW = 17

fun getCol(iColumnNumber: Int) : String
{
    var columnNumber = iColumnNumber
    var columnName = ""
    val letterA = 'A'

    while (columnNumber > 0) {
        // Find remainder
        var rem = columnNumber % 26
        // If remainder is 0, then a 'Z' must be there in output
        if (rem == 0) {
            columnName += "Z"
            columnNumber = (columnNumber / 26) - 1
        } else { // If remainder is non-zero
            columnName += letterA.plus(rem - 1)
            columnNumber /= 26
        }
    }
    // Reverse the string and print result
    return columnName.reversed()
    }

class SpreadsheetMaker {
    fun create(iSpreadsheetTitle: String,
        iSheetOneTitle: String,
        iDetails: List<RetirementCalculationRow>) : Spreadsheet {

        val spreadsheet = Spreadsheet()
        val detailsSheetMaker = DetailsSheetMaker()
        val sheets = mutableListOf<Sheet>()
        val spreadsheetProperties = SpreadsheetProperties()
        spreadsheetProperties.title = iSpreadsheetTitle
        spreadsheet.properties = spreadsheetProperties
        sheets.add(detailsSheetMaker.create(iSheetOneTitle, iDetails))
        spreadsheet.sheets = sheets
        return spreadsheet
    }
}

private class DetailsSheetMaker {
    fun create(iTitle: String,
        iDetails: List<RetirementCalculationRow>) : Sheet {

        val sheet = Sheet()
        val sheetProperty = SheetProperties()
        sheetProperty.title = iTitle
        sheet.properties = sheetProperty

        val listGridData = mutableListOf<GridData>()
        val listGridDataMaker = GridDataMaker()
        val gridData = listGridDataMaker.create(iDetails, 0, 0)
        listGridData.add(gridData)
        sheet.data = listGridData
        return sheet
    }
}

private class ScenarioHeadingRowDataMaker {
    fun create(iHeading: String, iValue: String) : RowData {

        val rowData = RowData()
        val listCellData : MutableList<CellData> = mutableListOf()
        val cellDataMaker = CellDataMaker()

        listCellData.add(cellDataMaker.create(iHeading, ""))
        listCellData.add(cellDataMaker.create(iValue, ""))
        rowData.setValues(listCellData)
        return rowData
    }
}

private class ColumnHeadingRowDataMaker(val iRow: RetirementCalculationRow?) {
    fun create(): RowData {
        val rowData = RowData()
        val listCellData: MutableList<CellData> = mutableListOf()
        val cellDataMaker = CellDataMaker()

        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.year), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.target_income), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.net_income), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.maximum_taxable_income), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.taxable_income), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.gross_income), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.tax), ""))
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.salary), ""))
        listCellData.add(cellDataMaker.create(String.format(
                    MyApplication.getString(R.string.s_pension),
                    MyApplication.getString(R.string.cpp)
                ), ""))
        iRow?.pensionIncomes?.forEach {
            listCellData.add(
                cellDataMaker.create(String.format(MyApplication.getString(R.string.s_pension), it.name), ""))
        }
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.oas), ""))
        var minAlreadyShown = false
        iRow?.assetIncomes?.forEach {
            when (it.assetType) {
                AssetType.RRSP -> {
                    if (!minAlreadyShown) {
                        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.minimum_rrif_withdrawal), ""))
                        minAlreadyShown = true
                    }
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_withdrawal),
                        MyApplication.getString(R.string.rrsp) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_growth),
                        it.getGrowthPct(),
                        MyApplication.getString(R.string.rrsp) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_balance),
                        MyApplication.getString(R.string.rrsp) + ": " + it.name), ""))
                }
                AssetType.LIRA_LIF, AssetType.LIRA_Annuity -> {
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_withdrawal),
                        MyApplication.getString(R.string.lira) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_growth),
                        it.getGrowthPct(),
                        MyApplication.getString(R.string.lira) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_balance),
                        MyApplication.getString(R.string.lira) + ": " + it.name), ""))
                }
                AssetType.TFSA -> {
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_withdrawal),
                        MyApplication.getString(R.string.tfsa) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_growth),
                        it.getGrowthPct(),
                        MyApplication.getString(R.string.tfsa) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_balance),
                        MyApplication.getString(R.string.tfsa) + ": " + it.name), ""))
                }
                AssetType.SAVINGS -> {
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_withdrawal),
                        MyApplication.getString(R.string.savings) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_growth),
                        it.getGrowthPct(),
                        MyApplication.getString(R.string.savings) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_balance),
                        MyApplication.getString(R.string.savings) + ": " + it.name), ""))
                }
                AssetType.PROPERTY -> {
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_withdrawal),
                        MyApplication.getString(R.string.property) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_growth),
                        it.getGrowthPct(),
                        MyApplication.getString(R.string.property) + ": " + it.name), ""))
                    listCellData.add(cellDataMaker.create(String.format(MyApplication.getString(R.string.s_balance),
                        MyApplication.getString(R.string.property) + ": " + it.name), ""))
                }
                else -> {}
            }
        }
        listCellData.add(cellDataMaker.create(MyApplication.getString(R.string.net_worth), ""))
        rowData.setValues(listCellData)
        return rowData
    }
}
private class GridDataMaker {
    fun create(iDetails: List<RetirementCalculationRow>,
        startRow: Int,
        startColumn: Int) : GridData {

        val gridData = GridData()
        val listRowData = mutableListOf<RowData>()
        val rowDataMaker = RowDataMaker()
        gridData.startRow = startRow
        gridData.startColumn = startColumn

        val headingRowDataMaker = ScenarioHeadingRowDataMaker()
        if (gRetirementScenario != null) {
            listRowData.add(headingRowDataMaker.create("Scenario Name",
                gRetirementScenario!!.name))
            listRowData.add(headingRowDataMaker.create("User Name",
                SpenderViewModel.getSpenderName(gRetirementScenario!!.userID)))
            listRowData.add(headingRowDataMaker.create("Birth Date",
                gRetirementScenario!!.birthDate))
            listRowData.add(headingRowDataMaker.create("Inflation Rate",
                (gRetirementScenario!!.inflationRate/100.0).toString()))
            listRowData.add(headingRowDataMaker.create("Investment Growth",
                (gRetirementScenario!!.investmentGrowthRate/100.0).toString()))
            listRowData.add(headingRowDataMaker.create("Property Growth",
                (gRetirementScenario!!.propertyGrowthRate/100.0).toString()))
            listRowData.add(headingRowDataMaker.create("Target Monthly Income",
                gRetirementScenario!!.targetMonthlyIncome.toString()))
            listRowData.add(headingRowDataMaker.create("Plan to Age",
                gRetirementScenario!!.planToAge.toString()))
            listRowData.add(headingRowDataMaker.create("Salary Amount",
                gRetirementScenario!!.salary.annualValueAfterTax.toString()))
            listRowData.add(headingRowDataMaker.create("Salary Growth",
                (gRetirementScenario!!.salary.estimatedGrowthPct/100.0).toString()))
            listRowData.add(headingRowDataMaker.create("Retirement Date",
                gRetirementScenario!!.retirementDate))
            val cppAmount = when (gRetirementScenario!!.cppAge) {
                60 -> gRetirementScenario!!.cpp.annualValueAt60
                65 -> gRetirementScenario!!.cpp.annualValueAt65
                70 -> gRetirementScenario!!.cpp.annualValueAt70
                else -> {0}
            }
            listRowData.add(headingRowDataMaker.create("CPP Amount",cppAmount.toString()))
            listRowData.add(headingRowDataMaker.create("CPP Start Age",
                gRetirementScenario!!.cppAge.toString()))
            listRowData.add(headingRowDataMaker.create("OAS Amount",
                gRetirementScenario!!.oas.currentAnnualValue.toString()))
            listRowData.add(headingRowDataMaker.create("", "")) // row 15
        }
        val columnHeadingRowDataMaker = if (iDetails.isNotEmpty()) // row 16
            ColumnHeadingRowDataMaker(iDetails[0])
        else
            ColumnHeadingRowDataMaker(null)
        listRowData.add(columnHeadingRowDataMaker.create())


        val firstYear = if (iDetails.isNotEmpty()) iDetails[0].year else 0
        val dListRowData = mutableListOf<RowData>()
        iDetails.mapTo(dListRowData) { rowDataMaker.create(it, firstYear)}
        dListRowData.forEach {
            listRowData.add(it)
        }
        gridData.rowData = listRowData
        return gridData
    }
}

private class RowDataMaker {
    fun create(iRow: RetirementCalculationRow, iFirstYear: Int) : RowData {
//        val cellInPrevRow = "=offset(indirect(address(row(),column())),-1,0)"
        val prevRowNum = iRow.year - iFirstYear + cFIRST_DATA_ROW - 1
        val currRowNum = iRow.year - iFirstYear + cFIRST_DATA_ROW
        val inflationRateAddress = "B$cRow_INFLATION_RATE"
        val salaryGrowthAddress = "B$cRow_SALARY_GROWTH"
        val investmentGrowthAddress = "B$cRow_INVESTMENT_GROWTH"
        val propertyGrowthAddress = "B$cRow_PROPERTY_GROWTH"
        var taxableIncomeFormula = ""
        var grossIncomeFormula = ""
        var netWorthFormula = ""
        var retirementYear = 0
        var retirementMonth = 0
        var birthYear = 0
        var birthMonth = 0
        var cppYear = 0
        var oasYear = 0
        if (gRetirementScenario != null) {
            retirementYear = gRetirementScenario?.retirementDate?.substring(0, 4)?.toInt()!!
            retirementMonth = gRetirementScenario?.retirementDate?.substring(5,7)?.toInt()!!
            birthYear = gRetirementScenario?.birthDate?.substring(0, 4)?.toInt()!!
            birthMonth = gRetirementScenario?.birthDate?.substring(5,7)?.toInt()!!
            cppYear = gRetirementScenario?.cppAge!! + birthYear
            oasYear = birthYear + 67
        }

        val rowData = RowData()
        val listCellData : MutableList<CellData> = mutableListOf()
        val cellDataMaker = CellDataMaker()

        var minAlreadyShown = false
        // column A Year
        listCellData.add(cellDataMaker.create(iRow.year.toString(), ""))
        // column B Target Monthly Income
        if (iRow.year == iFirstYear)
            listCellData.add(cellDataMaker.create("=B" + cRow_TARGET_MONTHLY_INCOME + "*12", ""))
        else
            listCellData.add(cellDataMaker.create("=round(B" + prevRowNum + "*(1+" + inflationRateAddress +"))", ""))
        // column C Net Income
        listCellData.add(cellDataMaker.create("=F" + currRowNum + "-G" + currRowNum, ""))
        // column D Maximum Taxable Income
        if (iRow.year == iFirstYear)
            listCellData.add(cellDataMaker.create(iRow.getMaximumTaxableIncome(), "$46226 is the amount in 2022 that stays within 15% tax bracket, thereafter adjusted for inflation"))
        else
            listCellData.add(cellDataMaker.create("=round(D" + prevRowNum + "*(1+" + inflationRateAddress +"))", ""))
        // column E Taxable Income
        if (iRow.year == iFirstYear) // taxable income
            listCellData.add(cellDataMaker.create(iRow.getMaximumTaxableIncome().toString(), ""))
        else
            listCellData.add(cellDataMaker.create("=round(B" + prevRowNum + "*(1+" + inflationRateAddress +"))", ""))
        // column F Gross Income
        if (iRow.year == iFirstYear) // gross income
            listCellData.add(cellDataMaker.create(iRow.getMaximumTaxableIncome().toString(), ""))
        else
            listCellData.add(cellDataMaker.create("=round(B" + prevRowNum + "*(1+" + inflationRateAddress +"))", ""))
        listCellData.add(cellDataMaker.create(iRow.getTotalTax(), "")) // column G Tax
        // column H Salary
        if (iRow.year < retirementYear) {
            listCellData.add(cellDataMaker.create("=round(B" + cRow_SALARY_AMOUNT +
                "*pow((1+" + salaryGrowthAddress + ")," + iRow.year + "-" + iFirstYear + "))", "adjusted for salary growth"))
        } else if (iRow.year == retirementYear) {
            listCellData.add(cellDataMaker.create("=round((B" + cRow_SALARY_AMOUNT +
                "*" + retirementMonth + "/12" +
                ")*pow((1+" + salaryGrowthAddress + ")," + iRow.year + "-" + iFirstYear + "))", "adjusted for partially worked year, and for salary growth"))
        } else {
            listCellData.add(cellDataMaker.create(0, ""))
        }
        grossIncomeFormula = "=" + getCol(listCellData.size) + currRowNum
        // column I CPP
        if (iRow.year < cppYear) {
            listCellData.add(cellDataMaker.create(0, ""))
        } else if (iRow.year == cppYear) {
            listCellData.add(cellDataMaker.create("=round(B" + cRow_CPP_AMOUNT +
                "*" + (12-birthMonth+1) + "/12)", "starts at age " + gRetirementScenario?.cppAge + ", adjusted for partial year"))
        } else {
            listCellData.add(cellDataMaker.create("=round(B" + cRow_CPP_AMOUNT +
                "*pow((1+" + inflationRateAddress + ")," + iRow.year + "-" + cppYear + "))", "adjusted for inflation"))
        }
        taxableIncomeFormula += "=" + getCol(listCellData.size) + currRowNum
        grossIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
        // each pension income
        iRow.pensionIncomes.forEach {
            listCellData.add(cellDataMaker.create(it.getPensionIncome(iRow.year).toString(), ""))
            taxableIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
            grossIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
        }
        // OAS
        if (iRow.year < oasYear) {
            listCellData.add(cellDataMaker.create(0, ""))
        } else if (iRow.year == oasYear) {
            listCellData.add(cellDataMaker.create("=round((B" + cRow_OAS_AMOUNT +
                "*" + (12-birthMonth+1) + "/12" +
                ")*pow((1+" + inflationRateAddress + ")," + iRow.year + "-" + iFirstYear + "))", "starts at age 67, adjusted for partial year, plus inflation"))
        } else {
            listCellData.add(cellDataMaker.create("=round(B" + cRow_OAS_AMOUNT +
                "*pow((1+" + inflationRateAddress + ")," + iRow.year + "-" + iFirstYear + "))", "adjusted for inflation"))
        }
        taxableIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
        grossIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
        iRow.assetIncomes.forEach {
            if (it.assetType == AssetType.RRSP) {
                if (!minAlreadyShown) {
                    // minimum rrif withdrawal
                    val pct = iRow.getMinimumRRIFWithdrawalPercentage()
                    if (iRow.year == iFirstYear) // taxable income
                        listCellData.add(cellDataMaker.create("=round(" + pct + "*" + it.getValue()+")", ""))
                    else
                        listCellData.add(cellDataMaker.create("=round(" + pct + "*" + getCol(listCellData.size+4) + prevRowNum + ")", ""))
                    minAlreadyShown = true
                }
            }
            listCellData.add(cellDataMaker.create(it.withdrawalAmount, ""))
            if (it.assetType == AssetType.RRSP ||
                    it.assetType == AssetType.LIRA_LIF ||
                    it.assetType == AssetType.LIRA_Annuity) {
                taxableIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
            }
            grossIncomeFormula += "+" + getCol(listCellData.size) + currRowNum

            var formula: String
            if (it.growthThisYear == 0) {
                formula = if (it.additionalGrowthThisYear == 0)
                    "0"
                else
                    "=" + it.additionalGrowthThisYear
            } else {
                if (it.assetType == AssetType.LIRA_Annuity) { // don't subtract withdrawal
                    formula = if (iRow.year == iFirstYear)
                        "=round(" + it.getValue()
                    else
                        "=round(" + getCol(listCellData.size+2) + prevRowNum

                } else {
                    formula = if (iRow.year == iFirstYear)
                        "=round((" + it.getValue() + "-" +  getCol(listCellData.size) + currRowNum + ") "
                    else
                        "=round((" + getCol(listCellData.size+2) + prevRowNum  + "-" + getCol(listCellData.size) + currRowNum + ") "
                }
                if (it.monthsOfGrowthThisYear == 12)
                    formula += ("* " + it.getGrowthPct()/100)
                else
                    formula += ("* (" + it.getGrowthPct()/100 + "*" + it.monthsOfGrowthThisYear + "/12)")
                formula += ")"
                if (it.annualContribution > 0)
                    formula += (" +" + it.annualContribution)
                if (it.additionalGrowthThisYear != 0)
                    formula += (" +" + it.additionalGrowthThisYear)
            }
            if (formula == "0")
                listCellData.add(cellDataMaker.create(0, ""))
            else
                listCellData.add(cellDataMaker.create(formula, ""))
            if (it.assetType == AssetType.SAVINGS && it.growthIsTaxable()) {
                taxableIncomeFormula += "+" + getCol(listCellData.size) + currRowNum
            }
            if (it.assetType == AssetType.LIRA_Annuity) { // don't subtract withdrawal
                formula = if (iRow.year == iFirstYear)
                    "=" + it.getValue() + "+" + getCol(listCellData.size) + currRowNum
                else
                    "=" + getCol(listCellData.size+1) + prevRowNum + "+" + getCol(listCellData.size) + currRowNum
            } else {
                formula = if (iRow.year == iFirstYear)
                    "=" + it.getValue() + "-" + getCol(listCellData.size-1) + currRowNum + "+" + getCol(listCellData.size) + currRowNum
                else
                    "=" + getCol(listCellData.size+1) + prevRowNum + "-" + getCol(listCellData.size-1) + currRowNum + "+" + getCol(listCellData.size) + currRowNum
            }
            listCellData.add(cellDataMaker.create(formula, ""))
            if (it.assetType == AssetType.PROPERTY && !(it as Property).primaryResidence && it.soldInYear == iRow.year) {
                taxableIncomeFormula += "+.25*" + getCol(listCellData.size) + prevRowNum
            }
            if (netWorthFormula == "")
                netWorthFormula += "=" + getCol(listCellData.size) + currRowNum
            else
                netWorthFormula += "+" + getCol(listCellData.size) + currRowNum
        }
        listCellData.add(cellDataMaker.create(netWorthFormula, ""))
        listCellData[4] = cellDataMaker.create(taxableIncomeFormula, "")
        listCellData[5] = cellDataMaker.create(grossIncomeFormula, "")

        rowData.setValues(listCellData)
        return rowData
    }
}

private class CellDataMaker {
    fun create(iData: Int, iNote: String) : CellData {
        val cellData = CellData()
        val extendedValue = ExtendedValue()
        cellData.userEnteredValue = extendedValue.setNumberValue(iData.toDouble())
        if (iNote != "")
            cellData.note = iNote
        return cellData
    }
    fun create(iData: String, iNote: String) : CellData {
        val cellData = CellData()
        val extendedValue = ExtendedValue()
        if (iData.isNotEmpty()) {
            if (iData.substring(0, 1) == "=")
                cellData.userEnteredValue = extendedValue.setFormulaValue(iData)
            else
                cellData.userEnteredValue = extendedValue.setStringValue(iData)
        }
        if (iNote != "")
            cellData.note = iNote
        return cellData
    }
}