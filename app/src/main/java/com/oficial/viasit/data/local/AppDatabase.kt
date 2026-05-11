package com.oficial.viasit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.oficial.viasit.data.local.AutoEntity

@Database(entities = [AutoEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun autoData(): AutoData

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "viasit_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
