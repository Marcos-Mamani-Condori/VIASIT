package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.repository.IAdminRepository

class GetRutasUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(): Result<List<Ruta>> = repository.getRutas()
}

class CreateRutaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(
        name: String, description: String, startPoint: String = "",
        endPoint: String = "", lineaId: String = "", waypoints: String = ""
    ): Result<Ruta> = repository.createRuta(name, description, startPoint, endPoint, lineaId, waypoints)
}

class DeleteRutaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(rutaId: String): Result<Unit> = repository.deleteRuta(rutaId)
}

class AssignRutaToLineaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(lineaId: String, rutaId: String): Result<Unit> =
        repository.updateLineaRuta(lineaId, rutaId)
}
