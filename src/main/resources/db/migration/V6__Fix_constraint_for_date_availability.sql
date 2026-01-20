-- Fix constraint to be more flexible for specific date availability
-- Remove the overly restrictive constraint from V5
ALTER TABLE professional_schedules DROP CONSTRAINT IF EXISTS chk_day_or_date_flexible;

-- Add a more flexible constraint that allows either day_of_week OR specific_date
-- This prevents both from being null, but allows one to be null when the other is set
ALTER TABLE professional_schedules ADD CONSTRAINT chk_day_or_date_v6 
CHECK ((day_of_week IS NOT NULL) OR (specific_date IS NOT NULL));