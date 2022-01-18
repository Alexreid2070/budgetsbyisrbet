package com.isrbet.budgetsbyisrbet

import android.content.Intent
import android.icu.util.Calendar
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import java.sql.Time

data class TransactionDataFromTD(var amount: Double, var note: String, var category: String) {
}

class CustomNotificationListenerService : NotificationListenerService() {

    companion object {
        lateinit var singleInstance: CustomNotificationListenerService // used to track static single instance of self

        fun getExpenseNotificationCount(): Int {
            Log.d("Alex", "getting expense count")
            if (!::singleInstance.isInitialized)
                return 0
            val activeNotnCount = singleInstance.activeNotifications.size
            Log.d("Alex", "activeNotnCount is " + activeNotnCount)
            var tCount:Int = 0
            if (activeNotnCount > 0) {
                for (count in 0..activeNotnCount-1) {
                    val sbn = singleInstance.activeNotifications[count]
                    if (sbn.packageName == "com.td.myspend")
                        tCount++
                    else
                        Log.d("Alex", "package name is '" + sbn.packageName + "'")
                }
                return tCount
            } else {
                return 0
            }
        }

        fun getTransactionFromNotificationAndDeleteIt() : TransactionDataFromTD? {
            var tCategory: String
            var tAmount: Double
            var tNote: String

            for (count in 0..singleInstance.activeNotifications.size-1) {
                val sbn = singleInstance.activeNotifications[count]
                if (sbn.packageName == "com.td.myspend") {
                    val notification = sbn.notification
                    val notificationText = notification.extras.getCharSequence("android.text").toString()
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        Log.d("Alex", "notification text: " + notificationText)
                        val dateNow = Calendar.getInstance()
                        val key = dateNow.get(Calendar.YEAR).toString() + "-" +
                                dateNow.get(Calendar.MONTH).toString() + "-" +
                                dateNow.get(Calendar.DATE).toString() + "-" +
                                dateNow.get(Calendar.HOUR).toString() + "-" +
                                dateNow.get(Calendar.MINUTE).toString() + "-" +
                                dateNow.get(Calendar.SECOND).toString()

                        try {
                            var dollarSign = notificationText.indexOf("$")
                            var space = notificationText.indexOf(" ", dollarSign)
                            var textAmount = notificationText.substring(dollarSign+1, space).trim()
                            tAmount = textAmount.toDouble()
                            var credited = notificationText.indexOf("credited")
                            Log.d("Alex", "found '" + textAmount + "' for tAmount")
                            if (credited != -1) {
                                Log.d("Alex", "it's a credit!")
                                tAmount = tAmount * -1
                            }
                            var space2 = notificationText.indexOf(" ", space + 1)
                            var lbracket: Int
                            if (space2 >= 0) {
                                lbracket = notificationText.indexOf("[", space2)
                                tNote = notificationText.substring(space2, lbracket).trim()
                            } else {
                                lbracket = notificationText.indexOf("[", space)
                                tNote = "unknown"
                            }
                            Log.d("Alex", "space2 is " + space2.toString())
                            Log.d("Alex", "lbracket is " + lbracket.toString())
                            Log.d("Alex", "found '" + tNote + "' for tNote")
                            var rbracket = notificationText.indexOf("]", lbracket + 1)
                            tCategory = notificationText.substring(lbracket+1, rbracket).trim()
                            Log.d("Alex", "found '" + tCategory + "' for tCategory")
                            Log.d("Alex", "returning")
                            singleInstance.cancelNotification(sbn.key)
                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TDMySpend_Success")
                                .child(key).setValue(notificationText)
                            return TransactionDataFromTD(tAmount, tNote, tCategory)
                        }
                        catch (exception: Exception) {
                            MyApplication.database.getReference("Users/"+MyApplication.userUID+"/TDMySpend_Failure")
                                .child(key).setValue(notificationText)
                            return null
                        }
                    }
                }
            }
            return TransactionDataFromTD(0.0, "", "")
        }

        fun releaseResources() {
            singleInstance.requestUnbind()
        }
    }

    init {
        CustomNotificationListenerService.singleInstance = this
        Log.d("Alex", "starting CustomNotificationListenerService " + if (this == null) " null" else " not null")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        CustomNotificationListenerService.singleInstance = this

        fetchCurrentNotifications()
    }

    // this is called when a new notification is created
    override fun onNotificationPosted(newNotification: StatusBarNotification) {
        Log.i("Alex", "-------- onNotificationPosted(): " + "ID :" + newNotification.id + "\t" + newNotification.notification.tickerText + "\t" + newNotification.packageName)
        Log.d("Alex", "onNotificationPosted :" + newNotification.packageName + "\n")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

     fun fetchCurrentNotifications() {
        val activeNotnCount = this@CustomNotificationListenerService.activeNotifications.size
        Log.d("Alex", "fetchCurrentNotifications: activeNotnCount is " + activeNotnCount)

        if (activeNotnCount > 0) {
            /*
            for (count in 0..activeNotnCount-1) {
                val sbn = this@CustomNotificationListenerService.activeNotifications[count]
                val notification = sbn.notification
            } */
        } else {
            Log.d("Alex", "No active Notn found")
        }
   }
}