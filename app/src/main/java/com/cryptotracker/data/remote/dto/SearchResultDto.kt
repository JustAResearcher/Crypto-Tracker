package com.cryptotracker.data.remote.dto

data class SearchResponseDto(
    val coins: List<SearchCoinDto>
)

data class SearchCoinDto(
    val id: String,
    val name: String,
    val symbol: String,
    val thumb: String?,
    val large: String?,
    @com.google.gson.annotations.SerializedName("market_cap_rank") val marketCapRank: Int?
)
