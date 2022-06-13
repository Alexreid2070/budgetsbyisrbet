package com.isrbet.budgetsbyisrbet

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.icu.text.NumberFormat
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

const val cDiscTypeDiscretionary = "Discretionary"
const val cDiscTypeNondiscretionary = "Non-Discretionary"
const val cDiscTypeAll = "All"
val DiscTypeValues = listOf(cDiscTypeDiscretionary, cDiscTypeNondiscretionary)
const val cPeriodWeek = "Week"
const val cPeriodMonth = "Month"
const val cPeriodQuarter = "Quarter"
const val cPeriodYear = "Year"
val PeriodValues = listOf(cPeriodWeek, cPeriodMonth, cPeriodQuarter, cPeriodYear)
const val cBUDGET_RECURRING = "Recurring"
const val cBUDGET_JUST_THIS_MONTH = "Just once"
const val cFAKING_TD = false
const val cTRANSFER_CODE = -99
const val cLAST_ROW = 9999999
const val cBudgetDateView = "Date"
const val cBudgetCategoryView = "Category"
const val cON = "On"
const val cOFF = "Off"

val gDec = DecimalFormat("###0.00;(###0.00)")
val gDecM = DecimalFormat("###0.00;-###0.00")
val gDecRound = DecimalFormat("###0;(###0)")
val gDecRoundM = DecimalFormat("###0;-###0")
var goToPie = false
var homePageExpansionAreaExpanded = false
val gNumberFormat: NumberFormat = NumberFormat.getInstance()
val gDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

fun gMonthName(iMonth: Int) : String {
    val month = Month.of(iMonth)
    return month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

fun gDecWithCurrency(iDouble: Double, iRound: Boolean = false) : String{
    val s = getLocalCurrencySymbol()
    return if (iRound) {
        if (s == "")
            gDecRound.format(round(iDouble))
        else
            s + " " + gDecRound.format(round(iDouble))
    } else {
        if (s == "")
            gDec.format(iDouble)
        else
            s + " " + gDec.format(iDouble)
    }
}

fun gDecWithCurrencyM(iDouble: Double, iRound: Boolean = false) : String{
    val s = getLocalCurrencySymbol()
    return if (iRound) {
        if (s == "")
            gDecRoundM.format(round(iDouble))
        else
            s + " " + gDecRoundM.format(round(iDouble))
    } else {
        if (s == "")
            gDecM.format(iDouble)
        else
            s + " " + gDecM.format(iDouble)
    }
}
enum class LoanPaymentRegularity(val code: Int) {
    WEEKLY(1),
    BIWEEKLY(2),
    MONTHLY(3);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.code == value }
    }
}
enum class DateRange(val code: Int) {
    MONTH(1),
    YTD(2),
    YEAR(3),
    ALLTIME(4)
}

