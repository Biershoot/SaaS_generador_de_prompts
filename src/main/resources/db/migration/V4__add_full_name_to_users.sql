-- Add full_name column to users table
ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NOT NULL DEFAULT 'Usuario';

-- Update existing users to have a default full name
UPDATE users SET full_name = CONCAT('Usuario ', id) WHERE full_name = 'Usuario';
