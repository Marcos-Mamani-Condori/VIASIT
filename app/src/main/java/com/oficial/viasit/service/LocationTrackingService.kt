package com.oficial.viasit.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.MainActivity
import com.oficial.viasit.R
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio de tracking de ubicación optimizado para enviar coordenadas a PocketBase.
 *
 * FIXES aplicados:
 * - Usa el cliente PocketBase compartido de AutosAplicacion (Fix 4) en lugar de crear
 *   una tercera instancia de OkHttpClient.
 * - calculateBearing() ahora usa FloatArray(2) donde result[1] es el bearing real (Fix 3).
 * - updateLocationRequest() solo re-registra si el intervalo cambió (Fix 9).
 *
 * Características:
 * - Umbral de distancia mínimo de 10 metros antes de enviar actualización
 * - Intervalos adaptativos basados en velocidad:
 *   - En movimiento (>5 km/h): 5 segundos
 *   - Lentamente (1-5 km/h): 10 segundos
 *   - Detenido (<1 km/h): 30 segundos
 */
class LocationTrackingService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_SERVICE_STATUS = "ACTION_UPDATE_SERVICE_STATUS"
        private const val CHANNEL_ID = "location_channel"
        private const val TAG = "LocationTrackingService"

        // Configuration
        private const val MIN_DISTANCE_METERS = 10f
        private const val INTERVAL_MOVING  = 5000L      // 5 seconds - moving fast
        private const val INTERVAL_SLOW    = 10000L     // 10 seconds - moving slow
        private const val INTERVAL_STOPPED = 30000L     // 30 seconds - stopped
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    // Usa el cliente compartido en vez de crear una instancia nueva (evita 3er OkHttpClient)
    private lateinit var pocketBaseClient: PocketBaseRealtimeClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var userId: String = ""
    private var autoId: String = ""
    private var userName: String = ""
    private var userRole: UserRole = UserRole.usuario
    private var isInService: Boolean = false
    private var isTracking: Boolean = false

    // Tracking state
    private var lastLocation: Location? = null
    private var lastLocationTime: Long = 0
    private var currentInterval: Long = INTERVAL_MOVING
    // Fix 9: solo llama updateLocationRequest cuando el intervalo realmente cambia
    private var lastRequestedInterval: Long = -1L

    private var previousLocationForBearing: Location? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Reutiliza el cliente compartido; no crea OkHttpClient extra
        pocketBaseClient = AutosAplicacion.instance.pocketBaseClient
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getStringExtra("userId") ?: ""
                autoId = intent.getStringExtra("autoId") ?: ""
                userName = intent.getStringExtra("userName") ?: ""
                userRole = try {
                    UserRole.valueOf(intent.getStringExtra("userRole") ?: UserRole.usuario.name)
                } catch (e: Exception) {
                    UserRole.usuario
                }
                isInService = intent.getBooleanExtra("isInService", false)
                Log.d(TAG, "ACTION_START - autoId: $autoId, userId: $userId, isInService: $isInService")
                startForeground(NOTIFICATION_ID, createNotification())
                startLocationTracking()
            }
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP")
                stopLocationTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_SERVICE_STATUS -> {
                isInService = intent.getBooleanExtra("isInService", false)
                Log.d(TAG, "ACTION_UPDATE_SERVICE_STATUS - isInService: $isInService")
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val status = if (isInService) "En servicio" else "Fuera de servicio"
        val role = if (userRole == UserRole.conductor) "Conductor" else "Usuario"
        val text = if (isInService) "$role: $userName - Enviando ubicación" else "$role: $userName"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VIASIT - $status")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_car_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification())
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { processLocation(it) }
            }
        }
    }

    private fun processLocation(location: Location) {
        if (!isInService || userId.isEmpty()) return

        val now = System.currentTimeMillis()
        val shouldSend = shouldSendUpdate(location)

        if (shouldSend) {
            serviceScope.launch { sendLocationToPocketBase(location) }
        }
        updateTrackingState(location, now)
    }

    private fun shouldSendUpdate(currentLocation: Location): Boolean {
        if (lastLocation == null) return true
        return lastLocation!!.distanceTo(currentLocation) >= MIN_DISTANCE_METERS
    }

    private fun updateTrackingState(location: Location, now: Long) {
        val previousLocation = lastLocation
        lastLocation = location
        lastLocationTime = now

        if (previousLocation != null) {
            val speedKmH = location.speed * 3.6f
            val newInterval = when {
                speedKmH > 5 -> INTERVAL_MOVING
                speedKmH > 1 -> INTERVAL_SLOW
                else         -> INTERVAL_STOPPED
            }
            // Fix 9: solo actualiza si el intervalo realmente cambió (evita remove+add innecesario)
            if (newInterval != lastRequestedInterval) {
                currentInterval = newInterval
                updateLocationRequest(newInterval)
            }
        }
    }

    private suspend fun sendLocationToPocketBase(location: Location) {
        try {
            if (autoId.isEmpty()) {
                Log.w(TAG, "No hay autoId válido, no se puede enviar ubicación")
                return
            }
            val bearing = calculateBearing(location)
            pocketBaseClient.updateAutoLocation(
                autoId = autoId,
                lat = location.latitude,
                lng = location.longitude,
                angulo = bearing
            )
            Log.d(TAG, "Ubicación enviada: lat=${location.latitude}, lng=${location.longitude}, bearing=$bearing")
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando ubicación", e)
        }
    }

    /**
     * Calcula el bearing (ángulo de movimiento).
     *
     * FIX: Location.distanceBetween con array de 2 elementos:
     *   result[0] = distancia en metros
     *   result[1] = bearing inicial (el que necesitamos)
     * Antes se usaba FloatArray(1) que solo da la distancia, nunca el bearing.
     */
    private fun calculateBearing(location: Location): Double {
        // Primero usar el bearing del GPS si es válido
        if (location.hasBearing() && location.bearing > 0f) {
            previousLocationForBearing = location
            return location.bearing.toDouble()
        }

        // Fallback: calcular desde la última posición conocida
        previousLocationForBearing?.let { prev ->
            val result = FloatArray(2) // [0]=distancia, [1]=bearing inicial
            Location.distanceBetween(
                prev.latitude, prev.longitude,
                location.latitude, location.longitude,
                result
            )
            previousLocationForBearing = location
            return result[1].toDouble() // result[1] es el bearing, NO result[0]
        }

        return 0.0
    }

    private fun startLocationTracking() {
        if (isTracking) return
        currentInterval = INTERVAL_MOVING
        lastRequestedInterval = INTERVAL_MOVING

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentInterval)
            .setMinUpdateIntervalMillis(currentInterval / 2)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isTracking = true
            Log.d(TAG, "Location tracking iniciado")
        }
    }

    private fun updateLocationRequest(newInterval: Long) {
        if (!isTracking) return
        lastRequestedInterval = newInterval

        fusedLocationClient.removeLocationUpdates(locationCallback)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, newInterval)
            .setMinUpdateIntervalMillis(newInterval / 2)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "Intervalo de tracking actualizado: ${newInterval}ms")
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
        lastLocation = null
        lastRequestedInterval = -1L
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLocationTracking()
    }
}