class MyApplication : Application() {
    companion object {
        lateinit var database: FirebaseDatabase
        lateinit var databaseref: DatabaseReference
        var transactionSearchText: String = ""
        var transactionFirstInList: Int = cLAST_ROW
        var userUID: String = ""
        var userIndex: Int = 0
        var originalUserUID: String = ""
        var userEmail: String = ""
        var userGivenName: String = ""
        var userFamilyName: String = ""
        var userPhotoURL: String = ""
        private var quoteForThisSession: String = ""
        var currentUserEmail: String = ""
        var mediaPlayer: MediaPlayer? = null
        var adminMode: Boolean = false
        var haveLoadedDataForThisUser = false
        lateinit var myMainActivity: MainActivity

        fun displayToast(iMessage: String) {
            Log.d("Alex", "displaying toast")
            Toast.makeText(myMainActivity, iMessage, Toast.LENGTH_SHORT).show()
        }
        fun getQuote(): String {
            if (quoteForThisSession == "") {
                val randomIndex = (0 until inspirationalQuotes.size-1).random()
                val randomElement = inspirationalQuotes[randomIndex]
                quoteForThisSession = randomElement
            }
            return quoteForThisSession
        }

        fun playSound(context: Context?, iSound: Int ) {
            if (DefaultsViewModel.getDefault(cDEFAULT_SOUND) == "Off")
                return

            if (mediaPlayer == null)
                mediaPlayer = MediaPlayer.create(context, iSound)
            try {
                mediaPlayer?.start()
            }
            catch (exception: Exception) {
                Log.d("Alex", "caught an exception in playSound")
            }
        }

        fun releaseResources() {
            mediaPlayer?.release()
            transactionFirstInList = cLAST_ROW
            haveLoadedDataForThisUser = false
            transactionSearchText = ""
//            CustomNotificationListenerService.releaseResources()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // initialization code here
        Firebase.database.setPersistenceEnabled(true)
        database = FirebaseDatabase.getInstance()
        databaseref = database.reference
    }

    override fun onTerminate() {
        super.onTerminate()
        mediaPlayer?.release()
    }
}

data class DataObject(var id: Int, var label: String, var value: Double, var priority: String, var color: Int)

data class BudgetMonth(var year: Int, var month: Int = 0) { // note that month can be 0, signifying the entire year
    constructor(period: String) : this(period.substring(0,4).toInt(), 0) {
        // might get "2022-02-23", or might get "2022-02", or might get "2022-2"
        val dash = period.indexOf("-")
        val dash2 = period.indexOf("-", dash+1)
        month = if (dash2 == -1)
            if (dash > -1) period.substring(dash+1,period.length).toInt() else 0
        else {
            if (dash > -1) period.substring(dash + 1, dash2).toInt() else 0
        }
        }
    constructor(bm: BudgetMonth) : this(bm.year, bm.month)

    fun isAnnualBudget(): Boolean {
        return (month == 0)
    }

    fun setValue(iBM:BudgetMonth) {
        year = iBM.year
        month = iBM.month
    }

    operator fun compareTo(iBM: BudgetMonth): Int {
        return if (year == iBM.year && month == iBM.month)
            0
        else if (year < iBM.year || (year == iBM.year && month < iBM.month))
            -1
        else
            1
    }

    fun addMonth(inc: Int = 1) { // only works up to increases of 12
        if (month == 0) { // isannual
            year += inc
        } else {
            month += inc
            if (month > 12) {
                year++
                month -= 12
            }
        }
    }

    fun decrementMonth(inc: Int = 1) { // only works up to decreases of 12
        if (month == 0) { // isannual
            year -= inc
        } else {
            month -= inc
            if (month <= 0) {
                year--
                month += 12
            }
        }
    }

