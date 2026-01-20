-- Migración V3: Arreglar ID auto-increment en professional_schedules

-- Crear tabla nueva con estructura correcta y BIGSERIAL para auto-increment
CREATE TABLE professional_schedules_fixed (
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

-- Migrar datos existentes (si hay alguno) excluyendo la columna id
INSERT INTO professional_schedules_fixed (professional_id, day_of_week, start_time, end_time, slot_duration_minutes, active, created_at)
SELECT 
    professional_id,
    CASE 
        WHEN day_of_week::text ~ '^[0-9]+$' THEN
            CASE day_of_week::integer
                WHEN 1 THEN 'MONDAY'
                WHEN 2 THEN 'TUESDAY'
                WHEN 3 THEN 'WEDNESDAY'
                WHEN 4 THEN 'THURSDAY'
                WHEN 5 THEN 'FRIDAY'
                WHEN 6 THEN 'SATURDAY'
                WHEN 7 THEN 'SUNDAY'
                ELSE 'MONDAY'
            END
        ELSE day_of_week  -- Ya es string
    END,
    start_time,
    end_time,
    COALESCE(slot_duration_minutes, 30),
    COALESCE(active, true),
    COALESCE(created_at, CURRENT_TIMESTAMP)
FROM professional_schedules
WHERE professional_id IS NOT NULL;

-- Eliminar tabla original
DROP TABLE professional_schedules CASCADE;

-- Renombrar tabla nueva
ALTER TABLE professional_schedules_fixed RENAME TO professional_schedules;

-- Crear índices para mejorar performance
CREATE INDEX idx_prof_schedules_professional_id ON professional_schedules(professional_id);
CREATE INDEX idx_prof_schedules_day ON professional_schedules(professional_id, day_of_week);