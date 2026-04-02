package com.cryptotracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val coinName: String,
    val targetPrice: Double,
    val isAbove: Boolean, // true = alert when price goes above target
    val createdAt: Long = System.currentTimeMillis(),
    val triggered: Boolean = false
)
