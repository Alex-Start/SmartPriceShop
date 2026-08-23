package com.example.data.local

import androidx.room.*
import com.example.data.model.ShoppingList
import com.example.data.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    // Shopping Lists
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getShoppingListById(id: Long): ShoppingList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingLists(lists: List<ShoppingList>): List<Long>

    @Update
    suspend fun updateShoppingList(list: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(list: ShoppingList)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteShoppingListById(id: Long)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()

    @Query("SELECT * FROM shopping_lists")
    suspend fun getAllShoppingListsList(): List<ShoppingList>

    // Shopping List Items
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY isChecked ASC, createdAt ASC")
    fun getItemsForList(listId: Long): Flow<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItem>): List<Long>

    @Update
    suspend fun updateItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteItem(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId AND isChecked = 1")
    suspend fun deleteCheckedItemsForList(listId: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun deleteAllItemsForList(listId: Long)

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM shopping_list_items")
    suspend fun getAllItemsList(): List<ShoppingListItem>
}
