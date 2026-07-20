#!/usr/bin/env /home/marcos123rmc/AndroidStudioProjects/VIASIT/.venv/bin/python
"""
VIASIT - Demo Simulator
=======================
Simula el movimiento de vehículos en La Paz, Bolivia.
No toca nada del código Android. Solo hace PATCH a los vehículos en PocketBase.

NUEVO: Utiliza la API pública de OSRM para seguir las calles reales de forma
precisa e interpola los puntos para que el movimiento sea fluido y natural.

REQUISITO en PocketBase: Collection 'vehicles' → Update rule = (vacío)
"""

import requests
import time
import math
import sys
import random

# ─────────────────────────────────────────────────────────────────────────────
#  CONFIGURACIÓN — EDITA ESTOS VALORES
# ─────────────────────────────────────────────────────────────────────────────

POCKETBASE_URL = "https://dbvia.onrender.com"

# Segundos entre cada movimiento (2 = tiempo real, 1 = más rápido para demo)
INTERVALO_SEGUNDOS = 2

# Cuántos vehículos simular como máximo
MAX_VEHICULOS_SIMULADOS = 10

# ─────────────────────────────────────────────────────────────────────────────
#  WAYPOINTS (PUNTOS CLAVE DE LAS RUTAS)
#  OSRM se encargará de encontrar las calles entre estos puntos
# ─────────────────────────────────────────────────────────────────────────────

# Ruta 1: Micro 132 — Villa Fátima ↔ Plaza del Estudiante (Ida y Vuelta)
WAYPOINTS_132_LOOP = [
    (-16.4785, -68.1275),   # Villa Fátima - inicio
    (-16.4852, -68.1255),
    (-16.4921, -68.1228),
    (-16.4982, -68.1202),
    (-16.5003, -68.1193),   # Plaza del Estudiante - medio (retorno)
    (-16.4982, -68.1202),
    (-16.4921, -68.1228),
    (-16.4852, -68.1255),
    (-16.4785, -68.1275),   # Fin de vuelta
]

# Ruta 2: Minibus 2 — Max Paredes ↔ Sopocachi (Ida y Vuelta)
WAYPOINTS_2_LOOP = [
    (-16.4943, -68.1521),   # Max Paredes - inicio
    (-16.5012, -68.1420),
    (-16.5085, -68.1300),   # El Prado
    (-16.5155, -68.1235),
    (-16.5264, -68.1211),   # Sopocachi - medio (retorno)
    (-16.5155, -68.1235),
    (-16.5085, -68.1300),
    (-16.5012, -68.1420),
    (-16.4943, -68.1521),   # Fin de vuelta
]


# ─────────────────────────────────────────────────────────────────────────────
#  LÓGICA DE RUTAS (OSRM + INTERPOLACIÓN)
# ─────────────────────────────────────────────────────────────────────────────

def obtener_ruta_calles(waypoints):
    """
    Usa el servicio público de enrutamiento OSRM para obtener la geometría exacta
    de las calles entre los waypoints dados.
    """
    coords = ";".join([f"{lng},{lat}" for lat, lng in waypoints])
    url = f"http://router.project-osrm.org/route/v1/driving/{coords}?geometries=geojson&overview=full"

    try:
        r = requests.get(url, timeout=10)
        if r.status_code == 200:
            data = r.json()
            if "routes" in data and len(data["routes"]) > 0:
                # OSRM devuelve [lng, lat], convertimos a (lat, lng)
                geojson_coords = data["routes"][0]["geometry"]["coordinates"]
                return [(lat, lng) for lng, lat in geojson_coords]
    except Exception as e:
        print(f"⚠️ Error al conectar con OSRM para ruta: {e}")

    print("⚠️ Usando waypoints originales como fallback (linea recta)")
    return waypoints

def interpolar_ruta(ruta, max_distancia_grados=0.00015):
    """
    Rellena los espacios entre coordenadas para que el vehículo se mueva
    suavemente. max_distancia_grados=0.00015 equivale a unos 16 metros.
    Así, un salto de 2 segundos representa una velocidad realista de ~30 km/h.
    """
    ruta_interpolada = []
    for i in range(len(ruta) - 1):
        p1 = ruta[i]
        p2 = ruta[i+1]

        # Distancia entre puntos
        dist = math.hypot(p2[0] - p1[0], p2[1] - p1[1])
        pasos = max(1, int(dist / max_distancia_grados))

        for j in range(pasos):
            f = j / pasos
            lat = p1[0] + (p2[0] - p1[0]) * f
            lng = p1[1] + (p2[1] - p1[1]) * f
            ruta_interpolada.append((lat, lng))

    ruta_interpolada.append(ruta[-1])
    return ruta_interpolada

def calcular_angulo(lat1, lng1, lat2, lng2):
    """Calcula la orientación del vehículo."""
    dlng = math.radians(lng2 - lng1)
    lat1_r = math.radians(lat1)
    lat2_r = math.radians(lat2)
    x = math.sin(dlng) * math.cos(lat2_r)
    y = math.cos(lat1_r) * math.sin(lat2_r) - math.sin(lat1_r) * math.cos(lat2_r) * math.cos(dlng)
    angulo = math.degrees(math.atan2(x, y))
    return (angulo + 360) % 360


