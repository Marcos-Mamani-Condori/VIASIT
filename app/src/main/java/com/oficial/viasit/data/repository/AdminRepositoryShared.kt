package com.oficial.viasit.data.repository

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class AdminRepositoryShared(
    private val authRepository: com.oficial.viasit.domain.repository.IAuthRepository? = null,
    private val getToken: () -> String?
) {
    fun authToken(): String? = getToken()
    fun getCurrentUserName(): String? = authRepository?.getCurrentUser()?.name

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")

    fun formatNowPlus(hours: Int): String =
        LocalDateTime.now(ZoneId.of("UTC")).plusHours(hours.toLong()).format(formatter)

    fun formatNow(): String =
        LocalDateTime.now(ZoneId.of("UTC")).format(formatter)

    fun getStartDateForFilter(period: String): String {
        val now = LocalDateTime.now(ZoneId.of("UTC"))
        val date = when (period.lowercase()) {
            "today" -> now.withHour(0).withMinute(0).withSecond(0)
            "yesterday" -> now.minusDays(1).withHour(0).withMinute(0).withSecond(0)
            "last_week" -> now.minusWeeks(1)
            "last_month" -> now.minusMonths(1)
            else -> return ""
        }
        return date.format(formatter)
    }

    fun generateSecureCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }
}
