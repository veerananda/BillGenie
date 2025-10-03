package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.MenuItem

class OrderMenuItemAdapter(
    private val onAddToOrder: (MenuItem, Int) -> Unit
) : ListAdapter<MenuItem, OrderMenuItemAdapter.MenuItemViewHolder>(MenuItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_menu_item, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVegStatus: TextView = itemView.findViewById(R.id.tvVegStatus)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnMinus: ImageView = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: ImageView = itemView.findViewById(R.id.btnPlus)

        private var currentQuantity = 0

        fun bind(menuItem: MenuItem) {
            tvVegStatus.text = if (menuItem.isVegetarian) "🌱" else "🍖"
            tvItemName.text = menuItem.name
            tvItemPrice.text = "₹${String.format("%.2f", menuItem.price)}"
            
            currentQuantity = 0
            updateQuantityDisplay()

            btnMinus.setOnClickListener {
                if (currentQuantity > 0) {
                    currentQuantity--
                    updateQuantityDisplay()
                    // Automatically update order when quantity changes
                    onAddToOrder(menuItem, currentQuantity)
                }
            }

            btnPlus.setOnClickListener {
                currentQuantity++
                updateQuantityDisplay()
                // Automatically add/update item in order when quantity increases
                onAddToOrder(menuItem, currentQuantity)
            }
        }

        private fun updateQuantityDisplay() {
            tvQuantity.text = currentQuantity.toString()
            btnMinus.alpha = if (currentQuantity > 0) 1.0f else 0.3f
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