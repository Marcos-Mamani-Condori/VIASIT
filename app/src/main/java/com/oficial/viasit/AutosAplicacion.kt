package com.oficial.viasit

import android.app.Application
import android.util.Log
import com.oficial.viasit.data.local.AppDatabase
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminRepository
import com.oficial.viasit.data.repository.AutoRepository
import com.oficial.viasit.data.repository.AuthRepository

class AutosAplicacion : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }

    // Cliente compartido: una sola instancia de OkHttpClient + SSE para toda la app
    val pocketBaseClient by lazy {
        PocketBaseRealtimeClient(database.autoData())
    }

    val repository: AutoRepository by lazy {
        AutoRepository(
            pocketBaseClient = pocketBaseClient,
            dao = database.autoData()
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(applicationContext)
    }

    // AdminRepository comparte el mismo cliente y usa el token de la sesión activa
    val adminRepository: AdminRepository by lazy {
        AdminRepository(pocketBaseClient, authRepository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("AutosAplicacion", "Inicializado")
    }

    override fun onTerminate() {
        super.onTerminate()
        pocketBaseClient.release()
        Log.d("AutosAplicacion", "Recursos liberados")
    }

    companion object {
        lateinit var instance: AutosAplicacion
            private set
    }
}
