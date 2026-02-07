package com.oficial.viasit.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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
import com.oficial.viasit.domain.model.UserRole

class LocationTrackingService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "location_channel"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var userId: String = ""
    private var userName: String = ""
    private var userRole: UserRole = UserRole.USUARIO
    private var isInService: Boolean = false
    private var isTracking: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        
        val status = if (isInService) "En servicio" else "Ubicación activa"
        val role = if (userRole == UserRole.CONDUCTOR) "Conductor" else "Usuario"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VIASIT - $status")
            .setContentText("$role: $userName")
            .setSmallIcon(R.drawable.ic_car_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Location processed - can be sent to server here
                }
            }
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return
        
        val interval = when (userRole) {
            UserRole.CONDUCTOR -> 5000L
            else -> 10000L
        }
        
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isTracking = true
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
    }
}
