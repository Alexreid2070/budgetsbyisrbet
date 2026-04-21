package com.isrbet.budgetsbyisrbet

import android.accounts.Account
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.*
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.icu.text.NumberFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.LocaleList
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.ArrayMap
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.Firebase
import timber.log.Timber
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.math.*

const val cMODE_VIEW = 0
const val cMODE_EDIT = 1
const val cMODE_ADD = 2

const val cTRUE = "true"
const val cFALSE = "false"
const val cDiscTypeDiscretionary = "Discretionary"
const val cDiscTypeNondiscretionary = "Non-Discretionary"
const val cDiscTypeAll = "All"

const val cPeriodDay = "Day"
const val cPeriodWeek = "Week"
const val cPeriodMonth = "Month"
const val cPeriodQuarter = "Quarter"
const val cPeriodYear = "Year"
const val cPeriodYTD = "YTD"
const val cPeriodAllTime = "All-Time"

const val gMaxNumbersBeforeDecimalPlace = 5
const val gMaxNumbersAfterDecimalPlace = 2

fun getTranslationForPeriod(iRegularity: Int, iPeriod: String) : String {
    val suffix = if (iRegularity == 1)
        ""
    else
        "s"
    return when (iPeriod) {
        cPeriodWeek -> MyApplication.getString(R.string.week) + suffix
        cPeriodMonth -> MyApplication.getString(R.string.month) + suffix
        cPeriodQuarter -> MyApplication.getString(R.string.quarter) + suffix
        cPeriodYear -> MyApplication.getString(R.string.year) + suffix
        cPeriodYTD -> MyApplication.getString(R.string.ytd) + suffix
        cPeriodAllTime -> MyApplication.getString(R.string.all_time) + suffix
        else -> ""
    }
}

const val cBUDGET_RECURRING = 0
const val cBUDGET_JUST_THIS_MONTH = 1
const val cFAKING_TD = false
const val cTRANSFER_CODE = -99
const val cLAST_ROW = 9999999
const val cBudgetDateView = "Date"
const val cBudgetCategoryView = "Category"
const val cOpacity = "1F"

const val cHINT_BUDGET = "Budget"
const val cHINT_DASHBOARD = "Dashboard"
const val cHINT_LOAN = "Loan"
const val cHINT_TRANSACTION = "Transaction"
const val cHINT_PREFERENCES = "Preferences"
const val cHINT_TRACKER = "Tracker"
const val cHINT_ACCOUNTING = "Accounting"
const val cHINT_CATEGORY = "Category"
const val cHINT_HOME = "Home"
const val cHINT_SCHEDULED_PAYMENT = "ScheduledPayment"
const val cHINT_TRANSACTION_VIEW_ALL = "TransactionViewAll"

const val cTRANSACTION_TYPE_EXPENSE = "Expense"
const val cTRANSACTION_TYPE_CREDIT = "Credit"
const val cTRANSACTION_TYPE_SCHEDULED = "Recurring" // uses Recurring for historical db reasons
const val cTRANSACTION_TYPE_TRANSFER = "Transfer"
const val cTRANSACTION_TYPE_INSURANCE_REIMBURSEMENT = "Insurance"

const val cNEXT_YEAR = 0
const val cPREV_YEAR = 1
const val cNEXT_MONTH = 2
const val cPREV_MONTH = 3

enum class RetirementScenarioType(val code: Int) {
    SCENARIO(0),
    DEFAULTS(1)
}

val gDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
var gHomePageExpansionAreaExpanded = false
var gRetirementWorking:RetirementData? = null
var gRetirementDefaults:RetirementData? = null
var gRetirementDetailsList: MutableList<RetirementCalculationRow> = arrayListOf()
var gCurrentDate: MyDate = MyDate() // ie current date
var gActualRow: YOYTableRow? = null // these 4 are hacks to support the YOYDialog
var gBudgetRow: YOYTableRow? = null
var gAverageRow: YOYTableRow? = null
var gDeltaRow: YOYTableRow? = null
var gDidSecondPersonEverExist:Boolean = false
var gVersionName = ""
var gVersionCode = ""

