package com.oficial.viasit

import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAdminRepository
import com.oficial.viasit.domain.repository.IAuthRepository
import com.oficial.viasit.domain.usecases.RegisterUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUseCaseTest {

    private val authRepository = mockk<IAuthRepository>()
    private val adminRepository = mockk<IAdminRepository>()
    private val registerUseCase = RegisterUseCase(authRepository, adminRepository)

    @Test
    fun `cuando las contrasenias no coinciden debe retornar error de validacion`() = runTest {
        val request = RegisterRequest(
            email = "test@viasit.com",
            password = "123",
            passwordConfirm = "456",
            name = "Test",
            phone = "123",
            role = "usuario"
        )

        val result = registerUseCase(request)

        assertTrue(result.isFailure)
        assertEquals("Las contraseñas no coinciden", result.exceptionOrNull()?.message)
    }

    @Test
    fun `cuando el registro es exitoso en el repositorio debe retornar exito`() = runTest {
        val request = RegisterRequest("t@t.com", "123", "123", "User", "123", "usuario")
        val expectedUser = User(id = "uid", email = "t@t.com", name = "User")
        
        coEvery { authRepository.register(request) } returns Result.success(expectedUser)

        val result = registerUseCase(request)

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
    }
}
