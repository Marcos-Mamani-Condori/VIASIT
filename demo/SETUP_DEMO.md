# Demo VIASIT — Pasos para correr el simulador

## Antes de la primera vez (solo una vez)

Abre una terminal en la carpeta del proyecto:

```bash
python3 -m venv .venv
.venv/bin/pip install -r demo/requirements.txt
```

---

## Configurar el script (solo una vez)

Abre `demo/demo_simulator.py` y edita estas 3 líneas:

```python
POCKETBASE_URL = "http://127.0.0.1:8090"    # si usas WiFi: cambia a IP de tu laptop
AUTO_1_ID = "pega_aqui_el_id_del_auto_1"
AUTO_2_ID = "pega_aqui_el_id_del_auto_2"
```

**¿Cómo obtener los IDs?**
PocketBase → Collections → autos → click en el auto → copia el campo `id`

**¿Cuál es mi IP si uso WiFi?**
```bash
ip addr show | grep "inet " | grep -v 127
# Busca algo como 192.168.1.X
```

---

## Correr el simulador (cada vez que hagas demo)

```bash
.venv/bin/python demo/demo_simulator.py
```

No necesitas "activar" el venv ni hacer nada más. Solo ese comando.

Para parar: `Ctrl + C`

---

## En PocketBase (hacer esto una sola vez)

Para que el script pueda mover los autos sin dar error:

```
Admin panel → Collections → autos → ícono 🔒 (API Rules)
→ "Update rule" → borra todo, deja VACÍO → Save
```

---

## Agregar más autos al simulador

En `demo_simulator.py`, edita la lista `VEHICULOS`:

```python
VEHICULOS = [
    {"id": "id_auto_1", "nombre": "Micro 132", "ruta": RUTA_132, "offset": 0},
    {"id": "id_auto_2", "nombre": "Minibus 2",  "ruta": RUTA_2,   "offset": 8},
    {"id": "id_auto_3", "nombre": "Micro 273",  "ruta": RUTA_132, "offset": 12},  # ← nuevo
]
```
