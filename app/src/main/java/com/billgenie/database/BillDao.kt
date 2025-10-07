package com.billgenie.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.billgenie.model.Bill

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE isActive = 1 ORDER BY dateCreated DESC")
    fun getAllBills(): LiveData<List<Bill>>
    
    @Query("SELECT * FROM bills WHERE isActive = 1 ORDER BY dateCreated DESC")
    suspend fun getAllActiveBills(): List<Bill>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    @Insert
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("UPDATE bills SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteBill(id: Long)

    @Query("SELECT COUNT(*) FROM bills WHERE isActive = 1")
    suspend fun getActiveBillCount(): Int
    
    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getTotalBillCount(): Int
    
    @Query("UPDATE bills SET isActive = 0 WHERE dateCreated < :cutoffDate")
    suspend fun archiveOldBills(cutoffDate: String)
    
    @Query("SELECT * FROM bills WHERE dateCreated BETWEEN :startDate AND :endDate ORDER BY dateCreated DESC")
    suspend fun getBillsInDateRange(startDate: String, endDate: String): List<Bill>
    
    @Query("SELECT MIN(dateCreated) FROM bills WHERE isActive = 1")
    suspend fun getOldestBillDate(): String?
    
    @Query("SELECT MAX(dateCreated) FROM bills WHERE isActive = 1")
    suspend fun getNewestBillDate(): String?
    
    @Query("DELETE FROM bills WHERE isActive = 0 AND dateCreated < :cutoffDate")
    suspend fun permanentlyDeleteOldBills(cutoffDate: String)
    
    // Methods for monthly backup
    @Query("SELECT * FROM bills WHERE dateCreated BETWEEN :startDate AND :endDate ORDER BY dateCreated DESC")
    suspend fun getBillsByDateRange(startDate: String, endDate: String): List<Bill>
    
    @Query("DELETE FROM bills WHERE dateCreated BETWEEN :startDate AND :endDate")
    suspend fun deleteBillsByDateRange(startDate: String, endDate: String)
    
    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getTotalBillsCount(): Int
    
    @Query("SELECT COUNT(*) FROM bills WHERE dateCreated < :date")
    suspend fun getBillsCountBeforeDate(date: String): Int
}