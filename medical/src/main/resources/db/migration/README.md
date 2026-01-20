# Migraciones de Base de Datos con Flyway

## Estructura de directorios
```
src/main/resources/
└── db/
    └── migration/
        ├── V1__Initial_schema.sql
        ├── V2__Add_patients_table.sql
        └── V3__Add_appointments_table.sql
```

## Convención de nombres de Flyway

Los archivos de migración deben seguir esta convención:
- **V{version}__{description}.sql**

### Ejemplos:
- `V1__Initial_schema.sql` - Migración inicial (versión 1)
- `V2__Add_patients_table.sql` - Agregar tabla de pacientes (versión 2)
- `V3__Add_appointments_table.sql` - Agregar tabla de citas (versión 3)
- `V4__Update_user_table.sql` - Actualizar tabla de usuarios (versión 4)

### Reglas importantes:
1. **V** (mayúscula) seguido del número de versión
2. **Doble guión bajo** `__` después del número de versión
3. **Descripción** en inglés, palabras separadas por guiones bajos
4. **Extensión** .sql
5. Los números de versión deben ser secuenciales
6. **NUNCA** modifiques un archivo de migración que ya se ejecutó en producción

### Tipos de migraciones:
- **V** - Versioned migrations (migraciones versionadas)
- **U** - Undo migrations (migraciones de deshacer) - Solo en versión Pro
- **R** - Repeatable migrations (migraciones repetibles)

### Comandos útiles:
```bash
# Ejecutar migraciones
./mvnw flyway:migrate

# Ver información del estado
./mvnw flyway:info

# Validar migraciones
./mvnw flyway:validate

# Limpiar base de datos (¡CUIDADO! - solo para desarrollo)
./mvnw flyway:clean
```

## Configuración en application.properties

```properties
# Flyway configuration
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.sql-migration-prefix=V
spring.flyway.sql-migration-separator=__
spring.flyway.sql-migration-suffixes=.sql
```
