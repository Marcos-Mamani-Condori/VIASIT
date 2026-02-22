# Estructura de PocketBase para VIASIT - Sistema de Admin y Líneas

## Resumen de Roles

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **ADMIN** | Administrador del sistema | Gestionar líneas, conductores, ver estadísticas |
| **CONDUCTOR** | Conductor de colectivo | Registrar vehículo, activar servicio, enviar ubicación |
| **USUARIO** | Usuario normal | Ver vehículos en el mapa |
| **INVITADO** | Usuario sin registro | Ver vehículos en el mapa (limitado) |

---

## 1. **users** (Usuarios)

> ⚠️ Esta colección YA viene por defecto en PocketBase.

### Campos a AGREGAR:

| Campo | Tipo | Requerido | Descripción | Ejemplo |
|-------|------|-----------|-------------|---------|
| name | Text | Sí | Nombre completo | "Juan Pérez" |
| phone | Text | No | Teléfono (opcional) | "12345678" |
| role | Select | Sí | Tipo de usuario | "ADMIN", "CONDUCTOR", "USUARIO", "INVITADO" |
| isInService | Bool | No | Conductor en servicio | true/false |
| isActive | Bool | No | Cuenta activa | true/false |
| lineaId | Relation | No | Línea asignada (solo conductores) | "RECORD_ID" |

### Configuración de la colección:

1. Ve a **Collection Settings** → **Fields**
2. Agrega los campos de arriba
3. En **API Rules**:

| Regla | Valor | Descripción |
|--------|-------|-------------|
| **List** | `@request.auth.role = "ADMIN"` | Solo admins ven todos los usuarios |
| **View** | `@request.auth.id != ""` | Usuarios ven su propio perfil |
| **Create** | `""` | Público - para registro (rol por defecto: USUARIO) |
| **Update** | `@request.auth.id = @collection.users.id` | Usuario puede editar su perfil |
| **Delete** | `@request.auth.role = "ADMIN"` | Solo admins eliminan usuarios |

### Auth Methods:
- Habilitar: **Email/Password**

---

## 2. **lineas** (Líneas de colectivo)

> 📝 Esta colección la debes CREAR nueva. **Solo admins pueden crear/editar.**

### Campos a crear:

| Campo | Tipo | Requerido | Descripción | Ejemplo |
|-------|------|-----------|-------------|---------|
| nombre | Text | Sí | Nombre de la línea | "Línea 101" |
| codigo | Text | Sí | Código único | "LINEA-101" |
| color | Text | No | Color hex para el mapa | "#FF5722" |
| descripcion | Text | No | Descripción | "Ruta: Centro - Sur" |
| adminId | Relation | Sí | Admin que creó la línea | "RECORD_ID" |
| isActive | Bool | No | Línea activa | true |
| created | Date | Auto | Fecha creación | - |
| updated | Date | Auto | Última modificación | - |

### Configuración de la colección:

1. Crea una nueva colección llamada **lineas**
2. Agrega los campos de arriba
3. En **API Rules**:

| Regla | Valor | Descripción |
|--------|-------|-------------|
| **List** | `@request.auth.id != ""` | Usuarios logueados ven líneas |
| **View** | `@request.auth.id != ""` | Usuarios logueados ven detalles |
| **Create** | `@request.auth.role = "ADMIN"` | Solo admins crean líneas |
| **Update** | `@request.auth.role = "ADMIN" && @collection.lineas.adminId = @request.auth.id` | Solo el admin creador puede editar |
| **Delete** | `@request.auth.role = "ADMIN" && @collection.lineas.adminId = @request.auth.id` | Solo el admin creador puede eliminar |

### Relation Configuration (para adminId):
- **Collection**: users
- **Max documents**: 1
- **Cascade delete**: false

---

## 3. **autos** (Vehículos de conductores)

> 📝 Esta colección la debes CREAR nueva.

### Campos a crear:

| Campo | Tipo | Requerido | Descripción | Ejemplo |
|-------|------|-----------|-------------|---------|
| userId | Relation | Sí | Relaciona con users | "RECORD_ID" |
| placa | Text | Sí | Placa del vehículo | "ABC-1234" |
| lineaId | Relation | Sí | Línea asignada (obligatorio) | "RECORD_ID" |
| lat | Number | No | Latitud actual | -17.7835 |
| lng | Number | No | Longitud actual | -63.1821 |
| angulo | Number | No | Ángulo/dirección (0-360) | 45.0 |
| isVerified | Bool | No | Verificado por admin | true/false |
| verifiedBy | Relation | No | Admin que verificó | "RECORD_ID" |
| created | Date | Auto | Fecha creación | - |
| updated | Date | Auto | Última modificación | - |

