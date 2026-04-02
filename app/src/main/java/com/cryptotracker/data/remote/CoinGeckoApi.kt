package com.cryptotracker.data.remote

import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.data.remote.dto.MarketChartDto
import com.cryptotracker.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {

    companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }

    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): List<CoinMarketDto>

    @GET("search")
    suspend fun search(
        @Query("query") query: String
    ): SearchResponseDto

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") coinId: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String
    ): MarketChartDto

    @GET("coins/markets")
    suspend fun getCoinsByIds(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): List<CoinMarketDto>
}
