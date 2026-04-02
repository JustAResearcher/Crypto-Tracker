package com.cryptotracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.domain.model.Coin
import com.cryptotracker.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val coins: List<Coin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _rawCoins = MutableStateFlow<List<CoinMarketDto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _favoriteIds = repository.getAllFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        _rawCoins,
        _isLoading,
        _error,
        _favoriteIds
    ) { coins, loading, error, favoriteIds ->
        HomeUiState(
            coins = coins.map { it.toCoin(favoriteIds.contains(it.id)) },
            isLoading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        loadMarkets()
    }

    fun loadMarkets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val markets = repository.getMarkets()
                val hasMewc = markets.any { it.id == PINNED_COIN_ID }
                val merged = if (hasMewc) {
                    markets
                } else {
                    val mewc = try {
                        repository.getCoinsByIds(listOf(PINNED_COIN_ID))
                    } catch (_: Exception) {
                        emptyList()
                    }
                    markets + mewc
                }
                _rawCoins.value = merged
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load market data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val PINNED_COIN_ID = "meowcoin"
    }

    fun toggleFavorite(coinId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(coinId, isFavorite)
        }
    }
}

private fun CoinMarketDto.toCoin(isFavorite: Boolean): Coin {
    return Coin(
        id = id,
        symbol = symbol,
        name = name,
        imageUrl = image,
        currentPrice = currentPrice ?: 0.0,
        marketCap = marketCap ?: 0.0,
        marketCapRank = marketCapRank ?: 0,
        totalVolume = totalVolume ?: 0.0,
        high24h = high24h ?: 0.0,
        low24h = low24h ?: 0.0,
        priceChangePercentage24h = priceChangePercentage24h ?: 0.0,
        circulatingSupply = circulatingSupply ?: 0.0,
        ath = ath ?: 0.0,
        sparkline = sparklineIn7d?.price ?: emptyList(),
        isFavorite = isFavorite
    )
}
