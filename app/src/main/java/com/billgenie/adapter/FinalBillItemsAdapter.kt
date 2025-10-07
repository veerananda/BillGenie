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

class FinalBillItemsAdapter : ListAdapter<BillItemDisplay, FinalBillItemsAdapter.FinalBillItemViewHolder>(BillItemDiffCallback()) {

    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FinalBillItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_final_bill_item, parent, false)
        return FinalBillItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: FinalBillItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FinalBillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemDetails: TextView = itemView.findViewById(R.id.tvItemDetails)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)

        fun bind(billItem: BillItemDisplay) {
            tvItemName.text = billItem.itemName
            tvItemDetails.text = "${billItem.quantity} x ${rupeeFormatter.format(billItem.itemPrice)}"
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