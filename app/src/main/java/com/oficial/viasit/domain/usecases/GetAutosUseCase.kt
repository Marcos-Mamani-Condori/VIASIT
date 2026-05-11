package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.repository.IAutoRepository
import kotlinx.coroutines.flow.StateFlow

class GetAutosUseCase(private val repository: IAutoRepository) {
    operator fun invoke(): StateFlow<List<Auto>> = repository.autos
}
