package com.cryptotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, PriceAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun priceAlertDao(): PriceAlertDao
}
