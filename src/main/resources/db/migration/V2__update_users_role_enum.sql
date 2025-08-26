-- Update users table to use ENUM for role column
ALTER TABLE users MODIFY COLUMN role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER';

-- Insert default admin user if not exists
INSERT IGNORE INTO users (username, password, email, role) 
VALUES ('admin', '$2a$10$rDmFN6ZqJdcQKzKzKzKzK.9zKzKzKzKzKzKzKzKzKzKzKzKzKzKzK', 'admin@promptsaas.com', 'ADMIN');
