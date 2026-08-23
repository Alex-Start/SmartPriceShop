package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "shops")
@JsonClass(generateAdapter = true)
data class Shop(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String = "",
    val colorHex: String = "#006C4C",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
