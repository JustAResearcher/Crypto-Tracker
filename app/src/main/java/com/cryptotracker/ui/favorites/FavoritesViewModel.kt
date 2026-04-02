package com.cryptotracker.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptotracker.data.remote.dto.CoinMarketDto
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

data class FavoritesUiState(
    val coins: List<Coin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _rawCoins = MutableStateFlow<List<CoinMarketDto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _favoriteIds = repository.getAllFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<FavoritesUiState> = combine(
        _rawCoins,
        _isLoading,
        _error,
        _favoriteIds
    ) { coins, loading, error, favoriteIds ->
        FavoritesUiState(
            coins = coins
                .filter { favoriteIds.contains(it.id) }
                .map { dto ->
                    Coin(
                        id = dto.id,
                        symbol = dto.symbol,
                        name = dto.name,
                        imageUrl = dto.image,
                        currentPrice = dto.currentPrice ?: 0.0,
                        marketCap = dto.marketCap ?: 0.0,
                        marketCapRank = dto.marketCapRank ?: 0,
                        totalVolume = dto.totalVolume ?: 0.0,
                        high24h = dto.high24h ?: 0.0,
                        low24h = dto.low24h ?: 0.0,
                        priceChangePercentage24h = dto.priceChangePercentage24h ?: 0.0,
                        circulatingSupply = dto.circulatingSupply ?: 0.0,
                        ath = dto.ath ?: 0.0,
                        sparkline = dto.sparklineIn7d?.price ?: emptyList(),
                        isFavorite = true
                    )
                },
            isLoading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    init {
        viewModelScope.launch {
            _favoriteIds.collect { ids ->
                if (ids.isNotEmpty()) {
                    loadFavoriteCoins(ids)
                } else {
                    _rawCoins.value = emptyList()
                }
            }
        }
    }

    private fun loadFavoriteCoins(ids: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val coins = repository.getCoinsByIds(ids)
                _rawCoins.value = coins
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load favorites"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val ids = _favoriteIds.value
            if (ids.isNotEmpty()) {
                loadFavoriteCoins(ids)
            }
        }
    }

    fun toggleFavorite(coinId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(coinId, isFavorite)
        }
    }
}
