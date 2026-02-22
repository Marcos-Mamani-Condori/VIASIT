package com.oficial.viasit.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.flow.Flow

/**
 * Entidad Room para vehículos (Auto)
 * NOTA: La línea del vehículo se obtiene del conductor (userId -> lineaId)
 * No se almacena linea aquí para evitar redundancia.
 */
@Entity(tableName = "autos")
data class AutoEntity(
    @PrimaryKey
    val id: String = "",
    val primaryUserId: String = "",
    val placa: String = "",
    val code: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val angulo: Double = 0.0,
    val colectivoid: String = ""
)

/**
 * Funciones de extensión para convertir entre Auto (PocketBase) y AutoEntity (Room)
 */

/**
 * Convertir Auto (PocketBase) a AutoEntity (Room)
 * userId ahora es un String (relación directa)
 */
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

/**
 * Convertir lista de Auto (PocketBase) a lista de AutoEntity (Room)
 */
fun List<Auto>.toEntities(): List<AutoEntity> {
    return this.map { it.toEntity() }
}

/**
 * Convertir AutoEntity (Room) a Auto (PocketBase)
 * Crea un Auto con userId como String (relación directa)
 */
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

/**
 * Convertir lista de AutoEntity (Room) a lista de Auto (PocketBase)
 */
fun List<AutoEntity>.toAutos(): List<Auto> {
    return this.map { it.toAuto() }
}

@Dao
interface AutoData {
    @Query("SELECT * FROM autos")
    fun getAllAutos(): Flow<List<AutoEntity>>

    @Upsert
    suspend fun insertOrUpdateAutos(autos: List<AutoEntity>)

    @Query("DELETE FROM autos")
    suspend fun clearAll()
}
