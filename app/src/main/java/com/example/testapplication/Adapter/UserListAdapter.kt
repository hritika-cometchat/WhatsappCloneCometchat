package com.example.testapplication.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.chat.models.User
import com.example.testapplication.R

class UserListAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var users: List<User>? = null
    private var onClickListener: OnClickListener? = null
    private var onHeaderClickListener: OnHeaderClickListener? = null

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_USER = 1
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: ImageView = view.findViewById(R.id.imageView)
        val userName: TextView = view.findViewById(R.id.tvUName)
        val userStatus: TextView = view.findViewById(R.id.tvUStatus)
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clNewGroup: ConstraintLayout = view.findViewById(R.id.clNewGroup)
        val clNewUser: ConstraintLayout = view.findViewById(R.id.clNewUser)
    }

    override fun getItemCount(): Int {
        return if (users != null) users!!.size + 1 else 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(context).inflate(R.layout.layout_row_header_add, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_USER -> {
                val view = LayoutInflater.from(context).inflate(R.layout.layout_row_user, parent, false)
                ViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_HEADER -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.clNewGroup.setOnClickListener {
                    onHeaderClickListener?.onAddGroupClicked()
                }
                headerHolder.clNewUser.setOnClickListener {
                    onHeaderClickListener?.onAddUserClicked()
                }
            }
            TYPE_USER -> {
                val userHolder = holder as ViewHolder
                val user = users?.get(position - 1)
                if (user != null) {
                    userHolder.userName.text = user.name
                    userHolder.userStatus.text = user.status
                    Glide.with(context).load(user.avatar)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(userHolder.userAvatar)

                    userHolder.itemView.setOnClickListener {
                        onClickListener?.onClick(position - 1, user)
                    }
                }
            }
        }
    }

    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    fun setOnHeaderClickListener(listener: OnHeaderClickListener?) {
        this.onHeaderClickListener = listener
    }

    interface OnClickListener {
        fun onClick(position: Int, user: User)
    }

    interface OnHeaderClickListener {
        fun onAddGroupClicked()
        fun onAddUserClicked()
    }
}
