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
            form.name.isBlank() -> form.copy(nameError = "Tu nombre es necesario para la transparencia", emailError = null)
            form.name.length < 3 -> form.copy(nameError = "Nombre muy corto", emailError = null)
            form.email.isBlank() -> form.copy(emailError = "Email requerido", nameError = null)
            !emailRegex.matches(form.email) -> form.copy(emailError = "Email invalido", nameError = null)
            form.password.isBlank() -> form.copy(passwordError = "Contrasena requerida", emailError = null)
            form.password.length < 8 -> form.copy(passwordError = "Minimo 8 caracteres para tu seguridad", emailError = null)
            form.confirmPassword != form.password -> form.copy(confirmPasswordError = "Las contrasenas no coinciden")
            form.role == "conductor" && form.invitationCode.isBlank() -> form.copy(error = "El codigo de invitacion es obligatorio para conductores")
            else -> form.copy(emailError = null, passwordError = null, confirmPasswordError = null, nameError = null, error = null)
        }
    }
}
