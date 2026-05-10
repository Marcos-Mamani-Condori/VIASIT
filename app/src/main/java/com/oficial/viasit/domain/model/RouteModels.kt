package com.oficial.viasit.domain.model

data class RouteLocation(
    val lat: Double,
    val lng: Double,
    val name: String = ""
)

data class RouteData(
    val name: String,
    val description: String,
    val startPoint: RouteLocation?,
    val endPoint: RouteLocation?,
    val waypoints: List<RouteLocation> = emptyList()
)

enum class PointSelectionMode {
    START,
    END 
}
