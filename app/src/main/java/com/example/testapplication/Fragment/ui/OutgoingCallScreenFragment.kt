package com.example.testapplication.Fragment.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.calls.outgoingcall.CometChatOutgoingCall
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallStyle
import com.cometchat.chatuikit.shared.views.button.ButtonStyle
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.R
import com.google.gson.Gson

class OutgoingCallScreenFragment : Fragment() {

    private var sessionId = ""
    private var receiverType = ""
    private lateinit var cometchatOutgoingCall: CometChatOutgoingCall

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cometchatOutgoingCall = CometChatOutgoingCall(requireContext())
        cometchatOutgoingCall.setDeclineButtonStyle(ButtonStyle().apply {
            buttonBackgroundDrawable = R.drawable.bg_rounded_date
        })
        cometchatOutgoingCall.setDeclineButtonIcon(R.drawable.ic_call_end)
        cometchatOutgoingCall.setStyle(OutgoingCallStyle().apply {
            setBackground(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.layout_bg_gradient
                )
            )
        })

        cometchatOutgoingCall.setOnDeclineCallClick { cancelCall() }

        return cometchatOutgoingCall
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()
        val receiverJson = arguments?.getString("KEY_RECEIVER")
        if (!receiverJson.isNullOrEmpty()) {
            if (receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {

            } else {
                val receiver = Gson().fromJson(receiverJson, User::class.java)
                cometchatOutgoingCall.setUser(receiver)
            }
        }
    }

    private fun cancelCall() {
        val status = CometChatConstants.CALL_STATUS_CANCELLED
        CometChat.rejectCall(sessionId, status, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "Call Cancelled")
                activity?.runOnUiThread {
                    (activity as? CallActivity)?.finish()
                }
            }

            override fun onError(e: com.cometchat.chat.exceptions.CometChatException?) {
                Log.e("CALL_RECEIVER_LISTENER", "Error cancelling call: ${e?.message}")
                activity?.runOnUiThread {
                    (activity as? CallActivity)?.finish()
                }
            }
        })
    }

}