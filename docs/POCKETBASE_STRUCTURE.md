# Estructura de PocketBase para VIASIT

## Resumen

Necesitas crear **2 colecciones** en PocketBase:

---

## 1. **users** (Usuarios)

> ⚠️ Esta colección YA viene por defecto en PocketBase. Solo necesitas agregar los campos adicionales.

### Campos a AGREGAR:

| Campo | Tipo | Requerido | Descripción | Ejemplo |
|-------|------|-----------|-------------|---------|
| name | Text | Sí | Nombre completo | "Juan Pérez" |
| phone | Text | No | Teléfono (opcional) | "12345678" |
| role | Select | Sí | Tipo de usuario | "USUARIO", "CONDUCTOR", "INVITADO" |
| isInService | Bool | No | Conductor en servicio | true/false |
| isActive | Bool | No | Cuenta activa | true/false |

### Configuración de la colección:

1. Ve a **Collection Settings** → **Fields**
2. Agrega los campos de arriba
3. En **API Rules**:
   - **List/View**: `""` (público - todos pueden ver usuarios)
   - **Create**: `""` (público - para registro)
   - **Update/Delete**: `@request.auth.id != ""` (solo el usuario puede editar su propia cuenta)

### Auth Methods:
- Habilitar: **Email/Password**

---

## 2. **autos** (Vehículos de conductores)

> 📝 Esta colección la debes CREAR nueva.

### Campos a crear:

| Campo | Tipo | Requerido | Descripción | Ejemplo |
|-------|------|-----------|-------------|---------|
| userId | Relation | Sí | Relaciona con users | "RECORD_ID" |
| placa | Text | Sí | Placa del vehículo | "ABC-1234" |
| linea | Text | Sí | Línea/Ruta del colectivo | "Línea 101" |
| lat | Number | No | Latitud actual | -17.7835 |
| lng | Number | No | Longitud actual | -63.1821 |
| angulo | Number | No | Ángulo/dirección (0-360) | 45.0 |
| colectivoid | Text | No | ID del colectivo | "COLECT-001" |

### Configuración de la colección:

1. Crea una nueva colección llamada **autos**
2. Agrega los campos de arriba
3. En **API Rules**:

| Regla | Valor | Descripción |
|--------|-------|-------------|
| **List** | `""` | Público - todos pueden ver vehículos |
| **View** | `""` | Público - todos pueden ver detalles |
| **Create** | `@request.auth.id != ""` | Solo usuarios logueados |
| **Update** | `@request.auth.id != ""` | Solo usuarios logueados |
| **Delete** | `@request.auth.id != ""` | Solo usuarios logueados |

### Relation Configuration (para userId):
- **Collection**: users
- **Max documents**: 1
- **Cascade delete**: false
- **Field ID**: userId

---

## 3. **locations** (Historial de ubicaciones) - OPCIONAL

Si quieres guardar el historial de ubicaciones:

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| userId | Relation | Relaciona con users | "RECORD_ID" |
| lat | Number | Latitud | -17.7835 |
| lng | Number | Longitud | -63.1821 |
| accuracy | Number | Precisión GPS (metros) | 10.5 |
| speed | Number | Velocidad (km/h) | 45.0 |
| bearing | Number | Dirección (0-360) | 180.0 |
| timestamp | Number | Fecha/hora (Unix timestamp) | 1707324000 |
| isInService | Bool | Si está en servicio | true |

---

## 📊 Resumen Visual del Modelo de Datos

```
┌─────────────────────────────────────────────────────────┐
│                     users                               │
├─────────────────────────────────────────────────────────┤
│  • id (auto-generated)                                  │
│  • email (PocketBase default)                           │
│  • password (PocketBase default)                        │
│  • name ← AGREGAR                                       │
│  • phone ← AGREGAR (opcional)                          │
│  • role ← AGREGAR (USUARIO/CONDUCTOR/INVITADO)         │
│  • isInService ← AGREGAR                                │
│  • isActive ← AGREGAR                                   │
│  • created (auto)                                       │
│  • updated (auto)                                       │
└─────────────────────────────────────────────────────────┘
                           │
                           │ Relation (1:N)
                           ▼
┌─────────────────────────────────────────────────────────┐
│                       autos                              │
├─────────────────────────────────────────────────────────┤
│  • id (auto-generated)                                  │
│  • userId ─────────────────────────┐                    │
│  • placa                           │ Relation          │
│  • linea                           │ a users           │
│  • lat                             │                   │
│  • lng                             │                   │
│  • angulo                          │                   │
│  • colectivoid                     │                   │
│  • created (auto)                   │                   │
│  • updated (auto)                  ┘                   │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 Configuración en Android

### URL de PocketBase:
```kotlin
// En PocketBaseClient.kt
const val POCKETBASE_URL = "https://TU_POCKETBASE_URL.com"
```

### Permisos de API Rules Recomendados:

**Para users:**
```json
{
  "listRule": "",
  "viewRule": "",
  "createRule": "",
  "updateRule": "@request.auth.id != \"\"",
  "deleteRule": "@request.auth.id != \"\""
}
```

**Para autos:**
```json
{
  "listRule": "",
  "viewRule": "",
  "createRule": "@request.auth.id != \"\"",
  "updateRule": "@request.auth.id != \"\"",
  "deleteRule": "@request.auth.id != \"\""
}
```

---

## 📱 Ejemplo de uso en la app:

```kotlin
// Registro de usuario
POST /api/collections/users/records
{
  "email": "conductor@test.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "Juan Pérez",
  "phone": "12345678",
  "role": "CONDUCTOR"
}

// Crear vehículo
POST /api/collections/autos/records
{
  "userId": "RECORD_ID_DEL_USUARIO",
  "placa": "ABC-1234",
  "linea": "Línea 101"
}

// Ver todos los vehículos
GET /api/collections/autos/records

// Ver vehículo específico
GET /api/collections/autos/records/RECORD_ID
```

---

## ❓ Preguntas Frecuentes

**¿Puedo usar el mismo PocketBase para desarrollo y producción?**
Sí, pero se recomienda crear dos instancias separadas.

**¿Necesito configurar webhooks?**
No es necesario para el funcionamiento básico.

**¿Los campos created/updated son automáticos?**
Sí, PocketBase los maneja automáticamente.

**¿Cómo manejo la autenticación?**
PocketBase Auth ya viene configurado. Usa `/api/collections/users/auth-with-password`
