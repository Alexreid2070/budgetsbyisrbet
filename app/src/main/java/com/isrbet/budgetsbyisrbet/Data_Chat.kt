package com.isrbet.budgetsbyisrbet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

data class Chat(
    var date: String = "",  // will be format yyyy-mm-dd
    var time: String = "",  // will be format hh:mm:ss
    var username: String = "",
    var text: String = ""
) {
    fun setValue(key: String, value: String) {
        when (key) {
            "date" -> date = value.trim()
            "time" -> time = value.trim()
            "username" -> username = value.trim()
            "text" -> text = value.trim()
        }
    }
}

class ChatViewModel : ViewModel() {
    private var chatListener: ValueEventListener? = null
    private val chats: MutableList<Chat> = ArrayList()
    private var dataUpdatedCallback: ChatDataUpdatedCallback? = null
    private var loaded:Boolean = false

    companion object {
        lateinit var singleInstance: ChatViewModel // used to track static single instance of self
        fun showMe() {
            singleInstance.chats.forEach {
                Log.d("Alex", "SM Chat is " + it.date + " " + it.time + " " + it.username + " " + it.text)
            }
        }

        fun isLoaded():Boolean {
            return singleInstance.loaded
        }

        private fun getChats(): MutableList<Chat> {
            return singleInstance.chats
        }

        fun getCopyOfChats(): MutableList<Chat> {
            val copy = mutableListOf<Chat>()
            copy.addAll(getChats())
            return copy
        }
        fun getCount(): Int {
            return singleInstance.chats.count()
        }

        fun getLastChat(): Chat {
            return singleInstance.chats[singleInstance.chats.count()-1]
        }

        fun addChat(iChat: Chat) {
            singleInstance.chats.add(iChat)
            singleInstance.sortYourself()

            MyApplication.database.getReference("Chats").child(iChat.date+iChat.time+iChat.username).setValue(iChat)
        }
        fun refresh() {
            singleInstance.loadChats()
        }
        fun clear() {
            if (singleInstance.chatListener != null) {
                MyApplication.databaseref.child("Chats").removeEventListener(singleInstance.chatListener!!)
                singleInstance.chatListener = null
            }
            singleInstance.chats.clear()
        }
    }

    init {
        singleInstance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (chatListener != null) {
            MyApplication.databaseref.child("Chats").removeEventListener(chatListener!!)
            chatListener = null
        }
    }

    fun setCallback(iCallback: ChatDataUpdatedCallback?) {
        dataUpdatedCallback = iCallback
    }

    fun clearCallback() {
        dataUpdatedCallback = null
    }

    fun loadChats() {
        // Do an asynchronous operation to fetch chats
        chatListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chats.clear()
                for (element in dataSnapshot.children.toMutableList()) {
                    val tChat = Chat()
                    for (child in element.children) {
                        tChat.setValue(child.key.toString(), child.value.toString())
                    }
                    chats.add(tChat)
                }
                sortYourself()
                singleInstance.loaded = true
                dataUpdatedCallback?.onDataUpdate()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Alex", "loadPost:onCancelled", databaseError.toException())
            }
        }
        MyApplication.database.getReference("Chats").addValueEventListener(
            chatListener as ValueEventListener
        )
    }

    fun sortYourself() {
        chats.sortWith(compareBy({ it.date }, { it.time }))
    }
}

interface ChatDataUpdatedCallback  {
    fun onDataUpdate()
}