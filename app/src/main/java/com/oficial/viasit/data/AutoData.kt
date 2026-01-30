package com.oficial.viasit.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oficial.viasit.model.Auto
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoData {
    @Query("SELECT * FROM autos")
    fun getAllAutos(): Flow<List<Auto>>

    @Upsert
    suspend fun insertOrUpdateAutos(autos: List<Auto>)

    @Query("DELETE FROM autos")
    suspend fun clearAll()
}
