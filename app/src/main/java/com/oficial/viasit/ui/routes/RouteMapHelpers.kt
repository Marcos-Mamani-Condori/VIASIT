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

fun updateMarker(style: Style, sourceId: String, latLng: LatLng) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    source.setGeoJson(
        FeatureCollection.fromFeatures(
            listOf(Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude)))
        )
    )
}

fun clearMarker(style: Style, sourceId: String) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}

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
