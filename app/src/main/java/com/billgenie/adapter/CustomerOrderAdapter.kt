package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.CustomerOrder
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.*

class CustomerOrderAdapter(
    private val onEditOrder: (CustomerOrder) -> Unit,
    private val onCheckoutOrder: (CustomerOrder) -> Unit
) : ListAdapter<CustomerOrder, CustomerOrderAdapter.CustomerOrderViewHolder>(CustomerOrderDiffCallback()) {

    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_order, parent, false)
        return CustomerOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerNumber: TextView = itemView.findViewById(R.id.tvCustomerNumber)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val tvTableName: TextView = itemView.findViewById(R.id.tvTableName)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val btnEditOrder: MaterialButton = itemView.findViewById(R.id.btnEditOrder)
        private val btnCheckoutOrder: MaterialButton = itemView.findViewById(R.id.btnCheckoutOrder)

        fun bind(customerOrder: CustomerOrder) {
            // Display table information prominently
            val displayTableName = if (customerOrder.tableName.isNotEmpty()) {
                customerOrder.tableName
            } else {
                "Table ${customerOrder.customerNumber}"
            }
            
            tvCustomerNumber.text = "Table $displayTableName"
            tvOrderStatus.text = "SAVED"
            
            // Hide table name since it's already shown at top
            tvTableName.visibility = android.view.View.GONE
            
            // Show only customer name without "Customer:" label
            tvCustomerName.text = if (customerOrder.customerName.isNotEmpty()) {
                customerOrder.customerName
            } else {
                "Walk-in Customer"
            }
            
            val itemCount = customerOrder.orderItems.sumOf { it.quantity }
            tvItemCount.text = "$itemCount item${if (itemCount != 1) "s" else ""}"
            
            tvOrderTotal.text = rupeeFormatter.format(customerOrder.total)

            // Set click listeners
            btnEditOrder.setOnClickListener { onEditOrder(customerOrder) }
            btnCheckoutOrder.setOnClickListener { onCheckoutOrder(customerOrder) }
        }
    }

    class CustomerOrderDiffCallback : DiffUtil.ItemCallback<CustomerOrder>() {
        override fun areItemsTheSame(oldItem: CustomerOrder, newItem: CustomerOrder): Boolean {
            return oldItem.customerNumber == newItem.customerNumber
        }

        override fun areContentsTheSame(oldItem: CustomerOrder, newItem: CustomerOrder): Boolean {
            return oldItem == newItem
        }
    }
}
