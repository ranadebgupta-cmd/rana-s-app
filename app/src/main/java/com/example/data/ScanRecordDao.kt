package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteRecords(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecord): Long

    @Update
    suspend fun updateRecord(record: ScanRecord)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun clearAllRecords()
}
