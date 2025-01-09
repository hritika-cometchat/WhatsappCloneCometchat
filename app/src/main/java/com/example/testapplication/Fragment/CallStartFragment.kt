package com.example.testapplication.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.listeners.CometChatCallsEventsListener
import com.cometchat.calls.model.AudioMode
import com.cometchat.calls.model.CallSwitchRequestInfo
import com.cometchat.calls.model.GenerateToken
import com.cometchat.calls.model.RTCMutedUser
import com.cometchat.calls.model.RTCRecordingInfo
import com.cometchat.calls.model.RTCUser
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChat.CallbackListener
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.databinding.FragmentCallStartBinding

class CallStartFragment : Fragment() {
    private var callToken = ""
    private var sessionId = ""
    private var isAudio = false
    private var isDefault = true
    private lateinit var binding: FragmentCallStartBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallStartBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()
        isAudio = arguments?.getBoolean("IS_AUDIO_CALL", false) == true
        isDefault = arguments?.getBoolean("IS_DEFAULT_CALL", true) == true
        generateToken()
    }

    private fun generateToken() {
        val userAuthToken = CometChat.getUserAuthToken()
        CometChatCalls.generateToken(
            sessionId,
            userAuthToken,
            object : CometChatCalls.CallbackListener<GenerateToken>() {
                override fun onSuccess(generateToken: GenerateToken) {
                    callToken = generateToken.token
                    startCall(isDefault)
                }

                override fun onError(e: CometChatException) {
                    e.printStackTrace()
                }
            }
        )
    }

    private fun startCall(isDefaultCall : Boolean) {
        var isCallEndedByMe = false

        val callSettings = CometChatCalls.CallSettingsBuilder((activity as CallActivity))
            .setDefaultLayoutEnable(true)
            .setIsAudioOnly(isAudio)
            .setMode(CometChatCallsConstants.MODE_SPOTLIGHT)
            .showAudioModeButton(true)
            .showSwitchToVideoCallButton(true)
            .setEventListener(object : CometChatCallsEventsListener {
                override fun onCallEnded() {
                    if (isDefaultCall) {
                        if (isCallEndedByMe) {
                            endCall()
                        } else {
                            CometChat.clearActiveCall()
                            CometChatCalls.endSession()
                        }
                    } else {
                        CometChatCalls.endSession()
                        endCall()
                    }
                }

                override fun onCallEndButtonPressed() {
                    if (isDefaultCall) {
                        isCallEndedByMe = true
                    } else {
                        CometChatCalls.endSession()
                    }
                }

                override fun onUserJoined(user: RTCUser) {

                }

                override fun onUserLeft(user: RTCUser) {

                }

                override fun onUserListChanged(users: ArrayList<RTCUser>) {

                }

                override fun onAudioModeChanged(devices: ArrayList<AudioMode>) {

                }

                override fun onCallSwitchedToVideo(callSwitchRequestInfo: CallSwitchRequestInfo) {

                }

                override fun onUserMuted(muteObj: RTCMutedUser) {

                }

                override fun onRecordingToggled(recordingInfo: RTCRecordingInfo) {

                }

                override fun onError(ce: CometChatException) {
                }
            })
            .build()


        CometChatCalls.startSession(callToken, callSettings, binding.videoContainer, object : CometChatCalls.CallbackListener<String>() {
                override fun onSuccess(s: String) {
                    Log.w("SESSION_STATUS", s)
                }

                override fun onError(e: CometChatException) {
                    Log.e("SESSION_STATUS", e.printStackTrace().toString())
                }
            })
    }

    private fun endCall(){
        CometChat.endCall(sessionId, object : CallbackListener<Call?>() {
            override fun onSuccess(call: Call?) {
                (activity as? CallActivity)?.finish()
            }
            override fun onError(e: com.cometchat.chat.exceptions.CometChatException?) {
                e?.printStackTrace()
            }
        })
    }
}