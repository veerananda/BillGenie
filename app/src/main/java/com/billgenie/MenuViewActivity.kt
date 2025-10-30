package com.billgenie

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.databinding.ActivityMenuViewBinding
import com.billgenie.model.MenuItem as MenuItemModel

class MenuViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuViewBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        handleIntent()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Menu"
    }
    
    private fun handleIntent() {
        val data = intent.data
        if (data?.scheme == "billgenie" && data.host == "menu") {
            val menuData = data.getQueryParameter("data")
            if (!menuData.isNullOrEmpty()) {
                displayMenu(parseMenuData(menuData))
            }
        }
    }
    
    private fun parseMenuData(data: String): List<MenuItemModel> {
        return data.split("|").flatMap { categoryData ->
            val parts = categoryData.split(";")
            val category = parts[0]
            parts.drop(1).map { itemData ->
                val (name, veg, price) = itemData.split(",")
                MenuItemModel(
                    id = 0, // Temporary ID
                    name = name,
                    price = price.toDouble(),
                    category = category,
                    isVegetarian = veg == "v",
                    isEnabled = true
                )
            }
        }
    }
    
    private fun displayMenu(items: List<MenuItemModel>) {
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = MenuAdapter(items)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}