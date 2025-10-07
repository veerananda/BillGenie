package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.MenuItem
import com.google.android.material.card.MaterialCardView

class MenuItemSelectionAdapter(
    private val onMenuItemSelected: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuItemSelectionAdapter.MenuItemViewHolder>() {

    private var menuItems = listOf<MenuItem>()
    private var selectedPosition = -1

    fun updateMenuItems(newMenuItems: List<MenuItem>) {
        menuItems = newMenuItems
        selectedPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_item_selection, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(menuItems[position], position == selectedPosition)
    }

    override fun getItemCount() = menuItems.size

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardMenuItem: MaterialCardView = itemView.findViewById(R.id.cardMenuItem)
        private val textMenuItemName: TextView = itemView.findViewById(R.id.textMenuItemName)
        private val iconMenuType: ImageView = itemView.findViewById(R.id.iconMenuType)

        fun bind(menuItem: MenuItem, isSelected: Boolean) {
            textMenuItemName.text = "${menuItem.name} ${menuItem.category}"

            // Set vegetarian icon
            if (menuItem.isVegetarian) {
                iconMenuType.setImageResource(R.drawable.ic_eco)
                iconMenuType.setColorFilter(itemView.context.getColor(R.color.success))
            } else {
                iconMenuType.setImageResource(R.drawable.ic_restaurant_menu)
                iconMenuType.setColorFilter(itemView.context.getColor(R.color.error))
            }

            // Update card appearance based on selection
            if (isSelected) {
                cardMenuItem.strokeWidth = 4
                cardMenuItem.strokeColor = itemView.context.getColor(R.color.primary)
            } else {
                cardMenuItem.strokeWidth = 0
            }

            cardMenuItem.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                // Notify changes for selection state
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                
                onMenuItemSelected(menuItem)
            }
        }
    }
}