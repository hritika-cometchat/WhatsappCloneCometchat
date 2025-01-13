package com.example.testapplication.Activity

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.constants.CometChatConstants.CONVERSATION_TYPE_GROUP
import com.cometchat.chat.constants.CometChatConstants.MESSAGE_TYPE_IMAGE
import com.cometchat.chat.constants.CometChatConstants.RECEIVER_TYPE_GROUP
import com.cometchat.chat.constants.CometChatConstants.USER_STATUS_ONLINE
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import com.example.testapplication.AppConstants
import com.example.testapplication.AppConstants.CALL_RECEIVER_LISTENER
import com.example.testapplication.R
import com.example.testapplication.databinding.ActivityConversationMsgBinding
import com.example.testapplication.databinding.CustomDialogEditBinding
import com.example.testapplication.databinding.CustomDialogLayoutBinding
import com.example.testapplication.databinding.LayoutCustomActionBarBinding
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationMsgActivity : AppCompatActivity() {

    private val TAG = "ConversationActivity"
    private val PICK_IMAGE_REQUEST = 1
    private val PREVIEW_IMAGE_REQUEST = 123

    private var isAudioPermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isNotificationPermissionGranted = false


    private var conversation: Conversation? = null
    private var adapter: MessageAdapter? = null
    private var idUG: String = ""
    private var receiverType: String = ""
    private var conversationType: String = ""
    private var newChat = false
    private var senderG = Group()
    private var senderU = User()
    private lateinit var actionBarView: View
    private var actionMode: ActionMode? = null
    private var selectedMessages = mutableListOf<BaseMessage>()
    private val categories = listOf("message", "custom", "call")



    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: ActivityConversationMsgBinding
    private lateinit var actionBarBinding: LayoutCustomActionBarBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationMsgBinding.inflate(layoutInflater)
        actionBarBinding = LayoutCustomActionBarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setDisplayShowCustomEnabled(true)
            customView = actionBarBinding.root
            setDisplayHomeAsUpEnabled(true)
            actionBarView = customView
        }



        if (intent.hasExtra("EXTRA_CONVO")) {
            conversation =
                Gson().fromJson(intent.getStringExtra("EXTRA_CONVO"), Conversation::class.java)
        }
        newChat = intent.getBooleanExtra("EXTRA_NEW_CHAT", false)
        conversationType =
            intent.getStringExtra("EXTRA_CONVO_TYPE") ?: conversation?.conversationType.orEmpty()
        adapter = MessageAdapter(this)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        if (intent.hasExtra("EXTRA_ID")) {
            idUG = intent.getStringExtra("EXTRA_ID").orEmpty()
        }
        if (newChat) getSingleConversation(conversationType) else getMessageHistory(idUG)

        receiverType = when (conversationType) {
            "group" -> RECEIVER_TYPE_GROUP
            else -> CometChatConstants.RECEIVER_TYPE_USER
        }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isAudioPermissionGranted =
                    permissions[Manifest.permission.RECORD_AUDIO] ?: isAudioPermissionGranted
                isCameraPermissionGranted =
                    permissions[Manifest.permission.CAMERA] ?: isCameraPermissionGranted
                isNotificationPermissionGranted =
                    permissions[Manifest.permission.POST_NOTIFICATIONS]
                        ?: isNotificationPermissionGranted

                if (isPermissionsGranted()) {
                    startCallActivity(CometChatConstants.CALL_TYPE_AUDIO, conversationType)
                } else {
                    showPermissionsDeniedMessage()
                }
            }

        adapter?.setOnClickListener(object : MessageAdapter.OnClickListener {
            override fun onClick(position: Int, baseMessage: BaseMessage) {
                if (actionMode != null) {
                    toggleSelection(baseMessage, position)
                } else return
            }
        })

        adapter?.setOnLongClickListener(object : MessageAdapter.OnLongClickListener {
            override fun onClick(position: Int, baseMessage: BaseMessage) {
                if ((baseMessage.receiver as User).uid == idUG) {
                    startActionMode()
                    toggleSelection(baseMessage, position)
                } else return
            }
        })

        actionBarBinding.rlUserInfo.setOnClickListener {
            startActivity(Intent(this, UserDetailsActivity::class.java).apply {
                putExtra("EXTRA_ID", idUG)
            })
        }



        binding.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (dy < 0 && firstVisibleItem == 0) {
                    getMoreMessageHistory(idUG)
                }
            }
        })

        binding.ivAttach.setOnClickListener {
            openGallery()
        }
        binding.ivSend.setOnClickListener {
            val msg = binding.etChatMessage.text.trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg.toString())
            } else {
                binding.etChatMessage.error = "Field Required"
                return@setOnClickListener
            }
        }
        binding.etChatMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    val typingIndicator = TypingIndicator(idUG, receiverType)
                    CometChat.startTyping(typingIndicator)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    val typingIndicator = TypingIndicator(idUG, receiverType)
                    CometChat.endTyping(typingIndicator)
                }
            }

        })

        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.msg_call_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_audio -> startAudioCall()
            R.id.menu_video -> startVideoCall()
            android.R.id.home -> {
                if (newChat) {
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        if (conversationType == CONVERSATION_TYPE_GROUP) {
                            putExtra("FRAGMENT_TYPE", "GroupFragment")
                        }
                    }
                    startActivity(intent)
                } else {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startAudioCall() {
        if (isPermissionsGranted()) {
            startCallActivity(CometChatConstants.CALL_TYPE_AUDIO, conversationType)
        } else {
            requestPermissions()
        }
    }

    private fun startVideoCall() {
        if (isPermissionsGranted()) {
            startCallActivity(CometChatConstants.CALL_TYPE_VIDEO, conversationType)
        } else {
            requestPermissions()
        }
    }

    private fun updateUi() {
        val senderJson = intent.getStringExtra("EXTRA_SENDER")
        if (senderJson.isNullOrEmpty()) return

        when (conversationType) {
            "group" -> setupGroupUi(senderJson)
            else -> setupUserUi(senderJson)
        }
    }

    private fun setupUserUi(senderJson: String) {
        senderU = Gson().fromJson(senderJson, User::class.java)
        actionBarBinding.tvProfileName.text = senderU.name
        actionBarBinding.tvUserPresence.text = if (senderU.status == USER_STATUS_ONLINE) {
            actionBarBinding.ivOnline.visibility = View.VISIBLE
            USER_STATUS_ONLINE
        } else {
            actionBarBinding.ivOnline.visibility = View.GONE
            "Last Active At ${formatTime(senderU.lastActiveAt)}"
        }
        Glide.with(this).load(senderU.avatar).placeholder(R.drawable.ic_avatar_placeholder)
            .into(actionBarBinding.ivProfile)
    }

    private fun setupGroupUi(senderJson: String) {
        actionBarBinding.ivOnline.visibility = View.GONE
        senderG = Gson().fromJson(senderJson, Group::class.java)
        actionBarBinding.tvProfileName.text = senderG.name
        actionBarBinding.tvUserPresence.visibility = View.GONE
        Glide.with(this).load(senderG.icon).placeholder(R.drawable.ic_avatar_placeholder)
            .into(actionBarBinding.ivProfile)
    }

    private fun sendMessage(msg: String) {
        val textMessage = TextMessage(idUG, msg, receiverType)
        binding.ivSend.isEnabled = false
        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(textMessage: TextMessage) {
                adapter?.addMessage(textMessage)
                binding.etChatMessage.setText("")
                scrollDown()
                binding.ivSend.isEnabled = true
            }

            override fun onError(e: CometChatException) {
                binding.ivSend.isEnabled = true
                Log.d(TAG, "Message sending failed with exception: " + e.message)
            }
        })
    }

    private fun deleteMessage(message: BaseMessage) {
        CometChat.deleteMessage(message.id, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(msg: BaseMessage) {
                adapter?.deleteMessage(msg)
                selectedMessages.remove(msg)
                if (selectedMessages.isEmpty()) {
                    actionMode?.finish()
                }
            }

            override fun onError(p0: CometChatException?) {
                Log.e("DeleteMsg", "$p0")
            }

        })
    }

    private fun editMessage(message: BaseMessage) {
        CometChat.editMessage(message, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(message: BaseMessage) {
                adapter?.editMessage(message)
                Log.d(TAG, "editMessage onSuccess: $message")
            }

            override fun onError(e: CometChatException) {
                Toast.makeText(this@ConversationMsgActivity, "$e", Toast.LENGTH_SHORT).show()

            }
        })
    }

    private fun sendMediaMsg(uri: Uri, msgType: String) {
        val file = uriToFile(uri)
        val mediaMessage = MediaMessage(idUG, file, msgType, receiverType)
        CometChat.sendMediaMessage(
            mediaMessage,
            object : CometChat.CallbackListener<MediaMessage>() {
                override fun onSuccess(mediaMessage: MediaMessage) {
                    adapter?.addMessage(mediaMessage)
                    scrollDown()
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "Media message sending failed with exception: ${e.message}")
                }
            })
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun handleMessageReceived(message: BaseMessage) {
        CometChat.markAsDelivered(message)
        CometChat.markAsRead(message)
        adapter?.addMessage(message)
        scrollDown()
    }

    private fun listenForMessages() {
        CometChat.addMessageListener(
            AppConstants.MESSAGE_RECEIVE_LISTENER, object : CometChat.MessageListener() {
                override fun onTextMessageReceived(message: TextMessage?) {
                    message?.let { handleMessageReceived(it) }
                }

                override fun onMediaMessageReceived(mMessage: MediaMessage?) {
                    mMessage?.let { handleMessageReceived(it) }
                }

                override fun onCustomMessageReceived(cMessage: CustomMessage?) {
                    cMessage?.let { handleMessageReceived(it) }
                }

                override fun onMessageDeleted(msg: BaseMessage) {
                    adapter?.deleteMessage(msg)
                }

                override fun onMessagesDelivered(messageReceipt: MessageReceipt) {
                    adapter?.markDelivered(messageReceipt)
                }

                override fun onMessagesDeliveredToAll(messageReceipt: MessageReceipt?) {
                    messageReceipt?.let { adapter?.markDelivered(it) }
                }

                override fun onMessagesReadByAll(messageReceipt: MessageReceipt?) {
                    messageReceipt?.let { adapter?.markRead(it) }
                }

                override fun onMessagesRead(messageReceipt: MessageReceipt) {
                    adapter?.markRead(messageReceipt)
                }

                override fun onTypingEnded(typingIndicator: TypingIndicator?) {
                    actionBarBinding.tvTypingStatus.visibility = View.GONE
                    actionBarBinding.tvUserPresence.visibility = View.VISIBLE
                }

                override fun onTypingStarted(typingIndicator: TypingIndicator?) {
                    Log.d(TAG, "onTypingStarted: ${typingIndicator.toString()}")
                    if (typingIndicator != null) {
                        actionBarBinding.tvTypingStatus.visibility = View.VISIBLE
                        actionBarBinding.tvUserPresence.visibility = View.GONE
                    }
                }


            })
    }

    private fun userStatusIndicator() {
        CometChat.addUserListener(AppConstants.USER_STATUS_LISTENER,
            object : CometChat.UserListener() {
                override fun onUserOffline(user: User?) {
                    if (user != null) {
                        if (user.uid == idUG) {
                            actionBarBinding.ivOnline.visibility = View.GONE
                            actionBarBinding.tvUserPresence.text =
                                "Last Active At ${formatTime(user.lastActiveAt)}"
                        }
                    }
                }

                override fun onUserOnline(user: User?) {
                    if (user != null) {
                        if (user.uid == idUG) {
                            actionBarBinding.ivOnline.visibility = View.VISIBLE
                            actionBarBinding.tvUserPresence.text = USER_STATUS_ONLINE
                        }
                    }
                }
            })
    }

    private fun receiveRealTimeCalls() {
        CometChat.addCallListener(CALL_RECEIVER_LISTENER, object : CometChat.CallListener() {
            override fun onOutgoingCallAccepted(p0: Call?) {

            }

            override fun onIncomingCallReceived(call: Call?) {
                startActivity(Intent(this@ConversationMsgActivity, CallActivity::class.java).apply {
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


    private fun getMessageHistory(id: String) {
        adapter?.messages?.clear()
        val messagesRequest = if (conversation?.conversationType == "group") {
            MessagesRequest.MessagesRequestBuilder().setLimit(15).setGUID(id)
                .setCategories(categories)
                .hideDeletedMessages(true).build()
        } else {
            MessagesRequest.MessagesRequestBuilder().setLimit(15).setUID(id)
                .setCategories(categories)
                .hideDeletedMessages(true).build()
        }

        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage?>>() {
            override fun onSuccess(list: List<BaseMessage?>) {
                val filteredMessages = list.filter { it?.deletedAt == 0L || it?.deletedAt == null }
                    .filterNotNull()
                adapter?.loadMessages(filteredMessages.toMutableList())
                filteredMessages
                    .asSequence()
                    .filter { it.readAt == 0L }
                    .forEach {
                        println(it)
                        CometChat.markAsRead(it)
                    }

                conversation?.unreadMessageCount = 0
                scrollDown()
            }

            override fun onError(e: CometChatException) {
                Log.d(TAG, "Message fetching failed with exception: ${e.message}")
            }
        })
    }

    var isLoading = false
    private fun getMoreMessageHistory(id: String) {
        if (isLoading) return
        isLoading = true
        val lastMsg = adapter?.messages?.firstOrNull()?.id ?: return
        val messagesRequest = if (conversation?.conversationType == "group") {
            MessagesRequest.MessagesRequestBuilder().setLimit(15).setGUID(id).setMessageId(lastMsg)
                .setCategories(categories)
                .build()
        } else {
            MessagesRequest.MessagesRequestBuilder().setLimit(15).setUID(id).setMessageId(lastMsg)
                .setCategories(categories)
                .build()
        }
        binding.pbLoader.visibility = View.VISIBLE
        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage?>>() {
            override fun onSuccess(list: List<BaseMessage?>) {
                binding.pbLoader.visibility = View.GONE
                isLoading = false
                val filteredMessages = list.filter { it?.deletedAt == 0L || it?.deletedAt == null }
                    .filterNotNull()
                adapter?.loadMessages(filteredMessages.toMutableList())
                filteredMessages.forEach { message ->
                    Log.e(TAG, "Media message received: $message")
                }
//                binding.rvChat.smoothScrollToPosition(0)
            }

            override fun onError(e: CometChatException) {
                binding.pbLoader.visibility = View.GONE
                isLoading = false
                Log.d(TAG, "Message fetching failed with exception: ${e.message}")
            }
        })
    }

    private fun getSingleConversation(conversationType: String) {
        val pd = ProgressDialog(this);
        pd.setMessage("loading");
        pd.show()
        CometChat.getConversation(idUG, conversationType,
            object : CometChat.CallbackListener<Conversation?>() {
                override fun onSuccess(newConversation: Conversation?) {
                    pd.dismiss()
                    newConversation?.let { conversation = it }
                    getMessageHistory(idUG)
                }

                override fun onError(e: CometChatException) {
                    pd.dismiss()
                    Log.i("$TAG Error", e.message.toString())

                }
            })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun displayImage(uri: Uri) {
        val intent = Intent(this, ImagePreviewSendActivity::class.java)
        intent.putExtra("imageUrlDisplay", uri)
        val senderName = when (conversation?.conversationType) {
            "group" -> senderG.name
            else -> senderU.name
        }
        intent.putExtra("senderName", senderName)
        startActivityForResult(intent, PREVIEW_IMAGE_REQUEST)
    }

    private fun scrollDown() {
        binding.rvChat.scrollToPosition((adapter?.itemCount?.minus(1) ?: 0))
    }

    private fun formatTime(epoch: Long): String {
        val date = Date(epoch * 1000)
        val format = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun formatTimeHHmm(time: Long): String {
        val date = Date(time * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun isPermissionsGranted(): Boolean {
        isAudioPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        isCameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        isNotificationPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        return isAudioPermissionGranted && isCameraPermissionGranted && isNotificationPermissionGranted
    }


    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!isAudioPermissionGranted) permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (!isCameraPermissionGranted) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!isNotificationPermissionGranted) permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun showPermissionsDeniedMessage() {
        Toast.makeText(
            this,
            "Camera and Audio permissions are required to make a call",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startCallActivity(callType: String, receiverType: String) {
        startActivity(Intent(this, CallActivity::class.java).apply {
            putExtra("EXTRA_ID", idUG)
            putExtra("EXTRA_RECEIVER_TYPE", receiverType)
            if (receiverType == RECEIVER_TYPE_GROUP) {
                sendCustomMessage()
                putExtra("EXTRA_START_CALL", true)
            } else putExtra("EXTRA_INITIATE_CALL", true)
            putExtra("EXTRA_CALL_TYPE", callType)
            putExtra("SESSION_ID", conversation?.conversationId)
            if (conversationType == CONVERSATION_TYPE_GROUP) {
                putExtra("EXTRA_RECEIVER", Gson().toJson(senderG))
            } else putExtra("EXTRA_RECEIVER", Gson().toJson(senderU))
        })
    }

    private fun sendCustomMessage() {
        val customData = JSONObject().apply {
            put("sessionId", conversation?.conversationId)
            put("receiverType", receiverType)
            put("status", "Initiated")
        }
        val customMessage = CustomMessage(idUG, receiverType, "Meeting", customData)
        CometChat.sendCustomMessage(
            customMessage,
            object : CometChat.CallbackListener<CustomMessage>() {
                override fun onSuccess(customMessage: CustomMessage) {
                    Log.d(TAG, customMessage.toString())
                }

                override fun onError(e: CometChatException) {
                    Log.d(TAG, e.message!!)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { displayImage(it) }
                }

                PREVIEW_IMAGE_REQUEST -> {
                    val imageUri = data?.getParcelableExtra<Uri>("imageUrlRetrieve")
                    imageUri?.let { sendMediaMsg(imageUri, MESSAGE_TYPE_IMAGE) }
                }
            }
        }
    }

    private fun toggleSelection(cItem: BaseMessage, position: Int) {
        if (selectedMessages.contains(cItem)) {
            selectedMessages.remove(cItem)
            adapter?.notifyItemChanged(position)
        } else {
            selectedMessages.add(cItem)
            adapter?.notifyItemChanged(position)
        }
        adapter?.toggleSelection(position)
        if (selectedMessages.isEmpty()) actionMode?.finish() else updateActionMode()
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = startActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    mode?.menuInflater?.inflate(R.menu.menu_action_mode_edit_delete, menu)
                    menu?.findItem(R.id.menu_mute)?.setVisible(false)
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    return when (item?.itemId) {
                        R.id.menu_delete -> {
                            showAlertDelete()
                            true
                        }
                        R.id.menu_edit -> {
                            showEditDialog()
                            true
                        }
                        else -> false
                    }
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    selectedMessages.clear()
                    adapter?.clearSelections()
                    adapter?.notifyDataSetChanged()
                    actionMode = null
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    return true
                }
            })
        }
    }

    private fun updateActionMode() {
        actionMode?.title = "${selectedMessages.size} Selected"
        actionMode?.menu?.let { menu ->
            val menuEditItem = menu.findItem(R.id.menu_edit)
            menuEditItem.isVisible = selectedMessages.size <= 1
        }
        adapter?.notifyDataSetChanged()
    }


    private fun showAlertDelete() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnYes = view.findViewById<Button>(R.id.btnYes)
        builder.setView(view)
        btnCancel.setOnClickListener {
            builder.dismiss()
            actionMode?.finish()
        }
        btnYes.setOnClickListener {
            builder.dismiss()
            deleteSelectedMessages()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun showAlertEdit(msg : BaseMessage, editMsgBuilder : AlertDialog){
        val bindingAlertEdit = CustomDialogLayoutBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()

        bindingAlertEdit.dialogMessage.text = "Are you sure to edit the message?"
        bindingAlertEdit.btnCancel.setOnClickListener {
            builder.dismiss()
            actionMode?.finish()
        }
        bindingAlertEdit.btnYes.setOnClickListener {
            editMessage(msg)
            builder.dismiss()
            editMsgBuilder.dismiss()
            actionMode?.finish()
        }
        builder.let {
            it.setView(bindingAlertEdit.root)
            it.setCanceledOnTouchOutside(false)
            it.show()
        }
    }

    private fun showEditDialog() {
        val msg = selectedMessages.first()
        if (msg is TextMessage) {
            val bindingEditDialog = CustomDialogEditBinding.inflate(layoutInflater)
            val builder = AlertDialog.Builder(this,R.style.CustomAlertDialog).create()

            bindingEditDialog.ivBack.setOnClickListener {
                builder.dismiss()
                actionMode?.finish()
            }

            bindingEditDialog.ivSend.setOnClickListener {
                msg.text = bindingEditDialog.etChatMessage.text.toString()
                if (bindingEditDialog.etChatMessage.text.isNullOrEmpty()){
                    bindingEditDialog.etChatMessage.error = "Field Required"
                    return@setOnClickListener
                } else showAlertEdit(msg, builder)

            }

            bindingEditDialog.etChatMessage.setText(msg.text)
            bindingEditDialog.tvChatMeMsg.text = msg.text
            bindingEditDialog.tvTimestampMe.text = formatTimeHHmm(msg.sentAt)

            val imgRes = if (msg.deliveredAt > 0) {
                if (msg.readAt > 0) R.drawable.ic_read_all else R.drawable.ic_done_all
            } else R.drawable.ic_send

            bindingEditDialog.ivMsgReceipt.setImageResource(imgRes)
            builder.let {
                it.setView(bindingEditDialog.root)
                it.setCanceledOnTouchOutside(false)
                it.show()
            }
        } else return
    }

    private fun deleteSelectedMessages() {
        selectedMessages.forEach { msg ->
            deleteMessage(msg)
        }
    }

    override fun onResume() {
        super.onResume()
        listenForMessages()
        userStatusIndicator()
        receiveRealTimeCalls()
    }

    override fun onPause() {
        super.onPause()
        CometChat.removeMessageListener(AppConstants.MESSAGE_RECEIVE_LISTENER)
        CometChat.removeMessageListener(AppConstants.USER_STATUS_LISTENER)
        CometChat.removeCallListener(CALL_RECEIVER_LISTENER)
    }
}