fun gMonthName(iMonth: Int) : String {
    val month = Month.of(iMonth)
    return month.getDisplayName(TextStyle.FULL, Locale.getDefault())
}
fun gShortMonthName(iMonth: Int) : String {
    val month = Month.of(iMonth)
    return month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

 fun gDecM(iDouble: Double): String {
    return DecimalFormat("###0.00;-###0.00").format(iDouble)
}
fun gDec(iInt: Int): String {
    return DecimalFormat("####;(####)").format(iInt)
}
fun gDec(iDouble: Double): String {
    return DecimalFormat("###0.00;(###0.00)").format(iDouble)
}
fun gDecRound(iDouble: Double): String {
    return DecimalFormat("###0;(###0)").format(iDouble)
}

fun gDecWithCurrency(iDouble: Double, iRound: Boolean = false) : String{
    val s = getLocalCurrencySymbol()
    return if (iRound) {
        if (s == "")
            gDecRound(round(iDouble))
        else
            s + " " + gDecRound(round(iDouble))
    } else {
        if (s == "")
            gDec(iDouble)
        else
            s + " " + gDec(iDouble)
    }
}
fun gDecWithCurrency(iInt: Int) : String{
    val s = getLocalCurrencySymbol()
    return if (s == "")
        gDec(iInt)
    else
        s + " " + gDec(iInt)
}
enum class LoanPaymentRegularity(val code: Int) {
    WEEKLY(1),
    BIWEEKLY(2),
    MONTHLY(3);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
}
enum class DateRangeEnum(val code: Int) {
    MONTH(1),
    YTD(2),
    YEAR(3),
    ALLTIME(4)
}

fun underlined(iString: String) : SpannableString {
    val content = SpannableString(iString)
    content.setSpan(UnderlineSpan(), 0, iString.length, 0)
    return content
}

class MyApplication : Application() {
    companion object {
        lateinit var mContext: Context
        lateinit var resources: Resources
        lateinit var database: FirebaseDatabase
        lateinit var databaseref: DatabaseReference

        var transactionSearchText: String = ""
        var transactionFirstInList: Int = cLAST_ROW
        var userUID: String = ""
        var userIndex: Int = 0
        var originalUserUID: String = ""
        var userEmail: String = ""
        var userGivenName: String = ""
        var userPhotoURL: String = ""
        var userAccount: Account? = null
        var currentUserEmail: String = ""
        var adminMode: Boolean = false
        var haveLoadedDataForThisUser = false
        lateinit var myMainActivity: MainActivity
        lateinit var prefs: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor

        fun getString(iParam: Int): String {
            return mContext.resources.getString(iParam)
        }

        fun displayToast(iMessage: String) {
            Toast.makeText(myMainActivity, iMessage, Toast.LENGTH_SHORT).show()
        }
        fun playSound(context: Context, iSound: Int ) {
            if (!DefaultsViewModel.getDefaultSound())
                return

            val mediaPlayer = MediaPlayer.create(context, iSound)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }

        fun releaseResources() {
            transactionFirstInList = cLAST_ROW
            haveLoadedDataForThisUser = false
            transactionSearchText = ""
//            CustomNotificationListenerService.releaseResources()
        }

        fun amCurrentlyImpersonating() : Boolean {
            return (userEmail != currentUserEmail)
        }
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
        FirebaseApp.initializeApp(this)
        Firebase.database.setPersistenceEnabled(true)
        database = FirebaseDatabase.getInstance()
        databaseref = database.reference

        prefs = applicationContext.getSharedPreferences("Prefs", 0)
        prefEditor = prefs.edit()
        LangUtils.init(this)
        Timber.plant(Timber.DebugTree())
    }
}

data class DataObject(var id: Int, var label: String, var value: Double, var priority: String, var color: Int)

data class MyDate(var representsYear: Boolean = false) {
    lateinit var theDate: LocalDate
    constructor() : this (false) {
        theDate = LocalDate.now()
    }
    constructor(iDate: String) : this (false) {
        if (iDate == "") {
            theDate = LocalDate.now()
        } else {
            // might get "2022-02-23", or might get "2022-2-23" or might get "2022-01"
            val year = iDate.substring(0, 4).toInt()
            var month = 1
            var day = 1
            val dash1 = iDate.indexOf("-")
            val dash2 = iDate.indexOf("-", dash1 + 1)
            if (dash1 != -1) {
                month = if (dash2 != -1)
                    iDate.substring(dash1 + 1, dash2).toInt()
                else
                    iDate.substring(dash1 + 1, iDate.length).toInt()
                day = if (dash2 != -1)
                    iDate.substring(dash2 + 1, iDate.length).toInt()
                else
                    1
            }
            if (month == 0)
                month = 1
            theDate = LocalDate.of(year, month, day)
        }
    }
    constructor(iMyDate: MyDate) : this (false) {
        theDate = LocalDate.of(iMyDate.theDate.year, iMyDate.theDate.month, iMyDate.theDate.dayOfMonth)
    }
    constructor(iYear: Int, iMonth: Int, iDay: Int) : this (false) {
        theDate = LocalDate.of(iYear, iMonth, iDay)
    }
    constructor(iYear: Int) : this(true) {
        theDate = LocalDate.of(iYear, 1, 1)
    }
    constructor(iLocalDate: LocalDate) : this(iLocalDate.year, iLocalDate.monthValue, iLocalDate.dayOfMonth)
    companion object {
        fun now() : MyDate {
            return MyDate(LocalDate.now())
        }
    }

    override fun toString(): String {
        return "%04d-%02d-%02d".format(theDate.year, theDate.monthValue, theDate.dayOfMonth)
    }
    operator fun compareTo(iDate: MyDate): Int {
        return if (this.toString() == iDate.toString())
            0
        else if (this.toString() < iDate.toString())
            -1
        else
            1
    }
    override fun equals(other: Any?) : Boolean {
        if (this === other) return true
        if (other !is MyDate) return false

        return this.toString() == other.toString()
    }
    fun setDay(iNewDay: Int) {
        theDate = LocalDate.of(theDate.year, theDate.month, iNewDay)
    }
    fun setMonth(iNewMonth: Int) {
        theDate = LocalDate.of(theDate.year, iNewMonth, theDate.dayOfMonth)
    }
    fun getYear(): Int {
        return theDate.year
    }
    fun getMonth(): Int {
        return theDate.monthValue
    }
    fun getMonthName(): String {
        return gMonthName(getMonth())
    }
    fun getDay(): Int {
        return theDate.dayOfMonth
    }
    fun getDayOfWeek(): DayOfWeek {
        return theDate.dayOfWeek
    }
    fun getYYYYMM(): String {
        return "%04d-%02d".format(theDate.year, theDate.monthValue)
    }
    fun getMMMYY(): String {
        return "${getMonthName().substring(0,3)} ${getYear()}"
    }
    fun getMMMDD(): String {
        return "${gShortMonthName(theDate.monthValue)} %d".format(theDate.dayOfMonth)
    }
    fun getMMMYYYY(): String {
        return "${getMonthName()} ${getYear()}"
    }
    fun getFirstOfMonth() : MyDate {
        return MyDate(theDate.year, theDate.monthValue, 1)
    }

    fun getLastDayOfMonth() : MyDate {
        return MyDate(theDate.year, theDate.monthValue, theDate.lengthOfMonth())
    }
    fun increment(iPeriod: String, iRegularity: Int) : MyDate {
        theDate =
            when (iPeriod) {
            cPeriodDay -> theDate.plusDays(iRegularity.toLong())
            cPeriodWeek -> theDate.plusWeeks(iRegularity.toLong())
            cPeriodMonth -> theDate.plusMonths(iRegularity.toLong())
            cPeriodYear -> theDate.plusYears(iRegularity.toLong())
            else -> theDate
        }
        return  this
    }
}

fun hideKeyboard(context: Context, view: View) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}
/*
fun hideKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = activity.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}
*/

