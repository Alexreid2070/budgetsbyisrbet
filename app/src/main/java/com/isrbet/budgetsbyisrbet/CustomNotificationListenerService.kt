package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class TransactionDataFromTD(var amount: Double, var where: String, var category: String)

class CustomNotificationListenerService : NotificationListenerService() {

    companion object {
        lateinit var singleInstance: CustomNotificationListenerService // used to track static single instance of self

        fun getExpenseNotificationCount(): Int {
            if (cFAKING_TD)
                return 1
            if (!::singleInstance.isInitialized)
                return 0
            val activeNotnCount = singleInstance.activeNotifications.size
            Log.d("Alex", "activeNotnCount is $activeNotnCount")
            var tCount = 0
            return if (activeNotnCount > 0) {
                for (count in 0 until activeNotnCount) {
                    val sbn = singleInstance.activeNotifications[count]
                    Log.d("Alex", "Package name is ${sbn.packageName}")
                    if (sbn.packageName == "com.td.myspend") {
                        tCount++
                        Log.d("Alex", "tcount is now $tCount")
                    }
                }
                tCount
            } else {
                0
            }
        }

        fun getTransactionFromNotificationAndDeleteIt() : TransactionDataFromTD? {
            val tCategory: String
            var tAmount: Double
            val tNote: String

            if (cFAKING_TD)
                return TransactionDataFromTD(123.45, "mty Bulk Barn # 145", "Groceries")

            for (count in 0 until singleInstance.activeNotifications.size) {
                val sbn = singleInstance.activeNotifications[count]
                if (sbn.packageName == "com.td.myspend") {
                    val notification = sbn.notification
                    val notificationText = notification.extras.getCharSequence("android.text").toString()
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        Log.d("Alex", "notification text: $notificationText")
                        val dateNow = Calendar.getInstance()
                        val key = dateNow.get(Calendar.YEAR).toString() + "-" +
                                dateNow.get(Calendar.MONTH).toString() + "-" +
                                dateNow.get(Calendar.DATE).toString() + "-" +
                                dateNow.get(Calendar.HOUR).toString() + "-" +
                                dateNow.get(Calendar.MINUTE).toString() + "-" +
                                dateNow.get(Calendar.SECOND).toString()

                        try {
                            val dollarSign = notificationText.indexOf("$")
                            val space = notificationText.indexOf(" ", dollarSign)
                            val textAmount = notificationText.substring(dollarSign+1, space).trim()

//                            textAmount = textAmount.replace(",","")
                            tAmount = getDoubleValue(textAmount)
                            val credited = notificationText.indexOf("credited")
                            if (credited != -1) {
                                Log.d("Alex", "it's a credit!")
                                tAmount *= -1
                            }
                            val space2 = notificationText.indexOf(" ", space + 1)
                            val lbracket: Int
                            if (space2 >= 0) {
                                lbracket = notificationText.indexOf("[", space2)
                                tNote = notificationText.substring(space2, lbracket).trim()
                            } else {
                                lbracket = notificationText.indexOf("[", space)
                                tNote = "unknown"
                            }
                            val rbracket = notificationText.indexOf("]", lbracket + 1)
                            tCategory = notificationText.substring(lbracket+1, rbracket).trim()
                            singleInstance.cancelNotification(sbn.key)
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

        fetchCurrentNotifications()
    }

    // this is called when a new notification is created
    override fun onNotificationPosted(newNotification: StatusBarNotification) {
//        Log.i("Alex", "-------- onNotificationPosted(): " + "ID :" + newNotification.id + "\t" + newNotification.notification.tickerText + "\t" + newNotification.packageName)
//        Log.d("Alex", "onNotificationPosted :" + newNotification.packageName + "\n")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

     private fun fetchCurrentNotifications() {
        val activeNotnCount = this@CustomNotificationListenerService.activeNotifications.size

        if (activeNotnCount > 0) {
            Log.d("Alex", "$activeNotnCount active notifications found")
            for (count in 0 until singleInstance.activeNotifications.size) {
                val sbn = singleInstance.activeNotifications[count]
                if (sbn.packageName == "com.td.myspend") {
                    val notification = sbn.notification
                    val notificationText =
                        notification.extras.getCharSequence("android.text").toString()
                    if (notificationText != "null" && notificationText != "") {  // this can happen when the TD notifications are grouped
                        Log.d("Alex", "package name ${sbn.packageName} notification text: $notificationText")
                    }
                }
            }
        } else {
            Log.d("Alex", "No active Notn found")
        }
   }
}