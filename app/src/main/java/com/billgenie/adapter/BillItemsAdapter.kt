package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.BillItemDisplay
import com.google.android.material.button.MaterialButton

class BillItemsAdapter(
    private val onQuantityChange: (BillItemDisplay, Int) -> Unit,
    private val onRemoveClick: (BillItemDisplay) -> Unit
) : ListAdapter<BillItemDisplay, BillItemsAdapter.BillItemViewHolder>(BillItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bill, parent, false)
        return BillItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBillItemName: TextView = itemView.findViewById(R.id.tvBillItemName)
        private val tvBillItemPrice: TextView = itemView.findViewById(R.id.tvBillItemPrice)
        private val tvBillItemQuantity: TextView = itemView.findViewById(R.id.tvBillItemQuantity)
        private val tvBillItemTotal: TextView = itemView.findViewById(R.id.tvBillItemTotal)
        private val btnIncreaseQuantity: TextView = itemView.findViewById(R.id.btnIncreaseQuantity)
        private val btnDecreaseQuantity: TextView = itemView.findViewById(R.id.btnDecreaseQuantity)
        private val btnRemoveItem: MaterialButton = itemView.findViewById(R.id.btnRemoveItem)

        fun bind(billItem: BillItemDisplay) {
            // Display item name with category at the end (e.g., "Pulled Chicken Burger")
            tvBillItemName.text = "${billItem.itemName} ${billItem.categoryName}"
            tvBillItemPrice.text = "₹${String.format("%.2f", billItem.itemPrice)}"
            tvBillItemQuantity.text = billItem.quantity.toString()
            tvBillItemTotal.text = "₹${String.format("%.2f", billItem.totalPrice)}"
            
            btnIncreaseQuantity.setOnClickListener { 
                onQuantityChange(billItem, billItem.quantity + 1) 
            }
            btnDecreaseQuantity.setOnClickListener { 
                onQuantityChange(billItem, billItem.quantity - 1) 
            }
            btnRemoveItem.setOnClickListener { onRemoveClick(billItem) }
        }
    }

    class BillItemDiffCallback : DiffUtil.ItemCallback<BillItemDisplay>() {
        override fun areItemsTheSame(oldItem: BillItemDisplay, newItem: BillItemDisplay): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BillItemDisplay, newItem: BillItemDisplay): Boolean {
            return oldItem == newItem
        }
    }
}