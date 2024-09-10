package com.example.apfinalproject.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.databinding.UserRowBinding
import com.example.apfinalproject.model.User

class UserAdapter(
    private val viewModel: MainViewModel,
    private val navigateToConversation: (User) -> Unit,
) :
    ListAdapter<User, UserAdapter.UserViewHolder>(UserDiff()) {
    inner class UserViewHolder(val userRowBinding: UserRowBinding) :
        RecyclerView.ViewHolder(userRowBinding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): UserViewHolder {
        val userRowBinding = UserRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(userRowBinding)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int,
    ) {
        val user = getItem(position)
        holder.userRowBinding.username.text = user.firstName
        viewModel.fetchUserImage(user.profileImage, holder.userRowBinding.profileImage)
        holder.userRowBinding.root.setOnClickListener {
            // Navigate to conversation
            navigateToConversation(user)
        }
    }

    class UserDiff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(
            oldItem: User,
            newItem: User,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: User,
            newItem: User,
        ): Boolean {
            return oldItem.firstName == newItem.firstName &&
                oldItem.profileImage == newItem.profileImage
        }
    }
}