### Configuración de la colección:

1. Crea una nueva colección llamada **autos**
2. Agrega los campos de arriba
3. En **API Rules**:

| Regla | Valor | Descripción |
|--------|-------|-------------|
| **List** | `@request.auth.id != ""` | Usuarios logueados ven vehículos |
| **View** | `@request.auth.id != ""` | Usuarios logueados ven detalles |
| **Create** | `@request.auth.role = "CONDUCTOR"` | Solo conductores crean vehículos |
| **Update** | `@request.auth.id = @collection.autos.userId` | Solo el dueño puede editar |
| **Delete** | `@request.auth.id = @collection.autos.userId` | Solo el dueño puede eliminar |

### Relation Configuration:
- **Para userId**: users (1:1, cascade delete: false)
- **Para lineaId**: lineas (1:1, cascade delete: false)

---

## 4. **verificaciones** (Historial de verificaciones de conductores)

> 📝 Opcional - para auditar quién verificó a quién

### Campos:

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| conductorId | Relation | Conductor verificado | "RECORD_ID" |
| autoId | Relation | Vehículo verificado | "RECORD_ID" |
| verifierId | Relation | Admin que verificó | "RECORD_ID" |
| action | Select | Tipo de acción | "VERIFY", "UNVERIFY", "SUSPEND" |
| reason | Text | Razón (opcional) | "Documentación verificada" |
| created | Date | Fecha de verificación | - |

---

## 📊 Resumen Visual del Modelo de Datos

```
┌─────────────────────────────────────────────────────────┐
│                     users                                │
├─────────────────────────────────────────────────────────┤
│  • id (auto-generated)                                  │
│  • email (PocketBase default)                           │
│  • password (PocketBase default)                       │
│  • name                                                 │
│  • phone (opcional)                                     │
│  • role (ADMIN/CONDUCTOR/USUARIO/INVITADO)             │
│  • isInService                                         │
│  • isActive                                             │
│  • lineaId ─────────────────────────┐                 │
│  • created (auto)                       │ Relation     │
│  • updated (auto)                       │ a lineas    │
└─────────────────────────────────────────┼─────────────┘
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                          │
              ▼                          ▼                          ▼
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│       lineas            │  │         autos           │  │      verificaciones     │
├─────────────────────────┤  ├─────────────────────────┤  ├─────────────────────────┤
│  • id                   │  │  • id                   │  │  • id                   │
│  • nombre               │  │  • userId ───────────┐ │  │  • conductorId          │
│  • codigo               │  │  • placa              │ │  │  • autoId              │
│  • color                │  │  • lineaId ──────────┼─┼──│  • verifierId           │
│  • descripcion          │  │  • lat                │ │  │  • action              │
│  • adminId ──────────┐  │  │  • lng                │ │  │  • reason              │
│  • isActive           │  │  │  • angulo            │ │  │  • created             │
│  • created (auto)     │  │  │  • isVerified        │ │  └─────────────────────────┘
│  • updated (auto)     │  │  │  • verifiedBy        │ │
└─────────────────────────┘  │  │  • created (auto)   │ │
                             │  │  • updated (auto)  │ │
                             │  └─────────────────────┘ │
                             └──────────────────────────┘
```

---

## 🔐 Sistema de Registro de Admins

### Opción 1: Registro manual (RECOMENDADO para producción)

```
1. Crear admin directamente en PocketBase Dashboard
   - Ir a usuarios → Crear nuevo
   - Asignar rol "ADMIN"
   - Establecer contraseña segura

2. Admin luego puede:
   - Crear líneas desde la app
   - Verificar conductores
```

### Opción 2: Registro con código de invitación

```kotlin
// En AuthRepository.kt
const val ADMIN_INVITE_CODE = "VIASIT-ADMIN-2024" // Código secreto

fun registerAsAdmin(email, password, name, inviteCode) {
    if (inviteCode != ADMIN_INVITE_CODE) {
        throw Exception("Código de invitación inválido")
    }
    // Proceder con registro como ADMIN
}
```

### API Rules restrictivos para admins:

```json
// Solo admins pueden crear otros admins
{
  "createRule": "@request.auth.role = 'ADMIN' || @request.data.role != 'ADMIN'"
}
```

---

## ✅ Flujo de Verificación de Conductores

