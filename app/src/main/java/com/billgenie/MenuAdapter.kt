package com.billgenie

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.databinding.ItemMenuViewBinding
import com.billgenie.model.MenuItem

class MenuAdapter(private val items: List<MenuItem>) : 
    RecyclerView.Adapter<MenuAdapter.ViewHolder>() {
    
    class ViewHolder(private val binding: ItemMenuViewBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: MenuItem) {
            binding.tvItemName.text = item.name
            binding.tvPrice.text = "₹${String.format("%.2f", item.price)}"
            binding.ivVegIndicator.setImageResource(
                if (item.isVegetarian) R.drawable.ic_veg else R.drawable.ic_non_veg
            )
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
}