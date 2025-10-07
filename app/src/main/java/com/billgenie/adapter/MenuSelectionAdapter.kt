package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.MenuItem
import com.google.android.material.button.MaterialButton

class MenuSelectionAdapter(
    private val onItemSelected: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MenuSelectionAdapter.MenuSelectionViewHolder>(MenuItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_selection, parent, false)
        return MenuSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMenuItemName: TextView = itemView.findViewById(R.id.tvMenuItemName)
        private val btnSelectItem: MaterialButton = itemView.findViewById(R.id.btnSelectItem)

        fun bind(menuItem: MenuItem) {
            tvMenuItemName.text = menuItem.name
            
            btnSelectItem.setOnClickListener { onItemSelected(menuItem) }
            itemView.setOnClickListener { onItemSelected(menuItem) }
        }
    }

    class MenuItemDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem == newItem
        }
    }
}