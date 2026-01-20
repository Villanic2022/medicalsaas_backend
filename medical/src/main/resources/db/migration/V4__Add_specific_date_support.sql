-- Agregar soporte para excepciones de horarios por fecha específica
-- Si specific_date es NULL -> configuración recurrente semanal (usa day_of_week)
-- Si specific_date NO es NULL -> configuración específica para esa fecha (day_of_week puede ser null)

-- Agregar campo para fecha específica
ALTER TABLE professional_schedules 
ADD COLUMN specific_date DATE NULL;

-- Hacer day_of_week opcional (para permitir configuraciones específicas por fecha)
ALTER TABLE professional_schedules 
ALTER COLUMN day_of_week DROP NOT NULL;

-- Agregar constraint para asegurar que se especifique al menos uno: day_of_week O specific_date
ALTER TABLE professional_schedules 
ADD CONSTRAINT chk_day_or_date 
CHECK (
    (day_of_week IS NOT NULL AND specific_date IS NULL) OR 
    (day_of_week IS NULL AND specific_date IS NOT NULL)
);

-- Crear índice compuesto para mejorar performance en búsquedas por fecha específica
CREATE INDEX idx_professional_schedules_specific_date 
ON professional_schedules(professional_id, specific_date, active) 
WHERE specific_date IS NOT NULL;

-- Crear índice compuesto para mejorar performance en búsquedas recurrentes
CREATE INDEX idx_professional_schedules_recurring 
ON professional_schedules(professional_id, day_of_week, active) 
WHERE specific_date IS NULL;