package com.billgenie.utils

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.billgenie.model.CustomerOrder
import com.billgenie.workers.DelayedInventoryWorker
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

/**
 * Manager for scheduling delayed inventory deductions
 */
class DelayedInventoryManager(private val context: Context) {
    
    companion object {
        const val TAG = "DelayedInventoryManager"
        const val DELAY_MINUTES = 5L
    }
    
    /**
     * Schedule inventory deduction for an order after 5 minutes
     */
    fun scheduleDelayedInventoryDeduction(customerOrder: CustomerOrder): Boolean {
        return try {
            Log.d(TAG, "Scheduling delayed inventory deduction for customer ${customerOrder.customerNumber}")
            
            // Convert customer order to JSON for WorkManager
            val customerOrderJson = Gson().toJson(customerOrder)
            
            // Create input data for the worker
            val inputData = Data.Builder()
                .putString(DelayedInventoryWorker.KEY_CUSTOMER_ORDER, customerOrderJson)
                .build()
            
            // Create work request with 5-minute delay
            val workRequest = OneTimeWorkRequestBuilder<DelayedInventoryWorker>()
                .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("inventory_deduction_${customerOrder.customerNumber}")
                .build()
            
            // Schedule the work
            WorkManager.getInstance(context).enqueue(workRequest)
            
            Log.i(TAG, "Scheduled inventory deduction for customer ${customerOrder.customerNumber} to run in $DELAY_MINUTES minutes")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling delayed inventory deduction for customer ${customerOrder.customerNumber}", e)
            false
        }
    }
    
    /**
     * Cancel scheduled inventory deduction for an order (if order is cancelled/modified)
     */
    fun cancelDelayedInventoryDeduction(customerNumber: String): Boolean {
        return try {
            Log.d(TAG, "Cancelling delayed inventory deduction for customer $customerNumber")
            
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("inventory_deduction_$customerNumber")
            
            Log.i(TAG, "Cancelled delayed inventory deduction for customer $customerNumber")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling delayed inventory deduction for customer $customerNumber", e)
            false
        }
    }
}