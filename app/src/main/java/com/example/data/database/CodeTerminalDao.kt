package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeTerminalDao {

    // --- Code Files Queries ---
    @Query("SELECT * FROM code_files ORDER BY isReadOnly DESC, name ASC")
    fun getAllCodeFiles(): Flow<List<CodeFile>>

    @Query("SELECT * FROM code_files WHERE id = :id LIMIT 1")
    suspend fun getCodeFileById(id: Int): CodeFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeFile(codeFile: CodeFile): Long

    @Update
    suspend fun updateCodeFile(codeFile: CodeFile)

    @Delete
    suspend fun deleteCodeFile(codeFile: CodeFile)

    @Query("SELECT COUNT(*) FROM code_files")
    suspend fun getCodeFilesCount(): Int


    // --- Terminal Logs Queries ---
    @Query("SELECT * FROM terminal_logs ORDER BY timestamp ASC, id ASC")
    fun getAllTerminalLogs(): Flow<List<TerminalLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalLog(log: TerminalLog)

    @Query("DELETE FROM terminal_logs")
    suspend fun clearAllTerminalLogs()
}
