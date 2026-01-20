-- V10: Simple and direct constraint fix
-- Simply drop any existing constraints and add the correct one
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_flexible;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v2;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v6;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v7;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_final;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v9_final;

-- Add the correct constraint
ALTER TABLE professional_schedules ADD CONSTRAINT chk_day_or_date_working
CHECK ((day_of_week IS NOT NULL) OR (specific_date IS NOT NULL));