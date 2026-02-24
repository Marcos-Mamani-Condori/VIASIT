package com.oficial.viasit.ui.routes

/**
 * Modelos de datos usados en la pantalla de creación de rutas.
 *
 * Separados del resto de la UI para poder reutilizarlos en
 * otros módulos sin importar la pantalla completa.
 */

/** Un punto geográfico seleccionado en el mapa */
data class RouteLocation(
    val lat: Double,
    val lng: Double,
    val name: String = ""
)

/** Datos completos de una ruta antes de guardarla */
data class RouteData(
    val name: String,
    val description: String,
    val startPoint: RouteLocation?,
    val endPoint: RouteLocation?,
    /** Puntos intermedios opcionales trazados por el admin */
    val waypoints: List<RouteLocation> = emptyList()
)

/** Estado del mapa: cuál punto se está seleccionando ahora */
enum class PointSelectionMode {
    START,  // El usuario está eligiendo el punto de inicio
    END     // El usuario está eligiendo el punto final
}
