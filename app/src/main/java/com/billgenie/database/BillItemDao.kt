package com.billgenie.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.billgenie.model.BillItem

@Dao
interface BillItemDao {
    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getBillItems(billId: Long): List<BillItem>

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    fun getBillItemsLive(billId: Long): LiveData<List<BillItem>>

    @Insert
    suspend fun insertBillItem(billItem: BillItem): Long

    @Insert
    suspend fun insertBillItems(billItems: List<BillItem>)

    @Update
    suspend fun updateBillItem(billItem: BillItem)

    @Delete
    suspend fun deleteBillItem(billItem: BillItem)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteBillItemsByBillId(billId: Long)

    @Query("SELECT SUM(totalPrice) FROM bill_items WHERE billId = :billId")
    suspend fun getTotalAmountForBill(billId: Long): Double?
    
    @Query("DELETE FROM bill_items WHERE billId NOT IN (SELECT id FROM bills WHERE isActive = 1)")
    suspend fun cleanupOrphanedBillItems()
    
    // Methods for monthly backup
    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: Long): List<BillItem>
    
    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteItemsForBill(billId: Long)
}