-- Migración V2: Arreglar tabla professional_schedules para disponibilidades

-- Agregar columna slot_duration_minutes con valor por defecto
ALTER TABLE professional_schedules ADD COLUMN slot_duration_minutes INT DEFAULT 30;

-- Agregar columna created_at con valor por defecto  
ALTER TABLE professional_schedules ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Actualizar valores existentes
UPDATE professional_schedules SET slot_duration_minutes = 30 WHERE slot_duration_minutes IS NULL;
UPDATE professional_schedules SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

-- Hacer que slot_duration_minutes sea NOT NULL
ALTER TABLE professional_schedules ALTER COLUMN slot_duration_minutes SET NOT NULL;

-- Crear tabla nueva con estructura correcta
CREATE TABLE professional_schedules_new (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prof_avail_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
    CONSTRAINT chk_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_slot_duration CHECK (slot_duration_minutes >= 5 AND slot_duration_minutes <= 120),
    CONSTRAINT chk_time_order CHECK (start_time < end_time)
);

-- Migrar datos existentes (si hay alguno)
INSERT INTO professional_schedules_new (professional_id, day_of_week, start_time, end_time, slot_duration_minutes, active, created_at)
SELECT 
    professional_id,
    CASE 
        WHEN day_of_week = 1 THEN 'MONDAY'
        WHEN day_of_week = 2 THEN 'TUESDAY'
        WHEN day_of_week = 3 THEN 'WEDNESDAY'
        WHEN day_of_week = 4 THEN 'THURSDAY'
        WHEN day_of_week = 5 THEN 'FRIDAY'
        WHEN day_of_week = 6 THEN 'SATURDAY'
        WHEN day_of_week = 7 THEN 'SUNDAY'
        ELSE 'MONDAY'
    END,
    start_time,
    end_time,
    COALESCE(slot_duration_minutes, 30),
    active,
    COALESCE(created_at, CURRENT_TIMESTAMP)
FROM professional_schedules;

-- Eliminar tabla original
DROP TABLE professional_schedules;

-- Renombrar tabla nueva
ALTER TABLE professional_schedules_new RENAME TO professional_schedules;

-- Crear índices para mejorar performance
CREATE INDEX idx_prof_schedules_professional_id ON professional_schedules(professional_id);
CREATE INDEX idx_prof_schedules_day ON professional_schedules(professional_id, day_of_week);