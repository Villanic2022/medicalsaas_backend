-- Corregir constraint que está causando el error
-- El constraint actual es demasiado estricto

-- Eliminar constraint problemático
ALTER TABLE professional_schedules 
DROP CONSTRAINT IF EXISTS chk_day_or_date_v2;

-- Crear constraint más flexible que permita al menos uno de los campos
ALTER TABLE professional_schedules 
ADD CONSTRAINT chk_day_or_date_flexible
CHECK (
    (day_of_week IS NOT NULL OR specific_date IS NOT NULL)
);