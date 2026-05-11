package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.repository.IAdminRepository

class GetLineasUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(): Result<List<Linea>> = repository.getLineas()
}

class CreateLineaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(name: String, code: String, rutaId: String = ""): Result<Linea> =
        repository.createLinea(name, code, rutaId)
}

class UpdateLineaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(id: String, name: String, code: String, rutaId: String = ""): Result<Linea> =
        repository.updateLinea(id, name, code, rutaId)
}

class DeleteLineaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteLinea(id)
}
