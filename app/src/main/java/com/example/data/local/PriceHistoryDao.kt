package com.example.data.local

import androidx.room.*
import com.example.data.model.PriceHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history ORDER BY recordedAt DESC")
    fun getAllPriceHistory(): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history")
    suspend fun getAllPriceHistoryList(): List<PriceHistory>

    @Query("SELECT * FROM price_history WHERE goodId = :goodId ORDER BY recordedAt DESC")
    fun getHistoryForGood(goodId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE goodId = :goodId AND shopId = :shopId ORDER BY recordedAt DESC")
    fun getHistoryForGoodAndShop(goodId: Long, shopId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE goodId = :goodId AND shopId = :shopId ORDER BY recordedAt DESC LIMIT :limit")
    suspend fun getRecentHistoryForGoodAndShop(goodId: Long, shopId: Long, limit: Int = 2): List<PriceHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PriceHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<PriceHistory>): List<Long>

    @Delete
    suspend fun deleteHistory(history: PriceHistory)

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM price_history WHERE goodId = :goodId")
    suspend fun deleteHistoryForGood(goodId: Long)

    @Query("DELETE FROM price_history WHERE shopId = :shopId")
    suspend fun deleteHistoryForShop(shopId: Long)

    @Query("DELETE FROM price_history")
    suspend fun deleteAllPriceHistory()
}
