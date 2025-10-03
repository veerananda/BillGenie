package com.billgenie.database

import androidx.lifecycle.LiveData
import com.billgenie.model.Bill
import com.billgenie.model.BillItem

class BillRepository(
    private val billDao: BillDao,
    private val billItemDao: BillItemDao
) {
    
    fun getAllBills(): LiveData<List<Bill>> = billDao.getAllBills()
    
    suspend fun getBillById(id: Long): Bill? = billDao.getBillById(id)
    
    suspend fun insertBill(bill: Bill): Long = billDao.insertBill(bill)
    
    suspend fun updateBill(bill: Bill) = billDao.updateBill(bill)
    
    suspend fun deleteBill(bill: Bill) = billDao.deleteBill(bill)
    
    suspend fun softDeleteBill(id: Long) = billDao.softDeleteBill(id)
    
    suspend fun getActiveBillCount(): Int = billDao.getActiveBillCount()
    
    // Bill Items
    suspend fun getBillItems(billId: Long): List<BillItem> = billItemDao.getBillItems(billId)
    
    fun getBillItemsLive(billId: Long): LiveData<List<BillItem>> = billItemDao.getBillItemsLive(billId)
    
    suspend fun insertBillItem(billItem: BillItem): Long = billItemDao.insertBillItem(billItem)
    
    suspend fun insertBillItems(billItems: List<BillItem>) = billItemDao.insertBillItems(billItems)
    
    suspend fun updateBillItem(billItem: BillItem) = billItemDao.updateBillItem(billItem)
    
    suspend fun deleteBillItem(billItem: BillItem) = billItemDao.deleteBillItem(billItem)
    
    suspend fun deleteBillItemsByBillId(billId: Long) = billItemDao.deleteBillItemsByBillId(billId)
    
    suspend fun getTotalAmountForBill(billId: Long): Double = billItemDao.getTotalAmountForBill(billId) ?: 0.0
    
    // Combined operations
    suspend fun createBillWithItems(bill: Bill, billItems: List<BillItem>): Long {
        val billId = insertBill(bill)
        val itemsWithBillId = billItems.map { it.copy(billId = billId) }
        insertBillItems(itemsWithBillId)
        return billId
    }
}