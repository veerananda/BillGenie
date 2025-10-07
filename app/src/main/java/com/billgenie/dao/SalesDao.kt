package com.billgenie.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.billgenie.model.SalesRecord

@Dao
interface SalesDao {
    
    @Insert
    suspend fun insertSalesRecord(salesRecord: SalesRecord): Long
    
    @Query("SELECT * FROM sales_records WHERE saleDate = :date ORDER BY saleTimestamp DESC")
    suspend fun getSalesByDate(date: String): List<SalesRecord>
    
    @Query("SELECT * FROM sales_records WHERE saleMonth = :month ORDER BY saleTimestamp DESC")
    suspend fun getSalesByMonth(month: String): List<SalesRecord>
}