-- V14: Agregar user_id a la tabla professionals para vincular con usuarios
-- Permite que los profesionales puedan iniciar sesión

ALTER TABLE professionals ADD COLUMN user_id BIGINT;

-- Crear índice para búsqueda eficiente
CREATE INDEX idx_professionals_user_id ON professionals(user_id);

-- Agregar foreign key constraint
ALTER TABLE professionals ADD CONSTRAINT fk_professionals_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