fun perfectDecimal(iStr: String, MAX_BEFORE_POINT: Int, MAX_DECIMAL: Int): String {
    var str = iStr
    if (str[0] == gDecimalSeparator) str = "0$str"
    val max = str.length
    var rFinal = ""
    var after = false
    var i = 0
    var up = 0
    var decimal = 0
    var t: Char
    while (i < max) {
        t = str[i]
        if (t != gDecimalSeparator && !after) {
            up++
            if (up > MAX_BEFORE_POINT) return rFinal
        } else if (t == gDecimalSeparator) {
            after = true
        } else {
            decimal++
            if (decimal > MAX_DECIMAL) return rFinal
        }
        rFinal += t
        i++
    }
    return rFinal
}

fun showErrorMessage(iFragmentManager: FragmentManager, iMessage: String) {
    val newFragment = TransactionDialogFragment(iMessage)
    newFragment.show(iFragmentManager, "Error")
}

fun focusAndOpenSoftKeyboard(context: Context, view: View) {
    view.requestFocus()
    // open the soft keyboard
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
//    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0) don't use this it causes the app to crash if you type random chars in the Search box
}

/* object InternetConnection {
    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    fun checkConnection(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connMgr.activeNetworkInfo
        if (activeNetworkInfo != null) { // connected to the internet
            // connected to the mobile provider's data plan
            return if (activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                true
            } else activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
        return false
    }
}*/