# ─────────────────────────────────────────────────────────────────────────────
#  POCKETBASE Y SIMULACIÓN
# ─────────────────────────────────────────────────────────────────────────────

def mover_auto(auto_id, lat, lng, angulo):
    url = f"{POCKETBASE_URL}/api/collections/vehicles/records/{auto_id}"
    data = {"lat": lat, "lng": lng, "angle": angulo}
    try:
        resp = requests.patch(url, json=data, timeout=5)
        if resp.status_code == 200:
            return True
        else:
            return False
    except requests.exceptions.RequestException:
        return False

def obtener_vehiculos():
    try:
        r = requests.get(f"{POCKETBASE_URL}/api/collections/vehicles/records?perPage={MAX_VEHICULOS_SIMULADOS}", timeout=5)
        if r.status_code == 200:
            return r.json().get("items", [])
    except Exception:
        pass
    return []

def main():
    print("=" * 50)
    print("  VIASIT Demo Simulator — Modo Calles Reales")
    print("=" * 50)

    print(f"\n🔍 Verificando conexión a PocketBase ({POCKETBASE_URL})...")
    try:
        resp = requests.get(f"{POCKETBASE_URL}/api/health", timeout=5)
        if resp.status_code != 200:
            print(f"   ⚠️  PocketBase respondió con código {resp.status_code}")
        else:
            print("   ✅ Conectado a PocketBase")
    except requests.exceptions.ConnectionError:
        print(f"   ❌ No se puede conectar a {POCKETBASE_URL}")
        sys.exit(1)

    print("\n🗺️  Generando rutas siguiendo las calles (vía OSRM)...")
    ruta_132_bruta = obtener_ruta_calles(WAYPOINTS_132_LOOP)
    ruta_2_bruta = obtener_ruta_calles(WAYPOINTS_2_LOOP)

    print("✨ Interpolando puntos para movimiento suave...")
    RUTA_132 = interpolar_ruta(ruta_132_bruta)
    RUTA_2 = interpolar_ruta(ruta_2_bruta)

    print(f"   → Ruta 132: {len(RUTA_132)} puntos de alta resolución")
    print(f"   → Ruta 2: {len(RUTA_2)} puntos de alta resolución")

    RUTAS_DISPONIBLES = [RUTA_132, RUTA_2]

    print("\n🔍 Obteniendo vehículos de la base de datos...")
    vehiculos_db = obtener_vehiculos()
    if not vehiculos_db:
        print("❌ No se encontraron vehículos en la colección 'vehicles'.")
        print("   Asegúrate de crear algunos en la app primero.")
        sys.exit(1)

    print(f"✅ Se encontraron {len(vehiculos_db)} vehículos.")

    vehiculos_simulados = []
    for idx, v in enumerate(vehiculos_db):
        ruta_asignada = RUTAS_DISPONIBLES[idx % len(RUTAS_DISPONIBLES)]
        # Asignar a diferentes partes de la ruta para que no salgan todos del mismo lugar
        offset_inicial = int((len(ruta_asignada) / len(vehiculos_db)) * idx) % len(ruta_asignada)

        vehiculos_simulados.append({
            "id": v["id"],
            "placa": v.get("plate", "Desconocida"),
            "ruta": ruta_asignada,
            "offset": offset_inicial
        })
        print(f"   🚌 {v.get('plate', 'Desconocida')} ({v['id']}) asignado a Ruta {idx % len(RUTAS_DISPONIBLES) + 1}")

    posiciones = [v["offset"] for v in vehiculos_simulados]

    print(f"\n🚀 Iniciando simulación...")
    print(f"   Moviendo {len(vehiculos_simulados)} vehículos cada {INTERVALO_SEGUNDOS} seg")
    print(f"   Presiona Ctrl+C para detener\n")

    iteracion = 0
    try:
        while True:
            exitos = 0
            for i, vehiculo in enumerate(vehiculos_simulados):
                ruta = vehiculo["ruta"]
                idx_actual = posiciones[i]
                idx_siguiente = (idx_actual + 1) % len(ruta)

                lat, lng = ruta[idx_actual]
                lat_sig, lng_sig = ruta[idx_siguiente]
                angulo = calcular_angulo(lat, lng, lat_sig, lng_sig)

                if mover_auto(vehiculo["id"], lat, lng, angulo):
                    exitos += 1

                posiciones[i] = idx_siguiente

            iteracion += 1
            print(f"  [Iteración {iteracion}] {exitos}/{len(vehiculos_simulados)} vehículos actualizados correctamente. Esperando {INTERVALO_SEGUNDOS}s...", end="\r")
            time.sleep(INTERVALO_SEGUNDOS)

    except KeyboardInterrupt:
        print("\n\n🛑 Simulación detenida.")

if __name__ == "__main__":
    main()