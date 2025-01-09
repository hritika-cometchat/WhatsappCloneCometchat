package com.example.testapplication

import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.widget.Toast
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings.UIKitSettingsBuilder
import com.example.testapplication.Activity.CallScreenActivity

class CallConnection(service: CallConnectionService, var call: Call) : Connection() {
    var service: CallConnectionService = service

    override fun onCallAudioStateChanged(state: CallAudioState) {
    }

    override fun onDisconnect() {
        super.onDisconnect()
        destroyConnection()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL, AppConstants.MISSED))
        if (CometChat.getActiveCall() != null) onDisconnect(CometChat.getActiveCall())
    }

    fun onDisconnect(call: Call) {
        CometChat.rejectCall(
            call.sessionId,
            CometChatConstants.CALL_STATUS_CANCELLED,
            object : CometChat.CallbackListener<Call?>() {
                override fun onSuccess(call: Call?) {
                }

                override fun onError(e: CometChatException) {
                    Toast.makeText(service, R.string.cometchat_disconnect_error, Toast.LENGTH_LONG)
                        .show()
                }
            })
    }

    fun destroyConnection() {
        setDisconnected(
            DisconnectCause(
                DisconnectCause.REMOTE,
                AppConstants.REJECTED
            )
        )
        super.destroy()
    }

    override fun onAnswer(videoState: Int) {
        if (call.sessionId != null) {
            if (!CometChat.isInitialized()) {
                initializeCometChat()
            }
            CometChat.acceptCall(call.sessionId, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    val intent= Intent(
                        service,
                        CallScreenActivity::class.java
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(AppConstants.SESSION_ID, call.sessionId)
                    intent.putExtra(AppConstants.RECEIVER_TYPE, call.receiverType)
                    intent.putExtra(AppConstants.CALL_ACTION, call.action)
                    intent.putExtra(AppConstants.CALL_TYPE, call.type)
                    service.startActivity(intent)
                    destroyConnection()
                }

                override fun onError(e: CometChatException) {
                    destroyConnection()
                    Toast.makeText(service, "${R.string.error} ${ e.message }", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onShowIncomingCallUi() {
    }

    override fun onAnswer() {
        if (call.sessionId != null) {
            if (!CometChat.isInitialized()) {
                initializeCometChat()
            }
            CometChat.acceptCall(call.sessionId, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    service.sendBroadcast(callIntent)
                    val intent: Intent = Intent(
                        service,
                        CallScreenActivity::class.java
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(AppConstants.SESSION_ID, call.sessionId)
                    intent.putExtra(AppConstants.RECEIVER_TYPE, call.receiverType)
                    intent.putExtra(AppConstants.CALL_ACTION, call.action)
                    intent.putExtra(AppConstants.CALL_TYPE, call.type)
                    service.startActivity(intent)
                    destroyConnection()
                }

                override fun onError(e: CometChatException) {
                    destroyConnection()
                    Toast.makeText(service, "${R.string.error} ${e.message}" , Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onHold() {
    }

    override fun onUnhold() {
    }

    override fun onReject() {
        if (call.sessionId != null) {
            if (!CometChat.isInitialized()) {
                initializeCometChat()
            }
            CometChat.rejectCall(
                call.sessionId,
                CometChatConstants.CALL_STATUS_REJECTED,
                object : CometChat.CallbackListener<Call?>() {
                    override fun onSuccess(call: Call?) {
                        destroyConnection()
                        setDisconnected(
                            DisconnectCause(
                                DisconnectCause.REJECTED,
                                AppConstants.REJECTED
                            )
                        )
                    }

                    override fun onError(e: CometChatException) {
                        destroyConnection()
                        Toast.makeText(service, "${R.string.error} ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                })
        }
    }

    private val callIntent: Intent
        get() {
            val callIntent = Intent(AppConstants.COMETCHAT_CALL_EVENT)
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            callIntent.putExtra(AppConstants.SESSION_ID, call.sessionId)
            return callIntent
        }

    private fun initializeCometChat() {
        val uiKitSettings = UIKitSettingsBuilder()
            .setRegion(AppConstants.REGION)
            .setAppId(AppConstants.APP_ID)
            .setAuthKey(AppConstants.API_KEY)
            .subscribePresenceForAllUsers().build()

        CometChatUIKit.init(service, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(successString: String?) {
            }

            override fun onError(e: CometChatException) {
            }
        })
    }
}