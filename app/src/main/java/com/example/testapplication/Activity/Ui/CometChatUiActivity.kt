package com.example.testapplication.Activity.Ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChatNotifications
import com.cometchat.chat.enums.PushPlatforms
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.theme.Palette
import com.example.testapplication.Activity.BaseActivity
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.AppConstants
import com.example.testapplication.CometChatNotification
import com.example.testapplication.Fragment.ui.CallsLogFragment
import com.example.testapplication.Fragment.ui.ConversationsWithMessagesFragment
import com.example.testapplication.Fragment.ui.GroupsWithMessagesFragment
import com.example.testapplication.Fragment.ui.UsersWithMessagesFragment
import com.example.testapplication.R
import com.example.testapplication.databinding.ActivityCometChatUiBinding
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson

class CometChatUiActivity : BaseActivity() {
    private var TAG = "CometChatUiActivity"
    private lateinit var binding: ActivityCometChatUiBinding
    private var cometChatNotification: CometChatNotification? = null
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.MANAGE_OWN_CALLS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )
    private val PERMISSION_REQUEST_CODE = 99


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCometChatUiBinding.inflate(layoutInflater)
        cometChatNotification = CometChatNotification.getInstance(this)
        setContentView(binding.root)
        loadTheme()
        loadFragment(ConversationsWithMessagesFragment())
        requestPermissions()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val pushToken = task.result
                CometChatNotifications.registerPushToken(
                    pushToken!!,
                    PushPlatforms.FCM_ANDROID,
                    AppConstants.FCM_PROVIDER_ID,
                    object : CometChat.CallbackListener<String>() {
                        override fun onSuccess(s: String) {
                            Log.v(TAG, "onSuccess:  CometChat Notification Registered : $s")
                        }

                        override fun onError(e: CometChatException) {
                        }
                    })
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.chat -> loadFragment(ConversationsWithMessagesFragment())
                R.id.user -> loadFragment(UsersWithMessagesFragment())
                R.id.group -> loadFragment(GroupsWithMessagesFragment())
                R.id.call -> loadFragment(CallsLogFragment())
            }
            true
        }

        binding.bottomNavigationView.setSelectedItemId(R.id.chat)
    }

    private fun loadFragment(fragment: Fragment?) {
        if (fragment != null) supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment).commit()
    }

    private fun loadTheme() {
        val palette = Palette.getInstance()
        palette.background(Color.WHITE)
        palette.primary(Color.parseColor("#1EBE71"))
        palette.mode(CometChatTheme.MODE.LIGHT)
    }

    override fun onResume() {
        super.onResume()
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
//                startActivity(Intent(this@CometChatUiActivity, CallActivity::class.java).apply {
//                    putExtra("SESSION_ID", call.sessionId)
//                    putExtra("EXTRA_RECEIVE_CALL", true)
//                    putExtra("EXTRA_INITIATE", Gson().toJson(call.callInitiator))
//                })
            }

            override fun onOutgoingCallAccepted(call: Call) {
//                val isAudio = call.type == CometChatConstants.CALL_TYPE_AUDIO
//                val sessionId = call.sessionId
//                val intent = Intent(this@CometChatUiActivity, UiCallActivity::class.java)
//                intent.putExtra("KEY_SESSION_ID", sessionId)
//                intent.putExtra("IS_AUDIO_CALL", isAudio)
//                startActivity(intent)
            }

            override fun onOutgoingCallRejected(call: Call) {
                // To be implemented
            }

            override fun onIncomingCallCancelled(call: Call) {
                // To be implemented
            }
        })

    }

    private fun requestPermissions() {
        //Required Permission
        requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)

        //For VOIP
        if (!cometChatNotification?.checkAccountConnection(this)!!) {
            val alertDialog = android.app.AlertDialog.Builder(this)
            alertDialog.setTitle(R.string.cometchat_VoIP_permission)
            alertDialog.setMessage(R.string.cometchat_VoIP_message)
            alertDialog.setPositiveButton(R.string.cometchat_VoIP_openSettings) { dialog, which ->
                launchVoIPSetting(this@CometChatUiActivity)
            }
            alertDialog.setNegativeButton(R.string.cometchat_VoIP_cancel) { dialog, which -> dialog.dismiss() }
            alertDialog.create().show()
        }
    }

    fun launchVoIPSetting(context: Context) {
        val intent = Intent()
        intent.setAction(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        val telecomComponent = ComponentName(
            getString(R.string.cometchat_android_telecom_package),
            getString(R.string.cometchat_android_telecom_package_class)
        )
        intent.setComponent(telecomComponent)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }


    override fun onPause() {
        super.onPause()
        CometChat.removeCallListener(LISTENER_ID)

    }


}