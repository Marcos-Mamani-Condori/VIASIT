package com.oficial.viasit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oficial.viasit.model.Auto

@Database(entities = [Auto::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun autoData(): AutoData

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de v1 a v2 (agregar campos si es necesario)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Si necesitas agregar columnas, hazlo aquí
                // Ejemplo: database.execSQL("ALTER TABLE autos ADD COLUMN nuevo_campo TEXT DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "viasit_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
