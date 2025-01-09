package com.example.testapplication.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chatuikit.calls.ongoingcall.CometChatOngoingCall

class UiCallActivity : AppCompatActivity() {
    //    private lateinit var binding : ActivityOngoingCallBinding
    private lateinit var cometchatOngoingCall: CometChatOngoingCall

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra("KEY_SESSION_ID").orEmpty()

        cometchatOngoingCall = CometChatOngoingCall(this)
        cometchatOngoingCall.setReceiverType(CometChatConstants.RECEIVER_TYPE_USER)
        cometchatOngoingCall.setSessionId(sessionId)

        setContentView(cometchatOngoingCall)

        cometchatOngoingCall.startCall()

    }
}