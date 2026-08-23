package com.example.data.local

import androidx.room.*
import com.example.data.model.Shop
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY name ASC")
    fun getAllShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shops ORDER BY name ASC")
    suspend fun getAllShopsList(): List<Shop>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getShopById(id: Long): Shop?

    @Query("SELECT * FROM shops WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getShopByName(name: String): Shop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShops(shops: List<Shop>): List<Long>

    @Update
    suspend fun updateShop(shop: Shop)

    @Delete
    suspend fun deleteShop(shop: Shop)

    @Query("DELETE FROM shops WHERE id = :id")
    suspend fun deleteShopById(id: Long)

    @Query("DELETE FROM shops")
    suspend fun deleteAllShops()
}
