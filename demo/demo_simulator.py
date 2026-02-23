#!/usr/bin/env /home/marcos123rmc/AndroidStudioProjects/VIASIT/.venv/bin/python
"""
VIASIT - Demo Simulator
=======================
Simula el movimiento de minibuses en La Paz, Bolivia.
No toca nada del código Android. Solo hace PATCH a los autos en PocketBase.

REQUISITO en PocketBase: Collection 'autos' → Update rule = (vacío)

Instrucciones:
  1. Completa POCKETBASE_URL, AUTO_1_ID y AUTO_2_ID abajo
  2. Corre:  python3 demo_simulator.py
  3. Abre la app y ve al mapa
  4. Ctrl+C para parar

Requisitos:
  pip install requests
"""

import requests
import time
import math
import sys

# ─────────────────────────────────────────────────────────────────────────────
#  CONFIGURACIÓN — EDITA ESTOS VALORES
# ─────────────────────────────────────────────────────────────────────────────

POCKETBASE_URL = "http://127.0.0.1:8090"   # URL de tu PocketBase (misma red WiFi: usa IP del laptop)
CONDUCTOR_EMAIL = "conductor1@demo.com"     # Email del conductor dueño del auto (usuario en PocketBase)
CONDUCTOR_PASSWORD = "password123"          # Contraseña del conductor

# IDs de los autos en tu colección 'autos' de PocketBase
# Los encuentras en: PocketBase → Collections → autos → (click en el registro) → copia el ID
AUTO_1_ID = "REEMPLAZA_CON_ID_AUTO_1"
AUTO_2_ID = "REEMPLAZA_CON_ID_AUTO_2"

# Segundos entre cada movimiento (2 = tiempo real, 1 = más rápido para demo)
INTERVALO_SEGUNDOS = 2

# ─────────────────────────────────────────────────────────────────────────────
#  RUTAS REALES DE LA PAZ, BOLIVIA
# ─────────────────────────────────────────────────────────────────────────────

# Ruta 1: Micro 132 — Villa Fátima → Plaza del Estudiante
# Recorre Av. Baptista → Av. Montes → El Prado
RUTA_132 = [
    (-16.4785, -68.1275),   # Villa Fátima - inicio
    (-16.4803, -68.1270),
    (-16.4825, -68.1262),
    (-16.4852, -68.1255),
    (-16.4878, -68.1248),   # Av. Baptista
    (-16.4902, -68.1238),
    (-16.4921, -68.1228),
    (-16.4940, -68.1218),
    (-16.4958, -68.1210),   # cruce Av. Montes
    (-16.4970, -68.1205),
    (-16.4982, -68.1202),
    (-16.4990, -68.1198),
    (-16.5003, -68.1193),   # Plaza del Estudiante - fin
    # Vuelta (evitar teletransportación)
    (-16.4990, -68.1198),
    (-16.4982, -68.1202),
    (-16.4970, -68.1205),
    (-16.4958, -68.1210),
    (-16.4940, -68.1218),
    (-16.4921, -68.1228),
    (-16.4902, -68.1238),
    (-16.4878, -68.1248),
    (-16.4852, -68.1255),
    (-16.4825, -68.1262),
    (-16.4803, -68.1270),
]

# Ruta 2: Minibus 2 — Max Paredes → Sopocachi
# Recorre Av. Max Paredes → El Prado → Av. 6 de Agosto
RUTA_2 = [
    (-16.4943, -68.1521),   # Max Paredes - inicio
    (-16.4960, -68.1498),
    (-16.4978, -68.1475),
    (-16.4997, -68.1450),
    (-16.5012, -68.1420),   # cruce hacia El Prado
    (-16.5028, -68.1390),
    (-16.5045, -68.1360),
    (-16.5065, -68.1330),
    (-16.5085, -68.1300),   # El Prado
    (-16.5105, -68.1275),
    (-16.5130, -68.1252),
    (-16.5155, -68.1235),
    (-16.5180, -68.1220),
    (-16.5210, -68.1215),
    (-16.5240, -68.1213),
    (-16.5264, -68.1211),   # Sopocachi - fin
    # Vuelta
    (-16.5240, -68.1213),
    (-16.5210, -68.1215),
    (-16.5180, -68.1220),
    (-16.5155, -68.1235),
    (-16.5130, -68.1252),
    (-16.5105, -68.1275),
    (-16.5085, -68.1300),
    (-16.5065, -68.1330),
    (-16.5045, -68.1360),
    (-16.5028, -68.1390),
    (-16.5012, -68.1420),
    (-16.4997, -68.1450),
    (-16.4978, -68.1475),
    (-16.4960, -68.1498),
]

