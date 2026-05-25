package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CodeFile::class, TerminalLog::class], version = 1, exportSchema = false)
abstract class CodeTerminalDatabase : RoomDatabase() {

    abstract fun codeTerminalDao(): CodeTerminalDao

    companion object {
        @Volatile
        private var INSTANCE: CodeTerminalDatabase? = null

        fun getDatabase(context: Context): CodeTerminalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeTerminalDatabase::class.java,
                    "code_terminal_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
