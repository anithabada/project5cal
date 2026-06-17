package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val calculationHistoryDao: CalculationHistoryDao) {
    val historyList: Flow<List<CalculationHistory>> = calculationHistoryDao.getAllHistory()

    suspend fun insert(history: CalculationHistory) {
        calculationHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistory(id: Int) {
        calculationHistoryDao.deleteHistoryById(id)
    }

    suspend fun clearAll() {
        calculationHistoryDao.clearAllHistory()
    }
}