```
1. CONDUCTOR se registra en la app
   └─ Rol: CONDUCTOR (inicialmente no verificado)

2. CONDUCTOR registra su vehículo
   └─ Debe seleccionar línea de lista (no escribir)
   └─ isVerified = false inicialmente

3. ADMIN recibe notificación (o revisa lista)
   └─ Verifica documentos del conductor
   └─ Confirma que pertenece a la línea

4. ADMIN marca como verificado
   └─ isVerified = true
   └─ verifiedBy = ID del admin
   └─ Crea registro en verificaciones (auditoría)

5. CONDUCTOR puede activar servicio
   └─ Solo si isVerified = true
```

---

## 🔒 Reglas de Seguridad

### 1. Conductores no pueden crear líneas
```json
// En colección lineas
{
  "createRule": "@request.auth.role = 'ADMIN'"
}
```

### 2. Conductores no pueden modificar su línea asignada
```json
// En colección autos
{
  "updateRule": "@request.auth.id = @collection.autos.userId && @request.data.lineaId = @collection.autos.lineaId"
}
```

### 3. Solo admins pueden verificar conductores
```json
// Si tienes campo isVerified en autos
{
  "updateRule": "(@request.auth.id = @collection.autos.userId && @request.data.isVerified != true) || @request.auth.role = 'ADMIN'"
}
```

### 4. Vehículos no verificados no aparecen en mapa
```kotlin
// En AutosViewModel.kt
fun getActiveVehicles(): List<Auto> {
    return autos.filter { it.isVerified && it.isActive }
}
```

---

## 📱 Ejemplo de uso en la app

### Registro de conductor:
```kotlin
POST /api/collections/users/records
{
  "email": "conductor@test.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "Juan Pérez",
  "phone": "12345678",
  "role": "CONDUCTOR"
}
```

### Obtener líneas disponibles:
```kotlin
GET /api/collections/lineas/records?filter=isActive = true
// Respuesta:
{
  "items": [
    {"id": "xxx", "nombre": "Línea 101", "codigo": "LINEA-101"},
    {"id": "yyy", "nombre": "Línea 202", "codigo": "LINEA-202"}
  ]
}
```

### Registrar vehículo (conductor elige de lista):
```kotlin
POST /api/collections/autos/records
{
  "userId": "ID_DEL_CONDUCTOR",
  "placa": "ABC-1234",
  "lineaId": "ID_DE_LA_LINEA_ELEGIDA"
}
```

### Admin verifica conductor:
```kotlin
PATCH /api/collections/autos/records/ID_DEL_AUTO
{
  "isVerified": true,
  "verifiedBy": "ID_DEL_ADMIN"
}

//同时创建 registro de verificación
POST /api/collections/verificaciones/records
{
  "conductorId": "ID_DEL_CONDUCTOR",
  "autoId": "ID_DEL_AUTO",
  "verifierId": "ID_DEL_ADMIN",
  "action": "VERIFY",
  "reason": "Documentación verificada"
}
```

---

## 🔧 Configuración en Android

### Modelos nuevos:

```kotlin
// Linea.kt
@Serializable
data class Linea(
    val id: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val color: String = "#4CAF50",
    val descripcion: String = "",
    val adminId: String = "",
    val isActive: Boolean = true
)

// Verificacion.kt
@Serializable
data class Verificacion(
    val id: String = "",
    val conductorId: String = "",
    val autoId: String = "",
    val verifierId: String = "",
    val action: String = "", // VERIFY, UNVERIFY, SUSPEND
    val reason: String = "",
    val created: String = ""
)
```

---

## 📋 Checklist de Implementación

### Backend (PocketBase):
- [ ] Agregar rol ADMIN a users
- [ ] Agregar campo lineaId a users
- [ ] Crear colección lineas
- [ ] Crear colección verificaciones (opcional)
- [ ] Configurar API Rules restrictivos
- [ ] Crear primer admin manualmente

### Android:
- [ ] Actualizar modelo User con lineaId
- [ ] Crear modelo Linea
- [ ] Crear modelo Verificacion
- [ ] Crear API para obtener líneas
- [ ] Crear API para registrar vehículo con lineaId
- [ ] Crear API para verificar conductor (solo admin)
- [ ] Actualizar RegisterVehicleScreen con dropdown de líneas
- [ ] Filtrar vehículos no verificados del mapa
- [ ] Crear pantalla Admin (gestionar líneas)
- [ ] Crear pantalla Admin (verificar conductores)
