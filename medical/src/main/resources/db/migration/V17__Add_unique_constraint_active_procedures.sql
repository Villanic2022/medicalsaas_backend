/* V17: Agregar restricción única para procedimientos activos con el mismo nombre */

/*
 * Crear un índice único parcial que solo aplica a procedimientos activos.
 * Esto permite que el mismo nombre pueda existir múltiples veces si están inactivos (soft deleted),
 * pero solo una vez si está activo por tenant.
 */
CREATE UNIQUE INDEX idx_unique_active_procedure_name
    ON procedures(tenant_id, name)
    WHERE active = TRUE;

/*
 * Nota: Este índice único parcial garantiza que:
 * - No pueden existir dos procedimientos activos con el mismo nombre en un tenant
 * - Pero SÍ pueden existir múltiples procedimientos eliminados (active=false) con el mismo nombre
 * - Permite recrear/reactivar procedimientos previamente eliminados
 */

