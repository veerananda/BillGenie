package com.billgenie.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.billgenie.model.CustomerOrder
import com.billgenie.model.OrderStatus

@Dao
interface CustomerOrderDao {
    
    @Query("SELECT * FROM customer_orders WHERE status = :status ORDER BY orderTimestamp ASC")
    fun getAllPendingOrders(status: OrderStatus = OrderStatus.PENDING): LiveData<List<CustomerOrder>>
    
    @Query("SELECT * FROM customer_orders WHERE status = :status ORDER BY orderTimestamp ASC")
    suspend fun getAllPendingOrdersSync(status: OrderStatus = OrderStatus.PENDING): List<CustomerOrder>
    
    @Query("SELECT * FROM customer_orders WHERE status = :status ORDER BY orderTimestamp ASC")
    suspend fun getOrdersByStatus(status: OrderStatus): List<CustomerOrder>
    
    @Query("SELECT * FROM customer_orders WHERE customerNumber = :customerNumber")
    suspend fun getOrderByCustomerNumber(customerNumber: Int): CustomerOrder?
    
    @Query("SELECT * FROM customer_orders WHERE tableName = :tableName AND status = :status")
    suspend fun getActiveOrderByTableName(tableName: String, status: OrderStatus = OrderStatus.PENDING): CustomerOrder?
    
    @Query("SELECT COUNT(*) FROM customer_orders WHERE tableName = :tableName AND status = :status")
    suspend fun countActiveOrdersByTableName(tableName: String, status: OrderStatus = OrderStatus.PENDING): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(customerOrder: CustomerOrder)
    
    @Update
    suspend fun updateOrder(customerOrder: CustomerOrder)
    
    @Query("UPDATE customer_orders SET status = :status WHERE customerNumber = :customerNumber")
    suspend fun updateOrderStatus(customerNumber: Int, status: OrderStatus)
    
    @Delete
    suspend fun deleteOrder(customerOrder: CustomerOrder)
    
    @Query("DELETE FROM customer_orders WHERE customerNumber = :customerNumber")
    suspend fun deleteOrderByCustomerNumber(customerNumber: Int)
    
    @Query("DELETE FROM customer_orders WHERE status = :status")
    suspend fun deleteOrdersByStatus(status: OrderStatus)
    
    @Query("SELECT COUNT(*) FROM customer_orders WHERE status = :status")
    suspend fun countOrdersByStatus(status: OrderStatus = OrderStatus.PENDING): Int
    
    @Query("SELECT MAX(customerNumber) FROM customer_orders")
    suspend fun getMaxCustomerNumber(): Int?
}