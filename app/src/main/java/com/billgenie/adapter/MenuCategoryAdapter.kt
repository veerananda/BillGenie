package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.database.BillGenieDatabase
import com.billgenie.model.MenuCategory
import com.billgenie.model.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuCategoryAdapter(
    private var categories: List<MenuCategory>,
    private val onCategoryEditClick: (MenuCategory) -> Unit,
    private val onCategoryDeleteClick: (MenuCategory) -> Unit,
    private val onItemAddClick: (MenuCategory) -> Unit,
    private val onItemEditClick: (MenuItem) -> Unit,
    private val onItemDeleteClick: (MenuItem) -> Unit,
    private val onItemVegStatusChange: (MenuItem, Boolean) -> Unit,
    private val onItemEnabledStatusChange: (MenuItem, Boolean) -> Unit
) : RecyclerView.Adapter<MenuCategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val ivEditCategory: ImageView = view.findViewById(R.id.ivEditCategory)
        val ivDeleteCategory: ImageView = view.findViewById(R.id.ivDeleteCategory)
        val ivAddItem: ImageView = view.findViewById(R.id.ivAddItem)
        val recyclerViewItems: RecyclerView = view.findViewById(R.id.recyclerViewItems)
        val tvNoItems: TextView = view.findViewById(R.id.tvNoItems)

        fun bind(category: MenuCategory) {
            tvCategoryName.text = category.name

            // Set up click listeners
            ivEditCategory.setOnClickListener {
                onCategoryEditClick(category)
            }

            ivDeleteCategory.setOnClickListener {
                onCategoryDeleteClick(category)
            }

            ivAddItem.setOnClickListener {
                onItemAddClick(category)
            }

            // Load menu items for this category
            loadMenuItems(category)
        }

        private fun loadMenuItems(category: MenuCategory) {
            val context = itemView.context
            val database = BillGenieDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val items = database.menuItemDao().getItemsByCategorySync(category.name)
                
                withContext(Dispatchers.Main) {
                    if (items.isEmpty()) {
                        tvNoItems.visibility = View.VISIBLE
                        recyclerViewItems.visibility = View.GONE
                    } else {
                        tvNoItems.visibility = View.GONE
                        recyclerViewItems.visibility = View.VISIBLE
                        
                        val itemAdapter = MenuItemNestedAdapter(
                            items,
                            onItemEditClick,
                            onItemDeleteClick,
                            onItemVegStatusChange,
                            onItemEnabledStatusChange
                        )
                        recyclerViewItems.adapter = itemAdapter
                        recyclerViewItems.layoutManager = LinearLayoutManager(context)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_with_items, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<MenuCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}

// Nested adapter for menu items within each category
class MenuItemNestedAdapter(
    private val items: List<MenuItem>,
    private val onItemEditClick: (MenuItem) -> Unit,
    private val onItemDeleteClick: (MenuItem) -> Unit,
    private val onItemVegStatusChange: (MenuItem, Boolean) -> Unit,
    private val onItemEnabledStatusChange: (MenuItem, Boolean) -> Unit
) : RecyclerView.Adapter<MenuItemNestedAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVegStatus: TextView = view.findViewById(R.id.tvVegStatus)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val switchItemEnabled: com.google.android.material.switchmaterial.SwitchMaterial = view.findViewById(R.id.switchItemEnabled)
        val ivEditItem: ImageView = view.findViewById(R.id.ivEditItem)
        val ivDeleteItem: ImageView = view.findViewById(R.id.ivDeleteItem)

        fun bind(item: MenuItem) {
            tvVegStatus.text = if (item.isVegetarian) "🌱" else "🍖"
            tvItemName.text = item.name
            tvItemPrice.text = "₹${String.format("%.2f", item.price)}"
            switchItemEnabled.isChecked = item.isEnabled

            // Update visual state based on enabled status
            val alpha = if (item.isEnabled) 1.0f else 0.5f
            tvItemName.alpha = alpha
            tvItemPrice.alpha = alpha
            tvVegStatus.alpha = alpha

            ivEditItem.setOnClickListener {
                onItemEditClick(item)
            }

            ivDeleteItem.setOnClickListener {
                onItemDeleteClick(item)
            }

            switchItemEnabled.setOnCheckedChangeListener { _, isChecked ->
                onItemEnabledStatusChange(item, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_item_nested, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}