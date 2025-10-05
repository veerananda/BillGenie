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
    private val onAddToOrder: (MenuItem, Int) -> Unit,
    private val getCurrentQuantity: (Long) -> Int = { 0 } // Function to get current quantity for menu item ID
) : ListAdapter<MenuItem, OrderMenuItemAdapter.MenuItemViewHolder>(MenuItemDiffCallback()) {

    private val sessionQuantities = mutableMapOf<Long, Int>() // Track session quantities per item

    fun resetSessionQuantities() {
        sessionQuantities.clear()
        notifyDataSetChanged()
    }

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
            
            // Get session quantity for this item (how many added from menu in this session)
            currentQuantity = sessionQuantities[menuItem.id] ?: 0
            updateQuantityDisplay()

            btnMinus.setOnClickListener {
                if (currentQuantity > 0) {
                    currentQuantity--
                    sessionQuantities[menuItem.id] = currentQuantity
                    updateQuantityDisplay()
                    // Remove 1 from order
                    onAddToOrder(menuItem, -1)
                }
            }

            btnPlus.setOnClickListener {
                currentQuantity++
                sessionQuantities[menuItem.id] = currentQuantity
                updateQuantityDisplay()
                // Add 1 to order immediately
                onAddToOrder(menuItem, 1)
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