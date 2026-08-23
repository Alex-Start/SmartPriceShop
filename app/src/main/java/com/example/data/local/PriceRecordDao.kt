package com.example.data.local

import androidx.room.*
import com.example.data.model.PriceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceRecordDao {
    @Query("SELECT * FROM price_records ORDER BY updatedAt DESC")
    fun getAllPriceRecords(): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records")
    suspend fun getAllPriceRecordsList(): List<PriceRecord>

    @Query("SELECT * FROM price_records WHERE goodId = :goodId ORDER BY updatedAt DESC")
    fun getPricesForGood(goodId: Long): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records WHERE goodId = :goodId")
    suspend fun getPricesForGoodList(goodId: Long): List<PriceRecord>

    @Query("SELECT * FROM price_records WHERE shopId = :shopId")
    fun getPricesForShop(shopId: Long): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records WHERE goodId = :goodId AND shopId = :shopId LIMIT 1")
    suspend fun getPriceRecord(goodId: Long, shopId: Long): PriceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecord(record: PriceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecords(records: List<PriceRecord>): List<Long>

    @Update
    suspend fun updatePriceRecord(record: PriceRecord)

    @Delete
    suspend fun deletePriceRecord(record: PriceRecord)

    @Query("DELETE FROM price_records WHERE goodId = :goodId AND shopId = :shopId")
    suspend fun deletePriceRecord(goodId: Long, shopId: Long)

    @Query("DELETE FROM price_records WHERE goodId = :goodId")
    suspend fun deletePricesForGood(goodId: Long)

    @Query("DELETE FROM price_records WHERE shopId = :shopId")
    suspend fun deletePricesForShop(shopId: Long)

    @Query("DELETE FROM price_records")
    suspend fun deleteAllPriceRecords()
}
