package com.example.testapplication

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private var cometChatNotification: CometChatNotification? = null

    override fun onCreate() {
        super.onCreate()
        cometChatNotification = CometChatNotification.getInstance(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.e("RemoteMessage", message.toString())
        cometChatNotification!!.renderCometChatNotification(message)
    }
}