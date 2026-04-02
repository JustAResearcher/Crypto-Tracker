package com.cryptotracker.repository

import com.cryptotracker.data.local.CryptoDatabase
import com.cryptotracker.data.local.FavoriteEntity
import com.cryptotracker.data.local.PriceAlertEntity
import com.cryptotracker.data.remote.CoinGeckoApi
import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.data.remote.dto.MarketChartDto
import com.cryptotracker.data.remote.dto.SearchResponseDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepository @Inject constructor(
    private val api: CoinGeckoApi,
    private val db: CryptoDatabase
) {
    // Remote
    suspend fun getMarkets(page: Int = 1): List<CoinMarketDto> =
        api.getMarkets(page = page)

    suspend fun search(query: String): SearchResponseDto =
        api.search(query)

    suspend fun getMarketChart(coinId: String, days: String): MarketChartDto =
        api.getMarketChart(coinId = coinId, days = days)

    suspend fun getCoinsByIds(ids: List<String>): List<CoinMarketDto> =
        if (ids.isEmpty()) emptyList()
        else api.getCoinsByIds(ids = ids.joinToString(","))

    // Favorites
    fun getAllFavoriteIds(): Flow<List<String>> =
        db.favoriteDao().getAllFavoriteIds()

    fun isFavorite(coinId: String): Flow<Boolean> =
        db.favoriteDao().isFavorite(coinId)

    suspend fun toggleFavorite(coinId: String, isFavorite: Boolean) {
        if (isFavorite) {
            db.favoriteDao().removeFavorite(coinId)
        } else {
            db.favoriteDao().addFavorite(FavoriteEntity(coinId = coinId))
        }
    }

    // Alerts
    fun getActiveAlerts(): Flow<List<PriceAlertEntity>> =
        db.priceAlertDao().getActiveAlerts()

    suspend fun getActiveAlertsList(): List<PriceAlertEntity> =
        db.priceAlertDao().getActiveAlertsList()

    fun getAlertsForCoin(coinId: String): Flow<List<PriceAlertEntity>> =
        db.priceAlertDao().getAlertsForCoin(coinId)

    suspend fun addAlert(alert: PriceAlertEntity) =
        db.priceAlertDao().insertAlert(alert)

    suspend fun deleteAlert(alertId: Int) =
        db.priceAlertDao().deleteAlert(alertId)

    suspend fun markAlertTriggered(alertId: Int) =
        db.priceAlertDao().markTriggered(alertId)
}
