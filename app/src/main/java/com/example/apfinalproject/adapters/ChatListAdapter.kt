package com.example.apfinalproject.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.databinding.ChatListRowBinding
import com.example.apfinalproject.model.Conversation
import com.example.apfinalproject.model.invalidUser
import com.example.apfinalproject.storage.ViewModelDBHelper

class ChatListAdapter(
    private val viewModel: MainViewModel,
    private val dbHelper: ViewModelDBHelper,
    private val navigateToChat: (Conversation) -> Unit,
) :
    ListAdapter<Conversation, ChatListAdapter.ChatListViewHolder>(ChatListDiff()) {
    inner class ChatListViewHolder(val chatListRowBinding: ChatListRowBinding) :
        RecyclerView.ViewHolder(chatListRowBinding.root) {
        init {
            chatListRowBinding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val conversation = getItem(position)
                    navigateToChat(conversation)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ChatListViewHolder {
        val chatListRowBinding = ChatListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatListViewHolder(chatListRowBinding)
    }

    override fun onBindViewHolder(
        holder: ChatListViewHolder,
        position: Int,
    ) {
        val binding = holder.chatListRowBinding
        val conversation = getItem(position)
        val activeUser = viewModel.getActiveUser()
        val otherUser = conversation.userIDs.find { it != (activeUser?.id ?: "-1") }
        Log.d("ChatListAdapter", "Binding conversationID: ${conversation.id}")

        if (otherUser != null) {
            dbHelper.fetchUserByUid(otherUser) { user ->
                if (user != invalidUser && user != null) {
                    viewModel.fetchUserImage(user.profileImage, binding.chatUserImage)
                    binding.chatUserNameTV.text = user.firstName
                } else {
                    binding.chatUserNameTV.text = "Unknown User"
                }
            }
        }
        if (conversation.lastMessage.isEmpty()) {
            binding.chatMessageTV.text = "No messages yet"
        } else {
            if (activeUser?.id == conversation.lastMessageSender) {
                binding.chatMessageTV.text = "You: ${truncateMessage(conversation.lastMessage)}"
            } else {
                binding.chatMessageTV.text = truncateMessage(conversation.lastMessage)
            }
        }
    }

    private fun truncateMessage(message: String): String {
        return if (message.length > 40) {
            message.substring(0, 40) + "..."
        } else {
            message
        }
    }

    class ChatListDiff : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(
            oldItem: Conversation,
            newItem: Conversation,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Conversation,
            newItem: Conversation,
        ): Boolean {
            return oldItem.id == newItem.id &&
                oldItem.lastMessage == newItem.lastMessage
        }
    }
}
