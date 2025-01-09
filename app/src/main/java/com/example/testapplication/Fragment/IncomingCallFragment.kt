package com.example.testapplication.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.R
import com.example.testapplication.databinding.FragmentIncomingCallBinding
import com.google.gson.Gson

class IncomingCallFragment : Fragment() {
    private var callInitiater = User()
    private var sessionId = ""
    private val callStartFragment = CallStartFragment()
    private lateinit var binding: FragmentIncomingCallBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIncomingCallBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userJson = arguments?.getString("KEY_INITIATER")
        if (!userJson.isNullOrEmpty()) callInitiater = Gson().fromJson(userJson, User::class.java)
        binding.contactName.text = callInitiater.name ?: "Unknown"
        callInitiater.avatar?.let {
            Glide.with(requireContext()).load(callInitiater.avatar)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(binding.ivProfile)
        }
        sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()


        binding.ivCancelCall.setOnClickListener {
            rejectCall()
        }

        binding.ivAcceptCall.setOnClickListener {
            acceptCall()
        }

    }

    private fun startCall() {

    }

    private fun rejectCall() {
        val status: String = CometChatConstants.CALL_STATUS_REJECTED
        CometChat.rejectCall(sessionId, status, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(p0: Call?) {
                Log.i("CALL_RECEIVER_LISTENER", "Call Rejected")
                print("Call Received $sessionId")
                activity?.runOnUiThread {
                    (activity as? CallActivity)?.finish()
                }
            }

            override fun onError(p0: com.cometchat.chat.exceptions.CometChatException?) {
                println(p0)
                activity?.runOnUiThread {
                    (activity as? CallActivity)?.finish()
                }
            }
        })
    }

    private fun acceptCall() {
        CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call?) {
                val isAudio = call?.type == CometChatConstants.CALL_TYPE_AUDIO
                val bundle = Bundle().apply {
                    putString("KEY_SESSION_ID", sessionId)
                    putBoolean("IS_AUDIO_CALL", isAudio)
                }
                (activity as? CallActivity)?.setCurrentFragment(callStartFragment.apply { arguments = bundle })            }

            override fun onError(p0: com.cometchat.chat.exceptions.CometChatException?) {
                println(p0)
                (activity as CallActivity).finish()
            }
        })
    }
}