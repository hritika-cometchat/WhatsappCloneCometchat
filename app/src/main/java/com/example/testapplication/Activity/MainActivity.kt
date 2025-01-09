package com.example.testapplication.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.testapplication.Activity.Ui.CometChatUiActivity
import com.example.testapplication.ApiInterface
import com.example.testapplication.AppConstants
import com.example.testapplication.CometChatNotification
import com.example.testapplication.Model.UserAuthToken
import com.example.testapplication.RetrofitInstance
import com.example.testapplication.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : BaseActivity() {

    private var isCameraPermissionGranted = false
    private var isAudioPermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiInterface: ApiInterface
    var user: User? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initApp()
//        initUiKit()
        supportActionBar?.hide()

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isAudioPermissionGranted =
                    permissions[Manifest.permission.RECORD_AUDIO] ?: isAudioPermissionGranted
                isCameraPermissionGranted =
                    permissions[Manifest.permission.CAMERA] ?: isCameraPermissionGranted
            }

        requestPermission()
        askNotificationPermission()
        user = CometChat.getLoggedInUser()
        if (user != null) {
            openLoginUser()
            return
        }
        apiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)

        binding.btnAddUser.setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            createAuthToken()
        }
    }

    private fun loginWithUiKit() {
        val UID = binding.etUid.text.trim().toString()
        if (TextUtils.isEmpty(UID)) {
            binding.txtInputL.error = "Field Required"
            return
        } else binding.txtInputL.error = null

        CometChatUIKit.login(UID, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_LONG).show()
                openLoginUser()
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Login Failed : " + e.message)
            }
        })

    }

    private fun requestPermission() {
        isAudioPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        isCameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val permissionRequest: MutableList<String> = ArrayList()
        if (!isAudioPermissionGranted) permissionRequest.add(Manifest.permission.RECORD_AUDIO)
        if (!isCameraPermissionGranted) permissionRequest.add(Manifest.permission.CAMERA)

        if (permissionRequest.isNotEmpty()) permissionLauncher.launch(permissionRequest.toTypedArray())

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
        })
    }

    private fun createAuthToken() {
        val UID = binding.etUid.text.trim().toString()
        if (TextUtils.isEmpty(UID)) {
            binding.txtInputL.error = "Field Required"
            return
        } else binding.txtInputL.error = null

        apiInterface.createAuthToken(UID).enqueue(object : Callback<UserAuthToken> {
            override fun onResponse(call: Call<UserAuthToken>, response: Response<UserAuthToken>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.i("RES:", response.body().toString())
                    val userAuth = response.body()!!
                    loginWithAuthToken(userAuth.data!!.authToken)
                } else {
                    if (response.code() == 404) {
                        Toast.makeText(this@MainActivity, "User $UID Not Found", Toast.LENGTH_LONG)
                            .show()
                        binding.etUid.setText("")
                    }
                    Log.i("RES ERROR:", response.errorBody().toString())
                }
            }

            override fun onFailure(call: Call<UserAuthToken>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun loginWithAuthToken(authToken: String) {
        CometChat.login(authToken, object : CometChat.CallbackListener<User>() {
//        CometChatUIKit.loginWithAuthToken(authToken, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                Log.d("StatusLogin", "Login Successful : " + user?.toString())
                if (user != null) {
                    Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_LONG).show()
                    CometChatNotification.getInstance(this@MainActivity)
                        ?.registerCometChatNotification(object :
                            CometChat.CallbackListener<String?>() {
                            override fun onSuccess(s: String?) {
                                openLoginUser()

                            }

                            override fun onError(e: CometChatException) {
                            }
                        })
                }
            }

            override fun onError(p0: CometChatException?) {
                Log.d("StatusLogin", "Login failed with exception: " + p0?.message)
            }
        })
    }

    private fun openLoginUser() {
//        val intent = Intent(this, CometChatUiActivity::class.java).apply {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            Toast.makeText(
                this@MainActivity,
                "Notification will not be received",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Display an educational UI
                AlertDialog.Builder(this)
                    .setTitle("Notification Permission Needed")
                    .setMessage("Our app uses notifications to keep you updated with the latest features and important alerts. Granting this permission will enable these notifications.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("No thanks") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


}