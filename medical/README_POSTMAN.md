# 📋 Guía de Testing con Postman - Medical SaaS API

## 🚀 Configuración Inicial

### 1. Importar Archivos en Postman
1. Abrir Postman
2. Click en **Import**
3. Importar estos archivos:
   - `Medical_SaaS_API.postman_collection.json` (Colección)
   - `Medical_SaaS_Development.postman_environment.json` (Environment)

### 2. Seleccionar Environment
1. En Postman, selecciona el environment **"Medical SaaS - Development"** en el dropdown superior derecho
2. Verificar que las variables estén configuradas:
   - `base_url`: `http://localhost:8080/api`
   - `tenant_slug`: `demo-clinic`

## 🏥 Testing del MVP

### ✅ **Paso 1: Verificar que la aplicación esté corriendo**
```bash
# En terminal, ejecutar:
mvn spring-boot:run

# Verificar que aparezca:
# "Started MedicalApplication ... Tomcat started on port 8080"
```

### ✅ **Paso 2: Probar Endpoints Públicos**

#### 2.1 Health Check
- **Endpoint**: `GET /public/health`
- **Esperado**: `"API funcionando correctamente"`

#### 2.2 Listar Especialidades
- **Endpoint**: `GET /public/specialties` 
- **Esperado**: Array con 12 especialidades (Medicina General, Cardiología, etc.)

### ✅ **Paso 3: Probar APIs por Tenant**

#### 3.1 Información del Consultorio
- **Endpoint**: `GET /t/demo-clinic`
- **Esperado**: Información de "Clínica Demo"

#### 3.2 Profesionales del Consultorio
- **Endpoint**: `GET /t/demo-clinic/professionals`
- **Esperado**: 2 profesionales (Dr. Juan Pérez, Dra. Ana García)

#### 3.3 Especialidades del Consultorio
- **Endpoint**: `GET /t/demo-clinic/specialties`
- **Esperado**: Todas las especialidades disponibles

### ✅ **Paso 4: Crear Turnos**

#### 4.1 Turno Particular
```json
POST /t/demo-clinic/appointments
{
  "professionalId": 1,
  "startDateTime": "2025-12-30T09:00:00",
  "notes": "Primera consulta",
  "patient": {
    "dni": "12345678",
    "firstName": "Juan",
    "lastName": "Pérez", 
    "email": "juan.perez@email.com",
    "phone": "011-1234-5678",
    "insuranceName": "Particular",
    "insuranceNumber": null
  }
}
```

#### 4.2 Turno con Obra Social
```json
POST /t/demo-clinic/appointments
{
  "professionalId": 2,
  "startDateTime": "2025-12-30T10:00:00",
  "notes": "Control de rutina",
  "patient": {
    "dni": "87654321",
    "firstName": "María", 
    "lastName": "González",
    "email": "maria.gonzalez@email.com",
    "phone": "011-8765-4321",
    "insuranceName": "OSDE",
    "insuranceNumber": "12345678901"
  }
}
```

**Respuesta Esperada**: 
- Status: `201 Created`
- Body: Datos completos del turno + URLs de WhatsApp y Google Calendar

### ✅ **Paso 5: Testing de Autenticación**

#### 5.1 Login como ADMIN
```json
POST /auth/login
{
  "email": "admin@medical-saas.com",
  "password": "admin123"
}
```

#### 5.2 Login como OWNER  
```json
POST /auth/login
{
  "email": "owner@demo-clinic.com",
  "password": "owner123"
}
```

#### 5.3 Login como STAFF
```json
POST /auth/login
{
  "email": "staff@demo-clinic.com", 
  "password": "staff123"
}
```

**Nota**: Los scripts automáticos en Postman guardarán el JWT token en la variable `{{jwt_token}}`.

### ✅ **Paso 6: APIs Privadas (Requieren JWT)**

#### 6.1 Listar Turnos del Tenant
- **Endpoint**: `GET /appointments`
- **Headers**: `Authorization: Bearer {{jwt_token}}`
- **Esperado**: Lista de turnos del tenant autenticado

## 🗄️ Acceso a Base de Datos H2

### URL de Consola H2
```
http://localhost:8080/h2-console
```

### Configuración de Conexión
- **JDBC URL**: `jdbc:h2:mem:medical`
- **Username**: `sa`  
- **Password**: *(vacío)*

### Consultas SQL de Ejemplo
```sql
-- Ver todos los tenants
SELECT * FROM tenants;

-- Ver usuarios por tenant
SELECT u.*, r.name as role_name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id;

-- Ver profesionales
SELECT p.*, s.name as specialty_name 
FROM professionals p 
JOIN specialties s ON p.specialty_id = s.id;

-- Ver turnos
SELECT a.*, 
       pat.first_name || ' ' || pat.last_name as patient_name,
       pro.first_name || ' ' || pro.last_name as professional_name
FROM appointments a
JOIN patients pat ON a.patient_id = pat.id  
JOIN professionals pro ON a.professional_id = pro.id;
```

## 🔧 Troubleshooting

### Problema: Error 404
- **Solución**: Verificar que la aplicación esté corriendo en puerto 8080
- **Comando**: `netstat -ano | findstr :8080`

### Problema: Error de compilación
- **Solución**: Algunos servicios tienen errores. Usar solo las APIs que funcionan
- **APIs Funcionando**: `/public/*`, `/t/{slug}/*`, `/auth/*`

### Problema: JWT no funciona
- **Solución**: Ejecutar primero uno de los endpoints de login para obtener token

## 📈 Próximos Pasos

1. **Testing Completo**: Probar todos los endpoints en la colección
2. **Validaciones**: Verificar validaciones de datos (DNI duplicado, etc.)
3. **Emails**: Verificar que se muestren logs de emails simulados
4. **WhatsApp**: Probar URLs generadas de WhatsApp
5. **Google Calendar**: Probar URLs de Google Calendar

## 🎯 Criterios de Éxito

✅ Paciente puede reservar turno sin autenticación  
✅ Sistema previene turnos duplicados  
✅ Emails se simulan correctamente (logs)  
✅ URLs de WhatsApp y Calendar se generan  
✅ Multi-tenancy funciona (slug demo-clinic)  
✅ Autenticación JWT funciona  
✅ Roles ADMIN, OWNER, STAFF funcionan

---

🎉 **¡El MVP está listo para testing!**
