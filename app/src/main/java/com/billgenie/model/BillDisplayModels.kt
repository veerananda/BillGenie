package com.billgenie.model

import java.io.Serializable

// Data class for displaying bill items in RecyclerView
data class BillItemDisplay(
    val id: Long = 0,
    val menuItemId: Long,
    val itemName: String,
    val categoryName: String = "General", // Default value for backward compatibility
    val itemPrice: Double,
    val quantity: Int,
    val totalPrice: Double = itemPrice * quantity
) : Serializable

// Data class for bill summary
data class BillSummary(
    val customerName: String,
    val customerPhone: String,
    val items: List<BillItemDisplay>,
    val totalAmount: Double,
    val dateCreated: String
) : Serializable