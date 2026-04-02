package com.cryptotracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoinMarketDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("total_volume") val totalVolume: Double?,
    @SerializedName("high_24h") val high24h: Double?,
    @SerializedName("low_24h") val low24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    val ath: Double?,
    @SerializedName("sparkline_in_7d") val sparklineIn7d: SparklineDto?
)

data class SparklineDto(
    val price: List<Double>?
)
