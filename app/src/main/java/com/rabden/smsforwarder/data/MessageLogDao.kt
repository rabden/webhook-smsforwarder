package com.rabden.smsforwarder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MessageLog>>

    @Query("SELECT * FROM message_logs WHERE id = :id")
    suspend fun getLogById(id: Long): MessageLog?

    @Query("UPDATE message_logs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Insert
    suspend fun insert(log: MessageLog): Long

    @Query("DELETE FROM message_logs")
    suspend fun deleteAll()
}