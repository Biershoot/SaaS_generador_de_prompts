-- Script para verificar el estado de la base de datos MySQL
-- Ejecuta este script para ver qué hay en la base de datos

-- Usar la base de datos
USE prompt_saas;

-- Verificar todas las tablas existentes
SHOW TABLES;

-- Verificar si existe la tabla de Flyway
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'prompt_saas' 
AND TABLE_NAME LIKE '%flyway%';

-- Verificar si existen las tablas de la aplicación
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'prompt_saas' 
AND TABLE_NAME IN ('users', 'prompts', 'subscriptions');
