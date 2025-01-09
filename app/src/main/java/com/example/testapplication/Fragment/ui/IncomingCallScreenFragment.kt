package com.example.testapplication.Fragment.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.calls.incomingcall.CometChatIncomingCall
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.R
import com.google.gson.Gson

class IncomingCallScreenFragment : Fragment() {

    private var callInitiater = User()
    private var sessionId = ""
    private lateinit var cometchatIncomingCall: CometChatIncomingCall

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()
        val userJson = arguments?.getString("KEY_INITIATER")
        if (!userJson.isNullOrEmpty()) callInitiater = Gson().fromJson(userJson, User::class.java)
        cometchatIncomingCall = CometChatIncomingCall(requireContext())
        cometchatIncomingCall.setOnAcceptCallClick{ acceptCall() }
        val user = User()
        user.uid = callInitiater.uid
        user.name = callInitiater.name ?: "Unknown"
        user.avatar = callInitiater.avatar
        cometchatIncomingCall.setUser(user)
        return cometchatIncomingCall

    }

    private fun acceptCall() {
        CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call?) {
                val isAudio = call?.type == CometChatConstants.CALL_TYPE_AUDIO
                val bundle = Bundle().apply {
                    putString("KEY_SESSION_ID", sessionId)
                    putBoolean("IS_AUDIO_CALL", isAudio)
                }
                (activity as? CallActivity)?.setCurrentFragment(OngoingCallScreenFragment().apply { arguments = bundle })            }

            override fun onError(p0: com.cometchat.chat.exceptions.CometChatException?) {
                println(p0)
                (activity as CallActivity).finish()
            }
        })
    }
}