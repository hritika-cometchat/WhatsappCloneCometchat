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
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.R
import com.example.testapplication.databinding.FragmentCallInitiatedBinding
import com.google.gson.Gson

class CallInitiatedFragment : Fragment() {
    private var sessionId = ""
    private var receiverType = ""
    private lateinit var binding: FragmentCallInitiatedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallInitiatedBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        receiverType = arguments?.getString("KEY_RECEIVER_TYPE").orEmpty()
        val receiverJson = arguments?.getString("KEY_RECEIVER")
        if (!receiverJson.isNullOrEmpty()) {
            if (receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
                val receiver = Gson().fromJson(receiverJson, Group::class.java)
                binding.contactName.text = receiver.name ?: "Unknown"
                receiver.icon?.let {
                    Glide.with(requireContext()).load(it)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(binding.ivProfile)
                }
            } else {
                val receiver = Gson().fromJson(receiverJson, User::class.java)
                binding.contactName.text = receiver.name ?: "Unknown"
                receiver.avatar?.let {
                    Glide.with(requireContext()).load(it)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(binding.ivProfile)
                }
            }


        }
        sessionId = arguments?.getString("KEY_SESSION_ID").orEmpty()

        binding.cancelCallButton.setOnClickListener {
            cancelCall()
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