package com.isrbet.budgetsbyisrbet

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import timber.log.Timber
import java.time.LocalTime

data class BankTransactionData(var amount: Double, var where: String, var category: String)

@Suppress("HardCodedStringLiteral")
class CustomNotificationListenerService : NotificationListenerService() {

    companion object {
        lateinit var singleInstance: CustomNotificationListenerService // used to track static single instance of self

        fun getExpenseNotificationCount(): Int {
            if (cFAKING_TD)
                return 1
            if (!::singleInstance.isInitialized)
                return 0
            val activeNotificationCount = singleInstance.activeNotifications.size
            Timber.tag("Alex").d("activeNotificationCount is $activeNotificationCount")
            var tCount = 0
            return if (activeNotificationCount > 0) {
                for (count in 0 until activeNotificationCount) {
                    val sbn = singleInstance.activeNotifications[count]
                    val notification = sbn.notification
                    val notificationText = notification.extras.getCharSequence("android.text").toString()

                    if (sbn.packageName == "com.td.myspend" ||
                        sbn.packageName == "com.cibc.android.mobi" ||
                        (sbn.packageName == "com.google.android.apps.messaging" &&
                                notificationText.length > 7 &&
                                notificationText.substring(0,7) == "Alterna")) {
                        tCount++
                    }
                }
                tCount
            } else {
                0
            }
        }

        fun getTransactionFromNotificationAndDeleteIt() : BankTransactionData? {
            if (cFAKING_TD)
                return BankTransactionData(123.45, "mty Valumart # 145", "Groceries")

            for (count in 0 until singleInstance.activeNotifications.size) {
                val sbn = singleInstance.activeNotifications[count]
                val notification = sbn.notification
                val notificationText = notification.extras.getCharSequence("android.text").toString()
                if (sbn.packageName == "com.td.myspend" ||
                    sbn.packageName == "com.cibc.android.mobi" ||
                    (sbn.packageName == "com.google.android.apps.messaging" &&
                            notificationText.length > 7 &&
                            notificationText.substring(0,7) == "Alterna")) {
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        val notif = if (sbn.packageName == "com.td.myspend")
                            decipherTDMySpendNotification(notificationText)
                        else if (sbn.packageName == "com.cibc.android.mobi")
                            decipherCIBCNotification(notificationText)
                        else if (sbn.packageName == "com.google.android.apps.messaging")
                            decipherAlternaNotification(notificationText)
                        else
                            null
                        if (notif != null)
                            singleInstance.cancelNotification(sbn.key)
                        return notif
                    }
                }
            }
            return null
        }

/*        fun releaseResources() {
            singleInstance.requestUnbind()  // this seems to unbind from the app forever, not just this run
        } */
    }

    init {
        singleInstance = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        singleInstance = this

//        fetchCurrentNotifications()
    }

    // this is called when a new notification is created
    override fun onNotificationPosted(newNotification: StatusBarNotification) {
//        Log.i("Alex", "-------- onNotificationPosted(): " + "ID :" + newNotification.id + "\t" + newNotification.notification.tickerText + "\t" + newNotification.packageName)
//        Log.d("Alex", "onNotificationPosted :" + newNotification.packageName + "\n")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

/*     private fun fetchCurrentNotifications() {
        val activeNotnCount = this@CustomNotificationListenerService.activeNotifications.size

        if (activeNotnCount > 0) {
            for (count in 0 until singleInstance.activeNotifications.size) {
                val sbn = singleInstance.activeNotifications[count]
                if (sbn.packageName == "com.td.myspend") {
                    val notification = sbn.notification
                    val notificationText =
                        notification.extras.getCharSequence("android.text").toString()
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        Timber.tag("Alex").d("package name ${sbn.packageName} notification text: $notificationText")
                    }
                }
            }
        } else {
            Timber.tag("Alex").d("No active Notn found")
        }
   } */
}