    override fun toString(): String {
//        if (month == 0) {
//            return year.toString()
//        } else
        return if (month < 10) {
            "$year-0$month"
        } else {
            "$year-$month"
        }
    }
    fun get2DigitMonth(): String {
        return if (month < 10)
            "0$month"
        else
            month.toString()
    }
}

fun giveMeMyDateFormat(cal: Calendar) : String {
    var tempString: String = cal.get(Calendar.YEAR).toString() + "-"
    if (cal.get(Calendar.MONTH)+1 < 10)
        tempString += "0"
    tempString = tempString + (cal.get(Calendar.MONTH)+1).toString() + "-"
    if (cal.get(Calendar.DATE) < 10)
        tempString += "0"
    tempString += cal.get(Calendar.DATE).toString()
    return tempString
}

fun giveMeMyTimeFormat(cal: Calendar) : String {
    var tempString = ""
    if (cal.get(Calendar.HOUR_OF_DAY) < 10)
        tempString = "0"
    tempString = tempString + cal.get(Calendar.HOUR_OF_DAY).toString() + ":"
    if (cal.get(Calendar.MINUTE) < 10)
        tempString += "0"
    tempString = tempString + (cal.get(Calendar.MINUTE)).toString() + ":"
    if (cal.get(Calendar.SECOND) < 10)
        tempString += "0"
    tempString += cal.get(Calendar.SECOND).toString()
    return tempString
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
    if (str[0] == '.') str = "0$str"
    val max = str.length
    var rFinal = ""
    var after = false
    var i = 0
    var up = 0
    var decimal = 0
    var t: Char
    while (i < max) {
        t = str[i]
        if (t != '.' && !after) {
            up++
            if (up > MAX_BEFORE_POINT) return rFinal
        } else if (t == '.') {
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

fun getDaysInMonth(cal: Calendar): Int {
    val month = cal.get(Calendar.MONTH)+1
    val year = cal.get(Calendar.YEAR)
    return getDaysInMonth(year, month)
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
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
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
    } else if ((rActual > rBudget * (1.0 + (DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toInt()/100.0))) ||
        (rBudget == 0.0 && rActual > 0.0)) {
        return if (inDarkMode(context))
            ContextCompat.getColor(context, R.color.darkRed)
        else
            ContextCompat.getColor(context, R.color.red)
    } else {
        return ContextCompat.getColor(context, R.color.orange)
    }
}

fun getLocalCurrencySymbol() : String {
    return if (DefaultsViewModel.getDefault(cDEFAULT_SHOW_CURRENCY_SYMBOL) == "true") {
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

fun getNextBusinessDate(iDate: String) : String {
    val year = iDate.substring(0,4).toInt()
    val month = iDate.substring(5,7).toInt()
    val day = iDate.substring(8,10).toInt()
    val thisDate = Calendar.getInstance()
    thisDate.set(year, month-1, day)
    if(Calendar.SATURDAY == thisDate.get(Calendar.DAY_OF_WEEK)) {
        thisDate.add(Calendar.DATE, 2)
        return giveMeMyDateFormat(thisDate)
    } else if(Calendar.SUNDAY == thisDate.get(Calendar.DAY_OF_WEEK)) {
        thisDate.add(Calendar.DATE, 1)
        return giveMeMyDateFormat(thisDate)
    }

    return iDate
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

fun isNumber(s: String?): Boolean {
    return if (s.isNullOrEmpty()) false else s.all { Character.isDigit(it) }
}

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
            RecurringTransactionViewModel.isLoaded() && RecurringTransactionViewModel.getCount() == 0

}

fun switchTo(iUID: String) {
    MyApplication.userUID=iUID
    SpenderViewModel.refresh()
    DefaultsViewModel.refresh()
    TransactionViewModel.refresh()
    CategoryViewModel.refresh()
    BudgetViewModel.refresh()
    RecurringTransactionViewModel.refresh()
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

    val inspirationalQuotes = listOf(
    "Life is about making an impact, not making an income. -Kevin Kruse",
    "Whatever the mind of man can conceive and believe, it can achieve. -Napoleon Hill",
    "Strive not to be a success, but rather to be of value. -Albert Einstein",
    "Two roads diverged in a wood, and I took the one less traveled by, And that has made all the difference. -Robert Frost",
    "I attribute my success to this: I never gave or took any excuse. -Florence Nightingale",
    "You miss 100% of the shots you don’t take. -Wayne Gretzky",
    "I've missed more than 9000 shots in my career. I've lost almost 300 games. 26 times I've been trusted to take the game winning shot and missed. I've failed over and over and over again in my life. And that is why I succeed. -Michael Jordan",
    "The most difficult thing is the decision to act, the rest is merely tenacity. –Amelia Earhart",
    "Every strike brings me closer to the next home run. –Babe Ruth",
    "Definiteness of purpose is the starting point of all achievement. –W. Clement Stone",
    "Life isn't about getting and having, it's about giving and being. –Kevin Kruse",
    "Life is what happens to you while you’re busy making other plans. –John Lennon",
    "We become what we think about. –Earl Nightingale",
    "Twenty years from now you will be more disappointed by the things that you didn’t do than by the ones you did do, so throw off the bowlines, sail away from safe harbor, catch the trade winds in your sails.  Explore, Dream, Discover. –Mark Twain",
    "Life is 10% what happens to me and 90% of how I react to it. –Charles Swindoll",
    "The most common way people give up their power is by thinking they don’t have any. –Alice Walker",
    "The mind is everything. What you think you become.  –Buddha",
    "The best time to plant a tree was 20 years ago. The second best time is now. –Chinese Proverb",
    "An unexamined life is not worth living. –Socrates",
    "Eighty percent of success is showing up. –Woody Allen",
    "Your time is limited, so don’t waste it living someone else’s life. –Steve Jobs",
    "Winning isn’t everything, but wanting to win is. –Vince Lombardi",
    "I am not a product of my circumstances. I am a product of my decisions. –Stephen Covey",
    "Every child is an artist.  The problem is how to remain an artist once he grows up. –Pablo Picasso",
    "You can never cross the ocean until you have the courage to lose sight of the shore. –Christopher Columbus",
    "I’ve learned that people will forget what you said, people will forget what you did, but people will never forget how you made them feel. –Maya Angelou",
    "Either you run the day, or the day runs you. –Jim Rohn",
    "Whether you think you can or you think you can’t, you’re right. –Henry Ford",
    "The two most important days in your life are the day you are born and the day you find out why. –Mark Twain",
    "Whatever you can do, or dream you can, begin it.  Boldness has genius, power and magic in it. –Johann Wolfgang von Goethe",
    "The best revenge is massive success. –Frank Sinatra",
    "People often say that motivation doesn’t last. Well, neither does bathing.  That’s why we recommend it daily. –Zig Ziglar",
    "Life shrinks or expands in proportion to one's courage. –Anais Nin",
    "If you hear a voice within you say “you cannot paint,” then by all means paint and that voice will be silenced. –Vincent Van Gogh",
    "There is only one way to avoid criticism: do nothing, say nothing, and be nothing. –Aristotle",
    "Ask and it will be given to you; search, and you will find; knock and the door will be opened for you. –Jesus",
    "The only person you are destined to become is the person you decide to be. –Ralph Waldo Emerson",
    "Go confidently in the direction of your dreams.  Live the life you have imagined. –Henry David Thoreau",
    "When I stand before God at the end of my life, I would hope that I would not have a single bit of talent left and could say, I used everything you gave me. –Erma Bombeck",
    "Few things can help an individual more than to place responsibility on him, and to let him know that you trust him.  –Booker T. Washington",
    "Certain things catch your eye, but pursue only those that capture the heart. – Ancient Indian Proverb",
    "Believe you can and you’re halfway there. –Theodore Roosevelt",
    "Everything you’ve ever wanted is on the other side of fear. –George Addair",
    "We can easily forgive a child who is afraid of the dark; the real tragedy of life is when men are afraid of the light. –Plato",
    "Teach thy tongue to say, 'I do not know', and thous shalt progress. –Maimonides",
    "Start where you are. Use what you have.  Do what you can. –Arthur Ashe",
    "When I was 5 years old, my mother always told me that happiness was the key to life.  When I went to school, they asked me what I wanted to be when I grew up.  I wrote down ‘happy’.  They told me I didn’t understand the assignment, and I told them they didn’t understand life. –John Lennon",
    "Fall seven times and stand up eight. –Japanese Proverb",
    "When one door of happiness closes, another opens, but often we look so long at the closed door that we do not see the one that has been opened for us. –Helen Keller",
    "Everything has beauty, but not everyone can see. –Confucius",
    "How wonderful it is that nobody need wait a single moment before starting to improve the world. –Anne Frank",
    "When I let go of what I am, I become what I might be. –Lao Tzu",
    "Life is not measured by the number of breaths we take, but by the moments that take our breath away. –Maya Angelou",
    "Happiness is not something readymade.  It comes from your own actions. –Dalai Lama",
    "If you're offered a seat on a rocket ship, don't ask what seat! Just get on. –Sheryl Sandberg",
    "First, have a definite, clear practical ideal; a goal, an objective. Second, have the necessary means to achieve your ends; wisdom, money, materials, and methods. Third, adjust all your means to that end. –Aristotle",
    "If the wind will not serve, take to the oars. –Latin Proverb",
    "You can’t fall if you don’t climb.  But there’s no joy in living your whole life on the ground. –Unknown",
    "We must believe that we are gifted for something, and that this thing, at whatever cost, must be attained. –Marie Curie",
    "Too many of us are not living our dreams because we are living our fears. –Les Brown",
    "Challenges are what make life interesting and overcoming them is what makes life meaningful. –Joshua J. Marine",
    "If you want to lift yourself up, lift up someone else. –Booker T. Washington",
    "I have been impressed with the urgency of doing. Knowing is not enough; we must apply. Being willing is not enough; we must do. –Leonardo da Vinci",
    "Limitations live only in our minds.  But if we use our imaginations, our possibilities become limitless. –Jamie Paolinetti",
    "You take your life in your own hands, and what happens? A terrible thing, no one to blame. –Erica Jong",
    "What’s money? A man is a success if he gets up in the morning and goes to bed at night and in between does what he wants to do. –Bob Dylan",
    "I didn’t fail the test. I just found 100 ways to do it wrong. –Benjamin Franklin",
    "In order to succeed, your desire for success should be greater than your fear of failure. –Bill Cosby",
    "A person who never made a mistake never tried anything new. – Albert Einstein",
    "The person who says it cannot be done should not interrupt the person who is doing it. –Chinese Proverb",
    "There are no traffic jams along the extra mile. –Roger Staubach",
    "It is never too late to be what you might have been. –George Eliot",
    "You become what you believe. –Oprah Winfrey",
    "I would rather die of passion than of boredom. –Vincent van Gogh",
    "A truly rich man is one whose children run into his arms when his hands are empty. –Unknown",
    "It is not what you do for your children, but what you have taught them to do for themselves, that will make them successful human beings.  –Ann Landers",
    "If you want your children to turn out well, spend twice as much time with them, and half as much money. –Abigail Van Buren",
    "Build your own dreams, or someone else will hire you to build theirs. –Farrah Gray",
    "The battles that count aren't the ones for gold medals. The struggles within yourself--the invisible battles inside all of us--that's where it's at. –Jesse Owens",
    "Education costs money.  But then so does ignorance. –Sir Claus Moser",
    "I have learned over the years that when one's mind is made up, this diminishes fear. –Rosa Parks",
    "It does not matter how slowly you go as long as you do not stop. –Confucius",
    "If you look at what you have in life, you'll always have more. If you look at what you don't have in life, you'll never have enough. –Oprah Winfrey",
    "Remember that not getting what you want is sometimes a wonderful stroke of luck. –Dalai Lama",
    "You can’t use up creativity.  The more you use, the more you have. –Maya Angelou",
    "Dream big and dare to fail. –Norman Vaughan",
    "Our lives begin to end the day we become silent about things that matter. –Martin Luther King Jr.",
    "Do what you can, where you are, with what you have. –Teddy Roosevelt",
    "If you do what you’ve always done, you’ll get what you’ve always gotten. –Tony Robbins",
    "Dreaming, after all, is a form of planning. –Gloria Steinem",
    "It's your place in the world; it's your life. Go on and do all you can with it, and make it the life you want to live. –Mae Jemison",
    "You may be disappointed if you fail, but you are doomed if you don't try. –Beverly Sills",
    "Remember no one can make you feel inferior without your consent. –Eleanor Roosevelt",
    "Life is what we make it, always has been, always will be. –Grandma Moses",
    "The question isn’t who is going to let me; it’s who is going to stop me. –Ayn Rand",
    "When everything seems to be going against you, remember that the airplane takes off against the wind, not with it. –Henry Ford",
    "It’s not the years in your life that count. It’s the life in your years. –Abraham Lincoln",
    "Change your thoughts and you change your world. –Norman Vincent Peale",
    "Either write something worth reading or do something worth writing. –Benjamin Franklin",
    "Nothing is impossible, the word itself says, “I’m possible!” –Audrey Hepburn",
    "The only way to do great work is to love what you do. –Steve Jobs",
    "If you can dream it, you can achieve it. –Zig Ziglar",
    "If faced with a decision and you're not sure what to do, ask yourself, 'What would Nicholai do?'.",
    "You are not a tree.  If you don't like where you are, move.",
    "It is better to be hated for what you are than to be loved for what you are not.",
    "Be yourself and people will like you.",
    "The moment you doubt whether you can fly, you cease forever to be able to do it.",
    "Time you enjoy wasting is not wasted time.",
    "When you can’t find someone to follow, you have to find a way to lead by example.",
    "The worst enemy to creativity is self-doubt.",
    "And, now that you don’t have to be perfect you can be good.",
    "A friend may be waiting behind a stranger’s face.",
    "Each of us is more than the worst thing we’ve ever done.",
    "The greatest glory in living lies not in never falling, but in rising every time we fall. -Nelson Mandela",
    "The way to get started is to quit talking and begin doing. -Walt Disney",
    "Your time is limited, so don't waste it living someone else's life. Don't be trapped by dogma – which is living with the results of other people's thinking. -Steve Jobs",
    "If life were predictable it would cease to be life, and be without flavor. -Eleanor Roosevelt",
    "If you look at what you have in life, you'll always have more. If you look at what you don't have in life, you'll never have enough. -Oprah Winfrey",
    "If you set your goals ridiculously high and it's a failure, you will fail above everyone else's success. -James Cameron",
    "Spread love everywhere you go. Let no one ever come to you without leaving happier. -Mother Teresa",
    "When you reach the end of your rope, tie a knot in it and hang on. -Franklin D. Roosevelt",
    "Always remember that you are absolutely unique. Just like everyone else. -Margaret Mead",
    "Don't judge each day by the harvest you reap but by the seeds that you plant. -Robert Louis Stevenson",
    "The future belongs to those who believe in the beauty of their dreams. -Eleanor Roosevelt",
    "Tell me and I forget. Teach me and I remember. Involve me and I learn. -Benjamin Franklin",
    "The best and most beautiful things in the world cannot be seen or even touched — they must be felt with the heart. -Helen Keller",
    "It is during our darkest moments that we must focus to see the light. -Aristotle",
    "Whoever is happy will make others happy too. -Anne Frank",
    "Do not go where the path may lead, go instead where there is no path and leave a trail. -Ralph Waldo Emerson)",
    "In the end, it's not the years in your life that count. It's the life in your years. -Abraham Lincoln",
    "If life were predictable it would cease to be life and be without flavor. -Eleanor Roosevelt",
    "Too many people spend money they earned..to buy things they don't want..to impress people that they don't like. --Will Rogers",
    "A wise person should have money in their head, but not in their heart. --Jonathan Swift",
    "Wealth consists not in having great possessions, but in having few wants. --Epictetus",
    "Money often costs too much. --Ralph Waldo Emerson",
    "Everyday is a bank account, and time is our currency. No one is rich, no one is poor, we've got 24 hours each. --Christopher Rice",
    "It's how you deal with failure that determines how you achieve success. --David Feherty",
    "Frugality includes all the other virtues. --Cicero",
    "I love money. I love everything about it. I bought some pretty good stuff. Got me a $300 pair of socks. Got a fur sink. An electric dog polisher. A gasoline powered turtleneck sweater. And, of course, I bought some dumb stuff, too. --Steve Martin",
    "An investment in knowledge pays the best interest. --Benjamin Franklin",
    "I will tell you the secret to getting rich on Wall Street. You try to be greedy when others are fearful. And you try to be fearful when others are greedy. --Warren Buffett",
    "Annual income twenty pounds, annual expenditure nineteen six, result happiness. Annual income twenty pounds, annual expenditure twenty pound ought and six, result misery. --Charles Dickens",
    "Opportunity is missed by most people because it is dressed in overalls and looks like work. --Thomas Edison",
    "What we really want to do is what we are really meant to do. When we do what we are meant to do, money comes to us, doors open for us, we feel useful, and the work we do feels like play to us. --Julia Cameron",
    "I never attempt to make money on the stock market. I buy on the assumption that they could close the market the next day and not reopen it for ten years. --Warren Buffett",
    "A nickel ain't worth a dime anymore. --Yogi Berra",
    "Money never made a man happy yet, nor will it. The more a man has, the more he wants. Instead of filling a vacuum, it makes one. --Benjamin Franklin",
    "Many people take no care of their money till they come nearly to the end of it, and others do just the same with their time. --Johann Wolfgang von Goethe",
    "Formal education will make you a living; self-education will make you a fortune. --Jim Rohn",
    "Money is only a tool. It will take you wherever you wish, but it will not replace you as the driver. --Ayn Rand",
    "Financial peace isn't the acquisition of stuff. It's learning to live on less than you make, so you can give money back and have money to invest. You can't win until you do this. --Dave Ramsey",
    "It is not the man who has too little, but the man who craves more, that is poor. --Seneca",
    "It’s not the employer who pays the wages. Employers only handle the money. It’s the customer who pays the wages. --Henry Ford",
    "He who loses money, loses much; He who loses a friend, loses much more; He who loses faith, loses all. --Eleanor Roosevelt",
    "Happiness is not in the mere possession of money; it lies in the joy of achievement, in the thrill of creative effort. --Franklin D. Roosevelt",
    "Empty pockets never held anyone back. Only empty heads and empty hearts can do that. --Norman Vincent Peale",
    "It’s good to have money and the things that money can buy, but it’s good, too, to check up once in a while and make sure that you haven’t lost the things that money can’t buy. --George Lorimer",
    "You can only become truly accomplished at something you love. Don’t make money your goal. Instead, pursue the things you love doing, and then do them so well that people can’t take their eyes off you. --Maya Angelou",
    "Buy when everyone else is selling and hold until everyone else is buying. That’s not just a catchy slogan. It’s the very essence of successful investing. --J. Paul Getty",
    "If money is your hope for independence you will never have it. The only real security that a man will have in this world is a reserve of knowledge, experience, and ability. --Henry Ford",
    "If all the economists were laid end to end, they’d never reach a conclusion. --George Bernard Shaw",
    "How many millionaires do you know who have become wealthy by investing in savings accounts? I rest my case. --Robert G. Allen",
    "I made my money the old-fashioned way. I was very nice to a wealthy relative right before he died. --Malcolm Forbes",
    "Innovation distinguishes between a leader and a follower. --Steve Jobs",
    "The real measure of your wealth is how much you'd be worth if you lost all your money. --Anonymous",
    "Money is a terrible master but an excellent servant. --P.T. Barnum",
    "Try to save something while your salary is small; it’s impossible to save after you begin to earn more. --Jack Benny",
    "Wealth is the ability to fully experience life. --Henry David Thoreau",
    "The individual investor should act consistently as an investor and not as a speculator. --Ben Graham",
    "I’m a great believer in luck, and I find the harder I work the more I have of it. --Thomas Jefferson",
    "You must gain control over your money or the lack of it will forever control you. --Dave Ramsey",
    "Investing should be more like watching paint dry or watching grass grow. If you want excitement, take $800 and go to Las Vegas. --Paul Samuelson",
    "Every time you borrow money, you're robbing your future self. --Nathan W. Morris",
    "Rich people have small TVs and big libraries, and poor people have small libraries and big TVs. --Zig Ziglar",
    "Never spend your money before you have it. --Thomas Jefferson",
    "The stock market is filled with individuals who know the price of everything, but the value of nothing. --Phillip Fisher",
    "Wealth is not his that has it, but his that enjoys it. --Benjamin Franklin",
    "It's not how much money you make, but how much money you keep, how hard it works for you, and how many generations you keep it for. --Robert Kiyosaki",
    "I have not failed. I’ve just found 10,000 ways that won’t work. --Thomas A. Edison",
    "If you don’t value your time, neither will others. Stop giving away your time and talents. Value what you know & start charging for it. --Kim Garst",
    "The habit of saving is itself an education; it fosters every virtue, teaches self-denial, cultivates the sense of order, trains to forethought, and so broadens the mind. --T.T. Munger",
    "Don't tell me what you value, show me your budget, and I'll tell you what you value.” --Joe Biden",
    "If you live for having it all, what you have is never enough. --Vicki Robin",
    "Before you speak, listen. Before you write, think. Before you spend, earn. Before you invest, investigate. Before you criticize, wait. Before you pray, forgive. Before you quit, try. Before you retire, save. Before you die, give. --William A. Ward",
    "We make a living by what we get, but we make a life by what we give. --Winston Churchill",
    "Wealth after all is a relative thing since he that has little and wants less is richer than he that has much and wants more. --Charles Caleb Colton",
    "Not everything that can be counted counts, and not everything that counts can be counted. --Albert Einstein",
    "It is time for us to stand and cheer for the doer, the achiever, the one who recognizes the challenge and does something about it. --Vince Lombardi",
    "It's not the situation, but whether we react (negative) or respond (positive) to the situation that's important. --Zig Ziglar",
    "A successful man is one who can lay a firm foundation with the bricks others have thrown at him. --David Brinkley",
    "Let him who would enjoy a good future waste none of his present. --Roger Babson",
    "Courage is being scared to death, but saddling up anyway. --John Wayne",
    "Live as if you were to die tomorrow. Learn as if you were to live forever. --Mahatma Gandhi",
    "Twenty years from now you will be more disappointed by the things that you didn’t do than by the ones you did do. --Mark Twain",
    "It is our choices, that show what we truly are, far more than our abilities. --J. K Rowling",
    "The successful warrior is the average man, with laser-like focus. --Bruce Lee",
    "Develop success from failures. Discouragement and failure are two of the surest stepping stones to success. --Dale Carnegie",
    "The question isn’t who is going to let me; it’s who is going to stop me. --Ayn Rand",
    "Don’t let the fear of losing be greater than the excitement of winning. --Robert Kiyosaki",
    "You can’t connect the dots looking forward; you can only connect them looking backwards. So you have to trust that the dots will somehow connect in your future. You have to trust in something – your gut, destiny, life, karma, whatever. This approach has never let me down, and it has made all the difference in my life. --Steve Jobs",
    "Let no feeling of discouragement prey upon you, and in the end you are sure to succeed. --Abraham Lincoln",
    "Screw it, Let’s do it! --Richard Branson",
    "If your ship doesn’t come in, swim out to meet it! --Jonathan Winters",
    "A real entrepreneur is somebody who has no safety net underneath them. --Henry Kravis",
    "The only place where success comes before work is in the dictionary. --Vidal Sassoon",
    "Success is walking from failure to failure with no loss of enthusiasm. --Winston Churchill",
    "Without continual growth and progress, such words as improvement, achievement, and success have no meaning. --Benjamin Franklin",
    "If plan A fails, remember there are 25 more letters. --Chris Guillebeau",
    "Do not go where the path may lead, go instead where there is no path and leave a trail. --Ralph Waldo Emerson",
    "A journey of a thousand miles must begin with a single step. --Lao Tzu",
    "Do the one thing you think you cannot do. Fail at it. Try again. Do better the second time. The only people who never tumble are those who never mount the high wire. This is your moment. Own it. --Oprah Winfrey",
    "Believe you can and you’re halfway there. --Theodore Roosevelt",
    "The Stock Market is designed to transfer money from the Active to the Patient. --Warren Buffett",
    "I’m only rich because I know when I’m wrong…I basically have survived by recognizing my mistakes. --George Soros",
    "Persist – don’t take no for an answer. If you’re happy to sit at your desk and not take any risk, you’ll be sitting at your desk for the next 20 years. --David Rubenstein",
    "If you took our top fifteen decisions out, we’d have a pretty average record. It wasn’t hyperactivity, but a hell of a lot of patience. You stuck to your principles and when opportunities came along, you pounced on them with vigor. --Charlie Munger",
    "When buying shares, ask yourself, would you buy the whole company? --Rene Rivkin",
    "If you have trouble imagining a 20% loss in the stock market, you shouldn’t be in stocks. --John Bogle",
    "My old father used to have a saying:  If you make a bad bargain, hug it all the tighter. --Abraham Lincoln",
    "It takes as much energy to wish as it does to plan. --Eleanor Roosevelt",
    "The four most expensive words in the English language are, ‘This time it’s different.’ --Sir John Templeton",
    "I'd like to live as a poor man with lots of money. --Pablo Picasso",
    "Fortune sides with him who dares. --Virgil",
    "Wealth is like sea-water; the more we drink, the thirstier we become; and the same is true of fame. --Arthur Schopenhauer",
    "If we command our wealth, we shall be rich and free. If our wealth commands us, we are poor indeed. --Edmund Burke",
    "No wealth can ever make a bad man at peace with himself. --Plato",
    "My formula for success is rise early, work late and strike oil. --JP Getty",
    "The best thing money can buy is financial freedom. --Me"
)