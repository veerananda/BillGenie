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
import java.text.NumberFormat
import java.util.*

class CheckoutItemsAdapter : ListAdapter<BillItemDisplay, CheckoutItemsAdapter.CheckoutItemViewHolder>(BillItemDiffCallback()) {

    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkout_bill_item, parent, false)
        return CheckoutItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckoutItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CheckoutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemCategory: TextView = itemView.findViewById(R.id.tvItemCategory)
        private val tvItemPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)

        fun bind(billItem: BillItemDisplay) {
            tvItemName.text = billItem.itemName
            tvItemCategory.text = billItem.categoryName
            tvItemPrice.text = rupeeFormatter.format(billItem.itemPrice)
            tvQuantity.text = "x${billItem.quantity}"
            tvTotalPrice.text = rupeeFormatter.format(billItem.totalPrice)
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