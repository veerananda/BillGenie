package com.billgenie

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.adapter.InventoryAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.repository.InventoryRepository
import com.billgenie.viewmodel.InventoryViewModel
import com.billgenie.viewmodel.InventoryViewModelFactory
import com.google.android.material.appbar.MaterialToolbar

class InventoryActivity : AppCompatActivity() {

    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var repository: InventoryRepository
    
    private val viewModel: InventoryViewModel by viewModels {
        InventoryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        setupRepository()
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        
        // Load inventory data
        viewModel.loadInventoryItems()
    }

    private fun setupRepository() {
        val database = BillGenieDatabase.getDatabase(applicationContext)
        val suppressNotifications = intent.getBooleanExtra("opened_from_notification", false)
        
        repository = InventoryRepository(
            ingredientDao = database.ingredientDao(),
            inventoryDao = database.inventoryDao(),
            context = if (suppressNotifications) null else applicationContext
        )
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(
            onQuantityChanged = { inventoryItem, newQuantity ->
                // Handle current stock quantity update
                viewModel.updateInventoryQuantity(inventoryItem.ingredientId, newQuantity)
            },
            onFullQuantityChanged = { inventoryItem, newFullQuantity ->
                // Handle full stock update
                viewModel.updateFullQuantity(inventoryItem.ingredientId, newFullQuantity)
            },
            onRefreshData = {
                // Refresh data when user finishes editing
                viewModel.refreshInventoryList()
            }
        )
        
        findViewById<RecyclerView>(R.id.recyclerViewInventory).apply {
            layoutManager = LinearLayoutManager(this@InventoryActivity)
            adapter = inventoryAdapter
        }
    }

    private fun setupObservers() {
        viewModel.inventoryItems.observe(this, Observer { items ->
            inventoryAdapter.updateInventory(items)
        })

        viewModel.error.observe(this, Observer { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            // You can add a loading indicator here if needed
        })
    }
}