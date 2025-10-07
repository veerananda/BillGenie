package com.billgenie.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.billgenie.dao.UserDao
import com.billgenie.dao.MenuCategoryDao
import com.billgenie.dao.IngredientDao
import com.billgenie.dao.RecipeDao
import com.billgenie.dao.InventoryDao
// import com.billgenie.dao.PendingInventoryDeductionDao // Temporarily disabled

import com.billgenie.database.MenuItemDao
import com.billgenie.database.BillDao
import com.billgenie.database.BillItemDao
import com.billgenie.database.CustomerOrderDao
import com.billgenie.dao.SalesDao
import com.billgenie.model.MenuItem
import com.billgenie.model.MenuCategory
import com.billgenie.model.Bill
import com.billgenie.model.BillItem
import com.billgenie.model.CustomerOrder
import com.billgenie.model.User
import com.billgenie.model.Ingredient
import com.billgenie.model.Recipe
import com.billgenie.model.InventoryItem
import com.billgenie.model.SalesRecord
// import com.billgenie.model.PendingInventoryDeduction // Temporarily disabled

@Database(
    entities = [MenuItem::class, MenuCategory::class, Bill::class, BillItem::class, CustomerOrder::class, User::class, Ingredient::class, Recipe::class, InventoryItem::class, SalesRecord::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class BillGenieDatabase : RoomDatabase() {
    
    abstract fun menuItemDao(): MenuItemDao
    abstract fun menuCategoryDao(): MenuCategoryDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun salesDao(): SalesDao
    abstract fun billDao(): BillDao
    abstract fun billItemDao(): BillItemDao
    abstract fun customerOrderDao(): CustomerOrderDao
    abstract fun userDao(): UserDao

    
    companion object {
        @Volatile
        private var INSTANCE: BillGenieDatabase? = null
        
        fun getDatabase(context: Context): BillGenieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillGenieDatabase::class.java,
                    "billgenie_database"
                )
                .fallbackToDestructiveMigration() // Simple approach for now
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}