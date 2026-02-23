package com.oficial.viasit.ui.routes

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Funciones auxiliares del mapa para la pantalla de creación de rutas.
 *
 * Separadas de RouteMapPickerScreen para que la pantalla solo
 * contenga lógica de UI y estado, no funciones de bajo nivel de MapLibre.
 *
 * Flujo de uso:
 *  1. Al cargar el estilo: [addMarkerIconsToStyle] → [addMarkerLayer] (x2)
 *  2. Al hacer clic en el mapa: [updateMarker] con el sourceId correspondiente
 *  3. Al resetear: [clearMarker] en ambos sources
 *  4. Botón de localización: [centerOnUser]
 */

/** Dibuja los iconos de inicio (azul) y fin (verde) en el estilo del mapa */
fun addMarkerIconsToStyle(mapStyle: Style, context: android.content.Context) {
    // Marcador INICIO — círculo azul con punto blanco en el centro
    run {
        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint  = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#3B82F6") // Brand500
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(24f, 24f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(24f, 24f, 8f, paint)
        mapStyle.addImage("start-marker-icon", bitmap)
    }

    // Marcador FIN — círculo verde con punto blanco en el centro
    run {
        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint  = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#10B981") // Emerald500
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(24f, 24f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(24f, 24f, 8f, paint)
        mapStyle.addImage("end-marker-icon", bitmap)
    }
}

/** Registra un source GeoJSON vacío + una capa de símbolo para un marcador */
fun addMarkerLayer(style: Style, sourceId: String, layerId: String, iconImage: String) {
    if (style.getSource(sourceId) == null) {
        style.addSource(GeoJsonSource(sourceId))
    }
    if (style.getLayer(layerId) == null) {
        val symbolLayer = SymbolLayer(layerId, sourceId).withProperties(
            PropertyFactory.iconImage(iconImage),
            PropertyFactory.iconSize(1.0f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconAnchor("center")
        )
        style.addLayer(symbolLayer)
    }
}

/** Mueve el marcador a una nueva posición en el mapa */
fun updateMarker(style: Style, sourceId: String, latLng: LatLng) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    source.setGeoJson(
        FeatureCollection.fromFeatures(
            listOf(Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude)))
        )
    )
}

/** Elimina el marcador del mapa (deja el source vacío) */
fun clearMarker(style: Style, sourceId: String) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}

/** Centra la cámara en la ubicación GPS actual del usuario */
fun centerOnUserRoute(map: MapLibreMap, context: android.content.Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        map.locationComponent.lastKnownLocation?.let { location ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15.0)
            )
        }
    }
}

/** Activa el componente de ubicación en modo libre (sin seguimiento automático) */
fun enableRouteLocationComponent(style: Style, map: MapLibreMap, context: android.content.Context) {
    try {
        val locationComponent = map.locationComponent
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.renderMode = RenderMode.COMPASS
            locationComponent.cameraMode  = CameraMode.NONE  // no sigue automáticamente
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
