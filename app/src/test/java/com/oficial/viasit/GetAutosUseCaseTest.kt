package com.oficial.viasit

import app.cash.turbine.test
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.repository.IAutoRepository
import com.oficial.viasit.domain.usecases.GetAutosUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAutosUseCaseTest {

    private val repository = mockk<IAutoRepository>()
    private val getAutosUseCase = GetAutosUseCase(repository)

    @Test
    fun `debe emitir la lista de autos desde el repositorio`() = runTest {
        val mockAutos = listOf(Auto(id = "v1", placa = "XYZ-123"))
        val autosFlow = MutableStateFlow(mockAutos)
        
        every { repository.autos } returns autosFlow

        getAutosUseCase().test {
            val emission = awaitItem()
            assertEquals(1, emission.size)
            assertEquals("XYZ-123", emission[0].placa)
        }
    }
}