fun getDaysInMonth(cal: MyDate): Int {
    return getDaysInMonth(cal.getYear(), cal.getMonth())
}
/* fun getDaysInMonth(bm: BudgetMonth): Int {
    return getDaysInMonth(bm.year, bm.month)
} */
fun getDaysInMonth(year: Int, month: Int): Int { // month is 1..12
    return if (month == 4 || month == 6 || month == 9 || month == 11) {
        30
    } else if (month == 2) {
        if (year % 4 == 0)
            29
        else
            28
    }
    else
        31
}

open class OnSwipeTouchListener(ctx: Context) : OnTouchListener {
    private val gestureDetector: GestureDetector
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            if (e1 == null)
                return result
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            return result
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeTop() {}
    open fun onSwipeBottom() {}
}

fun inDarkMode(context: Context): Boolean {
    when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_YES -> { return true }
        Configuration.UI_MODE_NIGHT_NO -> { return false }
        Configuration.UI_MODE_NIGHT_UNDEFINED -> { return false }
    }
    return false
}

fun String.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun getBudgetColour(context: Context, iActual: Double, iBudget: Double, iAlwaysShowGreen: Boolean): Int {
    val rActual = round(iActual*100)
    val rBudget = round(iBudget*100)
    if (rActual <= rBudget) {
        val colorToReturn: Int = if (iAlwaysShowGreen) {
            if (inDarkMode(context))
                ContextCompat.getColor(context, R.color.darkGreen)
            else
                ContextCompat.getColor(context, R.color.green)
        } else
            MaterialColors.getColor(context, R.attr.background, Color.BLACK)
        return colorToReturn
    } else if ((rActual > rBudget * (1.0 + (DefaultsViewModel.getDefaultShowRed()/100.0))) ||
        (rBudget == 0.0 && rActual > 0.0)) {
        return if (inDarkMode(context))
            ContextCompat.getColor(context, R.color.darkRed)
        else
            ContextCompat.getColor(context, R.color.red)
    } else {
        return ContextCompat.getColor(context, R.color.orange)
    }
}

fun getLocalCurrencySymbol(iAlways: Boolean = false) : String {
    return if (DefaultsViewModel.getDefaultShowCurrencySymbol() || iAlways) {
        val locale = Locale.getDefault()
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        if (numberFormat.currency == null)
            ""
        else
            numberFormat.currency?.symbol.toString()
    } else {
        ""
    }
}

