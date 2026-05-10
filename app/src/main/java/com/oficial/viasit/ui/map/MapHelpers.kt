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
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import com.google.gson.JsonObject

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

// ─── Rutas / Polylines ────────────────────────────────────────────────────────

data class RoutePolyline(
    val id: String,
    val lineaName: String,
    val startPoint: String,    // "lat,lng"
    val endPoint: String,      // "lat,lng"
    val waypoints: String = "", // "lat,lng;lat,lng;..." — vacío = línea recta
    val color: String = "#3D5AFE"
)

fun addRoutesLayer(style: Style) {
    if (style.getSource("routes-source") == null) {
        style.addSource(GeoJsonSource("routes-source"))
    }
    if (style.getLayer("routes-layer") == null) {
        // Agregar la capa DEBAJO de la capa de autos para que los autos queden encima
        val lineLayer = LineLayer("routes-layer", "routes-source").withProperties(
            PropertyFactory.lineColor(Expression.get("color")),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineOpacity(0.85f),
            PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
        )
        // Insertar la capa de rutas por debajo de la de autos
        if (style.getLayer("cars-layer") != null) {
            style.addLayerBelow(lineLayer, "cars-layer")
        } else {
            style.addLayer(lineLayer)
        }
    }
}

fun updateRoutesSource(style: Style, routes: List<RoutePolyline>) {
    val source = style.getSource("routes-source") as? GeoJsonSource ?: return

    val linePalette = listOf(
        "#3D5AFE", "#06B6D4", "#10B981", "#FFB300", "#F43F5E",
        "#8B5CF6", "#EC4899", "#14B8A6", "#F97316", "#84CC16"
    )

    val features = routes.mapIndexedNotNull { index, route ->
        try {
            val startParts = route.startPoint.split(",").map { it.trim().toDouble() }
            val endParts   = route.endPoint.split(",").map { it.trim().toDouble() }
            if (startParts.size < 2 || endParts.size < 2) return@mapIndexedNotNull null

            val startPt = Point.fromLngLat(startParts[1], startParts[0]) // lng, lat
            val endPt   = Point.fromLngLat(endParts[1],   endParts[0])

            // Si la ruta tiene waypoints intermedios los usa; si no, línea recta inicio→fin
            val allPoints: List<Point> = if (route.waypoints.isNotBlank()) {
                val midPoints = route.waypoints.split(";").mapNotNull { pair ->
                    val parts = pair.trim().split(",")
                    if (parts.size >= 2) {
                        val lat = parts[0].trim().toDoubleOrNull() ?: return@mapNotNull null
                        val lng = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                        Point.fromLngLat(lng, lat)
                    } else null
                }
                listOf(startPt) + midPoints + listOf(endPt)
            } else {
                listOf(startPt, endPt)
            }

            val lineString = LineString.fromLngLats(allPoints)
            val color = linePalette[index % linePalette.size]

            val props = com.google.gson.JsonObject().apply {
                addProperty("color", color)
                addProperty("linea", route.lineaName)
            }
            Feature.fromGeometry(lineString, props)
        } catch (e: Exception) {
            null  // Ignorar rutas con formato incorrecto
        }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

fun clearRoutesSource(style: Style) {
    val source = style.getSource("routes-source") as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}

