package com.example.testapplication.Fragment.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.constants.CometChatConstants.CALL_TYPE_AUDIO
import com.cometchat.chat.constants.CometChatConstants.CALL_TYPE_VIDEO
import com.cometchat.chat.constants.CometChatConstants.RECEIVER_TYPE_GROUP
import com.cometchat.chat.constants.CometChatConstants.RECEIVER_TYPE_USER
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChatNotifications
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.conversations.ConversationsConfiguration
import com.cometchat.chatuikit.conversations.ConversationsStyle
import com.cometchat.chatuikit.messagecomposer.MessageComposerConfiguration
import com.cometchat.chatuikit.messagecomposer.MessageComposerStyle
import com.cometchat.chatuikit.messageheader.MessageHeaderConfiguration
import com.cometchat.chatuikit.messageheader.MessageHeaderStyle
import com.cometchat.chatuikit.messagelist.MessageListConfiguration
import com.cometchat.chatuikit.messages.MessagesConfiguration
import com.cometchat.chatuikit.messages.MessagesStyle
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.constants.UIKitConstants.MessageBubbleAlignment
import com.cometchat.chatuikit.shared.events.CometChatConversationEvents
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents
import com.cometchat.chatuikit.shared.models.CometChatMessageComposerAction
import com.cometchat.chatuikit.shared.models.CometChatMessageOption
import com.cometchat.chatuikit.shared.models.CometChatMessageTemplate
import com.cometchat.chatuikit.shared.viewholders.MessagesViewHolderListener
import com.cometchat.chatuikit.shared.views.CometChatAvatar.AvatarStyle
import com.cometchat.chatuikit.shared.views.CometChatAvatar.CometChatAvatar
import com.cometchat.chatuikit.shared.views.CometChatMessageBubble.CometChatMessageBubble
import com.cometchat.chatuikit.shared.views.CometChatMessageInput.MessageInputStyle
import com.cometchat.chatuikit.shared.views.CometChatStatusIndicator.StatusIndicatorStyle
import com.example.testapplication.Activity.BaseActivity
import com.example.testapplication.Activity.CallActivity
import com.example.testapplication.Activity.MainActivity
import com.example.testapplication.Activity.Ui.CometChatUiActivity
import com.example.testapplication.Activity.Ui.ContactsActivity
import com.example.testapplication.AppConstants.CONVERSATION_EVENTS_LISTENER
import com.example.testapplication.AppConstants.MESSAGE_EVENTS_LISTENER
import com.example.testapplication.Model.PinnedMessagesResponse
import com.example.testapplication.R
import com.example.testapplication.databinding.AuxiliaryButtonLayoutBinding
import com.example.testapplication.databinding.FragmentConversationsWithMessagesBinding
import com.example.testapplication.databinding.HeaderListviewPinnedBinding
import com.google.gson.Gson
import org.json.JSONObject
import java.util.UUID


class ConversationsWithMessagesFragment : Fragment() {
    private lateinit var binding: FragmentConversationsWithMessagesBinding
    private lateinit var headerBinder : HeaderListviewPinnedBinding