fun getNextBusinessDate(iDate: MyDate) : MyDate {
    val tDate = MyDate(iDate)
    if(iDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
        tDate.increment(cPeriodDay, 2)
    } else if(iDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
        tDate.increment(cPeriodDay, 1)
    }

    return tDate
}

fun textIsAlphaOrSpace(string: String): Boolean {
    return string.filter { it in 'A'..'Z' || it in 'a'..'z' || it == ' ' }.length == string.length
}

fun textIsSafeForKey(iText: String) : Boolean {
    return when {
        iText.contains(".") -> false
        iText.contains("#") -> false
        iText.contains("/") -> false
        iText.contains("\\") -> false
        iText.contains("[") -> false
        iText.contains("]") -> false
        iText.contains("+") -> false
        iText.contains("$") -> false
        else -> !iText.contains("%")
    }
}

fun textIsSafeForValue(iText: String) : Boolean {
    return !iText.contains("\\")
}

/* fun isNumber(s: String?): Boolean {
    return if (s.isNullOrEmpty()) false else s.all { Character.isDigit(it) }
} */

fun String.setFontSizeForPath(ind: Int, fontSizeInPixel: Int, colorCode: Int = Color.BLACK): SpannableString {
    val spannable = SpannableString(this)
    val startIndexOfPath = if (ind < spannable.length) ind else 0
    spannable.setSpan(
        AbsoluteSizeSpan(fontSizeInPixel),
        startIndexOfPath,
        spannable.length,
        0
    )
    spannable.setSpan(
        ForegroundColorSpan(colorCode),
        startIndexOfPath,
        spannable.length,
        0
    )

    return spannable
}

fun getColorInHex(iColor: Int, iOpacity: String): String {
    return java.lang.String.format("#%s%06X", iOpacity, 0xFFFFFF and iColor)
}

fun iAmPrimaryUser(): Boolean {
    return (MyApplication.adminMode || MyApplication.userUID == MyApplication.originalUserUID)
}

fun thisIsANewUser(): Boolean {
    // this function is only valid once all Data models have been loaded.
    return MyApplication.userUID != "" &&
            CategoryViewModel.isLoaded() && CategoryViewModel.getCount() == 0 &&
            SpenderViewModel.isLoaded() && SpenderViewModel.getTotalCount() == 0 &&
            TransactionViewModel.isLoaded() && TransactionViewModel.getCount() == 0 &&
            BudgetViewModel.isLoaded() && BudgetViewModel.getCount() == 0 &&
            ScheduledPaymentViewModel.isLoaded() && ScheduledPaymentViewModel.getCount() == 0

}

fun switchTo(iUID: String) {
    MyApplication.userUID=iUID
    SpenderViewModel.refresh()
    DefaultsViewModel.refresh()
    TransactionViewModel.refresh()
    CategoryViewModel.refresh()
    BudgetViewModel.refresh()
    ScheduledPaymentViewModel.refresh()
}

/*
fun getDoubleValue(iNumberToParse: String): Double {
    var numberToParse = iNumberToParse
    return if (numberToParse.contains("(")) {
        numberToParse = numberToParse.replace("[(),]".toRegex(), "")
        numberToParse.toDouble() * -1
    } else {
        numberToParse = numberToParse.replace("[,]".toRegex(), "")
        numberToParse.toDouble()
    }
} */

fun Context.copyToClipboard(clipLabel: String, text: CharSequence){
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(clipLabel, text))
}

