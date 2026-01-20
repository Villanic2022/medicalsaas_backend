-- V8: Force remove old constraint and add new one with explicit names
-- Check current constraints
DO $$ 
BEGIN
    -- Drop old constraints by exact name
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_name = 'professional_schedules' 
               AND constraint_name = 'chk_day_or_date_flexible') THEN
        ALTER TABLE professional_schedules DROP CONSTRAINT chk_day_or_date_flexible;
        RAISE NOTICE 'Dropped constraint chk_day_or_date_flexible';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_name = 'professional_schedules' 
               AND constraint_name = 'chk_day_or_date_v2') THEN
        ALTER TABLE professional_schedules DROP CONSTRAINT chk_day_or_date_v2;
        RAISE NOTICE 'Dropped constraint chk_day_or_date_v2';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_name = 'professional_schedules' 
               AND constraint_name = 'chk_day_or_date_v6') THEN
        ALTER TABLE professional_schedules DROP CONSTRAINT chk_day_or_date_v6;
        RAISE NOTICE 'Dropped constraint chk_day_or_date_v6';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_name = 'professional_schedules' 
               AND constraint_name = 'chk_day_or_date_v7') THEN
        ALTER TABLE professional_schedules DROP CONSTRAINT chk_day_or_date_v7;
        RAISE NOTICE 'Dropped constraint chk_day_or_date_v7';
    END IF;
END $$;

-- Add the final correct constraint
ALTER TABLE professional_schedules ADD CONSTRAINT chk_day_or_date_final
CHECK ((day_of_week IS NOT NULL) OR (specific_date IS NOT NULL));