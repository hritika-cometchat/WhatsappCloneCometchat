package com.example.testapplication.Adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.enums.MutedConversationType
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.MutedConversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import com.example.testapplication.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationListAdapter(private val context: Context) :
    RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    val typingStatusMap = mutableMapOf<String, Boolean>()
    val userStatusMap = mutableMapOf<String, Boolean>()
    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null
    var convos: MutableList<Conversation>? = null
    var currentUserUid = ""
    var mutedConversationsList : List<MutedConversation>? = null
    private var selectedItems = mutableSetOf<Int>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cAvatar: ImageView = view.findViewById(R.id.ivProfile)
        val cName: TextView = view.findViewById(R.id.tvConvoName)
        val cMessage: TextView = view.findViewById(R.id.tvConvoMessage)
        val cTypingStatus: TextView = view.findViewById(R.id.tvTypingStatus)
        val cLastTime: TextView = view.findViewById(R.id.tvTime)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
        val ivOnline: ImageView = view.findViewById(R.id.ivOnline)
        val ivMsgReceipt: ImageView = view.findViewById(R.id.ivMsgReceipt)
        val llConvoMessage: LinearLayout = view.findViewById(R.id.llConvoMessage)
        val ivNotify : ImageView = view.findViewById(R.id.ivNotify)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_row_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = convos?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cItem = convos?.get(position)
        currentUserUid = CometChat.getLoggedInUser().uid

        if (selectedItems.contains(position)) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.green_highlight))
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        if (cItem != null) {
            updateMuteState(cItem, holder)
            holder.tvCount.visibility = if (cItem.unreadMessageCount != 0) {
                holder.tvCount.text = String.format(cItem.unreadMessageCount.toString())
                holder.cLastTime.setTypeface(Typeface.DEFAULT_BOLD)
                holder.cLastTime.setTextColor(context.getColor(R.color.green))
                View.VISIBLE
            } else {
                holder.cLastTime.setTypeface(Typeface.DEFAULT)
                holder.cLastTime.setTextColor(context.getColor(R.color.secondaryTextColor))
                View.GONE
            }
            if (cItem.conversationType == "user") {
                val conversationsWith = cItem.conversationWith as User
                holder.cName.text = if (conversationsWith.uid == currentUserUid) {
                    "${conversationsWith.name} (You)"
                } else conversationsWith.name ?: "Unknown"
                Glide.with(context)
                    .load(conversationsWith.avatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(holder.cAvatar)

                val isTyping = typingStatusMap[conversationsWith.uid] ?: false
                holder.cTypingStatus.visibility = if (isTyping) View.VISIBLE else View.GONE
                holder.llConvoMessage.visibility = if (isTyping) View.GONE else View.VISIBLE
                val isUserOnline = userStatusMap[conversationsWith.uid] ?: false
                holder.ivOnline.visibility = if (isUserOnline) View.VISIBLE else View.GONE

            } else if (cItem.conversationType == "group") {
                val conversationsWith = cItem.conversationWith as Group
                holder.cName.text = conversationsWith.name ?: "Unknown Group"
                Glide.with(context)
                    .load(conversationsWith.icon)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(holder.cAvatar)
            }

            val lastMessage = cItem.lastMessage
            when (lastMessage) {
                is TextMessage -> holder.cMessage.text = lastMessage.text
                is MediaMessage -> holder.cMessage.text = "Photo"
                is Action -> holder.cMessage.text = formatActionMessage(lastMessage)
                is CustomMessage -> holder.cMessage.text = "Custom Data"
                else -> holder.llConvoMessage.visibility = View.INVISIBLE
            }

            if (lastMessage != null &&  (lastMessage.sender as User).uid == currentUserUid) {
                holder.ivMsgReceipt.visibility = View.VISIBLE
                val imgRes = when {
                    lastMessage.readAt > 0 -> R.drawable.ic_read_all
                    lastMessage.deliveredAt > 0 -> R.drawable.ic_done_all
                    lastMessage.deliveredAt == 0L && lastMessage.readAt == 0L -> R.drawable.ic_send
                    else -> R.drawable.ic_send
                }

                holder.ivMsgReceipt.setImageResource(imgRes)
            } else holder.ivMsgReceipt.visibility = View.GONE

            holder.cLastTime.text = lastMessage?.sentAt?.let { formatTime(it) }
            holder.itemView.setOnClickListener {
                onClickListener?.onClick(position, cItem)
            }

            holder.itemView.setOnLongClickListener {
                onLongClickListener?.onClick(position, cItem)
                true
            }
        }
    }

    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    fun setOnLongClickListener(listener: OnLongClickListener?) {
        this.onLongClickListener = listener
    }

    interface OnLongClickListener {
        fun onClick(position: Int, cItem: Conversation)
    }

    interface OnClickListener {
        fun onClick(position: Int, cItem: Conversation)
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    private fun formatActionMessage(action: Action): String {
        val actionByName = (action.actionBy as User).name ?: "System"
        val actionOnName = (action.actionOn as User).name ?: context.getString(R.string.user)
        return when (action.action) {
            "added" -> "$actionByName added $actionOnName"
            "removed" -> "$actionByName removed $actionOnName"
            else -> "$actionByName performed an action on $actionOnName"
        }
    }

    private fun formatTime(epoch: Long): String {
        val date = Date(epoch * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun updateUserStatus(user:User, status: Boolean) {
        userStatusMap[user.uid] = status
        val index = convos?.indexOfFirst {
            it.conversationType == CometChatConstants.CONVERSATION_TYPE_USER && (it.conversationWith as User).uid == user.uid
        }
        index?.let { notifyItemChanged(it) }
    }

    fun updateTypingIndicator( typingIndicator: TypingIndicator, show : Boolean) {
        if (typingIndicator.receiverType == CometChatConstants.RECEIVER_TYPE_USER) {
            typingStatusMap[typingIndicator.sender.uid] = show
        } else if (typingIndicator.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {

        }
        notifyDataSetChanged()
    }

    fun markRead(messageReceipt: MessageReceipt) {
        val conversation = convos?.find { it.lastMessage.id == messageReceipt.messageId }
        if (conversation != null) {
            val position = convos?.indexOf(conversation)
            position?.let {
                convos?.get(position)!!.lastMessage.readAt = messageReceipt.readAt
                notifyItemChanged(it)
            }
        }
    }

    fun markDelivered(messageReceipt: MessageReceipt) {
        val conversation = convos?.find { it.lastMessage.id == messageReceipt.messageId }
        if (conversation != null) {
            val position = convos?.indexOf(conversation)
            position?.let {
                convos?.get(position)!!.lastMessage.deliveredAt = messageReceipt.deliveredAt
                notifyItemChanged(it)
            }
        }
    }

    private fun isConversationMuted(conversation: Conversation): Boolean {
        val type = if (conversation.conversationType == CometChatConstants.CONVERSATION_TYPE_USER)
            MutedConversationType.ONE_ON_ONE
        else
            MutedConversationType.GROUP

        return mutedConversationsList?.any {
           val id =  if (conversation.conversationType == CometChatConstants.CONVERSATION_TYPE_GROUP){
               (conversation.conversationWith as Group).guid
            } else (conversation.conversationWith as User).uid
            it.id == id && it.type == type
        } ?: false
    }

    private fun updateMuteState(cItem: Conversation, holder: ViewHolder) {
        val mute = isConversationMuted(cItem)
        holder.ivNotify.visibility = if (mute) View.VISIBLE else View.GONE
    }

}
