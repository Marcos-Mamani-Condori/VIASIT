package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.InvitationCode

interface IInvitationCodesRepository {
    suspend fun generate(role: String, lineaId: String, expiresInHours: Int): Result<InvitationCode>
    suspend fun getAll(): Result<List<InvitationCode>>
    suspend fun validate(code: String): Result<InvitationCode>
    suspend fun delete(codeId: String): Result<Unit>
    suspend fun markUsed(codeId: String, userId: String): Result<Unit>
}
