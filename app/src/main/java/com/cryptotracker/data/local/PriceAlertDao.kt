package com.cryptotracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {

    @Query("SELECT * FROM price_alerts WHERE triggered = 0 ORDER BY createdAt DESC")
    fun getActiveAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE triggered = 0")
    suspend fun getActiveAlertsList(): List<PriceAlertEntity>

    @Query("SELECT * FROM price_alerts WHERE coinId = :coinId AND triggered = 0")
    fun getAlertsForCoin(coinId: String): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity)

    @Update
    suspend fun updateAlert(alert: PriceAlertEntity)

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Int)

    @Query("UPDATE price_alerts SET triggered = 1 WHERE id = :alertId")
    suspend fun markTriggered(alertId: Int)
}
