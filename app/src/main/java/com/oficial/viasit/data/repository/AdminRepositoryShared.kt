package com.oficial.viasit.data.repository

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utilidades compartidas entre todos los sub-repositorios de AdminRepository.
 * Encapsula: token de auth, fechas y generador de códigos seguros.
 */
internal class AdminRepositoryShared(
    private val authRepository: AuthRepository? = null,
    private val getToken: () -> String?
) {
    fun authToken(): String? = getToken()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")

    fun formatNowPlus(hours: Int): String =
        LocalDateTime.now(ZoneId.of("UTC")).plusHours(hours.toLong()).format(formatter)

    fun formatNow(): String =
        LocalDateTime.now(ZoneId.of("UTC")).format(formatter)

    fun generateSecureCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }
}
