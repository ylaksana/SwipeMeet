package com.example.apfinalproject.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.databinding.InterestItemBinding

class InterestAdapter(
    viewModel: MainViewModel,
    private val editMode: Boolean = false,
) :
    ListAdapter<String, InterestAdapter.InterestViewHolder>(InterestDiff()) {
    var tempInterests = viewModel.observeInterests().value?.toMutableList() ?: mutableListOf()

    inner class InterestViewHolder(val interestItemBinding: InterestItemBinding) :
        RecyclerView.ViewHolder(interestItemBinding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): InterestViewHolder {
        val interestItemBinding = InterestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InterestViewHolder(interestItemBinding)
    }

    override fun onBindViewHolder(
        holder: InterestViewHolder,
        position: Int,
    ) {
        val interest = getItem(position)
        holder.interestItemBinding.interestName.text = interest
        if (editMode) {
            setInterestColor(holder.interestItemBinding, interest, holder.interestItemBinding.root.context)
            holder.interestItemBinding.root.setOnClickListener {
                updateTempInterests(interest)
                setInterestColor(holder.interestItemBinding, interest, holder.interestItemBinding.root.context)
            }
        }
    }

    private fun updateTempInterests(interest: String) {
        if (tempInterests.contains(interest)) {
            tempInterests.remove(interest)
        } else {
            tempInterests.add(interest)
        }
    }

    private fun setInterestColor(
        binding: InterestItemBinding,
        interest: String,
        context: Context,
    ) {
        if (tempInterests.contains(interest)) {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.interest_selected)
            binding.interestName.text = interest
            binding.interestName.background = drawable
        } else {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.interest_unselected)
            binding.interestName.text = context.getString(R.string.interest_unselected, interest)
            binding.interestName.background = drawable
        }
    }

    class InterestDiff : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean {
            return oldItem == newItem
        }
    }

    fun clearInterests() {
        tempInterests.clear()
    }
}
