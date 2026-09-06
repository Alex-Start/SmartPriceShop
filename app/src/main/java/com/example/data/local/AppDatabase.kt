package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.Good
import com.example.data.model.PriceHistory
import com.example.data.model.PriceRecord
import com.example.data.model.Shop
import com.example.data.model.ShoppingList
import com.example.data.model.ShoppingListItem
import com.example.util.AppCurrency

@Database(
    entities = [
        Good::class,
        Shop::class,
        PriceRecord::class,
        PriceHistory::class,
        ShoppingList::class,
        ShoppingListItem::class,
        Category::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goodDao(): GoodDao
    abstract fun shopDao(): ShopDao
    abstract fun priceRecordDao(): PriceRecordDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // add currencyCode columns with default 'CZK' so existing rows are populated
                database.execSQL("ALTER TABLE price_records ADD COLUMN currencyCode TEXT NOT NULL DEFAULT '"+AppCurrency.CZK.code+"'")
                database.execSQL("ALTER TABLE price_history ADD COLUMN currencyCode TEXT NOT NULL DEFAULT '"+AppCurrency.CZK.code+"'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_price_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