class MovableFloatingActionButton : FloatingActionButton, OnTouchListener {
    private var downRawX = 0f
    private var downRawY = 0f
    private var dX = 0f
    private var dY = 0f

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init() {
        setOnTouchListener(this)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val layoutParams = view.layoutParams as MarginLayoutParams
        val action = motionEvent.action
        return if (action == MotionEvent.ACTION_DOWN) {
            downRawX = motionEvent.rawX
            downRawY = motionEvent.rawY
            dX = view.x - downRawX
            dY = view.y - downRawY
            true // Consumed
        } else if (action == MotionEvent.ACTION_MOVE) {
            val viewWidth = view.width
            val viewHeight = view.height
            val viewParent = view.parent as View
            val parentWidth = viewParent.width
            val parentHeight = viewParent.height
            var newX = motionEvent.rawX + dX
            newX = max(
                layoutParams.leftMargin.toFloat(),
                newX
            ) // Don't allow the FAB past the left hand side of the parent
            newX = min(
                (parentWidth - viewWidth - layoutParams.rightMargin).toFloat(),
                newX
            ) // Don't allow the FAB past the right hand side of the parent
            var newY = motionEvent.rawY + dY
            newY = max(
                layoutParams.topMargin.toFloat(),
                newY
            ) // Don't allow the FAB past the top of the parent
            newY = min(
                (parentHeight - viewHeight - layoutParams.bottomMargin).toFloat(),
                newY
            ) // Don't allow the FAB past the bottom of the parent
            view.animate()
                .x(newX)
                .y(newY)
                .setDuration(0)
                .start()
            true // Consumed
        } else if (action == MotionEvent.ACTION_UP) {
            val upRawX = motionEvent.rawX
            val upRawY = motionEvent.rawY
            val upDX = upRawX - downRawX
            val upDY = upRawY - downRawY
            if (abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE) { // A click
                performClick()
            } else { // A drag
                true // Consumed
            }
        } else {
            super.onTouchEvent(motionEvent)
        }
    }

    companion object {
        private const val CLICK_DRAG_TOLERANCE =
            10f // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.
    }
}

fun getSplitText (iSplit1: Int, iAmount: String): String {
    val split2 = 100 - iSplit1

    val amount = when (iAmount) {
        "" -> 0.0
        "-" -> 0.0
        else -> {
            val lNumberFormat: NumberFormat = NumberFormat.getInstance()
            lNumberFormat.parse(iAmount).toDouble()
        }
    }
    val amount1 = round(amount * iSplit1) / 100.0
    val amount2 = round(amount * split2) / 100.0

    if (amount == 0.0) {
        return if (iSplit1 == 0) {
            String.format(MyApplication.getString(R.string.split_is_x_pct_for_name1),
                100,
                SpenderViewModel.getSpenderName(1))
        } else if (split2 == 0) {
            String.format(MyApplication.getString(R.string.split_is_x_pct_for_name1),
                100,
                SpenderViewModel.getSpenderName(0))
        } else {
            String.format(MyApplication.getString(R.string.split_is_x_pct_for_name1_and_z_pct_for_name2),
                iSplit1,
                SpenderViewModel.getSpenderName(0),
                split2,
                SpenderViewModel.getSpenderName(1))
        }
    } else if (iSplit1 == 0) {
        return String.format(MyApplication.getString(R.string.split_is_x_pct_d_for_name1),
            100,
            gDec(amount2),
            SpenderViewModel.getSpenderName(1))
    } else if (split2 == 0) {
        return String.format(MyApplication.getString(R.string.split_is_x_pct_d_for_name1),
            100,
            gDec(amount1),
            SpenderViewModel.getSpenderName(0))
    } else {
        return String.format(MyApplication.getString(R.string.split_is_x_pct_d_for_name1_and_z_pct_d_for_name2),
            iSplit1,
            gDec(amount1),
            SpenderViewModel.getSpenderName(0),
            split2,
            gDec(amount2),
            SpenderViewModel.getSpenderName(1))
    }
}

fun getMyVersion() : Int {
    var appVersion = (gVersionName.toDouble() * 1000.0).roundToInt()
    appVersion += (gVersionCode.toDouble()).roundToInt()
    Timber.tag("Alex").d("gVersionName is '%s' and gVersionCode is '%s'", gVersionName, gVersionCode)
    return appVersion
}