    private val categories = listOf("message", "custom", "call")
    private val msgTemplates = mutableListOf<CometChatMessageTemplate>()
    private val conversationsStyle = ConversationsStyle()
    private val conversationsConfig = ConversationsConfiguration()
    private val messageListConfig = MessageListConfiguration()
    private val messageHeaderConfig = MessageHeaderConfiguration()
    private val messageComposerConfig = MessageComposerConfiguration()
    private val messagesConfig = MessagesConfiguration()
    private var pinnedMessagesResponse  = PinnedMessagesResponse()
    private val pinnedMuids = mutableListOf<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        headerBinder = HeaderListviewPinnedBinding.inflate(layoutInflater)
        binding = FragmentConversationsWithMessagesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupConversationsList()
        setupMessageTemplates()
        setupMessagesConfiguration()
    }

    private fun setupConversationsList() {
        val builder = ConversationsRequest.ConversationsRequestBuilder().apply {
            setLimit(50)
        }
        val imageView = createPopupMenuIcon()
        conversationsStyle.apply {
//            lastMessageTextAppearance = R.style.LastMessageTextStyle
            titleColor = resources.getColor(R.color.green, null)
        }
        conversationsConfig.apply {
            title = "Whatsapp"
        }
        binding.conversationWithMessages.apply {
            setConversationsRequestBuilder(builder)
            setMenu(imageView)
            setStyle(conversationsStyle)
            setConversationsConfiguration(conversationsConfig)
        }
    }

    private fun createPopupMenuIcon(): ImageView {
        val imageView = ImageView(requireContext())
        imageView.setImageResource(R.drawable.ic_menu_more)
        imageView.setOnClickListener {
            showPopupMenu(it)
        }
        return imageView
    }

    private fun setupMessageTemplates() {
        msgTemplates.addAll(CometChatUIKit.getDataSource().messageTemplates)
        for (template in msgTemplates) {
            if (template.category == UIKitConstants.MessageCategory.MESSAGE) {
                template.headerView = createCustomHeaderView()
            }

            if (template.type == UIKitConstants.MessageType.TEXT) {
                template.footerView = createCustomFooterView()
                template.setOptions { context, baseMessage, group ->
                    getPinnedMessages(baseMessage.receiverType, baseMessage.receiverUid)
                    val isPinned = pinnedMuids.contains(baseMessage.muid.toString())
                    val optionList: MutableList<CometChatMessageOption> =
                        CometChatUIKit.getDataSource()
                            .getMessageOptions(context, baseMessage, group)
                    val optionTitle = if (isPinned) "Unpin the message" else "Pin the message"
                    val icon = if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin
                    val option2 = CometChatMessageOption(
                        "Pin",
                        optionTitle,
                        Color.BLACK,
                        icon,
                        Color.GRAY,
                        0,
                        Color.WHITE,
                        null
                    )
                    optionList.add(option2)
                    for (option in optionList) {
                        if (option.id == UIKitConstants.MessageOption.COPY) {
                            option.backgroundColor = resources.getColor(R.color.green_new)
                            option.iconTintColor = resources.getColor(R.color.green_darkest, null)
                            option.titleColor = resources.getColor(R.color.green_darkest, null)
                            option.setOnClick {
                                Toast.makeText(requireContext(), "This is Copy Option", Toast.LENGTH_SHORT).show()
                            }
                        } else if (option.id == "Pin") {
                            option.setOnClick {
                                pinUnpinMessage(baseMessage)
                            }
                        }
                    }
                    optionList.removeIf { it.id == UIKitConstants.MessageOption.DELETE }
                    optionList
                }
            }
        }
        val customMeetingTemplate = customMessageTemplate()
        msgTemplates.add(customMeetingTemplate)
    }

    private fun pinUnpinMessage(baseMessage: BaseMessage){
        if (baseMessage is TextMessage || (baseMessage is CustomMessage && baseMessage.type == "Meeting")) {
            val body = JSONObject()
            body.put("msgId", baseMessage.id)
            body.put("receiverType", baseMessage.receiverType)
            if (baseMessage.receiverType == RECEIVER_TYPE_GROUP){
                body.put("receiver", (baseMessage.receiver as Group).guid)
            } else body.put("receiver", (baseMessage.receiver as User).uid)

            val isPinned = pinnedMuids.contains(baseMessage.muid.toString())

            val endpoint = if (isPinned) "/v1/unpin" else "/v1/pin"
            val successMessage = if (isPinned) "Message is Unpinned" else "Message is Pinned"
            val method = if (isPinned) "DELETE" else "POST"

            CometChat.callExtension("pin-message", method, endpoint, body,
                object : CometChat.CallbackListener<JSONObject?>() {
                    override fun onSuccess(jsonObject: JSONObject?) {
                        (activity as CometChatUiActivity).showToast(successMessage)
                        getPinnedMessages(baseMessage.receiverType, baseMessage.receiverUid)
                        updateListHeader()
                    }

                    override fun onError(e: CometChatException) {
                        Log.e("Error", "$e")
                        (activity as CometChatUiActivity).showToast("Action Failed")
                    }
                })
        }
    }

    private fun customMessageTemplate() = CometChatMessageTemplate()
        .setCategory(CometChatConstants.CATEGORY_CUSTOM)
        .setType("Meeting")
        .setBubbleView(object : MessagesViewHolderListener() {
            override fun createView(
                context: Context,
                cometChatMessageBubble: CometChatMessageBubble,
                messageBubbleAlignment: MessageBubbleAlignment
            ): View {

                return layoutInflater.inflate(R.layout.item_chat_group_call_invitation, null)
            }

            override fun bindView(
                context: Context,
                view: View,
                baseMessage: BaseMessage,
                messageBubbleAlignment: MessageBubbleAlignment,
                viewHolder: RecyclerView.ViewHolder,
                list: List<BaseMessage>,
                i: Int
            ) {
                val ivMsgReceipt  = view.findViewById<ImageView>(R.id.ivMsgReceipt)
                val cardView = view.findViewById<CardView>(R.id.cvChatMe)
                val cvTitle = view.findViewById<CardView>(R.id.clGroupVideoTitle)
                val tvInfo = view.findViewById<TextView>(R.id.tvInfo)
                val isMyMsg = CometChatUIKit.getLoggedInUser().uid == baseMessage.sender.uid
                val tvJoinGroup: TextView = view.findViewById(R.id.joinGroup)
                val params = cardView.layoutParams as ConstraintLayout.LayoutParams
                if (isMyMsg) {
                    params.reset()
                    params.apply {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        startToStart = ConstraintLayout.LayoutParams.UNSET
                    }
                    val imgRes = if (baseMessage.deliveredAt > 0) {
                        if (baseMessage.readAt > 0) R.drawable.ic_read_all else R.drawable.ic_done_all
                    } else R.drawable.ic_send
                    ivMsgReceipt.setImageResource(imgRes)
                    tvInfo.text = "You started a group call. Join? "
                    ivMsgReceipt.visibility = View.VISIBLE
                    cardView.setCardBackgroundColor(resources.getColor(R.color.message_yours))
                    cvTitle.setCardBackgroundColor(resources.getColor(R.color.green_lighter))
                } else {
                    params.apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.UNSET
                    }
                    tvInfo.text = "You are invited to join the group call."
                    ivMsgReceipt.visibility = View.GONE
                    cardView.setCardBackgroundColor(resources.getColor(R.color.white))
                    cvTitle.setCardBackgroundColor(resources.getColor(R.color.blue_lighter))
                }
                cardView.layoutParams = params
                tvJoinGroup.setOnClickListener {
                    if (baseMessage is CustomMessage) {
                        Log.i("Custom Data", baseMessage.customData.toString())
                        val sessionId = baseMessage.customData.optString("sessionId")
                        context.startActivity(Intent(context, CallActivity::class.java).apply {
                            putExtra("EXTRA_START_CALL", true)
                            putExtra("SESSION_ID", sessionId)
                        })
                    }
                }
            }
        })

    private fun createCustomHeaderView() = object : MessagesViewHolderListener() {
        override fun createView(
            context: Context,
            cometChatMessageBubble: CometChatMessageBubble,
            messageBubbleAlignment: MessageBubbleAlignment
        ): View {
            return layoutInflater.inflate(R.layout.custom_txt_header_layout, null)
        }

        override fun bindView(context: Context, view: View, baseMessage: BaseMessage, messageBubbleAlignment: MessageBubbleAlignment,
            viewHolder: RecyclerView.ViewHolder,
            list: List<BaseMessage>,
            i: Int) {
            val avatar = view.findViewById<CometChatAvatar>(R.id.item_avatar)
            val isMyMsg = CometChatUIKit.getLoggedInUser().uid == baseMessage.sender.uid
            avatar.visibility = if (!isMyMsg) {
                avatar.setImage(baseMessage.sender.avatar)
                View.VISIBLE
            } else View.GONE
        }
    }

    private fun createCustomFooterView() = object : MessagesViewHolderListener() {
        override fun createView(
            context: Context,
            cometChatMessageBubble: CometChatMessageBubble,
            messageBubbleAlignment: MessageBubbleAlignment
        ): View {
            return LayoutInflater.from(context).inflate(R.layout.custom_message_bottom_layout, null)
        }

        override fun bindView(
            context: Context,
            view: View,
            baseMessage: BaseMessage,
            messageBubbleAlignment: MessageBubbleAlignment,
            viewHolder: RecyclerView.ViewHolder,
            list: List<BaseMessage>,
            i: Int
        ) {
            val tvMessage = view.findViewById<TextView>(R.id.translate_message)
            val isMyMsg = CometChatUIKit.getLoggedInUser().uid == baseMessage.sender.uid
            if (baseMessage is TextMessage) {
                tvMessage.text = baseMessage.text
                tvMessage.gravity = if (isMyMsg) Gravity.END else Gravity.START
            }
        }
    }

    private fun setupMessagesConfiguration() {
        messageListConfig.apply {
            loadingStateView = R.layout.loading_view_layout
            setTemplates(msgTemplates)
            emptyStateText = "Empty Text"
            readIcon = R.drawable.ic_read_all
            deliverIcon = R.drawable.ic_done_all
            sentIcon = R.drawable.ic_send
            hideReceipt(false)
            messagesRequestBuilder = MessagesRequest.MessagesRequestBuilder().apply {
                setCategories(categories)
            }
            headerView = setMsgListHeaderView()
        }

        messageHeaderConfig.apply {
            style = MessageHeaderStyle().apply {
                background = resources.getColor(R.color.white, null)
            }
            statusIndicatorStyle = StatusIndicatorStyle().apply {
                cornerRadius = 100F
                borderColor = resources.getColor(R.color.green_darkest, null)
                borderWidth = 6
            }

            avatarStyle = AvatarStyle().apply {
                innerViewBorderColor = resources.getColor(R.color.white, null)
                innerViewRadius = 100
                setOuterBorderWidth(2)
                innerViewWidth = 10
                setOuterBorderColor(resources.getColor(R.color.green_darkest, null))
            }
        }

        messageComposerConfig.apply {
            setAttachmentOption { context, user, group, stringStringHashMap ->
                val actionList = CometChatUIKit.getDataSource()
                    .getAttachmentOptions(context, user, group, stringStringHashMap)
                val action2 = CometChatMessageComposerAction().apply {
                    id = "GoLive"
                    title = "Go Live"
                    icon = R.drawable.ic_camera
                    setOnClick {
                        (activity as? CometChatUiActivity)?.showToast("Go Live is Selected")
                    }
                }
                actionList.add(action2)

                val action3 = CometChatMessageComposerAction().apply {
                    id = "Payments"
                    title = "Payments"
                    icon = R.drawable.ic_payment
                    setOnClick {
                        (activity as? CometChatUiActivity)?.showToast("Payment is Selected")
                    }
                }
                actionList.add(action3)
                actionList
            }
            messageInputStyle = MessageInputStyle().apply {
                background = resources.getColor(R.color.white, null)
                cornerRadius = 40F
                borderColor = resources.getColor(R.color.blue_lightest, null)
                borderWidth = 4
                textColor = resources.getColor(R.color.primaryTextColor, null)
            }
            style = MessageComposerStyle().apply {
                // setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_chat_wallpaper))
                background = resources.getColor(R.color.off_white, null)
            }
            setAuxiliaryButtonView { context, user, group, stringStringHashMap ->
                createAuxiliaryButtonView(context)
            }
        }

        messagesConfig.apply {
            style = MessagesStyle().apply {
                cornerRadius = 20f
                background = resources.getColor(R.color.off_white, null)
            } //            setMessageComposerView { context, user, group ->
//                val view = LayoutInflater.from(context).inflate(R.layout.custom_composer_view_layout, null)
//                val ivSend = view.findViewById<ImageView>(R.id.ivSend)
//                val ivAttach = view.findViewById<ImageView>(R.id.ivAttach)
//                ivAttach.setOnClickListener {
//
//                }
//                view
//            }
            messageListConfiguration = messageListConfig
            messageComposerConfiguration = messageComposerConfig
            messageHeaderConfiguration = messageHeaderConfig

            setAuxiliaryHeaderMenu { context, user, group ->
                val receiver = group?:user
                if (receiver is Group){
                    getPinnedMessages(RECEIVER_TYPE_GROUP, receiver.guid)
                } else getPinnedMessages(RECEIVER_TYPE_USER, user.uid )

                val auxiliaryButtonLayoutBinding: AuxiliaryButtonLayoutBinding =
                    AuxiliaryButtonLayoutBinding.inflate(LayoutInflater.from(requireContext()))
                auxiliaryButtonLayoutBinding.ivAudio.setOnClickListener {
                    startCall(CALL_TYPE_AUDIO, group, user)
                }
                auxiliaryButtonLayoutBinding.ivVideo.setOnClickListener {
                    startCall(CALL_TYPE_VIDEO, group, user)
                }
                auxiliaryButtonLayoutBinding.ivPins.setOnClickListener {
                    when(receiver) {
                        is Group -> getPinnedMessages(RECEIVER_TYPE_GROUP, receiver.guid)
                        is User -> getPinnedMessages(RECEIVER_TYPE_USER, receiver.uid)
                        else -> return@setOnClickListener
                    }
                }

                auxiliaryButtonLayoutBinding.root
            }
        }
        binding.conversationWithMessages.messagesConfiguration = messagesConfig
    }


    private fun createAuxiliaryButtonView(context: Context): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.auxiliary_button_composer_layout, null)
        val cardView: CardView = view.findViewById(R.id.card_button)
        cardView.setOnClickListener {
            (activity as? CometChatUiActivity)?.showToast("Clicked on SOS")
        }
        return view
    }

    private fun setMsgListHeaderView():View
    {
        return headerBinder.root
    }
    private fun updateListHeader()
    {
        if (pinnedMessagesResponse != null && !pinnedMessagesResponse.data?.pinnedMessages.isNullOrEmpty()) {
            headerBinder.root.visibility = View.VISIBLE
            val lastMsg = pinnedMessagesResponse.data?.pinnedMessages?.firstOrNull()
            headerBinder.tvLastPinMessage.text = lastMsg?.data?.text.orEmpty()
        } else {
            headerBinder.root.visibility = View.GONE
            headerBinder.tvLastPinMessage.text = "No pinned messages"
        }


    }

    private fun addEvents() {
        CometChatConversationEvents.addListener(
            CONVERSATION_EVENTS_LISTENER,
            object : CometChatConversationEvents() {
                override fun ccConversationDeleted(conversation: Conversation) {
                    (activity as? BaseActivity)?.showToast("${conversation.conversationId} is Deleted")
                }
            })

        CometChatMessageEvents.addListener(
            MESSAGE_EVENTS_LISTENER,
            object : CometChatMessageEvents() {
                override fun ccMessageSent(baseMessage: BaseMessage, status: Int) {
                    (activity as? BaseActivity)?.showToast("${baseMessage.conversationId} status is : $status")
                }
            })
    }

    private fun getPinnedMessages(receiverType: String, receiverId : String){
        val URL = ("/v1/fetch?receiverType=$receiverType").toString() + "&receiver=" + receiverId

        CometChat.callExtension("pin-message", "GET", URL, null,
            object : CometChat.CallbackListener<JSONObject?>() {
                override fun onSuccess(jsonObject: JSONObject?) {
//                   Log.i("Pinned Messages List", jsonObject.toString())
                    val json = jsonObject.toString()
                    pinnedMuids.clear()
                    pinnedMessagesResponse = Gson().fromJson(json, PinnedMessagesResponse::class.java)
                    val muids = pinnedMessagesResponse.data?.pinnedMessages?.mapNotNull { it.muid }?.toSet() ?: emptySet()
                    pinnedMuids.addAll(muids)
                   Log.i("Pinned Messages List", jsonObject.toString())
                    updateListHeader()
                }

                override fun onError(e: CometChatException) {
                    // Some error occured
                }
            })
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.convo_dashboard_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuProfile -> openProfile()
                R.id.menuContacts -> openNewChat()
                R.id.menuLogout -> logOut()
            }
            true
        }
        popupMenu.show()
    }

    private fun logOut() {
        CometChatUIKit.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(p0: String?) {
                Toast.makeText(requireContext(), "Logout Successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                CometChatNotifications.unregisterPushToken(object :
                    CometChat.CallbackListener<String?>() {
                    override fun onSuccess(s: String?) {
                        // Success callback
                    }

                    override fun onError(e: CometChatException) {
                        // Error callback
                    }
                })



                startActivity(intent)
            }

            override fun onError(p0: CometChatException?) {
                Toast.makeText(requireContext(), "Failure", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openNewChat() {
        startActivity(Intent(requireContext(), ContactsActivity::class.java))
    }

    private fun openProfile() {

    }

    private fun startCall(callType: String, group: Group? = null, user: User? = null) {
        val intent = Intent(requireContext(), CallActivity::class.java)
        val receiver = group ?: user
        when (receiver) {
            is User -> {
                intent.putExtra("EXTRA_ID", receiver.uid)
                intent.putExtra("EXTRA_RECEIVER_TYPE", RECEIVER_TYPE_USER)
                intent.putExtra("EXTRA_INITIATE_CALL", true)
            }

            is Group -> {
                sendCustomMessage(RECEIVER_TYPE_GROUP, receiver.guid)
                intent.putExtra("EXTRA_ID", receiver.guid)
                intent.putExtra("EXTRA_RECEIVER_TYPE", RECEIVER_TYPE_GROUP)
                intent.putExtra("EXTRA_START_CALL", true)
                intent.putExtra("SESSION_ID", UUID.randomUUID().toString())
            }

            else -> {
                return
            }
        }
        intent.putExtra("EXTRA_RECEIVER", Gson().toJson(receiver))
        intent.putExtra("EXTRA_CALL_TYPE", callType)
        startActivity(intent)
    }

    private fun sendCustomMessage(receiverType: String, idUG: String) {
        val customData = JSONObject().apply {
            put("sessionId", UUID.randomUUID().toString())
            put("receiverType", receiverType)
            put("status", "Initiated")
        }
        val customMessage = CustomMessage(idUG, receiverType, "Meeting", customData)
        CometChatUIKit.sendCustomMessage(
            customMessage,
            object : CometChat.CallbackListener<CustomMessage>() {
                override fun onSuccess(customMessage: CustomMessage) {
                    Log.d("TAG", customMessage.toString())
                }

                override fun onError(e: CometChatException) {
                    Log.d("TAG", e.message!!)
                }
            })
    }

    override fun onResume() {
        super.onResume()
//        addEvents()
    }

    override fun onPause() {
        super.onPause()
//        CometChatMessageEvents.removeListener(MESSAGE_EVENTS_LISTENER)
//        CometChatConversationEvents.removeListener(CONVERSATION_EVENTS_LISTENER)
    }
}