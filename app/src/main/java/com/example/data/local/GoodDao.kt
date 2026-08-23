package com.example.data.local

import androidx.room.*
import com.example.data.model.Good
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodDao {
    @Query("SELECT * FROM goods ORDER BY name ASC")
    fun getAllGoods(): Flow<List<Good>>

    @Query("SELECT * FROM goods WHERE id = :id LIMIT 1")
    suspend fun getGoodById(id: Long): Good?

    @Query("SELECT * FROM goods WHERE barcode = :barcode LIMIT 1")
    suspend fun getGoodByBarcode(barcode: String): Good?

    @Query("SELECT * FROM goods WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGoods(query: String): Flow<List<Good>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGood(good: Good): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoods(goods: List<Good>): List<Long>

    @Update
    suspend fun updateGood(good: Good)

    @Delete
    suspend fun deleteGood(good: Good)

    @Query("DELETE FROM goods WHERE id = :id")
    suspend fun deleteGoodById(id: Long)

    @Query("DELETE FROM goods")
    suspend fun deleteAllGoods()

    @Query("SELECT * FROM goods")
    suspend fun getAllGoodsList(): List<Good>
}
