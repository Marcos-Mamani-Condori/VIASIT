package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.LoginFormState
import com.oficial.viasit.domain.model.RegisterFormState

object AuthValidators {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    fun validateLogin(form: LoginFormState): LoginFormState {
        return when {
            form.email.isBlank() -> form.copy(emailError = "Email requerido", passwordError = null)
            !emailRegex.matches(form.email) -> form.copy(emailError = "Email invalido", passwordError = null)
            form.password.isBlank() -> form.copy(emailError = null, passwordError = "Contrasena requerida")
            form.password.length < 6 -> form.copy(emailError = null, passwordError = "Minimo 6 caracteres")
            else -> form.copy(emailError = null, passwordError = null)
        }
    }

    fun validateRegister(form: RegisterFormState): RegisterFormState {
        return when {
            form.email.isBlank() -> form.copy(emailError = "Email requerido", passwordError = null, confirmPasswordError = null, nameError = null, error = null)
            !emailRegex.matches(form.email) -> form.copy(emailError = "Email invalido", passwordError = null, confirmPasswordError = null, nameError = null, error = null)
            form.password.isBlank() -> form.copy(emailError = null, passwordError = "Contrasena requerida", confirmPasswordError = null, nameError = null, error = null)
            form.password.length < 8 -> form.copy(emailError = null, passwordError = "Minimo 8 caracteres", confirmPasswordError = null, nameError = null, error = null)
            form.confirmPassword != form.password -> form.copy(emailError = null, passwordError = null, confirmPasswordError = "No coinciden", nameError = null, error = null)
            form.name.isBlank() -> form.copy(emailError = null, passwordError = null, confirmPasswordError = null, nameError = "Nombre requerido", error = null)
            form.role == "conductor" && form.invitationCode.isBlank() -> form.copy(emailError = null, passwordError = null, confirmPasswordError = null, nameError = null, error = "Los conductores necesitan un código de invitación")
            else -> form.copy(emailError = null, passwordError = null, confirmPasswordError = null, nameError = null, error = null)
        }
    }
}
