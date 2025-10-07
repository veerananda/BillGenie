package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.util.Date

@Entity(tableName = "customer_orders")
@TypeConverters(CustomerOrderConverters::class)
data class CustomerOrder(
    @PrimaryKey
    val customerNumber: Int,
    val tableName: String,
    val customerName: String,
    val orderItems: List<BillItemDisplay>,
    val total: Double,
    val orderTimestamp: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.PENDING
) : Serializable

enum class OrderStatus {
    PENDING,    // Order saved, waiting for checkout
    COMPLETED,  // Bill generated and paid
    CANCELLED   // Order cancelled
}

// Type converters for Room database
class CustomerOrderConverters {
    
    @TypeConverter
    fun fromBillItemDisplayList(items: List<BillItemDisplay>): String {
        return Gson().toJson(items)
    }
    
    @TypeConverter
    fun toBillItemDisplayList(itemsString: String): List<BillItemDisplay> {
        val listType = object : TypeToken<List<BillItemDisplay>>() {}.type
        return Gson().fromJson(itemsString, listType)
    }
    
    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toOrderStatus(statusString: String): OrderStatus {
        return OrderStatus.valueOf(statusString)
    }
}