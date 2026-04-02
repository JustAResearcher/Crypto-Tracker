package com.cryptotracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val coinId: String,
    val addedAt: Long = System.currentTimeMillis()
)
