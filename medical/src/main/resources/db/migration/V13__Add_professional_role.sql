-- V13: Agregar rol PROFESSIONAL para médicos/especialistas
-- Este rol permite que los profesionales de la salud se registren en el sistema

INSERT INTO roles (name, description) 
VALUES ('PROFESSIONAL', 'Profesional médico/especialista del consultorio')
ON CONFLICT (name) DO NOTHING;
