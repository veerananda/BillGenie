package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.google.android.material.card.MaterialCardView

class CategorySelectionAdapter(
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CategorySelectionAdapter.CategoryViewHolder>() {

    private var categories = listOf<String>()
    private var selectedPosition = -1

    fun updateCategories(newCategories: List<String>) {
        categories = newCategories
        selectedPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selection, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardCategory: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        private val textCategoryName: TextView = itemView.findViewById(R.id.textCategoryName)
        private val textItemCount: TextView = itemView.findViewById(R.id.textItemCount)

        fun bind(category: String, isSelected: Boolean) {
            textCategoryName.text = category
            textItemCount.text = "Category"

            // Update card appearance based on selection
            if (isSelected) {
                cardCategory.strokeWidth = 4
                cardCategory.strokeColor = itemView.context.getColor(R.color.primary)
            } else {
                cardCategory.strokeWidth = 0
            }

            cardCategory.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                // Notify changes for selection state
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                
                onCategorySelected(category)
            }
        }
    }
}