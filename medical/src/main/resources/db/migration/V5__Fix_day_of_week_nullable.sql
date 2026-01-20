-- URGENTE: Hacer day_of_week nullable para soportar fechas específicas
-- Esto permite que day_of_week sea NULL cuando se especifica specific_date

-- Eliminar constraint existente si existe
ALTER TABLE professional_schedules 
DROP CONSTRAINT IF EXISTS chk_day_or_date;

-- Hacer day_of_week nullable
ALTER TABLE professional_schedules 
ALTER COLUMN day_of_week DROP NOT NULL;

-- Recrear constraint mejorado
ALTER TABLE professional_schedules 
ADD CONSTRAINT chk_day_or_date_v2 
CHECK (
    (day_of_week IS NOT NULL AND specific_date IS NULL) OR 
    (day_of_week IS NULL AND specific_date IS NOT NULL) OR
    (day_of_week IS NOT NULL AND specific_date IS NOT NULL)
);

-- Índices para optimización
DROP INDEX IF EXISTS idx_professional_schedules_specific_date;
DROP INDEX IF EXISTS idx_professional_schedules_recurring;

CREATE INDEX idx_professional_schedules_specific_date 
ON professional_schedules(professional_id, specific_date, active) 
WHERE specific_date IS NOT NULL;

CREATE INDEX idx_professional_schedules_recurring 
ON professional_schedules(professional_id, day_of_week, active) 
WHERE specific_date IS NULL;