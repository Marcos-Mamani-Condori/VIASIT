# VIASIT Authentication and Location Tracking System

## Overview

This document describes the complete authentication and real-time location tracking system implemented for the VIASIT Android application.

## Features

### Authentication System
- **Login**: Users can log in with email and password
- **Registration**: New users can register with email, password, name, and optional phone
- **Guest Mode**: Users can enter as guests without authentication
- **Session Persistence**: Login state is preserved across app restarts

### User Roles
1. **GUEST**: Can view all vehicle locations on the map
2. **MINIUSER**: Can view locations and send their own location to the server
3. **DRIVER**: Can view locations, send location, and toggle "in service" status

### Location Tracking
- **Real-time Updates**: Location is sent to the server at recommended intervals
- **Foreground Service**: Reliable tracking even when app is in background
- **Battery Optimization**: Configurable update intervals based on user role
- **Driver Mode**: Drivers have a toggle to show/hide their location ("in service")

## Architecture

### Domain Layer
- `User.kt`: User model with roles (UserRole enum)
- `UserLocation.kt`: Location data model
- `AuthState.kt`: Authentication state management

### Data Layer
- `AuthApiClient.kt`: PocketBase authentication API
- `UserLocationsApiClient.kt`: Location updates API
- `AuthRepository.kt`: Authentication data management
- `UserLocationRepository.kt`: Location data management
- `AppDatabase.kt`: Room database with UserLocation entity

### UI Layer
- `AuthScreen.kt`: Login/Register/Guest login screen
- `AuthViewModel.kt`: Authentication state management
- `MainActivity.kt`: Main map screen with user features

### Service Layer
- `LocationTrackingService.kt`: Foreground service for location tracking

## Implementation Details

### PocketBase Collections Required

#### users Collection
```json
{
    "name": "users",
    "type": "base",
    "schema": [
        {"name": "email", "type": "email", "required": true},
        {"name": "name", "type": "text", "required": true},
        {"name": "phone", "type": "text"},
        {"name": "role", "type": "select", "options": ["GUEST", "MINIUSER", "DRIVER"]},
        {"name": "isInService", "type": "bool"},
        {"name": "isActive", "type": "bool"}
    ]
}
```

#### user_locations Collection
```json
{
    "name": "user_locations",
    "type": "base",
    "schema": [
        {"name": "userId", "type": "relation", "required": true},
        {"name": "userName", "type": "text", "required": true},
        {"name": "lat", "type": "number", "required": true},
        {"name": "lng", "type": "number", "required": true},
        {"name": "accuracy", "type": "number"},
        {"name": "altitude", "type": "number"},
        {"name": "speed", "type": "number"},
        {"name": "bearing", "type": "number"},
        {"name": "timestamp", "type": "number"},
        {"name": "isInService", "type": "bool"},
        {"name": "userRole", "type": "text"}
    ]
}
```

### Update Intervals
- **Drivers**: 5 seconds (recommended for real-time tracking)
- **Miniusers**: 10 seconds
- **Guests**: No location sending (view only)

## API Endpoints

### Authentication
- `POST /api/collections/users/auth-with-password` - Login
- `POST /api/collections/users/records` - Register
- `POST /api/collections/users/auth-refresh` - Refresh token
- `PATCH /api/collections/users/records/{id}` - Update user

### Location
- `GET /api/collections/user_locations/records` - Get active locations
- `POST /api/collections/user_locations/records` - Send location
- `PATCH /api/collections/user_locations/records/{id}` - Update location
- `DELETE /api/collections/user_locations/records/{id}` - Delete location

## Usage

### Starting Location Tracking (for Drivers)
```kotlin
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_START
    putExtra("userId", userId)
    putExtra("userName", userName)
    putExtra("userRole", userRole.name)
    putExtra("isInService", isInService)
}
ContextCompat.startForegroundService(context, intent)
```

### Toggling "In Service" Mode
```kotlin
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_UPDATE_INTERVAL
    putExtra("intervalMs", newInterval)
}
context.startService(intent)

// Or update service directly
locationService?.setInService(true)
```

### Stopping Tracking
```kotlin
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_STOP
}
context.startService(intent)
```

## Security Considerations
- Authentication tokens are stored in SharedPreferences
- Location tracking requires user consent via permissions
- Only authenticated users can send locations
- Guests can only view, not send locations

## Dependencies Added
- `okhttp` - HTTP client for API calls
- `kotlinx.serialization` - JSON serialization
- Room for local caching
- Google Play Services Location for GPS

## Next Steps
1. Add Firebase Cloud Messaging for push notifications
2. Implement WebSocket for real-time location updates
3. Add location history tracking
4. Implement geofencing for stops
