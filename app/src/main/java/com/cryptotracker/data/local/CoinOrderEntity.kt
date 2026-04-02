package com.cryptotracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_order")
data class CoinOrderEntity(
    @PrimaryKey val coinId: String,
    val position: Int
)
