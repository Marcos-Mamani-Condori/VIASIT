package com.oficial.viasit

import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAuthRepository
import com.oficial.viasit.domain.usecases.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val repository = mockk<IAuthRepository>()
    private val loginUseCase = LoginUseCase(repository)

    @Test
    fun `cuando el login es exitoso debe retornar el usuario`() = runTest {
        val expectedUser = User(id = "1", email = "test@viasit.com", name = "Test User")
        coEvery { repository.login("test@viasit.com", "password") } returns Result.success(expectedUser)

        val result = loginUseCase("test@viasit.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
    }

    @Test
    fun `cuando el login falla debe retornar error`() = runTest {
        val exception = Exception("Error de red")
        coEvery { repository.login(any(), any()) } returns Result.failure(exception)

        val result = loginUseCase("wrong@viasit.com", "wrong")

        assertTrue(result.isFailure)
        assertEquals("Error de red", result.exceptionOrNull()?.message)
    }
}
