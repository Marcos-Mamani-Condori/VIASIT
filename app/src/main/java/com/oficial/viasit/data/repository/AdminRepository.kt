package com.oficial.viasit.data.repository

import com.oficial.viasit.data.remote.PocketBaseAuthClient
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Log
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AdminRepository {
    private val client = PocketBaseRealtimeClient()
    private val authClient = PocketBaseAuthClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    private fun getAuthToken(): String? = authClient.getAuthToken()

    /**
     * Generate a new invitation code
     */
    suspend fun generateInvitationCode(
        role: String = "ADMIN_LINEA",
        lineaId: String = "",
        expiresInHours: Int = 24
    ): Result<InvitationCode> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AdminRepository", "generateInvitationCode: role=$role, lineaId=$lineaId")
            
            val code = generateSecureCode()
            val expiresAt = System.currentTimeMillis() + (expiresInHours * 60 * 60 * 1000)
            val expiresAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(expiresAt))

            val data = mutableMapOf(
                "code" to code,
                "role" to role,
                "expiresAt" to expiresAtStr,
                "isUsed" to false
            )
            // Agregar linea_id solo si es ADMIN_LINEA y se proporcionó una línea
            if (role == "ADMIN_LINEA" && lineaId.isNotEmpty()) {
                data["linea_id"] = lineaId
                android.util.Log.d("AdminRepository", "Agregando linea_id al código: $lineaId")
            } else {
                android.util.Log.w("AdminRepository", "NO se agregó linea_id: role=$role, lineaId=$lineaId")
            }

            val token = getAuthToken()
            android.util.Log.d("AdminRepository", "Datos a enviar: $data")
            
            val result: Result<String> = client.createRecord("invitation_codes", data, token)
            result.fold(
                onSuccess = { response ->
                    android.util.Log.d("AdminRepository", "Código creado exitosamente: $response")
                    val invitationCode = parseInvitationCode(response)
                    Result.success(invitationCode)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminRepository", "Error creando código: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminRepository", "Excepción creando código: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all invitation codes (con paginación automática)
     */
    suspend fun getInvitationCodes(): Result<List<InvitationCode>> = withContext(Dispatchers.IO) {
        try {
            val allCodes = mutableListOf<InvitationCode>()
            var page = 1
            val perPage = 50
            var hasMore = true
            val token = getAuthToken()

            while (hasMore) {
                val result: Result<String> = client.getList("invitation_codes", page = page, perPage = perPage, sort = "-created", authToken = token)
                result.fold(
                    onSuccess = { response ->
                        val codes = parseInvitationCodeList(response)
                        allCodes.addAll(codes)
                        hasMore = codes.size == perPage
                        page++
                    },
                    onFailure = { error ->
                        return@withContext Result.failure(error)
                    }
                )
            }
            Result.success(allCodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate an invitation code
     */
    suspend fun validateInvitationCode(code: String): Result<InvitationCode?> = withContext(Dispatchers.IO) {
        try {
            // Filtrar por code Y isUsed=false en una sola query
            val token = getAuthToken()
            android.util.Log.d("AdminRepository", "Validando código: $code")
            val result: Result<String> = client.getList(
                "invitation_codes",
                page = 1,
                perPage = 1,
                filter = "code=\"$code\" && isUsed=false",
                authToken = token
            )
            result.fold(
                onSuccess = { response ->
                    android.util.Log.d("AdminRepository", "Respuesta validación: $response")
                    val codes = parseInvitationCodeList(response)
                    if (codes.isNotEmpty()) {
                        val invitationCode = codes.first()
                        android.util.Log.d("AdminRepository", "Código encontrado: id=${invitationCode.id}, role=${invitationCode.role}, lineaId=${invitationCode.lineaId}")
                        if (invitationCode.isValid()) {
                            Result.success(invitationCode)
                        } else {
                            Result.failure(Exception("El código ha expirado"))
                        }
                    } else {
                        Result.failure(Exception("Código de invitación no válido o ya utilizado"))
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("AdminRepository", "Error validando código: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminRepository", "Excepción validando código: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an invitation code
     */
    suspend fun deleteInvitationCode(codeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<Unit> = client.deleteRecord("invitation_codes", codeId, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark an invitation code as used
     */
    suspend fun useInvitationCode(codeId: String, userEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val usedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())

            val data = mapOf(
                "isUsed" to true,
                "usedBy" to userEmail,  // Guardar el email del usuario
                "usedAt" to usedAt
            )

            val token = getAuthToken()
            val result: Result<String> = client.updateRecord("invitation_codes", codeId, data, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new line
     */
    suspend fun createLinea(
        name: String,
        code: String,
        rutaId: String = ""
    ): Result<Linea> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AdminRepository", "Creando línea: name=$name, code=$code, rutaId=$rutaId")
            val data = mapOf(
                "name" to name,
                "code" to code,
                "ruta_id" to rutaId
            )

            val token = getAuthToken()
            android.util.Log.d("AdminRepository", "Token disponible: ${token != null}")
            
            val result: Result<String> = client.createRecord("lineas", data, token)
            result.fold(
                onSuccess = { response ->
                    android.util.Log.d("AdminRepository", "Línea creada exitosamente: $response")
                    val linea = parseLinea(response)
                    Result.success(linea)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminRepository", "Error creando línea: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminRepository", "Excepción creando línea: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all lines (con paginación automática)
     */
    suspend fun getLineas(): Result<List<Linea>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AdminRepository", "Cargando líneas...")
            val allLineas = mutableListOf<Linea>()
            var page = 1
            val perPage = 50
            var hasMore = true

            val token = getAuthToken()
            android.util.Log.d("AdminRepository", "Token disponible para getLineas: ${token != null}")

            while (hasMore) {
                val result: Result<String> = client.getList("lineas", page = page, perPage = perPage, sort = "name", authToken = token)
                result.fold(
                    onSuccess = { response ->
                        android.util.Log.d("AdminRepository", "Respuesta recibida: ${response.take(200)}...")
                        val lineas = parseLineaList(response)
                        android.util.Log.d("AdminRepository", "Líneas parseadas: ${lineas.size}")
                        allLineas.addAll(lineas)
                        hasMore = lineas.size == perPage
                        page++
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminRepository", "Error cargando líneas: ${error.message}", error)
                        return@withContext Result.failure(error)
                    }
                )
            }
            android.util.Log.d("AdminRepository", "Total líneas cargadas: ${allLineas.size}")
            Result.success(allLineas)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepository", "Excepción cargando líneas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get lines by ruta ID
     */
    suspend fun getLineasByRuta(rutaId: String): Result<List<Linea>> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getList("lineas", page = 1, perPage = 100, filter = "ruta_id=\"$rutaId\"", authToken = token)
            result.fold(
                onSuccess = { response ->
                    val lineas = parseLineaList(response)
                    Result.success(lineas)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update a line
     */
    suspend fun updateLinea(
        lineaId: String,
        name: String,
        code: String,
        rutaId: String = ""
    ): Result<Linea> = withContext(Dispatchers.IO) {
        try {
            val data = mutableMapOf(
                "name" to name,
                "code" to code
            )
            if (rutaId.isNotEmpty()) {
                data["ruta_id"] = rutaId
            }

            val token = getAuthToken()
            val result: Result<String> = client.updateRecord("lineas", lineaId, data, token)
            result.fold(
                onSuccess = { response ->
                    val linea = parseLinea(response)
                    Result.success(linea)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /**
     * Delete a line
     */
    suspend fun deleteLinea(lineaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<Unit> = client.deleteRecord("lineas", lineaId, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateSecureCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }

    private fun parseInvitationCode(jsonString: String): InvitationCode {
        return try {
            json.decodeFromString<InvitationCode>(jsonString)
        } catch (e: Exception) {
            InvitationCode()
        }
    }

    private fun parseInvitationCodeList(jsonString: String): List<InvitationCode> {
        return try {
            val wrapper = json.decodeFromString<InvitationCodeListWrapper>(jsonString)
            wrapper.items
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLinea(jsonString: String): Linea {
        return try {
            json.decodeFromString<Linea>(jsonString)
        } catch (e: Exception) {
            Linea()
        }
    }

    private fun parseLineaList(jsonString: String): List<Linea> {
        return try {
            val wrapper = json.decodeFromString<LineaListWrapper>(jsonString)
            wrapper.items
        } catch (e: Exception) {
            emptyList()
        }
    }

    @kotlinx.serialization.Serializable
    data class InvitationCodeListWrapper(
        val page: Int = 1,
        val perPage: Int = 100,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<InvitationCode> = emptyList()
    )

    @kotlinx.serialization.Serializable
    data class LineaListWrapper(
        val page: Int = 1,
        val perPage: Int = 100,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<Linea> = emptyList()
    )

    // ============ Logs simplificados ============

    /**
     * Crear un registro de log simple
     * @param userId ID del usuario que realiza la acción
     * @param description Descripción de la acción realizada
     */
    suspend fun createLog(
        userId: String,
        description: String
    ): Result<LogEntry> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "userid" to userId,
                "description" to description
            )

            val token = getAuthToken()
            val result: Result<String> = client.createRecord("logs", data, token)
            result.fold(
                onSuccess = { response ->
                    val logEntry = parseLogEntry(response)
                    Result.success(logEntry)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener todos los logs (ordenados por fecha desc, con paginación)
     */
    suspend fun getLogs(limit: Int = 50): Result<List<LogEntry>> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getList(
                "logs",
                page = 1,
                perPage = limit,
                sort = "-created",
                authToken = token
            )
            result.fold(
                onSuccess = { response ->
                    val logs = parseLogEntryList(response)
                    Result.success(logs)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener logs por usuario
     */
    suspend fun getLogsByUser(userId: String): Result<List<LogEntry>> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getList(
                "logs",
                page = 1,
                perPage = 50,
                filter = "userid=\"$userId\"",
                sort = "-created",
                authToken = token
            )
            result.fold(
                onSuccess = { response ->
                    val logs = parseLogEntryList(response)
                    Result.success(logs)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseLogEntry(jsonString: String): LogEntry {
        return try {
            json.decodeFromString<LogEntry>(jsonString)
        } catch (e: Exception) {
            LogEntry()
        }
    }

    private fun parseLogEntryList(jsonString: String): List<LogEntry> {
        return try {
            val wrapper = json.decodeFromString<LogEntryListWrapper>(jsonString)
            wrapper.items
        } catch (e: Exception) {
            emptyList()
        }
    }

    @kotlinx.serialization.Serializable
    data class LogEntryListWrapper(
        val page: Int = 1,
        val perPage: Int = 100,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<LogEntry> = emptyList()
    )

    // ============ Rutas ============

    /**
     * Crear una nueva ruta para una línea
     */
    suspend fun createRuta(
        name: String,
        description: String,
        startPoint: String = "",
        endPoint: String = "",
        lineaId: String = ""
    ): Result<Ruta> = withContext(Dispatchers.IO) {
        try {
            val data = mutableMapOf(
                "name" to name,
                "description" to description
            )
            if (startPoint.isNotEmpty()) data["start_point"] = startPoint
            if (endPoint.isNotEmpty()) data["end_point"] = endPoint

            val token = getAuthToken()
            val result: Result<String> = client.createRecord("rutas", data, token)
            result.fold(
                onSuccess = { response ->
                    val ruta = parseRuta(response)
                    // Si hay lineaId, actualizar la línea con esta ruta
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty()) {
                        updateLineaRuta(lineaId, ruta.id)
                    }
                    Result.success(ruta)
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener ruta por ID
     */
    suspend fun getRuta(rutaId: String): Result<Ruta> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getRecord("rutas", rutaId, token)
            result.fold(
                onSuccess = { response -> Result.success(parseRuta(response)) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar la ruta asignada a una línea
     */
    private suspend fun updateLineaRuta(lineaId: String, rutaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf("ruta_id" to rutaId)
            val token = getAuthToken()
            val result: Result<String> = client.updateRecord("lineas", lineaId, data, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRuta(jsonString: String): Ruta {
        return try {
            json.decodeFromString<Ruta>(jsonString)
        } catch (e: Exception) {
            Ruta()
        }
    }

    // ============ Vehicle Invitation Codes ============

    /**
     * Generar código de invitación para vehículo
     */
    suspend fun generateVehicleInvitationCode(
        lineaId: String,
        creadoPor: String,
        expiresInHours: Int = 72
    ): Result<VehicleInvitationCode> = withContext(Dispatchers.IO) {
        try {
            val code = generateSecureCode()
            val expiresAt = System.currentTimeMillis() + (expiresInHours * 60 * 60 * 1000)
            val expiresAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(expiresAt))

            val data = mapOf(
                "code" to code,
                "linea_id" to lineaId,
                "expires_at" to expiresAtStr,
                "max_usos" to 1,
                "creado_por" to creadoPor,
                "activo" to true
            )

            val token = getAuthToken()
            val result: Result<String> = client.createRecord("vehicle_invitation_codes", data, token)
            result.fold(
                onSuccess = { response ->
                    val vehicleCode = parseVehicleInvitationCode(response)
                    Result.success(vehicleCode)
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener códigos de invitación de vehículo por línea
     */
    suspend fun getVehicleInvitationCodesByLinea(lineaId: String): Result<List<VehicleInvitationCode>> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getList(
                "vehicle_invitation_codes",
                page = 1,
                perPage = 50,
                filter = "linea_id=\"$lineaId\"",
                sort = "-created",
                authToken = token
            )
            result.fold(
                onSuccess = { response ->
                    val codes = parseVehicleInvitationCodeList(response)
                    Result.success(codes)
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validar código de invitación de vehículo
     */
    suspend fun validateVehicleInvitationCode(code: String): Result<VehicleInvitationCode?> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<String> = client.getList(
                "vehicle_invitation_codes",
                page = 1,
                perPage = 1,
                filter = "code=\"$code\" && activo=true",
                authToken = token
            )
            result.fold(
                onSuccess = { response ->
                    val codes = parseVehicleInvitationCodeList(response)
                    if (codes.isNotEmpty()) {
                        val vehicleCode = codes.first()
                        if (vehicleCode.isValid()) {
                            Result.success(vehicleCode)
                        } else {
                            Result.failure(Exception("El código ha expirado o ya fue usado"))
                        }
                    } else {
                        Result.failure(Exception("Código de invitación no válido"))
                    }
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar código de vehículo como usado
     */
    suspend fun useVehicleInvitationCode(codeId: String, usadoPor: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val usadoEn = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())

            val data = mapOf(
                "activo" to false,
                "usado_por" to usadoPor,
                "usado_en" to usadoEn
            )

            val token = getAuthToken()
            val result: Result<String> = client.updateRecord("vehicle_invitation_codes", codeId, data, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar código de invitación de vehículo
     */
    suspend fun deleteVehicleInvitationCode(codeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            val result: Result<Unit> = client.deleteRecord("vehicle_invitation_codes", codeId, token)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseVehicleInvitationCode(jsonString: String): VehicleInvitationCode {
        return try {
            json.decodeFromString<VehicleInvitationCode>(jsonString)
        } catch (e: Exception) {
            VehicleInvitationCode()
        }
    }

    private fun parseVehicleInvitationCodeList(jsonString: String): List<VehicleInvitationCode> {
        return try {
            val wrapper = json.decodeFromString<VehicleInvitationCodeListWrapper>(jsonString)
            wrapper.items
        } catch (e: Exception) {
            emptyList()
        }
    }

    @kotlinx.serialization.Serializable
    data class VehicleInvitationCodeListWrapper(
        val page: Int = 1,
        val perPage: Int = 100,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<VehicleInvitationCode> = emptyList()
    )
}
