package com.cryptotracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolio ORDER BY addedAt DESC")
    fun getAll(): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PortfolioEntity)

    @Query("DELETE FROM portfolio WHERE id = :id")
    suspend fun delete(id: Int)
}
