package com.cryptotracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptotracker.data.local.PriceAlertEntity
import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.domain.model.ChartData
import com.cryptotracker.domain.model.Coin
import com.cryptotracker.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val coin: Coin? = null,
    val chartData: List<ChartData> = emptyList(),
    val selectedPeriod: String = "7",
    val alerts: List<PriceAlertEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isChartLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CryptoRepository
) : ViewModel() {

    private val coinId: String = savedStateHandle.get<String>("coinId") ?: ""

    private val _coin = MutableStateFlow<Coin?>(null)
    private val _chartData = MutableStateFlow<List<ChartData>>(emptyList())
    private val _selectedPeriod = MutableStateFlow("7")
    private val _isLoading = MutableStateFlow(false)
    private val _isChartLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isFavorite = repository.isFavorite(coinId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val _alerts = repository.getAlertsForCoin(coinId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DetailUiState> = combine(
        _coin,
        _chartData,
        _selectedPeriod,
        _isLoading,
        _isChartLoading,
        _error,
        _isFavorite,
        _alerts
    ) { values ->
        DetailUiState(
            coin = values[0] as Coin?,
            chartData = values[1] as List<ChartData>,
            selectedPeriod = values[2] as String,
            isLoading = values[3] as Boolean,
            isChartLoading = values[4] as Boolean,
            error = values[5] as String?,
            isFavorite = values[6] as Boolean,
            alerts = values[7] as List<PriceAlertEntity>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    init {
        loadCoinData()
        loadChart("7")
    }

    private fun loadCoinData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val coins = repository.getCoinsByIds(listOf(coinId))
                coins.firstOrNull()?.let { dto ->
                    _coin.value = dto.toCoin()
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChart(days: String) {
        _selectedPeriod.value = days
        viewModelScope.launch {
            _isChartLoading.value = true
            try {
                val chart = repository.getMarketChart(coinId, days)
                _chartData.value = chart.prices.map { point ->
                    ChartData(
                        timestamp = point[0].toLong(),
                        price = point[1]
                    )
                }
            } catch (e: Exception) {
                // Chart error, keep existing data
            } finally {
                _isChartLoading.value = false
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(coinId, _isFavorite.value)
        }
    }

    fun addAlert(targetPrice: Double, isAbove: Boolean) {
        viewModelScope.launch {
            repository.addAlert(
                PriceAlertEntity(
                    coinId = coinId,
                    coinName = _coin.value?.name ?: coinId,
                    targetPrice = targetPrice,
                    isAbove = isAbove
                )
            )
        }
    }

    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }
}

private fun CoinMarketDto.toCoin(): Coin {
    return Coin(
        id = id,
        symbol = symbol,
        name = name,
        imageUrl = image,
        currentPrice = currentPrice ?: 0.0,
        marketCap = marketCap ?: 0L,
        marketCapRank = marketCapRank ?: 0,
        totalVolume = totalVolume ?: 0.0,
        high24h = high24h ?: 0.0,
        low24h = low24h ?: 0.0,
        priceChangePercentage24h = priceChangePercentage24h ?: 0.0,
        circulatingSupply = circulatingSupply ?: 0.0,
        ath = ath ?: 0.0,
        sparkline = sparklineIn7d?.price ?: emptyList()
    )
}
