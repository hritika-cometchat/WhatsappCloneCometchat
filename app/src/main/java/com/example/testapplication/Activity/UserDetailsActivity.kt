package com.example.testapplication.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants.USER_STATUS_ONLINE
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import com.example.testapplication.AppConstants
import com.example.testapplication.R
import com.example.testapplication.databinding.ActivityUserDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private var idUG: String = ""
    private val PICK_IMAGE_REQUEST = 1
    private val PREVIEW_IMAGE_REQUEST = 123

    private var userInfo = User()
    private var TAG: String = "UserDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.elevation = 0f
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        idUG = intent.getStringExtra("EXTRA_ID").orEmpty()

        if (idUG.isNotEmpty()) {
            listenForMessages()
            userStatusIndicator()
            getUserDetails()
        } else {
            val user = CometChat.getLoggedInUser()
            userInfo = user
            updateUi(user, true)
        }

        binding.ivProfilePic.setOnClickListener {
            if (userInfo.avatar != null) {
                val intent = Intent(this, FullViewActivity::class.java)
                intent.putExtra("imageUrl", userInfo.avatar)
                startActivity(intent)
            }
        }

        binding.tvBlock.setOnClickListener {
            blockUsers()
        }
        binding.ivEditName.setOnClickListener {
            openBottomDialog(userInfo.name)
        }

        binding.ivUpload.setOnClickListener {
            openGallery()
        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openBottomDialog(userName: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_edit_bottom_sheet,null)
        val btnCancel = view.findViewById<TextView>(R.id.tvCancel)
        val btnSave = view.findViewById<TextView>(R.id.tvSave)
        val etUserName = view.findViewById<EditText>(R.id.etUserName)
        etUserName.setText(userName)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        btnSave.setOnClickListener {
            if (etUserName.text.toString().isEmpty()) {
                etUserName.error = "Field Required"
                return@setOnClickListener
            } else {
                dialog.dismiss()
                val user = User()
                user.name = etUserName.text.toString()
                updateLoggedInUser(user)
            }
        }
        dialog.setCancelable(false)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun blockUsers(){
        val uids = ArrayList<String>()
        uids.add("UID1")
        uids.add("UID2")
        uids.add("UID3")

        CometChat.blockUsers(uids,object:CometChat.CallbackListener<HashMap<String, String>>() {
            override fun onSuccess(resultMap: HashMap<String, String>) {
                // Handle unblock users success.
            }

            override fun onError(e: CometChatException) {
                // Handle unblock users failure
            }
        })
    }

    private fun getUserDetails() {
        CometChat.getUser(idUG, object :CometChat.CallbackListener<User>(){
            override fun onSuccess(user: User?) {
                user?.let {
                    userInfo = user
                    updateUi(it)
                }
            }

            override fun onError(e: CometChatException?) {
                Log.w("USerDetails", "$e")
            }
        })
    }

    private fun getGroupDetails(idG:String) {

        CometChat.getGroup(idG,object :CometChat.CallbackListener<Group>(){
            override fun onSuccess(group: Group?) {
                Log.d(TAG, "$group")
            }
            override fun onError(p0: CometChatException?) {
                Log.d(TAG, "Group details fetching failed with exception: " +p0?.message)
            }
        })
    }

    private fun updateUi(user: User, isCurrentUser : Boolean = false) {
        if (isCurrentUser){
            binding.rlStatus.visibility = View.GONE
            binding.tvUserName.visibility = View.GONE
            binding.tvUserUid.visibility = View.GONE
            binding.clContactType.visibility = View.GONE
            binding.ivUpload.visibility = View.VISIBLE
            binding.clSectionUser.visibility = View.VISIBLE
            binding.etUserName.text = user.name

        } else {
            binding.rlStatus.visibility = View.VISIBLE
            binding.tvUserName.visibility = View.VISIBLE
            binding.tvUserUid.visibility = View.VISIBLE
            binding.clContactType.visibility = View.VISIBLE
            binding.ivUpload.visibility = View.GONE
            binding.clSectionUser.visibility = View.GONE
            binding.tvUserName.text = user.name
            binding.tvUserUid.text = user.uid
            binding.tvUserStatus.text = user.status
            binding.tvUserStatus.text = if (user.status == USER_STATUS_ONLINE) {
                binding.tvUserStatus.setTextColor(getColor(R.color.green))
                "Online"
            } else {
                binding.tvUserStatus.setTextColor(getColor(R.color.secondaryTextColor))
                "Last Active At ${formatTime(user.lastActiveAt)}"
            }
        }
        Glide.with(this).load(user.avatar).placeholder(R.drawable.ic_avatar_placeholder)
            .into(binding.ivProfilePic)

    }

    private fun formatTime(epoch: Long): String {
        val date = Date(epoch * 1000)
        val format = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun listenForMessages() {
        CometChat.addMessageListener(
            AppConstants.MESSAGE_RECEIVE_LISTENER, object : CometChat.MessageListener() {
                override fun onTypingEnded(typingIndicator: TypingIndicator?) {
                    Log.d(TAG, "onTypingEnded: ${typingIndicator.toString()}")
                    binding.tvTypingStatus.visibility = View.GONE
                    binding.tvUserStatus.visibility = View.VISIBLE
                }

                override fun onTypingStarted(typingIndicator: TypingIndicator?) {
                    Log.d(TAG, "onTypingStarted: ${typingIndicator.toString()}")
                    if (typingIndicator != null) {
                        binding.tvTypingStatus.visibility = View.VISIBLE
                        binding.tvUserStatus.visibility = View.GONE
                    }
                }


            })
    }

    private fun userStatusIndicator() {
        CometChat.addUserListener(
            AppConstants.USER_STATUS_LISTENER,
            object : CometChat.UserListener() {
                override fun onUserOffline(user: User?) {
                    if (user != null) {
                        if (user.uid == idUG) {
                            binding.tvUserStatus.text = "Last Active At ${formatTime(user.lastActiveAt)}"
                            binding.tvUserStatus.setTextColor(getColor(R.color.secondaryTextColor))

                        }
                    }
                }

                override fun onUserOnline(user: User?) {
                    if (user != null) {
                        if (user.uid == idUG) {
                            binding.tvUserStatus.text = "Online"
                            binding.tvUserStatus.setTextColor(getColor(R.color.green))
                        }
                    }
                }
            })
    }

    private fun updateLoggedInUser(user : User) {
        CometChat.updateCurrentUserDetails(user, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Toast.makeText(this@UserDetailsActivity, "Updated Successfully", Toast.LENGTH_SHORT).show()
                updateUi(user, true)
            }

            override fun onError(e: CometChatException) {
                Log.d(TAG, e.message.toString())
            }
        })
    }

    private fun displayImage(uri: Uri) {
        val intent = Intent(this, ImagePreviewSendActivity::class.java)
        intent.putExtra("imageUrlDisplay", uri)
        startActivityForResult(intent, PREVIEW_IMAGE_REQUEST)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK){
            if (requestCode == PICK_IMAGE_REQUEST){
                val selectedImageUri = data?.data
                selectedImageUri?.let { displayImage(it) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeMessageListener(AppConstants.MESSAGE_RECEIVE_LISTENER)
        CometChat.removeMessageListener(AppConstants.USER_STATUS_LISTENER)
    }
}