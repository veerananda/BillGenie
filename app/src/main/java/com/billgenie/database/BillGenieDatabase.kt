package com.billgenie.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.billgenie.dao.UserDao
import com.billgenie.dao.MenuCategoryDao
// import com.billgenie.dao.MenuItemIngredientsDao
import com.billgenie.database.MenuItemDao
import com.billgenie.database.BillDao
import com.billgenie.database.BillItemDao
import com.billgenie.database.CustomerOrderDao
import com.billgenie.model.MenuItem
import com.billgenie.model.MenuCategory
import com.billgenie.model.Bill
import com.billgenie.model.BillItem
import com.billgenie.model.CustomerOrder
import com.billgenie.model.User

@Database(
    entities = [MenuItem::class, MenuCategory::class, Bill::class, BillItem::class, CustomerOrder::class, User::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class BillGenieDatabase : RoomDatabase() {
    
    abstract fun menuItemDao(): MenuItemDao
    abstract fun menuCategoryDao(): MenuCategoryDao
    // abstract fun menuItemIngredientsDao(): MenuItemIngredientsDao
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