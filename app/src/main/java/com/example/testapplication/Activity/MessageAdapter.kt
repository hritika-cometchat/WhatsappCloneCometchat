package com.example.testapplication.Activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.*
import com.example.testapplication.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_USER_MESSAGE_ME = 10
    private val VIEW_TYPE_USER_MESSAGE_OTHER = 11
    private val VIEW_TYPE_ACTION_MESSAGE = 12
    private val VIEW_TYPE_USER_MEDIA_MESSAGE_OTHER = 13
    private val VIEW_TYPE_USER_MEDIA_MESSAGE_ME = 14
    private val VIEW_TYPE_CALL = 15
    private val VIEW_TYPE_CUSTOM_MESSAGE_ME = 16
    private val VIEW_TYPE_CUSTOM_MESSAGE_OTHER = 17

    private val user = CometChat.getLoggedInUser()
    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null
    var messages = mutableListOf<BaseMessage>()
    private var selectedItems = mutableSetOf<Int>()


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

    fun addMessage(message: BaseMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun editMessage(msg: BaseMessage) {
        val position = messages.indexOfFirst { it.id == msg.id }
        if (position!=-1) {
            messages[position] = msg
            notifyItemChanged(position)
        }
    }

    fun deleteMessage(msg: BaseMessage) {
        val position = messages.indexOfFirst { it.id == msg.id }
        messages.remove(msg)
        if (position!=-1) {
            notifyItemRemoved(position)
        }
    }

    fun markDelivered(messageReceipt: MessageReceipt) {
        val position = messages.indexOfFirst { it.id == messageReceipt.messageId }
        if (position != -1) {
            messages[position].deliveredAt = messageReceipt.deliveredAt
            notifyItemChanged(position)
        }
    }

    fun markRead(messageReceipt: MessageReceipt) {
        val position = messages.indexOfFirst { it.id == messageReceipt.messageId }
        if (position != -1) {
            messages[position].readAt = messageReceipt.readAt
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun loadMessages(messages: MutableList<BaseMessage>) {
        this.messages.addAll(0, messages)
        notifyItemRangeInserted(0, messages.size)
    }

    private fun formatTime(time: Long): String {
        val date = Date(time * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun formatDate(timeInMillis: Long): String {
        val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
        return dateFormat.format(timeInMillis)
    }

    private fun shouldShowDate(position: Int): Boolean {
        if (position == 0) return true
        val previousMessage = messages[position - 1]
        val currentMessage = messages[position]
        return formatDate(previousMessage.sentAt) != formatDate(currentMessage.sentAt)
    }

    inner class MyUserHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.tvChatMeMsg)
        private val date: TextView = view.findViewById(R.id.tvDateMe)
        private val messageDate: TextView = view.findViewById(R.id.tvTimestampMe)
        private val ivMsgReceipt: ImageView = view.findViewById(R.id.ivMsgReceipt)

        fun bindView(message: TextMessage, showDate: Boolean) {
            messageText.text = message.text
            messageDate.text = formatTime(message.sentAt)
            date.visibility = if (showDate) View.VISIBLE else View.GONE
            date.text = formatDate(message.sentAt)
            val imgRes = if (message.deliveredAt > 0) {
                if (message.readAt > 0) R.drawable.ic_read_all else R.drawable.ic_done_all
            } else R.drawable.ic_send
            ivMsgReceipt.setImageResource(imgRes)
        }
    }

    inner class OtherUserHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.tvChatOther)
        private val date: TextView = view.findViewById(R.id.tvDateOther)
        private val timestamp: TextView = view.findViewById(R.id.tvTimestampOther)
        private val ivUserProfile: ImageView = view.findViewById(R.id.ivUserProfile)

        fun bindView(message: TextMessage, showDate: Boolean) {
            messageText.text = message.text
            timestamp.text = formatTime(message.sentAt)
            date.visibility = if (showDate) {
                date.text = formatDate(message.sentAt)
                View.VISIBLE
            } else View.GONE
            ivUserProfile.visibility =
                if (message.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
                    Glide.with(context)
                        .load((message.sender as User).avatar)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(ivUserProfile)
                    View.VISIBLE
                } else View.GONE
        }
    }

    inner class ActionMessageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.tvActionName)
        private val root: ConstraintLayout = view.findViewById(R.id.clAction)

        fun bindView(action: Action) {
            messageText.text = formatActionMessage(action)
        }

        private fun formatActionMessage(action: Action): String {
            val actionBy = action.actionBy as? User
            val actionOn = action.actionOn as? User
            val actionByName = if (actionBy?.uid == user.uid) "You" else actionBy?.name ?: "System"
            val actionOnName = if (actionOn?.uid == user.uid) "You" else actionOn?.name ?: "User"

            return when (action.action) {
                "added" -> {
                    if (action.actionFor is Group) {
                        "$actionByName added $actionOnName to the group"
                    } else {
                        "$actionByName added $actionOnName"
                    }
                }
                "removed" -> {
                    if (action.actionFor is Group) {
                        "$actionByName removed $actionOnName from the group"
                    } else {
                        "$actionByName removed $actionOnName"
                    }
                }
                "joined" -> "$actionOnName joined"
                "left" -> "$actionOnName left"
                "kicked" -> "$actionByName kicked $actionOnName"
                "banned" -> "$actionByName banned $actionOnName"
                "deleted" -> {
                    if (action.actionOn is MediaMessage) {
                        val mediaMessage = action.actionOn as MediaMessage
                        if (mediaMessage.type == "image") {
                            "$actionByName deleted an image"
                        } else "$actionByName deleted a message"
                    } else "$actionByName deleted a message"
                }
                else -> "$actionByName performed an action on $actionOnName"
            }
        }
    }


    inner class CallMessageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.tvActionName)

        fun bindView(call: Call) {
            messageText.text = formatCallMessage(call)
        }

        private fun formatCallMessage(call: Call): String {
            val callByName =   if (call.callInitiator is Group) {
                val callBy = call.callInitiator as Group
                callBy.name
            } else {
                val callBy = call.callInitiator as User
                if(callBy.uid == user.uid) "You" else callBy.name
            }

            val callOnName = if (call.callReceiver is Group){
                val callOn = call.callReceiver as Group
                callOn.name
            } else {
                val callOn = call.callReceiver as User
                if(callOn.uid == user.uid) "You" else callOn.name
            }

            val callStatus = call.callStatus ?: "unknown"
            return when (callStatus) {
                CometChatCallsConstants.CALL_STATUS_MISSED -> "$callOnName missed a call from $callByName"
                CometChatCallsConstants.CALL_STATUS_UNANSWERED, CometChatConstants.CALL_STATUS_INITIATED-> "Unanswered Call from $callOnName"
                CometChatCallsConstants.CALL_STATUS_REJECTED -> "Rejected Call By $callOnName"
                else -> "call from $callByName"
            }
        }
    }

    inner class ImageMsgMeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgMessage: ImageView = view.findViewById(R.id.ivMediaMsg)
        private val date: TextView = view.findViewById(R.id.tvDate)
        private val timestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val ivMsgReceipt: ImageView = view.findViewById(R.id.ivMsgReceipt)
        private val root: ConstraintLayout = view.findViewById(R.id.clRootME)

        fun bindView(mediaMessage: MediaMessage, showDate: Boolean, context: Context) {
            if (mediaMessage.deletedAt<=0){
                root.visibility = View.VISIBLE
                Glide.with(context).load(mediaMessage.attachment.fileUrl).into(imgMessage)
                imgMessage.setOnClickListener {
                    openFullScreen(mediaMessage.attachment.fileUrl)
                }
                timestamp.text = formatTime(mediaMessage.sentAt)
                date.visibility = if (showDate) {
                    date.text = formatDate(mediaMessage.sentAt)
                    View.VISIBLE
                } else View.GONE
                val imgRes = if (mediaMessage.deliveredAt > 0) {
                    if (mediaMessage.readAt > 0) R.drawable.ic_read_all else R.drawable.ic_done_all
                } else R.drawable.ic_send
                ivMsgReceipt.setImageResource(imgRes)
            } else root.visibility = View.GONE
        }

    }

    inner class ImageMsgOtherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgMessage: ImageView = view.findViewById(R.id.ivMediaMsg)
        private val date: TextView = view.findViewById(R.id.tvDate)
        private val timestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val ivUserProfile: ImageView = view.findViewById(R.id.ivUserProfile)
        private val clRootOther: ConstraintLayout = view.findViewById(R.id.clRootOther)


        fun bindView(mediaMessage: MediaMessage, showDate: Boolean, context: Context) {
            if (mediaMessage.deletedAt<=0) {
                clRootOther.visibility = View.VISIBLE
                Glide.with(context).load(mediaMessage.attachment.fileUrl).into(imgMessage)
                imgMessage.setOnClickListener {
                    openFullScreen(mediaMessage.attachment.fileUrl)
                }
                timestamp.text = formatTime(mediaMessage.sentAt)
                date.visibility = if (showDate) {
                    date.text = formatDate(mediaMessage.sentAt)
                    View.VISIBLE
                } else View.GONE
                ivUserProfile.visibility =
                    if (mediaMessage.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
                        Glide.with(context)
                            .load((mediaMessage.sender as User).avatar)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .into(ivUserProfile)
                        View.VISIBLE
                    } else View.GONE
            } else clRootOther.visibility = View.GONE
        }

    }

    inner class MyCustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val date: TextView = view.findViewById(R.id.tvDateMe)
        private val messageDate: TextView = view.findViewById(R.id.tvTimestampMe)
        private val ivMsgReceipt: ImageView = view.findViewById(R.id.ivMsgReceipt)
        private val tvJoinGroup: TextView = view.findViewById(R.id.joinGroup)

        fun bindView(message: CustomMessage, showDate: Boolean) {
            tvJoinGroup.setOnClickListener {
                Log.i("Custom Data", message.customData.toString())
                val sessionId = message.customData.optString("sessionId")
                context.startActivity(Intent(context, CallActivity::class.java).apply {
                    putExtra("EXTRA_START_CALL", true)
                    putExtra("SESSION_ID", sessionId)
                })
            }
            messageDate.text = formatTime(message.sentAt)
            date.visibility = if (showDate) View.VISIBLE else View.GONE
            date.text = formatDate(message.sentAt)
            val imgRes = if (message.deliveredAt > 0) {
                if (message.readAt > 0) R.drawable.ic_read_all else R.drawable.ic_done_all
            } else R.drawable.ic_send
            ivMsgReceipt.setImageResource(imgRes)
        }
    }

    inner class OtherCustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val date: TextView = view.findViewById(R.id.tvDateOther)
        private val timestamp: TextView = view.findViewById(R.id.tvTimestampOther)
        private val ivUserProfile: ImageView = view.findViewById(R.id.ivUserProfile)
        private val tvJoinGroup: TextView = view.findViewById(R.id.joinGroup)

        fun bindView(message: CustomMessage, showDate: Boolean) {
            tvJoinGroup.setOnClickListener {
                Log.i("Custom Data", message.customData.toString())
                val sessionId = message.customData.optString("sessionId")
                context.startActivity(Intent(context, CallActivity::class.java).apply {
                    putExtra("EXTRA_START_CALL", true)
                    putExtra("SESSION_ID", sessionId)
                })
            }
            date.visibility = if (showDate) {
                date.text = formatDate(message.sentAt)
                View.VISIBLE
            } else View.GONE
            ivUserProfile.visibility =
                if (message.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
                    Glide.with(context)
                        .load((message.sender as User).avatar)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(ivUserProfile)
                    View.VISIBLE
                } else View.GONE
            timestamp.text = formatTime(message.sentAt)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER_MESSAGE_ME -> MyUserHolder(layoutInflater.inflate(R.layout.item_chat_me, parent, false))
            VIEW_TYPE_USER_MESSAGE_OTHER -> OtherUserHolder(layoutInflater.inflate(R.layout.item_chat_other, parent, false))
            VIEW_TYPE_USER_MEDIA_MESSAGE_ME -> ImageMsgMeViewHolder(layoutInflater.inflate(R.layout.item_chat_media_image, parent, false))
            VIEW_TYPE_USER_MEDIA_MESSAGE_OTHER -> ImageMsgOtherViewHolder(layoutInflater.inflate(R.layout.item_chat_media_other, parent, false))
            VIEW_TYPE_ACTION_MESSAGE -> ActionMessageHolder(layoutInflater.inflate(R.layout.item_action_message, parent, false))
            VIEW_TYPE_CALL -> CallMessageHolder(layoutInflater.inflate(R.layout.item_action_message, parent, false))
            VIEW_TYPE_CUSTOM_MESSAGE_ME -> MyCustomViewHolder(layoutInflater.inflate(R.layout.item_chat_group_call_invitation, parent, false))
            VIEW_TYPE_CUSTOM_MESSAGE_OTHER -> OtherCustomViewHolder(layoutInflater.inflate(R.layout.item_chat_group_call_invitation_other, parent, false))
            else -> MyUserHolder(layoutInflater.inflate(R.layout.item_chat_me, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val sender = message.sender as User
        return when (message) {
            is TextMessage -> if (sender.uid == user.uid) VIEW_TYPE_USER_MESSAGE_ME else VIEW_TYPE_USER_MESSAGE_OTHER
            is Action -> VIEW_TYPE_ACTION_MESSAGE
            is MediaMessage -> if (sender.uid == user.uid) VIEW_TYPE_USER_MEDIA_MESSAGE_ME else VIEW_TYPE_USER_MEDIA_MESSAGE_OTHER
            is Call -> VIEW_TYPE_CALL
            is CustomMessage -> if (sender.uid == user.uid) VIEW_TYPE_CUSTOM_MESSAGE_ME else VIEW_TYPE_CUSTOM_MESSAGE_OTHER
            else -> VIEW_TYPE_USER_MESSAGE_OTHER
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val showDate = shouldShowDate(position)

        if (selectedItems.contains(position)) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.blue_lightest))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        when (holder.itemViewType) {
            VIEW_TYPE_USER_MESSAGE_ME -> (holder as MyUserHolder).bindView(message as TextMessage, showDate)
            VIEW_TYPE_USER_MESSAGE_OTHER -> (holder as OtherUserHolder).bindView(message as TextMessage, showDate)
            VIEW_TYPE_USER_MEDIA_MESSAGE_OTHER -> {
                (holder as ImageMsgOtherViewHolder).bindView(
                    message as MediaMessage,
                    showDate,
                    context
                )
            }
            VIEW_TYPE_USER_MEDIA_MESSAGE_ME -> {
                (holder as ImageMsgMeViewHolder).bindView(
                    message as MediaMessage,
                    showDate,
                    context
                )
            }
            VIEW_TYPE_ACTION_MESSAGE -> (holder as ActionMessageHolder).bindView(message as Action)
            VIEW_TYPE_CALL -> (holder as CallMessageHolder).bindView(message as Call)
            VIEW_TYPE_CUSTOM_MESSAGE_ME -> (holder as MyCustomViewHolder).bindView(message as CustomMessage, showDate)
            VIEW_TYPE_CUSTOM_MESSAGE_OTHER -> (holder as OtherCustomViewHolder).bindView(message as CustomMessage, showDate)
        }

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(position, message)
        }

        holder.itemView.setOnLongClickListener {
            onLongClickListener?.onClick(position, message)
            (message.receiver as User).uid == user.uid
        }
    }
    fun setOnLongClickListener(listener: OnLongClickListener?) {
        this.onLongClickListener = listener
    }

    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    interface OnClickListener {
        fun onClick(position: Int, baseMessage: BaseMessage)
    }

    interface OnLongClickListener {
        fun onClick(position: Int, baseMessage: BaseMessage)
    }
    private fun openFullScreen(imgUrl: String){
        val intent = Intent(context, FullViewActivity::class.java)
        intent.putExtra("imageUrl", imgUrl)
        context.startActivity(intent)
    }

}
