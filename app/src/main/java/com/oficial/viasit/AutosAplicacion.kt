package com.oficial.viasit

import android.app.Application
import android.util.Log
import com.oficial.viasit.data.AppDatabase
import com.oficial.viasit.data.AutoRepository
import com.oficial.viasit.data.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutosAplicacion : Application() {

    // Un CoroutineScope para toda la aplicación
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    private val database by lazy { AppDatabase.getDatabase(this) }

    val repository: AutoRepository by lazy {
        AutoRepository(
            api = RetrofitClient.api,
            dao = database.autoData()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Lanzamos la inicialización en un hilo secundario
        applicationScope.launch {
            repository
        }
    }

    companion object {
        lateinit var instance: AutosAplicacion
            private set
    }
}
