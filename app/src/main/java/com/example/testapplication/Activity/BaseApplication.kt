package com.example.testapplication.Activity

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.testapplication.AppConstants
import com.example.testapplication.AppConstants.CALL_RECEIVER_LISTENER
import com.example.testapplication.R

class BaseApplication : Application() {
    companion object {
        private val LISTENER_ID = CALL_RECEIVER_LISTENER
    }

    override fun onCreate() {
        super.onCreate()

        initUiKit()

        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
//                CometChatCallActivity.launchIncomingCallScreen(this@BaseApplication, call, null)
            }

            override fun onOutgoingCallAccepted(call: Call) {
                // To be implemented
            }

            override fun onOutgoingCallRejected(call: Call) {
                // To be implemented
            }

            override fun onIncomingCallCancelled(call: Call) {
                // To be implemented
            }
        })
    }

    private fun initUiKit() {
        val appID = AppConstants.APP_ID
        val region = AppConstants.REGION

        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setRegion(region)
            .setAppId(appID)
            .subscribePresenceForAllUsers().build()

        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(successString: String?) {
                Log.d("INIT", "Ui Kit Initialization completed successfully")
            }

            override fun onError(e: CometChatException?) {}
        })
    }

}