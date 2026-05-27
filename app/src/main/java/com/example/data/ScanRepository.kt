package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val dao: ScanRecordDao) {
    val allRecords: Flow<List<ScanRecord>> = dao.getAllRecords()
    val favoriteRecords: Flow<List<ScanRecord>> = dao.getFavoriteRecords()

    suspend fun insertRecord(record: ScanRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun updateRecord(record: ScanRecord) {
        dao.updateRecord(record)
    }

    suspend fun deleteRecordById(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun clearAllRecords() {
        dao.clearAllRecords()
    }
}
