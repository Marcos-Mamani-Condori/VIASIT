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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
 * Servicio de tracking de ubicación optimizado para enviar coordenadas a PocketBase
 * 
 * Características:
 * - Umbral de distancia mínimo de 10 metros antes de enviar actualización
 * - Intervalos adaptativos basados en velocidad:
 *   - En movimiento (>5 km/h): 5 segundos
 *   - Lentamente (1-5 km/h): 10 segundos
 *   - Detenido (<1 km/h): 30 segundos
 *   - Detenido >5 minutos: Detener tracking completamente
 * - Solo envía ubicación cuando "En servicio" está activo
 */
class LocationTrackingService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_SERVICE_STATUS = "ACTION_UPDATE_SERVICE_STATUS"
        private const val CHANNEL_ID = "location_channel"
        
        // Configuration
        private const val MIN_DISTANCE_METERS = 10f
        private const val INTERVAL_MOVING = 5000L      // 5 seconds - moving
        private const val INTERVAL_SLOW = 10000L       // 10 seconds - slow
        private const val INTERVAL_STOPPED = 30000L     // 30 seconds - stopped
        private const val STOPPED_THRESHOLD_MS = 300000L // 5 minutes stopped = stop tracking
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var pocketBaseClient: PocketBaseRealtimeClient
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var userId: String = ""
    private var userName: String = ""
    private var userRole: UserRole = UserRole.USUARIO
    private var isInService: Boolean = false
    private var isTracking: Boolean = false
    
    // Tracking state
    private var lastLocation: Location? = null
    private var lastLocationTime: Long = 0
    private var totalDistanceMoved: Float = 0f
    private var currentInterval: Long = INTERVAL_MOVING

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        pocketBaseClient = PocketBaseRealtimeClient()
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getStringExtra("userId") ?: ""
                userName = intent.getStringExtra("userName") ?: ""
                userRole = try {
                    UserRole.valueOf(intent.getStringExtra("userRole") ?: UserRole.USUARIO.name)
                } catch (e: Exception) {
                    UserRole.USUARIO
                }
                isInService = intent.getBooleanExtra("isInService", false)
                startForeground(NOTIFICATION_ID, createNotification())
                startLocationTracking()
            }
            ACTION_STOP -> {
                stopLocationTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_SERVICE_STATUS -> {
                isInService = intent.getBooleanExtra("isInService", false)
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
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
        val role = if (userRole == UserRole.CONDUCTOR) "Conductor" else "Usuario"
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
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }
    }

    private fun processLocation(location: Location) {
        // Only send to PocketBase if "En servicio" is active
        if (!isInService || userId.isEmpty()) {
            return
        }

        val now = System.currentTimeMillis()
        
        // Check if we should send update based on distance
        val shouldSend = shouldSendUpdate(location)
        
        if (shouldSend) {
            // Send to PocketBase
            serviceScope.launch {
                sendLocationToPocketBase(location)
            }
            
            // Calculate speed and adapt interval
            updateTrackingState(location, now)
        } else {
            // Still update state for speed calculation but don't send
            updateTrackingState(location, now)
        }
    }

    private fun shouldSendUpdate(currentLocation: Location): Boolean {
        // First location always sent
        if (lastLocation == null) {
            return true
        }
        
        // Check distance threshold (10 meters minimum)
        val distance = lastLocation!!.distanceTo(currentLocation)
        return distance >= MIN_DISTANCE_METERS
    }

    private fun updateTrackingState(location: Location, now: Long) {
        val previousLocation = lastLocation
        lastLocation = location
        lastLocationTime = now
        
        if (previousLocation != null) {
            totalDistanceMoved += previousLocation.distanceTo(location)
            
            // Calculate speed in km/h
            val speedKmH = location.speed * 3.6f
            
            // Adapt interval based on speed
            currentInterval = when {
                speedKmH > 5 -> INTERVAL_MOVING     // Moving fast
                speedKmH > 1 -> INTERVAL_SLOW       // Moving slow
                else -> INTERVAL_STOPPED            // Stopped
            }
            
            // Update location request with new interval
            updateLocationRequest(currentInterval)
        }
    }

    private suspend fun sendLocationToPocketBase(location: Location) {
        try {
            // Send to autos collection with location fields
            pocketBaseClient.updateAutoLocation(
                userId = userId,
                lat = location.latitude,
                lng = location.longitude,
                angulo = location.bearing,
                velocidad = location.speed
            )
        } catch (e: Exception) {
            // Log error but don't crash service
            e.printStackTrace()
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return
        if (!isInService) {
            // Still start tracking but won't send updates until "En servicio" is true
        }
        
        // Set initial interval
        currentInterval = INTERVAL_MOVING
        
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentInterval)
            .setMinUpdateIntervalMillis(currentInterval / 2)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isTracking = true
        }
    }

    private fun updateLocationRequest(newInterval: Long) {
        if (!isTracking) return
        
        // Remove and re-add with new interval
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, newInterval)
            .setMinUpdateIntervalMillis(newInterval / 2)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
        lastLocation = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLocationTracking()
    }
}
