package com.isrbet.budgetsbyisrbet

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.app.Activity
import android.graphics.Color
import androidx.fragment.app.FragmentManager
import android.net.ConnectivityManager
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.random.Random

const val RC_SIGN_IN = 7
const val cDiscTypeDiscretionary = "Discretionary"
const val cDiscTypeNondiscretionary = "Non-Discretionary"
const val cDiscTypeOff = "Off"
val DiscTypeValues = listOf(cDiscTypeDiscretionary, cDiscTypeNondiscretionary, cDiscTypeOff)
const val cPeriodWeek = "Week"
const val cPeriodMonth = "Month"
const val cPeriodQuarter = "Quarter"
const val cPeriodYear = "Year"
val PeriodValues = listOf(cPeriodWeek, cPeriodMonth, cPeriodQuarter, cPeriodYear)

const val january = "Jan"
const val february = "Feb"
const val march = "Mar"
const val april = "Apr"
const val may = "May"
const val june = "Jun"
const val july = "Jul"
const val august = "Aug"
const val september = "Sep"
const val october = "Oct"
const val november = "Nov"
const val december = "Dec"
val MonthNames = listOf(january, february, march, april, may, june, july, august, september, october, november, december)

class MyApplication : Application() {
    companion object {
        lateinit var database: FirebaseDatabase
        lateinit var databaseref: DatabaseReference
        var transactionSearchText: String = ""
        var transactionFirstInList: Int = 0
        var userUID: String = ""
        var userEmail: String = ""
        var quoteForThisSession: String = ""

        fun getQuote(): String {
            if (quoteForThisSession == "") {
                val randomIndex = Random.nextInt(inspirationalQuotes.size);
                val randomElement = inspirationalQuotes[randomIndex]
                quoteForThisSession = randomElement
            }
            return quoteForThisSession
        }
}

    override fun onCreate() {
        super.onCreate()
        // initialization code here
        Firebase.database.setPersistenceEnabled(true)
        database = FirebaseDatabase.getInstance()
        databaseref = database.getReference()
    }
}

data class BudgetMonth(var year: Int, var month: Int = 0) { // note that month can be 0, signifying the entire year
    constructor(period: String) : this(period.substring(0,4).toInt(), period.substring(5,7).toInt())
    constructor(bm: BudgetMonth) : this(bm.year, bm.month)

    fun addMonth(inc: Int = 1) { // only works up to increases of 12
        month += inc
        if (month > 12) {
            year++
            month -= 12
        }
    }

    fun decrementMonth(inc: Int = 1) { // only works up to decreases of 12
        month -= inc
        if (month <= 0) {
            year--
            month += 12
        }
    }

    override fun toString(): String {
        if (month == 0) {
            return year.toString()
        } else if (month < 10) {
            return year.toString() + "-0" + month.toString()
        } else {
            return year.toString() + "-" + month.toString()
        }
    }
}

fun giveMeMyDateFormat(cal: Calendar) : String {
    var tempString: String
    tempString = cal.get(Calendar.YEAR).toString() + "-"
    if (cal.get(Calendar.MONTH)+1 < 10)
        tempString = tempString + "0"
    tempString = tempString + (cal.get(Calendar.MONTH)+1).toString() + "-"
    if (cal.get(Calendar.DATE) < 10)
        tempString = tempString + "0"
    tempString = tempString + cal.get(Calendar.DATE).toString()
    return tempString
}

fun hideKeyboard(context: Context, view: View) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

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

fun PerfectDecimal(str: String, MAX_BEFORE_POINT: Int, MAX_DECIMAL: Int): String {
    var str = str
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
        if (t != '.' && after == false) {
            up++
            if (up > MAX_BEFORE_POINT) return rFinal
        } else if (t == '.') {
            after = true
        } else {
            decimal++
            if (decimal > MAX_DECIMAL) return rFinal
        }
        rFinal = rFinal + t
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
}

object InternetConnection {
    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    fun checkConnection(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connMgr != null) {
            val activeNetworkInfo = connMgr.activeNetworkInfo
            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                return if (activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    true
                } else activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
            }
        }
        return false
    }
}

fun getDaysInMonth(cal: Calendar): Int {
    val month = cal.get(Calendar.MONTH)+1
    val year = cal.get(Calendar.YEAR)
    if (month == 4 || month == 6 || month == 9 || month == 11) {
        return 30
    } else if (month == 2) {
        if (year % 4 == 0)
            return 29
        else
            return 28
    }
    else
        return 31
}

open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {
    private val gestureDetector: GestureDetector
    companion object {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100
    }

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
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
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
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

fun getBudgetColour(iActual: Double, iBudget: Double): Int {
    if (iActual <= iBudget) {
        return Color.GREEN
    } else if ((iActual > iBudget * (1.0 + (DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toInt()/100.0))) ||
        (iBudget == 0.0 && iActual > 0.0)) {
            Log.d("Alex", "iActual " + iActual.toString() + " iBudget " + iBudget.toString() + " dsRed " + DefaultsViewModel.getDefault(cDEFAULT_SHOWRED) + " c1 " + (1.0 +(DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toInt()/100.0)).toString())
        return Color.RED
    } else {
        Log.d("Alex", "found orange")
        return Color.parseColor("#FFA500")  // orange
    }
}

fun textIsSafe(iText: String) : Boolean {
    if (iText.contains("("))
        return false
    else if (iText.contains(")"))
        return false
    else if (iText.contains("^"))
        return false
    else if (iText.contains("."))
        return false
    else if (iText.contains("/"))
        return false
    else if (iText.contains("\\"))
        return false
    else if (iText.contains("["))
        return false
    else if (iText.contains("]"))
        return false
    else if (iText.contains("+"))
        return false
    else if (iText.contains("#"))
        return false
    else if (iText.contains("$"))
        return false
    else if (iText.contains("%"))
        return false
    else
        return true
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
    "If life were predictable it would cease to be life and be without flavor. -Eleanor Roosevelt"
)