object LangUtils {
    private const val LANG_AUTO = "auto"
    private const val LANG_DEFAULT = "en-US"
    private var sLocaleMap: ArrayMap<String, Locale>? = null
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private val mLastLocales: HashMap<ComponentName, Locale> = HashMap()
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?
            ) {
                mLastLocales[activity.componentName] = applyLocaleToActivity(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                if (mLastLocales[activity.componentName] != getFromPreference(activity)) {
                    Timber.tag("Alex").d("Locale changed in activity %s", activity.componentName)
                    ActivityCompat.recreate(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle
            ) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                mLastLocales.remove(activity.componentName)
            }
        })
        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                applyLocale(application)
            }

            override fun onLowMemory() {}
        })
        applyLocale(application)
    }

    private fun setAppLanguages(context: Context) {
        if (sLocaleMap == null) sLocaleMap = ArrayMap()
        val res = context.resources
        val conf = res.configuration
        // Assume that there is an array called language_key which contains all the supported language tags
        val locales = context.resources.getStringArray(R.array.languages_key)
        val langTag = MyApplication.prefs.getString("lang", null)
        for (locale in locales) {
            if (LANG_AUTO == locale) {
                sLocaleMap!![LANG_AUTO] = null
            } else if (LANG_DEFAULT == langTag) {
                sLocaleMap!![LANG_DEFAULT] = Locale.forLanguageTag(LANG_DEFAULT)
            } else {
                conf.setLocale(Locale.forLanguageTag(locale))
//                val ctx = context.createConfigurationContext(conf)
                sLocaleMap!![locale] = ConfigurationCompat.getLocales(conf)[0]
            }
        }
    }

    private fun getAppLanguages(context: Context): ArrayMap<String, Locale>? {
        if (sLocaleMap == null) setAppLanguages(context)
        return sLocaleMap
    }

    fun getFromPreference(context: Context): Locale {
        getAppLanguages(context)
        val language: String? = MyApplication.prefs.getString("lang", null)
        val locale: Locale? = sLocaleMap?.get(language)
        if (locale != null) {
            return locale
        }
        // Load from system configuration
        val conf = Resources.getSystem().configuration
        return conf.locales[0]
    }

    fun applyLocaleToActivity(activity: Activity): Locale {
        val locale = applyLocale(activity)
        // Update title
        try {
            val info = activity.packageManager.getActivityInfo(activity.componentName, 0)
            if (info.labelRes != 0) {
                activity.setTitle(info.labelRes)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        // Update menu
        activity.invalidateOptionsMenu()
        return locale
    }

    private fun applyLocale(context: Context): Locale {
        return applyLocale(context, getFromPreference(context))
    }

    private fun applyLocale(context: Context, locale: Locale): Locale {
        updateResources(context, locale)
        val appContext = context.applicationContext
        if (appContext !== context) {
            updateResources(appContext, locale)
        }
        return locale
    }

    private fun updateResources(context: Context, locale: Locale) {
        Locale.setDefault(locale)
        val res = context.resources
        var conf = res.configuration
        val current =
            conf.locales[0]
        if (current === locale) {
            return
        }
        conf = Configuration(conf)
        setLocaleApi24(conf, locale)
        res.updateConfiguration(conf, res.displayMetrics)
    }

    private fun setLocaleApi24(config: Configuration, locale: Locale) {
        val defaultLocales = LocaleList.getDefault()
        val locales: LinkedHashSet<Locale?> = LinkedHashSet(defaultLocales.size() + 1)
        // Bring the target locale to the front of the list
        // There's a hidden API, but it's not currently used here.
        locales.add(locale)
        for (i in 0 until defaultLocales.size()) {
            locales.add(defaultLocales[i])
        }
        config.setLocales(LocaleList(*locales.toArray(arrayOfNulls<Locale>(0))))
    }
}

interface DataUpdatedCallback  {
    fun onDataUpdate()
}