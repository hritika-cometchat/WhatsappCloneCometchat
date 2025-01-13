package com.example.testapplication.Fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChatNotifications
import com.cometchat.chat.core.ConversationsRequest.ConversationsRequestBuilder
import com.cometchat.chat.enums.MutedConversationType
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.MutedConversation
import com.cometchat.chat.models.NotificationPreferences
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.UnmutedConversation
import com.cometchat.chat.models.User
import com.example.testapplication.Activity.ConversationMsgActivity
import com.example.testapplication.Activity.UsersListActivity
import com.example.testapplication.Adapter.ConversationListAdapter
import com.example.testapplication.ApiInterface
import com.example.testapplication.AppConstants
import com.example.testapplication.Model.Conversations
import com.example.testapplication.R
import com.example.testapplication.RetrofitInstance
import com.example.testapplication.databinding.FragmentConversationBinding
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response

class ConversationFragment : Fragment() {
    private lateinit var apiInterface : ApiInterface
    private var TAG = "ConversationFragment"
    private lateinit var cAdapter: ConversationListAdapter
    private lateinit var binding: FragmentConversationBinding
    private var selectedConversations = mutableListOf<Conversation>()
    private var actionMode: ActionMode? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConversationBinding.inflate(layoutInflater)
        apiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val payload = arguments?.getString(AppConstants.NOTIFICATION_PAYLOAD).orEmpty()
        if (payload.isNotEmpty()) {
            try {
                val jsonPayload = JSONObject(payload)
                navigateToConversationNotify(jsonPayload)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        cAdapter = ConversationListAdapter(requireContext())
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cAdapter
        }

        getConversationList()
//        getConversationsList()
        fetchPreferences()
        getMutedConversations()

        binding.floatBtnNewChat.setOnClickListener {
            startActivity(Intent(requireContext(), UsersListActivity::class.java))
        }
        cAdapter.setOnClickListener(object : ConversationListAdapter.OnClickListener {
            override fun onClick(position: Int, cItem: Conversation) {
                if (actionMode == null) {
                    navigateToConversation(cItem)
                } else {
                    toggleSelection(cItem, position)
                }
            }
        })

        cAdapter.setOnLongClickListener(object : ConversationListAdapter.OnLongClickListener {
            override fun onClick(position: Int, cItem: Conversation) {
                startActionMode()
                toggleSelection(cItem, position)
            }
        })
    }

    private fun fetchPreferences(){
        CometChatNotifications.fetchPreferences(object :
            CometChat.CallbackListener<NotificationPreferences>() {
            override fun onSuccess(notificationPreferences: NotificationPreferences) {
                val mutePreferences = notificationPreferences.mutePreferences
                val dndPreference = mutePreferences.dndPreference
                Log.w("MutePreference", mutePreferences.toJson().toString())
                Log.w("dndPreference", dndPreference.toString())

            }

            override fun onError(e: CometChatException) {
                Log.e("MutePreference", e.message.toString())
            }
        })
    }

    private fun getMutedConversations() {
        CometChatNotifications.getMutedConversations(object :
            CometChat.CallbackListener<List<MutedConversation>?>() {
            override fun onSuccess(mutedConversations: List<MutedConversation>?) {
                cAdapter.mutedConversationsList = mutedConversations
                Log.d("MutedConvos", mutedConversations.toString())
            }

            override fun onError(e: CometChatException) {
                Log.e("MutedConvos", e.message.toString())
            }
        })
    }

    private fun getConversationsList(){
//        val params = HashMap<String, String>()
//        params["limit"] = "10"
//        params["conversationId"] = "cometchat-uid-1_user_cometchat-uid-6"
        val mediaType = "application/json".toMediaTypeOrNull()

        val body = RequestBody.create(
            mediaType,
            """
            {
            "blockedUids": ["user-8"]
            } 
            """.trimIndent()
        )
        apiInterface.blockUsers("cometchat-uid-1", body).enqueue(object : retrofit2.Callback<List<Conversations>> {
            override fun onResponse(call: Call<List<Conversations>>, response: Response<List<Conversations>>)
            {
                if (response.isSuccessful) {
                    println(response.body().toString())
                }
            }

            override fun onFailure(call: Call<List<Conversations>>, t: Throwable) {
                println(t.message.toString())
            }
        })
    }

