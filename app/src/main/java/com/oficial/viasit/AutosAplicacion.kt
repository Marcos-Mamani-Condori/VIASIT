package com.oficial.viasit

import android.app.Application
import android.util.Log
import com.oficial.viasit.data.AppDatabase
import com.oficial.viasit.data.AutoRepository
import com.oficial.viasit.data.PocketBaseRealtimeClient

class AutosAplicacion : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }

    // Cliente PocketBase con SDK oficial y Realtime
    private val pocketBaseClient by lazy { 
        PocketBaseRealtimeClient(database.autoData()) 
    }

    val repository: AutoRepository by lazy {
        AutoRepository(
            pocketBaseClient = pocketBaseClient,
            dao = database.autoData()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("AutosAplicacion", "Inicializado con PocketBase Kotlin SDK + Realtime")
    }

    override fun onTerminate() {
        super.onTerminate()
        // Liberar recursos del cliente PocketBase
        pocketBaseClient.release()
        Log.d("AutosAplicacion", "Recursos liberados")
    }

    companion object {
        lateinit var instance: AutosAplicacion
            private set
    }
}
