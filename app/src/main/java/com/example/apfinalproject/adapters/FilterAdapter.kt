package com.example.apfinalproject.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.databinding.InterestItemBinding
import com.example.apfinalproject.model.InterestCategories

class FilterAdapter(private val viewModel: MainViewModel) :
    ListAdapter<InterestCategories.Interest, FilterAdapter.VH>(InterestDiff()) {

    private var currentPosition = 0
    private var previousPosition = 0

    private fun getFirstSelectedIndex(): Int {
        for (i in 0 until itemCount) {
            if (getItem(i).selected) {
                return i
            }
        }
        return -1 // return -1 if no item is selected
    }

    inner class VH(val rowPostBinding: InterestItemBinding) :
        RecyclerView.ViewHolder(rowPostBinding.root) {
        init {
            itemView.setOnClickListener {
                Log.d("Binding index", "$bindingAdapterPosition")
                val firstSelectedIndex = getFirstSelectedIndex()
                previousPosition =
                    if (firstSelectedIndex != -1) {
                        firstSelectedIndex
                    } else {
                        bindingAdapterPosition
                    }
                currentPosition = bindingAdapterPosition
                val previousItem = getItem(previousPosition)
                val item = getItem(currentPosition)

                notifyItemChanged(previousPosition)
                notifyItemChanged(currentPosition)
                Log.d("Item Index", "current: $currentPosition, previous: $previousPosition")
                if (item == previousItem) {
                    if (item.selected)
                        {
                            item.selected = false
                            viewModel.setFilter("")
                        } else {
                        item.selected = true
                        viewModel.setFilter(item.category)
                    }
                } else if (item.selected)
                    {
                        item.selected = false
                        viewModel.setFilter("")
                    } else {
                    previousItem.selected = false
                    item.selected = true
                    viewModel.setFilter(item.category)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH {
        val interestItemBinding = InterestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(interestItemBinding)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int,
    ) {
        val rowBinding = holder.rowPostBinding
        val item = getItem(position)
        Log.d("Item Selected", "${item.category}, ${item.selected}")
        rowBinding.interestName.text = item.category
        if (item.selected)
            {
                rowBinding.interestName.setBackgroundResource(R.drawable.interest_selected)
            } else {
            rowBinding.interestName.setBackgroundResource(R.drawable.interest_unselected)
        }
    }

    class InterestDiff : DiffUtil.ItemCallback<InterestCategories.Interest>() {
        override fun areItemsTheSame(
            oldItem: InterestCategories.Interest,
            newItem: InterestCategories.Interest,
        ): Boolean {
            // Return true if the items are the same.
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(
            oldItem: InterestCategories.Interest,
            newItem: InterestCategories.Interest,
        ): Boolean {
            // Return true if the contents of the items are the same.
            return oldItem == newItem
        }
    }
}