    private fun getConversationList() {
        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(progressBar)
            .setCancelable(false)
            .create()
        dialog.show()

        val conversationsRequest = ConversationsRequestBuilder().setLimit(50).build()
        conversationsRequest.fetchNext(object : CometChat.CallbackListener<List<Conversation>?>() {
            override fun onSuccess(conversations: List<Conversation>?) {
                activity?.runOnUiThread {
                    dialog.dismiss()
                    conversations?.let {
                        println(it)
                        cAdapter.convos = it.toMutableList()
                        it.forEach { cItem ->
                            if (cItem.conversationType == CometChatConstants.CONVERSATION_TYPE_USER) {
                                val conversationsWith = cItem.conversationWith as? User ?: return@forEach
                                val isOnline = conversationsWith.status == CometChatConstants.USER_STATUS_ONLINE
                                cAdapter.updateUserStatus(conversationsWith, isOnline)
                                cAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
            override fun onError(e: CometChatException) {
                dialog.dismiss()
                Log.i("Conversation Error", e.message.toString())
            }
        })
    }

    private fun navigateToConversation(cItem: Conversation) {
        val intent = Intent(requireContext(), ConversationMsgActivity::class.java).apply {
            when (cItem.conversationType) {
                "group" -> {
                    val group = cItem.conversationWith as? Group
                    if (group != null) {
                        putExtra("EXTRA_ID", group.guid)
                        putExtra("EXTRA_SENDER", Gson().toJson(group))
                    }
                }

                "user" -> {
                    val user = cItem.conversationWith as? User
                    if (user != null) {
                        putExtra("EXTRA_ID", user.uid)
                        putExtra("EXTRA_SENDER", Gson().toJson(user))
                    }
                }
            }
            putExtra("EXTRA_CONVO", Gson().toJson(cItem))
        }
        startActivity(intent)
    }

    private fun navigateToConversationNotify(payload: JSONObject) {
        val conversationId = payload.optString("conversationId")
        val senderId = payload.optString("sender")
        val receiverType = payload.optString("receiverType")
        val senderName = payload.optString("senderName")
        val senderAvatar = payload.optString("senderAvatar")

        val intent = Intent(requireContext(), ConversationMsgActivity::class.java).apply {
            when (receiverType) {
                "group" -> {
                    putExtra("EXTRA_ID", senderId)
                    val group = Group().apply {
                        name = senderName
                        icon = senderAvatar
                    }
                    putExtra("EXTRA_SENDER", Gson().toJson(group))
                }
                "user" -> {
                    putExtra("EXTRA_ID", senderId)
                    val user = User().apply {
                        name = senderName
                        avatar = senderAvatar
                    }
                    putExtra("EXTRA_SENDER", Gson().toJson(user))
                }
            }
        }
        startActivity(intent)
    }

    private fun toggleSelection(cItem: Conversation, position: Int) {
        if (selectedConversations.contains(cItem)) {
            selectedConversations.remove(cItem)
            cAdapter.notifyItemChanged(position)
        } else {
            selectedConversations.add(cItem)
            cAdapter.notifyItemChanged(position)
        }
        cAdapter.toggleSelection(position)
        if (selectedConversations.isEmpty()) actionMode?.finish() else updateActionMode()
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (activity as? Activity)?.startActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: android.view.Menu?): Boolean {
                    mode?.menuInflater?.inflate(R.menu.menu_action_mode_edit_delete, menu)
                    menu?.findItem(R.id.menu_edit)?.setVisible(false)
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode?, item: android.view.MenuItem?): Boolean {
                    return when (item?.itemId) {
                        R.id.menu_delete -> {
                            showAlertDelete()
                            true
                        }
                        R.id.menu_mute -> {
                            showAlertSelection()
                            true
                        }
                        R.id.menu_unmute -> {
                            unmuteSelectedConversations()
                            true
                        }
                        else -> false
                    }
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    selectedConversations.clear()
                    cAdapter.clearSelections()
                    cAdapter.notifyDataSetChanged()
                    actionMode = null
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: android.view.Menu?): Boolean {
                    return true
                }
            })
        }
    }

    private fun updateActionMode() {
        actionMode?.title = "${selectedConversations.size} Selected"
        cAdapter.notifyDataSetChanged()
    }

    private fun showAlertDelete() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).create()
        val view = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnYes = view.findViewById<Button>(R.id.btnYes)
        builder.setView(view)
        btnCancel.setOnClickListener { builder.dismiss() }
        btnYes.setOnClickListener {
            builder.dismiss()
            deleteSelectedConversations()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun showAlertSelection() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).create()
        val view = layoutInflater.inflate(R.layout.dialog_mute_notifications, null)

        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnYes = view.findViewById<Button>(R.id.btnYes)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        val rbAlways = view.findViewById<RadioButton>(R.id.rbAlways)