fun decipherTDMySpendNotification (notificationText: String) : BankTransactionData? {
    var tCategory = ""
    var tAmount = 0.0
    var tNote = ""

    val timeNow = LocalTime.now()
    val key = "%04d-%02d-%02d-%02d-%02d-%02d".format(gCurrentDate.getYear(),
        gCurrentDate.getMonth(),
        gCurrentDate.getDay(),
        timeNow.hour,
        timeNow.minute,
        timeNow.second)

    try {
        var credit = false
        var startAt = 0
        val inFrench = notificationText.indexOf(" $ ") != -1

        if (notificationText.substring(0,10) == "Credit of ") {
            credit = true
            startAt = 10
        } else if (notificationText.substring(0,10) == "Crédit de ") {
            credit = true
            startAt = 10
        }
        if (!inFrench)
            startAt += 1

        var endOfNumber : Int
        var textAmount : String

        if (inFrench) {
            endOfNumber = notificationText.indexOf("$", startAt)
            textAmount = notificationText.substring(startAt, endOfNumber).trim()
            textAmount = textAmount.replace(" ", "")
            textAmount = textAmount.replace(",", ".")
        } else { // English
            endOfNumber = notificationText.indexOf(" ", startAt)
            textAmount = notificationText.substring(startAt, endOfNumber).trim()
            textAmount = textAmount.replace(",", "")
        }
        tAmount = textAmount.toDoubleOrNull()!!

        if (credit)
            tAmount *= -1

        val space2 = notificationText.indexOf(" ", endOfNumber + 1)
        var lbracket: Int
        if (space2 >= 0)
        {
            lbracket = notificationText.indexOf("[", space2)
            if (lbracket == -1)
                lbracket = notificationText.length
            tNote = notificationText.substring(space2, lbracket).trim()
        } else
        {
            lbracket = notificationText.indexOf("[", endOfNumber)
            tNote = "unknown"
        }

        val rbracket = notificationText.indexOf("]", lbracket + 1)
        if (lbracket != -1 && rbracket != -1)
            tCategory = notificationText.substring(lbracket + 1, rbracket).trim()
    }
    catch (exception: Exception) {
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TDMySpend_Failure")
            .child(key).setValue(notificationText)
        return null
    }
    return BankTransactionData(tAmount, tNote, tCategory)
}

fun decipherCIBCNotification (notificationText: String) : BankTransactionData? {
    var tCategory = "CIBC"
    var tAmount = 0.0
    var tNote = ""

    val timeNow = LocalTime.now()
    val key = "%04d-%02d-%02d-%02d-%02d-%02d".format(gCurrentDate.getYear(),
        gCurrentDate.getMonth(),
        gCurrentDate.getDay(),
        timeNow.hour,
        timeNow.minute,
        timeNow.second)

    try {
        var startOfNote : Int
        var currencySymbol : Int
        var textAmount : String

        startOfNote = notificationText.indexOf("\n", 0)
        startOfNote += 2
        currencySymbol = notificationText.indexOf("$", startOfNote)
        tNote = notificationText.substring(startOfNote, currencySymbol)

        textAmount = notificationText.substring(currencySymbol+1, notificationText.length).trim()
        textAmount = textAmount.replace(",", "")
        tAmount = textAmount.toDoubleOrNull()!!
    }
    catch (exception: Exception) {
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/CIBC_Failure")
            .child(key).setValue(notificationText)
        return null
    }
    return BankTransactionData(tAmount, tNote.trim(), tCategory)
}

fun decipherAlternaNotification (notificationText: String) : BankTransactionData? {
    var tCategory = "Alterna"
    var tAmount = 0.0
    var tNote = ""
    var currencySymbol : Int
    var onText : Int
    var fromText : Int
    var endOfFromText : Int
    var textAmount : String
    var noteText : String

    val timeNow = LocalTime.now()
    val key = "%04d-%02d-%02d-%02d-%02d-%02d".format(gCurrentDate.getYear(),
        gCurrentDate.getMonth(),
        gCurrentDate.getDay(),
        timeNow.hour,
        timeNow.minute,
        timeNow.second)

    try {
        currencySymbol = notificationText.indexOf("$", 0)
        onText = notificationText.indexOf(" on ", currencySymbol+1)
        fromText = notificationText.indexOf(" at ", onText+1)
        endOfFromText = notificationText.indexOf(",", fromText+1)
        textAmount = notificationText.substring(currencySymbol+1, onText).trim()
        textAmount = textAmount.replace(",", "")
        tAmount = textAmount.toDoubleOrNull()!!
        noteText = notificationText.substring(fromText+4, endOfFromText).trim()
    }
    catch (exception: Exception) {
        MyApplication.database.getReference("Users/"+MyApplication.userUID+"/Alterna_Failure")
            .child(key).setValue(notificationText)
        return null
    }
    return BankTransactionData(tAmount, noteText.trim(), tCategory)
}
