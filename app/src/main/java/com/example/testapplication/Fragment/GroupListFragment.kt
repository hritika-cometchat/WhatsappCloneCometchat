package com.example.testapplication.Fragment

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.example.testapplication.Activity.ConversationMsgActivity
import com.example.testapplication.Adapter.GroupListAdapter
import com.example.testapplication.databinding.FragmentGroupListBinding
import com.google.gson.Gson

class GroupListFragment : Fragment() {

    private val TAG = "GroupListFragment"
    private lateinit var adapter: GroupListAdapter
    private lateinit var binding: FragmentGroupListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupListBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = GroupListAdapter(requireContext())
        binding.rvGroupList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGroupList.adapter = adapter
        getGroupList()

        adapter.setOnClickListener(object : GroupListAdapter.OnClickListener {
            override fun onClick(position: Int, cItem: Group) {
                val intent = Intent(requireContext(), ConversationMsgActivity::class.java).apply {
                    putExtra("EXTRA_ID", cItem.guid)
                    putExtra("EXTRA_SENDER", Gson().toJson(cItem))
                    putExtra("EXTRA_NEW_CHAT",true)
                    putExtra("EXTRA_CONVO_TYPE", CometChatConstants.CONVERSATION_TYPE_GROUP)
                }
                startActivity(intent)
            }
        })

    }

    private fun getGroupList() {
        val pd = ProgressDialog(requireContext())
        pd.setMessage("loading")
        pd.show()
        val groupsRequest = GroupsRequest
            .GroupsRequestBuilder()
            .setLimit(30)
            .joinedOnly(true)
            .build()

        groupsRequest.fetchNext(object : CometChat.CallbackListener<MutableList<Group>>() {
            override fun onSuccess(list: MutableList<Group>) {
                pd.dismiss()
                Log.d(TAG, "Group list received: " + list.size)
                adapter.groups = list
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

}