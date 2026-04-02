package com.cryptotracker.domain.model

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
    val marketCap: Long,
    val marketCapRank: Int,
    val totalVolume: Double,
    val high24h: Double,
    val low24h: Double,
    val priceChangePercentage24h: Double,
    val circulatingSupply: Double,
    val ath: Double,
    val sparkline: List<Double>,
    val isFavorite: Boolean = false
)
