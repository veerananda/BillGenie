package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

@Entity(tableName = "sales_records")
@TypeConverters(SalesRecordConverters::class)
data class SalesRecord(
    @PrimaryKey(autoGenerate = true)
    val salesId: Long = 0,
    val orderNumber: Int,
    val tableName: String,
    val customerName: String,
    val orderItems: List<BillItemDisplay>,
    val subtotal: Double,
    val discountAmount: Double,
    val finalTotal: Double,
    val paymentMethod: String, // "CASH" or "UPI"
    val amountPaid: Double,
    val changeAmount: Double,
    val saleTimestamp: Long = System.currentTimeMillis(),
    val saleDate: String, // Format: "2025-10-06"
    val saleMonth: String, // Format: "2025-10"
    val saleYear: String // Format: "2025"
) : Serializable



// Type converters for Room database
class SalesRecordConverters {
    
    @TypeConverter
    fun fromBillItemDisplayList(items: List<BillItemDisplay>): String {
        return Gson().toJson(items)
    }
    
    @TypeConverter
    fun toBillItemDisplayList(itemsString: String): List<BillItemDisplay> {
        val listType = object : TypeToken<List<BillItemDisplay>>() {}.type
        return Gson().fromJson(itemsString, listType)
    }
}