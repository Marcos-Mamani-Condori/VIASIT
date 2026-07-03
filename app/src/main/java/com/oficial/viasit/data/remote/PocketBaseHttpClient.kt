package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PocketBaseHttpClient(private val okHttpClient: OkHttpClient) {

    companion object {
        private const val TAG = "PocketBaseHttp"
        private val BASE_URL: String get() = BuildConfig.POCKETBASE_URL

        fun create(): PocketBaseHttpClient = PocketBaseHttpClient(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        )
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun handleError(code: Int, body: String?): Result<Nothing> {
        android.util.Log.e("LLAMADOS", ">>> ERROR SERVIDOR ($code): $body")
        if (body.isNullOrBlank()) return Result.failure(Exception("Error $code: No body"))
        return try {
            val errorJson = json.parseToJsonElement(body)
            val message = errorJson.asJsonObject()["message"]?.toString()?.removeSurrounding("\"")
                ?: "Error $code"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception("Error $code: $body"))
        }
    }

    private fun kotlinx.serialization.json.JsonElement.asJsonObject() = this as? kotlinx.serialization.json.JsonObject ?: kotlinx.serialization.json.JsonObject(emptyMap())

    suspend fun createRecord(collection: String, data: Map<String, Any>, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = buildJsonString(data).toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records")
                    .post(body)
                    .applyAuth(authToken)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                val resBody = res.body?.string()
                if (res.isSuccessful) Result.success(resBody ?: "{}")
                else handleError(res.code, resBody)
            } catch (e: Exception) { Log.e(TAG, "createRecord", e); Result.failure(e) }
        }
    }

    suspend fun updateRecord(collection: String, recordId: String, data: Map<String, Any>, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = buildJsonString(data).toRequestBody("application/json".toMediaType())
                val url = "$BASE_URL/api/collections/$collection/records/$recordId"
                Log.d(TAG, "PATCH Request to: $url with data: $data")
                val req = Request.Builder()
                    .url(url)
                    .patch(body)
                    .applyAuth(authToken)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                val resBody = res.body?.string()
                Log.d(TAG, "PATCH Response code: ${res.code}, body: $resBody")
                if (res.isSuccessful) Result.success(resBody ?: "{}")
                else handleError(res.code, resBody)
            } catch (e: Exception) { Log.e(TAG, "updateRecord", e); Result.failure(e) }
        }
    }

    suspend fun deleteRecord(collection: String, recordId: String, authToken: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records/$recordId")
                    .delete()
                    .applyAuth(authToken)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                if (res.isSuccessful) Result.success(Unit)
                else handleError(res.code, res.body?.string())
            } catch (e: Exception) { Log.e(TAG, "deleteRecord", e); Result.failure(e) }
        }
    }

    suspend fun getRecord(collection: String, recordId: String, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records/$recordId")
                    .get()
                    .applyAuth(authToken)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                val resBody = res.body?.string()
                if (res.isSuccessful) Result.success(resBody ?: "{}")
                else handleError(res.code, resBody)
            } catch (e: Exception) { Log.e(TAG, "getRecord", e); Result.failure(e) }
        }
    }

    suspend fun getList(
        collection: String,
        page: Int = 1,
        perPage: Int = 30,
        filter: String = "",
        sort: String = "",
        expand: String = "",
        authToken: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var url = "$BASE_URL/api/collections/$collection/records?page=$page&perPage=$perPage"
                if (filter.isNotEmpty()) url += "&filter=${java.net.URLEncoder.encode(filter, "UTF-8")}"
                if (sort.isNotEmpty())   url += "&sort=${java.net.URLEncoder.encode(sort, "UTF-8")}"
                if (expand.isNotEmpty()) url += "&expand=${java.net.URLEncoder.encode(expand, "UTF-8")}"
                
                android.util.Log.e("LLAMADOS", ">>> [GET_LIST] URL: $url")
                
                val req = Request.Builder().url(url).get().applyAuth(authToken).build()
                val res = okHttpClient.newCall(req).execute()
                val resBody = res.body?.string()
                
                android.util.Log.e("LLAMADOS", ">>> [GET_LIST] RESP CODE: ${res.code}")
                
                if (res.isSuccessful) Result.success(resBody ?: "{}")
                else handleError(res.code, resBody)
            } catch (e: Exception) { android.util.Log.e("LLAMADOS", ">>> [GET_LIST] ERROR: ${e.message}"); Result.failure(e) }
        }
    }

    fun buildJsonString(data: Map<String, Any>): String {
        val sb = StringBuilder("{")
        data.entries.forEachIndexed { i, (key, value) ->
            if (i > 0) sb.append(",")
            sb.append(json.encodeToString(key)).append(":")
            when (value) {
                is String  -> sb.append(json.encodeToString(value))
                is Boolean -> sb.append(value)
                is Number  -> sb.append(value)
                is List<*> -> {
                    sb.append("[")
                    value.forEachIndexed { j, item ->
                        if (j > 0) sb.append(",")
                        sb.append(json.encodeToString(item?.toString() ?: ""))
                    }
                    sb.append("]")
                }
                else -> sb.append(json.encodeToString(value.toString()))
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun Request.Builder.applyAuth(token: String?): Request.Builder =
        if (token != null) header("Authorization", "Bearer $token") else this
}
