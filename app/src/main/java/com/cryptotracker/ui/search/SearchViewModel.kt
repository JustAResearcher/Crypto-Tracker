package com.cryptotracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.domain.model.Coin
import com.cryptotracker.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Coin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<Coin>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Cache of all market coins for instant local filtering
    private var cachedMarkets: List<CoinMarketDto> = emptyList()

    val uiState: StateFlow<SearchUiState> = combine(
        _query, _results, _isLoading, _error
    ) { query, results, loading, error ->
        SearchUiState(query = query, results = results, isLoading = loading, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    private var searchJob: Job? = null

    init {
        // Pre-load market data (including pinned coins) for instant local search
        viewModelScope.launch {
            try {
                val markets = repository.getMarkets()
                val hasMewc = markets.any { it.id == "meowcoin" }
                cachedMarkets = if (hasMewc) {
                    markets
                } else {
                    val mewc = try {
                        repository.getCoinsByIds(listOf("meowcoin"))
                    } catch (_: Exception) { emptyList() }
                    markets + mewc
                }
            } catch (_: Exception) { }
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _results.value = emptyList()
            _error.value = null
            return
        }

        val q = query.trim().lowercase()

        // Instant local filter on cached markets
        val localMatches = cachedMarkets.filter { dto ->
            dto.name.lowercase().contains(q) ||
            dto.symbol.lowercase().contains(q) ||
            dto.id.lowercase().contains(q)
        }.map { it.toCoin() }

        if (localMatches.isNotEmpty()) {
            _results.value = localMatches
            _error.value = null
            return
        }

        // Fall back to API search if no local matches
        searchJob = viewModelScope.launch {
            delay(250)
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.search(query)
                // Fetch full market data for matched coins so we can show prices
                val ids = response.coins.take(20).map { it.id }
                if (ids.isNotEmpty()) {
                    val coins = repository.getCoinsByIds(ids)
                    _results.value = coins.map { it.toCoin() }
                } else {
                    _results.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _query.value = ""
        _results.value = emptyList()
        _error.value = null
    }
}

private fun CoinMarketDto.toCoin(): Coin {
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
        sparkline = sparklineIn7d?.price ?: emptyList()
    )
}
