package com.example.apfinalproject.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.adapters.ChatAdapter
import com.example.apfinalproject.databinding.ChatFragmentBinding
import com.example.apfinalproject.model.Message
import com.example.apfinalproject.storage.ChatDBHelper
import com.example.apfinalproject.storage.ViewModelDBHelper

class ChatFragment : Fragment() {
    private val TAG = "ChatFragment"
    private val viewModel: MainViewModel by activityViewModels()
    private val chatDBHelper = ChatDBHelper()
    private val dBHelper = ViewModelDBHelper()
    private var chatAdapter: ChatAdapter? = null
    private lateinit var binding: ChatFragmentBinding
    private lateinit var navController: NavController
    private val args: ChatFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        binding = ChatFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        viewModel.hideActionBar()
        navController = findNavController()

        val conversationID = args.Conversation.id

        chatAdapter = ChatAdapter(viewModel)
        val activeUser = viewModel.getActiveUser()

        // I think this db call is unnecessary because users are already in the conversation arg
        if (activeUser != null) {
            chatDBHelper.getChatUsers(conversationID) { users ->
                val otherUser = users.filter { it != activeUser.id }
                dBHelper.fetchUserByUid(otherUser[0]) {
                    binding.chatTitle.text = it?.firstName
                }
            }
        }

        chatDBHelper.fetchMessages(conversationID) { messages ->
            chatAdapter?.submitList(messages)
            binding.chatRV.scrollToPosition(chatAdapter!!.itemCount - 1)
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageET.text.toString()

            if (messageText.isEmpty()) {
                return@setOnClickListener
            }

            val message =
                activeUser?.let { it1 ->
                    Message(
                        senderID = it1.id,
                        messageText = messageText,
                    )
                }
            if (message != null) {
                chatDBHelper.uploadMessage(conversationID, message) {
                    Log.d(TAG, "Message uploaded")
                    if (it) {
                        // Figure out how to update the last item in the recycler view
                        // itemChangedAt does update up the time initially, only on reload
                        // I think this is because the timestamp is set by firestore on upload
                        chatAdapter?.notifyDataSetChanged()
                        binding.chatRV.scrollToPosition(chatAdapter!!.itemCount - 1)
                        // Update the conversation's last message and timestamp
                        chatDBHelper.updateConversationLastMessage(conversationID, message) {
                            Log.d(TAG, "Conversation updated")
                        }
                    } else {
                        Log.d(TAG, "Failed to upload message")
                    }
                }
            }
            binding.messageET.text.clear()
        }

        binding.chatRV.adapter = chatAdapter
        binding.chatRV.layoutManager = LinearLayoutManager(context)

        binding.backButton.setOnClickListener {
            navController.navigate(ChatFragmentDirections.actionChatFragmentToChatListFragment())
        }
    }
}
