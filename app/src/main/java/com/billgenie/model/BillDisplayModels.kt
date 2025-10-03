package com.billgenie.model

// Data class for displaying bill items in RecyclerView
data class BillItemDisplay(
    val id: Long = 0,
    val menuItemId: Long,
    val itemName: String,
    val itemPrice: Double,
    val quantity: Int,
    val totalPrice: Double = itemPrice * quantity
)

// Data class for bill summary
data class BillSummary(
    val customerName: String,
    val customerPhone: String,
    val items: List<BillItemDisplay>,
    val totalAmount: Double,
    val dateCreated: String
)