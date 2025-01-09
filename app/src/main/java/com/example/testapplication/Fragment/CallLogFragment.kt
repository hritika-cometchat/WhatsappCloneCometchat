package com.example.testapplication.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.calls.core.CallLogRequest.CallLogRequestBuilder
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.model.CallLog
import com.cometchat.chat.core.CometChat
import com.example.testapplication.R


class CallLogFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_call, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCallLogs()
    }
    private fun getCallLogs(){
        val callLogRequest = CallLogRequestBuilder()
            .setAuthToken(CometChat.getUserAuthToken())
            .setLimit(30)
            .build()

        callLogRequest.fetchNext(object : CometChatCalls.CallbackListener<List<CallLog?>?>() {
            override fun onSuccess(callLogs: List<CallLog?>?) {
                callLogs?.forEach{
                    Log.w("CallLog", "$it")
                }
            }

            override fun onError(e: CometChatException) {
                Log.e("CallLog Error", e.toString())
            }
        })
    }
}