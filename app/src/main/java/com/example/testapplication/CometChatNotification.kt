package com.example.testapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChatNotifications
import com.cometchat.chat.enums.PushPlatforms
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.Ui.CometChatUiActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class CometChatNotification {

    fun registerCometChatNotification(listener: CometChat.CallbackListener<String?>) {
        if (!isFirebaseAppInitialized) {
            listener.onError(
                CometChatException(
                    AppConstants.NOTIFICATION_NOT_REGISTERED,
                    AppConstants.FIREBASE_NOT_REGISTERED
                )
            )
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, context!!.getString(R.string.cometchat_fcmtoken_failed), task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            Log.i(TAG, "Push Notification Token = $token")
            CometChatNotifications.registerPushToken(
                token,
                PushPlatforms.FCM_ANDROID,
                AppConstants.FCM_PROVIDER_ID,
                object : CometChat.CallbackListener<String?>() {
                    override fun onSuccess(s: String?) {
                        listener.onSuccess(s)
                    }

                    override fun onError(e: CometChatException) {
                        listener.onError(e)
                    }
                })
        })
    }

    fun renderCometChatNotification(remoteMessage: RemoteMessage, ) {
        val data = JSONObject(remoteMessage.data as Map<*, *>?)
        Log.e("RemoteMessage", remoteMessage.toString())
        try {
            when (data.getString(AppConstants.TYPE)) {
                AppConstants.CHAT -> renderTextMessageNotification(data)
                CometChatConstants.CATEGORY_CALL -> handleCallNotification(data)
                else -> {}
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun handleCallNotification(data: JSONObject) {
        try {
            val callTime = data.getString(AppConstants.SENT_AT).toLong()
            if (data.getString(AppConstants.CALL_ACTION) == CometChatConstants.CALL_STATUS_INITIATED && System.currentTimeMillis() <= (callTime + 30000)) {
                val call = Call(
                    data.getString(AppConstants.RECEIVER),
                    data.getString(AppConstants.RECEIVER_TYPE),
                    data.getString(AppConstants.CALL_TYPE)
                )
                call.sessionId = data.getString(AppConstants.SESSION_ID)
                if (data.getString(AppConstants.RECEIVER_TYPE) == CometChatConstants.RECEIVER_TYPE_USER) {
                    val user = User()
                    user.uid = data.getString(AppConstants.RECEIVER)
                    user.name = data.getString(AppConstants.RECEIVER_NAME)
                    user.avatar = data.getString(AppConstants.RECEIVER_AVATAR)
                    call.callInitiator = user
                    call.callReceiver = user
                } else {
                    val group = Group()
                    group.guid = data.getString(AppConstants.RECEIVER)
                    group.name = data.getString(AppConstants.RECEIVER_NAME)
                    group.icon = data.getString(AppConstants.RECEIVER_AVATAR)
                    call.callInitiator = group
                    call.callReceiver = group
                }
                startIncomingCall(call)
            } else {
                if (ContextCompat.checkSelfPermission(
                        context!!,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED && telecomManager != null
                ) {
                    val isInCall = telecomManager!!.isInCall
                    if (isInCall) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            telecomManager!!.endCall()
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }


    fun startIncomingCall(call: Call) {
        val entity = call.callInitiator

        if (context!!.checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
            val extras = Bundle()
            val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, call.sessionId.substring(0, 11), null)

            extras.putString(AppConstants.SESSION_ID, call.sessionId)
            extras.putString(AppConstants.TYPE, call.receiverType)
            extras.putString(AppConstants.CALL_TYPE, call.type)

            if (entity is User) {
                extras.putString(AppConstants.NAME, entity.name)
            } else {
                extras.putString(AppConstants.NAME, (entity as Group).name)
            }

            if (entity is User) {
                extras.putString(AppConstants.ID, entity.uid)
            } else {
                extras.putString(AppConstants.ID, (entity as Group).guid)
            }

            if (call.type.equals(CometChatConstants.CALL_TYPE_VIDEO, ignoreCase = true)) {
                extras.putInt(
                    TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                    VideoProfile.STATE_BIDIRECTIONAL
                )
            } else {
                extras.putInt(
                    TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                    VideoProfile.STATE_AUDIO_ONLY
                )
            }

            extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telecomManager!!.isIncomingCallPermitted(phoneAccountHandle)
                }
                telecomManager!!.addNewIncomingCall(phoneAccountHandle, extras)
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
            }
        }
    }

    private fun renderTextMessageNotification(message: JSONObject) {
        try {
            if (message.getString(AppConstants.RECEIVER_TYPE) == CometChatConstants.RECEIVER_TYPE_USER) {
                if (message.has(AppConstants.SENDER_AVATAR)) {
                    showNotification(
                        message.getInt(AppConstants.TAG),
                        message.getString(AppConstants.SENDER_NAME),
                        message.getString(AppConstants.BODY),
                        message.getString(AppConstants.SENDER_AVATAR),
                        message
                    )
                } else {
                    showNotification(
                        message.getInt(AppConstants.TAG),
                        message.getString(AppConstants.SENDER_NAME),
                        message.getString(AppConstants.BODY),
                        "",
                        message
                    )
                }
            } else {
                if (message.has(AppConstants.SENDER_AVATAR)) {
                    showNotification(
                        message.getInt(AppConstants.TAG),
                        message.getString(AppConstants.RECEIVER_NAME),
                        message.getString(AppConstants.BODY),
                        message.getString(AppConstants.SENDER_AVATAR),
                        message
                    )
                } else {
                    showNotification(
                        message.getInt(AppConstants.TAG),
                        message.getString(AppConstants.RECEIVER_NAME),
                        message.getString(AppConstants.BODY),
                        "",
                        message
                    )
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    fun checkAccountConnection(context: Context?): Boolean {
        var isConnected = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED && telecomManager != null
            ) {
                val enabledAccounts = telecomManager!!.callCapablePhoneAccounts
                for (account in enabledAccounts) {
                    if (account.componentName.className == CallConnectionService::class.java.getCanonicalName()) {
                        isConnected = true
                        break
                    }
                }
            }
        }
        return isConnected
    }

    private fun showNotification(
        nid: Int,
        title: String,
        text: String,
        largeIconUrl: String,
        payload: JSONObject
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.MESSAGES,
                title,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Your messages!!"

            val notificationManager =
                context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notificationManager =
            context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context!!, AppConstants.MESSAGES
        )
        builder.setContentTitle(title)
        builder.setContentText(text)
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setAutoCancel(true)

        if (!TextUtils.isEmpty(largeIconUrl)) {
            builder.setLargeIcon(getBitmapFromURL(largeIconUrl))
        } else {
            builder.setLargeIcon(
                BitmapFactory.decodeResource(
                    context!!.resources,
                    R.drawable.ic_launcher_background
                )
            )
        }

        val intent = Intent(context, CometChatUiActivity::class.java)

        intent.putExtra(AppConstants.NOTIFICATION_PAYLOAD, payload.toString())
        val pendingIntent = PendingIntent.getActivity(
            context,
            501,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        notificationManager.notify(nid, notification)
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? {
        if (strURL != null) {
            try {
                val url = URL(strURL)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                return BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        } else {
            return null
        }
    }

    companion object {
        private val TAG: String = CometChatNotification::class.java.simpleName
        private var context: Context? = null
        private var cometChatNotification: CometChatNotification? = null
        private var notificationManager: NotificationManager? = null
        private var telecomManager: TelecomManager? = null
        private var phoneAccountHandle: PhoneAccountHandle? = null

        fun getInstance(c: Context): CometChatNotification? {
            if (cometChatNotification == null) {
                cometChatNotification = CometChatNotification()
                context = c
                notificationManager =
                    c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                //For VoIP
                telecomManager = context!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val componentName = ComponentName(context!!, CallConnectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    phoneAccountHandle = PhoneAccountHandle(componentName, context!!.packageName)
                    val phoneAccount =
                        PhoneAccount.builder(phoneAccountHandle, context!!.packageName)
                            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER).build()
                    telecomManager!!.registerPhoneAccount(phoneAccount)
                }
            }
            return cometChatNotification
        }


        private val isFirebaseAppInitialized: Boolean
            get() = FirebaseApp.getApps(context!!).isNotEmpty()
    }
}