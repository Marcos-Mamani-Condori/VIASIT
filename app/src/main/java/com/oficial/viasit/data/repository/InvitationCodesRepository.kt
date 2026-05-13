package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.repository.IInvitationCodesRepository

internal class InvitationCodesRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : IInvitationCodesRepository {
    override suspend fun generate(role: String, lineaId: String, expiresInHours: Int, adminName: String): Result<InvitationCode> {
        return try {
            val code = shared.generateSecureCode()
            val data = mutableMapOf<String, Any>(
                "code" to code, "role" to role,
                "expiresAt" to shared.formatNowPlus(expiresInHours), "isUsed" to false,
                "adminName" to adminName
            )
            if (lineaId.isNotEmpty()) data["lineId"] = lineaId
            client.createRecord("invitation_codes", data, shared.authToken()).fold(
                onSuccess = { json ->
                    Result.success(InvitationCode(
                        id = extractStringField(json, "id"),
                        code = code, role = role, lineaId = lineaId,
                        isUsed = false, expiresAt = shared.formatNowPlus(expiresInHours)
                    ))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getAll(): Result<List<InvitationCode>> = try {
        client.getList("invitation_codes", perPage = 50, sort = "-created", authToken = shared.authToken())
            .map { parseInvitationCodesFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun validate(code: String): Result<InvitationCode> = try {
        client.getList("invitation_codes", filter = "code='$code'&&isUsed=false", authToken = shared.authToken()).fold(
            onSuccess = { json ->
                val codes = parseInvitationCodesFromJson(json)
                if (codes.isNotEmpty()) Result.success(codes.first())
                else Result.failure(Exception("Código no válido o ya usado"))
            },
            onFailure = { Result.failure(it) }
        )
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun delete(codeId: String): Result<Unit> =
        client.deleteRecord("invitation_codes", codeId, shared.authToken()).map { }

    override suspend fun markUsed(codeId: String, userId: String): Result<Unit> =
        client.updateRecord("invitation_codes", codeId,
            mapOf("isUsed" to true, "usedBy" to userId), shared.authToken()).map { }
}
