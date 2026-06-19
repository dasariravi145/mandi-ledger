package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillNumberSeriesDao {
    @Query("SELECT * FROM bill_number_series")
    fun getAllSeries(): Flow<List<BillNumberSeriesEntity>>

    @Query("SELECT * FROM bill_number_series WHERE seriesType = :type LIMIT 1")
    suspend fun getSeriesByType(type: String): BillNumberSeriesEntity?

    @Query("SELECT * FROM bill_number_series")
    suspend fun getAllSeriesList(): List<BillNumberSeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: BillNumberSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(series: BillNumberSeriesEntity)

    @Update
    suspend fun update(series: BillNumberSeriesEntity)

    @Query("DELETE FROM bill_number_series")
    suspend fun deleteAll()
}
