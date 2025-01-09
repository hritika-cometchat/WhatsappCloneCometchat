package com.example.testapplication.Activity.Ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.users.UsersConfiguration
import com.example.testapplication.databinding.ActivityContactsBinding
import com.google.gson.Gson


class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Sort by user presence only users
        val usersRequest : UsersRequest.UsersRequestBuilder = UsersRequest.UsersRequestBuilder().apply {
            sortBy(CometChatConstants.SORT_BY_STATUS)
            build()
        }
        binding.contacts.usersConfiguration = UsersConfiguration().setUsersRequestBuilder(usersRequest)

        binding.contacts.setOnItemClickListener { _, user, group ->
            openMessages(user, group)
        }

    }

    private fun openMessages(user: User? = null, group: Group? = null) {
        val intent = Intent(this, MessagesActivity::class.java)
        user?.let {
            intent.putExtra("USER", Gson().toJson(it))
        }
        group?.let {
            intent.putExtra("GROUP", Gson().toJson(it))
        }
        startActivity(intent)
    }

}