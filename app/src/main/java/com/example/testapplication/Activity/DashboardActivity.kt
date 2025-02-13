package com.example.testapplication.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cometchat.calls.core.CallAppSettings.CallAppSettingBuilder
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChatNotifications
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.NotificationPreferences
import com.example.testapplication.AppConstants
import com.example.testapplication.AppConstants.CALL_RECEIVER_LISTENER
import com.example.testapplication.Fragment.CallLogFragment
import com.example.testapplication.Fragment.ConversationFragment
import com.example.testapplication.Fragment.GroupListFragment
import com.example.testapplication.R
import com.example.testapplication.databinding.ActivityDashboardBinding
import com.google.gson.Gson

class DashboardActivity : AppCompatActivity() {

    private val conversationFragment = ConversationFragment()
    private val callLogsFragment = CallLogFragment()
    private val groupFragment = GroupListFragment()
    private var isPrivacyTemplate = false

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fetchPreferences()
        supportActionBar?.elevation = 0F
        initFragment()
        initApp()
        initCall()
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.menuChats -> conversationFragment
                R.id.menuCalls -> callLogsFragment
                R.id.menuGroups -> groupFragment
                else -> return@setOnItemSelectedListener false
            }
            setCurrentFragment(fragment)
            true
        }
    }

    private fun initFragment() {
        val fragmentType = intent.getStringExtra("FRAGMENT_TYPE").orEmpty()
        val payload = intent.getStringExtra(AppConstants.NOTIFICATION_PAYLOAD).orEmpty()
        val convoFragment = conversationFragment.apply {
            if (payload.isNotEmpty()) {
                arguments = Bundle().apply {
                    putString(AppConstants.NOTIFICATION_PAYLOAD, payload)
                }
            }
        }
        when {
            fragmentType.contains("GroupFragment") -> {
                setCurrentFragment(groupFragment)
                binding.bottomNavigationView.selectedItemId = R.id.menuGroups
            }

            fragmentType.contains("CallFragment") -> {
                setCurrentFragment(callLogsFragment)
                binding.bottomNavigationView.selectedItemId = R.id.menuCalls
            }

            else -> setCurrentFragment(convoFragment)
        }
    }

    private fun initApp() {
        val appID: String = AppConstants.APP_ID
        val region: String = AppConstants.REGION

        val appSetting = AppSettings.AppSettingsBuilder()
            .setRegion(region)
            .subscribePresenceForAllUsers()
            .autoEstablishSocketConnection(true)
            .build()

        CometChat.init(this, appID, appSetting, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(p0: String?) {
                Log.d("INIT", "Initialization completed successfully")
            }

            override fun onError(p0: CometChatException?) {
                Log.d("INIT", "Initialization failed with exception: " + p0?.message)
            }
        }
        )
    }

    private fun initCall() {
        val callAppSettings = CallAppSettingBuilder()
            .setAppId(AppConstants.APP_ID)
            .setRegion(AppConstants.REGION)
            .build()

        CometChatCalls.init(
            this,
            callAppSettings,
            object : CometChatCalls.CallbackListener<String>() {
                override fun onSuccess(p0: String?) {
                    Log.i("CallApp", "$p0")
                }

                override fun onError(p0: com.cometchat.calls.exceptions.CometChatException?) {
                    Log.i("CallApp", "$p0")
                }
            })
    }

    private fun setCurrentFragment(fragment: Fragment) {
        title = when (fragment) {
            conversationFragment -> "WhatsApp"
            groupFragment -> "Groups"
            callLogsFragment -> "Calls"
            else -> "WhatsApp"
        }
        supportFragmentManager.beginTransaction().apply {
            replace(binding.flFragment.id, fragment)
            commit()
        }
    }

    private fun logout() {
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(p0: String?) {
                Toast.makeText(this@DashboardActivity, "Logout Successfully", Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(this@DashboardActivity, MainActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }

            override fun onError(p0: CometChatException?) {
                Toast.makeText(this@DashboardActivity, "Failure", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun receiveRealTimeCalls() {
        CometChat.addCallListener(CALL_RECEIVER_LISTENER, object : CometChat.CallListener() {
            override fun onOutgoingCallAccepted(call: Call?) {
            }

            override fun onIncomingCallReceived(call: Call?) {
                startActivity(Intent(this@DashboardActivity, CallActivity::class.java).apply {
                    putExtra("SESSION_ID", call?.sessionId)
                    putExtra("EXTRA_RECEIVE_CALL", true)
                    putExtra("EXTRA_INITIATE", Gson().toJson(call?.callInitiator))
                })
            }

            override fun onIncomingCallCancelled(p0: Call?) {

            }

            override fun onOutgoingCallRejected(p0: Call?) {

            }

            override fun onCallEndedMessageReceived(p0: Call?) {

            }
        })
    }

    private fun fetchPreferences() {
        CometChatNotifications.fetchPreferences(object :
            CometChat.CallbackListener<NotificationPreferences>() {
            override fun onSuccess(notificationPreferences: NotificationPreferences) {
                isPrivacyTemplate = notificationPreferences.usePrivacyTemplate
                invalidateOptionsMenu()
            }

            override fun onError(e: CometChatException) {
                Log.e("MutePreference", e.message.toString())
            }
        })
    }

    private fun fetchAndSetPrivacy(){
        CometChatNotifications.fetchPreferences(object :
            CometChat.CallbackListener<NotificationPreferences>() {
            override fun onSuccess(notificationPreferences: NotificationPreferences) {
                // Display a toggle for use privacy option
                val usePrivacyTemplate = notificationPreferences.usePrivacyTemplate
                Log.e("-------->", "$usePrivacyTemplate ${CometChat.getLoggedInUser()}")
                setPrivacyTemplate(usePrivacyTemplate)
            }

            override fun onError(e: CometChatException) {
                Log.e("-------->", "$e ${CometChat.getLoggedInUser()}")
            }
        })
    }


    private fun setPrivacyTemplate(isPrivacy : Boolean = false ) {
        val updatedPreferences = NotificationPreferences().apply {
//            usePrivacyTemplate = !isPrivacyTemplate
            Log.e("-------->updated", "$usePrivacyTemplate ${CometChat.getLoggedInUser()}")

            usePrivacyTemplate = !isPrivacy
        }
        CometChatNotifications.updatePreferences(
            updatedPreferences,
            object : CometChat.CallbackListener<NotificationPreferences?>() {
                override fun onSuccess(notificationPreferences: NotificationPreferences?) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "$notificationPreferences updated",
                        Toast.LENGTH_SHORT
                    ).show()
                    isPrivacyTemplate = notificationPreferences!!.usePrivacyTemplate
                    invalidateOptionsMenu()
                }

                override fun onError(e: CometChatException) {
                    Toast.makeText(this@DashboardActivity, "$e", Toast.LENGTH_SHORT).show()
                    Log.e("-------->", "$e ${CometChat.getLoggedInUser()}")
                }
            })
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        menu?.findItem(R.id.menuCamera)?.setVisible(false)
        val title = if (isPrivacyTemplate) getString(R.string.privacy_off) else getString(R.string.privacy_on)
        menu?.findItem(R.id.menuPrivacy)?.title = title
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuPrivacy -> fetchAndSetPrivacy()
            R.id.menuProfile -> startActivity(Intent(this, UserDetailsActivity::class.java))
            R.id.menuLogout -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_OK) {
            initFragment()
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