package com.example.testapplication.Fragment.ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cometchat.chatuikit.calls.calldetails.CallLogDetailsConfiguration
import com.cometchat.chatuikit.calls.calldetails.CallLogDetailsStyle
import com.cometchat.chatuikit.calls.calldetails.CometChatCallLogDetailsOption
import com.cometchat.chatuikit.shared.views.CometChatAvatar.AvatarStyle
import com.example.testapplication.R
import com.example.testapplication.databinding.FragmentCallLogBinding

class CallsLogFragment : Fragment() {
    private lateinit var binding : FragmentCallLogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCallLogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callLogDetailsConfig = CallLogDetailsConfiguration().apply {
            style = CallLogDetailsStyle().apply {
                background = resources.getColor(R.color.off_white)
                titleColor = Color.RED
            }
            avatarStyle = AvatarStyle().setBorderColor(Color.GREEN).setBorderWidth(10)

//            setData()
        }
        binding.callLogWithDetails.setCallLogDetailsConfiguration(callLogDetailsConfig)
    }
}