# ─────────────────────────────────────────────────────────────────────────────
#  CONFIGURACIÓN DE VEHÍCULOS DEMO
#  Agrega o quita vehículos aquí. Asegúrate de que el usuario_email/password
#  sea el conductor dueño del auto (para que PocketBase lo permita actualizar).
# ─────────────────────────────────────────────────────────────────────────────

VEHICULOS = [
    {"id": AUTO_1_ID, "nombre": "Micro 132",  "ruta": RUTA_132, "offset": 0},
    {"id": AUTO_2_ID, "nombre": "Minibus 2",  "ruta": RUTA_2,   "offset": 8},
    # ← agrega más autos aquí
]

# ─────────────────────────────────────────────────────────────────────────────
#  LÓGICA INTERNA — No necesitas editar nada de aquí para abajo
# ─────────────────────────────────────────────────────────────────────────────

def calcular_angulo(lat1, lng1, lat2, lng2):
    """Calcula el ángulo de movimiento entre dos coordenadas GPS (bearing)."""
    dlng = math.radians(lng2 - lng1)
    lat1_r = math.radians(lat1)
    lat2_r = math.radians(lat2)
    x = math.sin(dlng) * math.cos(lat2_r)
    y = math.cos(lat1_r) * math.sin(lat2_r) - math.sin(lat1_r) * math.cos(lat2_r) * math.cos(dlng)
    angulo = math.degrees(math.atan2(x, y))
    return (angulo + 360) % 360


def mover_auto(auto_id, lat, lng, angulo):
    """Hace PATCH al registro del auto con la nueva posición. Sin autenticación."""
    url = f"{POCKETBASE_URL}/api/collections/autos/records/{auto_id}"
    data = {"lat": lat, "lng": lng, "angulo": angulo}
    try:
        resp = requests.patch(url, json=data, timeout=5)
        return resp.status_code == 200
    except requests.exceptions.RequestException as e:
        print(f"  ⚠️  Error al mover auto {auto_id}: {e}")
        return False


def main():
    print("=" * 50)
    print("  VIASIT Demo Simulator — La Paz, Bolivia")
    print("=" * 50)

    ids_invalidos = [v for v in VEHICULOS if "REEMPLAZA" in v["id"]]
    if ids_invalidos:
        print("\n❌ ERROR: Reemplaza los IDs de los autos (AUTO_1_ID, AUTO_2_ID) al inicio del archivo\n")
        sys.exit(1)

    posiciones = [v["offset"] % len(v["ruta"]) for v in VEHICULOS]

    print(f"\n🚌 Iniciando simulación con {len(VEHICULOS)} vehículos...")
    print(f"   Intervalo: {INTERVALO_SEGUNDOS} seg | Ctrl+C para parar\n")

    iteracion = 0
    try:
        while True:
            for i, vehiculo in enumerate(VEHICULOS):
                ruta = vehiculo["ruta"]
                idx_actual = posiciones[i]
                idx_siguiente = (idx_actual + 1) % len(ruta)

                lat, lng = ruta[idx_actual]
                lat_sig, lng_sig = ruta[idx_siguiente]
                angulo = calcular_angulo(lat, lng, lat_sig, lng_sig)

                ok = mover_auto(vehiculo["id"], lat, lng, angulo)
                icono = "✅" if ok else "❌"
                print(f"  {icono} [{vehiculo['nombre']}] lat={lat:.4f}, lng={lng:.4f}, angulo={angulo:.0f}°  (punto {idx_actual+1}/{len(ruta)})")

                posiciones[i] = idx_siguiente

            iteracion += 1
            print(f"  — iteración {iteracion} — esperando {INTERVALO_SEGUNDOS}s...")
            time.sleep(INTERVALO_SEGUNDOS)
            print()

    except KeyboardInterrupt:
        print("\n\n🛑 Simulación detenida.")


if __name__ == "__main__":
    main()
