package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.repository.IAutoRepository

class GetAutoByUserIdUseCase(private val repository: IAutoRepository) {
    suspend operator fun invoke(userId: String): Result<Auto?> = repository.getAutoByUserId(userId)
}
