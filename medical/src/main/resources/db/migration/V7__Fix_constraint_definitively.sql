-- V7: Fix constraint definitively - remove old and add correct one
-- Drop all existing constraints related to day/date validation
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_flexible;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v2;
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_v6;

-- Add the correct constraint that allows either day_of_week OR specific_date but not both NULL
ALTER TABLE professional_schedules ADD CONSTRAINT chk_day_or_date_v7
CHECK ((day_of_week IS NOT NULL) OR (specific_date IS NOT NULL));