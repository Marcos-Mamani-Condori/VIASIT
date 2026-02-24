package com.oficial.viasit.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.oficial.viasit.R
import com.oficial.viasit.domain.model.Auto
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.google.gson.JsonObject

/**
 * Funciones auxiliares estáticas del mapa MapLibre.
 * Extraídas de MainActivity.kt para mejorar la organización del código.
 */

/** Carga el ícono del auto en el estilo del mapa */
fun addCarIconToStyle(style: Style, context: android.content.Context) {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_car_icon)
    if (drawable != null) {
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        style.addImage("car-icon", bitmap)
    }
}

/** Agrega el source GeoJSON y la capa de símbolos de autos al estilo */
fun addCarsLayer(style: Style) {
    if (style.getSource("cars-source") == null) {
        style.addSource(GeoJsonSource("cars-source"))
    }
    if (style.getLayer("cars-layer") == null) {
        val symbolLayer = SymbolLayer("cars-layer", "cars-source").withProperties(
            PropertyFactory.iconImage("car-icon"),
            PropertyFactory.iconSize(0.5f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor("center"),
            PropertyFactory.iconRotate(Expression.get("angulo"))
        )
        style.addLayer(symbolLayer)
    }
}

/** Actualiza las posiciones de todos los autos en el source GeoJSON */
fun updateCarsSource(style: Style, autos: List<Auto>) {
    val source = style.getSource("cars-source") as? GeoJsonSource ?: return
    // Filtrar coordenadas inválidas (0,0) que corresponden al océano Atlántico
    val features = autos
        .filter { it.lat != 0.0 || it.lng != 0.0 }
        .map { auto ->
            val properties = JsonObject().apply {
                addProperty("placa", auto.placa)
                addProperty("angulo", auto.angulo)
            }
            Feature.fromGeometry(Point.fromLngLat(auto.lng, auto.lat), properties)
        }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** Anima la cámara hacia la ubicación actual del usuario */
fun centerOnUser(map: MapLibreMap, context: android.content.Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        map.locationComponent.lastKnownLocation?.let { location ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 16.0
                )
            )
        }
    }
}

/** Activa el componente de ubicación en modo brújula */
fun enableLocationComponent(style: Style, map: MapLibreMap, context: android.content.Context) {
    try {
        val locationComponent = map.locationComponent
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.renderMode = RenderMode.COMPASS
            locationComponent.cameraMode  = CameraMode.TRACKING
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
