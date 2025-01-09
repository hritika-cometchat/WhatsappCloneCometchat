package com.example.testapplication.Fragment.ui

import android.os.Bundle
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
import com.cometchat.calls.model.MainVideoContainerSetting
import com.cometchat.calls.model.RTCMutedUser
import com.cometchat.calls.model.RTCRecordingInfo
import com.cometchat.calls.model.RTCUser
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chatuikit.calls.ongoingcall.CometChatOngoingCall
import com.cometchat.chatuikit.shared.events.CometChatCallEvents
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.R

class OngoingCallScreenFragment : Fragment() {
    private lateinit var cometchatOngoingCall: CometChatOngoingCall
    private var isAudio = false
    private var isDefault = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()
        isAudio = arguments?.getBoolean("IS_AUDIO_CALL", false) == true
        isDefault = arguments?.getBoolean("IS_DEFAULT_CALL", true) == true
        cometchatOngoingCall = CometChatOngoingCall(requireContext())

        val videoSettings = MainVideoContainerSetting().apply {
            setMainVideoAspectRatio(CometChatCallsConstants.ASPECT_RATIO_CONTAIN)
            setFullScreenButtonParams(CometChatCallsConstants.POSITION_BOTTOM_RIGHT, true)
            setNameLabelParams(CometChatCallsConstants.POSITION_BOTTOM_LEFT, true, "#333333")
            setZoomButtonParams(CometChatCallsConstants.POSITION_BOTTOM_RIGHT, true)
            setUserListButtonParams(CometChatCallsConstants.POSITION_BOTTOM_RIGHT, true)
        }



        val callSettingsBuilder = CometChatCalls.CallSettingsBuilder((activity as CallActivity)).apply {
            mode = CometChatCallsConstants.MODE_SPOTLIGHT
            showAudioModeButton(true)
            startWithVideoMuted(true)
            showSwitchToVideoCallButton(true)
            setIsAudioOnly(true)
//            mainVideoContainerSetting = videoSettings
        }


        cometchatOngoingCall.apply {
            setCallSettingsBuilder(callSettingsBuilder)
            setReceiverType(
                if (isDefault)CometChatConstants.RECEIVER_TYPE_USER else CometChatConstants.RECEIVER_TYPE_GROUP
            )
            setSessionId(sessionId)
            startCall()
        }
        return cometchatOngoingCall
    }

}