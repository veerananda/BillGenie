package com.billgenie.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.billgenie.database.BillGenieDatabase
import com.billgenie.model.CustomerOrder
import com.billgenie.utils.SimpleInventoryManager
import com.google.gson.Gson

/**
 * Worker that processes inventory deductions 5 minutes after an order is saved
 */
class DelayedInventoryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DelayedInventoryWorker"
        const val KEY_CUSTOMER_ORDER = "customer_order_json"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting delayed inventory deduction")
            
            // Get the customer order from input data
            val customerOrderJson = inputData.getString(KEY_CUSTOMER_ORDER)
            if (customerOrderJson == null) {
                Log.e(TAG, "No customer order data provided")
                return Result.failure()
            }
            
            // Parse the customer order
            val customerOrder = Gson().fromJson(customerOrderJson, CustomerOrder::class.java)
            Log.d(TAG, "Processing delayed inventory deduction for customer ${customerOrder.customerNumber}")
            
            // Get database instance
            val database = BillGenieDatabase.getDatabase(applicationContext)
            
            // Check if order still exists and is pending (not cancelled)
            val currentOrder = database.customerOrderDao().getOrderByCustomerNumber(customerOrder.customerNumber)
            if (currentOrder == null) {
                Log.i(TAG, "Order no longer exists for customer ${customerOrder.customerNumber}, skipping inventory deduction")
                return Result.success()
            }
            
            if (currentOrder.status.name != "PENDING") {
                Log.i(TAG, "Order status changed to ${currentOrder.status.name} for customer ${customerOrder.customerNumber}, skipping inventory deduction")
                return Result.success()
            }
            
            // Process inventory deduction using SimpleInventoryManager
            val inventoryManager = SimpleInventoryManager(database)
            val success = inventoryManager.processOrderInventoryDeduction(currentOrder)
            
            if (success) {
                Log.i(TAG, "Successfully completed delayed inventory deduction for customer ${customerOrder.customerNumber}")
                Result.success()
            } else {
                Log.w(TAG, "Some inventory deductions failed for customer ${customerOrder.customerNumber}")
                // Consider this a success since order was valid, just some items might not have been deducted
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing delayed inventory deduction", e)
            Result.failure()
        }
    }
}