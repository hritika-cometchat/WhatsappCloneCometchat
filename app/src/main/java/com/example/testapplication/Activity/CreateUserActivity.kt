package com.example.testapplication.Activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.Ui.CometChatUiActivity
import com.example.testapplication.ApiInterface
import com.example.testapplication.Model.AuthUser
import com.example.testapplication.Model.UserDataAuth
import com.example.testapplication.RetrofitInstance
import com.example.testapplication.databinding.ActivityCreateUserBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateUserBinding
    private lateinit var apiInterface: ApiInterface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        apiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)
        binding.button2.setOnClickListener {
            validateAndSaveData()
        }
    }

    private fun validateAndSaveData() {
        var error = false
        if (TextUtils.isEmpty(binding.etUid.text)) {
            binding.etUid.error = "Field Required"
            error = true
        }
        if (TextUtils.isEmpty(binding.etUname.text)) {
            binding.etUname.error = "Field Required"
            error = true
        }
        if (error) return

        val user = UserDataAuth().apply {
            uid = binding.etUid.text.trim().toString()
            name = binding.etUname.text.trim().toString()
            withAuthToken = true
        }

            apiInterface.createUserWithAuth(user).enqueue(object : Callback<AuthUser> {
                override fun onResponse(call: Call<AuthUser>, response: Response<AuthUser>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.i("RES:", response.body().toString())
                        val aUser = response.body()!!
                        aUser.data?.authToken?.let { it1 -> loginWithAuthToken(it1) }
                    } else Log.i("RES ERROR", response.errorBody().toString())
                }

                override fun onFailure(call: Call<AuthUser>, t: Throwable) {
                    t.printStackTrace()
                }
            })

    }

    private fun loginWithAuthToken(authToken: String) {
        CometChat.login(authToken, object :
            CometChat.CallbackListener<User>(){
            override fun onSuccess(user: User?) {
                Log.d("StatusLogin", "Login Successful : " + user?.toString())
                Toast.makeText(this@CreateUserActivity, "Success", Toast.LENGTH_LONG).show()
                if (user != null) {
                    openLoginUser()
                }
            }
            override fun onError(p0: CometChatException?) {
                Log.d("StatusLogin", "Login failed with exception: " +  p0?.message)
            }
        })
    }

    private fun openLoginUser() {
        val intent = Intent(this, CometChatUiActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}