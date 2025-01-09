package com.example.testapplication

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.view.Surface
import android.widget.Toast
import com.cometchat.chat.core.Call
import java.util.Locale

class CallConnectionService : ConnectionService() {
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val bundle = request.extras
        val sessionID = bundle.getString(AppConstants.SESSION_ID)
        val name = bundle.getString(AppConstants.NAME)
        val type = bundle.getString(AppConstants.TYPE)
        var callType = bundle.getString(AppConstants.CALL_TYPE)
        callType = callType!!.substring(0, 1).uppercase(Locale.getDefault()) + callType.substring(1)
        val receiverUID = bundle.getString(AppConstants.RECEIVER_ID)
        if (receiverUID != null && type != null) {
            val call = Call(receiverUID, type, callType)
            call.sessionId = sessionID
            conn = CallConnection(this, call)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            conn!!.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        }
        conn!!.setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
        conn!!.setAddress(
            Uri.parse(callType + getString(R.string.call)),
            TelecomManager.PRESENTATION_ALLOWED
        )
        conn!!.setInitializing()
        conn!!.videoProvider = object : Connection.VideoProvider() {
            override fun onSetCamera(cameraId: String) {
            }

            override fun onSetPreviewSurface(surface: Surface) {
            }

            override fun onSetDisplaySurface(surface: Surface) {
            }

            override fun onSetDeviceOrientation(rotation: Int) {
            }

            override fun onSetZoom(value: Float) {
            }

            override fun onSendSessionModifyRequest(
                fromProfile: VideoProfile,
                toProfile: VideoProfile
            ) {
            }

            override fun onSendSessionModifyResponse(responseProfile: VideoProfile) {
            }

            override fun onRequestCameraCapabilities() {
            }

            override fun onRequestConnectionDataUsage() {
            }

            override fun onSetPauseImage(uri: Uri) {
            }
        }
        conn!!.setActive()
        return conn!!
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val bundle = request.extras
        val sessionID = bundle.getString(AppConstants.SESSION_ID)
        val name = bundle.getString(AppConstants.NAME)
        val receiverType = bundle.getString(AppConstants.TYPE)
        val callType = bundle.getString(AppConstants.CALL_TYPE)
        val receiverUID = bundle.getString(AppConstants.RECEIVER_ID)

        if (receiverUID != null) {
            val call = Call(receiverUID, receiverType, callType)
            call.sessionId = sessionID
            conn = CallConnection(this, call)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                conn!!.connectionProperties = Connection.PROPERTY_SELF_MANAGED
            }
            conn!!.setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
            conn!!.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            conn!!.setInitializing()
            conn!!.setActive()
            return conn!!
        } else {
            val phoneNumber = bundle.getString(AppConstants.ORIGINAL_NUMBER)
            Toast.makeText(
                baseContext,
                "${R.string.cometchat_tried_calling} $phoneNumber",
                Toast.LENGTH_LONG
            ).show()
            return super.onCreateOutgoingConnection(connectionManagerPhoneAccount, request)
        }
    }

    companion object {
        var conn: CallConnection? = null
    }
}