package com.example.testapplication.Activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.example.testapplication.AppConstants.CALL_RECEIVER_LISTENER
import com.example.testapplication.Fragment.CallInitiatedFragment
import com.example.testapplication.Fragment.CallStartFragment
import com.example.testapplication.Fragment.IncomingCallFragment
import com.example.testapplication.Fragment.ui.IncomingCallScreenFragment
import com.example.testapplication.Fragment.ui.OngoingCallScreenFragment
import com.example.testapplication.Fragment.ui.OutgoingCallScreenFragment
import com.example.testapplication.databinding.ActivityCallBinding
import com.google.gson.Gson


class CallActivity : AppCompatActivity() {

    private val callInitiateFragment = CallInitiatedFragment()
    private val callStartFragment = CallStartFragment()
    private val callIncomingFragment = IncomingCallFragment()
    private val callOutgoingFragmentUi = OutgoingCallScreenFragment()
    private val callOngoingFragmentUi = OngoingCallScreenFragment()
    private val callIncomingUiFragment = IncomingCallScreenFragment()

    private var idUG = ""
    private var receiverType = ""
    private var callType = CometChatConstants.CALL_TYPE_AUDIO
    private val TAG = "CallActivity"

    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (intent.getBooleanExtra("EXTRA_INITIATE_CALL", false))
        {
            idUG = intent.getStringExtra("EXTRA_ID").orEmpty()
            receiverType = intent.getStringExtra("EXTRA_RECEIVER_TYPE").orEmpty()
            if (intent.hasExtra("EXTRA_CALL_TYPE")) callType = intent.getStringExtra("EXTRA_CALL_TYPE") ?: CometChatConstants.CALL_TYPE_AUDIO
            initiateCall()

        } else if (intent.getBooleanExtra("EXTRA_RECEIVE_CALL", false))
        {
            val intiaterJson = intent.getStringExtra("EXTRA_INITIATE")
            if (!intiaterJson.isNullOrEmpty()) {
                val callInitiate = Gson().fromJson(intiaterJson, User::class.java)
                val bundle = Bundle().apply {
                    putString("KEY_INITIATER", Gson().toJson(callInitiate))
                    putString("KEY_SESSION_ID", intent.getStringExtra("SESSION_ID").orEmpty())
                }
                setCurrentFragment(callIncomingUiFragment.apply { arguments = bundle })
            }
        } else if (intent.getBooleanExtra("EXTRA_START_CALL", false)) {
            val isAudio = callType == CometChatConstants.CALL_TYPE_AUDIO
            val isDefault = receiverType == CometChatConstants.RECEIVER_TYPE_USER
            Log.i("CALL_RECEIVER_LISTENER", "Outgoing Call Accepted")
            val bundle = Bundle().apply {
                putString("KEY_SESSION_ID", intent.getStringExtra("SESSION_ID").orEmpty())
                putBoolean("IS_AUDIO_CALL", isAudio)
                putBoolean("IS_DEFAULT_CALL", isDefault)
            }
            setCurrentFragment(callOngoingFragmentUi.apply { arguments = bundle })
        }

    }

    private fun initiateCall() {
        val call = Call(idUG, receiverType, callType)
        CometChat.initiateCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                Log.d(TAG, "Call initiated successfully: $call")
                val receiverJson = intent.getStringExtra("EXTRA_RECEIVER")
                if (!receiverJson.isNullOrEmpty()) {
                    val receiver =
                        if (receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
                            Gson().fromJson(receiverJson, Group::class.java)
                        } else Gson().fromJson(receiverJson, User::class.java)
                    val bundle = Bundle().apply {
                        putString("KEY_RECEIVER", Gson().toJson(receiver))
                        putString("KEY_RECEIVER_TYPE", receiverType)
                        putString("KEY_SESSION_ID", call.sessionId)
                    }
                    setCurrentFragment(callOutgoingFragmentUi.apply { arguments = bundle })
                }
            }

            override fun onError(p0: com.cometchat.chat.exceptions.CometChatException?) {
                Log.d(TAG, "Call initialization failed with exception: " + p0?.message)
            }
        })
    }

    private fun receiveRealTimeCalls() {
        CometChat.addCallListener(CALL_RECEIVER_LISTENER, object : CometChat.CallListener() {

            override fun onOutgoingCallAccepted(call: Call?) {
                val isAudio = call?.type == CometChatConstants.CALL_TYPE_AUDIO
                Log.i("CALL_RECEIVER_LISTENER", "Outgoing Call Accepted")
                val bundle = Bundle().apply {
                    putString("KEY_SESSION_ID", call?.sessionId?:"")
                    putBoolean("IS_AUDIO_CALL", isAudio)
                }
                setCurrentFragment(callOngoingFragmentUi.apply { arguments = bundle })
            }

            override fun onIncomingCallReceived(p0: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "Incoming Call Received")
            }

            override fun onIncomingCallCancelled(p0: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "Incoming Call Cancelled")
                finish()
            }

            override fun onOutgoingCallRejected(p0: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "Outgoing Call Cancelled")
                finish()
            }

            override fun onCallEndedMessageReceived(p0: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "CallEnded Cancelled")
                finish()
            }
        })
    }


    fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(binding.flFragment.id, fragment)
            commit()
        }
    }


    override fun onResume() {
        super.onResume()
        receiveRealTimeCalls()
    }

    override fun onPause() {
        super.onPause()
        CometChat.removeCallListener(CALL_RECEIVER_LISTENER)
    }


}