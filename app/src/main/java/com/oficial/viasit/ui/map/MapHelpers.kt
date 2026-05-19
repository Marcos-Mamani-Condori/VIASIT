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
    val iconSizePx = 120 // Tamaño equilibrado para nitidez

    fun drawableToBitmap(drawableId: Int): android.graphics.Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = android.graphics.Bitmap.createBitmap(iconSizePx, iconSizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Usamos el dibujo original (auto verde con blanco) sin tintes que lo tapen
    drawableToBitmap(R.drawable.ic_car_icon)?.let { style.addImage("car-icon", it) }
    drawableToBitmap(R.drawable.ic_car_icon)?.let { style.addImage("car-icon-self", it) }
}

fun addCarsLayer(style: Style) {
    if (style.getSource("cars-source") == null) {
        style.addSource(GeoJsonSource("cars-source"))
    }
    
    if (style.getLayer("cars-layer") == null) {
        val symbolLayer = SymbolLayer("cars-layer", "cars-source").withProperties(
            PropertyFactory.iconImage(
                Expression.switchCase(
                    Expression.eq(Expression.get("isSelf"), Expression.literal(true)), 
                    Expression.literal("car-icon-self"),
                    Expression.literal("car-icon")
                )
            ),
            PropertyFactory.iconSize(0.6f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconRotationAlignment("map"),
            PropertyFactory.iconRotate(Expression.get("angulo")),
            PropertyFactory.iconPadding(0f)
        )
        style.addLayer(symbolLayer)
    }
}

fun updateCarsSource(style: Style, autos: List<Auto>, currentUserId: String? = null) {
    val source = style.getSource("cars-source") as? GeoJsonSource ?: return
    val features = autos
        .filter { it.lat != 0.0 || it.lng != 0.0 }
        .map { auto ->
            val properties = JsonObject().apply {
                addProperty("id", auto.id)
                addProperty("placa", auto.placa)
                addProperty("lineaId", auto.lineaId)
                addProperty("angulo", auto.angulo)
                addProperty("userId", auto.userId)
                addProperty("isSelf", auto.userId == currentUserId && !currentUserId.isNullOrBlank())
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

data class RoutePolyline(
    val id: String,
    val lineaName: String,
    val startPoint: String,
    val endPoint: String,
    val waypoints: String = "",
    val color: String = "#3D5AFE"
)

fun addRoutesLayer(style: Style) {
    if (style.getSource("routes-source") == null) {
        style.addSource(GeoJsonSource("routes-source"))
    }
    if (style.getLayer("routes-layer") == null) {
        val lineLayer = LineLayer("routes-layer", "routes-source").withProperties(
            PropertyFactory.lineColor(Expression.get("color")),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineOpacity(0.85f),
            PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
        )
        if (style.getLayer("cars-layer") != null) {
            style.addLayerBelow(lineLayer, "cars-layer")
        } else {
            style.addLayer(lineLayer)
        }
    }

    if (style.getSource("tails-source") == null) {
        style.addSource(GeoJsonSource("tails-source"))
    }
    if (style.getLayer("tails-layer") == null) {
        val tailsLayer = LineLayer("tails-layer", "tails-source").withProperties(
            PropertyFactory.lineColor("#3D5AFE"),
            PropertyFactory.lineWidth(2.5f),
            PropertyFactory.lineOpacity(0.4f),
            PropertyFactory.lineDasharray(arrayOf(2f, 1f)),
            PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND)
        )
        style.addLayerBelow(tailsLayer, "routes-layer")
    }
}

fun updateTailsSource(style: Style, tails: Map<String, List<Pair<Double, Double>>>) {
    val source = style.getSource("tails-source") as? GeoJsonSource ?: return
    val features = tails.mapNotNull { (_, points) ->
        if (points.size < 2) return@mapNotNull null
        val latLngs = points.map { Point.fromLngLat(it.second, it.first) }
        Feature.fromGeometry(LineString.fromLngLats(latLngs))
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

fun updateRoutesSource(style: Style, routes: List<RoutePolyline>) {
    val source = style.getSource("routes-source") as? GeoJsonSource ?: return
    val linePalette = listOf("#3D5AFE", "#06B6D4", "#10B981", "#FFB300", "#F43F5E")

    val features = routes.mapIndexedNotNull { index, route ->
        try {
            if (route.waypoints.contains("|")) {
                val points = route.waypoints.split("|").mapNotNull { p ->
                    val parts = p.split(",")
                    if (parts.size >= 2) {
                        val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                        val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                        Point.fromLngLat(lng, lat)
                    } else null
                }
                if (points.size < 2) return@mapIndexedNotNull null
                return@mapIndexedNotNull Feature.fromGeometry(LineString.fromLngLats(points), JsonObject().apply {
                    addProperty("color", route.color)
                })
            }
            val startParts = route.startPoint.split(",").map { it.trim().toDouble() }
            val endParts = route.endPoint.split(",").map { it.trim().toDouble() }
            val startPt = Point.fromLngLat(startParts[1], startParts[0])
            val endPt = Point.fromLngLat(endParts[1], endParts[0])
            val allPoints = listOf(startPt) + (if(route.waypoints.isNotBlank()) route.waypoints.split(";").map { p -> 
                val pts = p.split(","); Point.fromLngLat(pts[1].toDouble(), pts[0].toDouble()) 
            } else emptyList()) + listOf(endPt)
            
            Feature.fromGeometry(LineString.fromLngLats(allPoints), JsonObject().apply {
                addProperty("color", linePalette[index % linePalette.size])
            })
        } catch (e: Exception) { null }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

fun clearRoutesSource(style: Style) {
    val source = style.getSource("routes-source") as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}
