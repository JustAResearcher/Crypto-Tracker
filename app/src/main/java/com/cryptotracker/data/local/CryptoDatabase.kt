package com.cryptotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        PriceAlertEntity::class,
        CoinOrderEntity::class,
        PortfolioEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun coinOrderDao(): CoinOrderDao
    abstract fun portfolioDao(): PortfolioDao
}
