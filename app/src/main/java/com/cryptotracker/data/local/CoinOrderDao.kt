package com.cryptotracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinOrderDao {

    @Query("SELECT * FROM coin_order ORDER BY position ASC")
    fun getAll(): Flow<List<CoinOrderEntity>>

    @Transaction
    suspend fun replaceAll(orders: List<CoinOrderEntity>) {
        deleteAll()
        insertAll(orders)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<CoinOrderEntity>)

    @Query("DELETE FROM coin_order")
    suspend fun deleteAll()
}
