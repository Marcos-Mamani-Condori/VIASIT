package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.repository.IAutoRepository

class StartRealtimeAutosUseCase(private val repository: IAutoRepository) {
    operator fun invoke() = repository.startRealtimeSubscription()
}

class StopRealtimeAutosUseCase(private val repository: IAutoRepository) {
    operator fun invoke() = repository.stopRealtimeSubscription()
}

class RefreshAutosUseCase(private val repository: IAutoRepository) {
    suspend operator fun invoke() = repository.refreshAutos()
}

class SyncAutosUseCase(private val repository: IAutoRepository) {
    suspend operator fun invoke() = repository.syncWithLocalCache()
}
