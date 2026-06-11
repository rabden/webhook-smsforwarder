package com.rabden.smsforwarder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_logs")
data class MessageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val message: String,
    val sim: String,
    val device: String,
    val timestamp: Long,
    val webhookUrl: String,
    val status: String
)