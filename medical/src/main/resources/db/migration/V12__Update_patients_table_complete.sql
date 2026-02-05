/* V12 - Update patients table with complete patient management fields */

/* Add new columns to patients table */
ALTER TABLE patients 
ADD COLUMN birth_date DATE,
ADD COLUMN gender VARCHAR(10),
ADD COLUMN address VARCHAR(500),
ADD COLUMN insurance_company_id BIGINT,
ADD COLUMN preferred_professional_id BIGINT,
ADD COLUMN notes TEXT,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN active BOOLEAN DEFAULT TRUE;

/* Modify existing columns */
ALTER TABLE patients ALTER COLUMN email DROP NOT NULL;

/* Add foreign key constraints */
ALTER TABLE patients 
ADD CONSTRAINT fk_patients_insurance_company 
FOREIGN KEY (insurance_company_id) REFERENCES insurance_companies(id);

ALTER TABLE patients 
ADD CONSTRAINT fk_patients_preferred_professional 
FOREIGN KEY (preferred_professional_id) REFERENCES professionals(id);

/* Create indexes for performance */
CREATE INDEX idx_patients_dni ON patients(tenant_id, dni) WHERE active = TRUE;
CREATE INDEX idx_patients_search ON patients(tenant_id, first_name, last_name, dni) WHERE active = TRUE;
CREATE INDEX idx_patients_insurance ON patients(insurance_company_id) WHERE active = TRUE;
CREATE INDEX idx_patients_preferred_professional ON patients(preferred_professional_id) WHERE active = TRUE;

/* Update existing patients to be active */
UPDATE patients SET active = TRUE WHERE active IS NULL;