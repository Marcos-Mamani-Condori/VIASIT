package com.oficial.viasit.data

import android.os.Build
import android.util.Log
import com.oficial.viasit.model.Auto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface PocketBaseService {
    @GET("api/collections/autos/records")
    suspend fun getAutos(): PocketBaseResponse
}

data class PocketBaseResponse(val items: List<Auto>)

object RetrofitClient {
    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.contains("generic") || 
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK")
    
    private val BASE_URL: String
        get() {
            val url = if (isEmulator) {
                "http://10.0.2.2:8090/"
            } else {
                "http://192.168.1.14:8090/"
            }
            return url
        }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: PocketBaseService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PocketBaseService::class.java)
    }
}
