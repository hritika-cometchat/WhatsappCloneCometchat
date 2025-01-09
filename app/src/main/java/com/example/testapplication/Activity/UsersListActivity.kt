package com.example.testapplication.Activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.core.UsersRequest.UsersRequestBuilder
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.example.testapplication.Adapter.UserListAdapter
import com.example.testapplication.databinding.ActivityUsersListBinding
import com.google.gson.Gson


class UsersListActivity : AppCompatActivity() {
    private val TAG = "UsersListActivity"
    private lateinit var adapter: UserListAdapter

    private lateinit var binding: ActivityUsersListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            elevation = 0F
            setDisplayHomeAsUpEnabled(true)
        }
        title = "Select User"

        adapter = UserListAdapter(this)
        binding.rvUsersList.layoutManager = LinearLayoutManager(this)
        binding.rvUsersList.adapter = adapter

        binding.rvUsersList

        getUsersList()

        adapter.setOnClickListener(object :
            UserListAdapter.OnClickListener {
            override fun onClick(position: Int, user: User) {
                val intent = Intent(this@UsersListActivity, ConversationMsgActivity::class.java).apply {
                    putExtra("EXTRA_ID", user.uid)
                    putExtra("EXTRA_SENDER", Gson().toJson(user))
                    putExtra("EXTRA_NEW_CHAT",true)
                    putExtra("EXTRA_CONVO_TYPE", CometChatConstants.CONVERSATION_TYPE_USER)

                }
                startActivity(intent)
            }
        })

        adapter.setOnHeaderClickListener(object : UserListAdapter.OnHeaderClickListener{
            override fun onAddGroupClicked() {
                //open group
            }

            override fun onAddUserClicked() {

            }
        })

    }

    private fun getUsersList() {
        val pd = ProgressDialog(this)
        pd.setMessage("loading")
        pd.show()
        val usersRequest: UsersRequest
        val limit = 30
        usersRequest = UsersRequestBuilder().setLimit(limit).build()
        usersRequest.fetchNext(object : CometChat.CallbackListener<MutableList<User>>() {
            override fun onSuccess(list: MutableList<User>) {
                pd.dismiss()
                supportActionBar?.subtitle = "Total Users ${list.size}"
                adapter.users = list
                binding.tvEmpty.text = ""
                adapter.notifyDataSetChanged()
            }

            override fun onError(e: CometChatException) {
                pd.dismiss()
                binding.tvEmpty.text = e.message
                Log.d(TAG, "User list fetching failed with exception: " + e.message)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home)
        {
            finish()
            true
        } else super.onOptionsItemSelected(item)
    }
}