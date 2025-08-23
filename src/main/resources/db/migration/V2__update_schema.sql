-- Add email column to users table if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(100) NOT NULL UNIQUE;

-- Add category column to prompts table if it doesn't exist
ALTER TABLE prompts ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Create subscriptions table if it doesn't exist
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Update foreign key constraint for prompts table
ALTER TABLE prompts DROP FOREIGN KEY IF EXISTS fk_prompt_user;
ALTER TABLE prompts ADD CONSTRAINT fk_prompt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
