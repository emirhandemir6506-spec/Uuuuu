package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code_files")
data class CodeFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val language: String,
    val content: String,
    val isReadOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "terminal_logs")
data class TerminalLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "INPUT", "OUTPUT", "SYSTEM", "ERROR"
    val text: String
)
