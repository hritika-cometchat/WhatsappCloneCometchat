package com.example.testapplication.Activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.testapplication.Activity.BaseApplication.Companion
import com.example.testapplication.AppConstants
import com.example.testapplication.AppConstants.CALL_RECEIVER_LISTENER
import com.example.testapplication.R
import com.example.testapplication.Utils
import com.google.android.gms.tasks.OnCompleteListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    private var token = ""
    companion object {
        val LISTENER_ID = CALL_RECEIVER_LISTENER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.elevation = 0F
        initUiKit()

//        FirebaseInstanceId.getInstance().getInstanceId()
//            .addOnCompleteListener(object : OnCompleteListener<InstanceIdResult?>() {
//                fun onComplete(task: com.google.android.gms.tasks.Task<InstanceIdResult>) {
//                    if (!task.isSuccessful()) {
//                        return
//                    }
//                    token = task.getResult().getToken()
//                    cometNotify()
//                    //CometChat.registerTokenForPushNotification(token,CometChat.CallbackListener<String?>())
//                }
//            })

    }

    private fun cometNotify(){
        CometChat.registerTokenForPushNotification(token, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(s: String?) {
                if (s != null) {
                    Log.e("onSuccessPN: ", s)
                }
            }

            override fun onError(e: CometChatException) {
                e.message?.let { Log.e("onErrorPN: ", it) }
            }
        })
    }

    private fun initUiKit() {
        val appID = AppConstants.APP_ID
        val region = AppConstants.REGION

        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setRegion(region)
            .setAppId(appID)
            .subscribePresenceForAllUsers().build()

        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(successString: String?) {
                Log.d("INIT", "Ui Kit Initialization completed successfully")
            }

            override fun onError(e: CometChatException?) {}
        })
    }

    fun showToast(resId: Int)
    {
        showToast(getString(resId))
    }

    fun showToast(message: String)
    {
        Utils.showToast(this, message)
    }

    fun formatTime(time: Long): String {
        val date = Date(time * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}