package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.repository.IAutoRepository

class RegisterVehicleUseCase(private val repository: IAutoRepository) {
    suspend operator fun invoke(userId: String, placa: String, lineaCode: String): Result<Auto> {
        return repository.registerAuto(userId, placa, lineaCode)
    }
}
