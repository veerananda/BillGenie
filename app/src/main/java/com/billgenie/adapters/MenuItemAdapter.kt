package com.billgenie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.databinding.ItemMenuItemBinding
import com.billgenie.model.MenuItem
import java.text.NumberFormat
import java.util.*

class MenuItemAdapter(
    private val onMenuItemClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MenuItemAdapter.MenuItemViewHolder>(MenuItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val binding = ItemMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuItemViewHolder(
        private val binding: ItemMenuItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.textViewItemName.text = menuItem.name
            
            val currency = NumberFormat.getCurrencyInstance(Locale.US)
            binding.textViewItemPrice.text = currency.format(menuItem.price)
            
            if (!menuItem.description.isNullOrBlank()) {
                binding.textViewItemDescription.text = menuItem.description
                binding.textViewItemDescription.visibility = View.VISIBLE
            } else {
                binding.textViewItemDescription.visibility = View.GONE
            }
            
            binding.root.setOnClickListener {
                onMenuItemClick(menuItem)
            }
        }
    }

    private class MenuItemDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem == newItem
        }
    }
}