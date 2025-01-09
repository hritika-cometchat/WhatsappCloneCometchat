package com.example.testapplication.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.chat.models.Group
import com.example.testapplication.R

class GroupListAdapter(private val context: Context) : RecyclerView.Adapter<GroupListAdapter.ViewHolder>() {

    var groups: List<Group>? = null
    private var onClickListener: OnClickListener? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val grpIcon: ImageView = view.findViewById(R.id.imageView)
        val grpName: TextView = view.findViewById(R.id.tvGName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layot_row_group, parent, false)
        return  ViewHolder(view)
    }

    override fun getItemCount(): Int = groups?.count() ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val groupItem = groups?.get(position)
        if (groupItem!=null){
            holder.grpName.text = groupItem.name
            Glide.with(context).load(groupItem.icon).placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.grpIcon)
        }

        holder.itemView.setOnClickListener {
            if (groupItem != null) {
                onClickListener?.onClick(position, groupItem)
            }
        }

    }

    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    interface OnClickListener {
        fun onClick(position: Int, cItem: Group)
    }


}