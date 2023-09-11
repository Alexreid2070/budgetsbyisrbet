package com.isrbet.budgetsbyisrbet

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import timber.log.Timber
import java.time.LocalTime

data class TransactionDataFromTD(var amount: Double, var where: String, var category: String)

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
                    if (sbn.packageName == "com.td.myspend") {
                        tCount++
                    }
                }
                tCount
            } else {
                0
            }
        }

        fun getTransactionFromNotificationAndDeleteIt() : TransactionDataFromTD? {
            if (cFAKING_TD)
                return TransactionDataFromTD(123.45, "mty Valumart # 145", "Groceries")

            for (count in 0 until singleInstance.activeNotifications.size) {
                val sbn = singleInstance.activeNotifications[count]
                if (sbn.packageName == "com.td.myspend") {
                    val notification = sbn.notification
                    val notificationText = notification.extras.getCharSequence("android.text").toString()
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        return decipherTDMySpendNotification(notificationText)
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

fun decipherTDMySpendNotification (notificationText: String) : TransactionDataFromTD? {
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
    return TransactionDataFromTD(tAmount, tNote, tCategory)
}
