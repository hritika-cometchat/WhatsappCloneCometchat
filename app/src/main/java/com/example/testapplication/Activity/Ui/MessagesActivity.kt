package com.example.testapplication.Activity.Ui

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Layout.Alignment
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.messagecomposer.MessageComposerStyle
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.views.CometChatMessageInput.MessageInputStyle
import com.example.testapplication.R
import com.example.testapplication.databinding.ActivityMessagesBinding
import com.example.testapplication.databinding.AuxiliaryButtonLayoutBinding
import com.google.gson.Gson

class MessagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessagesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent.hasExtra("USER")) {
            val userJson = intent.getStringExtra("USER")
            Gson().fromJson(userJson, User::class.java)?.let {
                binding.message.setUser(it)
            }
        } else if (intent.hasExtra("GROUP")) {
            val userJson = intent.getStringExtra("GROUP")
            Gson().fromJson(userJson, Group::class.java)?.let {
                binding.message.setGroup(it)
            }
        } else finish()


        binding.message.setAuxiliaryHeaderMenu { context, user, group ->
            val auxiliaryButtonLayoutBinding: AuxiliaryButtonLayoutBinding = AuxiliaryButtonLayoutBinding.inflate(LayoutInflater.from(this))
            auxiliaryButtonLayoutBinding.ivAudio.setOnClickListener {
                Toast.makeText(context, "Clicked on payment option", Toast.LENGTH_SHORT).show()
            }
            auxiliaryButtonLayoutBinding.root
        }
    }
}