        rbAlways.isChecked = true
        builder.setView(view)
        btnCancel.setOnClickListener {
            builder.dismiss()
        }

        btnYes.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            val muteUntil = when (selectedOptionId) {
                R.id.rb8Hours -> System.currentTimeMillis() + 8 * 60 * 60 * 1000L
                R.id.rb1Week -> System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
                R.id.rbAlways -> 33322263675000
                else -> 0L
            }

            if (muteUntil > 0L) {
                muteSelectedConversations(muteUntil)
            } else {
                Toast.makeText(requireContext(), "Please select a valid option", Toast.LENGTH_SHORT).show()
            }

            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun muteSelectedConversations(muteUntil: Long) {
        if (selectedConversations.isNotEmpty()) {
            val mutedConversations = selectedConversations.map { conversation ->
                MutedConversation().apply {
                    val isGroup = conversation.conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP
                    id = if (isGroup) (conversation.conversationWith as Group).guid else (conversation.conversationWith as User).uid
                    type = if (isGroup) MutedConversationType.GROUP else MutedConversationType.ONE_ON_ONE
                    until = muteUntil
                }
            }

            CometChatNotifications.muteConversations(mutedConversations, object : CometChat.CallbackListener<String>() {
                override fun onSuccess(response: String) {
                    Toast.makeText(requireContext(), "Conversations muted successfully", Toast.LENGTH_SHORT).show()
                    selectedConversations.clear()
                    getMutedConversations()
                    cAdapter.notifyDataSetChanged()
                    if (selectedConversations.isEmpty()) {
                        actionMode?.finish()
                    }
                }

                override fun onError(e: CometChatException) {
                    Log.e("MuteConversation", e.message.toString())
                    Toast.makeText(requireContext(), "Failed to mute conversations: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "No conversations selected to mute", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unmuteSelectedConversations() {
        if (selectedConversations.isNotEmpty()) {
            val unmutedConversations = selectedConversations.map { conversation ->
                UnmutedConversation().apply {
                    val isGroup = conversation.conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP
                    id = if (isGroup) (conversation.conversationWith as Group).guid else (conversation.conversationWith as User).uid
                    type = if (isGroup) MutedConversationType.GROUP else MutedConversationType.ONE_ON_ONE
                }
            }

            CometChatNotifications.unmuteConversations(unmutedConversations, object : CometChat.CallbackListener<String>() {
                override fun onSuccess(response: String) {
                    Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show()
                    selectedConversations.clear()
                    getMutedConversations()
                    cAdapter.notifyDataSetChanged()
                    if (selectedConversations.isEmpty()) {
                        actionMode?.finish()
                    }
                }

                override fun onError(e: CometChatException) {
                    Log.e("MuteConversation", e.message.toString())
                    Toast.makeText(requireContext(), "Failed to mute conversations: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "No conversations selected to mute", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedConversations() {
        selectedConversations.forEach { cItem ->
            var idUG = ""
            when (cItem.conversationType) {
                "group" -> {
                    val group = cItem.conversationWith as? Group
                    if (group != null) {
                        idUG = group.guid
                    }
                }

                "user" -> {
                    val user = cItem.conversationWith as? User
                    if (user != null) {
                        idUG = user.uid
                    }
                }
            }

            CometChat.deleteConversation(idUG, cItem.conversationType, object : CometChat.CallbackListener<String?>() {
                    override fun onSuccess(s: String?) {
                        Log.d(TAG, s.toString())
                        val position = cAdapter.convos?.indexOf(cItem)
                        if (position != null) {
                            cAdapter.convos?.removeAt(position)
                            cAdapter.notifyItemRemoved(position)
                            selectedConversations.remove(cItem)
                        }
                        if (selectedConversations.isEmpty()) {
                            actionMode?.finish()
                        }
                    }

                    override fun onError(e: CometChatException) {
                        Log.d(TAG, e.message.toString())
                    }
                })
        }
    }

    private fun handleMessageReceived(message: BaseMessage) {
        CometChat.markAsDelivered(message)
        val newConvo = CometChatHelper.getConversationFromMessage(message)
        val position = cAdapter.convos?.indexOfFirst { it.conversationId == newConvo.conversationId }

        if (position != null && position >= 0) {
            val existingConvo = cAdapter.convos?.get(position)
            existingConvo?.let {
                newConvo.unreadMessageCount = it.unreadMessageCount + 1
            }
            cAdapter.convos?.apply {
                removeAt(position)
                cAdapter.notifyItemRemoved(position)
                add(0, newConvo)
                cAdapter.notifyItemInserted(0)
            }         } else {
            cAdapter.convos?.add(0, newConvo)
            cAdapter.notifyItemInserted(0)
        }
    }

    private fun listenForMessages() {
        CometChat.addMessageListener(
            AppConstants.MESSAGE_RECEIVE_LISTENER,
            object : CometChat.MessageListener() {
                override fun onTextMessageReceived(message: TextMessage) {
                    handleMessageReceived(message)
                }

                override fun onMediaMessageReceived(mediaMessage: MediaMessage?) {
                    mediaMessage?.let { handleMessageReceived(it) }
                }

                override fun onCustomMessageReceived(customMessage: CustomMessage?) {
                    customMessage?.let { handleMessageReceived(it) }
                }

                override fun onMessagesRead(messageReceipt: MessageReceipt) {
                    cAdapter.markRead(messageReceipt)
                }

                override fun onMessagesDelivered(messageReceipt: MessageReceipt) {
                    cAdapter.markDelivered(messageReceipt)
                }

                override fun onTypingStarted(typingIndicator: TypingIndicator?) {
                    super.onTypingStarted(typingIndicator)
                    typingIndicator?.let { cAdapter.updateTypingIndicator(it, true) }
                }

                override fun onTypingEnded(typingIndicator: TypingIndicator?) {
                    super.onTypingEnded(typingIndicator)
                    typingIndicator?.let { cAdapter.updateTypingIndicator(it, false) }
                }

            })
    }

    private fun userStatusIndicator() {
        CometChat.addUserListener(
            AppConstants.USER_STATUS_LISTENER,
            object : CometChat.UserListener() {
                override fun onUserOffline(user: User?) {
                    (requireContext() as Activity).runOnUiThread {
                        user?.let { cAdapter.updateUserStatus(it, false) }
                    }
                }

                override fun onUserOnline(user: User?) {
                    activity?.runOnUiThread {
                        user?.let { cAdapter.updateUserStatus(it, true) }
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        listenForMessages()
        userStatusIndicator()
    }

    override fun onPause() {
        super.onPause()
        CometChat.removeMessageListener(AppConstants.MESSAGE_RECEIVE_LISTENER)
        CometChat.removeMessageListener(AppConstants.USER_STATUS_LISTENER)
    }
}