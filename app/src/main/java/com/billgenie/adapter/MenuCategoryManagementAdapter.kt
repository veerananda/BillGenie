package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.MenuCategory

class MenuCategoryManagementAdapter(
    private var categories: List<MenuCategory>,
    private val onCategoryClick: (MenuCategory) -> Unit,
    private val onEditClick: (MenuCategory) -> Unit,
    private val onDeleteClick: (MenuCategory) -> Unit
) : RecyclerView.Adapter<MenuCategoryManagementAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)

        fun bind(category: MenuCategory) {
            tvCategoryName.text = category.name

            itemView.setOnClickListener {
                onCategoryClick(category)
            }

            ivEdit.setOnClickListener {
                onEditClick(category)
            }

            ivDelete.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_management, parent, false)
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