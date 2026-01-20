# Sistema de Disponibilidad Horaria - Ejemplos de Uso

## Resumen de la Implementación

Se implementó un sistema completo de disponibilidad horaria para profesionales médicos con las siguientes características:

### ✅ Lo que se implementó:

1. **Entidad ProfessionalAvailability** (renombrada desde ProfessionalSchedule)
   - Enum `DayOfWeek` (MONDAY, TUESDAY, etc.)
   - Campo `slotDurationMinutes` (5-120 minutos)
   - Campo `createdAt` con timestamp automático
   - Validaciones de negocio integradas

2. **DTOs**
   - `ProfessionalAvailabilityRequest` con validaciones Jakarta
   - `ProfessionalAvailabilityResponse` con información completa

3. **Repository actualizado**
   - Métodos específicos para consultas por professional y tenant
   - Soporte para eliminación transaccional

4. **Service extendido**
   - Validaciones de solapamiento de horarios
   - Control de acceso por tenant
   - Operaciones CRUD completas con auditoría

5. **Controller con endpoints REST**
   - Todos los endpoints con `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")`
   - Documentación Swagger completa

6. **Migración de base de datos**
   - Migración de datos existentes de integer a enum
   - Preservación de datos históricos

### 🎯 Endpoints Disponibles:

```
GET    /professionals/{id}/availability           - Obtener disponibilidades
POST   /professionals/{id}/availability           - Agregar una disponibilidad  
PUT    /professionals/{id}/availability           - Reemplazar todas las disponibilidades
DELETE /availability/{availabilityId}             - Eliminar una disponibilidad específica
```

## Ejemplos de Uso

### 1. Obtener disponibilidades de un profesional
```http
GET /professionals/1/availability
Authorization: Bearer {jwt-token}
```

### 2. Agregar una nueva disponibilidad
```http
POST /professionals/1/availability
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "dayOfWeek": "TUESDAY",
  "startTime": "08:00:00",
  "endTime": "12:00:00",
  "slotDurationMinutes": 30,
  "active": true
}
```

### 3. Configurar horarios completos del Dr. Juan Pérez
```http
PUT /professionals/1/availability
Authorization: Bearer {jwt-token}
Content-Type: application/json

[
  {
    "dayOfWeek": "TUESDAY",
    "startTime": "08:00:00",
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "active": true
  },
  {
    "dayOfWeek": "THURSDAY", 
    "startTime": "08:00:00",
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "active": true
  },
  {
    "dayOfWeek": "SATURDAY",
    "startTime": "08:00:00", 
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "active": true
  }
]
```

### 4. Ejemplo de respuesta
```json
{
  "id": 1,
  "dayOfWeek": "TUESDAY",
  "dayOfWeekDisplay": "Martes",
  "startTime": "08:00:00",
  "endTime": "18:00:00",
  "slotDurationMinutes": 30,
  "active": true,
  "createdAt": "2026-01-08T15:30:00",
  "professionalId": 1,
  "professionalName": "Dr. Juan Pérez"
}
```

### 5. Eliminar una disponibilidad específica
```http
DELETE /availability/5
Authorization: Bearer {jwt-token}
```

## Validaciones Implementadas

- ✅ `startTime` debe ser menor que `endTime`
- ✅ `slotDurationMinutes` debe estar entre 5 y 120
- ✅ No se permiten solapamientos de horarios para el mismo día
- ✅ Solo propietarios y administradores pueden gestionar disponibilidades
- ✅ Verificación de que el professional pertenece al tenant actual

## Características de Seguridad

- ✅ Multi-tenancy: Solo se pueden gestionar profesionales del tenant actual
- ✅ Autorización: Roles ADMIN y OWNER requeridos
- ✅ Validación de acceso: Verificación de pertenencia antes de operaciones
- ✅ Transacciones: Operaciones atómicas para integridad de datos

## Estructura de Base de Datos

La tabla `professional_schedules` ahora incluye:
- `day_of_week` (VARCHAR): 'MONDAY', 'TUESDAY', etc.
- `slot_duration_minutes` (INT): Duración de cada turno
- `created_at` (TIMESTAMP): Fecha de creación del registro

## Cómo Probar

1. **Ejecutar la aplicación** y aplicar migraciones
2. **Autenticarse** como OWNER o ADMIN
3. **Crear un profesional** si no existe
4. **Configurar disponibilidad** usando los endpoints mostrados
5. **Verificar** que los turnos se generan según la configuración

## Ejemplo Completo: Dr. Juan Pérez

Para configurar que el Dr. Juan Pérez atienda **martes, jueves y sábado de 8:00 AM a 18:00 PM con turnos cada 30 minutos**:

```bash
# 1. Obtener ID del Dr. Juan Pérez
GET /professionals

# 2. Configurar sus horarios (asumir ID = 1)
PUT /professionals/1/availability
[
  {
    "dayOfWeek": "TUESDAY",
    "startTime": "08:00:00", 
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "active": true
  },
  {
    "dayOfWeek": "THURSDAY",
    "startTime": "08:00:00",
    "endTime": "18:00:00", 
    "slotDurationMinutes": 30,
    "active": true
  },
  {
    "dayOfWeek": "SATURDAY",
    "startTime": "08:00:00",
    "endTime": "18:00:00",
    "slotDurationMinutes": 30,
    "active": true
  }
]
```

¡El sistema está listo para usar! 🚀