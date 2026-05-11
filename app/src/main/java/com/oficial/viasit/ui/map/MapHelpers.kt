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
        // Habilitamos clustering para evitar solapamientos
        style.addSource(
            GeoJsonSource("cars-source", 
                org.maplibre.android.style.sources.GeoJsonOptions()
                    .withCluster(true)
                    .withClusterMaxZoom(14)
                    .withClusterRadius(50)
            )
        )
    }
    
    // Capa para vehículos individuales
    if (style.getLayer("cars-layer") == null) {
        val symbolLayer = SymbolLayer("cars-layer", "cars-source").withProperties(
            PropertyFactory.iconImage("car-icon"),
            PropertyFactory.iconSize(0.5f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor("center"),
            PropertyFactory.iconRotate(Expression.get("angulo"))
        ).withFilter(Expression.has("id")) // Solo si tiene ID (no es cluster)
        style.addLayer(symbolLayer)
    }

    // Capa para los círculos de los clusters
    if (style.getLayer("clusters-layer") == null) {
        val clusters = org.maplibre.android.style.layers.CircleLayer("clusters-layer", "cars-source")
        clusters.setProperties(
            PropertyFactory.circleColor(android.graphics.Color.parseColor("#3D5AFE")),
            PropertyFactory.circleRadius(18f),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeWidth(2f)
        )
        clusters.setFilter(Expression.has("point_count"))
        style.addLayer(clusters)
    }

    // Capa para el texto con la cuenta en los clusters
    if (style.getLayer("cluster-count-layer") == null) {
        val count = SymbolLayer("cluster-count-layer", "cars-source")
        count.setProperties(
            PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(android.graphics.Color.WHITE),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )
        count.setFilter(Expression.has("point_count"))
        style.addLayer(count)
    }
}

fun updateCarsSource(style: Style, autos: List<Auto>) {
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

    // Capa extra para el rastro (tails) de los buses - Más fina y transparente
    if (style.getSource("tails-source") == null) {
        style.addSource(GeoJsonSource("tails-source"))
    }
    if (style.getLayer("tails-layer") == null) {
        val tailsLayer = LineLayer("tails-layer", "tails-source").withProperties(
            PropertyFactory.lineColor("#3D5AFE"),
            PropertyFactory.lineWidth(2.5f),
            PropertyFactory.lineOpacity(0.4f), // Muy sutil
            PropertyFactory.lineDasharray(arrayOf(2f, 1f)), // Punteada
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

    val linePalette = listOf(
        "#3D5AFE", "#06B6D4", "#10B981", "#FFB300", "#F43F5E",
        "#8B5CF6", "#EC4899", "#14B8A6", "#F97316", "#84CC16"
    )

    val features = routes.mapIndexedNotNull { index, route ->
        try {
            // Caso 1: Historial de migajas (muchos puntos en un string separado por pipe '|')
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
                val lineString = LineString.fromLngLats(points)
                val props = com.google.gson.JsonObject().apply {
                    addProperty("color", route.color)
                    addProperty("linea", route.lineaName)
                    addProperty("isHistory", true)
                }
                return@mapIndexedNotNull Feature.fromGeometry(lineString, props)
            }

            // Caso 2: Ruta normal (Inicio, Fin y Waypoints separados por ';')
            val startParts = route.startPoint.split(",").map { it.trim().toDouble() }
            val endParts   = route.endPoint.split(",").map { it.trim().toDouble() }
            if (startParts.size < 2 || endParts.size < 2) return@mapIndexedNotNull null

            val startPt = Point.fromLngLat(startParts[1], startParts[0]) // lng, lat
            val endPt   = Point.fromLngLat(endParts[1],   endParts[0])

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
            null 
        }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

fun clearRoutesSource(style: Style) {
    val source = style.getSource("routes-source") as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}

