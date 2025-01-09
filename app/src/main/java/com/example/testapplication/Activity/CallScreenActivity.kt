package com.example.testapplication.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chatuikit.calls.ongoingcall.CometChatOngoingCall
import com.example.testapplication.AppConstants
import com.example.testapplication.R

class CallScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_screen)

        val ongoingCall = findViewById<CometChatOngoingCall>(R.id.ongoing_call)

        val intent = intent
        val sessionID = intent.getStringExtra(AppConstants.SESSION_ID)
        val receiverType = intent.getStringExtra(AppConstants.RECEIVER_TYPE)
        val action = intent.getStringExtra(AppConstants.CALL_ACTION)
        val type = intent.getStringExtra(AppConstants.CALL_TYPE)

        if (sessionID != null && receiverType != null && action != null) {
            if (action == CometChatConstants.CALL_STATUS_ONGOING) {
                ongoingCall.setSessionId(sessionID)
                ongoingCall.setReceiverType(receiverType)
                ongoingCall.setCallType(type)
                ongoingCall.startCall()
            }
        }
    }
}