package com.cryptotracker.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptotracker.data.local.PortfolioEntity
import com.cryptotracker.data.remote.dto.CoinMarketDto
import com.cryptotracker.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioHolding(
    val entryId: Int,
    val coinId: String,
    val coinName: String,
    val imageUrl: String,
    val quantity: Double,
    val currentPrice: Double,
    val totalValue: Double,
    val priceChangePercentage24h: Double
)

data class PortfolioUiState(
    val holdings: List<PortfolioHolding> = emptyList(),
    val totalValue: Double = 0.0,
    val totalChange24h: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _entries = repository.getPortfolioEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _prices = MutableStateFlow<List<CoinMarketDto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _showAddDialog = MutableStateFlow(false)

    val uiState: StateFlow<PortfolioUiState> = combine(
        _entries, _prices, _isLoading, _error, _showAddDialog
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val entries = values[0] as List<PortfolioEntity>
        val prices = values[1] as List<CoinMarketDto>
        val loading = values[2] as Boolean
        val error = values[3] as String?
        val showDialog = values[4] as Boolean

        val priceMap = prices.associateBy { it.id }
        val holdings = entries.map { entry ->
            val dto = priceMap[entry.coinId]
            val price = dto?.currentPrice ?: 0.0
            val change = dto?.priceChangePercentage24h ?: 0.0
            PortfolioHolding(
                entryId = entry.id,
                coinId = entry.coinId,
                coinName = entry.coinName,
                imageUrl = dto?.image ?: "",
                quantity = entry.quantity,
                currentPrice = price,
                totalValue = entry.quantity * price,
                priceChangePercentage24h = change
            )
        }
        val totalValue = holdings.sumOf { it.totalValue }
        // Weighted average 24h change
        val totalChange = if (totalValue > 0) {
            holdings.sumOf { it.priceChangePercentage24h * it.totalValue } / totalValue
        } else 0.0

        PortfolioUiState(
            holdings = holdings,
            totalValue = totalValue,
            totalChange24h = totalChange,
            isLoading = loading,
            error = error,
            showAddDialog = showDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioUiState())

    init {
        viewModelScope.launch {
            _entries.collect { entries ->
                if (entries.isNotEmpty()) {
                    refreshPrices(entries.map { it.coinId }.distinct())
                }
            }
        }
    }

    private fun refreshPrices(ids: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _prices.value = repository.getCoinsByIds(ids)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        val ids = _entries.value.map { it.coinId }.distinct()
        if (ids.isNotEmpty()) refreshPrices(ids)
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun addHolding(coinId: String, coinName: String, quantity: Double) {
        viewModelScope.launch {
            repository.addPortfolioEntry(
                PortfolioEntity(coinId = coinId, coinName = coinName, quantity = quantity)
            )
            _showAddDialog.value = false
        }
    }

    fun deleteHolding(entryId: Int) {
        viewModelScope.launch {
            repository.deletePortfolioEntry(entryId)
        }
    }
}
