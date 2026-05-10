package com.oficial.viasit.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vehicles")
data class AutoEntity(
    @PrimaryKey
    val id: String = "",
    val primaryUserId: String = "",
    val placa: String = "",
    val code: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val angulo: Double = 0.0
)

fun Auto.toEntity(): AutoEntity {
    return AutoEntity(
        id = this.id,
        primaryUserId = this.userId,
        placa = this.placa,
        code = this.code,
        lat = this.lat,
        lng = this.lng,
        angulo = this.angulo,
        colectivoid = this.colectivoid
    )
}

fun List<Auto>.toEntities(): List<AutoEntity> {
    return this.map { it.toEntity() }
}

fun AutoEntity.toAuto(): Auto {
    return Auto(
        id = this.id,
        userId = this.primaryUserId,
        placa = this.placa,
        code = this.code,
        lat = this.lat,
        lng = this.lng,
        angulo = this.angulo,
        colectivoid = this.colectivoid
    )
}

fun List<AutoEntity>.toAutos(): List<Auto> {
    return this.map { it.toAuto() }
}

@Dao
interface AutoData {
    @Query("SELECT * FROM vehicles")
    fun getAllAutos(): Flow<List<AutoEntity>>

    @Upsert
    suspend fun insertOrUpdateAutos(autos: List<AutoEntity>)

    @Query("DELETE FROM vehicles")
    suspend fun clearAll()
}
